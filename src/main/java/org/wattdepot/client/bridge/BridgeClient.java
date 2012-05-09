package org.wattdepot.client.bridge;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.XMLGregorianCalendar;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.wattdepot.client.BadXmlException;
import org.wattdepot.client.MiscClientException;
import org.wattdepot.client.NotAuthorizedException;
import org.wattdepot.client.OverwriteAttemptedException;
import org.wattdepot.client.ResourceNotFoundException;
import org.wattdepot.client.WattDepotClient;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.resource.source.jaxb.Source;
import org.wattdepot.resource.user.jaxb.User;
import org.wattdepot.util.UriUtils;
import org.wattdepot.util.tstamp.Tstamp;

/**
 * Transfers data from one WattDepot server to another. The two servers do not need to be the same
 * version or use the same database, as long as they can both use the same client version.
 * 
 * Note that sources with multi-level hierarchies (a virtual source that has a virtual source as a
 * subsource) may not transfer correctly on the first pass. Virtual sources are transferred in the
 * order they are pulled from the origin server, and if parent sources are transferred before their
 * subsources the transfer will fail. If this happens, simply rerun the bridge giving the specific
 * source name that failed as the source argument.
 * 
 * @author Andrea Connell
 * 
 */

public class BridgeClient {

  private WattDepotClient originClient;
  private WattDepotClient destClient;
  private String destUri;
  private int interval;
  private String sourceName;
  private XMLGregorianCalendar startTime;
  private XMLGregorianCalendar endTime;

  int userSuccessCount = 0;
  int userErrorCount = 0;
  int sourceSuccessCount = 0;
  int sourceErrorCount = 0;
  int sensorDataSuccessCount = 0;
  int sensorDatasSkippedCount = 0;
  int sensorDataErrorCount = 0;

  /** Name of this tool. */
  private static final String toolName = "BridgeClient";

  /**
   * Create a bridge client given the origin server and destination server connection details. If
   * interval is set (greater than zero), sensor datas for will be skipped if they are less than
   * interval seconds after the previous transferred sensor data from the same source. If sourceName
   * is set (non-null), only sensor datas from that source will be transferred. If startTime is set
   * (non-null), only sensor datas after that timestamp will be transferred. If endTime is set
   * (non-null), only sensor datas before that timestamp will be transferred.
   * 
   * @param originUri The URI of the server to transfer data from.
   * @param originAdmin The username for an administrator of the origin server.
   * @param originPass The password for the administrator of the origin server.
   * @param destUri The URI of the server to transfer data to.
   * @param destAdmin The username for an administrator of the destination server.
   * @param destPass The password for the administrator of the destination server.
   * @param interval The maximum frequency in seconds to transfer sensor datas for.
   * @param sourceName The sourceName to transfer data for (or if null, transfer for all sources).
   * @param startTime The beginning of the time range to transfer data for.
   * @param endTime The end of the time range to transfer data for.
   */
  public BridgeClient(String originUri, String originAdmin, String originPass, String destUri,
      String destAdmin, String destPass, int interval, String sourceName,
      XMLGregorianCalendar startTime, XMLGregorianCalendar endTime) {

    this.destUri = destUri;
    this.originClient = new WattDepotClient(originUri, originAdmin, originPass);
    this.destClient = new WattDepotClient(destUri, destAdmin, destPass);
    this.interval = interval;
    this.sourceName = sourceName;
    this.startTime = startTime;
    this.endTime = endTime;

    if (!originClient.isHealthy()) {
      throw new RuntimeException("Connection to origin at " + originUri + " failed.");
    }
    if (!originClient.isAuthenticated()) {
      throw new RuntimeException("Authentication to origin at " + originUri + " failed.");
    }
    if (!destClient.isHealthy()) {
      throw new RuntimeException("Connection to destination at " + destUri + " failed.");
    }
    if (!destClient.isAuthenticated()) {
      throw new RuntimeException("Authentication to destination at " + destUri + " failed.");
    }
    System.out.println("Connections to origin and destination servers was successful.");
  }

  /**
   * Begin the transfer of data between the sourceName server and the destination server.
   */
  private void transfer() {

    Source source = null;
    if (sourceName != null) {
      try {
        source = originClient.getSource(sourceName);
      }
      catch (NotAuthorizedException e) {
        System.out.println("The givin origin user is not authorized to retrieve " + sourceName);
        return;
      }
      catch (ResourceNotFoundException e) {
        System.out.println("The source " + sourceName + " was not found on the origin server.");
        return;
      }
      catch (BadXmlException e) {
        System.out.println("An exception occured when retrieving the source " + sourceName);
        return;
      }
      catch (MiscClientException e) {
        System.out.println("An exception occured when retrieving the source " + sourceName);
        return;
      }
    }

    System.out.println("Starting transfer...");
    Long before = new Date().getTime();
    storeUsers(source);
    storeSources(source);
    Long after = new Date().getTime();

    System.out.println("\n ----------------------- \n");
    if (userErrorCount < 0) {
      System.out.println("ERROR: No users could be transferred between servers!");
    }
    else if (userErrorCount == 0) {
      System.out.println(userSuccessCount + " users were transferred successfully.");
    }
    else {
      System.out.println(userSuccessCount + " users were transferred successfully.");
      System.out.println(userErrorCount + " users could not be transferred.");
    }

    if (sourceErrorCount < 0) {
      System.out
          .println("ERROR: No sources or sensor datas could be transferred between servers!");
    }
    else {
      System.out.println(sourceSuccessCount + " sources were transferred successfully.");
      System.out.println(sourceErrorCount + " sources could not be transferred.");
    }
    Long duration = (after - before) / 60000;
    System.out.println("Took " + duration + " minutes to complete");
  }

  /**
   * Transfer sources.
   * 
   * @param source If not null, only transfer the given source and the given source's sensor data.
   */
  private void storeSources(Source source) {
    try {

      List<Source> sources = null;
      if (source != null) {
        sources = new ArrayList<Source>();
        sources.add(source);
      }
      else {
        sources = originClient.getSources();
      }
      List<Source> virtualSources = new ArrayList<Source>();
      for (Source s : sources) {
        // Do virtual sources at the end
        if (s.isVirtual()) {
          virtualSources.add(s);
          continue;
        }
        try {
          if (destClient.storeSource(s, false)) {
            sourceSuccessCount++;
            System.out.println("Stored source " + s.getName()
                + "successfully. Now storing sensor datas.");
            storeSensorDatas(s.getName());
          }
          else {
            System.out.println("Could not store sourceName " + s.getName());
            sourceErrorCount++;
          }
        }
        catch (NotAuthorizedException e) {
          System.out.println("Could not store sourceName " + s.getName() + ": " + e.getMessage());
          sourceErrorCount++;
        }
        catch (BadXmlException e) {
          System.out.println("Could not store sourceName " + s.getName() + ": " + e.getMessage());
          sourceErrorCount++;
        }
        catch (MiscClientException e) {
          System.out.println("Could not store sourceName " + s.getName() + ": " + e.getMessage());
          sourceErrorCount++;

        }
        catch (OverwriteAttemptedException e) {
          try {
            Source existingSource = destClient.getSource(s.getName());

            if (existingSource.equals(s)) {
              System.out.println("Source " + s.getName() + " already exists on the destination "
                  + "and is equivalent to the one on the origin.");
              storeSensorDatas(s.getName());
            }
            else {
              System.out.println("Source " + s.getName() + " already exists on the destination "
                  + "and is NOT equivalent to the one on the origin.");
              System.out.println(" Origin: " + s);
              System.out.println(" Dest:   " + existingSource);
              sourceErrorCount++;
              storeSensorDatas(s.getName());
            }
          }
          catch (ResourceNotFoundException e1) {
            // This should never happen
            System.out.println("Overwrite attempted on " + s.getName());
            sourceErrorCount++;
            storeSensorDatas(s.getName());
          }
        }
        catch (JAXBException e) {
          System.out.println("Could not store sourceName " + s.getName() + ": " + e.getMessage());
          sourceErrorCount++;
        }
      }
      for (Source s : virtualSources) {
        try {
          if (destClient.storeSource(s, false)) {
            sourceSuccessCount++;
          }
          else {
            System.out.println("Could not store sourceName " + s.getName());
            sourceErrorCount++;
          }
        }
        catch (NotAuthorizedException e) {
          System.out.println("Could not store sourceName " + s.getName() + ": " + e.getMessage());
          sourceErrorCount++;
        }
        catch (BadXmlException e) {
          System.out.println("Could not store sourceName " + s.getName() + ": " + e.getMessage());
          sourceErrorCount++;
        }
        catch (MiscClientException e) {
          System.out.println("Could not store sourceName " + s.getName() + ": " + e.getMessage());
          sourceErrorCount++;
        }
        catch (OverwriteAttemptedException e) {
          System.out.println("Source " + s.getName() + " already exists.");
          sourceErrorCount++;
        }
        catch (JAXBException e) {
          System.out.println("Could not store sourceName " + s.getName() + ": " + e.getMessage());
          sourceErrorCount++;
        }
      }
    }
    catch (NotAuthorizedException e) {
      System.out.println("Could not get sources from sourceName server: " + e.getMessage());
      sourceErrorCount = -1;
    }
    catch (BadXmlException e) {
      System.out.println("Could not get sources from sourceName server: " + e.getMessage());
      sourceErrorCount = -1;
    }
    catch (MiscClientException e) {
      System.out.println("Could not get sources from sourceName server: " + e.getMessage());
      sourceErrorCount = -1;
    }
  }

  /**
   * Transfer all users.
   * 
   * @param source If not null, only transfer the given source's owner.
   */
  private void storeUsers(Source source) {
    try {
      List<User> users = null;
      if (source != null) {
        users = new ArrayList<User>();
        try {
          users.add(originClient.getUser(UriUtils.getUriSuffix(source.getOwner())));
        }
        catch (ResourceNotFoundException e) {
          System.out.println("The user " + source.getOwner()
              + " could not be found on the origin server.");
          userErrorCount++;
        }
      }
      else {
        users = originClient.getUsers();
      }
      for (User u : users) {
        try {
          if (destClient.storeUser(u)) {
            System.out.println("Stored user " + u.getEmail() + " successfully.");
            userSuccessCount++;
          }
          else {
            System.out.println("Could not store user " + u.getEmail());
            userErrorCount++;
          }
        }
        catch (NotAuthorizedException e) {
          System.out.println("Could not store user " + u.getEmail() + ": " + e.getMessage());
          userErrorCount++;
        }
        catch (BadXmlException e) {
          System.out.println("Could not store user " + u.getEmail() + ": " + e.getMessage());
          userErrorCount++;
        }
        catch (MiscClientException e) {
          System.out.println("Could not store user " + u.getEmail() + ": " + e.getMessage());
          userErrorCount++;
        }
        catch (OverwriteAttemptedException e) {
          try {
            User existingUser = destClient.getUser(u.getEmail());

            if (existingUser.equals(u)) {
              System.out.println("User " + u.getEmail() + " already exists on the destination "
                  + "and is equivalent to the one on the origin.");
            }
            else {
              System.out.println("User " + u.getEmail() + " already exists on the destination "
                  + "and is NOT equivalent to the one on the origin.");
              System.out.println(" Origin: " + u);
              System.out.println(" Dest:   " + existingUser);
              userErrorCount++;
            }
          }
          catch (ResourceNotFoundException e1) {
            // This should never happen
            System.out.println("Overwrite attempted on " + u.getEmail());
            userErrorCount++;
          }
        }
        catch (JAXBException e) {
          System.out.println("Could not store user " + u.getEmail() + ": " + e.getMessage());
          userErrorCount++;
        }

      }
    }
    catch (NotAuthorizedException e) {
      System.out.println("Could not get users from sourceName server: " + e.getMessage());
      userErrorCount = -1;
    }
    catch (BadXmlException e) {
      System.out.println("Could not get users from sourceName server: " + e.getMessage());
      userErrorCount = -1;
    }
    catch (MiscClientException e) {
      System.out.println("Could not get users from sourceName server: " + e.getMessage());
      userErrorCount = -1;
    }

  }

  /**
   * Transfer all sensor datas for the sourceName with the given name.
   * 
   * @param name The name of the sourceName to transfer sensor datas for.
   */
  private void storeSensorDatas(String name) {

    if (startTime == null) {
      startTime = Tstamp.makeTimestamp(0);
    }
    if (endTime == null) {
      endTime = Tstamp.makeTimestamp();
    }

    XMLGregorianCalendar firstTime = null;
    XMLGregorianCalendar lastTime = null;
    sensorDataSuccessCount = 0;
    sensorDataErrorCount = 0;
    sensorDatasSkippedCount = 0;

    try {
      List<SensorData> datas = originClient.getSensorDatas(name, startTime, endTime);

      XMLGregorianCalendar compareTime = Tstamp.makeTimestamp(0);

      for (SensorData d : datas) {
        if (compareTime.compare(d.getTimestamp()) <= 0) {
          try {
            d.setSource(Source.sourceToUri(UriUtils.getUriSuffix(d.getSource()), destUri));
            if (destClient.storeSensorData(d)) {
              if (firstTime == null) {
                firstTime = d.getTimestamp();
                System.out.println("  First timestamp: " + firstTime);
              }
              lastTime = d.getTimestamp();
              sensorDataSuccessCount++;
            }
            else {
              System.out.println("  Could not store sensordata " + d.getSource() + " "
                  + d.getTimestamp());
              sensorDataErrorCount++;
            }
          }
          catch (NotAuthorizedException e) {
            System.out.println("  Could not store sensordata " + d.getSource() + " "
                + d.getTimestamp() + ": " + e.getMessage());
            sensorDataErrorCount++;
          }
          catch (BadXmlException e) {
            System.out.println("  Could not store sensordata " + d.getSource() + " "
                + d.getTimestamp() + ": " + e.getMessage());
            sensorDataErrorCount++;
          }
          catch (MiscClientException e) {
            System.out.println("  Could not store sensordata " + d.getSource() + " "
                + d.getTimestamp() + ": " + e.getMessage());
            sensorDataErrorCount++;
          }
          catch (OverwriteAttemptedException e) {
            try {
              SensorData existingData =
                  destClient.getSensorData(UriUtils.getUriSuffix(d.getSource()), d.getTimestamp());

              if (existingData.equals(d)) {
                System.out.println("  Sensor Data " + d.getSource() + " " + d.getTimestamp()
                    + " already exists on the destination "
                    + "and is equivalent to the one on the origin.");
              }
              else {
                System.out.println("  Sensor Data " + d.getSource() + " " + d.getTimestamp()
                    + " already exists on the destination "
                    + "and is NOT equivalent to the one on the origin.");
                System.out.println("   Origin: " + d);
                System.out.println("   Dest:   " + existingData);
                sensorDataErrorCount++;
              }
            }
            catch (ResourceNotFoundException e1) {
              // This should never happen
              System.out.println("  Overwrite attempted on " + d.getSource() + " "
                  + d.getTimestamp());
              sensorDataErrorCount++;
            }
            catch (NotAuthorizedException e1) {
              System.out.println("  Overwrite attempted on " + d.getSource() + " "
                  + d.getTimestamp());
              sensorDataErrorCount++;
            }
            catch (BadXmlException e1) {
              System.out.println("  Overwrite attempted on " + d.getSource() + " "
                  + d.getTimestamp());
              sensorDataErrorCount++;
            }
            catch (MiscClientException e1) {
              System.out.println("  Overwrite attempted on " + d.getSource() + " "
                  + d.getTimestamp());
              sensorDataErrorCount++;
            }
          }
          catch (ResourceNotFoundException e) {
            System.out.println("  Could not store sensordata " + d.getSource() + " "
                + d.getTimestamp() + ": " + e.getMessage());
            sensorDataErrorCount++;
          }
          catch (JAXBException e) {
            System.out.println("  Could not store sensordata " + d.getSource() + " "
                + d.getTimestamp() + ": " + e.getMessage());
            sensorDataErrorCount++;
          }

          compareTime = Tstamp.incrementMinutes(d.getTimestamp(), interval);
        }
        else {
          sensorDatasSkippedCount++;
        }
      }
    }
    catch (ResourceNotFoundException e) {
      System.out.println("  Could not get sensor data from origin server for " + name + ": "
          + e.getMessage());
      sensorDataErrorCount = -1;
    }
    catch (NotAuthorizedException e) {
      System.out.println("  Could not get sensor data from origin server for " + name + ": "
          + e.getMessage());
      sensorDataErrorCount = -1;
    }
    catch (BadXmlException e) {
      System.out.println("  Could not get sensor data from origin server for " + name + ": "
          + e.getMessage());
      sensorDataErrorCount = -1;
    }
    catch (MiscClientException e) {
      System.out.println("  Could not get sensor data from origin server for " + name + ": "
          + e.getMessage());
      sensorDataErrorCount = -1;
    }

    if (sensorDataErrorCount < 0) {
      System.out.println("  ERROR: No sensor data could be transferred for this source.");
    }
    else {
      if (lastTime != null) {
        System.out.println("  Last timestamp: " + lastTime);
      }

      System.out.println("  " + sensorDataSuccessCount
          + " sensor datas were transferred successfully for this source.");
      if (interval > 0) {
        System.out.println("  " + sensorDatasSkippedCount
            + " sensor datas were skipped for this source due to the interval.");
      }
      System.out.println("  " + sensorDataErrorCount
          + " sensor datas could not be transferred for this source.");
    }
  }

  /**
   * Process command line arguments, start the BridgeClient, and transfer data.
   * 
   * @param args command line arguments.
   */
  public static void main(String[] args) {
    Options options = new Options();
    options.addOption("h", "help", false, "Print this message");
    options.addOption("o", "Origin-Uri", true,
        "URI of the origin WattDepot server, ex. \"http://localhost:8182/oldwattdepot/\".");
    options.addOption("ou", "Origin-Username", true,
        "The username of the Administrator on the origin server, ex. \"admin@example.com\"");
    options.addOption("op", "Origin-Password", true,
        "The password for the Administrator on the origin server, ex. \"Passw0rd\"");
    options.addOption("d", "Destination-Uri", true,
        "URI of the destination WattDepot server, ex. \"http://localhost:8182/newwattdepot/\".");
    options.addOption("du", "Destination-Username", true,
        "The username of the Administrator on the destination server, ex. \"admin@example.com\"");
    options.addOption("dp", "Destination-Password", true,
        "The password for the Administrator on the destination server, ex. \"Passw0rd\"");
    options.addOption("interval", "Transfer-Interval", true,
        "The minimum desired number of minutes between sensor datas on the destination server");
    options.addOption("source", "Transfer-Source", true,
        "If this option is used, data will only be transferred for the given sourceName "
            + " rather than for all sources. ex. \"Ilima-04-telco\"");
    options.addOption("start", "Transfer-StartTime", true,
        "If this option is used, only data timestamped at or after the given start time "
            + " will be transferred. ex. \"2012-04-23T00:00:00.000-10:00\"");
    options.addOption("end", "Transfer-EndTime", true,
        "If this option is used, only data timestamped at or before the given end time "
            + " will be transferred. ex. \"2012-04-23T11:59:59.999-10:00\"");

    CommandLine cmd = null;
    String originUri = null, originAdmin = null, originPass = null;
    String destUri = null, destAdmin = null, destPass = null;
    int interval = 0;
    String sourceName = null;
    XMLGregorianCalendar startTime = null, endTime = null;

    CommandLineParser parser = new PosixParser();
    HelpFormatter formatter = new HelpFormatter();
    try {
      cmd = parser.parse(options, args);
    }
    catch (ParseException e) {
      System.err.println("Command line parsing failed. Reason: " + e.getMessage() + ". Exiting.");
      System.exit(1);
    }

    if (cmd.hasOption("o")) {
      originUri = cmd.getOptionValue("o");
    }
    else {
      System.err.println("Required origin URI parameter not provided, exiting.");
      formatter.printHelp(toolName, options);
      System.exit(1);
    }
    if (cmd.hasOption("ou")) {
      originAdmin = cmd.getOptionValue("ou");
    }
    else {
      System.err.println("Required origin admin username parameter not provided, exiting.");
      formatter.printHelp(toolName, options);
      System.exit(1);
    }
    if (cmd.hasOption("op")) {
      originPass = cmd.getOptionValue("op");
    }
    else {
      System.err.println("Required origin admin password parameter not provided, exiting.");
      formatter.printHelp(toolName, options);
      System.exit(1);
    }
    if (cmd.hasOption("d")) {
      destUri = cmd.getOptionValue("d");
    }
    else {
      System.err.println("Required Destination URI parameter not provided, exiting.");
      formatter.printHelp(toolName, options);
      System.exit(1);
    }
    if (cmd.hasOption("du")) {
      destAdmin = cmd.getOptionValue("du");
    }
    else {
      System.err.println("Required Destination Admin Username parameter not provided, exiting.");
      formatter.printHelp(toolName, options);
      System.exit(1);
    }
    if (cmd.hasOption("dp")) {
      destPass = cmd.getOptionValue("dp");
    }
    else {
      System.err.println("Required Destination Admin Password parameter not provided, exiting.");
      formatter.printHelp(toolName, options);
      System.exit(1);
    }
    if (cmd.hasOption("interval")) {
      try {
        interval = Integer.parseInt(cmd.getOptionValue("interval"));
      }
      catch (NumberFormatException e) {
        System.err.println("Interval parameter is not a valid integer.");
        formatter.printHelp(toolName, options);
        System.exit(1);
      }
    }
    if (cmd.hasOption("source")) {
      sourceName = cmd.getOptionValue("source");
    }
    if (cmd.hasOption("start")) {
      try {
        startTime = Tstamp.makeTimestamp(cmd.getOptionValue("start"));
      }
      catch (Exception e) {
        System.err.println("Start time parameter is not a valid date.");
        formatter.printHelp(toolName, options);
        System.exit(1);
      }
    }
    if (cmd.hasOption("end")) {
      try {
        endTime = Tstamp.makeTimestamp(cmd.getOptionValue("end"));
      }
      catch (Exception e) {
        System.err.println("End time parameter is not a valid date.");
        formatter.printHelp(toolName, options);
        System.exit(1);
      }
    }

    BridgeClient bridge =
        new BridgeClient(originUri, originAdmin, originPass, destUri, destAdmin, destPass,
            interval, sourceName, startTime, endTime);
    bridge.transfer();
  }
}

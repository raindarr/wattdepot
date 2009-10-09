package org.wattdepot.datainput;

import static org.wattdepot.datainput.DataInputClientProperties.BMO_AS_KEY;
import static org.wattdepot.datainput.DataInputClientProperties.BMO_DB_KEY;
import static org.wattdepot.datainput.DataInputClientProperties.BMO_PASSWORD_KEY;
import static org.wattdepot.datainput.DataInputClientProperties.BMO_URI_KEY;
import static org.wattdepot.datainput.DataInputClientProperties.BMO_USERNAME_KEY;
import static org.wattdepot.datainput.DataInputClientProperties.FILENAME_KEY;
import static org.wattdepot.datainput.DataInputClientProperties.WATTDEPOT_PASSWORD_KEY;
import static org.wattdepot.datainput.DataInputClientProperties.WATTDEPOT_URI_KEY;
import static org.wattdepot.datainput.DataInputClientProperties.WATTDEPOT_USERNAME_KEY;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.hackystat.utilities.tstamp.Tstamp;
import org.restlet.Client;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Preference;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.wattdepot.client.WattDepotClient;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

/**
 * Retrieves data from Building Manager Online (website for power meter data provided by Obvius),
 * writes the data into a TSV file (temporary kludge since no persistence in the server yet),
 * creates a SensorData object for each line of the table, and sends the SensorData objects off to a
 * WattDepot server.
 * 
 * @author Robert Brewer
 */
public class BMODataInputClient {

  /** Making PMD happy. */
  private static final String REQUIRED_PARAMETER_ERROR_MSG =
      "Required parameter %s not found in properties.";

  /** Name of the property file containing essential preferences. */
  protected DataInputClientProperties properties;

  /** Name of Source to send data to. */
  protected String sourceName;

  /** Number of the meter to download data for. */
  protected String meterNumber;

  /** Starting point for data download. */
  protected String startTimestamp;

  /** Name of the application on the command line. */
  protected static final String toolName = "BMODataInputClient";

  /** The parser used to turn rows into SensorData objects. */
  protected RowParser parser;

  /** The client used to send SensorData to WattDepot server. */
  protected WattDepotClient client;

  /**
   * Creates the new BMODataInputClient.
   * 
   * @param propertyFilename Name of the file to read essential properties from.
   * @param sourceName name of the Source the sensor data should be sent to.
   * @param meterNumber The number of the meter data is to be read from.
   * @param startTimestamp Starting point for data download (ignored if output file already exists).
   * @throws IOException If the property file cannot be found.
   */
  public BMODataInputClient(String propertyFilename, String sourceName, String meterNumber,
      String startTimestamp) throws IOException {
    this.properties = new DataInputClientProperties(propertyFilename);
    // DEBUG
    System.err.println(this.properties.echoProperties());
    this.sourceName = sourceName;
    this.meterNumber = meterNumber;
    this.startTimestamp = startTimestamp;
  }

  /**
   * Does the work of inputting the data into WattDepot.
   * 
   * @return True if the data could be successfully input, false otherwise.
   */
  public boolean process() {
    // Extract all the properties we need, and fail if any critical ones are missing.
    String wattDepotURI = this.properties.get(WATTDEPOT_URI_KEY);
    if (wattDepotURI == null) {
      System.err.format(REQUIRED_PARAMETER_ERROR_MSG, WATTDEPOT_URI_KEY);
      return false;
    }
    String wattDepotUsername = this.properties.get(WATTDEPOT_USERNAME_KEY);
    if (wattDepotUsername == null) {
      System.err.format(REQUIRED_PARAMETER_ERROR_MSG, WATTDEPOT_USERNAME_KEY);
      return false;
    }
    String wattDepotPassword = this.properties.get(WATTDEPOT_PASSWORD_KEY);
    if (wattDepotPassword == null) {
      System.err.format(REQUIRED_PARAMETER_ERROR_MSG, WATTDEPOT_PASSWORD_KEY);
      return false;
    }
    String dataFilename = this.properties.get(FILENAME_KEY);
    if (dataFilename == null) {
      System.err.format(REQUIRED_PARAMETER_ERROR_MSG, FILENAME_KEY);
      return false;
    }
    String bmoURI = this.properties.get(BMO_URI_KEY);
    if (bmoURI == null) {
      System.err.format(REQUIRED_PARAMETER_ERROR_MSG, BMO_URI_KEY);
      return false;
    }
    String bmoUsername = this.properties.get(BMO_USERNAME_KEY);
    if (bmoUsername == null) {
      System.err.format(REQUIRED_PARAMETER_ERROR_MSG, BMO_USERNAME_KEY);
      return false;
    }
    String bmoPassword = this.properties.get(BMO_PASSWORD_KEY);
    if (bmoPassword == null) {
      System.err.format(REQUIRED_PARAMETER_ERROR_MSG, BMO_PASSWORD_KEY);
      return false;
    }
    String bmoDB = this.properties.get(BMO_DB_KEY);
    if (bmoDB == null) {
      System.err.format(REQUIRED_PARAMETER_ERROR_MSG, BMO_DB_KEY);
      return false;
    }
    String bmoAS = this.properties.get(BMO_AS_KEY);
    if (bmoAS == null) {
      System.err.format(REQUIRED_PARAMETER_ERROR_MSG, BMO_AS_KEY);
      return false;
    }

    // Use the tabular client to send existing data from file
    TabularFileDataInputClient tabularClient =
        new TabularFileDataInputClient(dataFilename, wattDepotURI, this.sourceName,
            wattDepotUsername, wattDepotPassword, false);
    this.client = new WattDepotClient(wattDepotURI, wattDepotUsername, wattDepotPassword);

    List<SensorData> list = null;

    this.parser = new VerisRowParser(toolName, wattDepotURI, sourceName);
    XMLGregorianCalendar lastTimestamp;

    // Check if we already have some data from previous client executions
    File dataFile = new File(dataFilename);
    if (dataFile.exists() && (dataFile.length() > 0)) {
      try {
        list = tabularClient.readFile(dataFilename, false, this.parser);
      }
      catch (IOException e) {
        return false;
      }

      if (list == null) {
        return false;
      }
      else {
        // We assume that our data file is monotonically increasing in time, so last element is
        // most recent.
        lastTimestamp = list.get(list.size() - 1).getTimestamp();
        System.out.println("last timestamp from file: " + lastTimestamp.toString());
        if (!tabularClient.sendData(this.client, list)) {
          return false;
        }
      }
    }
    else {
      // No data file, there had better timestamp provided on the command line
      if (this.startTimestamp == null) {
        System.err.println("Need an existing data file, or startTimestamp parameter to determine"
            + "start time.");
        return false;
      }
      try {
        dataFile.createNewFile();
      }
      catch (IOException e) {
        System.err.println("Unable to create output file.");
        return false;
      }
      try {
        // Make a timestamp out of the command line argument
        lastTimestamp = Tstamp.makeTimestamp(this.startTimestamp);
      }
      catch (Exception e) {
        System.err.println("Bad startTimestamp format.");
        return false;
      }
    }
    // Send request to BMO for data from last timestamp to right now
    Response response =
        makeBMORequest(bmoURI, bmoUsername, bmoPassword, bmoDB, bmoAS, this.meterNumber,
            lastTimestamp, Tstamp.makeTimestamp());
    Status status = response.getStatus();
    if (status.isSuccess()) {
      if (response.isEntityAvailable()) {
        // Finally we have content from the server
        CSVReader reader;
        try {
          reader = new CSVReader(response.getEntity().getReader(), '\t');
        }
        catch (IOException e) {
          System.err.println("Problem reading entity body from BMO");
          return false;
        }
        // nextLine[] will hold an array of values from the current row in the file
        String[] nextLine;
        // Make a CSVWriter for appending new data to the data file
        CSVWriter writer;
        try {
          writer =
              new CSVWriter(new FileWriter(dataFile, true), '\t', CSVWriter.NO_QUOTE_CHARACTER);
        }
        catch (IOException e) {
          System.err.println("Unable to open data file for writing.");
          return false;
        }
        try {
          while ((nextLine = reader.readNext()) != null) {
            SensorData data = parser.parseRow(nextLine);
            if (data == null) {
              System.err.println("Got unparseable data from BMO: " + Arrays.toString(nextLine));
              return false;
            }
            else {
              XMLGregorianCalendar dataTimestamp = data.getTimestamp();
              // Make sure this timestamp is not the overlap from the last fetch
              if (!dataTimestamp.equals(lastTimestamp)) {
                // Write out line to data file
                writer.writeNext(nextLine);
                // Send sensor data to WattDepot
                this.client.storeSensorData(data);
                // This timestamp becomes the lastTimestamp (for now)
                lastTimestamp = dataTimestamp;
              }
            }
          }
        }
        catch (Exception e) {
          System.err.println("Problem encountered processing BMO data: " + e.toString());
          return false;
        }
        // Finished processing all the BMO data received
        try {
          reader.close();
        }
        catch (IOException e) {
          System.err.println("Problem encountered closing BMO connection.");
        }
        try {
          writer.close();
        }
        catch (IOException e) {
          System.err.println("Problem encountered closing data file.");
        }
        return true;
      }
      else {
        System.err.println("BMO response was successful, but contained no data?");
        return false;
      }
    }
    else if (status.isError()) {
      System.err.format("Unable to retrieve data from BMO, status code: %d %s %s",
          status.getCode(), status.getName(), status.getDescription());
      return false;
    }
    else {
      System.err.format(
          "Unexpected status from BMO (neither error nor success), status code: %d %s %s", status
              .getCode(), status.getName(), status.getDescription());
      return false;
    }
  }

  /**
   * Makes an HTTP request to BMO server for meter data, returning the response from the server.
   * 
   * @param uri The URI of the BMO server.
   * @param username Username for the BMO server.
   * @param password Password for the BMO server.
   * @param dbValue DB parameter value for the BMO server.
   * @param asValue AS parameter value for the BMO server.
   * @param meterNumber The number of the meter data is to be fetched from.
   * @param startTime The desired starting point from which data will be retrieved.
   * @param endTime The desired starting point from which data will be retrieved.
   * @return The Response instance returned from the server.
   */
  public Response makeBMORequest(String uri, String username, String password, String dbValue,
      String asValue, String meterNumber, XMLGregorianCalendar startTime,
      XMLGregorianCalendar endTime) {
    Client client = new Client(Protocol.HTTP);
    client.setConnectTimeout(2000);

    Reference reference = new Reference(uri);
    reference.addQueryParameter("DB", dbValue);
    reference.addQueryParameter("AS", asValue);
    reference.addQueryParameter("MB", meterNumber);
    reference.addQueryParameter("DOWNLOAD", "YES");
    reference.addQueryParameter("DELIMITER", "TAB");
    reference.addQueryParameter("EXPORTTIMEZONE", "US%2FHawaii");
    reference.addQueryParameter("mnuStartYear", Integer.toString(startTime.getYear()));
    reference.addQueryParameter("mnuStartMonth", Integer.toString(startTime.getMonth()));
    reference.addQueryParameter("mnuStartDay", Integer.toString(startTime.getDay()));
    // mnuStartTime should look like 24-hour:minute, i.e. 0:31 or 10:13 or 23:46.
    // Need to zero prefix single digit minutes so we always have 2 digit minute strings
    int minutes = startTime.getMinute();
    String minuteString =
        (minutes < 10) ? "0" + Integer.toString(minutes) : Integer.toString(minutes);
    // addQueryParameter will do the URL encoding of ":" for us
    reference.addQueryParameter("mnuStartTime", Integer.toString(startTime.getHour()) + ":"
        + minuteString);
    reference.addQueryParameter("mnuEndYear", Integer.toString(endTime.getYear()));
    reference.addQueryParameter("mnuEndMonth", Integer.toString(endTime.getMonth()));
    reference.addQueryParameter("mnuEndDay", Integer.toString(endTime.getDay()));
    // mnuEndTime should look like 24-hour:minute, i.e. 0:31 or 10:13 or 23:46.
    // Need to zero prefix single digit minutes so we always have 2 digit minute strings
    minutes = endTime.getMinute();
    minuteString = (minutes < 10) ? "0" + Integer.toString(minutes) : Integer.toString(minutes);
    // addQueryParameter will do the URL encoding of ":" for us
    reference.addQueryParameter("mnuEndTime", Integer.toString(endTime.getHour()) + ":"
        + minuteString);
    Request request = new Request(Method.GET, reference);
    request.getClientInfo().getAcceptedMediaTypes().add(
        new Preference<MediaType>(MediaType.APPLICATION_ALL));
    ChallengeResponse authentication =
        new ChallengeResponse(ChallengeScheme.HTTP_BASIC, username, password);
    request.setChallengeResponse(authentication);
    return client.handle(request);
  }

  /**
   * Processes command line arguments and starts data input.
   * 
   * @param args command line arguments.
   */
  public static void main(String[] args) {
    Options options = new Options();
    options.addOption("h", "help", false, "print this message");
    options.addOption("p", "propertyFilename", true, "filename of property file");
    options
        .addOption("s", "source", true, "Name of the source to send data to, ex. \"foo-source\"");
    options.addOption("m", "meterNum", true, "meter number to retrieve data from");
    options.addOption("t", "startTimestamp", true, "start point for data collection (if datafile "
        + "doesn't already exist) in XML format (like 2009-07-28T09:00:00.000-10:00)");
    CommandLine cmd = null;
    String propertyFilename = null, sourceName = null, meterNumber = null, startTimestamp = null;

    CommandLineParser parser = new PosixParser();
    try {
      cmd = parser.parse(options, args);
    }
    catch (ParseException e) {
      System.err.println("Command line parsing failed. Reason: " + e.getMessage() + ". Exiting.");
      System.exit(1);
    }

    if (cmd.hasOption("h")) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp(toolName, options);
      System.exit(0);
    }
    if (cmd.hasOption("p")) {
      propertyFilename = cmd.getOptionValue("p");
    }
    else {
      System.err.println("Required propertyFilename parameter not provided, exiting.");
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp(toolName, options);
      System.exit(1);
    }
    if (cmd.hasOption("s")) {
      sourceName = cmd.getOptionValue("s");
    }
    else {
      System.err.println("Required Source name parameter not provided, exiting.");
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp(toolName, options);
      System.exit(1);
    }
    if (cmd.hasOption("m")) {
      meterNumber = cmd.getOptionValue("m");
    }
    else {
      System.err.println("Required meterNum parameter not provided, terminating.");
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp(toolName, options);
      System.exit(1);
    }
    if (cmd.hasOption("t")) {
      startTimestamp = cmd.getOptionValue("t");
    }

    // Results of command line processing, should probably be commented out
    System.out.println("propertyFilename: " + propertyFilename);
    System.out.println("sourceName: " + sourceName);
    System.out.println("meterNumber: " + meterNumber);
    System.out.println("startTimestamp: " + startTimestamp);

    // Actually create the input client
    BMODataInputClient inputClient = null;
    try {
      inputClient =
          new BMODataInputClient(propertyFilename, sourceName, meterNumber, startTimestamp);
    }
    catch (IOException e) {
      System.err.println("Unable to read properties file, terminating.");
      System.exit(2);
    }
    // Just do it
    if ((inputClient != null) && (inputClient.process())) {
      System.out.println("Successfully input data.");
      System.exit(0);
    }
    else {
      System.err.println("Problem encountered inputting data.");
      System.exit(2);
    }
  }
}

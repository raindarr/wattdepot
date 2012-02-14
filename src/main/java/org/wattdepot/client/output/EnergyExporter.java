package org.wattdepot.client.output;

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
import org.wattdepot.client.BadXmlException;
import org.wattdepot.client.MiscClientException;
import org.wattdepot.client.NotAuthorizedException;
import org.wattdepot.client.ResourceNotFoundException;
import org.wattdepot.client.WattDepotClient;
import org.wattdepot.resource.source.jaxb.Source;
import org.wattdepot.util.tstamp.Tstamp;
import au.com.bytecode.opencsv.CSVWriter;

/**
 * Retrieves energy data for a list of sources and produces a CSV file with the results.
 * 
 * @author Robert Brewer
 */
public class EnergyExporter {

  private static final String IO_ERROR_MESSAGE = "Got IO error writing to file: ";

  /** Name of the file to be output. */
  protected String filename;

  /** URI of WattDepot server to retrieve data to. */
  protected String serverUri;

  /** List of source names to collect energy data from. */
  protected String[] sources;

  /** Whether to display debugging data. */
  protected boolean debug;

  /** First day to retrieve energy data for. */
  protected XMLGregorianCalendar startDate;

  /** Last day to retrieve energy data for. */
  protected XMLGregorianCalendar endDate;

  /** Length of the energy interval for each column, in days. */
  protected int intervalDays;

  protected static final int MINUTES_IN_DAY = 60 * 24;

  private static final String toolName = "DailyEnergyExporter";

  /**
   * Creates the new EnergyExporter.
   * 
   * @param filename Name of the file to send data to.
   * @param uri URI of the server to retrieve data from (ending in "/").
   * @param sources List of source names to collect energy data from.
   * @param debug Whether to display debugging data or not.
   * @param startDate First day to retrieve energy data for.
   * @param endDate Last day to retrieve energy data for.
   * @param intervalDays Length of the energy interval for each column, in days.
   */
  public EnergyExporter(String filename, String uri, String[] sources, boolean debug,
      XMLGregorianCalendar startDate, XMLGregorianCalendar endDate, int intervalDays) {
    this.filename = filename;
    this.serverUri = uri;
    // Make Findbugs happy with a clone
    this.sources = sources.clone();
    this.debug = debug;
    this.startDate = startDate;
    this.endDate = endDate;
    this.intervalDays = intervalDays;
  }

  /**
   * Does the work of retrieving the data from WattDepot.
   * 
   * @return True if the data could be successfully input, false otherwise.
   */
  public boolean process() {

    WattDepotClient client = new WattDepotClient(this.serverUri);
    List<Source> sourceList;

    if (!client.isHealthy()) {
      System.err.println("Unable to connect to server: " + this.serverUri);
      return false;
    }

    try {
      sourceList = client.getSources();
    }
    catch (NotAuthorizedException e) {
      System.err.println("Bad credentials (which should never happen for anonymous access).");
      return false;
    }
    catch (BadXmlException e) {
      System.err.println("Server claims it received bad XML (" + e.toString() + ")");
      return false;
    }
    catch (MiscClientException e) {
      System.err.println("Server reported an error (" + e.toString() + ")");
      return false;
    }

    boolean found;
    for (String sourceName : this.sources) {
      found = false;
      for (Source source : sourceList) {
        if (source.getName().equals(sourceName)) {
          found = true;
          break;
        }
      }
      if (!found) {
        // Went through list of Sources, but didn't find desired source name
        System.err.println("Unable to find source \"" + sourceName + "\" on server. Aborting.");
        return false;
      }
    }

    FileWriter writer;
    CSVWriter csvWriter;
    try {
      writer = new FileWriter(filename);
      csvWriter = new CSVWriter(writer, ',');
    }
    catch (IOException e) {
      System.err.println(IO_ERROR_MESSAGE + e.getMessage());
      return false;
    }

    // Since endDate is the last day to get data for, the timestamp list actually should have
    // N+1 entries, with the last entry being equal to endDate + 1 day.
    List<XMLGregorianCalendar> timestampList =
        Tstamp.getTimestampList(startDate, Tstamp.incrementDays(endDate, 1), intervalDays
            * MINUTES_IN_DAY);

    if ((timestampList == null) || (timestampList.isEmpty())) {
      System.err.println("Bad start or end date provided, aborting.");
      return false;
    }

    // There are N pairs of timestamps, with all the interior pairs sharing timestamps, so there
    // are N + 1 timestamps in total. There are N date column headers, plus 1 for the Source column.
    String[] columnHeaders = new String[timestampList.size()];
    int i = 0;
    columnHeaders[i] = "Source";
    // Due to source column header, store timestamps shifted 1, and skip last timestamp (which is
    // an endpoint only).
    for (i = 0; i < columnHeaders.length - 1; i++) {
      XMLGregorianCalendar timestamp = timestampList.get(i);
      columnHeaders[i + 1] =
          String.format("%d/%d/%d", timestamp.getMonth(), timestamp.getDay(), timestamp.getYear());
    }
    if (debug) {
      System.out.println("columnHeaders: " + Arrays.toString(columnHeaders));
    }
    csvWriter.writeNext(columnHeaders);
    try {
      csvWriter.flush();
    }
    catch (IOException e) {
      System.err.println(IO_ERROR_MESSAGE + e.getMessage());
      return false;
    }

    String[] energy = new String[timestampList.size() + 1];

    for (String sourceName : this.sources) {
      energy[0] = sourceName;
      for (i = 1; i < timestampList.size(); i++) {
        // Starting from i = 1, so start timestamp is one behind.
        XMLGregorianCalendar start = timestampList.get(i - 1);
        XMLGregorianCalendar end = timestampList.get(i);
        if (debug) {
          System.out.format("source: %s, start: %s, end: %s%n", sourceName, start.toString(),
              end.toString());
        }
        try {
          energy[i] = Double.toString(client.getEnergyConsumed(sourceName, start, end, 0));
        }
        catch (NotAuthorizedException e) {
          System.err.println("Bad credentials (which should never happen for anonymous access).");
          return false;
        }
        catch (ResourceNotFoundException e) {
          // No energy found for this day, so enter empty string
          energy[i] = "";
        }
        catch (BadXmlException e) {
          // No energy found for this day, so enter empty string
          energy[i] = "";
        }
        catch (MiscClientException e) {
          System.err.println("Server reported an error (" + e.toString() + ")");
          return false;
        }
      }
      csvWriter.writeNext(energy);
      try {
        csvWriter.flush();
      }
      catch (IOException e) {
        System.err.println(IO_ERROR_MESSAGE + e.getMessage());
        return false;
      }
    }

    try {
      csvWriter.close();
    }
    catch (IOException e) {
      System.err.println(IO_ERROR_MESSAGE + e.getMessage());
      return false;
    }

    return true;
  }

  /**
   * Processes command line arguments and starts data output.
   * 
   * @param args command line arguments.
   */
  public static void main(String[] args) {
    Options options = new Options();
    options.addOption("h", "help", false, "print this message");
    options.addOption("f", "file", true, "filename to store data (will be overwritten)");
    options.addOption("w", "uri", true, "URI of WattDepot server to retrieve data from, ex. "
        + "\"http://wattdepot.example.com:1234/wattdepot/");
    options.addOption("s", "sources", true,
        "Name of the sources to get data from, as a comma separated list");
    options.addOption("d", "debug", false, "Turns on debugging output.");
    options.addOption("b", "startDate", true,
        "Start date (inclusive) in XML format (2012-01-01T00:00:00.000-10:00)");
    options.addOption("e", "endDate", true,
        "End date (inclusive) in XML format (2012-01-31T00:00:00.000-10:00)");
    options.addOption("i", "interval", true, "Interval between columns in days, defaults to 1");
    CommandLine cmd = null;

    String filename = null, uri = null, sourceNamesArg = null;
    String[] sources = null;
    boolean debug;
    XMLGregorianCalendar startDate = null, endDate = null;
    int intervalDays = 1;

    CommandLineParser parser = new PosixParser();
    HelpFormatter formatter = new HelpFormatter();
    try {
      cmd = parser.parse(options, args);
    }
    catch (ParseException e) {
      System.err.println("Command line parsing failed. Reason: " + e.getMessage() + ". Exiting.");
      System.exit(1);
    }

    if (cmd.hasOption("h")) {
      formatter.printHelp(toolName, options);
      System.exit(0);
    }
    if (cmd.hasOption("f")) {
      filename = cmd.getOptionValue("f");
    }
    else {
      System.err.println("Required filename parameter not provided, exiting.");
      formatter.printHelp(toolName, options);
      System.exit(1);
    }
    if (cmd.hasOption("w")) {
      uri = cmd.getOptionValue("w");
    }
    else {
      System.err.println("Required URI parameter not provided, exiting.");
      formatter.printHelp(toolName, options);
      System.exit(1);
    }
    if (cmd.hasOption("s")) {
      sourceNamesArg = cmd.getOptionValue("s");
      sources = sourceNamesArg.split("\\s*,\\s*");
    }
    else {
      System.err.println("Required Source name parameter not provided, exiting.");
      formatter.printHelp(toolName, options);
      System.exit(1);
    }
    if (cmd.hasOption("b")) {
      try {
        startDate = Tstamp.makeTimestamp(cmd.getOptionValue("b"));
      }
      catch (Exception e) {
        System.err.println("Unable to parse startDate, exiting.");
        System.exit(1);
      }
    }
    else {
      System.err.println("Required filename parameter not provided, exiting.");
      formatter.printHelp(toolName, options);
      System.exit(1);
    }
    if (cmd.hasOption("e")) {
      try {
        endDate = Tstamp.makeTimestamp(cmd.getOptionValue("e"));
      }
      catch (Exception e) {
        System.err.println("Unable to parse startDate, exiting.");
        System.exit(1);
      }
    }
    else {
      System.err.println("Required filename parameter not provided, exiting.");
      formatter.printHelp(toolName, options);
      System.exit(1);
    }
    if (cmd.hasOption("i")) {
      try {
        intervalDays = Integer.parseInt(cmd.getOptionValue("i"));
      }
      catch (NumberFormatException e) {
        System.err.println("Unable to parse interval, exiting: " + e);
        System.exit(1);
      }
    }

    debug = cmd.hasOption("d");

    if (debug) {
      System.out.println("filename:  " + filename);
      System.out.println("uri:       " + uri);
      System.out.println("sources:   " + Arrays.toString(sources));
      System.out.println("startDate: " + startDate);
      System.out.println("endDate:   " + endDate);
      System.out.println("interval:  " + intervalDays);
      System.out.println("debug:     " + debug);
    }

    EnergyExporter outputClient =
        new EnergyExporter(filename, uri, sources, debug, startDate, endDate, intervalDays);
    // Just do it
    if (outputClient.process()) {
      System.out.println("Successfully output data.");
      System.exit(0);
    }
    else {
      System.err.println("Problem encountered outputting data.");
      System.exit(2);
    }
  }
}

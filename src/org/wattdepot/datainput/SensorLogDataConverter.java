package org.wattdepot.datainput;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;
import java.util.regex.MatchResult;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.XMLGregorianCalendar;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.wattdepot.resource.property.jaxb.Properties;
import org.wattdepot.resource.property.jaxb.Property;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.resource.sensordata.jaxb.SensorDatas;
import org.wattdepot.util.tstamp.Tstamp;

/**
 * Reads a logfile where each line is the output of SensorData.toString() and turns it into a series
 * of files, each containing a SensorData object in XML format, one per row of the input file.
 * 
 * @author Robert Brewer
 */
public class SensorLogDataConverter {

  /** Name of the application on the command line. */
  protected static final String TOOL_NAME = "SensorLogDataConverter";

  /** Name of the file to be input. */
  protected String filename;

  /** Name of the directory where sensordata will be output. */
  protected String directory;

  /** URI of WattDepot server this data will live in. */
  protected String serverUri;

  /** Whether to write output to a single big file or not. */
  protected boolean singleFile;

  // /** Scanner to process a log file entry into SensorData. */
  // protected Scanner logScanner;

  /**
   * Creates the new SensorLogDataConverter.
   * 
   * @param filename Name of the file to read data from.
   * @param directory Name of the directory where sensordata will be output.
   * @param uri URI of the server to send data to (ending in "/").
   * @param singleFile True if output is to be written to a single file.
   */
  public SensorLogDataConverter(String filename, String directory, String uri, boolean singleFile) {
    this.filename = filename;
    this.directory = directory;
    this.serverUri = uri;
    this.singleFile = singleFile;
    // this.format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
  }

  /**
   * Does the work of reading the log data and converting it into SensorData, one row at a time.
   * 
   * @return True if the data could be successfully input, false otherwise.
   * @throws IOException If problems are encountered reading or writing to files.
   */
  public boolean process() throws IOException {
    // JAXB setup
    JAXBContext sensorDataJAXB;
    Marshaller marshaller;
    try {
      sensorDataJAXB =
          JAXBContext.newInstance(org.wattdepot.resource.sensordata.jaxb.ObjectFactory.class);
      marshaller = sensorDataJAXB.createMarshaller();
    }
    catch (JAXBException e) {
      System.err.format("Problem creating JAXBContext: %s%n", e);
      return false;
    }

    // Check that our input file is available
    File inputFile = new File(this.filename), outputDir = new File(this.directory);
    Scanner scanner;
    try {
      scanner = new Scanner(new FileReader(inputFile));
    }
    catch (FileNotFoundException e) {
      System.err.format("Input file %s not found.%n", inputFile.toString());
      return false;
    }

    // Check that output directory is available
    if (!outputDir.isDirectory()) {
      System.err.format("Output directory %s not found.%n", outputDir.toString());
      return false;
    }

    File outputFile;
    int rowsConverted = 0;
    SensorDatas datas = new SensorDatas();

    try {
      while (scanner.hasNextLine()) {
        SensorData data = null;
        try {
          data = parseLogEntry(scanner.nextLine());
        }
        catch (RowParseException e) {
          System.err.println(e);
          // all parse errors are fatal
          return false;
        }
        if (data != null) {
          if (singleFile) {
            // Just append to the list, which will be written out later
            datas.getSensorData().add(data);
          }
          else {
            // Create a new output file in the output directory named by source name and timestamp
            String sourceUri = data.getSource();
            String sourceName = sourceUri.substring(sourceUri.lastIndexOf('/') + 1);
            outputFile = new File(outputDir, sourceName + "_" + data.getTimestamp().toString());
            try {
              marshaller.marshal(data, outputFile);
            }
            catch (JAXBException e) {
              System.err.format("Problem creating writing output file: %s %s%n", outputFile, e);
              return false;
            }
          }
          rowsConverted++;
        }
      }
    }
    finally {
      scanner.close();
    }

    // If we are in singleFile mode and there is output data
    if (singleFile && datas.isSetSensorData()) {
      outputFile = new File(outputDir, this.filename + ".xml");
      System.out.format("Writing out %d sensordata entries in one big file...%n", rowsConverted);
      try {
        marshaller.marshal(datas, outputFile);
      }
      catch (JAXBException e) {
        System.err.format("Problem creating writing output file: %s %s%n", outputFile, e);
        return false;
      }
    }
    System.out.format("Converted %d rows of input data.%n", rowsConverted);
    return true;
  }

  /**
   * Takes a single line of log data in SensorData.toString() format and returns a SensorData object
   * with equivalent data. If the line cannot be parsed for some reason, a RowParseException is
   * thrown. Note that rather than supporting arbitrary properties, we assume that the logs being
   * input have only powerConsumed and energyConsumedToDate properties in that order, which is
   * fairly bogus.
   * 
   * @param input The line of log data.
   * @return the new SensorData object.
   * @throws RowParseException If the log data cannot be parsed into a SensorData object.
   */
  public SensorData parseLogEntry(String input) throws RowParseException {
    // An example logfile entry (wrapped for legibility):
    // SensorData [properties=[Property [key=powerConsumed, value=704.98486328125],
    // Property [key=energyConsumedToDate, value=9649930.0]],
    // source=http://server.wattdepot.org:8190/wattdepot/sources/Ilima-10-lounge,
    // timestamp=2012-01-04T16:16:49.329-10:00, tool=Shark200SSensor]

    // "SensorData [properties=" + properties + ", source=" + source + ", timestamp="
    // + timestamp + ", tool=" + tool + "]";

    Scanner logScanner = new Scanner(input);
    logScanner.findInLine("SensorData \\[properties=\\[Property \\[key=(\\w+), value=(.+)\\], "
        + "Property \\[key=(\\w+), value=(.+)\\]\\], source=(.+), timestamp=(.+), "
        + "tool=(\\w+)\\]");
    MatchResult result = logScanner.match();
    if (result.groupCount() == 7) {
      Property powerGenerated, energyConsumedToDate;

      if (result.group(1).equals(SensorData.POWER_CONSUMED)) {
        powerGenerated = new Property(SensorData.POWER_CONSUMED, result.group(2));
      }
      else {
        throw new RowParseException("Unable to parse log entry, expected "
            + SensorData.POWER_CONSUMED + ": " + input);
      }
      if (result.group(3).equals(SensorData.ENERGY_CONSUMED_TO_DATE)) {
        energyConsumedToDate = new Property(SensorData.ENERGY_CONSUMED_TO_DATE, result.group(4));
      }
      else {
        throw new RowParseException("Unable to parse log entry, expected "
            + SensorData.ENERGY_CONSUMED_TO_DATE + ": " + input);
      }

      Properties props = new Properties();
      props.getProperty().add(powerGenerated);
      props.getProperty().add(energyConsumedToDate);

      String sourceName = result.group(5);
      String dateString = result.group(6);
      String toolName = result.group(7);

      XMLGregorianCalendar timestamp;
      try {
        timestamp = Tstamp.makeTimestamp(dateString);
      }
      catch (Exception e) {
        throw new RowParseException("Bad timestamp found in input file: " + dateString, e);
      }

      return new SensorData(timestamp, toolName, sourceName, props);
    }
    else {
      throw new RowParseException("Unable to parse log entry: " + input);
    }
  }

  /**
   * Processes command line arguments and starts data input.
   * 
   * @param args command line arguments.
   * @throws IOException If there are problems reading the input or output files.
   */
  public static void main(String[] args) throws IOException {
    Options options = new Options();
    options.addOption("h", "help", false, "print this message");
    options.addOption("f", "file", true, "filename of tabular data");
    options.addOption("d", "directory", true, "directory where sensordata files will be written");
    options.addOption("w", "uri", true,
        "URI of WattDepot server, ex. \"http://wattdepot.example.com:1234/wattdepot/\"."
            + " Note that this parameter is used only to populate the SensorData object "
            + "Source field, no network connection will be made.");
    options.addOption("s", "singleFile", false, "output will be written to one big file");
    CommandLine cmd = null;
    String filename = null, directory = null, uri = null;
    boolean singleFile = false;

    CommandLineParser parser = new PosixParser();
    try {
      cmd = parser.parse(options, args);
    }
    catch (ParseException e) {
      System.err.println("Command line parsing failed. Reason: " + e.getMessage() + ". Exiting.");
      System.exit(1);
    }

    HelpFormatter formatter = new HelpFormatter();
    if (cmd.hasOption("h")) {
      formatter.printHelp(TOOL_NAME, options);
      System.exit(0);
    }
    if (cmd.hasOption("s")) {
      singleFile = true;
    }

    // required options
    if (cmd.hasOption("f")) {
      filename = cmd.getOptionValue("f");
    }
    else {
      System.err.println("Required filename parameter not provided, exiting.");
      formatter.printHelp(TOOL_NAME, options);
      System.exit(1);
    }
    if (cmd.hasOption("d")) {
      directory = cmd.getOptionValue("d");
    }
    else {
      System.err.println("Required directory parameter not provided, exiting.");
      formatter.printHelp(TOOL_NAME, options);
      System.exit(1);
    }
    if (cmd.hasOption("w")) {
      uri = cmd.getOptionValue("w");
    }
    else {
      System.err.println("Required URI parameter not provided, exiting.");
      formatter.printHelp(TOOL_NAME, options);
      System.exit(1);
    }

    // Results of command line processing, should probably be commented out
    System.out.println("filename:   " + filename);
    System.out.println("directory:  " + directory);
    System.out.println("uri:        " + uri);
    System.out.println("singleFile: " + singleFile);
    // Actually create the input client
    SensorLogDataConverter inputClient =
        new SensorLogDataConverter(filename, directory, uri, singleFile);
    // Just do it
    if (inputClient.process()) {
      System.out.println("Successfully converted data.");
      System.exit(0);
    }
    else {
      System.err.println("Problem encountered converting data.");
      System.exit(2);
    }
  }
}

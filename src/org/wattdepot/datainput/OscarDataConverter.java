package org.wattdepot.datainput;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
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
import org.hackystat.utilities.tstamp.Tstamp;
import org.wattdepot.resource.sensordata.SensorDataUtils;
import org.wattdepot.resource.sensordata.jaxb.Properties;
import org.wattdepot.resource.sensordata.jaxb.Property;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.server.Server;
import au.com.bytecode.opencsv.CSVReader;

/**
 * Reads the table file produced by <a href="http://code.google.com/p/oscar-project/">OSCAR</a> and
 * turns it into a series of files, each containing a SensorData object in XML format, one per row
 * of the input file.
 * 
 * @author Robert Brewer
 */
public class OscarDataConverter {

  /** Name of the application on the command line. */
  protected static final String toolName = "OscarDataConverter";

  /** Name of the file to be input. */
  protected String filename;

  /** Name of the directory where sensordata will be output. */
  protected String directory;

  /** URI of WattDepot server this data will live in. */
  protected String serverUri;

  /** Used to parse dates from the input data, kept in field to reduce object creation. */
  protected SimpleDateFormat format;

  /**
   * Creates the new OscarDataConverter.
   * 
   * @param filename Name of the file to read data from.
   * @param directory Name of the directory where sensordata will be output.
   * @param uri URI of the server to send data to (ending in "/").
   */
  public OscarDataConverter(String filename, String directory, String uri) {
    this.filename = filename;
    this.directory = directory;
    this.serverUri = uri;
    this.format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
  }

  /**
   * Does the work of reading the Oscar data and converting it into SensorData, one row at a time.
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
    CSVReader reader;
    try {
      // use 4 arg constructor with skip lines = 1 to skip the column header
      reader = new CSVReader(new FileReader(filename), ',', CSVReader.DEFAULT_QUOTE_CHARACTER, 1);
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

    String[] nextLine;
    File outputFile;
    int rowsConverted = 0;
    while ((nextLine = reader.readNext()) != null) {
      // nextLine[] is an array of values from the current row in the file
      SensorData data = parseRow(nextLine);
      if (data != null) {
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
        rowsConverted++;
      }
    }
    reader.close();
    System.out.format("Converted %d rows of input data.%n", rowsConverted);
    return true;
  }

  /**
   * Converts a row of the table into an appropriate SensorData object.
   * 
   * @param col The row of the table, with each column represented as a String array element.
   * @return The new SensorData object.
   */
  public SensorData parseRow(String[] col) {
    String dateString = col[0];
    Date newDate = null;
    try {
      newDate = this.format.parse(dateString);
    }
    catch (java.text.ParseException e) {
      System.err.format("Bad timestamp found in input file: %s%n", dateString);
      return null;
    }
    XMLGregorianCalendar timestamp = Tstamp.makeTimestamp(newDate.getTime());

    String sourceName = col[1];
    // Create the properties based on the PropertyDictionary
    // http://code.google.com/p/wattdepot/wiki/PropertyDictionary
    String powerGeneratedString = col[2];
    double powerGenerated;
    try {
      // Value in file is a String representing an integer value in MW, while powerGenerated
      // SensorData property is defined in dictionary as being in W, so parse and multiply by 10^6.
      powerGenerated = Integer.parseInt(powerGeneratedString) * 1000 * 1000;
    }
    catch (NumberFormatException e) {
      System.err.println("Unable to parse power generated: " + powerGeneratedString);
      return null;
    }
    Property prop1 =
        SensorDataUtils.makeSensorDataProperty("powerGenerated", Double.toString(powerGenerated));

    Properties props = new Properties();
    props.getProperty().add(prop1);
    return SensorDataUtils.makeSensorData(timestamp, toolName, this.serverUri + Server.SOURCES_URI
        + "/" + sourceName, props);
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
    CommandLine cmd = null;
    String filename = null, directory = null, uri = null;

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
    if (cmd.hasOption("d")) {
      directory = cmd.getOptionValue("d");
    }
    else {
      System.err.println("Required directory parameter not provided, exiting.");
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

    // Results of command line processing, should probably be commented out
    System.out.println("filename: " + filename);
    System.out.println("directory: " + directory);
    System.out.println("uri:      " + uri);
    // Actually create the input client
    OscarDataConverter inputClient = new OscarDataConverter(filename, directory, uri);
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

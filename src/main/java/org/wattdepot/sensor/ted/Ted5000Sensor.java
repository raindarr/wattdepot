package org.wattdepot.sensor.ted;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.w3c.dom.Document;
import org.wattdepot.datainput.SensorSource;
import org.wattdepot.datainput.SensorSource.METER_TYPE;
import org.wattdepot.resource.property.jaxb.Property;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.sensor.MultiThreadedSensor;
import org.wattdepot.util.tstamp.Tstamp;
import org.xml.sax.SAXException;

/**
 * Polls a TED 5000 home energy monitor for power data periodically, and sends the results to a
 * WattDepot server. For more information about the XML that the TED generates, see
 * http://code.google.com/p/wattdepot/wiki/Ted5000XmlExplanation
 * 
 * @author Robert Brewer
 */
public class Ted5000Sensor extends MultiThreadedSensor {

  /** Name of this tool. */
  private static final String toolName = "Ted5000Sensor";

  /**
   * Initializes an Ted5000 sensor by calling the constructor for MultiThreadedSensor.
   * 
   * @param wattDepotUri URI of the WattDepot server to send this sensor data to.
   * @param wattDepotUsername Username to connect to the WattDepot server with.
   * @param wattDepotPassword Password to connect to the WattDepot server with.
   * @param sensorSource The SensorSource containing configuration settings for this sensor.
   * @param debug If true then display new sensor data when sending it.
   */
  public Ted5000Sensor(String wattDepotUri, String wattDepotUsername, String wattDepotPassword,
      SensorSource sensorSource, boolean debug) {
    super(wattDepotUri, wattDepotUsername, wattDepotPassword, sensorSource, debug);
  }

  @Override
  public void run() {

    SensorData data = null;
    // Get data from TED
    try {
      data = pollTed(this.meterHostname, toolName, this.sourceUri);
    }
    catch (XPathExpressionException e) {
      System.err.println("Bad XPath expression, this should never happen.");
    }
    catch (ParserConfigurationException e) {
      System.err.println("Unable to configure XML parser, this is weird.");
    }
    catch (SAXException e) {
      System.err.format("%s: Got bad XML from TED meter (%s), hopefully this is temporary.%n",
          Tstamp.makeTimestamp(), e);
    }
    catch (IOException e) {
      System.err.format(
          "%s: Unable to retrieve data from TED (%s), hopefully this is temporary.%n",
          Tstamp.makeTimestamp(), e);
    }
    // Store SensorData in WattDepot server
    try {
      client.storeSensorData(data);
    }
    catch (Exception e) {
      System.err.format(
          "%s: Unable to store sensor data due to exception (%s), hopefully this is temporary.%n",
          Tstamp.makeTimestamp(), e);
    }
    if (debug) {
      System.out.println(data);
    }

  }

  /**
   * Connects to a TED 5000 meter gateway, retrieves the latest data, and returns it as a SensorData
   * object.
   * 
   * @param hostname The hostname of the TED 5000. Only the hostname, not URI.
   * @param toolName The name of the tool to be placed in the SensorData.
   * @param sourceURI The URI of the source (needed to create SensorData object).
   * @return The meter data as SensorData
   * @throws ParserConfigurationException If there are problems creating a parser.
   * @throws IOException If there are problems retrieving the data via HTTP.
   * @throws SAXException If there are problems parsing the XML from the meter.
   * @throws XPathExpressionException If the hardcoded XPath expression is bogus.
   * @throws IllegalArgumentException If the provided hostname is invalid.
   * 
   */
  private SensorData pollTed(String hostname, String toolName, String sourceURI)
      throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
    String tedURI = "http://" + hostname + "/api/LiveData.xml";

    DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
    domFactory.setNamespaceAware(true);
    DocumentBuilder builder = domFactory.newDocumentBuilder();

    // Have to make HTTP connection manually so we can set proper timeouts
    URL url;
    try {
      url = new URL(tedURI);
    }
    catch (MalformedURLException e) {
      throw new IllegalArgumentException("Hostname was invalid leading to malformed URL", e);
    }
    URLConnection httpConnection;
    httpConnection = url.openConnection();
    // Set both connect and read timeouts to 15 seconds. No point in long timeouts since the
    // sensor will retry before too long anyway.
    httpConnection.setConnectTimeout(15 * 1000);
    httpConnection.setReadTimeout(15 * 1000);
    httpConnection.connect();

    // Record current time as close approximation to time for reading we are about to make
    XMLGregorianCalendar timestamp = Tstamp.makeTimestamp();

    Document doc = builder.parse(httpConnection.getInputStream());

    XPathFactory factory = XPathFactory.newInstance();
    XPath powerXpath = factory.newXPath();
    XPath energyXpath = factory.newXPath();
    // Path to get the current power consumed measured by the meter in watts
    XPathExpression exprPower = powerXpath.compile("/LiveData/Power/Total/PowerNow/text()");
    // Path to get the energy consumed month to date in watt hours
    XPathExpression exprEnergy = energyXpath.compile("/LiveData/Power/Total/PowerMTD/text()");
    Object powerResult = exprPower.evaluate(doc, XPathConstants.NUMBER);
    Object energyResult = exprEnergy.evaluate(doc, XPathConstants.NUMBER);

    Double currentPower = (Double) powerResult;
    Double mtdEnergy = (Double) energyResult;

    SensorData data = new SensorData(timestamp, toolName, sourceURI);
    data.addProperty(new Property(SensorData.POWER_CONSUMED, currentPower));
    data.addProperty(new Property(SensorData.ENERGY_CONSUMED_TO_DATE, mtdEnergy));
    return data;
  }

  /**
   * Processes command line arguments and uses MultiThreadedSensor to start polling all Ted5000
   * Sensors listed in the property file.
   * 
   * @param args command line arguments.
   * @throws InterruptedException If some other thread interrupts our sleep.
   */
  public static void main(String[] args) throws InterruptedException {
    Options options = new Options();
    options.addOption("h", "help", false, "Print this message");
    options.addOption("p", "propertyFilename", true, "Filename of property file");
    options.addOption("d", "debug", false, "Displays sensor data as it is sent to the server.");

    CommandLine cmd = null;
    String propertyFilename = null;
    boolean debug = false;

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
    if (cmd.hasOption("p")) {
      propertyFilename = cmd.getOptionValue("p");
    }
    else {
      System.out.println("No property file name provided, using default.");
    }
    debug = cmd.hasOption("d");

    if (debug) {
      System.out.println("propertyFilename: " + propertyFilename);
      System.out.println("debug: " + debug);
      System.out.println();
    }

    if (!MultiThreadedSensor.start(propertyFilename, debug, METER_TYPE.TED5000)) {
      System.exit(1);
    }
  }

}

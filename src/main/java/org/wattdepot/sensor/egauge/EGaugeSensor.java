package org.wattdepot.sensor.egauge;

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
import org.wattdepot.client.WattDepotClient;
import org.wattdepot.datainput.SensorSource;
import org.wattdepot.datainput.SensorSource.METER_TYPE;
import org.wattdepot.resource.property.jaxb.Property;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.resource.source.jaxb.Source;
import org.wattdepot.sensor.MultiThreadedSensor;
import org.wattdepot.util.tstamp.Tstamp;
import org.xml.sax.SAXException;

/**
 * Polls an eGauge meter for energy and power periodically, and sends the results to a WattDepot
 * server. For more information about eGauge meters, see:
 * http://www.egauge.net/docs/egauge-xml-api.pdf
 * 
 * @author Andrea Connell
 * 
 */
public class EGaugeSensor extends MultiThreadedSensor {

  /** Name of this tool. */
  private static final String toolName = "eGaugeSensor";

  /**
   * Initializes an eGauge sensor by calling the constructor for MultiThreadedSensor.
   * 
   * @param wattDepotUri Uri of the WattDepot server to send this sensor data to.
   * @param wattDepotUsername Username to connect to the WattDepot server with.
   * @param wattDepotPassword Password to connect to the WattDepot server with.
   * @param sensorSource The SensorSource containing configuration settings for this sensor.
   * @param debug If true then display new sensor data when sending it.
   */
  public EGaugeSensor(String wattDepotUri, String wattDepotUsername, String wattDepotPassword,
      SensorSource sensorSource, boolean debug) {
    super(wattDepotUri, wattDepotUsername, wattDepotPassword, sensorSource, debug);
  }

  @Override
  public void run() {
    SensorData data = null;

    WattDepotClient client =
        new WattDepotClient(wattDepotUri, wattDepotUsername, wattDepotPassword);

    if (this.updateRate <= UPDATE_RATE_SENTINEL) {
      // Need to pick a reasonable default pollingInterval
      // Check the polling rate specified in the source
      Source source = getSourceFromClient(client);
      if (source == null) {
        this.cancel();
        return;
      }
      String updateIntervalString = source.getProperty(Source.UPDATE_INTERVAL);
      if (updateIntervalString == null) {
        // no update interval, so just use hard coded default
        updateRate = DEFAULT_UPDATE_RATE;
      }
      else {
        int possibleInterval;
        try {
          possibleInterval = Integer.valueOf(updateIntervalString);
          if (possibleInterval > DEFAULT_UPDATE_RATE) {
            // Sane interval, so use it
            updateRate = possibleInterval;
          }
          else {
            // Bogus interval, so use hard coded default
            updateRate = DEFAULT_UPDATE_RATE;
          }
        }
        catch (NumberFormatException e) {
          System.err.format("Unable to parse updateInterval for %s, using default value: %d%n",
              this.sourceKey, DEFAULT_UPDATE_RATE);
          // Bogus interval, so use hard coded default
          updateRate = DEFAULT_UPDATE_RATE;
        }
      }
    }

    String sourceUri = Source.sourceToUri(this.sourceName, wattDepotUri);

    String eGaugeUri = "http://" + meterHostname + "/cgi-bin/egauge?tot";

    DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
    domFactory.setNamespaceAware(true);
    try {
      DocumentBuilder builder = domFactory.newDocumentBuilder();

      // Have to make HTTP connection manually so we can set proper timeouts
      URL url = new URL(eGaugeUri);
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
      XPathExpression exprPower =
          powerXpath.compile("//meter[@type='total' and @title='Student Usage V']/power/text()");
      // Path to get the energy consumed month to date in watt hours
      XPathExpression exprEnergy =
          energyXpath.compile("//meter[@type='total' and @title='Student Usage V']/energy/text()");
      Object powerResult = exprPower.evaluate(doc, XPathConstants.NUMBER);
      Object energyResult = exprEnergy.evaluate(doc, XPathConstants.NUMBER);

      // power is given in W
      Double currentPower = (Double) powerResult;
      // energy is given in kWh
      Double energyToDate = ((Double) energyResult) * 1000;
      data = new SensorData(timestamp, toolName, sourceUri);
      if (currentPower <= 0) {
        data.addProperty(new Property(SensorData.POWER_CONSUMED, currentPower * -1));
      }
      else {
        data.addProperty(new Property(SensorData.POWER_GENERATED, currentPower));
      }
      if (energyToDate <= 0) {
        data.addProperty(new Property(SensorData.ENERGY_CONSUMED_TO_DATE, energyToDate * -1));
      }
      else {
        data.addProperty(new Property(SensorData.ENERGY_GENERATED_TO_DATE, energyToDate));
      }
    }
    catch (MalformedURLException e) {
      System.err.format("Hostname %s for %s was invalid leading to malformed URL%n",
          meterHostname, sourceKey);
    }
    catch (XPathExpressionException e) {
      System.err.println("Bad XPath expression, this should never happen.");
    }
    catch (ParserConfigurationException e) {
      System.err.println("Unable to configure XML parser, this is weird.");
    }
    catch (SAXException e) {
      System.err.format(
          "%s: Got bad XML from TED meter for %s (%s), hopefully this is temporary.%n",
          Tstamp.makeTimestamp(), sourceKey, e);
    }
    catch (IOException e) {
      System.err.format(
          "%s: Unable to retrieve data from TED for %s (%s), hopefully this is temporary.%n",
          Tstamp.makeTimestamp(), sourceKey, e);
    }

    try {
      if (data != null) {
        client.storeSensorData(data);
      }
    }
    catch (Exception e) {
      System.err
          .format(
              "%s: Unable to store sensor data from %s due to exception (%s), hopefully this is temporary.%n",
              Tstamp.makeTimestamp(), this.sourceKey, e);
    }
  }

  /**
   * Processes command line arguments and uses MultiThreadedSensor to start polling all eGauge
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

    if (!MultiThreadedSensor.start(propertyFilename, METER_TYPE.EGAUGE)) {
      System.exit(1);
    }
  }

}

package org.wattdepot.sensor.hammer;

import javax.xml.datatype.XMLGregorianCalendar;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.wattdepot.datainput.SensorSource;
import org.wattdepot.datainput.SensorSource.METER_TYPE;
import org.wattdepot.resource.property.jaxb.Property;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.sensor.MultiThreadedSensor;
import org.wattdepot.util.tstamp.Tstamp;

/**
 * This sensor "hammers" a server in order to stress test it. It is based on the SharkSensor.
 * 
 * @author Robert Brewer, Andrea Connell
 */
public class HammerSensor extends MultiThreadedSensor {

  /** Name of this tool. */
  private static final String toolName = "HammerSensor";
  /** Stores the fake wattHours value that will be incremented each time it is sent to the server. */
  private double wattHours = 0;

  /**
   * Initializes a HammerSensor by calling the constructor for MultiThreadedSensor.
   * 
   * @param wattDepotUri URI of the WattDepot server to send this sensor data to.
   * @param wattDepotUsername Username to connect to the WattDepot server with.
   * @param wattDepotPassword Password to connect to the WattDepot server with.
   * @param sensorSource The SensorSource containing configuration settings for this sensor.
   * @param debug If true then display new sensor data when sending it.
   */
  public HammerSensor(String wattDepotUri, String wattDepotUsername, String wattDepotPassword,
      SensorSource sensorSource, boolean debug) {
    super(wattDepotUri, wattDepotUsername, wattDepotPassword, sensorSource, debug);
  }

  /**
   * Generates fake SensorData and sends it to a Source in a WattDepot server.
   */
  @Override
  public void run() {
    SensorData data = generateFakeSensorData();
    if (data != null) {
      try {
        this.client.storeSensorData(data);
      }
      catch (Exception e) {
        System.err
            .format(
                "%s: Unable to store sensor data from %s due to exception (%s), hopefully this is temporary.%n",
                Tstamp.makeTimestamp(), this.sourceKey, e);
      }
      if (debug) {
        System.out.println(data);
      }
    }
  }

  /**
   * Generates fake sensor data with current time, powerConsumed set to the milliseconds of current
   * time, and energyConsumedToDate incremented by 1.
   * 
   * @return The new fake SensorData.
   */
  public SensorData generateFakeSensorData() {
    XMLGregorianCalendar timestamp = Tstamp.makeTimestamp();
    // Use the number of milliseconds in timestamp as current number of watts consumed
    int watts = timestamp.getMillisecond();
    this.wattHours += 1.0;
    SensorData fakeData = new SensorData(timestamp, toolName, this.sourceUri);
    fakeData.addProperty(new Property(SensorData.POWER_CONSUMED, watts));
    fakeData.addProperty(new Property(SensorData.ENERGY_CONSUMED_TO_DATE, this.wattHours));
    return fakeData;
  }

  /**
   * Processes command line arguments and uses MultiThreadedSensor to start polling all Shark
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

    if (!MultiThreadedSensor.start(propertyFilename, debug, METER_TYPE.HAMMER)) {
      System.exit(1);
    }
  }
}

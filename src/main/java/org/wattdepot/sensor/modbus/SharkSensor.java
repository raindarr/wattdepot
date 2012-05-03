package org.wattdepot.sensor.modbus;

import static org.wattdepot.datainput.DataInputClientProperties.WATTDEPOT_PASSWORD_KEY;
import static org.wattdepot.datainput.DataInputClientProperties.WATTDEPOT_URI_KEY;
import static org.wattdepot.datainput.DataInputClientProperties.WATTDEPOT_USERNAME_KEY;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import javax.xml.datatype.XMLGregorianCalendar;
import net.wimpi.modbus.Modbus;
import net.wimpi.modbus.io.ModbusTCPTransaction;
import net.wimpi.modbus.msg.ExceptionResponse;
import net.wimpi.modbus.msg.ModbusResponse;
import net.wimpi.modbus.msg.ReadMultipleRegistersRequest;
import net.wimpi.modbus.msg.ReadMultipleRegistersResponse;
import net.wimpi.modbus.net.TCPMasterConnection;
import net.wimpi.modbus.util.ModbusUtil;
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
import org.wattdepot.datainput.DataInputClientProperties;
import org.wattdepot.datainput.SensorSource;
import org.wattdepot.resource.property.jaxb.Property;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.resource.source.jaxb.Source;
import org.wattdepot.util.tstamp.Tstamp;

/**
 * Polls a ElectroIndustries Shark power meter for power & energy data periodically, and sends the
 * results to a WattDepot server. For more information about Shark meters, see:
 * http://www.electroind.com/pdf/Shark_New/E149721_Shark_200-S_Meter_Manual_V.1.03.pdf
 * 
 * Inspiration for this sensor came from the work on the WattDepot Modbus sensor:
 * http://code.google.com/p/wattdepot-sensor-modbus/
 * 
 * @author Robert Brewer
 */
public class SharkSensor extends TimerTask {

  protected String wattDepotUri;
  protected String wattDepotUsername;
  protected String wattDepotPassword;
  /** The source key, which identifies the source in the config file. */
  protected String sourceKey;
  /** The name of the source to send data to. */
  private String sourceName;
  /** The rate at which to poll the device for new data. */
  private int updateRate;
  /** Whether to display debugging data. */
  private boolean debug;
  /** The hostname of the Shark meter to be monitored. */
  private String meterHostname;

  // Note that the Modbus register map in the Shark manual appears to start at 1, while jamod
  // expects it to start at 0, so you must subtract 1 from the register index listed in the manual!
  /** Register index for "Power & Energy Format". */
  private static final int ENERGY_FORMAT_REGISTER = 30006 - 1;
  /** Number of words (registers) that make up "Power & Energy Format". */
  private static final int ENERGY_FORMAT_LENGTH = 1;
  /** Register index for "W-hours, Total". */
  private static final int ENERGY_REGISTER = 1506 - 1;
  /** Number of words (registers) that make up "W-hours, Total". */
  private static final int ENERGY_LENGTH = 2;
  /** Register index for "Watts, 3-Ph total". */
  private static final int POWER_REGISTER = 1018 - 1;
  /** Number of words (registers) that make up "Watts, 3-Ph total". */
  private static final int POWER_LENGTH = 2;

  /** Name of this tool. */
  private static final String toolName = "Shark200SSensor";
  /** The default polling rate, in seconds. */
  private static final int DEFAULT_UPDATE_RATE = 10;
  /** The polling rate that indicates that it needs to be set to a default. */
  private static final int UPDATE_RATE_SENTINEL = 0;

  /** Making PMD happy. */
  private static final String REQUIRED_SOURCE_PARAMETER_ERROR_MSG =
      "Required parameter %s not found in properties for %s.%n";
  private static final String REQUIRED_PARAMETER_ERROR_MSG =
      "Required parameter %s not found in properties.%n";

  /**
   * Creates the new streaming sensor.
   * 
   * @param wattDepotUri Uri of the WattDepot server to send this sensor data to.
   * @param wattDepotUsername Username to connect to the WattDepot server with.
   * @param wattDepotPassword Password to connect to the WattDepot server with.
   * @param sensorSource The SensorSource containing configuration settings for this sensor.
   * @param debug If true then display new sensor data when sending it.
   */
  public SharkSensor(String wattDepotUri, String wattDepotUsername,
      String wattDepotPassword, SensorSource sensorSource, boolean debug) {

    this.wattDepotUri = wattDepotUri;
    this.wattDepotUsername = wattDepotUsername;
    this.wattDepotPassword = wattDepotPassword;
    this.sourceKey = sensorSource.getKey();
    this.sourceName = sensorSource.getName();
    this.meterHostname = sensorSource.getMeterHostname();
    this.updateRate = sensorSource.getUpdateRate();
    this.debug = debug;
  }

  /**
   * Checks that all required parameters are set, that the WattDepotClient can connect, that the
   * source exists, and that the meterHostname is valid.
   * 
   * @return true if everything is good to go.
   */
  protected boolean isValid() {
    if (wattDepotUri == null) {
      System.err.format(REQUIRED_PARAMETER_ERROR_MSG, WATTDEPOT_URI_KEY);
      return false;
    }

    if (wattDepotUsername == null) {
      System.err.format(REQUIRED_PARAMETER_ERROR_MSG, WATTDEPOT_USERNAME_KEY);
      return false;
    }
    if (wattDepotPassword == null) {
      System.err.format(REQUIRED_PARAMETER_ERROR_MSG, WATTDEPOT_PASSWORD_KEY);
      return false;
    }
    if (sourceName == null) {
      System.err.format(REQUIRED_SOURCE_PARAMETER_ERROR_MSG, "Source name", sourceKey);
      return false;
    }
    if (meterHostname == null) {
      System.err.format(REQUIRED_SOURCE_PARAMETER_ERROR_MSG, "Meter hostname", sourceKey);
      return false;
    }

    WattDepotClient client =
        new WattDepotClient(wattDepotUri, wattDepotUsername, wattDepotPassword);
    Source source = getSourceFromClient(client);
    if (source == null) {
      return false;
    }

    if (this.updateRate <= UPDATE_RATE_SENTINEL) {
      // Need to pick a reasonable default pollingInterval
      // Check the polling rate specified in the source
      int possibleInterval = source.getPropertyAsInt(Source.UPDATE_INTERVAL);

      if (possibleInterval > DEFAULT_UPDATE_RATE) {
        // Sane interval, so use it
        this.updateRate = possibleInterval;
      }
      else {
        // Bogus interval, so use hard coded default
        this.updateRate = DEFAULT_UPDATE_RATE;
      }
    }

    if (getMeterAddress() == null) {
      return false;
    }
    return true;
  }

  /**
   * Given a WattDepotClient, ensure authentication has succeeded and that the desired source is
   * accessible.
   * 
   * @param client The client to attempt to retrieve the source from.
   * @return The source object with the source name provided in the constructor.
   */
  private Source getSourceFromClient(WattDepotClient client) {
    if (!client.isHealthy()) {
      System.err.format("Could not connect to server %s. Aborting.%n", this.wattDepotUri);
      return null;
    }
    if (!client.isAuthenticated()) {
      System.err.format("Invalid credentials for server %s. Aborting.%n", this.wattDepotUri);
      return null;
    }

    try {
      return client.getSource(sourceName);
    }
    catch (NotAuthorizedException e) {
      System.err.format("You do not have access to the source %s. Aborting.%n", sourceName);
    }
    catch (ResourceNotFoundException e) {
      System.err.format("Source %s does not exist on server. Aborting.%n", sourceName);
    }
    catch (BadXmlException e) {
      System.err.println("Received bad XML from server, which is weird. Aborting.");
    }
    catch (MiscClientException e) {
      System.err.println("Had problems retrieving source from server, which is weird. Aborting.");
    }
    return null;
  }

  /**
   * Returns the InetAddress of the meterHostname given in the constructor, or null if it cannot be
   * resolved.
   * 
   * @return The InetAddress of the meterHostname given in the constructor, or null if it cannot be
   * resolved.
   */
  private InetAddress getMeterAddress() {
    try {
      return InetAddress.getByName(meterHostname);
    }
    catch (UnknownHostException e) {
      System.err.format("Unable to resolve meter hostname %s, aborting.%n", meterHostname);
      return null;
    }
  }

  /**
   * Retrieves meter sensor data and sends it to a Source in a WattDepot server.
   */
  public void run() {
    WattDepotClient client =
        new WattDepotClient(wattDepotUri, wattDepotUsername, wattDepotPassword);
    Source source = getSourceFromClient(client);
    if (source == null) {
      this.cancel();
      return;
    }

    // Resolve provided meter hostname
    InetAddress meterAddress = getMeterAddress();
    if (meterAddress == null) {
      this.cancel();
      return;
    }

    if (this.updateRate <= UPDATE_RATE_SENTINEL) {
      // Need to pick a reasonable default pollingInterval
      // Check the polling rate specified in the source
      String updateIntervalString = source.getProperty(Source.UPDATE_INTERVAL);
      if (updateIntervalString == null) {
        // no update interval, so just use hard coded default
        this.updateRate = DEFAULT_UPDATE_RATE;
      }
      else {
        int possibleInterval;
        try {
          possibleInterval = Integer.valueOf(updateIntervalString);
          if (possibleInterval > DEFAULT_UPDATE_RATE) {
            // Sane interval, so use it
            this.updateRate = possibleInterval;
          }
          else {
            // Bogus interval, so use hard coded default
            this.updateRate = DEFAULT_UPDATE_RATE;
          }
        }
        catch (NumberFormatException e) {
          System.err.println("Unable to parse updateInterval, using default value: "
              + DEFAULT_UPDATE_RATE);
          // Bogus interval, so use hard coded default
          this.updateRate = DEFAULT_UPDATE_RATE;
        }
      }
    }

    // Need to retrieve some configuration parameters from meter, but only want to do it once
    // per session.
    ModbusResponse response;
    ReadMultipleRegistersResponse goodResponse = null;
    try {
      response = readRegisters(meterAddress, ENERGY_FORMAT_REGISTER, ENERGY_FORMAT_LENGTH);
    }
    catch (Exception e) {
      System.err.format("Unable to retrieve energy format parameters from meter: %s, aborting.%n",
          e.getMessage());
      this.cancel();
      return;
    }
    if (response instanceof ReadMultipleRegistersResponse) {
      goodResponse = (ReadMultipleRegistersResponse) response;
    }
    else if (response instanceof ExceptionResponse) {
      System.err
          .println("Got Modbus exception response while retrieving energy format parameters from meter, code: "
              + ((ExceptionResponse) response).getExceptionCode());
      this.cancel();
    }
    else {
      System.err
          .println("Got strange Modbus reply while retrieving energy format parameters from meter, aborting.");
      this.cancel();
      return;
    }

    double energyMultiplier = decodeEnergyMultiplier(goodResponse);
    if (energyMultiplier == 0) {
      System.err.println("Got bad energy multiplier from meter energy format, aborting.");
      this.cancel();
      return;
    }

    int energyDecimals = decodeEnergyDecimals(goodResponse);
    if (energyDecimals == 0) {
      System.err.println("Got bad energy decimal format from meter energy format, aborting.");
      this.cancel();
      return;
    }

    // Get data from meter
    String sourceURI = Source.sourceToUri(this.sourceName, wattDepotUri);
    SensorData data =
        pollMeter(meterAddress, toolName, sourceURI, energyMultiplier, energyDecimals);
    if (data != null) {
      try {
        client.storeSensorData(data);
      }
      catch (Exception e) {
        System.err
            .format(
                "%s: Unable to store sensor data due to exception (%s), hopefully this is temporary.%n",
                Tstamp.makeTimestamp(), e);
      }
      if (debug) {
        System.out.println(data);
      }
    }
  }

  /**
   * Reads one or more consecutive registers from a device using Modbus/TCP using the default TCP
   * port (502).
   * 
   * @param address The IP address of the device to be read from
   * @param register The Modbus register to be read, as a decimal integer
   * @param length The number of registers to read
   * @return A ReadMultipleRegistersResponse containing the response from the device, or null if
   * there was some unexpected error.
   * @throws Exception If an error occurs while attempting to read the registers.
   */
  private ModbusResponse readRegisters(InetAddress address, int register, int length)
      throws Exception {
    return readRegisters(address, Modbus.DEFAULT_PORT, register, length);
  }

  /**
   * Reads one or more consecutive registers from a device using Modbus/TCP.
   * 
   * @param address The IP address of the device to be read from
   * @param port The destination TCP port to connect to
   * @param register The Modbus register to be read, as a decimal integer
   * @param length The number of registers to read
   * @return A ReadMultipleRegistersResponse containing the response from the device, or null if
   * there was some unexpected error.
   * @throws Exception If an error occurs while attempting to read the registers.
   */
  private ModbusResponse readRegisters(InetAddress address, int port, int register, int length)
      throws Exception {
    TCPMasterConnection connection = null;
    ModbusTCPTransaction transaction = null;
    ReadMultipleRegistersRequest request = null;

    // Open the connection
    connection = new TCPMasterConnection(address);
    connection.setPort(port);
    connection.connect();

    // Prepare the request
    request = new ReadMultipleRegistersRequest(register, length);

    // Prepare the transaction
    transaction = new ModbusTCPTransaction(connection);
    transaction.setRequest(request);
    transaction.execute();
    ModbusResponse response = transaction.getResponse();

    // Close the connection
    connection.close();

    return response;
  }

  /**
   * Decodes the energy multiplier configured on the meter, which really means whether the energy
   * value returned is in Wh, kWh, or MWh.
   * 
   * @param response The response from the meter containing the power and energy format.
   * @return A double that represents the scale which energy readings should be multiplied by, or 0
   * if there was some problem decoding the value.
   */
  private double decodeEnergyMultiplier(ReadMultipleRegistersResponse response) {
    if ((response != null) && (response.getWordCount() == 1)) {
      // From Shark manual, bitmap looks like this ("-" is unused bit apparently):
      // ppppiinn feee-ddd
      //
      // pppp = power scale (0-unit, 3-kilo, 6-mega, 8-auto)
      // ii = power digits after decimal point (0-3),
      // applies only if f=1 and pppp is not auto
      // nn = number of energy digits (5-8 --> 0-3)
      // eee = energy scale (0-unit, 3-kilo, 6-mega)
      // f = decimal point for power
      // (0=data-dependant placement, 1=fixed placement per ii value)
      // ddd = energy digits after decimal point (0-6)

      // Get energy scale by shifting off 4 bits and then mask with 111
      int energyScale = (response.getRegisterValue(0) >>> 4) & 7;
      switch (energyScale) {
      case 0:
        // watts
        return 1.0;
      case 3:
        // kilowatts
        return 1000.0;
      case 6:
        // megawatts
        return 1000000.0;
      default:
        // should never happen, according to manual, so return 0
        // System.err.println("Unknown energy scale from meter, defaulting to kWh");
        return 0.0;
      }
    }
    else {
      return 0.0;
    }
  }

  /**
   * Decodes the configured number of energy digits after the decimal point. For example, if the
   * retrieved energy value is "12345678" and the decimals value is 2, then the actual energy value
   * is "123456.78".
   * 
   * @param response The response from the meter containing the power and energy format.
   * @return An int that represents the number of ending digits from the energy reading that should
   * be considered as decimals.
   */
  private int decodeEnergyDecimals(ReadMultipleRegistersResponse response) {
    if ((response != null) && (response.getWordCount() == 1)) {
      // From Shark manual, bitmap looks like this ("-" is unused bit apparently):
      // ppppiinn feee-ddd
      //
      // pppp = power scale (0-unit, 3-kilo, 6-mega, 8-auto)
      // ii = power digits after decimal point (0-3),
      // applies only if f=1 and pppp is not auto
      // nn = number of energy digits (5-8 --> 0-3)
      // eee = energy scale (0-unit, 3-kilo, 6-mega)
      // f = decimal point for power
      // (0=data-dependant placement, 1=fixed placement per ii value)
      // ddd = energy digits after decimal point (0-6)

      // Get # of energy digits after decimal point by masking with 111
      return response.getRegisterValue(0) & 7;
    }
    else {
      return 0;
    }
  }

  /**
   * Given a response with two consecutive registers, extract the values as a 4 byte array so they
   * can be passed to methods in ModbusUtil. It seems like there should be a better way to do this.
   * 
   * @param response The response containing the two registers
   * @return a byte[4] array or null if there is a problem with the response.
   */
  private byte[] extractByteArray(ReadMultipleRegistersResponse response) {
    byte[] regBytes = new byte[4];
    if (response.getWordCount() == 2) {
      regBytes[0] = response.getRegister(0).toBytes()[0];
      regBytes[1] = response.getRegister(0).toBytes()[1];
      regBytes[2] = response.getRegister(1).toBytes()[0];
      regBytes[3] = response.getRegister(1).toBytes()[1];
      return regBytes;
    }
    else {
      return null;
    }
  }

  /**
   * Connects to meter, retrieves the latest data, and returns it as a SensorData object.
   * 
   * @param meterAddress The address of the meter.
   * @param toolName The name of the tool to be placed in the SensorData.
   * @param sourceURI The URI of the source (needed to create SensorData object).
   * @param energyMultiplier The amount to multiply energy readings by to account for unit of
   * measurement.
   * @param energyDecimals The configured number of decimals included in the energy reading.
   * @return The meter data as SensorData
   */
  private SensorData pollMeter(InetAddress meterAddress, String toolName, String sourceURI,
      double energyMultiplier, int energyDecimals) {
    return pollMeter(meterAddress, Modbus.DEFAULT_PORT, toolName, sourceURI, energyMultiplier,
        energyDecimals);
  }

  /**
   * Connects to meter, retrieves the latest data, and returns it as a SensorData object.
   * 
   * @param meterAddress The address of the meter.
   * @param port The destination TCP port to connect to
   * @param toolName The name of the tool to be placed in the SensorData.
   * @param sourceURI The URI of the source (needed to create SensorData object).
   * @param energyMultiplier The amount to multiply energy readings by to account for unit of
   * measurement.
   * @param energyDecimals The configured number of decimals included in the energy reading.
   * @return The meter data as SensorData
   */
  private SensorData pollMeter(InetAddress meterAddress, int port, String toolName,
      String sourceURI, double energyMultiplier, int energyDecimals) {

    // Record current time as close approximation to time for reading we are about to make
    XMLGregorianCalendar timestamp = Tstamp.makeTimestamp();
    ModbusResponse energyResponse, powerResponse;
    ReadMultipleRegistersResponse goodResponse = null;
    try {
      // Make both queries in rapid succession to reduce any lag on sensor side
      energyResponse = readRegisters(meterAddress, port, ENERGY_REGISTER, ENERGY_LENGTH);
      powerResponse = readRegisters(meterAddress, port, POWER_REGISTER, POWER_LENGTH);
    }
    catch (Exception e) {
      System.err.format("%s Unable to retrieve energy data from meter: %s.%n", new Date(),
          e.getMessage());
      return null;
    }

    // First handle energy response
    if (energyResponse instanceof ReadMultipleRegistersResponse) {
      goodResponse = (ReadMultipleRegistersResponse) energyResponse;
    }
    else if (energyResponse instanceof ExceptionResponse) {
      System.err
          .println("Got Modbus exception response while retrieving energy data from meter, code: "
              + ((ExceptionResponse) energyResponse).getExceptionCode());
      return null;
    }
    else {
      System.err.println("Got strange Modbus reply while retrieving energy data from meter.");
      return null;
    }
    int wattHoursInt = ModbusUtil.registersToInt(extractByteArray(goodResponse));
    // Take integer value, divide by 10^energyDecimals to move decimal point to right place,
    // then multiply by a value depending on units (nothing, kilo, or mega).
    double wattHours = (wattHoursInt / (Math.pow(10.0, energyDecimals))) * energyMultiplier;

    // Then handle power response
    if (powerResponse instanceof ReadMultipleRegistersResponse) {
      goodResponse = (ReadMultipleRegistersResponse) powerResponse;
    }
    else if (powerResponse instanceof ExceptionResponse) {
      System.err
          .println("Got Modbus exception response while retrieving power data from meter, code: "
              + ((ExceptionResponse) powerResponse).getExceptionCode());
      return null;
    }
    else {
      System.err.println("Got strange Modbus reply while retrieving power data from meter.");
      return null;
    }
    float watts = ModbusUtil.registersToFloat(extractByteArray(goodResponse));

    SensorData data = new SensorData(timestamp, toolName, sourceURI);
    data.addProperty(new Property(SensorData.POWER_CONSUMED, watts));
    data.addProperty(new Property(SensorData.ENERGY_CONSUMED_TO_DATE, wattHours));
    return data;
  }

  /**
   * Processes command line arguments, creates the SharkSensor object and starts sending.
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

    DataInputClientProperties properties = null;
    try {
      properties = new DataInputClientProperties(propertyFilename);
    }
    catch (IOException e) {
      System.err.println("Unable to read properties file " + propertyFilename + ", terminating.");
      System.exit(2);
    }

    String wattDepotUri = properties.get(WATTDEPOT_URI_KEY);
    String wattDepotUsername = properties.get(WATTDEPOT_USERNAME_KEY);
    String wattDepotPassword = properties.get(WATTDEPOT_PASSWORD_KEY);
    Collection<SensorSource> sources = properties.getSources().values();

    boolean running = false;
    for (SensorSource s : sources) {
      // Create a separate Timer for each source. Otherwise they interfere with each other and the
      // timing becomes unreliable.
      Timer t = new Timer();
      SharkSensorThreaded sensor =
          new SharkSensorThreaded(wattDepotUri, wattDepotUsername, wattDepotPassword, s, debug);
      if (sensor.isValid()) {
        System.out.format("Started polling %s meter at %s%n", s.getKey(), Tstamp.makeTimestamp());
        t.scheduleAtFixedRate(sensor, 0, sensor.updateRate * 1000);
        running = true;
      }
      else {
        System.err.format("Cannot poll %s meter%n", s.getKey());
      }
    }
    if (!running) {
      System.err.println("\nNo meters are being polled.");
      System.exit(2);
    }
  }
}

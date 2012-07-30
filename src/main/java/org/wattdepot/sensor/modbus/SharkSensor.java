package org.wattdepot.sensor.modbus;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
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
import org.wattdepot.datainput.SensorSource;
import org.wattdepot.datainput.SensorSource.METER_TYPE;
import org.wattdepot.resource.property.jaxb.Property;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.sensor.MultiThreadedSensor;
import org.wattdepot.util.tstamp.Tstamp;

/**
 * Polls a ElectroIndustries Shark power meter for power & energy data periodically, and sends the
 * results to a WattDepot server. For more information about Shark meters, see:
 * http://www.electroind.com/pdf/Shark_New/E149721_Shark_200-S_Meter_Manual_V.1.03.pdf
 * 
 * Inspiration for this sensor came from the work on the WattDepot Modbus sensor:
 * http://code.google.com/p/wattdepot-sensor-modbus/
 * 
 * @author Robert Brewer, Andrea Connell
 */
public class SharkSensor extends MultiThreadedSensor {

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
  /** InetAddress of the meter to be polled. */
  private InetAddress meterAddress;
  /** Name of this tool. */
  private static final String toolName = "Shark200SSensor";

  /**
   * Stores the energy multiplier configured on the meter, which really means whether the energy
   * value returned is in Wh, kWh, or MWh.
   */
  private double energyMultiplier = 0;
  /**
   * Stores the configured number of energy digits after the decimal point. For example, if the
   * retrieved energy value is "12345678" and the decimals value is 2, then the actual energy value
   * is "123456.78".
   */
  private int energyDecimals = 0;
  /**
   * Whether all required parameters (such as energy format parameters from the meter) have been
   * successfully retrieved.
   */
  private boolean initialized = false;

  /**
   * Initializes a shark sensor by calling the constructor for MultiThreadedSensor.
   * 
   * @param wattDepotUri URI of the WattDepot server to send this sensor data to.
   * @param wattDepotUsername Username to connect to the WattDepot server with.
   * @param wattDepotPassword Password to connect to the WattDepot server with.
   * @param sensorSource The SensorSource containing configuration settings for this sensor.
   * @param debug If true then display new sensor data when sending it.
   */
  public SharkSensor(String wattDepotUri, String wattDepotUsername, String wattDepotPassword,
      SensorSource sensorSource, boolean debug) {
    super(wattDepotUri, wattDepotUsername, wattDepotPassword, sensorSource, debug);
    this.meterAddress = getMeterAddress();
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
   * Need to retrieve some configuration parameters from meter, but only want to do it once per
   * session. Note that all failures to retrieve the parameters are considered temporary for
   * caution's sake, so a misconfigured meter might continue to be polled indefinitely until the
   * problem is resolved.
   * 
   * @return True if configuration parameters could be retrieved from meter, false otherwise.
   */
  private boolean initialize() {
    ModbusResponse response;
    ReadMultipleRegistersResponse goodResponse = null;
    try {
      response = readRegisters(this.meterAddress, ENERGY_FORMAT_REGISTER, ENERGY_FORMAT_LENGTH);
    }
    catch (Exception e) {
      System.err.format(
          "Unable to retrieve energy format parameters from meter %s: %s, retrying.%n",
          this.sourceKey, e.getMessage());
      return false;
    }
    if (response instanceof ReadMultipleRegistersResponse) {
      goodResponse = (ReadMultipleRegistersResponse) response;
    }
    else if (response instanceof ExceptionResponse) {
      System.err
          .format(
              "Got Modbus exception response while retrieving energy format parameters from meter %s, code: %s%n",
              this.sourceKey, ((ExceptionResponse) response).getExceptionCode());
      return false;
    }
    else {
      System.err
          .format(
              "Got strange Modbus reply while retrieving energy format parameters from meter %s, aborting.%n",
              this.sourceKey);
      return false;
    }

    double energyMultiplier = decodeEnergyMultiplier(goodResponse);
    if (energyMultiplier == 0) {
      System.err.format("Got bad energy multiplier from meter %s energy format, aborting.%n",
          this.sourceKey);
      return false;
    }

    int energyDecimals = decodeEnergyDecimals(goodResponse);
    if (energyDecimals == 0) {
      System.err.format("Got bad energy decimal format from meter %s energy format, aborting.%n",
          this.sourceKey);
      return false;
    }
    // Able to retrieve all the energy format parameters, so set the class fields for future use
    this.energyMultiplier = energyMultiplier;
    this.energyDecimals = energyDecimals;
    return true;

  }

  /**
   * Retrieves meter sensor data and sends it to a Source in a WattDepot server.
   */
  @Override
  public void run() {
    if (!this.initialized) {
      this.initialized = initialize();
    }
    if (initialized) {
      SensorData data =
          pollMeter(this.meterAddress, toolName, this.sourceUri, this.energyMultiplier,
              this.energyDecimals);
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
      else {
        // No data from meter
        System.err.format("%s: Unable to get sensor data from meter.%n", Tstamp.makeTimestamp());
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
      System.err.format("%s Unable to retrieve energy data from meter %s: %s.%n", new Date(),
          this.sourceKey, e.getMessage());
      return null;
    }

    // First handle energy response
    if (energyResponse instanceof ReadMultipleRegistersResponse) {
      goodResponse = (ReadMultipleRegistersResponse) energyResponse;
    }
    else if (energyResponse instanceof ExceptionResponse) {
      System.err.format(
          "Got Modbus exception response while retrieving energy data from meter %s, code: %s%n",
          this.sourceKey, ((ExceptionResponse) energyResponse).getExceptionCode());
      return null;
    }
    else {
      System.err.format("Got strange Modbus reply while retrieving energy data from meter %s.%n",
          this.sourceKey);
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
      System.err.format(
          "Got Modbus exception response while retrieving power data from meter %s, code: %s%n",
          this.sourceKey, ((ExceptionResponse) powerResponse).getExceptionCode());
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

    if (!MultiThreadedSensor.start(propertyFilename, debug, METER_TYPE.MODBUS)) {
      System.exit(1);
    }
  }
}

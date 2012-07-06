package org.wattdepot.sensor;

import static org.wattdepot.datainput.DataInputClientProperties.WATTDEPOT_PASSWORD_KEY;
import static org.wattdepot.datainput.DataInputClientProperties.WATTDEPOT_URI_KEY;
import static org.wattdepot.datainput.DataInputClientProperties.WATTDEPOT_USERNAME_KEY;
import java.io.IOException;
import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;
import org.wattdepot.client.BadXmlException;
import org.wattdepot.client.MiscClientException;
import org.wattdepot.client.NotAuthorizedException;
import org.wattdepot.client.ResourceNotFoundException;
import org.wattdepot.client.WattDepotClient;
import org.wattdepot.datainput.DataInputClientProperties;
import org.wattdepot.datainput.SensorSource;
import org.wattdepot.datainput.SensorSource.METER_TYPE;
import org.wattdepot.resource.source.jaxb.Source;
import org.wattdepot.sensor.egauge.EGaugeSensor;
import org.wattdepot.sensor.hammer.HammerSensor;
import org.wattdepot.sensor.modbus.SharkSensor;
import org.wattdepot.sensor.ted.Ted5000Sensor;
import org.wattdepot.util.tstamp.Tstamp;

/**
 * 
 * Common functionality for all Multi-Threaded Sensors.
 * 
 * @author Andrea Connell
 * 
 */
public abstract class MultiThreadedSensor extends TimerTask {

  protected String wattDepotUri;
  protected String wattDepotUsername;
  protected String wattDepotPassword;
  /** The source key, which identifies the source in the config file. */
  protected String sourceKey;
  /** The name of the source to send data to. */
  protected String sourceName;
  /** The rate at which to poll the device for new data. */
  protected int updateRate;
  /** Whether to display debugging data. */
  protected boolean debug;
  /** The hostname of the Shark meter to be monitored. */
  protected String meterHostname;

  /** The default polling rate, in seconds. */
  protected static final int DEFAULT_UPDATE_RATE = 10;
  /** The polling rate that indicates that it needs to be set to a default. */
  protected static final int UPDATE_RATE_SENTINEL = 0;

  /** Making PMD happy. */
  protected static final String REQUIRED_SOURCE_PARAMETER_ERROR_MSG =
      "Required parameter %s not found in properties for %s.%n";
  protected static final String REQUIRED_PARAMETER_ERROR_MSG =
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
  public MultiThreadedSensor(String wattDepotUri, String wattDepotUsername,
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
  public boolean isValid() {
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
      try {
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
      catch (NumberFormatException e) {
        System.err.format("Unable to parse updateInterval for %s, using default value: %d%n",
            this.sourceKey, DEFAULT_UPDATE_RATE);
        // Bogus interval, so use hard coded default
        this.updateRate = DEFAULT_UPDATE_RATE;
      }
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
  protected Source getSourceFromClient(WattDepotClient client) {
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
   * Return the update rate in seconds.
   * 
   * @return The update rate.
   */
  public int getUpdateRate() {
    return this.updateRate;
  }

  /**
   * Reads the data input property file and starts polling the sensors configured there. If
   * meterType is not null, only meters matching the given type will be started.
   * 
   * @param propertyFilename The file to read data input properties from. If null, the default
   * property file is used.
   * @param debug Debug flag passed to specific sensor type.
   * @param meterType When set, only SensorSources with this meter type will be started.
   * @return True if all sensors have been started successfully, or false if one or more sensors
   * could not be started.
   */
  public static boolean start(String propertyFilename, boolean debug, METER_TYPE meterType) {

    DataInputClientProperties properties = null;
    try {
      properties = new DataInputClientProperties(propertyFilename);
    }
    catch (IOException e) {
      System.err.println("Unable to read properties file " + propertyFilename + ".");
      return false;
    }

    String wattDepotUri = properties.get(WATTDEPOT_URI_KEY);
    String wattDepotUsername = properties.get(WATTDEPOT_USERNAME_KEY);
    String wattDepotPassword = properties.get(WATTDEPOT_PASSWORD_KEY);
    Collection<SensorSource> sources = properties.getSources().values();

    for (SensorSource s : sources) {
      // Create a separate Timer for each source. Otherwise they interfere with each other and the
      // timing becomes unreliable.
      Timer t = new Timer();
      METER_TYPE type = s.getMeterType();
      if (type == null) {
        System.err.format("No meter type specified for %s%n", s.getKey());
        return false;
      }
      if (SensorSource.METER_TYPE.MODBUS.equals(type)
          && (meterType == null || type.equals(meterType))) {
        SharkSensor sensor =
            new SharkSensor(wattDepotUri, wattDepotUsername, wattDepotPassword, s, debug);
        if (sensor.isValid()) {
          System.out.format("Started polling %s meter at %s%n", s.getKey(), Tstamp.makeTimestamp());
          t.scheduleAtFixedRate(sensor, 0, sensor.getUpdateRate() * 1000);
        }
        else {
          System.err.format("Cannot poll %s meter%n", s.getKey());
          return false;
        }
      }
      else if (SensorSource.METER_TYPE.EGAUGE.equals(type)
          && (meterType == null || type.equals(meterType))) {
        EGaugeSensor sensor =
            new EGaugeSensor(wattDepotUri, wattDepotUsername, wattDepotPassword, s, debug);
        if (sensor.isValid()) {
          System.out.format("Started polling %s meter at %s%n", s.getKey(), Tstamp.makeTimestamp());
          t.scheduleAtFixedRate(sensor, 0, sensor.getUpdateRate() * 1000);
        }
        else {
          System.err.format("Cannot poll %s meter%n", s.getKey());
          return false;
        }
      }
      else if (SensorSource.METER_TYPE.TED5000.equals(type)
          && (meterType == null || type.equals(meterType))) {
        Ted5000Sensor sensor =
            new Ted5000Sensor(wattDepotUri, wattDepotUsername, wattDepotPassword, s, debug);
        if (sensor.isValid()) {
          System.out.format("Started polling %s meter at %s%n", s.getKey(), Tstamp.makeTimestamp());
          t.scheduleAtFixedRate(sensor, 0, sensor.getUpdateRate() * 1000);
        }
        else {
          System.err.format("Cannot poll %s meter%n", s.getKey());
          return false;
        }
      }
      else if (SensorSource.METER_TYPE.HAMMER.equals(type)
          && (meterType == null || type.equals(meterType))) {
        HammerSensor sensor =
            new HammerSensor(wattDepotUri, wattDepotUsername, wattDepotPassword, s, debug);
        if (sensor.isValid()) {
          System.out.format("Started polling %s meter at %s%n", s.getKey(), Tstamp.makeTimestamp());
          t.scheduleAtFixedRate(sensor, 0, sensor.getUpdateRate() * 1000);
        }
        else {
          System.err.format("Cannot poll %s meter%n", s.getKey());
          return false;
        }
      }
      else {
        System.err.format("Unsupported sensor type %s%nCannot poll %s meter%n", s.getMeterType(),
            s.getKey());
        return false;
      }
    }
    return true;
  }
}

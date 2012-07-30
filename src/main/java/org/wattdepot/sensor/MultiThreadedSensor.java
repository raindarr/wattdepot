package org.wattdepot.sensor;

import static org.wattdepot.datainput.DataInputClientProperties.WATTDEPOT_PASSWORD_KEY;
import static org.wattdepot.datainput.DataInputClientProperties.WATTDEPOT_URI_KEY;
import static org.wattdepot.datainput.DataInputClientProperties.WATTDEPOT_USERNAME_KEY;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
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
 * Common functionality for all Multi-Threaded Sensors.
 * 
 * @author Andrea Connell
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
  /** The hostname of the meter to be monitored. */
  protected String meterHostname;

  /** The client used to communicate with the WattDepot server. */
  protected WattDepotClient client;
  /** URI of Source where data will be stored. Needed to create SensorData object. */
  protected String sourceUri;

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
   * @param wattDepotUri URI of the WattDepot server to send this sensor data to.
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
    this.client = new WattDepotClient(wattDepotUri, wattDepotUsername, wattDepotPassword);
    this.sourceKey = sensorSource.getKey();
    this.sourceName = sensorSource.getName();
    this.meterHostname = sensorSource.getMeterHostname();
    this.updateRate = sensorSource.getUpdateRate();
    this.debug = debug;
    this.sourceUri = Source.sourceToUri(this.sourceName, this.wattDepotUri);
  }

  /**
   * Checks that all required parameters are set, that the source exists, determines the update rate
   * for the sensor, and checks that the meterHostname is valid (either a resolvable domain name or
   * an IP address in a valid format).
   * 
   * @return true if everything is good to go.
   */
  public boolean isValid() {
    if (sourceName == null) {
      System.err.format(REQUIRED_SOURCE_PARAMETER_ERROR_MSG, "Source name", sourceKey);
      return false;
    }
    if ((this.meterHostname == null) || (this.meterHostname.length() == 0)) {
      System.err.format(REQUIRED_SOURCE_PARAMETER_ERROR_MSG, "Meter hostname", sourceKey);
      return false;
    }

    Source source = null;
    try {
      source = this.client.getSource(sourceName);
    }
    catch (NotAuthorizedException e) {
      System.err.format("You do not have access to the source %s. Aborting.%n", sourceName);
      return false;
    }
    catch (ResourceNotFoundException e) {
      System.err.format("Source %s does not exist on server. Aborting.%n", sourceName);
      return false;
    }
    catch (BadXmlException e) {
      System.err.println("Received bad XML from server, which is weird. Aborting.");
      return false;
    }
    catch (MiscClientException e) {
      System.err.format("Had problems retrieving source %s from server, which is weird. Aborting.",
          sourceName);
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

    // Check validity of meterHostname
    try {
      InetAddress.getByName(this.meterHostname);
    }
    catch (UnknownHostException e) {
      System.err.format("Unable to resolve meter hostname %s, aborting.%n", meterHostname);
      return false;
    }

    return true;
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
   * @throws InterruptedException If sleep is interrupted for some reason.
   */
  public static boolean start(String propertyFilename, boolean debug, METER_TYPE meterType)
      throws InterruptedException {

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

    // Check that server parameters exist
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

    // Before starting any sensors, confirm that we can connect to the WattDepot server. We do
    // this here because if the server and sensors are running on the same system, at boot time the
    // sensor might start before the server, causing client calls to fail. If this happens, we
    // want to catch it at the top level, where it will result in the sensor process terminating.
    // If we wait to catch it at the per-sensor level, it might cause a sensor to abort for what
    // might be a short-lived problem. The sensor process should be managed by some other process
    // (such as launchd), so it is OK to terminate because it should get restarted if the server
    // isn't up quite yet.
    WattDepotClient staticClient =
        new WattDepotClient(wattDepotUri, wattDepotUsername, wattDepotPassword);
    if (!staticClient.isHealthy()) {
      System.err.format("Could not connect to server %s. Aborting.%n", wattDepotUri);
      // Pause briefly to rate limit restarts if server doesn't come up for a long time
      Thread.sleep(2000);
      return false;
    }
    if (!staticClient.isAuthenticated()) {
      System.err.format("Invalid credentials for server %s. Aborting.%n", wattDepotUri);
      return false;
    }

    // Record whether any meters have been able to be configured
    boolean aSensorPolling = false;
    
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
          t.schedule(sensor, 0, sensor.getUpdateRate() * 1000);
          aSensorPolling = true;
        }
        else {
          System.err.format("Cannot poll %s meter%n", s.getKey());
        }
      }
      else if (SensorSource.METER_TYPE.EGAUGE.equals(type)
          && (meterType == null || type.equals(meterType))) {
        EGaugeSensor sensor =
            new EGaugeSensor(wattDepotUri, wattDepotUsername, wattDepotPassword, s, debug);
        if (sensor.isValid()) {
          System.out.format("Started polling %s meter at %s%n", s.getKey(), Tstamp.makeTimestamp());
          t.schedule(sensor, 0, sensor.getUpdateRate() * 1000);
          aSensorPolling = true;
        }
        else {
          System.err.format("Cannot poll %s meter%n", s.getKey());
        }
      }
      else if (SensorSource.METER_TYPE.TED5000.equals(type)
          && (meterType == null || type.equals(meterType))) {
        Ted5000Sensor sensor =
            new Ted5000Sensor(wattDepotUri, wattDepotUsername, wattDepotPassword, s, debug);
        if (sensor.isValid()) {
          System.out.format("Started polling %s meter at %s%n", s.getKey(), Tstamp.makeTimestamp());
          t.schedule(sensor, 0, sensor.getUpdateRate() * 1000);
          aSensorPolling = true;
        }
        else {
          System.err.format("Cannot poll %s meter%n", s.getKey());
        }
      }
      else if (SensorSource.METER_TYPE.HAMMER.equals(type)
          && (meterType == null || type.equals(meterType))) {
        HammerSensor sensor =
            new HammerSensor(wattDepotUri, wattDepotUsername, wattDepotPassword, s, debug);
        if (sensor.isValid()) {
          System.out.format("Started polling %s meter at %s%n", s.getKey(), Tstamp.makeTimestamp());
          t.schedule(sensor, 0, sensor.getUpdateRate() * 1000);
          aSensorPolling = true;
        }
        else {
          System.err.format("Cannot poll %s meter%n", s.getKey());
        }
      }
      else {
        System.err.format("Unsupported sensor type %s%nCannot poll %s meter%n", s.getMeterType(),
            s.getKey());
        return false;
      }
      // Prevent thundering herd by sleeping for 1.1 seconds between sensor instantiations
      // but skip sleep for HammerSensor
      if (!SensorSource.METER_TYPE.HAMMER.equals(type)) {
        Thread.sleep(1100);
      }
    }
    // Return true if at least 1 sensor is polling, or false otherwise.
    return aSensorPolling;
  }
}



# 1.0 Introduction #

This page explains how to write a WattDepot sensor. We define a _sensor_ as a WattDepot client designed to input data into a WattDepot server. This page assumes that you already understand basic WattDepot concepts discussed in the [REST API](RestApi.md) page and the [explanation of how to write a WattDepot client](WritingWattDepotClients.md).

## 1.1 Meter Sensors ##

A sensor will generally obtain its data in one of two ways: polling or being pushed. A polling sensor will connect somehow to the sensing device (which we will call a _meter_) periodically and retrieve data from the meter. The other way to obtain the meter data is for the meter to push the data to the sensor process whenever it has new data. For example, a meter might make an HTTP POST to a particular sensor URL.

Once the meter data has been obtained, the sensor will then convert it into WattDepot sensor data, and store it in a WattDepot server using the PUT method of the RestApi.

The [BMOSensor class](http://code.google.com/p/wattdepot/source/browse/trunk/src/org/wattdepot/datainput/BMOSensor.java) included in the WattDepot distribution is an example of a polling sensor. Note that the BMO sensor is somewhat complicated because it maintains a flat-file representation of all the data it retrieves in addition to sending it to WattDepot. This is historical, it was implemented before WattDepot had persistent storage.

## 1.2 Data Converters ##

In some cases, you might already have power data in some sort of tabular format already, and simply wish to import it into WattDepot. The WattDepot distribution includes the [Oscar data converter class](http://code.google.com/p/wattdepot/source/browse/trunk/src/org/wattdepot/datainput/OscarDataConverter.java), which converts the output data format from the [OSCAR simulator](http://code.google.com/p/oscar-project/) and converts it into XML designed for import into WattDepot through the default resource mechanism. The core of the class is the implementation of the abstract RowParser class that turns a row of input data into a SensorData resource.

# 2.0 Writing A Sensor #

## 2.1 Creating a user and source(s) ##

While data from public sources can be retrieved anonymously, storing data in WattDepot can only be done after proper authentication. This means that a user must have been created on the WattDepot server, and you must know the username and password to the account.

In addition, sensor data must be stored in a source, so at least one source must be created on the server. The owner of the source must be the user we discussed in the previous paragraph. Your sensor will need to obtain the username, password, and source name where the sensor data will be stored either by prompting the user for the data or reading it from a configuration file.

## 2.2 Coding a single-threaded sensor in Java ##

Since WattDepot uses a [RESTful API](RestApi.md), sensors can be written in any language. This section will explain how to write a sensor using the Java client library provided with WattDepot.

Since the meter interface portion of the sensor will be specific to that meter, this example code reads the meter data from a Java Property file and stores the data we find there in the server. When writing your sensor you can replace the property file reading with your meter-specific code.

```
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import javax.xml.datatype.XMLGregorianCalendar;
import org.wattdepot.client.WattDepotClient;
import org.wattdepot.resource.property.jaxb.Property;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.resource.source.jaxb.Source;
import org.wattdepot.util.tstamp.Tstamp;

public class SimpleSensor {

  /**
   * Reads data from a file in property format and stores it in the given WattDepot server.
   * 
   * @param args The command line arguments.
   * @throws IOException If there are problems closing the properties file.
   */
  public static void main(String[] args) throws IOException {
    // Assign command line args to variables
    String serverUri = args[0], username = args[1], password = args[2], sourceName = args[3], fileName =
        args[4];
    Properties properties = new Properties();
    String sourceUri = Source.sourceToUri(sourceName, serverUri);

    // Load properties file
    FileInputStream stream = null;
    try {
      stream = new FileInputStream(fileName);
      properties.load(stream);
    }
    catch (IOException e) {
      System.out.println(fileName + " not found.");
      return;
    }
    finally {
      if (stream != null) {
        stream.close();
      }
    }

    // Create SensorData object
    SensorData data;
    try {
      XMLGregorianCalendar timestamp = Tstamp.makeTimestamp(properties.getProperty("timestamp"));
      Property powerConsumed =
          new Property(SensorData.POWER_CONSUMED, properties.getProperty("powerConsumed"));
      data = new SensorData(timestamp, "SimpleSensor", sourceUri, powerConsumed);
    }
    catch (Exception e) {
      System.out.println("Unable to convert timestamp.");
      return;
    }

    // Store SensorData in WattDepot server
    WattDepotClient client = new WattDepotClient(serverUri, username, password);
    try {
      client.storeSensorData(data);
    }
    catch (Exception e) {
      System.out.println("Unable to store sensor data.");
      return;
    }
    System.out.println("Sensor data successfully stored.");
  }
}
```

The code is fairly straightforward. First we assign command line arguments to variables. The arguments are:

  * server URI
  * username
  * password
  * source name
  * property file name

Note that it is poor security practice to read passwords from command line arguments, since command line arguments can be observed by other processes. Reading passwords from files is also problematic (even with proper permissions on the file). The security requirements of your sensor are up to you, but password management is something to think carefully about. For simplicity's sake, the example just reads the password from the command line.

The next section just loads the property file from disk. This is the functionality that needs to be replaced by your meter-specific code. In this case the property file only needs a "timestamp" and a "powerConsumed" property, something like this:

```
timestamp = 2010-02-13T22:04:00.000-10:00
powerConsumed = 1000.0
```

The third section creates the new SensorData object. The timestamp (WattDepot uses XMLGregorianCalendar for timestamps, which is an implementation of the XML [dateTime](http://www.w3.org/TR/2001/REC-xmlschema-2-20010502/#dateTime) data type) is created from the property String using a static method of the Tstamp class. Next we create a Property from the org.wattdepot.resource.property.jaxb package. Note that this Property is not related to the built-in Java Properties type we are using to read in our simulated meter data, a confusion that will disappear when your meter-specific code is in place. Also note that WattDepot Properties are always Strings, even when the value represents a number. The PropertyDictionary lists all the canonical WattDepot properties.

Finally, we create the client object and attempt to store the SensorData in the server. WattDepot does not allow resources to be overwritten, so if you run SimpleSensor twice with the same property file (thus the same timestamp), you will get an error the second time as it attempts to overwrite the data stored on the first run.

In your own sensor, you should handle errors better (for example `client.storeSensorData(data)` throws many different types of exceptions that let you determine what really happened. You also might want to support multiple properties rather than just powerConsumed.

## 2.2 Coding a multi-threaded sensor in Java ##

When using a single-threaded sensor, a separate process needs to be started for every sensor. With a multi-threaded sensor many sensors can be polled by a single process external to the server, or even within the server process itself.

```
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import javax.xml.datatype.XMLGregorianCalendar;
import org.wattdepot.client.WattDepotClient;
import org.wattdepot.datainput.SensorSource;
import org.wattdepot.datainput.SensorSource.METER_TYPE;
import org.wattdepot.resource.property.jaxb.Property;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.resource.source.jaxb.Source;
import org.wattdepot.sensor.MultiThreadedSensor;
import org.wattdepot.util.tstamp.Tstamp;

public class BetterSensor extends MultiThreadedSensor {

  /**
   * Processes command line arguments and uses MultiThreadedSensor to start polling all Better
   * Sensors listed in the property file.
   * 
   * @param args command line arguments.
   */
  public static void main(String[] args) {
    String propertyFilename = args[0];

    if (!MultiThreadedSensor.start(propertyFilename, METER_TYPE.BETTER)) {
      System.exit(1);
    }
  }

  /**
   * Initializes a sensor by calling the constructor for MultiThreadedSensor.
   * 
   * @param wattDepotUri Uri of the WattDepot server to send this sensor data to.
   * @param wattDepotUsername Username to connect to the WattDepot server with.
   * @param wattDepotPassword Password to connect to the WattDepot server with.
   * @param sensorSource The SensorSource containing configuration settings for this sensor.
   * @param debug If true then display new sensor data when sending it.
   */
  public BetterSensor(String wattDepotUri, String wattDepotUsername, String wattDepotPassword,
      SensorSource sensorSource, boolean debug) {
    super(wattDepotUri, wattDepotUsername, wattDepotPassword, sensorSource, debug);
  }

  @Override
  public void run() {
    Properties properties = new Properties();
    String sourceUri = Source.sourceToUri(sourceName, this.wattDepotUri);

    // Load properties file
    FileInputStream stream = null;
    try {
      stream = new FileInputStream(this.meterHostname);
      properties.load(stream);
    }
    catch (IOException e) {
      System.out.println(this.meterHostname + " not found.");
      return;
    }
    finally {
      if (stream != null) {
        stream.close();
      }
    }

    // Create SensorData object
    SensorData data;
    try {
      XMLGregorianCalendar timestamp = Tstamp.makeTimestamp(properties.getProperty("timestamp"));
      Property powerConsumed =
          new Property(SensorData.POWER_CONSUMED, properties.getProperty("powerConsumed"));
      data = new SensorData(timestamp, "SimpleSensor", sourceUri, powerConsumed);
    }
    catch (Exception e) {
      System.out.println("Unable to convert timestamp.");
      return;
    }

    // Store SensorData in WattDepot server
    WattDepotClient client =
        new WattDepotClient(this.wattDepotUri, this.wattDepotUsername, this.wattDepotPassword);
    try {
      client.storeSensorData(data);
    }
    catch (Exception e) {
      System.out.println("Unable to store sensor data.");
      return;
    }
    System.out.println("Sensor data successfully stored.");
  }
}
```


Here we only need a single command line argument which is the name of a file that holds the rest of the configuration information. An example file is shown below. The first three lines of the file are the WattDepot server information. The rest of the file is made up of sensor source properties. The sensor source keys (`1E` and `1W` below) uniquely identify a sensor, and need to be the same for every property of that sensor. The name property refers to the name of the Source Resource on the server. The updateRate property indicates the number of seconds that should elapse between each data poll. The meterHostname property is a string that allows you to access the data. In this case it is a filename, but it may be an IP Address, a hostname, or any other string that you need to make use of. The meterType specifies which type of meter we are reading.


```
datainput.wattdepot-server.uri=http://localhost:8182/wattdepot/
datainput.wattdepot-server.username=admin@example.com
datainput.wattdepot-server.password=admin@example.com
datainput.source.1E.name=FirstFloorEast
datainput.source.1E.updateRate=15
datainput.source.1E.meterHostname=Floor1East.txt
datainput.source.1E.meterType=BETTER
datainput.source.1W.name=FirstFloorWest
datainput.source.1W.updateRate=15
datainput.source.1W.meterHostname=Floor1West.txt
datainput.source.1W.meterType=BETTER
```

These properties are all parsed by the MultiThreadedSensor class. A new BetterSensor object will be created for each meterType=BETTER found in the properties file, and will be scheduled to run based on the updateRate given. By also giving the MultiThreadedSensor the `METER_TYPE.BETTER` parameter, we are asking it to run BetterSensors only, even if other meterTypes appear in the properties file. A value of `null` would run ALL sensors.

The `run()` method does the actual work of reading the data and creating a SensorData object. In this example, as in the one above, we simply read the data from a file and send it to the WattDepotClient.

# 3.0 Running a Sensor #

Sensors can be run in two ways.
  * The first is by using the main methods shown in the examples above. This allows the sensors to run as their own processes. If a sensor crashes or needs to be reconfigured, it is easy to restart.
  * The second is by creating a multi-threaded sensor and configuring the WattDepot Server to run it automatically on start-up. Note that reconfiguration of sensors will require a complete server restart if you select this option. Add the property
```
wattdepot-server.datainput.start=true
```
> to your wattdepot-server.properties file to have sensors start automatically. Use the
```
  wattdepot-server.datainput.file
```
> property to tell the server where to find the sensor configuration. By default, the server will look for `.wattdepot\client\datainput.properties`


# 4.0 Conclusion #

Hopefully this document has given you an idea of how to write a sensor for your meter. If you do write a sensor that is available for public use, please let us know on the [WattDepot users mailing list](http://groups.google.com/group/wattdepot-users).
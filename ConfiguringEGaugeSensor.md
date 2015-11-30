# Introduction #

This document explains how to configure the WattDepot eGauge sensor.

# Requirements #

Many users will want to run the sensor on the same computer that the WattDepot server runs on, so you should have completed either InstallingWattDepotServerOnHeroku or InstallingWattDepotServerLocally.

# Writing Configuration File #

The eGauge sensor is configured via a properties file. By default, the sensor will look for this properties file in `~/.wattdepot/client/datainput.properties`

Here is an example properties file:

```
## Server properties
datainput.wattdepot-server.uri=http://localhost:8182/wattdepot/
datainput.wattdepot-server.username=kukuicup@example.com
datainput.wattdepot-server.password=CHANGEME

## Meters
datainput.source.MyMeter1.name=MyMeter1
datainput.source.MyMeter1.updateRate=10
datainput.source.MyMeter1.meterHostname=example.egaug.es
datainput.source.MyMeter1.meterType=EGAUGE
datainput.source.MyMeter1.registerName=Student Usage V
```

The first three properties specify how to communicate with the WattDepot server. The URI depends on the port number that the WattDepot server was configured for. The username and password should match a user that was created on the server during the installation (it is not recommended to use the admin user here, since that has more privileges than are needed).

Each meter will have 5 lines of configuration. Each meter's name is used as part a key in the property, so that the eGauge sensor knows which properties belong to which meter. In this example, the meter is called "MyMeter1", and all the properties start with `datainput.source.MyMeter1`.

The `name` property is the Source to which SensorData should be sent to. This must exactly match the name of a Source created when the server was installed. For consistency, it is recommended that the meter key and name match.

The `updateRate` property is the number of seconds between polls of the meter. In this example, we are polling the meter every 10 seconds.

The `meterHostname` property is the domain name or IP address of the meter. Depending on your local network configuration, this could be the actual IP address or hostname of the meter, or (if you have chosen to use eGauge's public proxy connection) it could be a hostname in the `egaug.es` domain.

`meterType` must always be `EGAUGE` for eGauge meters.

`registerName` is the name of the register on the eGauge meter that contains the energy and power data you would like to record in WattDepot. The eGauge has a very flexible configuration system for registers, so it is best to use that system to determine what values you want to record, and just configure the register name here.

For additional meters, just create additional blocks of meter config, each with their own meter key and parameters.

# Running the sensor #

Change to the top level of the wattdepot distribution you downloaded. The available command line arguments can be displayed via the `--help` flag:

```
java -jar wattdepot-sensor-egauge.jar --helpusage: eGaugeSensor
 -d,--debug                    Displays sensor data as it is sent to the
                               server.
 -h,--help                     Print this message
 -p,--propertyFilename <arg>   Filename of property file
```

You can now run the sensor and make sure the properties file is set up properly.

```
java -jar wattdepot-sensor-egauge.jar
```


# Running the sensor in production #

Once you are done configuring the sensor and testing that it is working properly, you will probably want to make sure it runs every time your server boots. This can be done in the same way described in the server installation page.
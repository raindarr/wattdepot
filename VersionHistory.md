# Introduction #

This is a list of the WattDepot released versions in reverse chronological order describing what changed between each release.

# Version 2.1.0822 #
Can be found as [wattdepot-2.1.0822.zip](http://code.google.com/p/wattdepot/downloads/detail?name=wattdepot-2.1.0822-dist.zip). Tagged as "v2.1.0822".
  * Postgres back end now supports postgres accounts that are not superusers or do not have the create database privilege
  * Initial Postgres database initialization should now work in all cases for both local and Heroku deployments
  * Update to Postgres 9.1.4 library

# Version 2.1.0803 #
Can be found as [wattdepot-2.1.0803.zip](http://code.google.com/p/wattdepot/downloads/detail?name=wattdepot-2.1.0803-dist.zip). Tagged as "v2.1.0803", now available in [Ivy RoundUp](http://ivyroundup.googlecode.com/svn/trunk/repo/modules/org.wattdepot/wattdepot/2.1.0803/ivy.xml)

  * Debug flag works again in sensors ([issue 104](https://code.google.com/p/wattdepot/issues/detail?id=104))
  * Added new jar file for PropertyAdder client that allows adding properties to existing Sources
  * Fixed a nasty concurrency problem in PostgresStorageImplementation ([issue 106](https://code.google.com/p/wattdepot/issues/detail?id=106))
  * Restlet thread now configurable via `wattdepot-server.maxthreads` server property, defaults to 255
  * New HammerSensor (it "hammers" the server with rapid requests), which is useful for load testing (developed in the course of closing [issue 106](https://code.google.com/p/wattdepot/issues/detail?id=106))
  * Updated to Restlet 2.1RC5 as part of fixing [issue 106](https://code.google.com/p/wattdepot/issues/detail?id=106)
  * Ephemeral high-frequency data storage was not working properly for virtual sources ([issue 107](https://code.google.com/p/wattdepot/issues/detail?id=107))
  * Multithreaded sensors are now fixed-delay rather than fixed-execution to prevent pileups if sensor polls are delayed ([issue 108](https://code.google.com/p/wattdepot/issues/detail?id=108))
  * Error handling in multithreaded sensors is much improved. In previous versions, if a meter could not be reached even once, the sensor would stop polling the meter ([issue 105](https://code.google.com/p/wattdepot/issues/detail?id=105)).
  * eGauge sensor now available as a jar file.
  * eGauge sensor now reads register name from client properties file ([issue 109](https://code.google.com/p/wattdepot/issues/detail?id=109))
  * Now supports optional Postgres schemas for namespace separation ([issue 111](https://code.google.com/p/wattdepot/issues/detail?id=111)).

# Version 2.0.0510 #
Can be found as [wattdepot-2.0.0510.zip](http://code.google.com/p/wattdepot/downloads/detail?name=wattdepot-2.0.0510-dist.zip). Tagged as "v2.0.0510"

  * Added eGauge sensor, multi-threaded
  * Ted5000 sensor made multi-threaded
  * All three sensors (Modbus, Ted5000, eGauge) can be run from the server process
  * New server property `wattdepot-server.datainput.start` specifies whether sensors should be automatically started in the server process when the server is started. Set to `false` by default.
  * New server property `wattdepot-server.datainput.file` gives the location of the sensor configuration file. By default, looks in `.wattdepot/client/datainput.properties`
  * The WritingSensors wiki has been updated for multi-threaded usage
  * Wattdepot-bridge-client has been improved with better messaging
  * Made better use of Restlet 2.0 in Resources and Client


# Version 2.0.0504 #
Can be found as [wattdepot-2.0.0504.zip](http://wattdepot.googlecode.com/files/wattdepot-2.0.0504-dist.zip). Tagged as "v2.0.0504"

  * Converted from Ant to Maven build system
  * Converted from SVN to Git
  * PostgreSQL storage implementation added. See StorageImplementations for information on the three database options.
  * Google visualizations integrated into REST API rather than using a Servlet on a separate port. See UsingGoogleVisualization for updated API. This change breaks old visualization clients. The [WattDepot-Apps](http://code.google.com/p/wattdepot-apps/) Visualizer has been updated to work with the new version.
  * Running and testing on Heroku enabled. See InstallingWattDepotServerOnHeroku for instructions.
  * Database schemas redesigned to remove XML and make known properties first-class entities in storage. These changes are **not backwards compatible** for production databases, but testing databases will be automatically updated. Use the wattdepot-bridge-client.jar to convert data from a 1.x server to a new 2.0 server.
  * REST API completed with PUT User, DELETE User, and DELETE Source. See the RestApi for usage details.
  * Ephemeral high-frequency data supported through caching
  * Modbus sensor changed to be multi-threaded

# Version 1.6.130 #
Can be found as [wattdepot-1.6.130.zip](http://wattdepot.googlecode.com/files/wattdepot-1.6.130.zip). Tagged as "v1.6.130"

  * Upgrade from Restlet 1.1.8 to 2.0.11 in the server and resources. However, this version doesn't take full advantage of Restlet 2.0 functionality - that is planned for a future release. There are no changes to the client or the RestApi.

# Version 1.6.111 #
Can be found as [wattdepot-1.6.111.zip](http://wattdepot.googlecode.com/files/wattdepot-1.6.111.zip).

  * Adds new API method to delete all SensorData for a Source.
  * Deletion of SensorData is now listed as implemented in RestApi.
  * SharkLogSensor now supports having a different server URI in SensorData than the one the SensorData is actually being sent to.
  * Created a logfile to XML SensorData converter (SensorLogDataConverter) so that logs produced by sensors can be used to reinput data in case of database corruption.
  * (For developers) made Hackystat disabled by default since the public Hackystat server is shut down.

# Version 1.5.1015 #
Can be found as [wattdepot-1.5.1015.zip](http://wattdepot.googlecode.com/files/wattdepot-1.5.1015.zip). Now available via [Ivy RoundUp](http://ivyroundup.googlecode.com/svn/trunk/repo/modules/org.wattdepot/wattdepot/1.5.1015/ivy.xml).

  * Bugfix: sockets were not being closed properly when client finished requests, which could lead to sockets in CLOSE\_WAIT state piling up for long-running processes. Strangely, this wasn't observed before, but quite prominent on Mac OS X Server 10.6.8 with Security Update 2011-06.
  * Created a one-off log converter for Shark meter data (after massaging with Excel).

# Version 1.5.412 #
Can be found as [wattdepot-1.5.412.zip](http://wattdepot.googlecode.com/files/wattdepot-1.5.412.zip).

  * **Important fix:** when a database was initialized, the crucial sensor data index was not being created, leading to abysmal performance for many operations. Any databases created before this fix should be reindexed once using the `-r` command line flag. The error message "Failed to drop SensorData(Source, Tstamp DESC) index." is expected when reindexing a database that never had the index in the first place.
  * A Modbus sensor for ElectroIndustries' Shark 200S meter is now included in WattDepot (wattdepot-sensor-shark.jar)
  * An experimental DbImplementation that uses BerkeleyDB for storage is available, but not used by default. This code has not yet received extensive testing, so use at your own risk.

# Version 1.4.903 #
Can be found as [wattdepot-1.4.903.zip](http://wattdepot.googlecode.com/files/wattdepot-1.4.903.zip).  Now available via [Ivy RoundUp](http://ivyroundup.googlecode.com/).

  * Bulk retrieval of SensorData is now supported. The RestApi supports a "fetchAll" parameter that returns the SensorData resources themselves, rather than a SensorDataIndex. The client library already had a getSensorDatas() convenience method, but now it uses the more efficient fetchAll parameter on the back end ([issue 28](https://code.google.com/p/wattdepot/issues/detail?id=28)).

# Version 1.4.729 #
Can be found as [wattdepot-1.4.729.zip](http://wattdepot.googlecode.com/files/wattdepot-1.4.729.zip).

  * Added new Database resource to RestApi, to support making snapshots of database on server. Also wrote client program to initiate snapshots via API (to be run periodically) ([issue 70](https://code.google.com/p/wattdepot/issues/detail?id=70)).
  * Added new `etc` directory that contains example configuration files for [launchd](http://launchd.macosforge.org/trac/).

# Version 1.4.712 #
Can be found as [wattdepot-1.4.712.zip](http://wattdepot.googlecode.com/files/wattdepot-1.4.712.zip).

  * The PUT method is now implemented for Source resources, including a way to change existing Sources using the `overwrite` flag. [See RestApi](http://code.google.com/p/wattdepot/wiki/RestApi#PUT_{host}/sources/{source}) for more details ([issue 6](https://code.google.com/p/wattdepot/issues/detail?id=6), [issue 63](https://code.google.com/p/wattdepot/issues/detail?id=63)).
  * The client library now supports storing Source resources.
  * Energy calculations are now done using energy counters if the Source supports them. This is many times faster, and much more accurate ([issue 27](https://code.google.com/p/wattdepot/issues/detail?id=27)).
  * New PropertyAdder client that shows how to update existing Sources with new `supportsEnergyCounters` property so they will start computing energy faster.
  * DerbyStorageImplementation now supports reindexing and compressing the database, and new command line arguments to perform those actions ([issue 68](https://code.google.com/p/wattdepot/issues/detail?id=68), [issue 69](https://code.google.com/p/wattdepot/issues/detail?id=69)). **Note that command line syntax is changed:** to specify a subdirectory for server use, the `-d` flag is required. Update any scripts that use the old syntax.
  * Major performance improvement when retrieving the latest sensor data from a Source that has a lot of sensor data ([issue 73](https://code.google.com/p/wattdepot/issues/detail?id=73)).

# Version 1.3.505 #
Can be found as [wattdepot-1.3.505.zip](http://wattdepot.googlecode.com/files/wattdepot-1.3.505.zip).

  * TED5000 sensor is now better behaved when the TED Gateway unit cannot be reached or fails to respond in a timely manner ([issue 56](https://code.google.com/p/wattdepot/issues/detail?id=56))
  * REST API now supports cross-origin use via the Cross-Origin Resource Sharing header. This is a temporary fix, see [issue 64](https://code.google.com/p/wattdepot/issues/detail?id=64)

# Version 1.3.304 #
Can be found as [wattdepot-1.3.304.zip](http://wattdepot.googlecode.com/files/wattdepot-1.3.304.zip).

  * Now includes sensor for [TED 5000 home energy meter](http://www.theenergydetective.com/ted-5000-overview.html) ([issue 47](https://code.google.com/p/wattdepot/issues/detail?id=47))
  * Added support for multiple server processes running on the same system by adding a command line argument to the server indicating the name of the server's home directory under "~/.wattdepot" ([issue 53](https://code.google.com/p/wattdepot/issues/detail?id=53))
  * Added a convenience method for creating a Property from a double, useful when creating SensorData to send to a server.
  * The BMOSensor is now modernized for current versions of WattDepot ([issue 54](https://code.google.com/p/wattdepot/issues/detail?id=54))

# Version 1.2.224 #
Can be found as [wattdepot-1.2.224.zip](http://wattdepot.googlecode.com/files/wattdepot-1.2.224.zip). Probably won't be added to [Ivy RoundUp](http://ivyroundup.googlecode.com/) since there are no client changes.
  * [Visualization data source API](UsingGoogleVisualization.md) now provides a way to get the latest sensor data for a source ([issue 51](https://code.google.com/p/wattdepot/issues/detail?id=51))
  * Reorganized URIs in the Visualization data source API to be more consistent with REST API, and more internally consistent ([issue 50](https://code.google.com/p/wattdepot/issues/detail?id=50)). UsingGoogleVisualization has been updated with the new URI syntax.
  * Created ExampleStreamingSensor class that just sends changing bogus data to a source, for testing monitoring clients ([issue 49](https://code.google.com/p/wattdepot/issues/detail?id=49)).

# Version 1.2.216 #

  * New [GET latest sensor data](http://code.google.com/p/wattdepot/wiki/RestApi#GET_{host}/sources/{source}/sensordata/latest) method for near-real time display of sensor data ([issue 43](https://code.google.com/p/wattdepot/issues/detail?id=43))
  * Example [MonitorSourceClient](http://code.google.com/p/wattdepot/source/browse/trunk/src/org/wattdepot/client/monitor/MonitorSourceClient.java) class that shows how to use the GET latest method ([issue 46](https://code.google.com/p/wattdepot/issues/detail?id=46))

# Version 1.1.208 #

  * The GET method for the User resource now works ([issue 8](https://code.google.com/p/wattdepot/issues/detail?id=8))
  * List of library dependency versions is now maintained in the project, rather than parasitizing from Hackystat project
  * Updated to Derby 10.5.3.0 and Restlet 1.1.8
  * Fixed a bug in the Google Visualization data source that completely prevented raw sensordata from being retrieved.
  * Fixed intermittent JUnit test failure, tracked down to Restlet connector incompatibility (see [issue 38](https://code.google.com/p/wattdepot/issues/detail?id=38))

# Version 1.0.120 #

First fully functional release, with Derby for persistent data storage and support for Google Visualization data source API. Previous versions had very inefficient SQL queries for certain common operations, this version fixes that. Still plenty of open issues, and parts of the REST API are not yet implemented.
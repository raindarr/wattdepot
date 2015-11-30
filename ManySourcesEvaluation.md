

# Summary #

I created a simple program to use WattDepotClient API to communicate to a local wattdepot server, created 7000 sources and stored 1 month worth of hourly sample sensor data into each source, yielding roughly 5 million rows of wattdepot data. A simple query to retrieve the latest sensor data is also performed. The time statistics of the above operations are recorded to help analysis of the performance of the wattdepot system.

# Test Environment #

Intel Core Duo 3.16GHz, 3.25G RAM, Windows XP 32bit

# Performance Data #

  1. It took about 5 minutes to create 7000 sources.
  1. It took about 30 seconds to store 720 sensordata (30d x 24h hourly data).
    * ~0.05 second per sensor data entry
  1. It is trivial to display the latest sensor data for a specific source.
  1. It took 4 seconds to visual 2 sources' one month data through WattDepot-Visualizer.

# Conclusion #

WattDepot Server performs well in the scenario of 7000 sources and 5 million rows of sensor data.

# Issues #

  1. It will take 7000\*30/3600 = ~58 hours to completely load all the data.
    * Workaround: I did not have enough patience to wait for the program to complete, so I killed the program and loaded it directly into the Derby database using JDBC. It took a little less than 1 second to load 720 rows into DB directly, and ~1.5 hours to load all data.
    * It may not be an issue because the sensor/source will send in data hourly. According to the above performance data, it only takes 0.05 second for one data entry through the REST API, which seems to be sufficient for normally data collection.
  1. The concurrent data collection is not tested. It may or may not be a concern for a single WattDepot server to handle 7000 concurrent data input.
  1. It takes some time to query sensordata without using the index. The WattDepot data is indexed by source + timestamp, so it performs very well when using the index. Other types of data query seems to suffer because of the Derby DB implementation.

# To Do #

  1. Test the concurrent data collection scalability.
  1. Test the display and visualization of aggregation of raw sensor data.
  1. Experiment the clustering of wattdepot server.

# Source Code #

```
package org.wattdepot.examples;

import java.util.List;
import java.util.Random;

import javax.xml.datatype.XMLGregorianCalendar;

import org.wattdepot.client.WattDepotClient;
import org.wattdepot.client.WattDepotClientException;
import org.wattdepot.resource.property.jaxb.Property;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.resource.source.jaxb.Source;
import org.wattdepot.util.tstamp.Tstamp;

 /**
  * Provides an example for client program to communicate with a WattDepot server
  * using WattDepot Client API:
  * 1. create sources
  * 2. list sources
  * 3. create sensordata
  * 4. list latest sensor data
  * 
  * It also provides an exercise of creating 7000 sources and loading 1 month 
  * worth of hourly random power consumption data into each source, yielding 
  * roughly 5 million rows of energy data.
  * 
  * To run the program, include the wattdepot-lib-client.jar in the classpath.
  * 
  * @author Yongwen Xu  2010-10-17
  */
public class WattExample {

    // wattdepot server info
    public static String mServerURI = "http://localhost:8182/wattdepot/";  
    public static String mServerUser = "admin@example.com";
    public static String mServerPwd = "admin@example.com";

    // example source info
    public static String mSourceName = "ExampleSourceTestAgain";                     
    public static String mSourceOwnerUri = "Example Owner URI";
    public static String mSensorTool = "ExampleSensorTool";
    
    // number of source
    public static int SOURCE_COUNT = 7000;          // CHANGE this to create less data
    // sensor data timestamp year and month
    public static String DATA_YEAR_MONTH = "2010-08-";  // CHANGE this to use different time
    // sensor data max random value
    public static int DATA_MAX_RANDOM_VALUE = 10000;   

    public static Random mRandom = new Random();
    public static WattDepotClient mClient = new WattDepotClient(mServerURI, mServerUser, mServerPwd);
    
    public static void main(String[] args) throws Exception {
      healthCheck();
     
      System.out.println(Tstamp.makeTimestamp() + " ------- starting create source");      
      for (int i = 0; i < SOURCE_COUNT; i++) 
            createSource(i);        
      System.out.println(Tstamp.makeTimestamp() + " ------- done create source");
      
      listSource();

      System.out.println(Tstamp.makeTimestamp() + " ------- loading sensor data");
      for (int i = 0; i < SOURCE_COUNT; i++)
        createSensorData(mSourceName + i);
      System.out.println(Tstamp.makeTimestamp() + " ------- done loading sensor data");
      
      for (int i = 0; i < SOURCE_COUNT; i++)         
            listLatestSensorData(mSourceName + i);        
      System.out.println(Tstamp.makeTimestamp() + " done displaying sensor data");        

    }

    /**
     * check the wattdepot server status. if not ok, exit the program.
     */
    public static void healthCheck() 
    {    
        if (mClient.isHealthy()) {
          System.out.println("WattDepot server found.");
        }
        else {
          System.out.println("WattDepot server NOT found. Exiting...");
          System.exit(-1);
        }
    }

    /** create the source using the index as part of the unique name.
     * 
     * @param index
     */
    public static void createSource(int index) {
       try{
         mClient.storeSource(new Source(
            mSourceName + index
            ,mSourceOwnerUri + index
            ,true                 // public
            ,false                // not virtual
            ,"Example Coordinates"
            ,"Example Location"
            ,"Example Description"
            ,null                 // no additional props
            ,null)               // no sub source
         ,true);
      } catch (Exception e) {
          System.out.println("Unable to store source " + mSourceName + index + ". Exception is " + e);
      }
    }
    
    /**
     * list the sources in the server.
     */
    public static void listSource() {
        List<Source> sourceList = null;
        try {
            sourceList = mClient.getSources();
        } catch (WattDepotClientException e) {
            System.out.println("There was a problem accessing the server: " + e.toString());
        }
        System.out.println("List of sources:");
        for (Source source: sourceList) {
            System.out.println(source.getName());
        }
    }

    /**
     * create one month of hourly sensorData and store in the specified source,
     *   random SensorData.POWER_CONSUMED data is used.
     * @param sourceName
     */
    public static void createSensorData(String sourceName){
        String sourceUri = Source.sourceToUri(sourceName, mServerURI);

        try {
            for (int date = 1; date < 31; date++)
                for (int hour = 0; hour < 24; hour++) {
                    String time = DATA_YEAR_MONTH + pad(date) + "T" + pad(hour) + ":00:00.0-10:00";
                    XMLGregorianCalendar timestamp = Tstamp.makeTimestamp(time);
                    Property powerConsumed = new Property(SensorData.POWER_CONSUMED, mRandom.nextInt(DATA_MAX_RANDOM_VALUE));
                
                    SensorData data = new SensorData(timestamp, mSensorTool, sourceUri, powerConsumed);

                    mClient.storeSensorData(data);
                }
           System.out.println(Tstamp.makeTimestamp() + " Sensor data stored for source " + sourceName);
        } catch (Exception e) {
            System.out.println("Unable to store sensor data for source "+sourceName+". Exception is " + e);
            return;
        }        
    }

    /**
     * list the latest sensor data for the specified source.
     * @param sourceName
     */
    public static void listLatestSensorData(String sourceName) {
       SensorData list = null;
        try {
            list = mClient.getLatestSensorData(sourceName);
        } catch (WattDepotClientException e) {
            System.out.println("There was a problem accessing the server: " + 
                               e.toString());
        }
        System.out.println("latest sensor data of "+sourceName + " : " +list.getProperties());
    }

    /**
     * utility function to pad the date/hour number to the 2-digit string.
     *  e.g., 1 => "01".
     * @param number
     * @return String
     */
    private static String pad(int number) {
        String str = Integer.toString(number);
        if (str.length() < 2) {
            str = "0" + str;
        }
        return str;
    }
}
```
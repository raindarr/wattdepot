

# 1.0 Introduction #

WattDepot implements the web service functionality specified in the [SensorBase REST API Specification](RestApi.md). This page describes how to use the WattDepotClient Java library. While the library will make the lower-level HTTP API calls, the API introduction explains the concepts and data types that are crucial for effective use of WattDepot.

For more details on the WattDepotClient API, see its JavaDoc documentation, which can be found inside the distribution ZIP file available in the  [Downloads area](http://code.google.com/p/wattdepot/downloads/list).

**Note:** As of the time this document is being written (10/21/2009), the WattDepot server does not save to disk any of the data that is stored in it. Therefore, do not store any data in WattDepot that is not saved somewhere else. This will be remedied at some point, but for now consider yourself duly warned.

## 1.1 Prepare your Java environment ##

We assume that you are already a Java developer, and have your Java development environment set up. Inside the distribution ZIP file at the top level is a JAR file named `wattdepot-clientlib.jar`. You will need to add this to the classpath of your environment. If you are using an IDE like Eclipse, this involves adding the JAR file to your Java Build Path. The `wattdepot-javadoc.zip` and `wattdepot-source.zip` files contain the JavaDocs and Java source for the entire WattDepot system (not just the client library). In some IDEs (like Eclipse), you can attach them to the library to allow easy linking and display of the JavaDocs and source code.

## 1.2 Choosing a WattDepot server ##

Unsurprisingly, the WattDepot client needs to talk to a WattDepot server. If you already have access to a WattDepot server, you need to know the URI for the server (which should look something like `http://wattdepot.example.com:1234/wattdepot`), and possibly a username and password if you plan to store data in WattDepot or retrieve private data. No username or password is required if the Sources you are interested in on the server are public.

If you do not have access to a WattDepot server, you can easily start one up. You can start a server using default values from your environment's shell in the unpacked WattDepot distribution directory using the following command:

`java -jar wattdepot-server.jar`

You should receive output something like this:

```
/Users/rbrewer/.wattdepot/server/wattdepot-server.properties not found. Using default server properties.
2009/10/21 20:27:06  Starting WattDepot server.
2009/10/21 20:27:06  Host: http://localhost:8182/wattdepot/
2009/10/21 20:27:06  Server Properties:
                wattdepot-server.admin.email = admin@example.com
                wattdepot-server.admin.password = admin@example.com
                wattdepot-server.context.root = wattdepot
                wattdepot-server.db.dir = /Users/rbrewer/.wattdepot/server/db
                wattdepot-server.db.impl = org.wattdepot.server.db.memory.MemoryStorageImplementation
                wattdepot-server.gviz.context.root = gviz
                wattdepot-server.gviz.port = 8184
                wattdepot-server.hostname = localhost
                wattdepot-server.logging.level = INFO
                wattdepot-server.port = 8182
                wattdepot-server.restlet.logging = false
                wattdepot-server.smtp.host = mail.hawaii.edu
                wattdepot-server.test.admin.email = admin@example.com
                wattdepot-server.test.admin.password = admin@example.com
                wattdepot-server.test.db.dir = /Users/rbrewer/.wattdepot/server/testdb
                wattdepot-server.test.domain = example.com
                wattdepot-server.test.gviz.port = 8185
                wattdepot-server.test.hostname = localhost
                wattdepot-server.test.install = false
                wattdepot-server.test.port = 8183

2009/10/21 20:27:06  Default resource directory /Users/rbrewer/.wattdepot/server/default-resources not found, no default resources loaded.
2009-10-21 20:27:06.243::INFO:  Logging to STDERR via org.mortbay.log.StdErrLog
2009/10/21 20:27:06  Google visualization URL: http://localhost:8184/gviz/
2009/10/21 20:27:06  Maximum Java heap size (MB): 85.393408
2009-10-21 20:27:06.327::INFO:  jetty-6.1.9
2009-10-21 20:27:06.365::INFO:  Started SocketConnector@0.0.0.0:8184
2009/10/21 20:27:06  WattDepot server (Version Development) now running.
```

The third line of output displays the URI of your new WattDepot server, in this case "`http://localhost:8182/wattdepot/`". Note that the trailing slash is required. The default administrator username and password are both `admin@example.com`. For now, you can ignore the other URI provided near the end.

## 1.3 Connecting to the WattDepot server ##

Bring up your favorite Java editing environment and create a Project to hold your first example program, which is called "HealthCheck".

Now, cut and paste the following code into a file called "HealthCheck.java":

```
import org.wattdepot.client.WattDepotClient;

public class HealthCheck {
  /**
   * Tests whether the WattDepot server is running.
   * 
   * @param args First arg is the host URI, such as 'http://localhost:8182/wattdepot/'.
   */
  public static void main(String[] args) {
    WattDepotClient client = new WattDepotClient(args[0]);
    if (client.isHealthy()) {
      System.out.println("WattDepot server found.");
    }
    else {
      System.out.println("WattDepot server NOT found.");
    }
  }
}
```

This program creates a WattDepotClient object with the server URI provided on the command line, and then performs a health test on the server. If this works, then you have written the simplest WattDepot client. Congratulations! If it doesn't work, check to make sure your WattDepot server is still running, and that you copied or typed the URI in properly.

## 1.4 Retrieve Sources from WattDepot ##

As of the time this document is being written (10/21/2009), the WattDepot server's ability to store Users and Sources is not yet fully implemented. For the rest of this tutorial we assume that you have a WattDepot server running that has default data loaded from files (such as the output of the [OSCAR project](http://code.google.com/p/oscar-project/) that simulates the generation of power on the island of Oahu, Hawaii).

Create a new file called GetSource, and paste the following code into it:

```
import java.util.List;
import org.wattdepot.client.WattDepotClient;
import org.wattdepot.client.WattDepotClientException;
import org.wattdepot.resource.source.jaxb.Source;

public class GetSources {
  /**
   * Retrieves the list of Sources from the WattDepot server.
   * 
   * @param args First arg is the host URI, such as 'http://localhost:8182/wattdepot/'.
   */
  public static void main(String[] args) {
    WattDepotClient client = new WattDepotClient(args[0]);
    List<Source> sourceList = null;
    try {
      sourceList = client.getSources();
    }
    catch (WattDepotClientException e) {
      System.err.println("There was a problem accessing the server: " + e.toString());
    }
    System.out.println("List of sources:");
    for (Source source : sourceList) {
      System.out.println(source.getName());
    }
  }
}
```

We again take the server URI from the command line. We then attempt to get a List of the Sources publically available on the server. Note that the client can throw various types of exceptions if it encounters errors in retrieving the list of Sources. For this example, we choose to catch WattDepotClientException, which is the parent of all those exceptions, but in other cases you may wish to catch each one separately.

Once we have the list of sources, we simply iterate through each one and print its name to System.out. The resulting output might look something like this:

```
List of sources:
SIM_AES
SIM_HONOLULU
SIM_HONOLULU_8
SIM_HONOLULU_9
SIM_HPOWER
SIM_IPP
SIM_KAHE
SIM_KAHE_1
SIM_KAHE_2
SIM_KAHE_3
SIM_KAHE_4
SIM_KAHE_5
SIM_KAHE_6
SIM_KAHE_7
SIM_KALAELOA
SIM_OAHU_GRID
SIM_WAIAU
SIM_WAIAU_10
SIM_WAIAU_5
SIM_WAIAU_6
SIM_WAIAU_7
SIM_WAIAU_8
SIM_WAIAU_9
```

## 1.5 Retrieve SensorData from WattDepot ##

Now that we have retrieved the list of Sources, let's retrieve some SensorData from a particular Source. One of the sources from the Oscar data set is "SIM\_KAHE\_2", representing one of the Kahe power plants. Create a new file called GetSensorData and paste in the following code:

```
import javax.xml.datatype.XMLGregorianCalendar;
import org.wattdepot.client.BadXmlException;
import org.wattdepot.client.MiscClientException;
import org.wattdepot.client.NotAuthorizedException;
import org.wattdepot.client.ResourceNotFoundException;
import org.wattdepot.client.WattDepotClient;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.util.tstamp.Tstamp;

public class GetSensorData {
  /**
   * Retrieves SensorData for a particular Source from the WattDepot server.
   * 
   * @param args First arg is the host URI, such as 'http://localhost:8182/wattdepot/'.
   */
  public static void main(String[] args) {
    WattDepotClient client = new WattDepotClient(args[0]);
    SensorData data = null;
    XMLGregorianCalendar timestamp = null;
    try {
      timestamp = Tstamp.makeTimestamp("2009-10-31T23:45:00.000-10:00");
    }
    catch (Exception e) {
      System.err.println("Unable to create timestamp");
    }
    try {
      data = client.getSensorData("SIM_KAHE_2", timestamp);
    }
    catch (NotAuthorizedException e) {
      System.err.println("Not authorized to view source.");
    }
    catch (ResourceNotFoundException e) {
      System.err.println("Source name not found.");
    }
    catch (BadXmlException e) {
      System.err.println("Server provided bad XML: " + e.toString());
    }
    catch (MiscClientException e) {
      System.err.println("Problem encountered with server: " + e.toString());
    }
    if (data == null) {
      System.err.println("Unable to retrieve sensor data.");
    }
    else {
      System.out.println("Timestamp: " + data.getTimestamp());
      System.out.println("Tool: " + data.getTool());
      System.out.println("Properties: " + data.getProperties());
    }
  }
}
```

Running this code might return output something like this:

```
Timestamp: 2009-10-31T23:45:00.000-10:00
Tool: OscarDataConverter
Properties: [Property [key=powerGenerated, value=5.7E7]]
```

Most of the code here is for error handling. Timestamps, representing a particular point in time, are a commonly used data type in WattDepot. They are represented via the `XMLGregorianCalendar` type. Creating and modifying these timestamps can be laborious, so we provide a `Tstamp` class full of static methods for creating and modifying them. In the class above we use the `makeTimestamp` method to create a timestamp from a String representation. You can see that the string consists of a date in year-month-day format, the letter "T" to separate the date from the time, and the time as hours:minutes:seconds.millisecondsin 24-hour format. Finally, the string ends with any time zone offset from UTC (aka GMT). The string in our code represents a time 15 minutes before midnight on Halloween in Hawaii Standard Time. Unfortunately, parsing the timestamp can throw an exception, so we surround the `makeTimestamp` call with try/catch blocks.

Once we have the timestamp, we make the request for sensor data from SIM\_KAHE\_2 at that timestamp with the `client.getSensorData` method. If successful, this method will return a value of type SensorData. `getsSensorData` can throw several types of exceptions, and in this example we catch each one separately (unlike the previous example in 1.4). The exceptions should be fairly self explanatory, and some should be quite rare in practice (such as the server returning bad XML).

Once we have navigated the exceptions, we can extract fields we are interested from the SensorData object, and print them to System.out.

### 1.5.1 Retrieve a range of SensorData from WattDepot ###

We can also retrieve all sensor data from a range specified by two timestamps. The code is very similar to the example in 1.5:

```
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;
import org.wattdepot.client.WattDepotClient;
import org.wattdepot.client.WattDepotClientException;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.util.tstamp.Tstamp;

public class GetSensorDataRange {
  /**
   * Retrieves SensorData for a range of times from a particular Source from the WattDepot server.
   * 
   * @param args First arg is the host URI, such as 'http://localhost:8182/wattdepot/'.
   */
  public static void main(String[] args) {
    WattDepotClient client = new WattDepotClient(args[0]);
    List<SensorData> dataList = null;
    XMLGregorianCalendar start = null, end = null;
    try {
      start = Tstamp.makeTimestamp("2009-10-31T23:45:00.000-10:00");
    }
    catch (Exception e) {
      System.err.println("Unable to create timestamp");
    }
    end = Tstamp.incrementDays(start, 1);
    try {
      dataList = client.getSensorDatas("SIM_KAHE_2", start, end);
    }
    catch (WattDepotClientException e) {
      System.err.println("Problem encountered with client: " + e);
    }
    if (dataList == null) {
      System.err.println("Unable to retrieve sensor data.");
    }
    else {
      for (SensorData data : dataList) {
        System.out.println("Timestamp: " + data.getTimestamp());
        System.out.println("Tool: " + data.getTool());
        System.out.println("Properties: " + data.getProperties());
      }
    }
  }
}
```

This code (with appropriate server URI passed as command line argument) produces this type of output:

```
Timestamp: 2009-10-31T23:45:00.000-10:00
Tool: OscarDataConverter
Properties: [Property [key=powerGenerated, value=5.7E7]]
Timestamp: 2009-11-01T00:00:00.000-10:00
Tool: OscarDataConverter
Properties: [Property [key=powerGenerated, value=5.5E7]]
Timestamp: 2009-11-01T00:15:00.000-10:00
Tool: OscarDataConverter
Properties: [Property [key=powerGenerated, value=5.9E7]]
[...]
```

In this example we create two timestamps, pass them to `getSensorDatas` and then iterate through the resulting list of SensorData, printing each one to System.out. We create the end timestamp using another useful method `Tstamp.incrementDays` which takes a timestamp and a number of days to increment and returns a new timestamp.

## 1.6 Retrieve Power data from WattDepot ##

While retrieving sensor data is useful, in many cases you might want to determine how much power is being generated or consumed by a Source at an arbitrary point in time. But if the timestamp is arbitrary, it may not correspond to any sensor data we have stored. Luckily, WattDepot can handle this situation using piece-wise linear interpolation between the available sensor data points. If you think of the sensor data as being plotted as points on a graph, the linear interpolation is just drawing lines between each adjacent set of points. WattDepot can then provide an estimate of the power at any point on the graph just by looking up where the chosen timestamp intersects the interpolation line. The good news is that WattDepotClient makes this very easy, as can be seen in the following code:

```
import javax.xml.datatype.XMLGregorianCalendar;
import org.wattdepot.client.WattDepotClient;
import org.wattdepot.client.WattDepotClientException;
import org.wattdepot.util.tstamp.Tstamp;

public class GetPower {
  /**
   * Retrieves the power generated by a particular Source from the WattDepot server.
   * 
   * @param args First arg is the host URI, such as 'http://localhost:8182/wattdepot/'.
   */
  public static void main(String[] args) {
    WattDepotClient client = new WattDepotClient(args[0]);
    double power = 0;
    XMLGregorianCalendar timestamp = null;
    try {
      timestamp = Tstamp.makeTimestamp("2009-11-01T12:34:56.789-10:00");
    }
    catch (Exception e) {
      System.err.println("Unable to create timestamp");
    }
    try {
      power = client.getPowerGenerated("SIM_KAHE_2", timestamp);
    }
    catch (WattDepotClientException e) {
      System.err.println("Problem encountered with client: " + e);
    }
    System.out.format("Power at timestamp %s: %g W%n", timestamp, power);
    System.out.format("Power at timestamp %s: %.3g MW%n", timestamp, power / 1E6);
  }
}
```

The output of this code might be something like this based on simulated data from Ocsar:

```
Power at timestamp 2009-11-01T12:34:56.789-10:00: 8.80000e+07 W
Power at timestamp 2009-11-01T12:34:56.789-10:00: 88.0 MW
```

The core of the code is the call to `getPowerGenerated`. This makes a request to the WattDepot server, which will find that no sensor data corresponds to the timestamp provided. Therefore, the server will interpolate between the two nearest sensor data objects, and return a SensorData object to the client library with the interpolated value. This SensorData object is the same type as the ones we retrieved earlier in 1.4 & 1.5, but contains an additional property that indicates that the data was produced through interpolation. `getPowerGenerated` then extracts this power data and provides it as a double floating point value. We display the power generated as Watts, and also in megawatts, which is more appropriate for power plants.

### 1.6.1 Retrieve Power data for a virtual source ###

Another useful concept in WattDepot is the virtual source. A virtual source does not directly store sensor data, it just includes data from other sub-sources. Virtual sources can be chained together, with virtual sources having virtual subsources. This is very useful for aggregating sensor data from multiple sources. For example, in Oscar there are virtual sources that combine data from a particular set of power plants. If we change the code in `GetPower` from 1.6 above to use the source named "SIM\_KAHE", we can see how much power was being generated by the set of plants Kahu at that moment in time. The output might look like this:

```
Power at timestamp 2009-11-01T12:34:56.789-10:00: 5.75000e+08 W
Power at timestamp 2009-11-01T12:34:56.789-10:00: 575 MW
```

With no additional code we were able to get the power generated by an entire group of power plants!

## 1.7 Charting data from WattDepot ##

One useful thing you can do with data from WattDepot is make a chart. In this example, we use the [Google Chart API](http://code.google.com/apis/chart/) which provides an easy way to make charts. The Charts API responds to HTTP GET requests with a PNG image based on the data in the URI. The following example will produce a chart of power over a single day with data points every 30 minutes. The code looks similar to the example in 1.6 (though with less error checking, for brevity):

```
import javax.xml.datatype.XMLGregorianCalendar;
import org.wattdepot.client.WattDepotClient;
import org.wattdepot.util.tstamp.Tstamp;

public class ChartPower {
  /**
   * Generates a URI suitable for use with Google Charts that displays the power generated by a
   * particular Source from the WattDepot server, sampled over a particular range of time.
   * 
   * @param args First arg is the host URI, such as 'http://localhost:8182/wattdepot/'.
   */
  public static void main(String[] args) throws Exception {
    WattDepotClient client = new WattDepotClient(args[0]);
    double power = 0, minPower = Double.MAX_VALUE, maxPower = Double.MIN_VALUE;
    XMLGregorianCalendar startTime = null, endTime = null, timestamp;
    int interval = 30;
    int maxUriLength = 2048;
    StringBuffer chartUri = new StringBuffer(maxUriLength);

    startTime = Tstamp.makeTimestamp("2009-11-01T00:00:00.000-10:00");
    endTime = Tstamp.makeTimestamp("2009-11-02T00:00:00.000-10:00");

    chartUri.append("http://chart.apis.google.com/chart?chs=250x100&cht=lc&chd=t:");
    timestamp = startTime;
    while (Tstamp.lessThan(timestamp, endTime)) {
      // Convert to megawatts
      power = client.getPowerGenerated("SIM_OAHU_GRID", timestamp) / 1000000;
      chartUri.append(String.format("%.1f,", power));
      if (power < minPower) {
        minPower = power;
      }
      if (power > maxPower) {
        maxPower = power;
      }
      timestamp = Tstamp.incrementMinutes(timestamp, interval);
    }
    // Delete trailing ','
    chartUri.deleteCharAt(chartUri.length() - 1);
    chartUri.append(String.format("&chds=%.1f,%.1f&chxt=y&chxr=0,%.1f,%.1f", minPower, maxPower,
        minPower, maxPower));
    System.out.format("Google Charts URI:%n%s%n", chartUri);
  }
}
```

The output of this code might be something like this based on simulated data from Ocsar:

```
Google Charts URI:
http://chart.apis.google.com/chart?chs=250x100&cht=lc&chd=t:801.0,801.0,805.0,803.9,796.9,797.9,802.9,798.8,798.8,800.8,805.8,818.8,823.7,836.7,848.7,853.7,862.7,872.6,887.6,897.6,907.6,915.6,924.5,932.5,947.5,954.5,962.5,963.4,963.4,959.4,959.4,959.4,958.3,956.3,960.3,958.3,958.2,960.2,957.2,961.2,950.2,930.1,911.1,893.1,874.1,848.1,834.0,810.0&chds=796.9,963.4&chxt=y&chxr=0,796.9,963.4
```

When processed by Google Charts, the result looks like this:

![http://wattdepot.googlecode.com/svn/wiki/ExampleChart.png](http://wattdepot.googlecode.com/svn/wiki/ExampleChart.png)
[link to chart](http://chart.apis.google.com/chart?chs=250x100&cht=lc&chd=t:801.0,801.0,805.0,803.9,796.9,797.9,802.9,798.8,798.8,800.8,805.8,818.8,823.7,836.7,848.7,853.7,862.7,872.6,887.6,897.6,907.6,915.6,924.5,932.5,947.5,954.5,962.5,963.4,963.4,959.4,959.4,959.4,958.3,956.3,960.3,958.3,958.2,960.2,957.2,961.2,950.2,930.1,911.1,893.1,874.1,848.1,834.0,810.0&chds=796.9,963.4&chxt=y&chxr=0,796.9,963.4)

We start by creating timestamps for the start and end of our desired range. We build up the URI that we are going to use in a StringBuffer. The URI starts with parameters to set the size of the resulting chart in pixels, the type of the chart (line in this case) and the start of the chart data. We then sample the power starting at startTime, convert the value into megawatts, and then append the value to the URI in the Google Chart format. We also keep track of the minimum and maximum power seen, as we will need this when making the Chart API call. We then increment the timestamp value by the sampling interval and start the loop again until we are done.

After the loop completes, we remove the trailing "," (note: Google Charts will silently fail if the extra comma is not deleted). We then append the Chart API parameters to scale the data to the minimum and maximum power we sampled, and to display a y-axis with labels. Finally, we output the URI, which can be cut and pasted into a browser for display.

## 1.8 PUT data to the WattDepot server ##

For an example of how to PUT data to the server, check the WritingSensors wiki page.

# 2.0 Other ways to interact with the WattDepot server #

## 2.1 Using a standard browser ##

You can also use a standard browser to perform HTTP GET operations. For example, you could enter the URI `http://localhost:8182/wattdepot/sources/` into your browser to display the list of sources on the server. Since you have not provided any authentication credentials, you will receive the list of public sources on the server.

Some browsers, such as Safari, will not display raw XML. For this browser, you can use View -> View Source to see the XML.

## 2.2 Using the Firefox Poster add-in ##

The [Firefox Poster Add-on](https://addons.mozilla.org/en-US/firefox/addon/2691) enhances the [Firefox](http://www.mozilla.com/firefox/) browser with the ability to specify a variety of additional HTTP methods besides GET and POST, and specify the payloads and headers associated with these requests.  This add-on makes it much easier to interactively explore the behavior of a web service such as the WattDepot server.

## 2.3 Client programs in other languages ##

At the moment there are no libraries for interacting with the WattDepot server other than the Java one.

Since the WattDepot server interacts with client programs using the standard
HTTP protocol, it is straightforward to create clients for the WattDepot server
in other languages and environments such as C#, .NET, Ruby, and Ruby on
Rails.  There are two basic issues you will need to resolve when implementing a client for another language/environment:

  * Your language/environment should supply an HTTP library that supports GET, PUT, POST, HEAD, and DELETE, as well as HTTP Basic authentication.  In other words, you will need something analogous to the Restlet Framework that we use in the Java examples above.
  * You will need a way to create and manipulate XML representations of the WattDepot resources.  In other words, you will need to implement something analogous to the marshall and unmarshall methods supplied with the wattdepot-clientlib.jar file.  Alternatively, you could use XPath and stay entirely in the XML realm.

Once you have resolved these issues, you should be able to implement clients in your favorite language/environment as easily as the above examples in Java. If you are interested in creating a client in another language, please let us know about it!

# 3.0 Questions? Problems? #

The best resource to consult is the [WattDepot users mailing list](http://groups.google.com/group/wattdepot-users).


# Introduction #

The [Google Visualization API](https://developers.google.com/chart/) consists of a protocol for data sources (like WattDepot) to serve up their data to visualization widgets for viewing and analysis. The data source provides a standardized way to query for data, which can be connected to a large set of visualization tools. Google provides extensive [information about the data source API](https://developers.google.com/chart/interactive/docs/dev/implementing_data_source), and a [gallery of visualization widgets](https://developers.google.com/chart/interactive/docs/gallery).

This document explains how to use the Annotated Timeline visualization with WattDepot to produce graphs of data from WattDepot. Familiarity with the [REST API](RestApi.md) is assumed, and WritingWattDepotClients might also be helpful. You may also find reading the [Google Visualization API documentation](https://developers.google.com/chart/) helpful.

**Note** that the examples here use the URI for the public WattDepot server `http://server.wattdepot.org:8194`. If you are using a different WattDepot server, then you should change the URI to point to your server (WattDepot writes the URI for the visualization resource on standard out when the server is started up).

# WattDepot Data Source #

WattDepot provides a Google Visualization API data source that can be used to look at power data. The visualization data source is created as a resource within the WattDepot REST API, and follows the Google Visualization [data source wire protocol](https://developers.google.com/chart/interactive/docs/dev/implementing_data_source). While the data source will respond to any properly formatted HTTP request, most requests will come from Javascript running in a web browser, in order to feed into a visualization widget.

## URI Scheme ##

The WattDepot data source follows the Google Visualization URI query parameters, and adds a few of its own. Like in the RestApi, there are two distinct types of queries: sensor data, and calculated values (power, energy, and carbon). Queries for sensor data return the raw sensor data for a source, while the calculated values may combine data from multiple sources (in the case of virtual sources), and may interpolate between sensor data values if there is no sensor data at the requested time.

## Sensor Data URI ##

Sensor data queries have the following format:

Carbon resources are named using the following URI specification:
|`{host}/sources/{source}/gviz/sensordata?startTime={timestamp}&endTime={timestamp}&tq={queryString}`|
|:---------------------------------------------------------------------------------------------------|

Where:

| **Element** | **Description** | **Example** |
|:------------|:----------------|:------------|
| `{host}`    | the HTTP prefix of the server hosting the service | http://server.wattdepot.org:8194/wattdepot |
| `{source}`  | the name of a Source resource. | SIM\_HPOWER |
| `{timestamp}` | an XML [dateTime](http://www.w3.org/TR/2001/REC-xmlschema-2-20010502/#dateTime) object | 2009-12-01T00:00:00.000-10:00 |
| `{queryString}` | a [Google Visualization API parameter](https://developers.google.com/chart/interactive/docs/dev/implementing_data_source#requestformat) representing a [data source query](https://developers.google.com/chart/interactive/docs/querylanguage) | select%20timePoint%2C%20powerGenerated |

Example sensor data URI:

| http://server.wattdepot.org:8194/wattdepot/sources/SIM_HPOWER/gviz/sensordata?startTime=2009-12-01T00:00:00.000-10:00&endTime=2009-12-07T00:00:00.000-10:00&tq=select%20timePoint%2C%20powerGenerated |
|:------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|

Note: the `{queryString}` in this context is a parameter distinct from the HTTP "query string", which is everything that follows after the "?" in the URI. Also, note that the queryString must be [URL-encoded](https://developers.google.com/chart/interactive/docs/querylanguage#Setting_the_Query_in_the_Data_Source_URL).

Technically the startTime and endTime parameters are optional, but leaving them off will request all the sensor data for the given source, which could be voluminous.

### Latest Sensor Data URI ###

In some contexts it is useful to be able to retrieve a table containing a single row that contains the latest sensor data value for a particular Source. In keeping with the REST API, this is done using the string "latest" in the URI, like this:

|`{host}/sources/{source}/gviz/sensordata/latest?tq={queryString}`|
|:----------------------------------------------------------------|

Where:

| **Element** | **Description** | **Example** |
|:------------|:----------------|:------------|
| `{host}`    | the HTTP prefix of the server hosting the service | http://server.wattdepot.org:8194/wattdepot |
| `{source}`  | the name of a Source resource. | SIM\_HPOWER |
| `{queryString}` | a [Google Visualization API parameter](https://developers.google.com/chart/interactive/docs/dev/implementing_data_source#requestformat) representing a [data source query](https://developers.google.com/chart/interactive/docs/querylanguage) | select%20timePoint%2C%20powerGenerated |

Example latest sensor data URI:

| http://server.wattdepot.org:8194/wattdepot/sources/SIM_HPOWER/gviz/sensordata/latest?tq=select%20timePoint%2C%20powerGenerated |
|:-------------------------------------------------------------------------------------------------------------------------------|

## Power/Energy/Carbon URI ##

Queries for calculated data (power, energy, and carbon) have the following format:

|`{host}/sources/{source}/gviz/calculated?startTime={timestamp}&endTime={timestamp}&samplingInterval={interval}&displaySubsources={boolean}&tq={queryString}`|
|:-----------------------------------------------------------------------------------------------------------------------------------------------------------|

Where:

| **Element** | **Description** | **Example** |
|:------------|:----------------|:------------|
| `{host}`    | the HTTP prefix of the server hosting the service | http://server.wattdepot.org:8194/wattdepot |
| `{source}`  | the name of a Source resource. | SIM\_HPOWER |
| `{timestamp}` | an XML [dateTime](http://www.w3.org/TR/2001/REC-xmlschema-2-20010502/#dateTime) object | 2009-12-01T00:00:00.000-10:00 |
| `{interval}` | an interval in minutes that the range of times will be sampled at | 240         |
| `{boolean}` | a boolean value, either "true" or "false" | "true"      |
| `{queryString}` | a [Google Visualization API parameter](https://developers.google.com/chart/interactive/docs/dev/implementing_data_source#requestformat) representing a [data source query](https://developers.google.com/chart/interactive/docs/querylanguage) | select%20timePoint%2C%20powerGenerated |

Example power URI:

| http://server.wattdepot.org:8194/wattdepot/sources/SIM_HPOWER/gviz/calculated?startTime=2009-12-01T00:00:00.000-10:00&endTime=2009-12-07T00:00:00.000-10:00&samplingInterval=240&displaySubsources=true&tq=select%20timePoint%2C%20powerGenerated |
|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|

Note: the `{queryString}` in this context is a parameter distinct from the HTTP "query string", which is everything that follows after the "?" in the URI. Also, note that the queryString must be [URL-encoded](https://developers.google.com/chart/interactive/docs/querylanguage#Setting_the_Query_in_the_Data_Source_URL).

The displaySubsources parameter is optional, if not specified it will default to false.

# Retrieving Data From Data Source #

The Google Visualization API's core data representation is [JSON](http://en.wikipedia.org/wiki/JSON), the JavaScript Object Notation. This is a lightweight way to represent objects, unlike XML. Google [documents their JSON response format](https://developers.google.com/chart/interactive/docs/dev/implementing_data_source#jsondatatable), which is well worth reading.

Since JSON is just plain text, it is easy to input various URIs to the WattDepot data source and see what you get. For example, the following URI requests 48 hours of powerGeneration data from a particular source:

| http://server.wattdepot.org:8194/wattdepot/sources/SIM_HONOLULU/gviz/calculated?tq=select%20timePoint%2C%20powerGenerated&startTime=2009-12-01T00:00:00.000-10:00&endTime=2009-12-03T00:00:00.000-10:00&samplingInterval=240 |
|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|

Note that we are querying for sampled power data at 240 minute (4 hour) intervals. This is different than asking for 48 hours of sensor data, which would give us all the sensor data collected over that range.

The resulting text from the data source might be something like this:

```
google.visualization.Query.setResponse({status:'ok',
table:{cols:[{id:'timePoint',label:'Date & Time',type:'datetime',pattern:''},
{id:'powerGenerated',label:'Power Gen. (W)',type:'number',pattern:''}],
rows:[{c:[{v:new Date(2009,11,1,0,0,0)},{v:0.0}]},
{c:[{v:new Date(2009,11,1,4,0,0)},{v:0.0}]},
{c:[{v:new Date(2009,11,1,8,0,0)},{v:4.8E7}]},
{c:[{v:new Date(2009,11,1,12,0,0)},{v:1.06E8}]},
{c:[{v:new Date(2009,11,1,16,0,0)},{v:1.06E8}]},
{c:[{v:new Date(2009,11,1,20,0,0)},{v:1.06E8}]},
{c:[{v:new Date(2009,11,2,0,0,0)},{v:0.0}]},
{c:[{v:new Date(2009,11,2,4,0,0)},{v:0.0}]},
{c:[{v:new Date(2009,11,2,8,0,0)},{v:0.0}]},
{c:[{v:new Date(2009,11,2,12,0,0)},{v:3.5E7}]},
{c:[{v:new Date(2009,11,2,16,0,0)},{v:4.1E7}]},
{c:[{v:new Date(2009,11,2,20,0,0)},{v:1.06E8}]},
{c:[{v:new Date(2009,11,3,0,0,0)},{v:0.0}]}]}});
```

(line breaks have been added to the JSON output for readability)

Hopefully, even without experience with JSON, you can see what kind of data has been returned. Technically, the data source emits [JSONP](http://en.wikipedia.org/wiki/JSON#JSONP), which is a method call that sets the response into a variable. You can see the columns of the data table are specified, followed by rows of data. You can play around with the URI and see the raw JSON response. For example, here is an invalid request (where the startTime comes after the endTime):

| http://server.wattdepot.org:8194/wattdepot/sources/SIM_HONOLULU/gviz/calculated?tq=select%20timePoint%2C%20powerGenerated&startTime=2009-12-03T00:00:00.000-10:00&endTime=2009-12-01T00:00:00.000-10:00&samplingInterval=240 |
|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|

which returns this JSON with embedded error message:

```
google.visualization.Query.setResponse({version:'0.6',status:'error',errors:[{reason:'invalid_request',message:'Invalid request',detailed_message:'startTime parameter later than endTime parameter'}]});
```

When working with visualizations, if you find that you are getting strange or unexpected results, try pasting the URI into your browser and check out the raw JSON. This can be very helpful when debugging.

# Graphing WattDepot Data #

Now that we understand the URI scheme and how to retrieve data from the data source, it is time to plug that data into a visualization. The visualization we will be working with is the [Annotated Timeline visualization](https://developers.google.com/chart/interactive/docs/gallery/annotatedtimeline), which can display multiple columns of time-indexed data on the same graph. Google visualizations are accessed using JavaScript embedded into an HTML page. Google provides an [excellent introduction to using visualizations](https://developers.google.com/chart/interactive/docs/), which you should at least skim before continuing this tutorial.

## Basic Graph ##

The basic control flow starts from JavaScript running in a browser, loaded from an HTML file. The JavaScript loads the Google Visualization API library, makes a request to the data source, passes the data to the visualization widget, which then displays the data. Here is an example HTML page:

```
<!DOCTYPE html>
<html>
 <head>
  <title>WattDepot Oscar Charts</title>
  <script type='text/javascript' src='http://www.google.com/jsapi'></script>
  <script type='text/javascript'>
   // Load the Visualization API and Annotated Timeline visualization
   google.load('visualization', '1', {'packages':['annotatedtimeline']});
   
   // Set a callback to run when the API is loaded.
   google.setOnLoadCallback(init);
   
   // Send the query to the data source.
   function init() {
    var query = new google.visualization.Query('http://server.wattdepot.org:8194/wattdepot/sources/SIM_HPOWER/gviz/calculated?startTime=2009-12-01T00:00:00.000-10:00&endTime=2009-12-07T00:00:00.000-10:00&samplingInterval=240');
    query.setQuery('select timePoint, powerGenerated');
    query.send(handleQueryResponse);
   }
   
  // Handle the query response.
  function handleQueryResponse(response) {
   if (response.isError()) {
    alert('Error in query: ' + response.getMessage() + ' ' + response.getDetailedMessage());
    return;
   }

   // Draw the visualization.
   var data = response.getDataTable();
   var chart = new google.visualization.AnnotatedTimeLine(document.getElementById('HPOWER_div'));
   chart.draw(data, {displayAnnotations: false, legendPosition: 'newRow'});
  }
  
  </script>
 </head>

 <body>
  <h1>WattDepot Oscar Charts</h1>
  A chart that shows data generated by <a href="http://code.google.com/p/oscar-project/">OSCAR</a>.
  <p></p>
  <h2>HPOWER output</h2>
  <div id='HPOWER_div' style='width: 700px; height: 400px;'></div>
 </body>
</html>
```

Note that the URI for the data source is given as the parameter to google.visualization.Query, which includes all the WattDepot parameters except for the "tq" parameter that contains the visualization query. Since that parameter often includes characters that need to be URL-encoded (such as " " and ","), we set them using the setQuery call on the following line. The body of the HTML document is at the very bottom, and mostly exists to provide the `<div>` to place the graph into.

Loading the above file in a browser will result in a display something like this:

![http://wattdepot.googlecode.com/svn/wiki/TimelineVisualization01.png](http://wattdepot.googlecode.com/svn/wiki/TimelineVisualization01.png)

For the remaining examples we'll just cover the changes from this template HTML file.

## Visualization Query Parameter ##

Now we cover the query parameter, that you might have been wondering about. In the previous examples, the query parameter was set to `'select timePoint, powerGenerated'`. The Google visualization API uses this query parameter to allow the client to tell the server what data it is interested in. The API provides for an [extensive, SQL-like query language](https://developers.google.com/chart/interactive/docs/querylanguage), most of which is implemented in the server-side Google Visualization [Java library](http://code.google.com/p/google-visualization-java/) (different from the client-side JavaScript library!) that WattDepot uses. The SELECT operator is the only part of the query language that we will cover here.

As you are familiar from the RestApi, there are many types of data that WattDepot can provide about a source. The data source API represents this as a big data table, where each row is information about a particular point in time, and each column represents some value such as power, energy, or carbon. Since we are often interested in only a couple different values per graph, we use the SELECT operator to pick which columns we want in the data table that is sent to the client. This reduces bandwidth usage, and ensures that the server doesn't bother calculating data that the client doesn't want or need.

The example query of `'select timePoint, powerGenerated'` requests the timestamp column (named `timepoint` to avoid conflicts with reserved words) and the `powerGenerated` column. When using the Annotated Timeline visualization, you will always want to select the timePoint column, or you cannot graph the results.

The set of columns that can be queried correspond to the values in the PropertyDictionary. For sensor data the columns are:

| **Column Name** | **Description** |
|:----------------|:----------------|
| `powerConsumed` | power consumed by the source |
| `powerGenerated` | power generated by the source |
| `energyConsumedToDate` | an 'odometer' for energy, see PropertyDictionary for more details |
| `energyGeneratedToDate` | an 'odometer' for energy, see PropertyDictionary for more details |

For the calculated resources, the column names are:

| **Column Name** | **Description** |
|:----------------|:----------------|
| `powerConsumed` | power consumed by the source |
| `powerGenerated` | power generated by the source |
| `energyConsumed` | energy consumed by the source |
| `energyGenerated` | energy generated by the source |
| `carbonEmitted` | carbon emitted by the source |

Note that the powerConsumed and powerGenerated columns can be obtained from either the sensor data
or calculated data.

Using this information, we can change the query string from our example to `'select timePoint, energyGenerated'` to see the energy generated by SIM\_HPOWER, which results in something like this:

![http://wattdepot.googlecode.com/svn/wiki/TimelineVisualization02.png](http://wattdepot.googlecode.com/svn/wiki/TimelineVisualization02.png)

We can plot multiple quantities on the same graph by selecting additional columns. For example, we can select both power and energy generated: `'select timePoint, powerGenerated, energyGenerated'`. This produces a graph with two traces that looks like this:

![http://wattdepot.googlecode.com/svn/wiki/TimelineVisualization03.png](http://wattdepot.googlecode.com/svn/wiki/TimelineVisualization03.png)

## Displaying Subsource Data ##

One thing you might want to do is display the data for a virtual source with the data for all its subsources on the same graph. WattDepot supports this with the "displaySubsources" URI parameter (note, not Google query parameter). If `displaySubsources=true` is present in the URI parameters, then WattDepot will add additional columns for each of the non-virtual subsources of the given virtual source. The name of each column will be prefixed with the subsource's name, such as `SIM_HONOLULU_8`. So for the source `SIM_HONOLULU`, one of the columns would be named `SIM_HONOLULU_8powerGenerated`. Note that the subsource-specific columns are in addition to the column for the virtual source itself.

While displaySubsources will generate these extra columns, the desired columns still have to be selected in the query parameter or they will be filtered out of the data table sent to the client. Unfortunately, this means that the client will need to figure out what the subsource names are, so that they can be used in the select statement of the query string. However, this can be done using the [REST API](http://code.google.com/p/wattdepot/wiki/RestApi#GET_{host}/sources/{source}).

Here is an example using SIM\_HONOLULU:

```
    var query = new google.visualization.Query('http://server.wattdepot.org:8194/wattdepot/sources/SIM_HONOLULU/gviz/calculated?startTime=2009-12-01T00:00:00.000-10:00&endTime=2009-12-07T00:00:00.000-10:00&samplingInterval=240&displaySubsources=true');
    query.setQuery('select timePoint, powerGenerated, SIM_HONOLULU_8powerGenerated, SIM_HONOLULU_9powerGenerated');
```

Which produces a graph with multiple traces:

![http://wattdepot.googlecode.com/svn/wiki/TimelineVisualization04.png](http://wattdepot.googlecode.com/svn/wiki/TimelineVisualization04.png)

You can see how the two Honolulu power plants (orange and red) combine to make the virtual SIM\_HONOLULU source (in blue).

Another example with the Oahu Independent Power Producers (SIM\_IPP):

```
    var query = new google.visualization.Query('http://server.wattdepot.org:8194/wattdepot/sources/SIM_IPP/gviz/calculated?startTime=2009-12-01T00:00:00.000-10:00&endTime=2009-12-07T00:00:00.000-10:00&samplingInterval=240&displaySubsources=true');
    query.setQuery('select timePoint, powerGenerated, SIM_AESpowerGenerated, SIM_HPOWERpowerGenerated, SIM_KALAELOApowerGenerated');
```

![http://wattdepot.googlecode.com/svn/wiki/TimelineVisualization05.png](http://wattdepot.googlecode.com/svn/wiki/TimelineVisualization05.png)

# Caveats #

  * The current WattDepot code is fairly slow when interpolating data. This can be compounded by virtual sources with many subsources (like SIM\_OAHU\_GRID from the OSCAR data set). Therefore it is **highly recommended** that the samplingInterval always be a multiple of the OSCAR data output rate, which is 15 minutes. So a value of 120 minutes will work fine, but 121 minutes will take much much longer (and when using visualizations, you will likely get a timeout error from the widget).
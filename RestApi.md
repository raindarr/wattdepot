

# Introduction #

The WattDepot API accepts sensor data about power usage (or potentially other related types of data), persists it, and makes it available for further analysis and visualization. To expose this functionality, it provides a [REST](http://en.wikipedia.org/wiki/Representational_State_Transfer) API to be used over [HTTP](http://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol). The design of the API follows the Resource Oriented Architecture described in the O'Reilly [RESTful Web Services](http://oreilly.com/catalog/9780596529260/) book by Richardson & Ruby.

In the interest of expediency, the representations served to clients will be custom XML derived from an XML schema. While it would be nice to provide XHTML output that can be navigated using a browser (as the RESTful Web Services book recommends), that can be left for a future version. Adding XHTML output would not require any changes to existing clients since the two representations can exist in parallel.

The following diagram shows the relationships between the resources in the WattDepot REST API:

![http://wattdepot.googlecode.com/svn/wiki/WattDepotAPI-1.1.png](http://wattdepot.googlecode.com/svn/wiki/WattDepotAPI-1.1.png)

The diagram shows two Users A and B. User A has created 3 Sources. The Sources named "Saunders Hall North" and "Saunders Hall South" each correspond to a power meter that monitors half of the power consumption of Saunders Hall. These two sources have generated Sensor Data resources corresponding to power consumption readings from each meter. User A has also created a Source named "Saunders Hall" that aggregates the data from both meters.

User B has created a Source called "Shidler Solar PV" that corresponds to a meter monitoring power production from an array of solar photovoltaic panels. The source has generated Sensor Data resources corresponding to power production readings from the meter.

The next diagram shows how data flows into WattDepot:

![http://wattdepot.googlecode.com/svn/wiki/WattDepotInput-1.0.png](http://wattdepot.googlecode.com/svn/wiki/WattDepotInput-1.0.png)

The diagram shows three example inputs paths for WattDepot. The first path shows data from a solar photovoltaic panel using a [microinverter from Enphase](http://www.enphaseenergy.com/). An Envoy meter from Enphase is recording power production data from the panel. A WattDepot client polls the Envoy meter periodically and then creates Sensor Data resources under the Source corresponding to the meter using HTTP PUTs to the WattDepot server. Similarly, the next two examples show two WattDepot clients polling Obvius Acquisuite meters, one for power consumption data from a building, the other for power generation from a wind turbine.

The final diagram shows how data flows out of WattDepot:

![http://wattdepot.googlecode.com/svn/wiki/WattDepotOutput-1.0.png](http://wattdepot.googlecode.com/svn/wiki/WattDepotOutput-1.0.png)

The diagram shows two example output paths for WattDepot. The first example shows a Google Visualization Time Series Chart of instantaneous power readings from a Source over several days. The chart gadget obtains the data to be displayed through HTTP GET requests to the WattDepot server, using the Google Visualization data source API implemented by WattDepot. The second example shows a hypothetical traffic light visualization that could warn users of a particular grid (from a building to a metropolitan area) when power usage is high, leading to extra high GHG emissions. Ecotricity (a power company in the UK) has such a [live display](http://www.ecotricity.co.uk/about/live-grid-carbon-intensity) on their website.

# Access Control Levels #

WattDepot uses HTTP Basic authentication to support access control. The WattDepot server supports the following types of access control: None, TheUser, SourceOwner, and Admin.

| **Access Control Level** | **Definition** |
|:-------------------------|:---------------|
| None                     | No authentication is required. This corresponds to resources that are intended to be public. |
| TheUser                  | The User specified in the URI must be the same as the User specified in the authentication credentials.  For example, GET `{host}/users/{user}` enforces TheUser access control, which means that if a client submits `{host}/users/foo@example.com`, then the client must also submit valid authentication credentials for `foo@example.com`.  Note that a client authenticated as the Admin user (as specified elsewhere) is also allowed to perform these operations.  For example, if the admin is `admin@example.com`, then a client authenticated as `admin@example.com` would be able to GET `{host}/users/foo@example.com`. |
| SourceOwner              | The authenticated user must be the Source owner. For example, PUT `{host}/sensordata/saunders-floor-3/{timestamp}` requires the authenticated user to be owner of the `saunders-floor-3` Source, or the Admin. |
| Admin                    | The authenticated user must be the Admin (as specified elsewhere).  For example, GET `{host}/users` requires the Admin access control level, as only Administrators are allowed to view the list of all users. |

# Health Resource #

The Health resource represents the health of the WattDepot service. It is provided as a way to determine if the WattDepot service is up and functioning normally (like a "ping" resource).

## Health URI Specification ##

The Health resource is named using the following URI specification:
|`{host}/health`|
|:--------------|

Where:

| **Element** | **Description** | **Example** |
|:------------|:----------------|:------------|
| `{host}`    | the HTTP prefix of the server hosting the service | http://server.wattdepot.org:8182/wattdepot |

Example Health URI:

| http://server.wattdepot.org:8182/wattdepot/health |
|:--------------------------------------------------|

## Health Representation ##

The representation of Health is a string that should be considered completely advisory. It currently responds with "WattDepot is alive.", but could return other strings in the future. It is expected that the HTTP status codes should be used to determine the health of the system, not the representation of the resource.

## HTTP Methods Supported ##

### GET `{host}/health` ###

Access Control Level: **None**

Returns a message about the health of the system. For example,

| GET http://server.wattdepot.org:8182/wattdepot/health |
|:------------------------------------------------------|

Might return:

```
WattDepot is alive.
```

Note that the string returned could change, and should not be parsed in an attempt to determine the health of the system. Rely on the status codes for health status (or the inability to make an HTTP connection to the service).

Possible status codes:

| **[HTTP status code](http://en.wikipedia.org/wiki/List_of_HTTP_status_codes)** | **Meaning** | **Result** |
|:-------------------------------------------------------------------------------|:------------|:-----------|
| 200                                                                            | OK          | The entity-body contains a message about the health of the system |
| 500                                                                            | Internal Server Error | The service is not functioning properly. |



# Source Resource #

There are two kinds of Source resources. Normal or non-virtual Sources represent something that generates sensor data (for example, a power meter). Virtual Sources are aggregations of data from other Sources, and do not generate data themselves. For example, a virtual Source might be a collection of the power meters for all floors of a building, or a collection of power meters from buildings on a campus.

## Source URI Specification ##

Source resources are named using the following URI specification:
|`{host}/sources/{source}`|
|:------------------------|

Where:

| **Element** | **Description** | **Example** |
|:------------|:----------------|:------------|
| `{host}`    | the HTTP prefix of the server hosting the service | http://server.wattdepot.org:8182/wattdepot |
| `{source}`  | the name of a Source resource. | saunders-floor-3 |

Example Source URI:

| http://server.wattdepot.org:8182/wattdepot/sources/saunders-floor-3 |
|:--------------------------------------------------------------------|

## Source Representation ##

The Source representation consists of 7 required fields, then a series of key-value pairs. The first four required fields are required to be non-empty, but the last three can be empty. Additional metadata about Sources can be represented in the key-value pairs. Virtual sources will include the SubSources element that lists the Sources that make up the virtual Source. PropertyDictionary defines the standard properties that are used to represent particular kinds of Sources, and may be expected by higher-level analyses. The representation of each Source resource contains the following fields:

| **field name** | **description** | **example** |
|:---------------|:----------------|:------------|
| Name           | An URL-safe string that uniquely identifies this source. | saunders-floor-3 |
| Owner          | A URI indicating the User that owns this Source. | http://server.wattdepot.org:8182/wattdepot/users/foo@example.com |
| Public         | A boolean indicating whether this Source (and Sensor Data collected from this Source) is to be readable by the public. | true        |
| Virtual        | A boolean indicating whether this is a virtual Source. Virtual Sources must include the SubSources element to list which Sources they draw data from. | false       |
| Coordinates    | A string with comma separated latitude, longitude (in degrees), and altitude (in meters). Can be the empty string if coordinates are unknown. The altitude can also be omitted if unknown. | "21.30078,-157.819129,41" |
| Location       | A string with a human-readable description of the location of this source. | "The third floor of Saunders Hall at the University of Hawaii at Manoa" |
| Description    | A string containing a description of the source. | "Obvius-brand power meter recording electricity usage on the floor." |
| Properties     | A set of key-value string pairs containing additional metadata | {typeOfMeter="consumption"} |
| SubSources     | A list of subsources, used only for virtual sources |             |
To represent a list of Source resources, the SourceIndex element is provided, which consists of SourceRef elements that contain the required metadata for a Source resource. Since Source resources are relatively small, there is also a Sources element that contains a list of full Source elements (rather than SourceRef elements).

The full XML schema can be found here: http://code.google.com/p/wattdepot/source/browse/trunk/xml/schema/source.xsd

## HTTP Methods Supported ##

### GET `{host}/sources` ###

Access Control Level: **None**

For anonymous access, this returns an index of all public Sources in this server as a SourceIndex XML element. If the client is an authenticated user, the index includes all public Sources and all Sources that the authenticated user owns. If the client is the admin user, the index includes all Sources. For example,

| GET http://server.wattdepot.org:8182/wattdepot/sources |
|:-------------------------------------------------------|

Might return:

```
<?xml version="1.0"?>
<SourceIndex>
 <SourceRef Name="saunders-floor-3"
  Owner="http://server.wattdepot.org:8182/wattdepot/users/foo@example.com"
  Public="true"
  Virtual="false"
  Coordinates="21.30078,-157.819129,41"
  Location="The third floor of Saunders Hall at the University of Hawaii at Manoa"
  Description="Obvius-brand power meter recording electricity usage on the floor."
  href="http://server.wattdepot.org:8182/wattdepot/source/saunders-floor-3"/>
 <SourceRef Name="saunders-floor-4"
  Owner="http://server.wattdepot.org:8182/wattdepot/users/foo@example.com"
  Public="true"
  Virtual="false"
  Coordinates="21.30078,-157.819129,46"
  Location="The fourth floor of Saunders Hall at the University of Hawaii at Manoa"
  Description="Obvius-brand power meter recording electricity usage on the floor."
  href="http://server.wattdepot.org:8182/wattdepot/source/saunders-floor-4"/>
 <SourceRef Name="saunders-floor-5"
  Owner="http://server.wattdepot.org:8182/wattdepot/users/foo@example.com"
  Public="true"
  Virtual="false"
  Coordinates="21.30078,-157.819129,51"
  Location="The fifth floor of Saunders Hall at the University of Hawaii at Manoa"
  Description="Obvius-brand power meter recording electricity usage on the floor."
  href="http://server.wattdepot.org:8182/wattdepot/source/saunders-floor-5"/>
</SourceIndex>
```

It is also possible to retrieve the Sources themselves, instead of SourceRefs by using the `fetchAll` query parameter. For example,

| GET http://server.wattdepot.org:8182/wattdepot/sources/?fetchAll=true |
|:----------------------------------------------------------------------|

Might return:

```
<?xml version="1.0"?>
<Sources>
 <Source>
  <Name>saunders-floor-3</Name>
  <Owner>http://server.wattdepot.org:8182/wattdepot/users/foo@example.com</Owner>
  <Public>true</Public>
  <Virtual>false</Virtual>
  <Coordinates>21.30078,-157.819129,41</Coordinates>
  <Location>The third floor of Saunders Hall at the University of Hawaii at Manoa</Location>
  <Description>Obvius-brand power meter recording electricity usage on the floor.</Description>
  <Properties>
    <Property>
      <Key>typeOfMeter</Key>
      <Value>consumption</Value>
    </Property>
  </Properties>
 </Source>
 <Source>
  <Name>saunders-floor-4</Name>
  <Owner>http://server.wattdepot.org:8182/wattdepot/users/foo@example.com</Owner>
  <Public>true</Public>
  <Virtual>false</Virtual>
  <Coordinates>21.30078,-157.819129,46</Coordinates>
  <Location>The fourth floor of Saunders Hall at the University of Hawaii at Manoa</Location>
  <Description>Obvius-brand power meter recording electricity usage on the floor.</Description>
  <Properties>
    <Property>
      <Key>typeOfMeter</Key>
      <Value>consumption</Value>
    </Property>
  </Properties>
 </Source>
 <Source>
  <Name>saunders-floor-5</Name>
  <Owner>http://server.wattdepot.org:8182/wattdepot/users/foo@example.com</Owner>
  <Public>true</Public>
  <Virtual>false</Virtual>
  <Coordinates>21.30078,-157.819129,51</Coordinates>
  <Location>The fifth floor of Saunders Hall at the University of Hawaii at Manoa</Location>
  <Description>Obvius-brand power meter recording electricity usage on the floor.</Description>
  <Properties>
    <Property>
      <Key>typeOfMeter</Key>
      <Value>consumption</Value>
    </Property>
  </Properties>
 </Source>
</Sources>
```

Possible status codes:

| **[HTTP status code](http://en.wikipedia.org/wiki/List_of_HTTP_status_codes)** | **Meaning** | **Result** |
|:-------------------------------------------------------------------------------|:------------|:-----------|
| 200                                                                            | OK          | The entity-body contains a list of all Source resources the user is allowed to access. |
| 401                                                                            | Not Authorized | The client failed to authenticate properly. |
| 500                                                                            | Internal Server Error | The service has encountered an error that prevents it from responding with the requested resource. |

### GET `{host}/sources/{source}` ###

Access Control Level: if `{source}` is public then **None**, if `{source}` is private then **SourceOwner**

Returns a representation of `{source}` as a Source XML element. For example,

| GET http://server.wattdepot.org:8182/wattdepot/sources/saunders-floor-3 |
|:------------------------------------------------------------------------|

Might return:

```
<?xml version="1.0"?>
<Source>
 <Name>saunders-floor-3</Name>
 <Owner>http://server.wattdepot.org:8182/wattdepot/users/foo@example.com</Owner>
 <Public>true</Public>
 <Virtual>false</Virtual>
 <Coordinates>21.30078,-157.819129,41</Coordinates>
 <Location>The third floor of Saunders Hall at the University of Hawaii at Manoa</Location>
 <Description>Obvius-brand power meter recording electricity usage on the floor.</Description>
 <Properties>
   <Property>
     <Key>typeOfMeter</Key>
     <Value>consumption</Value>
   </Property>
 </Properties>
</Source>
```

A virtual Source might look something like this:

```
<?xml version="1.0"?>
<Source>
 <Name>saunders-building</Name>
 <Owner>http://server.wattdepot.org:8182/wattdepot/users/bar@example.com</Owner>
 <Public>true</Public>
 <Virtual>true</Virtual>
 <Coordinates>21.30078,-157.819129,41</Coordinates>
 <Location>Saunders Hall at the University of Hawaii at Manoa</Location>
 <Description>Virtual Source recording all power usage in the building.</Description>
 <SubSources>
   <Href>http://server.wattdepot.org:8182/wattdepot/sources/saunders-floor-1</Href>
   <Href>http://server.wattdepot.org:8182/wattdepot/sources/saunders-floor-2</Href>
   <Href>http://server.wattdepot.org:8182/wattdepot/sources/saunders-floor-3</Href>
 </SubSources>
</Source>
```


Possible status codes:

| **[HTTP status code](http://en.wikipedia.org/wiki/List_of_HTTP_status_codes)** | **Meaning** | **Result** |
|:-------------------------------------------------------------------------------|:------------|:-----------|
| 200                                                                            | OK          | The entity-body contains the requested Source representation. |
| 400                                                                            | Bad Request | The request was ill-formed, perhaps an illegal Source name was specified. |
| 401                                                                            | Not Authorized | The Source resource could not be retrieved because the requested Source is private and the client failed to provide authentication credentials or the authenticated client is not authorized to view the specified Source. |
| 404                                                                            | Not Found   | The specified Source is unknown. |
| 500                                                                            | Internal Server Error | The service has encountered an error that prevents it from responding with the requested resource. |

### GET `{host}/sources/{source}/summary` ###

Access Control Level: if `{source}` is public then **None**, if `{source}` is private then **SourceOwner**

Returns a summary representation of `{source}` as a SourceSummary XML element. The summary includes the interval in which this Source has Sensor Data, and the total number of Sensor Data resources it contains. For example,

| GET http://server.wattdepot.org:8182/wattdepot/sources/saunders-floor-3/summary |
|:--------------------------------------------------------------------------------|

Might return:

```
<?xml version="1.0"?>
<SourceSummary>
 <Href>http://server.wattdepot.org:8182/wattdepot/sources/saunders-floor-3</Href>
 <FirstSensorData>"2009-07-28T09:00:00.000-10:00"</FirstSensorData>
 <LastSensorData>"2009-08-28T09:00:00.000-10:00"</LastSensorData>
 <TotalSensorDatas>2880</TotalSensorDatas>
</SourceSummary>
```

The full XML schema can be found here: http://code.google.com/p/wattdepot/source/browse/trunk/xml/schema/source-summary.xsd

Possible status codes:

| **[HTTP status code](http://en.wikipedia.org/wiki/List_of_HTTP_status_codes)** | **Meaning** | **Result** |
|:-------------------------------------------------------------------------------|:------------|:-----------|
| 200                                                                            | OK          | The entity-body contains the requested SourceSummary representation. |
| 400                                                                            | Bad Request | The request was ill-formed, perhaps an illegal Source name was specified. |
| 401                                                                            | Not Authorized | The Source summary could not be retrieved because the requested Source is private and the client failed to provide authentication credentials or the authenticated client is not authorized to view the specified Source. |
| 404                                                                            | Not Found   | The specified Source is unknown. |
| 500                                                                            | Internal Server Error | The service has encountered an error that prevents it from responding with the requested resource. |

### PUT `{host}/sources/{source}` ###

Access Control Level: **TheUser**

Creates a new Source resource in this server called `{source}` as described in the client-provided representation as a Source XML element. For example,

| PUT http://server.wattdepot.org:8182/wattdepot/sources/saunders-floor-4 |
|:------------------------------------------------------------------------|

Might have this in the entity-body:

```
<?xml version="1.0"?>
<Source>
 <Name>saunders-floor-4</Name>
 <Owner>http://server.wattdepot.org:8182/wattdepot/users/foo@example.com</Owner>
 <Public>true</Public>
 <Virtual>false</Virtual>
 <Coordinates>21.30078,-157.819129,46</Coordinates>
 <Location>The fourth floor of Saunders Hall at the University of Hawaii at Manoa</Location>
 <Description>Obvius-brand power meter recording electricity usage on the floor.</Description>
 <Properties>
   <Property>
     <Key>typeOfMeter</Key>
     <Value>consumption</Value>
   </Property>
 </Properties>
</Source>
```

If the resource already exists, the request will fail, unless the "overwrite=true" parameter is provided (see below). Clients are advised to only provide the overwrite parameter when necessary, to avoid accidentally overwriting Sources. Note that the Name specified in the URI must match the Name specified in the XML entity-body.

| PUT http://server.wattdepot.org:8182/wattdepot/sources/saunders-floor-4?overwrite=true |
|:---------------------------------------------------------------------------------------|

Possible status codes:

| **[HTTP status code](http://en.wikipedia.org/wiki/List_of_HTTP_status_codes)** | **Meaning** | **Result** |
|:-------------------------------------------------------------------------------|:------------|:-----------|
| 201                                                                            | Created     | The provided Source resource has been accepted and stored |
| 400                                                                            | Bad Request | The request was ill-formed, perhaps an illegal Source name was specified or the XML representation provided was ill-formed |
| 401                                                                            | Not Authorized | The Source resource could not be stored because the client failed to provide authentication credentials |
| 409                                                                            | Conflict    | The Source resource specified already exists. If you wish to replace a Source resource, use the overwrite parameter |
| 500                                                                            | Internal Server Error | The service has encountered an error that prevents it from responding with the requested resource. |

### DELETE `{host}/sources/{source}` ###

Access Control Level: **SourceOwner**

Deletes the Source resource in this server called `{source}`. For example,

| DELETE http://server.wattdepot.org:8182/wattdepot/sources/saunders-floor-3 |
|:---------------------------------------------------------------------------|

Will delete the specified Source resource.

Possible status codes:

| **[HTTP status code](http://en.wikipedia.org/wiki/List_of_HTTP_status_codes)** | **Meaning** | **Result** |
|:-------------------------------------------------------------------------------|:------------|:-----------|
| 200                                                                            | OK          | The requested Source resource has been deleted |
| 400                                                                            | Bad Request | The request was ill-formed, perhaps an illegal Source name was specified |
| 401                                                                            | Not Authorized | The Source resource could not be deleted because the client failed to provide authentication credentials, or the authenticated client is not authorized to DELETE the specified Source |
| 404                                                                            | Not Found   | The Source resource could not be deleted because the Source is unknown |
| 500                                                                            | Internal Server Error | The service has encountered an error that prevents it from responding with the requested resource. |



# Sensor Data Resource #

The Sensor Data resource is a single reading from a data source at a particular point in time. The reading might contain multiple types of information (voltage, current, power) from that moment in time. If the data from a Source is visualized as a table of data, then a Sensor Data resource would be one row of the table.

## Sensor Data URI Specification ##

Sensor Data resources are named using the following URI specification:
|`{host}/sources/{source}/sensordata/{timestamp}`|
|:-----------------------------------------------|

Where:

| **Element** | **Description** | **Example** |
|:------------|:----------------|:------------|
| `{host}`    | the HTTP prefix of the server hosting the service | http://server.wattdepot.org:8182/wattdepot |
| `{source}`  | the name of a Source resource. | saunders-floor-3 |
| `{timestamp}` | an XML [dateTime](http://www.w3.org/TR/2001/REC-xmlschema-2-20010502/#dateTime) object | 2009-07-28T09:00:00.000-10:00 |

Example Sensor Data URI:

| http://server.wattdepot.org:8182/wattdepot/sources/saunders-floor-3/sensordata/2009-07-28T09:00:00.000-10:00 |
|:-------------------------------------------------------------------------------------------------------------|

Note: each Sensor Data resource belongs to a Source, and a timestamp uniquely identifies a particular Sensor Data resource from a source.

## Sensor Data Representation ##

The Sensor Data representation consists of 3 required fields, and then a series of key-value pairs. To allow for full flexibility in the types of sensor data collected, domain-specific information should be represented in the key-value pairs. PropertyDictionary defines the standard properties that are used to represent particular kinds of sensor information, and may be expected by higher-level analyses. The representation of each Sensor Data resource contains the following fields:

| **field name** | **description** | **example** |
|:---------------|:----------------|:------------|
| Timestamp      | An XML dateTime indicating the time at which this sensor data was generated. | 2009-07-28T09:00:00.000-10:00  |
| Tool           | A string identifying the tool that generated this sensor data. | BMO-Downloader |
| Source         | A URI indicating the Source that generated this sensor data. | http://server.wattdepot.org:8182/wattdepot/sources/saunders-floor-3 |
| Properties     | A set of key-value string pairs containing domain-specific sensor data and/or meta-data | {powerConsumed=2359,energyConsumedToDate=135187200} |

To represent a list of Sensor Data resources, the SensorDataIndex element is provided, which consists of SensorDataRef elements that contain the metadata for a Sensor Data resource.

The full XML schema can be found here: http://code.google.com/p/wattdepot/source/browse/trunk/xml/schema/sensordata.xsd

## HTTP Methods Supported ##

### GET `{host}/sources/{source}/sensordata` ###

Access Control Level: if `{source}` is public then **None**, if `{source}` is private then **SourceOwner**

Returns an index of all Sensor Data resources in this server generated by `{source}` as a SensorDataIndex XML element. For example,

| GET http://server.wattdepot.org:8182/wattdepot/sources/saunders-floor-3/sensordata |
|:-----------------------------------------------------------------------------------|

Might return:

```
<?xml version="1.0"?>
<SensorDataIndex>
 <SensorDataRef Timestamp="2009-07-28T09:00:00.000-10:00"
  Tool="BMO-Downloader"
  Source="http://server.wattdepot.org:8182/wattdepot/sources/saunders-floor-3"
  href="http://server.wattdepot.org:8182/wattdepot/sources/saunders-floor-3/sensordata/2009-07-28T09:00:00.000-10:00"/>
 <SensorDataRef Timestamp="2009-07-28T09:00:01.000-10:00"
  Tool="BMO-Downloader"
  Source="http://server.wattdepot.org:8182/wattdepot/sources/saunders-floor-3"
  href="http://server.wattdepot.org:8182/wattdepot/sources/saunders-floor-3/sensordata/2009-07-28T09:00:01.000-10:00"/>
 <SensorDataRef Timestamp="2009-07-28T09:00:02.000-10:00"
  Tool="BMO-Downloader"
  Source="http://server.wattdepot.org:8182/wattdepot/sources/saunders-floor-3"
  href="http://server.wattdepot.org:8182/wattdepot/sources/saunders-floor-3/sensordata/2009-07-28T09:00:02.000-10:00"/>
</SensorDataIndex>
```

Note that this request could return a very large XML file.

Possible status codes:

| **[HTTP status code](http://en.wikipedia.org/wiki/List_of_HTTP_status_codes)** | **Meaning** | **Result** |
|:-------------------------------------------------------------------------------|:------------|:-----------|
| 200                                                                            | OK          | The entity-body contains a list of all Sensor Data resources generated by the given Source. |
| 400                                                                            | Bad Request | The request was ill-formed, perhaps an illegal Source name was specified. |
| 401                                                                            | Not Authorized | The SensorData resource could not be retrieved because the Source is private and the client failed to provide authentication credentials or the authenticated client is not authorized to view SensorData from the specified Source. |
| 404                                                                            | Not Found   | The specified Source is unknown. |
| 500                                                                            | Internal Server Error | The service has encountered an error that prevents it from responding with the requested resource. |

### GET `{host}/sources/{source}/sensordata/?startTime={timestamp}&endTime={timestamp}` ###

Access Control Level: if `{source}` is public then **None**, if `{source}` is private then **SourceOwner**

Returns an index of all Sensor Data resources in this server generated by `{source}` that have timestamp equal to or greater than `startTime`, and less than or equal to `endTime` as a SensorDataIndex XML element. For example,

| GET http://server.wattdepot.org:8182/wattdepot/sources/saunders-floor-3/sensordata/?startTime=2009-07-28T09:00:00.000-10:00&endTime=2009-07-29T09:00:00.000-10:00 |
|:------------------------------------------------------------------------------------------------------------------------------------------------------------------|

Might return:

```
<?xml version="1.0"?>
<SensorDataIndex>
 <SensorDataRef Timestamp="2009-07-28T09:00:00.000-10:00"
  Tool="BMO-Downloader"
  Source="http://server.wattdepot.org:8182/wattdepot/sources/saunders-floor-3"
  href="http://server.wattdepot.org:8182/wattdepot/sources/saunders-floor-3/sensordata/2009-07-28T09:00:00.000-10:00"/>
 <SensorDataRef Timestamp="2009-07-28T09:00:01.000-10:00"
  Tool="BMO-Downloader"
  Source="http://server.wattdepot.org:8182/wattdepot/sources/saunders-floor-3"
  href="http://server.wattdepot.org:8182/wattdepot/sources/saunders-floor-3/sensordata/2009-07-28T09:00:01.000-10:00"/>
 <SensorDataRef Timestamp="2009-07-28T09:00:02.000-10:00"
  Tool="BMO-Downloader"
  Source="http://server.wattdepot.org:8182/wattdepot/sources/saunders-floor-3"
  href="http://server.wattdepot.org:8182/wattdepot/sources/saunders-floor-3/sensordata/2009-07-28T09:00:02.000-10:00"/>
</SensorDataIndex>
```

Using `endTime=latest` will include all SensorData after the `startTime` in the returned index.

It is also possible to retrieve the SensorData resources themselves, instead of SensorDataRefs by using the `fetchAll` query parameter. For example,

| GET http://server.wattdepot.org:8182/wattdepot/sources/saunders-floor-3/sensordata/?startTime=2009-07-28T09:00:00.000-10:00&endTime=2009-07-29T09:00:00.000-10:00&fetchAll=true |
|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|

Might return:

```
<?xml version="1.0"?>
<SensorDatas>
 <SensorData>
  <Timestamp>2009-07-28T09:00:00.000-10:00</Timestamp>
  <Tool>BMO-Downloader</Tool>
  <Source>http://server.wattdepot.org:8182/wattdepot/sources/saunders-floor-3</Source>
  <Properties>
    <Property>
      <Key>powerConsumed</Key>
      <Value>2359</Value>
    </Property>
    <Property>
      <Key>energyConsumedToDate</Key>
      <Value>135187200</Value>
    </Property>
  </Properties>
 </SensorData>
 <SensorData>
  <Timestamp>2009-07-28T09:00:01.000-10:00</Timestamp>
  <Tool>BMO-Downloader</Tool>
  <Source>http://server.wattdepot.org:8182/wattdepot/sources/saunders-floor-3</Source>
  <Properties>
    <Property>
      <Key>powerConsumed</Key>
      <Value>1786</Value>
    </Property>
    <Property>
      <Key>energyConsumedToDate</Key>
      <Value>135187201</Value>
    </Property>
  </Properties>
 </SensorData>
 <SensorData>
  <Timestamp>2009-07-28T09:00:02.000-10:00</Timestamp>
  <Tool>BMO-Downloader</Tool>
  <Source>http://server.wattdepot.org:8182/wattdepot/sources/saunders-floor-3</Source>
  <Properties>
    <Property>
      <Key>powerConsumed</Key>
      <Value>3456</Value>
    </Property>
    <Property>
      <Key>energyConsumedToDate</Key>
      <Value>135187203</Value>
    </Property>
  </Properties>
 </SensorData>
</SensorDatas>
```

Possible status codes:

| **[HTTP status code](http://en.wikipedia.org/wiki/List_of_HTTP_status_codes)** | **Meaning** | **Result** |
|:-------------------------------------------------------------------------------|:------------|:-----------|
| 200                                                                            | OK          | The entity-body contains a list of all Sensor Data resources generated by the given Source. |
| 400                                                                            | Bad Request | The request was ill-formed, perhaps an illegal Source name was specified, or one of the timestamps is formatted improperly, or startTime is greater than endTime. |
| 401                                                                            | Not Authorized | The SensorData resource could not be retrieved because the Source is private and the client failed to provide authentication credentials or the authenticated client is not authorized to view SensorData from the specified Source. |
| 404                                                                            | Not Found   | The specified Source is unknown. |
| 500                                                                            | Internal Server Error | The service has encountered an error that prevents it from responding with the requested resource. |


### GET `{host}/sources/{source}/sensordata/{timestamp}` ###

Access Control Level: if `{source}` is public then **None**, if `{source}` is private then **SourceOwner**

Returns a single Sensor Data resource in this server generated by `{source}` with the given `{timestamp}` as a SensorData XML element. For example,

| GET http://server.wattdepot.org:8182/wattdepot/sources/saunders-floor-3/sensordata/2009-07-28T09:00:00.000-10:00 |
|:-----------------------------------------------------------------------------------------------------------------|

Might return:

```
<?xml version="1.0"?>
<SensorData>
 <Timestamp>2009-07-28T09:00:00.000-10:00</Timestamp>
 <Tool>BMO-Downloader</Tool>
 <Source>http://server.wattdepot.org:8182/wattdepot/sources/saunders-floor-3</Source>
 <Properties>
   <Property>
     <Key>powerConsumed</Key>
     <Value>2359</Value>
   </Property>
   <Property>
     <Key>energyConsumedToDate</Key>
     <Value>135187200</Value>
   </Property>
 </Properties>
</SensorData>
```

Possible status codes:

| **[HTTP status code](http://en.wikipedia.org/wiki/List_of_HTTP_status_codes)** | **Meaning** | **Result** |
|:-------------------------------------------------------------------------------|:------------|:-----------|
| 200                                                                            | OK          | The entity-body contains the requested Sensor Data resource |
| 400                                                                            | Bad Request | The request was ill-formed, perhaps an illegal Source name was specified or the timestamp is formatted improperly |
| 401                                                                            | Not Authorized | The SensorData resource could not be retrieved because the Source is private and the client failed to provide authentication credentials or the authenticated client is not authorized to view SensorData from the specified Source. |
| 404                                                                            | Not Found   | The Sensor Data resource could not be found because the Source is unknown or there is no resource corresponding to the requested timestamp. |
| 500                                                                            | Internal Server Error | The service has encountered an error that prevents it from responding with the requested resource. |


### GET `{host}/sources/{source}/sensordata/latest` ###

Access Control Level: if `{source}` is public then **None**, if `{source}` is private then **SourceOwner**

Returns a the latest (most recent) Sensor Data resource in this server generated by `{source}` as a SensorData XML element. This could be used to implement a realtime display that shows the latest sensor data value. For example,

| GET http://server.wattdepot.org:8182/wattdepot/sources/saunders-floor-3/sensordata/latest |
|:------------------------------------------------------------------------------------------|

Might return:

```
<?xml version="1.0"?>
<SensorData>
 <Timestamp>2009-07-28T09:00:00.000-10:00</Timestamp>
 <Tool>BMO-Downloader</Tool>
 <Source>http://server.wattdepot.org:8182/wattdepot/sources/saunders-floor-3</Source>
 <Properties>
   <Property>
     <Key>powerConsumed</Key>
     <Value>2359</Value>
   </Property>
   <Property>
     <Key>energyConsumedToDate</Key>
     <Value>135187200</Value>
   </Property>
 </Properties>
</SensorData>
```

Note that unlike other Sensor Data API methods, this one provides data even for virtual sources. For a virtual source, the latest sensordata values for each subsource are retrieved and then the values for each property present are summed to return one Sensor Data resource. Since the last data value from each subsource may be from different timestamps, the latest value from a virtual source is an approximation. The timestamp for the returned Sensor Data resource will be equal to the most recent of the subsources latest sensor data resources.

Possible status codes:

| **[HTTP status code](http://en.wikipedia.org/wiki/List_of_HTTP_status_codes)** | **Meaning** | **Result** |
|:-------------------------------------------------------------------------------|:------------|:-----------|
| 200                                                                            | OK          | The entity-body contains the requested Sensor Data resource |
| 400                                                                            | Bad Request | The request was ill-formed, perhaps an illegal Source name was specified |
| 401                                                                            | Not Authorized | The SensorData resource could not be retrieved because the Source is private and the client failed to provide authentication credentials or the authenticated client is not authorized to view SensorData from the specified Source. |
| 404                                                                            | Not Found   | The Sensor Data resource could not be found because the Source is unknown or the source has no Sensor Data. |
| 500                                                                            | Internal Server Error | The service has encountered an error that prevents it from responding with the requested resource. |


### PUT `{host}/sources/{source}/sensordata/{timestamp}` ###

Access Control Level: **SourceOwner**

Creates a single Sensor Data resource in this server generated by `{source}` with the given `{timestamp}` as described in the client-provided representation as a SensorData XML element. For example,

| PUT http://server.wattdepot.org:8182/wattdepot/sources/saunders-floor-3/sensordata/2009-07-28T09:00:00.000-10:00 |
|:-----------------------------------------------------------------------------------------------------------------|

Might have this in the entity-body:

```
<?xml version="1.0"?>
<SensorData>
 <Timestamp>2009-07-28T09:00:00.000-10:00</Timestamp>
 <Tool>BMO-Downloader</Tool>
 <Source>http://server.wattdepot.org:8182/wattdepot/sources/saunders-floor-3</Source>
 <Properties>
   <Property>
     <Key>powerConsumed</Key>
     <Value>2359</Value>
   </Property>
   <Property>
     <Key>energyConsumedToDate</Key>
     <Value>135187200</Value>
   </Property>
 </Properties>
</SensorData>
```

If the resource already exists, the request will fail. To change a Sensor Data resource, DELETE the resource first, then PUT. Note that the timestamp specified in the URI must match the timestamp specified in the XML entity-body.

Possible status codes:

| **[HTTP status code](http://en.wikipedia.org/wiki/List_of_HTTP_status_codes)** | **Meaning** | **Result** |
|:-------------------------------------------------------------------------------|:------------|:-----------|
| 201                                                                            | Created     | The provided Sensor Data resource has been accepted and stored |
| 400                                                                            | Bad Request | The request was ill-formed, perhaps an illegal Source name was specified, the timestamp was formatted improperly, or the XML representation provided was ill-formed |
| 401                                                                            | Not Authorized | The SensorData resource could not be stored because the client failed to provide authentication credentials or the authenticated client is not authorized to PUT SensorData for the specified Source. |
| 404                                                                            | Not Found   | The Sensor Data resource could not be stored because the Source is unknown. |
| 409                                                                            | Conflict    | The Sensor Data resource specified already exists. If you wish to replace a Sensor Data resource, DELETE it first, then PUT the new resource |
| 500                                                                            | Internal Server Error | The service has encountered an error that prevents it from responding with the requested resource. |

### DELETE `{host}/sources/{source}/sensordata/{timestamp}` ###

Access Control Level: **SourceOwner**

Deletes the Sensor Data resource generated by `{source}` with the given `{timestamp}`. For example,

| DELETE http://server.wattdepot.org:8182/wattdepot/sources/saunders-floor-3/sensordata/2009-07-28T09:00:00.000-10:00 |
|:--------------------------------------------------------------------------------------------------------------------|

Will delete the specified Sensor Data resource.

Possible status codes:

| **[HTTP status code](http://en.wikipedia.org/wiki/List_of_HTTP_status_codes)** | **Meaning** | **Result** |
|:-------------------------------------------------------------------------------|:------------|:-----------|
| 200                                                                            | OK          | The requested Sensor Data resource has been deleted |
| 400                                                                            | Bad Request | The request was ill-formed, perhaps an illegal Source name was specified, or the timestamp was formatted improperly |
| 401                                                                            | Not Authorized | The SensorData resource could not be deleted because the client failed to provide authentication credentials or the authenticated client is not authorized to DELETE SensorData from the specified Source. |
| 404                                                                            | Not Found   | The Sensor Data resource could not be deleted because the Source is unknown or there is no resource corresponding to the requested timestamp. |
| 500                                                                            | Internal Server Error | The service has encountered an error that prevents it from responding with the requested resource. |

### DELETE `{host}/sources/{source}/sensordata/all` ###

Access Control Level: **SourceOwner**

Deletes all the SensorData resources generated by `{source}`. For example,

| DELETE http://server.wattdepot.org:8182/wattdepot/sources/saunders-floor-3/sensordata/all |
|:------------------------------------------------------------------------------------------|

will delete all SensorData resources under the given source.

Possible status codes:

| **[HTTP status code](http://en.wikipedia.org/wiki/List_of_HTTP_status_codes)** | **Meaning** | **Result** |
|:-------------------------------------------------------------------------------|:------------|:-----------|
| 200                                                                            | OK          | The requested SensorData resources have been deleted |
| 400                                                                            | Bad Request | The request was ill-formed, perhaps an illegal Source name was specified |
| 401                                                                            | Not Authorized | The SensorData resource could not be deleted because the client failed to provide authentication credentials or the authenticated client is not authorized to DELETE SensorData from the specified Source. |
| 404                                                                            | Not Found   | The Sensor Data resource could not be deleted because the Source is unknown, or there are no SensorData resources. |
| 500                                                                            | Internal Server Error | The service has encountered an error that prevents it from responding with the requested resource. |


# Power Resource #

The Power resource represents the power generated or consumed by a particular source at a particular point in time. Unlike Sensor Data, the Power resource can be sampled at any time within the range of stored Sensor Data for the Source. If the timestamp requested does not correspond to any Sensor Data, the returned value will be linearly interpolated from the Sensor Data resources on either side of the requested timestamp. If the value is interpolated, the property "interpolated" will be set to "true" (see PropertyDictionary).
## Power URI Specification ##

Power resources are named using the following URI specification: |`{host}/sources/{source}/power/{timestamp}`|
|:------------------------------------------|

Where:

| **Element** | **Description** | **Example** |
|:------------|:----------------|:------------|
| `{host}`    | the HTTP prefix of the server hosting the service | http://server.wattdepot.org:8182/wattdepot |
| `{source}`  | the name of a Source resource. | saunders-floor-3 |
| `{timestamp}` | an XML [dateTime](http://www.w3.org/TR/2001/REC-xmlschema-2-20010502/#dateTime) object | 2009-07-28T09:00:00.000-10:00 |

Example Power URI:

| http://server.wattdepot.org:8182/wattdepot/sources/saunders-floor-3/power/2009-07-28T09:00:00.000-10:00 |
|:--------------------------------------------------------------------------------------------------------|

Note: each Power resource belongs to a Source, and a timestamp uniquely identifies a particular Power resource from a source.

## Power Representation ##

The Power resource uses the same representation as the Sensor Data resource (see above).

The full XML schema can be found here: http://code.google.com/p/wattdepot/source/browse/trunk/xml/schema/sensordata.xsd

## HTTP Methods Supported ##

### GET `{host}/sources/{source}/power/{timestamp}` ###

Access Control Level: if `{source}` is public then **None**, if `{source}` is private then **SourceOwner**

Returns a single Power resource in this server from `{source}` with the given `{timestamp}` as a SensorData XML element. For example,

| GET http://server.wattdepot.org:8182/wattdepot/sources/SIM_AES/power/2009-07-28T09:07:00.000-10:00 |
|:---------------------------------------------------------------------------------------------------|

Might return:

```
<?xml version="1.0"?>
<SensorData>
 <Timestamp>2009-07-28T09:07:00.000-10:00</Timestamp>
 <Tool>OscarDataConverter</Tool>
 <Source>http://server.wattdepot.org:8182/wattdepot/sources/SIM_AES</Source>
 <Properties>
   <Property>
     <Key>powerGenerated</Key>
     <Value>1.8E8</Value>
   </Property>
   <Property>
     <Key>powerConsumed</Key>
     <Value>0.0</Value>
   </Property>
   <Property>
     <Key>interpolated</Key>
     <Value>true</Value>
   </Property>
 </Properties>
</SensorData>
```

Currently the two power-related properties are powerGenerated and powerConsumed. The SensorData object returned will include both properties, but one or both may be zero. Thus a source that only generates power would always return SensorData that has the powerConsumed property set to zero.

Possible status codes:

| **[HTTP status code](http://en.wikipedia.org/wiki/List_of_HTTP_status_codes)** | **Meaning** | **Result** |
|:-------------------------------------------------------------------------------|:------------|:-----------|
| 200                                                                            | OK          | The entity-body contains the requested Power resource |
| 400                                                                            | Bad Request | The request was ill-formed, perhaps an illegal Source name was specified or the timestamp is formatted improperly |
| 404                                                                            | Not Found   | The Power resource could not be found because: the Source is unknown, there is no resource corresponding to the requested timestamp (outside of valid range), the requested Source is private and the client failed to provide authentication credentials, or the authenticated client is not authorized to view the requested Power |
| 500                                                                            | Internal Server Error | The service has encountered an error that prevents it from responding with the requested resource. |



# Energy Resource #

The Energy resource represents the energy generated or consumed by a particular source over a range of time specified by two timestamps. Like the Power resource, Energy can be sampled at any time within the range of stored Sensor Data for the Source via linear interpolation, so the start and end timestamps do not need to correspond to sensor data timestamps. Some Sources may only generate sensor data about power, in which case the energy data returned will be computed by sampling the power data at the provided sampling interval (or a default interval). Other Sources may record explicit sensor data about energy (such as a counter that is incremented for each watt-hour that passes through the meter), in which case the energy returned may be computed by finding the difference between the (possibly interpolated) energy values at the start and end timestamps. If the value is interpolated, the property "interpolated" will be set to "true" (see PropertyDictionary).

## Energy URI Specification ##

Energy resources are named using the following URI specification: |`{host}/sources/{source}/energy/?startTime={timestamp}&endTime={timestamp}&samplingInterval={interval}`|
|:------------------------------------------------------------------------------------------------------|

Where:

| **Element** | **Description** | **Example** |
|:------------|:----------------|:------------|
| `{host}`    | the HTTP prefix of the server hosting the service | http://server.wattdepot.org:8182/wattdepot |
| `{source}`  | the name of a Source resource. | saunders-floor-3 |
| `{timestamp}` | an XML [dateTime](http://www.w3.org/TR/2001/REC-xmlschema-2-20010502/#dateTime) object | 2009-07-28T09:00:00.000-10:00 |
| `{interval}` | an integer specifying the desired sampling interval in minutes | 60          |

Example Energy URI:

| http://server.wattdepot.org:8182/wattdepot/sources/SIM_KAHE_2/energy/?startTime=2009-07-28T09:00:00.000-10:00&endTime=2009-07-29T09:00:00.000-10:00&samplingInterval=60 |
|:------------------------------------------------------------------------------------------------------------------------------------------------------------------------|

Note: each Energy resource belongs to a Source, and the samplingInterval parameter is optional.

## Energy Representation ##

The Energy resource uses the same representation as the Sensor Data resource (see above). This is a little forced as Energy is always computed over a range, while a Sensor Data resource corresponds to a point in time. Perhaps in the future a separate representation will be used for Energy resources.

The full XML schema can be found here: http://code.google.com/p/wattdepot/source/browse/trunk/xml/schema/sensordata.xsd

## HTTP Methods Supported ##

### GET `{host}/sources/{source}/energy/?startTime={timestamp}&endTime={timestamp}&samplingInterval={interval}` ###

Access Control Level: if `{source}` is public then **None**, if `{source}` is private then **SourceOwner**

Returns a single Energy resource in this server from `{source}` over range specified by `startTime` and `endTime` as a SensorData XML element. Using `endTime=latest` will perform the energy calculation from the given `startTime` to the latest received SensorData (which is usually older than the current time).

The optional `samplingInterval` parameter can be included to indicate how often power should be sampled in the event that no explicit energy sensor data is available. If `samplingInterval` is not specified, then a default will be used equal to the larger of: 1/10 of the range, or 1 minute. Since most meters provide energy counters, the sampling interval is rarely needed and should be left off of queries.

For example,

| http://server.wattdepot.org:8182/wattdepot/sources/SIM_KAHE_2/energy/?startTime=2009-07-28T09:00:00.000-10:00&endTime=2009-07-29T09:00:00.000-10:00&samplingInterval=60 |
|:------------------------------------------------------------------------------------------------------------------------------------------------------------------------|

Might return:

```
<?xml version="1.0"?>
<SensorData>
 <Timestamp>2009-07-28T09:00:00.000-10:00</Timestamp>
 <Tool>OscarDataConverter</Tool>
 <Source>http://server.wattdepot.org:8182/wattdepot/sources/SIM_KAHE_2</Source>
 <Properties>
   <Property>
     <Key>energyGenerated</Key>
     <Value>1.55E13</Value>
   </Property>
   <Property>
     <Key>energyConsumed</Key>
     <Value>0.0</Value>
   </Property>
   <Property>
     <Key>interpolated</Key>
     <Value>true</Value>
   </Property>
 </Properties>
</SensorData>
```

Currently the two energy-related properties are energyGenerated and energyConsumed. The SensorData object returned will include both properties, but one or both may be zero. Thus a source that only generates energy would always return SensorData that has the energyConsumed property set to zero.

Possible status codes:

| **[HTTP status code](http://en.wikipedia.org/wiki/List_of_HTTP_status_codes)** | **Meaning** | **Result** |
|:-------------------------------------------------------------------------------|:------------|:-----------|
| 200                                                                            | OK          | The entity-body contains the energy from the given source. |
| 400                                                                            | Bad Request | The request was ill-formed, perhaps an illegal Source name was specified, or one of the timestamps is formatted improperly, or startTime is greater than endTime, or samplingInterval is formatted improperly, or samplingInterval is greater than the range from startTime to endTime. |
| 404                                                                            | Not Found   | The specified Source is unknown, or the requested Sensor Data is private and the client failed to provide authentication credentials, or the authenticated client is not authorized to view the requested Sensor Data from this Source |
| 500                                                                            | Internal Server Error | The service has encountered an error that prevents it from responding with the requested resource. |



# Carbon Resource #

The Carbon resource represents the GHG emitted by a particular source over a range of time specified by two timestamps. The carbon emitted by a source is determined by computing the energy generated by the source and multiplying it by the carbon intensity of the resource as specified by the "carbonIntensity" property of the Source. Like the Energy resource, Carbon can be sampled at any time within the range of stored Sensor Data for the Source via linear interpolation, so the start and end timestamps do not need to correspond to sensor data timestamps. For more information, see the Energy resource. If the value is interpolated, the property "interpolated" will be set to "true" (see PropertyDictionary).

## Carbon URI Specification ##

Carbon resources are named using the following URI specification: |`{host}/sources/{source}/carbon/?startTime={timestamp}&endTime={timestamp}&samplingInterval={interval}`|
|:------------------------------------------------------------------------------------------------------|

Where:

| **Element** | **Description** | **Example** |
|:------------|:----------------|:------------|
| `{host}`    | the HTTP prefix of the server hosting the service | http://server.wattdepot.org:8182/wattdepot |
| `{source}`  | the name of a Source resource. | saunders-floor-3 |
| `{timestamp}` | an XML [dateTime](http://www.w3.org/TR/2001/REC-xmlschema-2-20010502/#dateTime) object | 2009-07-28T09:00:00.000-10:00 |
| `{interval}` | an integer specifying the desired sampling interval in minutes | 60          |

Example Carbon URI:

| http://server.wattdepot.org:8182/wattdepot/sources/SIM_KAHE_2/carbon/?startTime=2009-07-28T09:00:00.000-10:00&endTime=2009-07-29T09:00:00.000-10:00&samplingInterval=60 |
|:------------------------------------------------------------------------------------------------------------------------------------------------------------------------|

Note: each Carbon resource belongs to a Source, and the samplingInterval parameter is optional.

## Carbon Representation ##

The Carbon resource uses the same representation as the Sensor Data resource (see above). This is a little forced as Carbon is always computed over a range, while a Sensor Data resource corresponds to a point in time. Perhaps in the future a separate representation will be used for Carbon resources.

The full XML schema can be found here: http://code.google.com/p/wattdepot/source/browse/trunk/xml/schema/sensordata.xsd

## HTTP Methods Supported ##

### GET `{host}/sources/{source}/carbon/?startTime={timestamp}&endTime={timestamp}&samplingInterval={interval}` ###

Access Control Level: if `{source}` is public then **None**, if `{source}` is private then **SourceOwner**

Returns a single Carbon resource in this server from `{source}` over range specified by `startTime` and `endTime` as a SensorData XML element. Using `endTime=latest` will perform the carbon calculation from the given `startTime` to the latest received SensorData (which is usually older than the current time).

The optional `samplingInterval` parameter can be included to indicate how often power should be sampled in the event that no explicit energy sensor data is available. If `samplingInterval` is not specified, then a default will be used equal to the larger of: 1/10 of the range, or 1 minute. Since most meters provide energy counters, the sampling interval is rarely needed and should be left off of queries. For example:

| http://server.wattdepot.org:8182/wattdepot/sources/SIM_KAHE_2/carbon/?startTime=2009-07-28T09:00:00.000-10:00&endTime=2009-07-29T09:00:00.000-10:00&samplingInterval=60 |
|:------------------------------------------------------------------------------------------------------------------------------------------------------------------------|

Might return:

```
<?xml version="1.0"?>
<SensorData>
 <Timestamp>2009-07-28T09:00:00.000-10:00</Timestamp>
 <Tool>OscarDataConverter</Tool>
 <Source>http://server.wattdepot.org:8182/wattdepot/sources/SIM_KAHE_2</Source>
 <Properties>
   <Property>
     <Key>carbonEmitted</Key>
     <Value>2.7032E10</Value>
   </Property>
   <Property>
     <Key>interpolated</Key>
     <Value>true</Value>
   </Property>
 </Properties>
</SensorData>
```

Unlike the power and energy resources, currently there is only a carbonEmitted property. This reflects the fact that in the world of electricity, carbon is emitted but rarely consumed. If carbon capture and sequestration or biomass growth sensors become common, then WattDepot can easily be extended to support a carbonConsumed property.

Possible status codes:

| **[HTTP status code](http://en.wikipedia.org/wiki/List_of_HTTP_status_codes)** | **Meaning** | **Result** |
|:-------------------------------------------------------------------------------|:------------|:-----------|
| 200                                                                            | OK          | The entity-body contains the energy from the given source. |
| 400                                                                            | Bad Request | The request was ill-formed, perhaps an illegal Source name was specified, or one of the timestamps is formatted improperly, or startTime is greater than endTime, or samplingInterval is formatted improperly, or samplingInterval is greater than the range from startTime to endTime. |
| 404                                                                            | Not Found   | The specified Source is unknown, or the requested Sensor Data is private and the client failed to provide authentication credentials, or the authenticated client is not authorized to view the requested Sensor Data from this Source |
| 500                                                                            | Internal Server Error | The service has encountered an error that prevents it from responding with the requested resource. |



# User Resource #

The User resource specifies a particular agent in the system. Once created, the credentials can be used to authenticate to WattDepot. Most operations in WattDepot are intended to be run by anonymous clients, so many clients will never make use of the User resource. Authenticated users can create Sources and create Sensor Data resources under those Sources. A User might be created for each entity responsible for one or more related Sources.

## User URI Specification ##

User resources are named using the following URI specification:
|`{host}/users/{user}`|
|:--------------------|

Where:

| **Element** | **Description** | **Example** |
|:------------|:----------------|:------------|
| `{host}`    | the HTTP prefix of the server hosting the service | http://server.wattdepot.org:8182/wattdepot |
| `{user}`    | a unique name for the user, expressed as an email address | foo@example.com |

Example User URI:

| http://server.wattdepot.org:8182/wattdepot/users/foo@example.com |
|:-----------------------------------------------------------------|

## User Representation ##

The User representation consists of 3 fields, and then a series of key-value pairs:

| **field name** | **description** | **example** |
|:---------------|:----------------|:------------|
| Email          | a string containing a unique name for the user, expressed as an email address. | foo@example.com  |
| Password       | A string containing the password for the user. | Sooper2:SekriT |
| Admin          | A boolean value indicating whether the user is an administrator. | false       |
| Properties     | A set of key-value string pairs containing domain-specific sensor data and/or meta-data | {sendNewsletter=false} |

Note that the password field may be used by the client to send a password to the server, but when the User representation is sent from server to the client, it will always be empty for security purposes. Conversely, the Admin field is ignored if sent by the client, but is populated by the server based on out-of-band configuration.

To represent a list of User resources, the UserIndex element is provided, which consists of UserRef elements that contain the metadata for a User resource.

The full XML schema can be found here: http://code.google.com/p/wattdepot/source/browse/trunk/xml/schema/user.xsd

## HTTP Methods Supported ##

Note that at present the creation of Users must be done by the admin user, and there is no provision for changing passwords other than DELETE followed by a PUT. For the current scope of WattDepot, there should only be a handful of users, so creating them by hand is not a problem. If there was the need to create many users, or have user creation and password resets be self-service, then additional methods should be implemented.

### GET `{host}/users` ###

Access Control Level: **Admin**

Returns an index of all User resources on this server as a UserIndex XML element. For example,

| GET http://server.wattdepot.org:8182/wattdepot/users |
|:-----------------------------------------------------|

Might return:

```
<?xml version="1.0"?>
<UserIndex>
 <UserRef Email="foo@example.com"
  Href="http://server.wattdepot.org:8182/wattdepot/users/foo@example.com"
 />
 <UserRef Email="admin@example.com"
  Href="http://server.wattdepot.org:8182/wattdepot/users/admin@example.com"
 />
</UserIndex>
```

Possible status codes:

| **[HTTP status code](http://en.wikipedia.org/wiki/List_of_HTTP_status_codes)** | **Meaning** | **Result** |
|:-------------------------------------------------------------------------------|:------------|:-----------|
| 200                                                                            | OK          | The entity-body contains a list of all User resources. |
| 401                                                                            | Not Authorized | The client failed to provide admin authentication credentials. |
| 500                                                                            | Internal Server Error | The service has encountered an error that prevents it from responding with the requested resource. |

### GET `{host}/users/{user}` ###

Access Control Level: **TheUser**

Returns a representation of the requested User as a User XML element. For example,

| GET http://server.wattdepot.org:8182/wattdepot/users/foo@example.com |
|:---------------------------------------------------------------------|

Might return:

```
<?xml version="1.0"?>
<User>
 <Email>foo@example.com</Email>
 <Password></Password>
 <Admin>false</Admin>
 <Properties>
   <Property>
     <Key>sendNewsletter</Key>
     <Value>false</Value>
   </Property>
 </Properties>
</User>
```

Possible status codes:

| **[HTTP status code](http://en.wikipedia.org/wiki/List_of_HTTP_status_codes)** | **Meaning** | **Result** |
|:-------------------------------------------------------------------------------|:------------|:-----------|
| 200                                                                            | OK          | The entity-body contains the requested User resource |
| 400                                                                            | Bad Request | The request was ill-formed, perhaps an illegal User name was specified |
| 404                                                                            | Not Found   | The User resource could not be found because: the User is unknown, the client failed to provide authentication credentials, or the authenticated client is not authorized to view the requested User |
| 500                                                                            | Internal Server Error | The service has encountered an error that prevents it from responding with the requested resource. |

### HEAD `{host}/users/{user}` ###

Access Control Level: **TheUser**

Provides a lightweight means for a higher-level service to determine if authentication credentials for a given user are valid. For example,

| HEAD http://server.wattdepot.org:8182/wattdepot/users/foo@example.com |
|:----------------------------------------------------------------------|

Might return a 200 status code, indicating that the provided credentials for foo@example.com are valid.

Possible status codes:

| **[HTTP status code](http://en.wikipedia.org/wiki/List_of_HTTP_status_codes)** | **Meaning** | **Result** |
|:-------------------------------------------------------------------------------|:------------|:-----------|
| 200                                                                            | OK          | The provided credentials are valid |
| 400                                                                            | Bad Request | The request was ill-formed, perhaps an illegal User name was specified |
| 404                                                                            | Not Found   | The User resource could not be found because: the User is unknown, the client failed to provide authentication credentials, or the authenticated client is not authorized to view the requested User |
| 500                                                                            | Internal Server Error | The service has encountered an error that prevents it from responding with the requested resource. |

### PUT `{host}/users/{user}` ###

Access Control Level: **Admin**

Creates a User resource as described in the client-provided representation as a User XML element. For example,

| PUT http://server.wattdepot.org:8182/wattdepot/users/bar@example.com |
|:---------------------------------------------------------------------|

Might have this in the entity-body:

```
<?xml version="1.0"?>
<User>
 <Email>bar@example.com</Email>
 <Password>DubblePlusSeekrat</Password>
 <Admin></Admin>
 <Properties>
   <Property>
     <Key>sendNewsletter</Key>
     <Value>true</Value>
   </Property>
 </Properties>
</User>
```

If the resource already exists, the request will fail. To change a User resource, DELETE the resource first, then PUT. Note that the Email specified in the URI must match the Email specified in the XML entity-body.

Possible status codes:

| **[HTTP status code](http://en.wikipedia.org/wiki/List_of_HTTP_status_codes)** | **Meaning** | **Result** |
|:-------------------------------------------------------------------------------|:------------|:-----------|
| 201                                                                            | Created     | The provided User resource has been accepted and stored |
| 400                                                                            | Bad Request | The request was ill-formed, perhaps an illegal User name was specified, or the XML representation provided was ill-formed. |
| 404                                                                            | Not Found   | The User resource could not be stored because: the client failed to provide authentication credentials, or the authenticated client is not authorized to PUT. |
| 409                                                                            | Conflict    | The User resource specified already exists. If you wish to replace a User resource, DELETE it first, then PUT the new resource |
| 500                                                                            | Internal Server Error | The service has encountered an error that prevents it from responding with the requested resource. |

### DELETE `{host}/users/{user}` ###

Access Control Level: **Admin**

Deletes the User resource specified. For example,

| DELETE http://server.wattdepot.org:8182/wattdepot/users/bar@example.com |
|:------------------------------------------------------------------------|

Will delete the specified User resource.

Possible status codes:

| **[HTTP status code](http://en.wikipedia.org/wiki/List_of_HTTP_status_codes)** | **Meaning** | **Result** |
|:-------------------------------------------------------------------------------|:------------|:-----------|
| 200                                                                            | OK          | The requested User resource has been deleted |
| 400                                                                            | Bad Request | The request was ill-formed, perhaps an illegal User name was specified |
| 404                                                                            | Not Found   | The Sensor Data resource could not be deleted because: the User is unknown, the client failed to provide authentication credentials, or the authenticated client is not authorized to DELETE data for the specified User |
| 500                                                                            | Internal Server Error | The service has encountered an error that prevents it from responding with the requested resource. |



# Database Resource #

The Database resource provides certain administrative access to the underlying persistance for the other resources. It only needs to be used by administrators.

## Database URI Specification ##

The Database resources are named using the following URI specification:
|`{host}/db`|
|:----------|

Where:

| **Element** | **Description** | **Example** |
|:------------|:----------------|:------------|
| `{host}`    | the HTTP prefix of the server hosting the service | http://server.wattdepot.org:8182/wattdepot |

Example Database URI:

| http://server.wattdepot.org:8182/wattdepot/db |
|:----------------------------------------------|

## Database Representation ##

The Database resource does not have any representation at this time, all methods are used for manipulating the database rather than retrieving data.

## HTTP Methods Supported ##

### PUT `{host}/db/snapshot` ###

Access Control Level: **Admin**

Takes a snapshot of the database and stores it in a pre-specified location on the server. This is needed so that consistent backups can be made of the database. For example,

| GET http://server.wattdepot.org:8182/wattdepot/db/snapshot |
|:-----------------------------------------------------------|

Might copy the contents of the database and store it in a timestamped snapshot directory on the server.

Possible status codes:

| **[HTTP status code](http://en.wikipedia.org/wiki/List_of_HTTP_status_codes)** | **Meaning** | **Result** |
|:-------------------------------------------------------------------------------|:------------|:-----------|
| 201                                                                            | Created     | The snapshot was created successfully. |
| 401                                                                            | Not Authorized | The client failed to provide admin authentication credentials. |
| 500                                                                            | Internal Server Error | The service has encountered an error that prevents it from responding with the requested resource. |
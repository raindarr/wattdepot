Note that the full API description is on another page: RestApi

# Introduction #

WattDepot accepts sensor data about power usage, persists it, and makes it available for further analysis and visualization. To expose this functionality, it provides a [REST](http://en.wikipedia.org/wiki/Representational_State_Transfer) API to be used over [HTTP](http://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol). The design of the API follows the Resource Oriented Architecture described in the O'Reilly [RESTful Web Services](http://oreilly.com/catalog/9780596529260/) book by Richardson & Ruby.

We start by laying out an informal description of the requirements of the WattDepot service, and then use the step-by-step technique for designing ROA web services in chapter 6 of the RESTful Web Services book. We then end with a methodical description of the WattDepot API, in the style used by the [Hackystat](http://code.google.com/p/hackystat/) [Sensorbase](http://code.google.com/p/hackystat-sensorbase-uh/) service.

# WattDepot Requirements #

The motivation for the WattDepot system is to collect power utilization data into a centralized system so that it can be used from other tools (for example, gadgets from the [Google Visualization API](http://code.google.com/apis/visualization/)). The initial source of power data will be power meters from [Obvius](http://www.obvius.com/) that have been installed on campus, but WattDepot should be flexible enough to accept data from a wide range of other sources, such as whole-home energy meters (like [The Energy Detective](http://www.theenergydetective.com/index.html), data from utilities (like this data on the [real-time carbon intensity of the UK power grid](http://www.earth.org.uk/_gridCarbonIntensityGB.html)), data from individual appliances (such as from a [Tweet-A-Watt](http://www.ladyada.net/make/tweetawatt/)), data on power generation from renewables, or power storage.

The data will be collected from a variety of data sources, potentially in different administrative realms. This means there must be a way for entities that do not trust the maintainer of the public WattDepot service to provide their data without providing any access to their monitoring equipment directly. An entity should be able to represent the data sources (like power meters) they are collecting data from, and annotate them with metadata (such as the type of device, the physical location, etc) for future reference. Only the owner of a meter should be allowed to upload data for that meter, and only the owner should be able to change or delete data for their meters. Hopefully, most of the data will be public and available for retrieval without requiring any authentication, but the system should provide for data that is kept private to the entity that collected it.

The structure of the power data to be collected may vary depending on the source of the data. Following the [Hackystat 8](http://code.google.com/p/hackystat-sensorbase-uh/wiki/RestApiSpecification) paradigm, it would be best if the types of sensor data to be collected could be specified dynamically instead of hewing to some predefined formats.

# REST API Decomposition #

The RESTful Web Services book recommends following 9 steps to turning requirements into an API:

  1. Figure out the data set
  1. Split the data set into resources
> > For each kind of resource:
  1. Name the resources with URIs
  1. Expose a subset of the uniform interface
  1. Design the representation(s) served to the client
  1. Design the representation(s) accepted from the client
  1. Integrate this resource into existing resources, using hypermedia links and forms
  1. Consider the typical course of events: what's supposed to happen?
  1. Consider error conditions: what might go wrong?

The primary goal is to collect power data from sensors, so obviously individual _sensor data_ must be one of the resources of the system. Sensor data come from _data sources_ (such as meters), so each sensor datum must be associated with a data source. We require that every sensor datum from a particular data source can be uniquely identified by its timestamp. Data sources are under the control of a _user_ who can upload new sensor data and decide who is allowed to download the data. The different types of sensor data can be collected, so we need a way to dynamically specify _sensor data types_. An obvious sensor data type would be PowerData, which could represent power consumed or produced. Finally, it might be useful to create _aggregates_ that combine the data from multiple data sources into one source. For example, an organization like the University of Hawaii might want to aggregate the data from all the building-level meters on campus to see campus wide usage, or the might also want to include data from research stations like Coconut Island.

Based on this data set, we split the data set into resources for each of the italicized terms above:
  * Sensor Data
  * Sensor Data Type
  * Sources
  * Users
  * Aggregates

We now go through steps 3-9 for each resource.

## Sensor Data ##

3. URI specification: `{host}/sensordata/{source}/{timestamp}`

Where `{host}` is the HTTP prefix of the server hosting the service, `{source}` is the name of a Source resource, and `{timestamp}` is an XML [dateTime](http://www.w3.org/TR/2001/REC-xmlschema-2-20010502/#dateTime) object. As discussed before, all Sensor Data belongs to a Source, and a timestamp uniquely identifies a particular Sensor Data from a source.

4. Sensor Data should support the HTTP methods GET, HEAD, PUT, and DELETE. POST is not appropriate since there is no hierarchy beyond the Source, and the client selects the name for the new object (the timestamp) not the server.

5. In the interest of expediency, the representation served to the client will be custom XML derived from an XML schema. While it would be nice to provide XHTML output that can be navigated using a browser (as the RESTful Web Services book recommends), that can be left for a future version. Adding XHTML output would not require any changes to existing clients since the two representations can exist in parallel.

Each Sensor Data resource contains the following fields:

| **field name** | **description** | **example** |
|:---------------|:----------------|:------------|
| Timestamp      | An XML dateTime indicating the time at which this sensor data was generated. | 2009-07-28T09:00:00.000-10:00  |
| Tool           | A string identifying the tool that generated this sensor data. | BMO-Downloader |
| SensorDataType | A URI identifying the sensor data type resource associated with this data. | http://dasha.ics.hawaii.edu:1234/wattdepot/sensordatatypes/PowerData |
| Source         | A URI indicating the Source that generated this sensor data. | http://dasha.ics.hawaii.edu:1234/wattdepot/sources/saunders-floor-3 |
| Properties     | A set of key-value string pairs containing domain-specific sensor data and/or meta-data | {powerConsumed=2359,energyConsumedToDate=135187200} |

6. The representation accepted from the client (for PUT) will be the same as the one served to the client.

7 & 8. The following lists the possible results for each of the supported HTTP methods

GET:
| [HTTP status code](http://en.wikipedia.org/wiki/List_of_HTTP_status_codes) | Meaning | Result |
|:---------------------------------------------------------------------------|:--------|:-------|
| 200                                                                        | OK      | The entity-body contains the requested Sensor Data resource |
| 400                                                                        | Bad Request | The request was ill-formed, perhaps an illegal Source name was specified or the timestamp is formatted improperly |
| 404                                                                        | Not Found | The Sensor Data resource could not be found because: the Source is unknown, there is no resource corresponding to the requested timestamp, the requested Sensor Data is private and the client failed to provide authentication credentials, or the authenticated client is not authorized to view the requested Sensor Data |
| 500                                                                        | Internal Server Error | The service has encountered an error that prevents it from responding with the requested resource. |

HEAD:
Same as GET, but entity-body is empty in the 200 case.

PUT:
| [HTTP status code](http://en.wikipedia.org/wiki/List_of_HTTP_status_codes) | Meaning | Result |
|:---------------------------------------------------------------------------|:--------|:-------|
| 200                                                                        | OK      | The provided Sensor Data resource has been accepted and stored |
| 400                                                                        | Bad Request | The request was ill-formed, perhaps an illegal Source name was specified, the timestamp was formatted improperly, or the XML representation was ill-formed |
| 404                                                                        | Not Found | The Sensor Data resource could not be stored because: the Source is unknown, the client failed to provide authentication credentials, or the authenticated client is not authorized to PUT data for the specified Source |
| 409                                                                        | Conflict | The Sensor Data resource specified already exists. If you wish to replace a Sensor Data resource, DELETE it first, then PUT the new resource |
| 500                                                                        | Internal Server Error | The service has encountered an error that prevents it from responding with the requested resource. |

DELETE:
| [HTTP status code](http://en.wikipedia.org/wiki/List_of_HTTP_status_codes) | Meaning | Result |
|:---------------------------------------------------------------------------|:--------|:-------|
| 200                                                                        | OK      | The requested Sensor Data resource has been deleted |
| 400                                                                        | Bad Request | The request was ill-formed, perhaps an illegal Source name was specified, or the timestamp was formatted improperly |
| 404                                                                        | Not Found | The Sensor Data resource could not be deleted because: the Source is unknown, there is no resource corresponding to the requested timestamp, the client failed to provide authentication credentials, or the authenticated client is not authorized to DELETE data for the specified Source |
| 409                                                                        | Conflict | The Sensor Data resource specified already exists. If you wish to replace a Sensor Data resource, DELETE it first, then PUT the new resource |
| 500                                                                        | Internal Server Error | The service has encountered an error that prevents it from responding with the requested resource. |

Having gone through this process for Sensor Data, now I'm going directly into a more formalized API description for the other resources.
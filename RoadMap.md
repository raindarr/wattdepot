# Phase 1 #

  * Implement WattDepot server with basic functionality specified in RestApi. This allows input of sensor data, and output of sensor data, but no direct way to chart data.

# Phase 2 #

  * Implement Google Visualization data source API using Google library. Once this is complete, embeddable charts can be created of sensor data from any source.
  * Implement flat file input client that reads flat files of comma or tab delimited data and sends it into a WattDepot server. This will allow input of historical Saunders data into WattDepot, as well as historical data collected from other sources. Probably use [opencsv](http://opencsv.sourceforge.net/) for this.

# Phase 3 #
  * Implement input client that dynamically reads data from the [BuildingManagerOnline](http://www.buildingmanageronline.com/overview_BMO.php) system and sends it to a WattDepot server. This would allow live charts of Saunders electricity usage.
  * Implement input client that reads data from TED 5000 Gateway using the XML API. This would allow live (and historical?) charting of Philip's house.
  * Implement virtual Sources, summing appropriate data from sub-Sources. Current best idea is to represent each Source's sensor data as a piece-wise linear interpolation between data points. Then the interpolated functions can be resampled at any desired frequency, and summed for output. This would allow charting of virtual Sources, such as a floor of Saunders Hall (made up of a mauka and a makai meter), or all of Saunders Hall (made up of two input meters).

# Phase 4 #
  * Implement input client that dynamically reads Enphase solar panel production data either exported from Enlighten website, or directly from Envoy data collection meter.

# Other ideas #
  * Encrypted password storage on server
  * Encrypted password storage on client (use keychain on Mac OS X, but what about other platforms?)
  * Output XHTML in addition to XML, allowing direct access to WattDepot server from a browser (as discussed in RESTful Web Services book)
  * Need to handle viewing of anonymized data.
    * Many people might be willing to make their power data available to others, but only if their personal details (particularly names and locations) were anonymized somehow.
    * One option would be to have flags that indicate whether particular metadata should be removed when retrieved by an anonymous user.
    * Another option is creating a second WattDepot server, and running all the private data through an anonymizing filter before making it public on the second server.
    * Things become more complicated if one wants to provide some level of aggregated or semi-anonymized data (like converting addresses to zip codes)
    * One barrier to this is that Sources are accessed by a descriptive name in the URI, which would often reveal information. Need a way to provide an anonymized Source name for anonymized sources.
    * Any solution would have to be communicated to users via a user agreement before they store data.
  * Sources are currently flat with no hierarchy. When there are many Sources, this won't work well. However, it's not clear what a natural hierarchy would be for Sources. Geographic is one possibility, but not great. By user is also not great, but possible.
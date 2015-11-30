### GET `{host}/sensordata` ###

Access Control Level: **Admin**

Returns an index of all Sensor Data resources in this server as a SensorDataIndex XML element. For example,

| GET http://dasha.ics.hawaii.edu:1234/wattdepot/sensordata |
|:----------------------------------------------------------|

Might return:

```
<?xml version="1.0"?>
<SensorDataIndex>
 <SensorDataRef Source="http://dasha.ics.hawaii.edu:1234/wattdepot/sources/saunders-floor-3"
  Timestamp="2009-07-28T09:00:00.000-10:00"
  Tool="BMO-Downloader"
  href="http://dasha.ics.hawaii.edu:1234/wattdepot/sensordata/saunders-floor-3/2009-07-28T09:00:00.000-10:00"/>
 <SensorDataRef Source="http://dasha.ics.hawaii.edu:1234/wattdepot/sources/saunders-floor-4"
  Timestamp="2009-07-28T09:00:00.000-10:00"\
  Tool="BMO-Downloader"
  href="http://dasha.ics.hawaii.edu:1234/wattdepot/sensordata/saunders-floor-4/2009-07-28T09:00:00.000-10:00"/>
 <SensorDataRef Source="http://dasha.ics.hawaii.edu:1234/wattdepot/sources/saunders-floor-5"
  Timestamp="2009-07-28T09:00:00.000-10:00"
  Tool="BMO-Downloader"
  href="http://dasha.ics.hawaii.edu:1234/wattdepot/sensordata/saunders-floor-5/2009-07-28T09:00:00.000-10:00"/>
</SensorDataIndex>
```

Note that this request could return a very, very large XML file.

Possible status codes:

| **[HTTP status code](http://en.wikipedia.org/wiki/List_of_HTTP_status_codes)** | **Meaning** | **Result** |
|:-------------------------------------------------------------------------------|:------------|:-----------|
| 200                                                                            | OK          | The entity-body contains a list of all Sensor Data resources |
| 401                                                                            | Not Authorized | Either the client failed to authenticate properly, or the authenticated user is not the Admin user. |
| 500                                                                            | Internal Server Error | The service has encountered an error that prevents it from responding with the requested resource. |
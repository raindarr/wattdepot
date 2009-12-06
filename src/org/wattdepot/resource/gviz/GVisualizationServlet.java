package org.wattdepot.resource.gviz;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.xml.datatype.XMLGregorianCalendar;
import org.wattdepot.resource.property.jaxb.Properties;
import org.wattdepot.resource.property.jaxb.Property;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.resource.sensordata.jaxb.SensorDataIndex;
import org.wattdepot.resource.sensordata.jaxb.SensorDataRef;
import org.wattdepot.server.Server;
import org.wattdepot.server.db.DbBadIntervalException;
import org.wattdepot.server.db.DbManager;
import org.wattdepot.util.tstamp.Tstamp;
import com.google.visualization.datasource.DataSourceServlet;
import com.google.visualization.datasource.base.DataSourceException;
import com.google.visualization.datasource.base.ReasonType;
import com.google.visualization.datasource.base.TypeMismatchException;
import com.google.visualization.datasource.datatable.ColumnDescription;
import com.google.visualization.datasource.datatable.DataTable;
import com.google.visualization.datasource.datatable.value.ValueType;
import com.google.visualization.datasource.query.Query;
import com.ibm.icu.util.TimeZone;

/**
 * Accepts requests using the Google Visualization API and fulfills then with data from the
 * WattDepot database. Currently does not use authentication, and therefore does not obey the
 * WattDepot security model.
 * 
 * @author Robert Brewer
 */
public class GVisualizationServlet extends DataSourceServlet {

  /** Keep Eclipse happy. */
  private static final long serialVersionUID = 1L;

  /** The WattDepot server (not Servlet container) for contact with WattDepot database. */
  protected transient Server server;

  /** The WattDepot database manager, for retrieving sensor data. */
  protected transient DbManager dbManager;

  /** Conversion factor for milliseconds per minute. */
  private static final long MILLISECONDS_PER_MINUTE = 60L * 1000;

  /**
   * Create the new servlet, and record the provided Server to make queries on the DB.
   * 
   * @param server The WattDepot server this servlet shares data with.
   */
  public GVisualizationServlet(Server server) {
    super();
    this.server = server;
    this.dbManager = (DbManager) server.getContext().getAttributes().get("DbManager");
  }

  /** {@inheritDoc} */
  public DataTable generateDataTable(Query query, HttpServletRequest request)
      throws DataSourceException {
    String remainingUri, sourceName;
    boolean sensorDataRequested = false;

    // This is everything following the URI, which should start with "/gviz/source/"
    String rawPath = request.getPathInfo();
    // Check for incomplete URIs
    if ((rawPath == null) || ("".equals(rawPath)) || "/".equals(rawPath)) {
      throw new DataSourceException(ReasonType.INVALID_REQUEST, "No Source name provided.");
    }
    else {
      if ((rawPath.length() > 1) && (rawPath.startsWith("/"))) {
        // need to strip off leading "/" to get remaining URI
        remainingUri = rawPath.substring(1);
      }
      else {
        // this shouldn't happen, based on my understanding of the path provided to the servlet
        throw new DataSourceException(ReasonType.INTERNAL_ERROR,
            "Internal problem with Source name provided.");
      }
      // URIs that start with "sensordata/" set the sensordata flag
      if (remainingUri.startsWith("sensordata/")) {
        sourceName = remainingUri.substring(11);
        sensorDataRequested = true;
      }
      // Otherwise, just grab the source name
      else {
        sourceName = remainingUri;
      }
    }
    // TODO Should filter the source name, check for unexpected characters and length
    // Check if the given source name exists in database
    if (dbManager.getSource(sourceName) == null) {
      throw new DataSourceException(ReasonType.INVALID_REQUEST, "No Source named \"" + sourceName
          + "\".");
    }

    // DEBUG code, printing query string parameters
    // Map<String, String[]> requestMap = getParameterMapGenerics(request);
    // Set<Map.Entry<String, String []>> entries = requestMap.entrySet();
    // System.err.println("query string: " + request.getQueryString()); // DEBUG
    // System.err.println("request.getParameterMap(): "); // DEBUG
    // for (Map.Entry<String, String []> entry : entries) {
    // System.err.println(entry.getKey() + ": " + Arrays.toString(entry.getValue()));
    // }

    String startTimeString = getQueryParameter(request, "startTime");
    String endTimeString = getQueryParameter(request, "endTime");
    XMLGregorianCalendar startTime = null;
    XMLGregorianCalendar endTime = null;
    if ((startTimeString != null) && (endTimeString != null)) {
      try {
        startTime = Tstamp.makeTimestamp(startTimeString);
      }
      catch (Exception e) { // NOPMD
        log("Unable to convert startTime parameter to XMLGregorianCalendar", e);
        throw new DataSourceException(ReasonType.INVALID_REQUEST, // NOPMD
            "startTime parameter was invalid."); // NOPMD
      }
      try {
        endTime = Tstamp.makeTimestamp(endTimeString);
      }
      catch (Exception e) {
        log("Unable to convert endTime parameter to XMLGregorianCalendar", e);
        throw new DataSourceException(ReasonType.INVALID_REQUEST, "endTime parameter was invalid."); // NOPMD
      }
    }

    // Create a data table,
    DataTable data = new DataTable();
    ArrayList<ColumnDescription> cd = new ArrayList<ColumnDescription>();

    if (sensorDataRequested) {
      cd.add(new ColumnDescription("timePoint", ValueType.DATETIME, "Date & Time"));
      cd.add(new ColumnDescription("powerConsumed", ValueType.NUMBER, "Power (W)"));
      cd
          .add(new ColumnDescription("energyConsumedToDate", ValueType.NUMBER,
              "Energy Consumed (Wh)"));
      cd.add(new ColumnDescription("powerGenerated", ValueType.NUMBER, "Power Generated (W)"));
      data.addColumns(cd);

      SensorDataIndex index;
      // Get all sensor data for this Source
      if ((startTime == null) && (endTime == null)) {
        index = dbManager.getSensorDataIndex(sourceName);
      }
      // If we received valid start and end times, retrieve just that data
      else {
        try {
          index = dbManager.getSensorDataIndex(sourceName, startTime, endTime);
        }
        catch (DbBadIntervalException e) {
          log("startTime came after endTime", e);
          throw new DataSourceException(ReasonType.INVALID_REQUEST, // NOPMD
              "startTime parameter was after endTime parameter."); // NOPMD
        }
      }
      Properties props;
      // Iterate over each SensorDataRef
      for (SensorDataRef ref : index.getSensorDataRef()) {
        Double powerConsumed = null, energyConsumedToDate = null, powerGenerated = null;
        props = dbManager.getSensorData(sourceName, ref.getTimestamp()).getProperties();
        // Look for properties on this SensorData that we are interested in
        for (Property prop : props.getProperty()) {
          try {
            if (prop.getKey().equals(SensorData.POWER_CONSUMED)) {
              powerConsumed = new Double(prop.getValue());
            }
            else if (prop.getKey().equals(SensorData.ENERGY_CONSUMED_TO_DATE)) {
              energyConsumedToDate = new Double(prop.getValue());
            }
            else if (prop.getKey().equals(SensorData.POWER_GENERATED)) {
              powerGenerated = new Double(prop.getValue());
            }
          }
          catch (NumberFormatException e) {
            // String value in database couldn't be converted to a number.
            throw new DataSourceException(ReasonType.INTERNAL_ERROR, "Found bad number in database"); // NOPMD
          }
        }
        // System.err.format("timestamp: %s, power: %f, energy: %f%n",
        // ref.getTimestamp().toString(),
        // powerConsumed, energyConsumedToDate); // DEBUG
        try {
          // Add a row into the table
          data.addRowFromValues(convertTimestamp(ref.getTimestamp()), powerConsumed,
              energyConsumedToDate, powerGenerated);
        }
        catch (TypeMismatchException e) {
          throw new DataSourceException(ReasonType.INTERNAL_ERROR, "Problem adding data to table"); // NOPMD
        }
      }
    }
    else {
      // String resourcesString = getQueryParameter(request, "resources");
      // String [] resourcesArray = null;
      // if (resourcesString == null) {
      // throw new DataSourceException(ReasonType.INVALID_REQUEST,
      // "No resource type(s) specified.");
      // }
      // else {
      // resourcesArray = resourcesString.split(" *, *", 100);
      // }
      if ((startTime == null) || (endTime == null)) {
        throw new DataSourceException(ReasonType.INVALID_REQUEST,
            "Valid startTime and endTime parameters are required.");
      }
      else if (Tstamp.greaterThan(startTime, endTime)) {
        throw new DataSourceException(ReasonType.INVALID_REQUEST,
            "startTime parameter later than endTime parameter");
      }

      String intervalString = getQueryParameter(request, "samplingInterval");
      SensorData powerData = null;
      long intervalMilliseconds;
      long rangeLength = Tstamp.diff(startTime, endTime);
      long minutesToMilliseconds = 60L * 1000L;
      int intervalMinutes = 0;

      if (intervalString != null) {
        // convert to integer
        try {
          intervalMinutes = Integer.valueOf(intervalString);
        }
        catch (NumberFormatException e) {
          log("Unable to convert samplingInterval parameter to int", e);
          throw new DataSourceException(ReasonType.INVALID_REQUEST, // NOPMD
              "samplingInterval parameter was invalid."); // NOPMD
        }
      }

      if (intervalMinutes < 0) {
        log("samplingInterval parameter less than 0");
        throw new DataSourceException(ReasonType.INVALID_REQUEST,
            "samplingInterval parameter was less than 0.");
      }
      else if (intervalMinutes == 0) {
        // use default interval
        intervalMilliseconds = rangeLength / 10;
      }
      else if ((intervalMinutes * minutesToMilliseconds) > rangeLength) {
        log("samplingInterval parameter less than 0");
        throw new DataSourceException(ReasonType.INVALID_REQUEST,
            "samplingInterval parameter was larger than time range.");
      }
      else {
        // got a good interval
        intervalMilliseconds = intervalMinutes * minutesToMilliseconds;
      }
      // DEBUG
      System.out.format("%nstartTime=%s, endTime=%s, interval=%d min%n", startTime, endTime,
          intervalMilliseconds / minutesToMilliseconds);

      cd.add(new ColumnDescription("timePoint", ValueType.DATETIME, "Date & Time"));
      cd.add(new ColumnDescription("powerGenerated", ValueType.NUMBER, "Power Generated (W)"));
      data.addColumns(cd);

      // Build list of timestamps, starting with startTime, separated by intervalMilliseconds
      List<XMLGregorianCalendar> timestampList = new ArrayList<XMLGregorianCalendar>();
      XMLGregorianCalendar timestamp = startTime;
      while (Tstamp.lessThan(timestamp, endTime)) {
        timestampList.add(timestamp);
        System.out.format("timestamp=%s%n", timestamp);
        timestamp = Tstamp.incrementMilliseconds(timestamp, intervalMilliseconds);
      }
      // add endTime to cover the last runt interval which is <= intervalMilliseconds
      timestampList.add(endTime);
      System.out.format("timestamp=%s%n", endTime);
      for (XMLGregorianCalendar powerTime : timestampList) {
        powerData = this.dbManager.getPower(sourceName, powerTime);
        System.out.println("powerData = " + powerData);
        System.out.println("convertTimestamp(powerTime) = " + convertTimestamp(powerTime)
            + ", powerGenerated = " + powerData.getPropertyAsDouble(SensorData.POWER_GENERATED));
        try {
          // Add a row into the table
          data.addRowFromValues(convertTimestamp(powerTime), powerData
              .getPropertyAsDouble(SensorData.POWER_GENERATED));
        }
        catch (TypeMismatchException e) {
          throw new DataSourceException(ReasonType.INTERNAL_ERROR, "Problem adding data to table"); // NOPMD
        }
      }
      // List<List<SensorDataStraddle>> masterList =
      // this.dbManager.getSensorDataStraddleListOfLists(this.uriSource, timestampList);
      // if ((masterList == null) || (masterList.isEmpty())) {
      // return null;
      // }
      // else {
      // energyData =
      // Energy.getEnergyFromListOfLists(masterList, Source.sourceToUri(this.uriSource, server));
      // }
      // if (energyData == null) {
      // return null;
      // }
    }
    System.out.println(data);
    return data;
  }

  /**
   * Converts the raw Map returned by HttpServletRequest.getParameterMap() into a generic Map with
   * proper parameters.
   * 
   * @param request The incoming HTTP request.
   * @return A Map<String, String[]>, which is the format getParameterMap() is documented to return.
   */
  @SuppressWarnings(value = "unchecked")
  private Map<String, String[]> getParameterMapGenerics(HttpServletRequest request) {
    // Force the cast, which should work based on the HttpServletRequest Javadoc
    // Map<String, String[]> returnMap = request.getParameterMap();
    // return returnMap;
    return request.getParameterMap();
  }

  /**
   * Retrieves the named HTTP query parameter from the request as a String, or null if the parameter
   * isn't present or has an empty value. ServletRequest returns String arrays, but for HTTP they
   * seem to always contain one element, so this method makes things more convenient.
   * 
   * @param request The incoming HTTP request.
   * @param parameterName The name of the HTTP query parameter
   * @return a String containing the query parameter's value, or null if the parameter name is not
   * present.
   */
  private String getQueryParameter(HttpServletRequest request, String parameterName) {
    Map<String, String[]> parameterMap = getParameterMapGenerics(request);
    String[] value = parameterMap.get(parameterName);
    if ((value == null) || (value.length == 0)) {
      return null;
    }
    else if (value.length == 1) {
      return value[0];
    }
    else {
      log("Got multiple values in parameter array, just returning first value.");
      return value[0];
    }
  }

  /**
   * Takes an XMLGregorianCalendar (the native WattDepot timestamp format) and returns a
   * com.ibm.icu.util.GregorianCalendar (<b>note:</b> this is <b>not</b> the same as a
   * java.util.GregorianCalendar!) with the time zone set to GMT, but with the timestamp adjusted by
   * any time zone offset in the XMLGregorianCalendar.
   * 
   * For example, if the input value is 10 AM Hawaii Standard Time (HST is -10 hours from UTC), then
   * the output will be 10 AM UTC, effectively subtracting 10 hours from the timestamp (in addition
   * to the conversion between types).
   * 
   * This rigamarole is needed because the Google Visualization data source library only accepts
   * timestamps in UTC, and the Annonated Timeline visualization displays the UTC values. Without
   * this conversion, any graphs of meter data recorded in the Hawaii time zone (for instance) would
   * display the data 10 hours later in the graph, which is not acceptable.
   * 
   * @param timestamp the input timestamp, presumably from WattDepot
   * @return a com.ibm.icu.util.GregorianCalendar suitable for use by the Google visualization data
   * source library, and normalized by any time zone offset in timestamp but with timeZone field set
   * to GMT.
   */
  protected com.ibm.icu.util.GregorianCalendar convertTimestamp(XMLGregorianCalendar timestamp) {
    return this.convertTimestamp(timestamp, timestamp.getTimezone());
  }

  /**
   * Takes an XMLGregorianCalendar (the native WattDepot timestamp format) and a time zone offset
   * (expressed as minutes from UTC) and returns a com.ibm.icu.util.GregorianCalendar (<b>note:</b>
   * this is <b>not</b> the same as a java.util.GregorianCalendar!) with the time zone set to GMT,
   * but with the timestamp adjusted by the provided offset.
   * 
   * For example, if the input value is 10 AM Hawaii Standard Time (HST is -10 hours from UTC), and
   * the offset is -600 minutes then the output will be 10 AM UTC, effectively subtracting 10 hours
   * from the timestamp (in addition to the conversion between types).
   * 
   * This rigamarole is needed because the Google Visualization data source library only accepts
   * timestamps in UTC, and the Annonated Timeline visualization displays the UTC values. Without
   * this conversion, any graphs of meter data recorded in the Hawaii time zone (for instance) would
   * display the data 10 hours later in the graph, which is not acceptable. The timezoneOffset is
   * provided if in the future we want the capability to display data normalized to an arbitrary
   * time zone, rather than just the one the data was collected in.
   * 
   * @param timestamp the input timestamp, presumably from WattDepot
   * @param timeZoneOffset the desired offset in minutes from UTC.
   * @return a com.ibm.icu.util.GregorianCalendar suitable for use by the Google visualization data
   * source library, and normalized by timeZoneOffset but with timeZone field set to GMT.
   */
  protected com.ibm.icu.util.GregorianCalendar convertTimestamp(XMLGregorianCalendar timestamp,
      int timeZoneOffset) {
    // Calendar conversion hell. GViz library uses com.ibm.icu.util.GregorianCalendar, which is
    // different from java.util.GregorianCalendar. So we go from our native format
    // XMLGregorianCalendar -> GregorianCalendar, extract milliseconds since epoch, feed that
    // to the IBM GregorianCalendar.
    com.ibm.icu.util.GregorianCalendar gvizCal = new com.ibm.icu.util.GregorianCalendar();
    // Added bonus, DateTimeValue only accepts GregorianCalendars if the timezone is GMT.
    gvizCal.setTimeZone(TimeZone.getTimeZone("GMT"));
    // System.err.println("timestamp: " + timestamp.toString()); // DEBUG
    GregorianCalendar standardCal = timestamp.toGregorianCalendar();
    // System.err.println("standardCal: " + standardCal.toString()); // DEBUG
    // Add the timeZoneOffset to the epoch time after converting to milliseconds
    gvizCal.setTimeInMillis(standardCal.getTimeInMillis()
        + (timeZoneOffset * MILLISECONDS_PER_MINUTE));
    // System.err.println("gvizCal: " + gvizCal.toString()); // DEBUG
    return gvizCal;
  }

  /**
   * @see com.google.visualization.datasource.DataSourceServlet#isRestrictedAccessMode()
   * @return True if the servlet should only respond to queries from the same domain, false
   * otherwise.
   */
  @Override
  protected boolean isRestrictedAccessMode() {
    return false;
  }
}
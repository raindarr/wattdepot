package org.wattdepot.resource.gviz;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;
import org.restlet.data.MediaType;
import org.restlet.representation.Variant;
import org.restlet.resource.Get;
import org.wattdepot.resource.WattDepotResource;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.resource.sensordata.jaxb.SensorDataIndex;
import org.wattdepot.resource.sensordata.jaxb.SensorDataRef;
import org.wattdepot.resource.source.jaxb.Source;
import org.wattdepot.resource.user.jaxb.User;
import org.wattdepot.server.Server;
import org.wattdepot.server.db.DbBadIntervalException;
import org.wattdepot.util.tstamp.Tstamp;
import com.google.common.collect.Lists;
import com.google.visualization.datasource.DataSourceHelper;
import com.google.visualization.datasource.base.DataSourceException;
import com.google.visualization.datasource.base.ReasonType;
import com.google.visualization.datasource.base.TypeMismatchException;
import com.google.visualization.datasource.datatable.ColumnDescription;
import com.google.visualization.datasource.datatable.DataTable;
import com.google.visualization.datasource.datatable.TableRow;
import com.google.visualization.datasource.datatable.value.DateTimeValue;
import com.google.visualization.datasource.datatable.value.ValueType;
import com.google.visualization.datasource.query.AbstractColumn;
import com.google.visualization.datasource.query.Query;
import com.google.visualization.datasource.render.JsonRenderer;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

//
/**
 * Provides access to SensorData via the Google Visualization API using the Google Visualization
 * Datasource library. The Datasource library typically wraps its functionality as a servlet,
 * requiring a servlet container. Since WattDepot is based on Restlet, we can create a cusotm
 * Datasource to avoid the servlet requirement.
 * 
 * @see <a
 * href="http://code.google.com/apis/chart/interactive/docs/dev/implementing_data_source.html">Google
 * Visualization Datasource API</a>
 * @author Andrea Connell
 */

public class GVisualizationResource extends WattDepotResource {

  /** The type of query (sensordata or calculated) to perform. */
  protected String queryType = null;
  /** The timestamp for the query. */
  protected String timestamp = null;
  /** The start time. */
  protected String startTimeString = null;
  /** The end time. */
  protected String endTimeString = null;
  /** The GViz query string. */
  protected String queryString = null;
  /** The sampling interval. */
  protected String samplingIntervalString = null;
  /** Whether to display subsources or not. */
  protected String displaySubsourcesString = null;
  /** The tqx query string. */
  protected String tqxString = null;

  /** Conversion factor for milliseconds per minute. */
  private static final long MILLISECONDS_PER_MINUTE = 60L * 1000;

  /** Name for timestamp column of data table. */
  private static final String TIME_POINT_COLUMN = "timePoint";
  /** Name for power consumed column of data table. */
  private static final String POWER_CONSUMED_COLUMN = SensorData.POWER_CONSUMED;
  /** Name for power consumed column of data table. */
  private static final String POWER_GENERATED_COLUMN = SensorData.POWER_GENERATED;
  /** Name for energy consumed to date (a counter) column of data table. */
  private static final String ENERGY_CONSUMED_TO_DATE_COLUMN = SensorData.ENERGY_CONSUMED_TO_DATE;
  /** Name for energy consumed to date (a counter) column of data table. */
  private static final String ENERGY_GENERATED_TO_DATE_COLUMN =
      SensorData.ENERGY_GENERATED_TO_DATE;

  /** List of all possible columns for sensor data requests. */
  private static final ColumnDescription[] SENSOR_DATA_TABLE_COLUMNS = new ColumnDescription[] {
      new ColumnDescription(TIME_POINT_COLUMN, ValueType.DATETIME, "Date & Time"),
      new ColumnDescription(POWER_CONSUMED_COLUMN, ValueType.NUMBER, "Power Cons. (W)"),
      new ColumnDescription(POWER_GENERATED_COLUMN, ValueType.NUMBER, "Power Gen. (W)"),
      new ColumnDescription(ENERGY_CONSUMED_TO_DATE_COLUMN, ValueType.NUMBER,
          "Energy Consumed To Date (Wh)"),
      new ColumnDescription(ENERGY_GENERATED_TO_DATE_COLUMN, ValueType.NUMBER,
          "Energy Generated To Date (Wh)") };

  /** Name for energy consumed column of data table. */
  private static final String ENERGY_CONSUMED_COLUMN = "energyConsumed";
  /** Name for energy generated column of data table. */
  private static final String ENERGY_GENERATED_COLUMN = "energyGenerated";
  /** Name for carbon emitted column of data table. */
  private static final String CARBON_EMITTED_COLUMN = "carbonEmitted";

  /** List of all possible columns for calculated value requests. */
  private static final ColumnDescription[] CALCULATED_TABLE_COLUMNS = new ColumnDescription[] {
      new ColumnDescription(TIME_POINT_COLUMN, ValueType.DATETIME, "Date & Time"),
      new ColumnDescription(POWER_CONSUMED_COLUMN, ValueType.NUMBER, "Power Cons. (W)"),
      new ColumnDescription(POWER_GENERATED_COLUMN, ValueType.NUMBER, "Power Gen. (W)"),
      new ColumnDescription(ENERGY_CONSUMED_COLUMN, ValueType.NUMBER, "Energy Cons. (Wh)"),
      new ColumnDescription(ENERGY_GENERATED_COLUMN, ValueType.NUMBER, "Energy Gen. (Wh)"),
      new ColumnDescription(CARBON_EMITTED_COLUMN, ValueType.NUMBER, "Carbon (lbs CO2)") };

  /**
   * Initialize with attributes from the Request.
   */
  @Override
  protected void doInit() {
    super.doInit();

    this.queryType = (String) this.getRequest().getAttributes().get("type");
    this.timestamp = (String) this.getRequest().getAttributes().get("timestamp");
    this.startTimeString =
        (String) this.getRequest().getResourceRef().getQueryAsForm().getFirstValue("startTime");
    this.endTimeString =
        (String) this.getRequest().getResourceRef().getQueryAsForm().getFirstValue("endTime");
    this.samplingIntervalString =
        (String) this.getRequest().getResourceRef().getQueryAsForm()
            .getFirstValue("samplingInterval");
    this.queryString = this.getRequest().getResourceRef().getQueryAsForm().getFirstValue("tq");
    this.tqxString =
        (String) this.getRequest().getResourceRef().getQueryAsForm().getFirstValue("tqx");
    this.displaySubsourcesString =
        (String) this.getRequest().getResourceRef().getQueryAsForm()
            .getFirstValue("displaySubsources");

    try {
      if (queryString != null) {
        queryString = URLDecoder.decode(queryString, "UTF-8");
      }
    }
    catch (UnsupportedEncodingException e) {
      queryString = null;
    }
    try {
      if (tqxString != null) {
        tqxString = URLDecoder.decode(tqxString, "UTF-8");
      }
    }
    catch (UnsupportedEncodingException e) {
      tqxString = null;
    }

    getVariants().add(new Variant(MediaType.TEXT_PLAIN));
  }

  /**
   * The GET method for plain text data.
   * 
   * @return The text representation of this resource
   */
  @Get("txt")
  public String getTxt() {
    try {

      XMLGregorianCalendar startTime = null;
      XMLGregorianCalendar endTime = null;
      if ((startTimeString != null) && (endTimeString != null)) {
        try {
          startTime = Tstamp.makeTimestamp(startTimeString);
        }
        catch (Exception e) {
          throw new DataSourceException(ReasonType.INVALID_REQUEST, "Invalid Start Time");
        }
        try {
          endTime = Tstamp.makeTimestamp(endTimeString);
        }
        catch (Exception e) {
          throw new DataSourceException(ReasonType.INVALID_REQUEST, "Invalid End Time");
        }
      }

      Query query = null;

      query = DataSourceHelper.parseQuery(queryString);

      DataTable table;

      if (this.queryType == null) {
        throw new DataSourceException(ReasonType.INVALID_REQUEST, "Query Type Required");
      }

      table = generateDataTable(uriSource, query, startTime, endTime);
      String tableString = JsonRenderer.renderDataTable(table, true, true).toString();

      String[] tqxArray;
      String reqId = null;
      if (tqxString != null) {
        tqxArray = tqxString.split(";");
        for (String s : tqxArray) {
          if (s.contains("reqId")) {
            reqId = s.substring(s.indexOf(":") + 1, s.length());
          }
        }
      }

      String response = "google.visualization.Query.setResponse({status:'ok',";
      if (reqId != null) {
        response += "reqId:'" + reqId + "',";
      }
      response += "table:" + tableString + "});";
      return response;
    }
    catch (DataSourceException e) {
      String response =
          "google.visualization.Query.setResponse({status:'error',errors:[{reason:'"
              + e.getReasonType().toString() + "',message:'" + e.getMessageToUser() + "'}]});";
      return response;
    }
  }

  /**
   * Determine which type of data is being requested, call the appropriate method to generate it,
   * and apply the Query.
   * 
   * @param source The name of the source.
   * @param query The query from the client.
   * @param startTime The start time.
   * @param endTime The end time.
   * @return A DataTable with the requested information.
   * @throws DataSourceException If an invalid query type is requested.
   */
  public DataTable generateDataTable(String source, Query query, XMLGregorianCalendar startTime,
      XMLGregorianCalendar endTime) throws DataSourceException {

    DataTable data = null;

    if ("sensordata".equals(this.queryType)) {
      // currently only supports latest timestamp
      if (this.timestamp != null && timestamp.equals(Server.LATEST)) {
        data = generateLatestSensorDataTable(query, source);
      }
      else {
        data = generateSensorDataTable(query, source, startTime, endTime);
      }
    }
    else if ("calculated".equals(this.queryType)) {
      int intervalMinutes = 0;

      if (samplingIntervalString != null) {
        // convert to integer
        try {
          intervalMinutes = Integer.valueOf(samplingIntervalString);
        }
        catch (NumberFormatException e) {
          // log("Unable to convert samplingInterval parameter to int", e);
          throw new DataSourceException(ReasonType.INVALID_REQUEST, // NOPMD
              "samplingInterval parameter was invalid."); // NOPMD
        }
      }

      boolean displaySubsources = "true".equals(displaySubsourcesString);
      data =
          generateCalculatedTable(query, source, startTime, endTime, intervalMinutes,
              displaySubsources);
    }
    else {
      throw new DataSourceException(ReasonType.INVALID_REQUEST, "Invalid query type");
    }

    DataSourceHelper.validateQueryAgainstColumnStructure(query, data);

    data = DataSourceHelper.applyQuery(query, data, ULocale.US);
    return data;
  }

  /**
   * Generates a DataTable of sensor data, given the query parameters. Supports the SELECT
   * capability, so only columns that are SELECTed will be retrieved and added to the table. The
   * startTime and endTime parameters can be null, in which case all sensor data for the source will
   * be retrieved (which can be very big).
   * 
   * @param sourceName The name of the source.
   * @param startTime The starting time for the interval.
   * @param endTime The ending time for the interval.
   * @param query The query from the data source client.
   * @return A DataTable with the selected columns for every sensor data resource within the
   * interval.
   * @throws DataSourceException If there are problems fulfilling the request.
   */
  private DataTable generateSensorDataTable(Query query, String sourceName,
      XMLGregorianCalendar startTime, XMLGregorianCalendar endTime) throws DataSourceException {

    DataTable data = new DataTable();
    // Sets up the columns requested by any SELECT in the data source query
    List<ColumnDescription> requiredColumns =
        getRequiredColumns(query, SENSOR_DATA_TABLE_COLUMNS, null);
    data.addColumns(requiredColumns);

    SensorDataIndex index;
    // Get all sensor data for this Source
    if ((startTime == null) || (endTime == null)) {
      index = dbManager.getSensorDataIndex(sourceName);
    }
    // If we received valid start and end times, retrieve just that data
    else {
      try {
        index = dbManager.getSensorDataIndex(sourceName, startTime, endTime);
      }
      catch (DbBadIntervalException e) {
        // log("startTime came after endTime", e);
        throw new DataSourceException(ReasonType.INVALID_REQUEST, // NOPMD
            "startTime parameter was after endTime parameter."); // NOPMD
      }
    }
    // Iterate over each SensorDataRef
    for (SensorDataRef ref : index.getSensorDataRef()) {
      SensorData sensorData = dbManager.getSensorData(sourceName, ref.getTimestamp());
      TableRow row = new TableRow();
      for (ColumnDescription selectionColumn : requiredColumns) {
        String columnName = selectionColumn.getId();
        try {
          if (columnName.equals(TIME_POINT_COLUMN)) {
            row.addCell(new DateTimeValue(convertTimestamp(ref.getTimestamp())));
          }
          else if (columnName.equals(POWER_CONSUMED_COLUMN)) {
            row.addCell(sensorData.getPropertyAsDouble(SensorData.POWER_CONSUMED));
          }
          else if (columnName.equals(POWER_GENERATED_COLUMN)) {
            row.addCell(sensorData.getPropertyAsDouble(SensorData.POWER_GENERATED));
          }
          else if (columnName.equals(ENERGY_CONSUMED_TO_DATE_COLUMN)) {
            row.addCell(sensorData.getPropertyAsDouble(SensorData.ENERGY_CONSUMED_TO_DATE));
          }
          else if (columnName.equals(ENERGY_GENERATED_TO_DATE_COLUMN)) {
            row.addCell(sensorData.getPropertyAsDouble(SensorData.ENERGY_GENERATED_TO_DATE));
          }
        }
        catch (NumberFormatException e) {
          // String value in database couldn't be converted to a number.
          throw new DataSourceException(ReasonType.INTERNAL_ERROR, "Found bad number in database"); // NOPMD
        }
      }
      try {
        data.addRow(row);
      }
      catch (TypeMismatchException e) {
        throw new DataSourceException(ReasonType.INTERNAL_ERROR, "Problem adding data to table"); // NOPMD
      }
    }

    return data;
  }

  /**
   * Generates a DataTable of the latest sensor data (which will have a single row), given the query
   * parameters. Supports the SELECT capability, so only columns that are SELECTed will be retrieved
   * and added to the table.
   * 
   * @param query The query from the data source client.
   * @param sourceName The name of the source.
   * @return A DataTable with the selected columns for every sensor data resource within the
   * interval.
   * @throws DataSourceException If there are problems fulfilling the request.
   */
  private DataTable generateLatestSensorDataTable(Query query, String sourceName)
      throws DataSourceException {
    DataTable data = new DataTable();
    // Sets up the columns requested by any SELECT in the data source query
    List<ColumnDescription> requiredColumns =
        getRequiredColumns(query, SENSOR_DATA_TABLE_COLUMNS, null);
    data.addColumns(requiredColumns);

    SensorData sensorData = dbManager.getLatestSensorData(sourceName);
    TableRow row = new TableRow();
    for (ColumnDescription selectionColumn : requiredColumns) {
      String columnName = selectionColumn.getId();
      try {
        if (columnName.equals(TIME_POINT_COLUMN)) {
          row.addCell(new DateTimeValue(convertTimestamp(sensorData.getTimestamp())));
        }
        else if (columnName.equals(POWER_CONSUMED_COLUMN)) {
          row.addCell(sensorData.getPropertyAsDouble(SensorData.POWER_CONSUMED));
        }
        else if (columnName.equals(POWER_GENERATED_COLUMN)) {
          row.addCell(sensorData.getPropertyAsDouble(SensorData.POWER_GENERATED));
        }
        else if (columnName.equals(ENERGY_CONSUMED_TO_DATE_COLUMN)) {
          row.addCell(sensorData.getPropertyAsDouble(SensorData.ENERGY_CONSUMED_TO_DATE));
        }
        else if (columnName.equals(ENERGY_GENERATED_TO_DATE_COLUMN)) {
          row.addCell(sensorData.getPropertyAsDouble(SensorData.ENERGY_GENERATED_TO_DATE));
        }
      }
      catch (NumberFormatException e) {
        // String value in database couldn't be converted to a number.
        throw new DataSourceException(ReasonType.INTERNAL_ERROR, "Found bad number in database"); // NOPMD
      }
    }
    try {
      data.addRow(row);
    }
    catch (TypeMismatchException e) {
      throw new DataSourceException(ReasonType.INTERNAL_ERROR, "Problem adding data to table"); // NOPMD
    }
    return data;
  }

  /**
   * Generates a DataTable of calculated data, given the query parameters. Supports the SELECT
   * capability, so only columns that are SELECTed will be retrieved and added to the table. The
   * startTime and endTime parameters are required, as is the sampling interval.
   * 
   * @param query The query from the data source client.
   * @param sourceName The name of the source.
   * @param startTime The starting time for the interval.
   * @param endTime The ending time for the interval.
   * @param intervalMinutes the rate at which the selected columns should be sampled, in minutes.
   * @param displaySubsources True if subsources are to be included as additional columns in the
   * DataTable, false otherwise.
   * @return A DataTable with the selected columns sampled at the given rate within the interval.
   * @throws DataSourceException If there are problems fulfilling the request.
   */
  private DataTable generateCalculatedTable(Query query, String sourceName,
      XMLGregorianCalendar startTime, XMLGregorianCalendar endTime, int intervalMinutes,
      boolean displaySubsources) throws DataSourceException {
    DataTable data = new DataTable();

    if ((startTime == null) || (endTime == null)) {
      throw new DataSourceException(ReasonType.INVALID_REQUEST,
          "Valid startTime and endTime parameters are required.");
    }
    else if (Tstamp.greaterThan(startTime, endTime)) {
      throw new DataSourceException(ReasonType.INVALID_REQUEST,
          "startTime parameter later than endTime parameter");
    }

    SensorData powerData = null, energyData = null, carbonData = null;
    long intervalMilliseconds;
    long rangeLength = Tstamp.diff(startTime, endTime);
    long minutesToMilliseconds = 60L * 1000L;

    if (intervalMinutes < 0) {
      // log("samplingInterval parameter less than 0");
      throw new DataSourceException(ReasonType.INVALID_REQUEST,
          "samplingInterval parameter was less than 0.");
    }
    else if (intervalMinutes == 0) {
      // use default interval
      intervalMilliseconds = rangeLength / 10;
    }
    else if ((intervalMinutes * minutesToMilliseconds) > rangeLength) {
      // log("samplingInterval parameter less than 0");
      throw new DataSourceException(ReasonType.INVALID_REQUEST,
          "samplingInterval parameter was larger than time range.");
    }
    else {
      // got a good interval
      intervalMilliseconds = intervalMinutes * minutesToMilliseconds;
    }
    // DEBUG
    // System.out.format("%nstartTime=%s, endTime=%s, interval=%d min%n", startTime, endTime,
    // intervalMilliseconds / minutesToMilliseconds);

    Source source = this.dbManager.getSource(sourceName);
    List<Source> subSources = null;
    Hashtable<String, Source> hash = null;
    if (displaySubsources && source.isVirtual()) {
      subSources = this.dbManager.getAllNonVirtualSubSources(source);

      hash = new Hashtable<String, Source>();
      for (int i = 0; i < subSources.size(); i++) {
        Source s = subSources.get(i);
        // remove any sources that we don't have permission to view
        if (!s.isPublic() && !isAdminUser()
            && !User.userToUri(authUsername, this.server).equals(source.getOwner())) {
          subSources.remove(s);
          i--;
        }
        else {
          // store these in a hash so we don't have to get them from the database again.
          hash.put(s.getName(), s);
        }
      }
    }

    // Sets up the columns requested by any SELECT in the datasource query
    List<ColumnDescription> requiredColumns =
        getRequiredColumns(query, CALCULATED_TABLE_COLUMNS, subSources);
    data.addColumns(requiredColumns);

    // Build list of timestamps, starting with startTime, separated by intervalMilliseconds
    List<XMLGregorianCalendar> timestampList =
        Tstamp.getTimestampList(startTime, endTime, intervalMinutes);

    for (int i = 0; i < timestampList.size(); i++, powerData = null, energyData = null, carbonData =
        null) {
      XMLGregorianCalendar currentTimestamp = timestampList.get(i);
      XMLGregorianCalendar previousTimestamp;
      if (i == 0) {
        // First timestamp in list doesn't have a previous timestamp we can use to make an
        // interval, so we look one interval _before_ the first timestamp.
        previousTimestamp = Tstamp.incrementMilliseconds(currentTimestamp, -intervalMilliseconds);
      }
      else {
        previousTimestamp = timestampList.get(i - 1);
      }
      int currentInterval =
          (int) (Tstamp.diff(previousTimestamp, currentTimestamp) / minutesToMilliseconds);

      TableRow row = new TableRow();
      for (ColumnDescription selectionColumn : requiredColumns) {
        String columnName = selectionColumn.getId();
        // If this is a subsource, then the custom property sourceName will be set
        String propertySourceName = selectionColumn.getCustomProperty("sourceName");
        Source currentSource;
        if (propertySourceName == null) {
          // normal column, not subsource
          currentSource = source;
        }
        else {
          // subsource, so use source name from column's custom property
          currentSource = hash.get(propertySourceName);
        }
        try {
          if (columnName.equals(TIME_POINT_COLUMN)) {
            row.addCell(new DateTimeValue(convertTimestamp(currentTimestamp)));
          }
          else if (columnName.endsWith(POWER_CONSUMED_COLUMN)) {
            powerData = this.dbManager.getPower(currentSource, currentTimestamp);
            if (powerData == null) {
              row.addCell(0);
            }
            else {
              row.addCell(powerData.getPropertyAsDouble(SensorData.POWER_CONSUMED));
            }
          }
          else if (columnName.endsWith(POWER_GENERATED_COLUMN)) {
            powerData = this.dbManager.getPower(currentSource, currentTimestamp);
            if (powerData == null) {
              row.addCell(0);
            }
            else {
              row.addCell(powerData.getPropertyAsDouble(SensorData.POWER_GENERATED));
            }
          }
          else if (columnName.endsWith(ENERGY_CONSUMED_COLUMN)) {
            energyData =
                this.dbManager.getEnergy(currentSource, previousTimestamp, currentTimestamp,
                    currentInterval);
            if (energyData == null) {
              row.addCell(0);
            }
            else {
              row.addCell(energyData.getPropertyAsDouble(SensorData.ENERGY_CONSUMED));
            }
          }
          else if (columnName.endsWith(ENERGY_GENERATED_COLUMN)) {
            energyData =
                this.dbManager.getEnergy(currentSource, previousTimestamp, currentTimestamp,
                    currentInterval);
            if (energyData == null) {
              row.addCell(0);
            }
            else {
              row.addCell(energyData.getPropertyAsDouble(SensorData.ENERGY_GENERATED));
            }
          }
          else if (columnName.endsWith(CARBON_EMITTED_COLUMN)) {
            carbonData =
                this.dbManager.getCarbon(currentSource, previousTimestamp, currentTimestamp,
                    currentInterval);
            if (carbonData == null) {
              row.addCell(0);
            }
            else {
              row.addCell(carbonData.getPropertyAsDouble(SensorData.CARBON_EMITTED));
            }
          }
        }
        catch (NumberFormatException e) {
          // String value in database couldn't be converted to a number.
          throw new DataSourceException(ReasonType.INTERNAL_ERROR, "Found bad number in database"); // NOPMD
        }
      }
      try {
        data.addRow(row);
      }
      catch (TypeMismatchException e) {
        throw new DataSourceException(ReasonType.INTERNAL_ERROR, "Problem adding data to table"); // NOPMD
      }
    }
    return data;
  }

  /**
   * Returns true if the given column name is requested in the given query. If the query is empty,
   * all columnNames returns true.
   * 
   * @param query The given query.
   * @param columnName The requested column name.
   * @return True if the given column name is requested in the given query.
   */
  private boolean isColumnRequested(Query query, String columnName) {
    // If the query is empty returns true, as if all columns were specified.
    if (query.isEmpty()) {
      return true;
    }
    // Returns true if the requested column id was specified (not case sensitive).
    List<AbstractColumn> columns = query.getSelection().getColumns();
    for (AbstractColumn column : columns) {
      if (column.getId().equalsIgnoreCase(columnName)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns a list of required columns based on the query and the actual columns.
   * 
   * @param query The user selection query.
   * @param availableColumns The list of possible columns.
   * @param subsourceList The list of subsources, or null if subsource display is not desired.
   * @return A List of required columns for the requested data table.
   */
  private List<ColumnDescription> getRequiredColumns(Query query,
      ColumnDescription[] availableColumns, List<Source> subsourceList) {
    List<ColumnDescription> requiredColumns = Lists.newArrayList();
    for (ColumnDescription column : availableColumns) {
      if (isColumnRequested(query, column.getId())) {
        // Always add the column without prefix
        requiredColumns.add(column);
        // Only create subsource columns if we have subsources, and don't do it for timePoint
        if ((subsourceList != null) && (!subsourceList.isEmpty())
            && (!column.getId().equals(TIME_POINT_COLUMN))) {
          for (Source subsource : subsourceList) {
            String subsourceName = subsource.getName();
            String subsourceColumnId = subsourceName + column.getId();
            String subsourceLabel = subsourceName + " " + column.getLabel();
            // System.out.println("Column subsource ID: " + subsourceColumnId +
            // ", subsource label: "
            // + subsourceLabel); // DEBUG
            ColumnDescription colDec =
                new ColumnDescription(subsourceColumnId, column.getType(), subsourceLabel);
            colDec.setCustomProperty("sourceName", subsourceName);
            requiredColumns.add(colDec);
          }
        }
      }
    }
    return requiredColumns;
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

}
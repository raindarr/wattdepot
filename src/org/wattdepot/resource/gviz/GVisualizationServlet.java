package org.wattdepot.resource.gviz;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import javax.servlet.http.HttpServletRequest;
import javax.xml.datatype.XMLGregorianCalendar;
import org.wattdepot.resource.property.jaxb.Properties;
import org.wattdepot.resource.property.jaxb.Property;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.resource.sensordata.jaxb.SensorDataIndex;
import org.wattdepot.resource.sensordata.jaxb.SensorDataRef;
import org.wattdepot.server.Server;
import org.wattdepot.server.db.DbManager;
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
    String sourceName;

    // This is everything following the URI, which should start with "/gviz/source/"
    String rawPath = request.getPathInfo();
    if ((rawPath == null) || ("".equals(rawPath)) || "/".equals(rawPath)) {
      throw new DataSourceException(ReasonType.INVALID_REQUEST, "No Source name provided.");
    }
    if ((rawPath.length() > 1) && (rawPath.startsWith("/"))) {
      // need to strip off leading "/" to get Source name
      sourceName = rawPath.substring(1);
    }
    else {
      // this shouldn't happen, based on my understanding of the path provided to the servlet
      throw new DataSourceException(ReasonType.INTERNAL_ERROR,
          "Internal problem with Source name provided.");
    }
    // Check if the given source name exists in database
    if (dbManager.getSource(sourceName) == null) {
      throw new DataSourceException(ReasonType.INVALID_REQUEST, "No Source named \"" + sourceName
          + "\".");
    }

    // Create a data table,
    DataTable data = new DataTable();
    ArrayList<ColumnDescription> cd = new ArrayList<ColumnDescription>();
    cd.add(new ColumnDescription("timePoint", ValueType.DATETIME, "Date & Time"));
    cd.add(new ColumnDescription("powerConsumed", ValueType.NUMBER, "Power (W)"));
    cd.add(new ColumnDescription("energyConsumedToDate", ValueType.NUMBER, "Energy Consumed (Wh)"));
    cd.add(new ColumnDescription("powerGenerated", ValueType.NUMBER, "Power Generated (Wh)"));
    data.addColumns(cd);

    // Get all sensor data for this Source
    SensorDataIndex index = dbManager.getSensorDataIndex(sourceName);
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
      // System.err.format("timestamp: %s, power: %f, energy: %f%n", ref.getTimestamp().toString(),
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
    return data;
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
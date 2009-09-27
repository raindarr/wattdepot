package org.wattdepot.resource.gviz;

import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;
import org.wattdepot.resource.sensordata.jaxb.Properties;
import org.wattdepot.resource.sensordata.jaxb.Property;
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
 * WattDepot database.
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
      System.err.println("rawPath: " + rawPath);
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
    cd.add(new ColumnDescription("timestamp", ValueType.DATETIME, "Date & Time"));
    cd.add(new ColumnDescription("powerConsumed", ValueType.NUMBER, "Power (W)"));
    cd.add(new ColumnDescription("energyConsumedToDate", ValueType.NUMBER, "Energy Consumed (Wh)"));
    data.addColumns(cd);

    // Get all sensor data for this Source
    SensorDataIndex index = dbManager.getSensorDataIndex(sourceName);
    Properties props;
    // Iterate over each SensorDataRef
    for (SensorDataRef ref : index.getSensorDataRef()) {
      Double powerConsumed = null, energyConsumedToDate = null;
      props = dbManager.getSensorData(sourceName, ref.getTimestamp()).getProperties();
      // Look for properties on this SensorData that we are interested in
      for (Property prop : props.getProperty()) {
        try {
          if (prop.getKey().equals("powerConsumed")) {
            powerConsumed = new Double(prop.getValue());
          }
          else if (prop.getKey().equals("energyConsumedToDate")) {
            energyConsumedToDate = new Double(prop.getValue());
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
        // Calendar conversion hell. GViz library uses com.ibm.icu.util.GregorianCalendar, which is
        // different from java.util.GregorianCalendar. So we go from our native format
        // XMLGregorianCalendar -> GregorianCalendar, extract milliseconds since epoch, feed that
        // to the IBM GregorianCalendar.
        com.ibm.icu.util.GregorianCalendar gvizCal = new com.ibm.icu.util.GregorianCalendar();
        gvizCal.setTimeInMillis(ref.getTimestamp().toGregorianCalendar().getTimeInMillis());
        // Added bonus, DateTimeValue only accepts GregorianCalendars if the timezone is GMT.
        // Need to verify that this isn't shifting times if the timezone was already set in
        // XMLGregorianCalendar.
        gvizCal.setTimeZone(TimeZone.getTimeZone("GMT"));
        // Add a row into the table
        data.addRowFromValues(gvizCal, powerConsumed, energyConsumedToDate);
      }
      catch (TypeMismatchException e) {
        throw new DataSourceException(ReasonType.INTERNAL_ERROR, "Problem adding data to table"); // NOPMD
      }
    }
    return data;
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
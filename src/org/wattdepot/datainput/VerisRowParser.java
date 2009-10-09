package org.wattdepot.datainput;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import javax.xml.datatype.XMLGregorianCalendar;
import org.hackystat.utilities.tstamp.Tstamp;
import org.wattdepot.resource.sensordata.SensorDataUtils;
import org.wattdepot.resource.sensordata.jaxb.Properties;
import org.wattdepot.resource.sensordata.jaxb.Property;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.server.Server;

/**
 * Parses the tabular data format sent by Veris meters monitored by the Obvius Acquisuite device.
 * 
 * @author Robert Brewer
 */
public class VerisRowParser extends RowParser {

  /** Used to parse dates from the meter data, kept in field to reduce object creation. */
  private SimpleDateFormat format;

  /**
   * Creates the RowParser, and initializes fields based on the provided arguments.
   * 
   * @param toolName Name of the tool sending the data.
   * @param serverUri URI of WattDepot server to send data to.
   * @param sourceName Name of Source to send data to.
   */
  public VerisRowParser(String toolName, String serverUri, String sourceName) {
    super(toolName, serverUri, sourceName);
    // Example date format from sensor data: 2009-08-01 00:15:12
    this.format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
  }

  /**
   * Converts a row of the table into an appropriate SensorData object. Child classes will generally
   * override this method to parse the table rows that come from their particular table layout.
   * 
   * @param col The row of the table, with each column represented as a String array element.
   * @return The new SensorData object.
   */
  public SensorData parseRow(String[] col) {
    // Example rows from BMO data (real data is tab separated):
    // time (US/Hawaii) error lowrange highrange Energy Consumption (kWh) Real Power (kW)
    // 2009-08-01 00:00:02 0 0 0 55307.16 3.594
    String dateString = col[0];
    Date newDate = null;
    try {
      newDate = format.parse(dateString);
    }
    catch (java.text.ParseException e) {
      System.err.println("Unable to parse date: " + dateString);
      return null;
    }
    // GregorianCalendar cal = new GregorianCalendar();
    // cal.setTime(newDate);
    XMLGregorianCalendar timestamp = Tstamp.makeTimestamp(newDate.getTime());

    // // DEBUG
    // DateFormat df = DateFormat.getDateTimeInstance();
    // System.out.println("Input date: " + dateString + ", output date: " + df.format(newDate));

    // Create the properties based on the PropertyDictionary
    // http://code.google.com/p/wattdepot/wiki/PropertyDictionary
    String powerConsumedString = col[5];
    double powerConsumed;
    try {
      // Value in file is a String representing a floating point value in kW, while powerConsumed
      // SensorData property is defined in dictionary as being in W, so parse and multiply by 1000.
      powerConsumed = Double.parseDouble(powerConsumedString) * 1000;
    }
    catch (NumberFormatException e) {
      System.err.println("Unable to parse floating point number: " + powerConsumedString);
      return null;
    }
    Property prop1 =
        SensorDataUtils.makeSensorDataProperty("powerConsumed", Double.toString(powerConsumed));

    String energyConsumedToDateString = col[4];
    double energyConsumedToDate;
    try {
      // Value in file is a String representing a floating point value in kWh, while
      // energyConsumedToDate SensorData property is defined in dictionary as being in Wh, so parse
      // and multiply by 1000.
      energyConsumedToDate = Double.parseDouble(energyConsumedToDateString) * 1000;
    }
    catch (NumberFormatException e) {
      System.err.println("Unable to parse floating point number: " + energyConsumedToDateString);
      return null;
    }
    Property prop2 =
        SensorDataUtils.makeSensorDataProperty("energyConsumedToDate", Double
            .toString(energyConsumedToDate));
    Properties props = new Properties();
    props.getProperty().add(prop1);
    props.getProperty().add(prop2);

    return SensorDataUtils.makeSensorData(timestamp, this.toolName, this.serverUri
        + Server.SOURCES_URI + "/" + this.sourceName, props);
  }
}

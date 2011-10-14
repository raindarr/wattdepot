package org.wattdepot.datainput;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import javax.xml.datatype.XMLGregorianCalendar;
import org.wattdepot.resource.property.jaxb.Properties;
import org.wattdepot.resource.property.jaxb.Property;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.resource.source.jaxb.Source;
import org.wattdepot.util.tstamp.Tstamp;

/**
 * Parses the tabular data format logged by Electro Industries Shark 200S meter (by way of Excel).
 * 
 * @author Robert Brewer
 */
public class SharkRowParser extends RowParser {

  /** Used to parse dates from the meter data, kept in field to reduce object creation. */
  private SimpleDateFormat format;

  /**
   * Creates the VerisRowParser, and initializes fields based on the provided arguments.
   * 
   * @param toolName Name of the tool sending the data.
   * @param serverUri URI of WattDepot server to send data to.
   * @param sourceName Name of Source to send data to.
   */
  public SharkRowParser(String toolName, String serverUri, String sourceName) {
    super(toolName, serverUri, sourceName);
    // Example date format from sensor data: 10/7/11 0:10
    this.format = new SimpleDateFormat("MM/dd/yy HH:mm", Locale.US);
  }

  /**
   * Converts a row of the table into an appropriate SensorData object.
   * 
   * @param col The row of the table, with each column represented as a String array element.
   * @return The new SensorData object, or null if this row didn't contain useful power data (due to
   * some error on meter side).
   * @throws RowParseException If there are problems parsing the row.
   */
  @Override
  public SensorData parseRow(String[] col) throws RowParseException {
    // Example rows from Shark data massaged through Excel:
    // Date/Time,Watts,W-hours
    // 10/7/11 0:10,5222.166016,68350
    if ((col == null) || (col.length < 3)) {
      // row is missing some columns, so don't try parsing, just give up
      throw new RowParseException("Row missing some columns.");
    }
    String dateString = col[0];
    Date newDate = null;
    try {
      newDate = format.parse(dateString);
    }
    catch (java.text.ParseException e) {
      throw new RowParseException("Bad timestamp found in input file: " + dateString, e);
    }
    XMLGregorianCalendar timestamp = Tstamp.makeTimestamp(newDate.getTime());

    // DEBUG
    // DateFormat df = DateFormat.getDateTimeInstance();
    // System.out.println("Input date: " + dateString + ", output date: " + df.format(newDate));

    // Create the properties based on the PropertyDictionary
    // http://code.google.com/p/wattdepot/wiki/PropertyDictionary
    String powerConsumedString = col[1];
    double powerConsumed;
    try {
      // Value in file is a String representing a floating point value in W, so just parse
      powerConsumed = Double.parseDouble(powerConsumedString);
    }
    catch (NumberFormatException e) {
      throw new RowParseException("Unable to parse floating point number: " + powerConsumedString,
          e);
    }
    Property prop1 = new Property(SensorData.POWER_CONSUMED, Double.toString(powerConsumed));

    String energyConsumedToDateString = col[2];
    double energyConsumedToDate;
    try {
      // Value in file is a String representing an integer value in Wh, so just parse
      energyConsumedToDate = Double.parseDouble(energyConsumedToDateString);
    }
    catch (NumberFormatException e) {
      throw new RowParseException("Unable to parse floating point number: "
          + energyConsumedToDateString, e);
    }
    Property prop2 =
        new Property(SensorData.ENERGY_CONSUMED_TO_DATE, Double.toString(energyConsumedToDate));
    Properties props = new Properties();
    props.getProperty().add(prop1);
    props.getProperty().add(prop2);

    return new SensorData(timestamp, this.toolName, Source.sourceToUri(this.sourceName,
        this.serverUri), props);
  }
}

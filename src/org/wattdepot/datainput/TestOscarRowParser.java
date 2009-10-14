package org.wattdepot.datainput;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import org.hackystat.utilities.tstamp.Tstamp;
import org.junit.Test;
import org.wattdepot.resource.sensordata.SensorDataUtils;
import org.wattdepot.resource.sensordata.jaxb.Properties;
import org.wattdepot.resource.sensordata.jaxb.Property;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.resource.source.SourceUtils;

/**
 * Tests the OscarRowParser class.
 * 
 * @author Robert Brewer
 */
public class TestOscarRowParser {

  private static final String TOOL_NAME = "JUnit";
  private static final String SERVER_URI = "http://server.wattdepot.org:1234/wattdepot/";

  /**
   * Tests whether a valid row is parsed properly into a SensorData object.
   * 
   * @throws Exception If there are problems converting the timestamp.
   */
  @Test
  public void testValidRow() throws Exception {
    RowParser parser = new OscarRowParser(TOOL_NAME, SERVER_URI);
    // Example row from Oscar data
    String row = "2009-10-12T00:15:00-1000,SIM_HPOWER,46,5,BASELOAD";
    Property powerGenerated = SensorDataUtils.makeSensorDataProperty("powerGenerated", "4.6E7");
    Properties props = new Properties();
    props.getProperty().add(powerGenerated);
    SensorData data =
        SensorDataUtils.makeSensorData(Tstamp.makeTimestamp("2009-10-12T00:15:00.000-10:00"),
            TOOL_NAME, SourceUtils.sourceToUri("SIM_HPOWER", SERVER_URI), props);
    SensorData parsedData = parser.parseRow(row.split(","));
    assertEquals("Parsed sensor data differs from hand-created sensor data", data, parsedData);
  }
  
  /**
   * Tests whether a valid row is parsed properly into a SensorData object.
   * 
   * @throws RowParseException If there are problems parsing the row.
   */
  @Test(expected = RowParseException.class)
  public void testMissingColumns() throws RowParseException {
    RowParser parser = new OscarRowParser(TOOL_NAME, SERVER_URI);
    String row = "2009-10-12T00:15:00-1000,SIM_HPOWER,,";
    SensorData parsedData = parser.parseRow(row.split(","));
    assertNull("Row with missing columns was successfully parsed", parsedData);
  }
  
  /**
   * Tests whether a valid row is parsed properly into a SensorData object.
   * 
   * @throws RowParseException If there are problems parsing the row.
   */
  @Test(expected = RowParseException.class)
  public void testEmptyRow() throws RowParseException {
    RowParser parser = new OscarRowParser(TOOL_NAME, SERVER_URI);
    String[] row = {""};
    SensorData parsedData = parser.parseRow(row);
    assertNull("Empty string row was parsed successfully", parsedData);
  }

  /**
   * Tests whether a valid row is parsed properly into a SensorData object.
   * 
   * @throws RowParseException If there are problems parsing the row.
   */
  @Test(expected = RowParseException.class)
  public void testNullRow() throws RowParseException {
    RowParser parser = new OscarRowParser(TOOL_NAME, SERVER_URI);
    String[] row = null;
    SensorData parsedData = parser.parseRow(row);
    assertNull("Null row was parsed successfully", parsedData);
  }

  /**
   * Tests whether a valid row is parsed properly into a SensorData object.
   */
  @Test
  public void testBogusColumns() {
    RowParser parser = new OscarRowParser(TOOL_NAME, SERVER_URI);
    String row;
    row = "Time,SIM_HPOWER,46,5,BASELOAD";
    try {
      parser.parseRow(row.split(","));
      fail("Row with bogus time column was successfully parsed");
    }
    catch (RowParseException e) { // NOPMD
      // Do nothing, this is expected behavior
    }
    row = "2009-10-12T00:15:00-1000,SIM_HPOWER,GridMW,5,BASELOAD";
    try {
      parser.parseRow(row.split(","));
      fail("Row with bogus energyConsumed column was successfully parsed");
    }
    catch (RowParseException e) { // NOPMD
      // Do nothing, this is expected behavior
    }
  }
}

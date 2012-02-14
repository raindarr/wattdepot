package org.wattdepot.datainput;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.wattdepot.resource.property.jaxb.Property;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.resource.source.jaxb.Source;
import org.wattdepot.util.tstamp.Tstamp;

/**
 * Tests the SharkLogParser class.
 * 
 * @author Robert Brewer
 */
public class TestSharkRowParser {

  private static final String SOURCE_NAME = "foo-source";
  private static final String TOOL_NAME = "JUnit";
  private static final String SERVER_URI = "http://server.wattdepot.org:1234/wattdepot/";

  /**
   * Tests whether a valid row is parsed properly into a SensorData object.
   * 
   * @throws Exception If there are problems converting the timestamp.
   */
  @Test
  public void testValidRow() throws Exception {
    RowParser parser = new SharkRowParser(TOOL_NAME, SERVER_URI, SOURCE_NAME);
    String row = "10/6/11 23:55,4865.527344,67080";
    SensorData data =
      new SensorData(Tstamp.makeTimestamp("2011-10-06T23:55:00.000-10:00"), TOOL_NAME, Source
          .sourceToUri(SOURCE_NAME, SERVER_URI));
    Property powerConsumed = new Property(SensorData.POWER_CONSUMED, "4865.527344");
    Property energyConsumed = new Property(SensorData.ENERGY_CONSUMED_TO_DATE, "67080.0");
    data.addProperty(powerConsumed);
    data.addProperty(energyConsumed);
    SensorData parsedData = parser.parseRow(row.split(","));
    assertEquals("Parsed sensor data differs from hand-created sensor data", data, parsedData);
  }

  /**
   * Tests whether a row missing columns raises an exception.
   * 
   * @throws RowParseException If there are problems parsing the row.
   */
  @Test(expected = RowParseException.class)
  public void testMissingColumns() throws RowParseException {
    RowParser parser = new SharkRowParser(TOOL_NAME, SERVER_URI, SOURCE_NAME);
    // Example row from 5mauka meter
    String row = "10/6/11 23:55,4865.527344";
    SensorData parsedData = parser.parseRow(row.split(","));
    assertNull("Row with missing columns was successfully parsed", parsedData);
  }

  /**
   * Tests whether an empty row raises an exception.
   * 
   * @throws RowParseException If there are problems parsing the row.
   */
  @Test(expected = RowParseException.class)
  public void testEmptyRow() throws RowParseException {
    RowParser parser = new SharkRowParser(TOOL_NAME, SERVER_URI, SOURCE_NAME);
    String[] row = { "" };
    SensorData parsedData = parser.parseRow(row);
    assertNull("Empty string row was parsed successfully", parsedData);
  }

  /**
   * Tests whether an null row raises an exception.
   * 
   * @throws RowParseException If there are problems parsing the row.
   */
  @Test(expected = RowParseException.class)
  public void testNullRow() throws RowParseException {
    RowParser parser = new SharkRowParser(TOOL_NAME, SERVER_URI, SOURCE_NAME);
    String[] row = null;
    SensorData parsedData = parser.parseRow(row);
    assertNull("Null row was parsed successfully", parsedData);
  }

  /**
   * Tests whether a row of bogus data raises an exception.
   */
  @Test
  public void testBogusColumns() {
    RowParser parser = new SharkRowParser(TOOL_NAME, SERVER_URI, SOURCE_NAME);
    String row;
    row = "foobar,3558.208252,6190";
    try {
      parser.parseRow(row.split(","));
      fail("Row with bogus time column was successfully parsed");
    }
    catch (RowParseException e) { // NOPMD
      // Do nothing, this is expected behavior
    }
    row = "10/6/11 11:30,foobar,6190";
    try {
      parser.parseRow(row.split(","));
      fail("Row with bogus energyConsumed column was successfully parsed");
    }
    catch (RowParseException e) { // NOPMD
      // Do nothing, this is expected behavior
    }
    row = "10/6/11 11:30,3558.208252,foobar";
    try {
      parser.parseRow(row.split(","));
      fail("Row with bogus powerConsumed column was successfully parsed");
    }
    catch (RowParseException e) { // NOPMD
      // Do nothing, this is expected behavior
    }
  }
}

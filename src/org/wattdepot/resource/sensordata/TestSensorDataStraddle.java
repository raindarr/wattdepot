package org.wattdepot.resource.sensordata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import javax.xml.datatype.XMLGregorianCalendar;
import org.hackystat.utilities.tstamp.Tstamp;
import org.junit.Test;
import org.wattdepot.resource.sensordata.jaxb.Properties;
import org.wattdepot.resource.sensordata.jaxb.Property;
import org.wattdepot.resource.sensordata.jaxb.SensorData;

/**
 * Tests the SensorDataStraddle class.
 * 
 * @author Robert Brewer
 */
public class TestSensorDataStraddle {

  private static final String POWER_GENERATED = "powerGenerated";

  /**
   * Tests the constructor, ensuring that it does not accept invalid data.
   * 
   * @throws Exception If there are problems creating timestamps.
   */
  @Test
  @SuppressWarnings("PMD.EmptyCatchBlock")
  public void testConstructor() throws Exception {
    XMLGregorianCalendar time1 = Tstamp.makeTimestamp("2009-07-28T08:00:00.000-10:00");
    XMLGregorianCalendar time2 = Tstamp.makeTimestamp("2009-07-28T09:00:00.000-10:00");
    XMLGregorianCalendar time3 = Tstamp.makeTimestamp("2009-07-28T09:07:00.000-10:00");
    XMLGregorianCalendar time4 = Tstamp.makeTimestamp("2009-07-28T09:23:00.000-10:00");
    XMLGregorianCalendar time5 = Tstamp.makeTimestamp("2009-07-28T09:30:00.000-10:00");
    String tool = "JUnit";
    String source = "http://server.wattdepot.org:1234/wattdepot/sources/foo-source";
    SensorData data1 = SensorDataUtils.makeSensorData(time1, tool, source, null), data2 =
        SensorDataUtils.makeSensorData(time2, tool, source, null), data4 =
        SensorDataUtils.makeSensorData(time4, tool, source, null), data5 =
        SensorDataUtils.makeSensorData(time5, tool, source, null);

    // all null
    try {
      new SensorDataStraddle(null, null, null);
      fail("Able to create SensorDataStraddle with null beforeData");
    }
    catch (IllegalArgumentException e) {
      // expected in this case
    }
    // afterData null
    try {
      new SensorDataStraddle(null, data1, null);
      fail("Able to create SensorDataStraddle with null afterData");
    }
    catch (IllegalArgumentException e) {
      // expected in this case
    }
    // swapped order
    try {
      new SensorDataStraddle(null, data2, data1);
      fail("Able to create SensorDataStraddle with beforeData > afterData");
    }
    catch (IllegalArgumentException e) {
      // expected in this case
    }
    // timestamp before range
    try {
      new SensorDataStraddle(time1, data2, data5);
      fail("Able to create SensorDataStraddle with timestamp < beforeData");
    }
    catch (IllegalArgumentException e) {
      // expected in this case
    }
    // timestamp after range
    try {
      new SensorDataStraddle(time5, data1, data4);
      fail("Able to create SensorDataStraddle with timestamp > afterData");
    }
    catch (IllegalArgumentException e) {
      // expected in this case
    }
    // timestamp equal to before
    assertNotNull("Unable to create SensorDataStraddle with timestamp == beforeData",
        new SensorDataStraddle(time2, data2, data4));
    // timestamp equal to after
    assertNotNull("Unable to create SensorDataStraddle with timestamp == afterData",
        new SensorDataStraddle(time4, data2, data4));
    // degenerate case: timestamp == beforeData == afterData
    assertNotNull("Unable to create SensorDataStraddle with timestamp == beforeData == afterData",
        new SensorDataStraddle(time2, data2, data2));
    // timestamp right in middle
    assertNotNull("Unable to create SensorDataStraddle with timestamp in range",
        new SensorDataStraddle(time3, data1, data5));
  }

  /**
   * Tests the interpolation code for getting power values.
   * 
   * @throws Exception If there are problems creating timestamps.
   */
  @Test
  public void testGetPower() throws Exception {
    XMLGregorianCalendar beforeTime, afterTime, timestamp;
    SensorData beforeData, afterData;
    String tool = "JUnit";
    String source = "http://server.wattdepot.org:1234/wattdepot/sources/foo-source";
    Properties beforeProps, afterProps;
    Property beforeProp, afterProp;
    SensorDataStraddle straddle;
    double interpolatedPower;

    // timestamp == beforeData == afterData, getPower should just return beforeData
    beforeTime = Tstamp.makeTimestamp("2009-07-28T08:00:00.000-10:00");
    beforeProp = SensorDataUtils.makeSensorDataProperty(POWER_GENERATED, "100");
    beforeProps = new Properties();
    beforeProps.getProperty().add(beforeProp);
    beforeData = SensorDataUtils.makeSensorData(beforeTime, tool, source, beforeProps);
    timestamp = beforeTime;
    straddle = new SensorDataStraddle(timestamp, beforeData, beforeData);
    assertEquals("getPower on degenerate straddle did not return beforeData", straddle.getPower(),
        beforeData);

    // slope is 2 (100 W difference in 50 seconds)
    beforeTime = Tstamp.makeTimestamp("2009-07-28T08:00:00.000-10:00");
    afterTime = Tstamp.makeTimestamp("2009-07-28T08:00:50.000-10:00");
    beforeProp = SensorDataUtils.makeSensorDataProperty(POWER_GENERATED, "100");
    beforeProps = new Properties();
    beforeProps.getProperty().add(beforeProp);
    afterProp = SensorDataUtils.makeSensorDataProperty(POWER_GENERATED, "200");
    afterProps = new Properties();
    afterProps.getProperty().add(afterProp);
    beforeData = SensorDataUtils.makeSensorData(beforeTime, tool, source, beforeProps);
    afterData = SensorDataUtils.makeSensorData(afterTime, tool, source, afterProps);
    timestamp = Tstamp.makeTimestamp("2009-07-28T08:00:25.000-10:00");
    straddle = new SensorDataStraddle(timestamp, beforeData, afterData);
    interpolatedPower =
        Double.valueOf(straddle.getPower().getProperties().getProperty().get(0).getValue());
    Property interpolatedProp = SensorDataUtils.makeSensorDataProperty("interpolated", "true");
    assertEquals("Interpolated power did not equal expected value", 150, interpolatedPower, 0.01);
    assertTrue("Interpolated property not found", straddle.getPower().getProperties().getProperty()
        .contains(interpolatedProp));

    // Computed by hand from Oscar data
    beforeTime = Tstamp.makeTimestamp("2009-10-12T00:00:00.000-10:00");
    afterTime = Tstamp.makeTimestamp("2009-10-12T00:15:00.000-10:00");
    beforeProp = SensorDataUtils.makeSensorDataProperty(POWER_GENERATED, "5.5E7");
    beforeProps = new Properties();
    beforeProps.getProperty().add(beforeProp);
    afterProp = SensorDataUtils.makeSensorDataProperty(POWER_GENERATED, "6.4E7");
    afterProps = new Properties();
    afterProps.getProperty().add(afterProp);
    beforeData = SensorDataUtils.makeSensorData(beforeTime, tool, source, beforeProps);
    afterData = SensorDataUtils.makeSensorData(afterTime, tool, source, afterProps);
    timestamp = Tstamp.makeTimestamp("2009-10-12T00:13:00.000-10:00");
    straddle = new SensorDataStraddle(timestamp, beforeData, afterData);
    interpolatedPower =
        Double.valueOf(straddle.getPower().getProperties().getProperty().get(0).getValue());
//    System.out.println(interpolatedPower);
    assertEquals("Interpolated power did not equal expected value", 6.28E7, interpolatedPower, 0.01);
    assertTrue("Interpolated property not found", straddle.getPower().getProperties().getProperty()
        .contains(interpolatedProp));
  }
}

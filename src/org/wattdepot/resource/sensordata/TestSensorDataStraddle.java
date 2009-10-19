package org.wattdepot.resource.sensordata;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import javax.xml.datatype.XMLGregorianCalendar;
import org.hackystat.utilities.tstamp.Tstamp;
import org.junit.Test;
import org.wattdepot.resource.sensordata.jaxb.SensorData;

/**
 * Tests the SensorDataStraddle class.
 * 
 * @author Robert Brewer
 */
public class TestSensorDataStraddle {

  /**
   * Tests the constructor, ensuring that it does not accept invalid data.
   * 
   * @throws Exception If there are problems creating timestamps.
   */
  @Test
  @SuppressWarnings("PMD.EmptyCatchBlock")
  public void testConstructor() throws Exception {
    XMLGregorianCalendar time1 = Tstamp.makeTimestamp("2009-07-28T08:00:00.000-10:00"),
    time2 = Tstamp.makeTimestamp("2009-07-28T09:00:00.000-10:00"),
    time3 = Tstamp.makeTimestamp("2009-07-28T09:07:00.000-10:00"),
    time4 = Tstamp.makeTimestamp("2009-07-28T09:23:00.000-10:00"),
    time5 = Tstamp.makeTimestamp("2009-07-28T09:30:00.000-10:00");
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
}

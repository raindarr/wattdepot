package org.wattdepot.resource.energy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import javax.xml.datatype.XMLGregorianCalendar;
import org.junit.Test;
import org.wattdepot.resource.property.jaxb.Property;
import org.wattdepot.resource.sensordata.SensorDataStraddle;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.util.tstamp.Tstamp;

/**
 * Tests the Energy class.
 * 
 * @author Robert Brewer
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class TestEnergy {

  /**
   * Tests the code for getting energy values.
   * 
   * @throws Exception If there are problems creating timestamps.
   */
  @Test
  public void testGetEnergy() throws Exception {
    XMLGregorianCalendar beforeTime, afterTime, timestamp;
    SensorData beforeData, afterData;
    String source = "http://server.wattdepot.org:1234/wattdepot/sources/foo-source";
    SensorDataStraddle straddle1, straddle2;
    Energy energy;

    // getEnergy for degenerate straddles with flat power generation
    beforeTime = Tstamp.makeTimestamp("2009-07-28T08:00:00.000-10:00");
    beforeData = SensorDataStraddle.makePowerSensorData(beforeTime, source, 100.0, 0, false);
    timestamp = beforeTime;
    straddle1 = new SensorDataStraddle(timestamp, beforeData, beforeData);
    beforeTime = Tstamp.makeTimestamp("2009-07-28T09:00:00.000-10:00");
    beforeData = SensorDataStraddle.makePowerSensorData(beforeTime, source, 100.0, 0, false);
    timestamp = beforeTime;
    straddle2 = new SensorDataStraddle(timestamp, beforeData, beforeData);
    energy = new Energy(straddle1, straddle2);
    assertEquals("getEnergyGenerated on degenerate straddles with flat power was wrong", 100.0,
        energy.getEnergyGenerated(), 0.01);

    // Now power increases by double over one hour
    beforeTime = Tstamp.makeTimestamp("2009-07-28T08:00:00.000-10:00");
    beforeData = SensorDataStraddle.makePowerSensorData(beforeTime, source, 100.0, 0, false);
    timestamp = beforeTime;
    straddle1 = new SensorDataStraddle(timestamp, beforeData, beforeData);
    beforeTime = Tstamp.makeTimestamp("2009-07-28T09:00:00.000-10:00");
    beforeData = SensorDataStraddle.makePowerSensorData(beforeTime, source, 200.0, 0, false);
    timestamp = beforeTime;
    straddle2 = new SensorDataStraddle(timestamp, beforeData, beforeData);
    energy = new Energy(straddle1, straddle2);
    assertEquals("getEnergyGenerated on degenerate straddles with doubling power was wrong", 150.0,
        energy.getEnergyGenerated(), 0.01);

    // Now power decreases by half over one day
    beforeTime = Tstamp.makeTimestamp("2009-07-28T08:00:00.000-10:00");
    beforeData = SensorDataStraddle.makePowerSensorData(beforeTime, source, 100.0, 0, false);
    timestamp = beforeTime;
    straddle1 = new SensorDataStraddle(timestamp, beforeData, beforeData);
    beforeTime = Tstamp.makeTimestamp("2009-07-29T08:00:00.000-10:00");
    beforeData = SensorDataStraddle.makePowerSensorData(beforeTime, source, 50.0, 0, false);
    timestamp = beforeTime;
    straddle2 = new SensorDataStraddle(timestamp, beforeData, beforeData);
    energy = new Energy(straddle1, straddle2);
    assertEquals("getEnergyGenerated on degenerate straddles with doubling power was wrong",
        1800.0, energy.getEnergyGenerated(), 0.01);
    Property interpolatedProp = new Property("interpolated", "true");
    assertTrue("Interpolated property not found", energy.getEnergy().containsProperty(
        interpolatedProp));

    // Computed by hand from Oscar data
    beforeTime = Tstamp.makeTimestamp("2009-10-12T00:00:00.000-10:00");
    afterTime = Tstamp.makeTimestamp("2009-10-12T00:15:00.000-10:00");
    beforeData = SensorDataStraddle.makePowerSensorData(beforeTime, source, 5.5E7, 0, false);
    afterData = SensorDataStraddle.makePowerSensorData(afterTime, source, 6.4E7, 0, false);
    timestamp = Tstamp.makeTimestamp("2009-10-12T00:13:00.000-10:00");
    straddle1 = new SensorDataStraddle(timestamp, beforeData, afterData);
    beforeTime = Tstamp.makeTimestamp("2009-10-12T00:30:00.000-10:00");
    afterTime = Tstamp.makeTimestamp("2009-10-12T00:45:00.000-10:00");
    beforeData = SensorDataStraddle.makePowerSensorData(beforeTime, source, 5.0E7, 0, false);
    afterData = SensorDataStraddle.makePowerSensorData(afterTime, source, 5.4E7, 0, false);
    timestamp = Tstamp.makeTimestamp("2009-10-12T00:42:00.000-10:00");
    straddle2 = new SensorDataStraddle(timestamp, beforeData, afterData);
    energy = new Energy(straddle1, straddle2);
    assertEquals("getEnergyGenerated on Oscar data was wrong", 2.8033333333333332E7, energy
        .getEnergyGenerated(), 0.1);
  }
}

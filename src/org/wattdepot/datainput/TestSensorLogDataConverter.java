package org.wattdepot.datainput;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.wattdepot.resource.property.jaxb.Properties;
import org.wattdepot.resource.property.jaxb.Property;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.util.tstamp.Tstamp;

/**
 * Tests the SensorLogDataConverter class.
 * 
 * @author Robert Brewer
 */
public class TestSensorLogDataConverter {

  /**
   * Tests whether a valid log entry is parsed properly into a SensorData object.
   * 
   * @throws Exception If there are problems converting the timestamp.
   */
  @Test
  public void testValidRow() throws Exception {
    SensorLogDataConverter converter = new SensorLogDataConverter(null, null, null, true);

    // Example row from log data
    String logEntry =
        "SensorData [properties=[Property [key=powerConsumed, "
            + "value=2422.90185546875], Property [key=energyConsumedToDate, value=8265580.0]], "
            + "source=http://server.wattdepot.org:8190/wattdepot/sources/Ilima-04-lounge, "
            + "timestamp=2012-01-08T16:30:35.339-10:00, tool=Shark200SSensor]";
    Properties props = new Properties();
    props.getProperty().add(new Property("powerConsumed", "2422.90185546875"));
    props.getProperty().add(new Property("energyConsumedToDate", "8265580.0"));
    SensorData data =
        new SensorData(Tstamp.makeTimestamp("2012-01-08T16:30:35.339-10:00"), "Shark200SSensor",
            "http://server.wattdepot.org:8190/wattdepot/sources/Ilima-04-lounge", props);
    SensorData parsedData = converter.parseLogEntry(logEntry);
    assertEquals("Parsed sensor data differs from hand-created sensor data", data, parsedData);
  }
}

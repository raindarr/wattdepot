package org.wattdepot.server.db.berkeleydb;

import javax.xml.datatype.XMLGregorianCalendar;
import org.wattdepot.resource.property.jaxb.Properties;
import org.wattdepot.resource.property.jaxb.Property;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.resource.source.jaxb.Source;
import org.wattdepot.server.Server;
import org.wattdepot.util.tstamp.Tstamp;
import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.KeyField;
import com.sleepycat.persist.model.Persistent;
import com.sleepycat.persist.model.PrimaryKey;

/**
 * Implementation of SensorData that is backed by BerkeleyDB.
 * 
 * @author George Lee
 * 
 */
@Entity
public class BerkeleyDbSensorData {
  @PrimaryKey
  private CompositeSensorDataKey compositeKey;
  private String tool;
  private Double powerConsumed;
  private Double energyConsumedToDate;
  private Double powerGenerated;
  private Double energyGeneratedToDate;
  private long lastMod;

  /**
   * Default constructor as required by BerkeleyDB.
   */
  public BerkeleyDbSensorData() {
    // Required by BerkeleyDB.
  }

  /**
   * Create a BerkeleyDbSensorData instance using parameters from SensorData.
   * 
   * @param timestamp The timestamp of the sensor data.
   * @param tool The tool the sensor data was recorded with.
   * @param sourceName The sourceName of the sensor data.
   */
  public BerkeleyDbSensorData(XMLGregorianCalendar timestamp, String tool, String sourceName) {
    this.compositeKey = new CompositeSensorDataKey(sourceName, timestamp);
    this.tool = tool;
    this.lastMod = Tstamp.makeTimestamp().toGregorianCalendar().getTimeInMillis();
  }

  /**
   * Create a BerkeleyDbSensorData instance using parameters from SensorData.
   * 
   * @param timestamp The timestamp of the sensor data.
   * @param tool The tool the sensor data was recorded with.
   * @param sourceName The sourceName of the sensor data.
   * @param properties The properties of the sensor data.
   */
  public BerkeleyDbSensorData(XMLGregorianCalendar timestamp, String tool, String sourceName,
      Properties properties) {
    this(timestamp, tool, sourceName);

    if (properties != null && properties.getProperty() != null) {
      if (properties.getProperty(SensorData.POWER_CONSUMED) != null) {
        this.powerConsumed = properties.getPropertyAsDouble(SensorData.POWER_CONSUMED);
      }
      if (properties.getProperty(SensorData.ENERGY_CONSUMED_TO_DATE) != null) {
        this.energyConsumedToDate =
            properties.getPropertyAsDouble(SensorData.ENERGY_CONSUMED_TO_DATE);
      }
      if (properties.getProperty(SensorData.POWER_GENERATED) != null) {
        this.powerGenerated = properties.getPropertyAsDouble(SensorData.POWER_GENERATED);
      }
      if (properties.getProperty(SensorData.ENERGY_GENERATED_TO_DATE) != null) {
        this.energyGeneratedToDate =
            properties.getPropertyAsDouble(SensorData.ENERGY_GENERATED_TO_DATE);
      }
    }
  }

  /**
   * Converts the BerkeleyDB representation of sensor data to the jaxb SensorData. We need the
   * Server in order to change the source name to a source uri.
   * 
   * @param server The WattDepot server that this sensor data is on.
   * 
   * @return Instance of SensorData with the same properties as this.
   */
  public SensorData asSensorData(Server server) {
    SensorData returnData = new SensorData();
    returnData.setSource(Source.sourceToUri(this.compositeKey.getSourceName(), server));
    returnData.setTimestamp(this.getTimestamp());
    returnData.setTool(this.tool);

    if (this.powerConsumed != null) {
      returnData.addProperty(new Property(SensorData.POWER_CONSUMED, this.powerConsumed));
    }
    if (this.energyConsumedToDate != null) {
      returnData.addProperty(new Property(SensorData.ENERGY_CONSUMED_TO_DATE,
          this.energyConsumedToDate));
    }
    if (this.powerGenerated != null) {
      returnData.addProperty(new Property(SensorData.POWER_GENERATED, this.powerGenerated));
    }
    if (this.energyGeneratedToDate != null) {
      returnData.addProperty(new Property(SensorData.ENERGY_GENERATED_TO_DATE,
          this.energyGeneratedToDate));
    }

    return returnData;
  }

  /**
   * Get the compositeKey (which contains the sourceName and timestamp) associated with this sensor
   * data.
   * 
   * @return The compositeKey for this sensor data.
   */
  public CompositeSensorDataKey getCompositeKey() {
    return this.compositeKey;
  }

  /**
   * Get the timestamp associated with this sensor data.
   * 
   * @return The timestamp of this sensor data in XMLGregorianCalendar format.
   */
  public XMLGregorianCalendar getTimestamp() {
    return Tstamp.makeTimestamp(this.compositeKey.getTimestamp());
  }

  /**
   * Get the tool associated with this sensor data.
   * 
   * @return The tool associated with this sensor data.
   */
  public String getTool() {
    return this.tool;
  }

  /**
   * Get the sourceName associated with this sensor data.
   * 
   * @return The sourceName assoiated with this sensor data.
   */
  public String getSourceName() {
    return this.compositeKey.getSourceName();
  }

  /**
   * Get the last modification time of this sensor data as a long.
   * 
   * @return The last modification time of this sensor as a long.
   */
  public long lastMod() {
    return this.lastMod;
  }

}

/**
 * Represents a composite key for sensor data in BerkeleyDB.
 * 
 * @author George Lee
 * 
 */
@Persistent
class CompositeSensorDataKey {
  @KeyField(1)
  private String sourceName;
  @KeyField(2)
  private long timestamp;

  /**
   * Default constructor required by BerkeleyDB.
   */
  CompositeSensorDataKey() {
    // Required by BerkeleyDB.
  }

  /**
   * Constructor for our composite key.
   * 
   * @param sourceName The name of the sourceName.
   * @param timestamp The timestamp of the sourceName data.
   */
  CompositeSensorDataKey(String sourceName, XMLGregorianCalendar timestamp) {
    this.timestamp = timestamp.toGregorianCalendar().getTimeInMillis();
    this.sourceName = sourceName;
  }

  /**
   * Get the sourceName associated with this compositeKey.
   * 
   * @return The sourceName associated with this compositeKey.
   */
  String getSourceName() {
    return this.sourceName;
  }

  /**
   * Get the timestamp associated with this compositeKey in long format.
   * 
   * @return The timestamp associated with this compositeKey represented as a long.
   */
  long getTimestamp() {
    return this.timestamp;
  }
}
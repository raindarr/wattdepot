package org.wattdepot.server.db.berkeleydb;

import javax.xml.datatype.XMLGregorianCalendar;
import org.wattdepot.resource.property.jaxb.Property;
import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.KeyField;
import com.sleepycat.persist.model.Persistent;
import com.sleepycat.persist.model.PrimaryKey;
import com.sleepycat.persist.model.SecondaryKey;
import static com.sleepycat.persist.model.Relationship.MANY_TO_ONE;

/**
 * Implementation of SensorDataProperty that is backed by BerkeleyDB.
 * 
 * @author Andrea Connell
 * 
 */

@Entity
public class BerkeleyDbSensorDataProperty {

  @PrimaryKey
  private CompositeSensorDataPropertyKey compositeKey;
  @SecondaryKey(relate = MANY_TO_ONE)
  private CompositeSensorDataKey sensorDataKey;
  private String propertyValue;

  /**
   * Default constructor as required by BerkeleyDB.
   */
  public BerkeleyDbSensorDataProperty() {
    // Required by BerkeleyDB.
  }

  /**
   * Initialize the sensor data property given the source name, timestamp, property key, and
   * property value.
   * 
   * @param sourceName The name of the source this property is associated with.
   * @param timestamp The timestamp that this property is associated with.
   * @param propertyKey The property key.
   * @param propertyValue The property value.
   */
  public BerkeleyDbSensorDataProperty(String sourceName, XMLGregorianCalendar timestamp,
      String propertyKey, String propertyValue) {
    this.compositeKey = new CompositeSensorDataPropertyKey(sourceName, timestamp, propertyKey);
    this.sensorDataKey = new CompositeSensorDataKey(sourceName, timestamp);
    this.propertyValue = propertyValue;
  }

  /**
   * Get the composite key (which contains the source name, timestamp, and property key) for this
   * property.
   * 
   * @return The composite key for this property.
   */
  CompositeSensorDataPropertyKey getCompositeKey() {
    return this.compositeKey;
  }

  /**
   * Get the value of this property.
   * 
   * @return The property value of this property.
   */
  public String getPropertyValue() {
    return this.propertyValue;
  }

  /**
   * Get the name of the source associated with this property.
   * 
   * @return The name of the source associated with this property.
   */
  public CompositeSensorDataKey getSensorDataKey() {
    return this.sensorDataKey;
  }

  /**
   * Get a WattDepot representation of this Property.
   * 
   * @return WattDepot representation of this property.
   */
  public Property asSensorDataProperty() {
    Property property = new Property();
    property.setKey(this.compositeKey.getPropertyKey());
    property.setValue(this.getPropertyValue());
    return property;
  }
}

/**
 * Represents a composite key for sensor data properties in BerkeleyDB.
 * 
 * @author Andrea Connell
 * 
 */
@Persistent
class CompositeSensorDataPropertyKey {
  @KeyField(1)
  private String sourceName;
  @KeyField(2)
  private long timestamp;
  @KeyField(3)
  private String propertyKey;

  /**
   * Default constructor required by BerkeleyDB.
   */
  CompositeSensorDataPropertyKey() {
    // Required by BerkeleyDB.
  }

  /**
   * Constructor for our composite key.
   * 
   * @param sourceName The name of the source.
   * @param timestamp The timestamp of the source data.
   * @param propertyKey The key of the property.
   */
  CompositeSensorDataPropertyKey(String sourceName, XMLGregorianCalendar timestamp, String propertyKey) {
    this.timestamp = timestamp.toGregorianCalendar().getTimeInMillis();
    this.sourceName = sourceName;
    this.propertyKey = propertyKey;
  }

  /**
   * Get the source name associated with this compositeKey.
   * 
   * @return The source name associated with this compositeKey.
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

  /**
   * Get the property key associated with this compositeKey.
   * 
   * @return The property key associated with this compositeKey.
   */
  String getPropertyKey() {
    return this.propertyKey;
  }
}

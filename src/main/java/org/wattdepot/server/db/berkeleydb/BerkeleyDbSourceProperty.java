package org.wattdepot.server.db.berkeleydb;

import org.wattdepot.resource.property.jaxb.Property;
import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.KeyField;
import com.sleepycat.persist.model.Persistent;
import com.sleepycat.persist.model.PrimaryKey;
import com.sleepycat.persist.model.SecondaryKey;
import static com.sleepycat.persist.model.Relationship.MANY_TO_ONE;

/**
 * Implementation of SourceProperty that is backed by BerkeleyDB.
 * 
 * @author Andrea Connell
 * 
 */

@Entity
public class BerkeleyDbSourceProperty {
  @PrimaryKey
  private CompositeSourcePropertyKey compositeKey;
  @SecondaryKey(relate = MANY_TO_ONE)
  private String sourceName;
  private String propertyValue;

  /**
   * Default constructor required by BerkeleyDb.
   */
  public BerkeleyDbSourceProperty() {
    // Required by BerkeleyDb.
  }

  /**
   * Construct a BerkeleyDbSourceProperty given the sourceName, key, and value.
   * 
   * @param sourceName The name of the source this property is associated with.
   * @param propertyKey The property key.
   * @param propertyValue The property value.
   */
  public BerkeleyDbSourceProperty(String sourceName, String propertyKey, String propertyValue) {
    this.compositeKey = new CompositeSourcePropertyKey(sourceName, propertyKey);
    this.sourceName = sourceName;
    this.propertyValue = propertyValue;
  }

  /**
   * Get the composite key (which includes the source name and property key) for this property.
   * 
   * @return The composite key for this property.
   */
  CompositeSourcePropertyKey getCompositeKey() {
    return this.compositeKey;
  }

  /**
   * Get the name of the source associated with this property.
   * 
   * @return The name of the source.
   */
  String getSourceName() {
    return this.sourceName;
  }

  /**
   * Get the value of this property.
   * 
   * @return The property value.
   */
  public String getPropertyValue() {
    return this.propertyValue;
  }

  /**
   * Get a WattDepot representation of this Property.
   * 
   * @return WattDepot representation of this property.
   */
  public Property asSourceProperty() {
    Property property = new Property();
    property.setKey(this.compositeKey.getPropertyKey());
    property.setValue(this.getPropertyValue());
    return property;
  }
}

/**
 * Represents a composite key for source properties in BerkeleyDB.
 * 
 * @author Andrea Connell
 * 
 */
@Persistent
class CompositeSourcePropertyKey {
  @KeyField(1)
  private String sourceName;
  @KeyField(2)
  private String propertyKey;

  /**
   * Default constructor required by BerkeleyDB.
   */
  CompositeSourcePropertyKey() {
    // Required by BerkeleyDB.
  }

  /**
   * Constructor for our composite key.
   * 
   * @param sourceName The name of the source.
   * @param propertyKey The key of the property.
   */
  CompositeSourcePropertyKey(String sourceName, String propertyKey) {
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
   * Get the property key associated with this compositeKey.
   * 
   * @return The property key associated with this compositeKey.
   */
  String getPropertyKey() {
    return this.propertyKey;
  }
}

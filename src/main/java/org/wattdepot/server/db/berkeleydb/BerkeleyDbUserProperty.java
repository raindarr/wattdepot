package org.wattdepot.server.db.berkeleydb;

import static com.sleepycat.persist.model.Relationship.MANY_TO_ONE;
import org.wattdepot.resource.property.jaxb.Property;
import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.KeyField;
import com.sleepycat.persist.model.Persistent;
import com.sleepycat.persist.model.PrimaryKey;
import com.sleepycat.persist.model.SecondaryKey;

/**
 * Implementation of UserProperty that is backed by BerkeleyDB.
 * 
 * @author Andrea Connell
 * 
 */

@Entity
public class BerkeleyDbUserProperty {
  @PrimaryKey
  private CompositeUserPropertyKey compositeKey;
  @SecondaryKey(relate = MANY_TO_ONE)
  private String username;
  private String propertyValue;

  /**
   * Default constructor required by BerkeleyDb.
   */
  public BerkeleyDbUserProperty() {
    // Required by BerkeleyDb.
  }

  /**
   * Construct a BerkeleyDbUserProperty given the username, key, and value.
   * 
   * @param username The username of the user this property is associated with.
   * @param propertyKey The property key.
   * @param propertyValue The property value.
   */
  public BerkeleyDbUserProperty(String username, String propertyKey, String propertyValue) {
    this.compositeKey = new CompositeUserPropertyKey(username, propertyKey);
    this.username = username;
    this.propertyValue = propertyValue;
  }

  /**
   * Get the username for the user associated with this property.
   * 
   * @return The username of the user associated with this property.
   */
  String getUsername() {
    return this.username;
  }

  /**
   * Get the composite key (which contains the user name and property key) for this property.
   * 
   * @return The composite key for this property.
   */
  CompositeUserPropertyKey getCompositeKey() {
    return this.compositeKey;
  }

  /**
   * Get the value of this property.
   * 
   * @return The value for this property.
   */
  public String getPropertyValue() {
    return this.propertyValue;
  }

  /**
   * Get a WattDepot representation of this Property.
   * 
   * @return WattDepot representation of this property.
   */
  public Property asUserProperty() {
    Property property = new Property();
    property.setKey(this.compositeKey.getPropertyKey());
    property.setValue(this.getPropertyValue());
    return property;
  }

}

/**
 * Represents a composite key for user properties in BerkeleyDB.
 * 
 * @author Andrea Connell
 * 
 */
@Persistent
class CompositeUserPropertyKey {
  @KeyField(1)
  private String username;
  @KeyField(2)
  private String propertyKey;

  /**
   * Default constructor required by BerkeleyDB.
   */
  CompositeUserPropertyKey() {
    // Required by BerkeleyDB.
  }

  /**
   * Constructor for our composite key.
   * 
   * @param username The name of the user associated with this property.
   * @param propertyKey The key of the property.
   */
  CompositeUserPropertyKey(String username, String propertyKey) {
    this.username = username;
    this.propertyKey = propertyKey;
  }

  /**
   * Get the username associated with this compositeKey.
   * 
   * @return The username associated with this compositeKey.
   */
  String getUsername() {
    return this.username;
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


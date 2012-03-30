package org.wattdepot.server.db.berkeleydb;

import org.wattdepot.util.UriUtils;
import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.KeyField;
import com.sleepycat.persist.model.Persistent;
import com.sleepycat.persist.model.PrimaryKey;
import com.sleepycat.persist.model.SecondaryKey;
import static com.sleepycat.persist.model.Relationship.MANY_TO_ONE;

/**
 * Implementation of SourceHierarchy that is backed by BerkeleyDB.
 * 
 * @author Andrea Connell
 * 
 */

@Entity
public class BerkeleyDbSourceHierarchy {

  @PrimaryKey
  private CompositeSourceHierarchyKey compositeKey;
  @SecondaryKey(relate = MANY_TO_ONE)
  private String parentSourceName;
  @SecondaryKey(relate = MANY_TO_ONE)
  private String subSourceName;

  /**
   * Default constructor as required by BerkeleyDB.
   */
  public BerkeleyDbSourceHierarchy() {
    // Required by BerkeleyDB.
  }

  /**
   * Create a BerkeleyDbSourceHierarchy instance using parameters from Source.
   * 
   * @param parentSourceName The name of the parent source for this hierarchy.
   * @param subSourceName The name of the sub-source for this hierarchy.
   */
  public BerkeleyDbSourceHierarchy(String parentSourceName, String subSourceName) {
    this.compositeKey = new CompositeSourceHierarchyKey(parentSourceName, subSourceName);
    this.parentSourceName = parentSourceName;
    this.subSourceName = UriUtils.getUriSuffix(subSourceName);
  }

  /**
   * Get the compsite key (which contains the parent source name and sub-source name) for this
   * hierarchy.
   * 
   * @return The composite key for this hierarchy.
   */
  public CompositeSourceHierarchyKey getCompositeKey() {
    return this.compositeKey;
  }

  /**
   * Get the sub-source name associated with this compositeKey.
   * 
   * @return The sub-source name associated with this compositeKey.
   */
  String getSubSourceName() {
    return this.subSourceName;
  }

  /**
   * Get the parent source name associated with this compositeKey.
   * 
   * @return The parent source name associated with this compositeKey.
   */
  String getParentSourceName() {
    return this.parentSourceName;
  }
}

/**
 * Represents a composite key for source hierarchies in BerkeleyDB.
 * 
 * @author Andrea Connell
 * 
 */
@Persistent
class CompositeSourceHierarchyKey {
  @KeyField(1)
  private String parentSourceName;
  @KeyField(2)
  private String subSourceName;

  /**
   * Default constructor required by BerkeleyDB.
   */
  CompositeSourceHierarchyKey() {
    // Required by BerkeleyDB.
  }

  /**
   * Constructor for our composite key.
   * 
   * @param parentSourceName The name of the parent source.
   * @param subSourceName The name of the sub-source.
   */
  CompositeSourceHierarchyKey(String parentSourceName, String subSourceName) {
    this.parentSourceName = parentSourceName;
    this.subSourceName = UriUtils.getUriSuffix(subSourceName);
  }

  /**
   * Get the sub-source name associated with this compositeKey.
   * 
   * @return The sub-source name associated with this compositeKey.
   */
  String getSubSourceName() {
    return this.subSourceName;
  }

  /**
   * Get the parent source name associated with this compositeKey.
   * 
   * @return The parent source name associated with this compositeKey.
   */
  String getParentSourceName() {
    return this.parentSourceName;
  }
}

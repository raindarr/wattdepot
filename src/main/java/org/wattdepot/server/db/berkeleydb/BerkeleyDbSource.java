package org.wattdepot.server.db.berkeleydb;

import static com.sleepycat.persist.model.Relationship.MANY_TO_ONE;
import javax.xml.datatype.XMLGregorianCalendar;
import org.wattdepot.resource.property.jaxb.Properties;
import org.wattdepot.resource.property.jaxb.Property;
import org.wattdepot.resource.source.jaxb.Source;
import org.wattdepot.resource.source.jaxb.SourceRef;
import org.wattdepot.resource.user.jaxb.User;
import org.wattdepot.server.Server;
import org.wattdepot.util.tstamp.Tstamp;
import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import com.sleepycat.persist.model.SecondaryKey;

/**
 * Represents a source entity for use in BerkeleyDB.
 * 
 * @author George Lee
 * 
 */
@Entity
public class BerkeleyDbSource {
  @PrimaryKey
  private String name;
  @SecondaryKey(relate = MANY_TO_ONE)
  private String owner;
  private boolean isPublic;
  private boolean isVirtual;
  private String location;
  private String coordinates;
  private String description;
  private Double carbonIntensity;
  private String fuelType;
  private Integer updateInterval;
  private String energyDirection;
  private Boolean supportsEnergyCounters;
  private Integer cacheWindowLength;
  private Integer cacheCheckpointInterval;
  private long lastMod;

  /**
   * Default constructor as required by BerkeleyDb.
   */
  public BerkeleyDbSource() {
    // Required by BerkeleyDb.
  }

  /**
   * Construct a BerkeleyDbSource given the source parameters.
   * 
   * @param name The name of the source.
   * @param owner The owner of the source.
   * @param isPublic True if the source is public, false otherwise.
   * @param isVirtual True if the source is virtual, false otherwise.
   * @param location The location of the source.
   * @param description The description of the source.
   * @param coordinates The coordinates of the source.
   * @param properties The properties of the source.
   */
  public BerkeleyDbSource(String name, String owner, boolean isPublic, boolean isVirtual,
      String location, String description, String coordinates, Properties properties) {
    this.name = name;
    this.owner = owner;
    this.isPublic = isPublic;
    this.location = location;
    this.description = description;
    this.coordinates = coordinates;
    this.isVirtual = isVirtual;
    this.lastMod = Tstamp.makeTimestamp().toGregorianCalendar().getTimeInMillis();

    if (properties != null && properties.getProperty() != null) {
      if (properties.getProperty(Source.CARBON_INTENSITY) != null) {
        this.carbonIntensity = properties.getPropertyAsDouble(Source.CARBON_INTENSITY);
      }
      if (properties.getProperty(Source.FUEL_TYPE) != null) {
        this.fuelType = properties.getProperty(Source.FUEL_TYPE);
      }
      if (properties.getProperty(Source.UPDATE_INTERVAL) != null) {
        this.updateInterval = properties.getPropertyAsInt(Source.UPDATE_INTERVAL);
      }
      if (properties.getProperty(Source.ENERGY_DIRECTION) != null) {
        this.energyDirection = properties.getProperty(Source.ENERGY_DIRECTION);
      }
      if (properties.getProperty(Source.SUPPORTS_ENERGY_COUNTERS) != null) {
        this.supportsEnergyCounters =
            Boolean.valueOf(properties.getProperty(Source.SUPPORTS_ENERGY_COUNTERS));
      }
      if (properties.getProperty(Source.CACHE_WINDOW_LENGTH) != null) {
        this.cacheWindowLength = properties.getPropertyAsInt(Source.CACHE_WINDOW_LENGTH);
      }
      if (properties.getProperty(Source.CACHE_CHECKPOINT_INTERVAL) != null) {
        this.cacheCheckpointInterval =
            properties.getPropertyAsInt(Source.CACHE_CHECKPOINT_INTERVAL);
      }
    }
  }

  /**
   * Get the last modification date of this source.
   * 
   * @return The last modification date.
   */
  public XMLGregorianCalendar getLastMod() {
    return Tstamp.makeTimestamp(this.lastMod);
  }

  /**
   * Get the name of the source.
   * 
   * @return The name of the source.
   */
  public String getName() {
    return this.name;
  }

  /**
   * Converts this BerkeleyDB source into a WattDepot source.
   * 
   * @param server The server that this source will be added to.
   * 
   * @return WattDepot representation of this source.
   */
  public Source asSource(Server server) {
    Source returnSource = new Source();
    returnSource.setName(this.name);
    returnSource.setOwner(User.userToUri(this.owner, server));
    returnSource.setPublic(this.isPublic);
    returnSource.setVirtual(this.isVirtual);
    returnSource.setLocation(this.location);
    returnSource.setDescription(this.description);
    returnSource.setCoordinates(this.coordinates);

    if (this.carbonIntensity != null) {
      returnSource.addProperty(new Property(Source.CARBON_INTENSITY, this.carbonIntensity));
    }
    if (this.fuelType != null) {
      returnSource.addProperty(new Property(Source.FUEL_TYPE, this.fuelType));
    }
    if (this.updateInterval != null) {
      returnSource.addProperty(new Property(Source.UPDATE_INTERVAL, this.updateInterval));
    }
    if (this.energyDirection != null) {
      returnSource.addProperty(new Property(Source.ENERGY_DIRECTION, this.energyDirection));
    }
    if (this.supportsEnergyCounters != null) {
      returnSource.addProperty(new Property(Source.SUPPORTS_ENERGY_COUNTERS, String
          .valueOf(this.supportsEnergyCounters)));
    }
    if (this.cacheWindowLength != null) {
      returnSource.addProperty(new Property(Source.CACHE_WINDOW_LENGTH, this.cacheWindowLength));
    }
    if (this.cacheCheckpointInterval != null) {
      returnSource.addProperty(new Property(Source.CACHE_CHECKPOINT_INTERVAL,
          this.cacheCheckpointInterval));
    }
    return returnSource;
  }

  /**
   * Returns a WattDepot SourceRef for this source.
   * 
   * @param server The current server instance. Used to calculate the Href value.
   * 
   * @return The WattDepot SourceRef for this source.
   */
  public SourceRef asSourceRef(Server server) {
    SourceRef ref = new SourceRef();
    ref.setName(this.name);
    ref.setOwner(User.userToUri(this.owner, server));
    ref.setPublic(this.isPublic);
    ref.setVirtual(this.isVirtual);
    ref.setLocation(this.location);
    ref.setDescription(this.description);
    ref.setHref(name, server);
    ref.setCoordinates(this.coordinates);
    return ref;
  }

}

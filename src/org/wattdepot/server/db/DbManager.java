package org.wattdepot.server.db;

import static org.wattdepot.resource.source.SourceUtils.makeSource;
import static org.wattdepot.resource.source.SourceUtils.makeSourceProperty;
import static org.wattdepot.resource.source.SourceUtils.sourceToUri;
import static org.wattdepot.resource.user.UserUtils.makeUser;
import static org.wattdepot.resource.user.UserUtils.userToUri;
import static org.wattdepot.server.ServerProperties.DB_IMPL_KEY;
import java.lang.reflect.Constructor;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;
import org.wattdepot.util.StackTrace;
import org.wattdepot.resource.sensordata.SensorDataStraddle;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.resource.sensordata.jaxb.SensorDataIndex;
import org.wattdepot.resource.source.jaxb.Source;
import org.wattdepot.resource.source.jaxb.SourceIndex;
import org.wattdepot.resource.source.jaxb.SubSources;
import org.wattdepot.resource.source.summary.jaxb.SourceSummary;
import org.wattdepot.resource.user.jaxb.User;
import org.wattdepot.resource.user.jaxb.UserIndex;
import org.wattdepot.server.Server;
import org.wattdepot.server.ServerProperties;

/**
 * Provides an interface to storage for the resources managed by the WattDepot server. Portions of
 * this code are adapted from http://hackystat-sensorbase-uh.googlecode.com/
 * 
 * @author Philip Johnson
 * @author Robert Brewer
 */
public class DbManager {

  /** The chosen Storage system. */
  private DbImplementation dbImpl;

  /** The server using this DbManager. */
  protected Server server;

  /** Name of the default public source for demo. */
  public static final String defaultPublicSource = "saunders-hall";

  /** Name of the default private source for demo. */
  public static final String defaultPrivateSource = "secret-place";

  /** Name of the default virtual source for demo. */
  public static final String defaultVirtualSource = "virtual-source";

  /** Username of the default user that owns both default sources for demo. */
  public static final String defaultOwnerUsername = "joebogus@example.com";

  /** Password of the default user that owns both default sources for demo. */
  public static final String defaultOwnerPassword = "totally-bogus";

  /** Username of the default user that owns no sources for demo. */
  public static final String defaultNonOwnerUsername = "jimbogus@example.com";

  /** Password of the default user that owns no sources for demo. */
  public static final String defaultNonOwnerPassword = "super-bogus";

  /**
   * Creates a new DbManager which manages access to the underlying persistency layer(s). Choice of
   * which implementation of persistency layer to use is based on the ServerProperties of the server
   * provided. Instantiates the underlying storage system for use.
   * 
   * @param server The Restlet server instance.
   */
  public DbManager(Server server) {
    this(server, server.getServerProperties().get(DB_IMPL_KEY), false);
  }

  /**
   * Creates a new DbManager which manages access to the underlying persistency layer(s), using the
   * class name provided. This is useful for forcing a particular implementation, such as in
   * testing. Instantiates the underlying storage system for use.
   * 
   * @param server The Restlet server instance.
   * @param dbClassName The name of the DbImplementation class to instantiate.
   * @param wipe If true, all stored data in the system should be discarded and reinitialized.
   */
  public DbManager(Server server, String dbClassName, boolean wipe) {
    this.server = server;
    Class<?> dbClass = null;
    // First, try to find the class specified in the properties file (or the default)
    try {
      dbClass = Class.forName(dbClassName);
    }
    catch (ClassNotFoundException e) {
      String msg = "DB error instantiating " + dbClassName + ". Could not find this class.";
      server.getLogger().warning(msg + "\n" + StackTrace.toString(e));
      throw new IllegalArgumentException(e);
    }
    // Next, try to find a constructor that accepts a Server as its parameter.
    Class<?>[] constructorParam = { org.wattdepot.server.Server.class };
    Constructor<?> dbConstructor = null;
    try {
      dbConstructor = dbClass.getConstructor(constructorParam);
    }
    catch (Exception e) {
      String msg = "DB error instantiating " + dbClassName + ". Could not find Constructor(server)";
      server.getLogger().warning(msg + "\n" + StackTrace.toString(e));
      throw new IllegalArgumentException(e);
    }
    // Next, try to create an instance of DbImplementation from the Constructor.
    Object[] serverArg = { server };
    try {
      this.dbImpl = (DbImplementation) dbConstructor.newInstance(serverArg);
    }
    catch (Exception e) {
      String msg = "DB error instantiating " + dbClassName + ". Could not create instance.";
      server.getLogger().warning(msg + "\n" + StackTrace.toString(e));
      throw new IllegalArgumentException(e);
    }
    this.dbImpl.initialize(wipe);
  }

  /**
   * Kludges up some default data so that SensorData can be stored. This is a total hack, and should
   * be removed as soon as the all the resources have been fully implemented.
   * 
   * @return True if the default data could be created, or false otherwise.
   */
  public boolean createDefaultData() {
    // Always want there to be an admin user
    ServerProperties serverProps =
        (ServerProperties) server.getContext().getAttributes().get("ServerProperties");
    String adminUsername = serverProps.get(ServerProperties.ADMIN_EMAIL_KEY);
    String adminPassword = serverProps.get(ServerProperties.ADMIN_PASSWORD_KEY);
    // create the admin User object based on the server properties
    User adminUser = makeUser(adminUsername, adminPassword, true, null);
    // stick admin user into database
    if (!this.storeUser(adminUser)) {
//      server.getLogger().severe("Unable to create admin user from properties!");
      return false;
    }
    // create a non-admin user that owns a source for testing
    User ownerUser = makeUser(defaultOwnerUsername, defaultOwnerPassword, false, null);
    if (!this.storeUser(ownerUser)) {
      return false;
    }
    // create a non-admin user that owns nothing for testing
    User nonOwnerUser = makeUser(defaultNonOwnerUsername, defaultNonOwnerPassword, false, null);
    if (!this.storeUser(nonOwnerUser)) {
      return false;
    }

    org.wattdepot.resource.source.jaxb.Properties props =
        new org.wattdepot.resource.source.jaxb.Properties();
    props.getProperty().add(makeSourceProperty("carbonIntensity", "294"));
    // create public source
    Source source1 =
        makeSource(defaultPublicSource, userToUri(ownerUser, this.server), true, false,
            "21.30078,-157.819129,41", "Saunders Hall on the University of Hawaii at Manoa campus",
            "Obvius-brand power meter", props, null);
    // stick public source into database
    if (!this.storeSource(source1)) {
      return false;
    }

    props.getProperty().clear();
    props.getProperty().add(makeSourceProperty("carbonIntensity", "128"));
    Source source2 =
        makeSource(defaultPrivateSource, userToUri(ownerUser, this.server), false, false,
            "21.35078,-157.819129,41", "Made up private place", "Foo-brand power meter", props,
            null);
    // stick public source into database
    if (!this.storeSource(source2)) {
      return false;
    }

    SubSources subSources = new SubSources();
    subSources.getHref().add(sourceToUri(source1, server));
    subSources.getHref().add(sourceToUri(source2, server));

    Source virtualSource =
        makeSource(defaultVirtualSource, userToUri(ownerUser, server), true,
            true, "31.30078,-157.819129,41", "Made up location 3", "Virtual source", null,
            subSources);
    return (this.storeSource(virtualSource));
  }

  /**
   * Returns a list of all Sources in the system. An empty index will be returned if there are no
   * Sources in the system. The list is sorted by source name.
   * 
   * @return a SourceIndex object containing a List of SourceRefs to all Source objects.
   */
  public SourceIndex getSources() {
    return this.dbImpl.getSources();
  }

  /**
   * Returns the named Source instance, or null if not found.
   * 
   * @param sourceName The name of the Source.
   * @return The requested Source, or null.
   */
  public Source getSource(String sourceName) {
    return this.dbImpl.getSource(sourceName);
  }

  /**
   * Returns a SourceSummary for the named Source instance, or null if not found.
   * 
   * @param sourceName The name of the Source.
   * @return The requested SourceSummary, or null.
   */
  public SourceSummary getSourceSummary(String sourceName) {
    return this.dbImpl.getSourceSummary(sourceName);
  }

  /**
   * Persists a Source instance. If a Source with this name already exists in the storage system, no
   * action is performed and the method returns false.
   * 
   * @param source The Source to store.
   * @return True if the user was successfully stored.
   */
  public boolean storeSource(Source source) {
    return this.dbImpl.storeSource(source);
  }

  /**
   * Ensures that the Source with the given name is no longer present in storage. All sensor data
   * associated with this Source will also be deleted.
   * 
   * @param sourceName The name of the Source.
   * @return True if the Source was deleted, or false if it was not deleted or the requested Source
   * does not exist.
   */
  public boolean deleteSource(String sourceName) {
    return this.dbImpl.deleteSource(sourceName);
  }

  /**
   * Returns the SensorDataIndex listing all sensor data for the named Source. If the Source has no
   * SensorData resources, the index will be empty (not null). The list will be sorted in order of
   * increasing timestamp values.
   * 
   * @param sourceName The name of the Source whose sensor data is to be returned.
   * @return a SensorDataIndex object containing all relevant sensor data resources, or null if
   * sourceName is invalid.
   */
  public SensorDataIndex getSensorDataIndex(String sourceName) {
    return this.dbImpl.getSensorDataIndex(sourceName);
  }

  /**
   * Returns the SensorDataIndex representing all the SensorData resources for the named Source such
   * that their timestamp is greater than or equal to the given start time and less than or equal to
   * the given end time. If the Source has no appropriate SensorData resources, the index will be
   * empty (not null). The list will be sorted in order of increasing timestamp values.
   * 
   * @param sourceName The name of the Source whose sensor data is to be returned.
   * @param startTime The earliest Sensor Data to be returned.
   * @param endTime The latest SensorData to be returned.
   * @throws DbBadIntervalException if startTime is later than endTime.
   * @return a SensorDataIndex object containing all relevant sensor data resources, or null if
   * sourceName, startTime, or endTime are invalid.
   */
  public SensorDataIndex getSensorDataIndex(String sourceName, XMLGregorianCalendar startTime,
      XMLGregorianCalendar endTime) throws DbBadIntervalException {
    return this.dbImpl.getSensorDataIndex(sourceName, startTime, endTime);
  }

  /**
   * Returns the SensorData instance for a particular named Source and timestamp, or null if not
   * found.
   * 
   * @param sourceName The name of the Source whose sensor data is to be returned.
   * @param timestamp The timestamp associated with this sensor data.
   * @return The SensorData resource, or null.
   */
  public SensorData getSensorData(String sourceName, XMLGregorianCalendar timestamp) {
    return this.dbImpl.getSensorData(sourceName, timestamp);
  }

  /**
   * Returns true if the passed [Source name, timestamp] has sensor data defined for it.
   * 
   * @param sourceName The name of the Source whose sensor data is to be returned.
   * @param timestamp The timestamp
   * @return True if there is any sensor data for this timestamp.
   */
  public boolean hasSensorData(String sourceName, XMLGregorianCalendar timestamp) {
    return this.dbImpl.hasSensorData(sourceName, timestamp);
  }

  /**
   * Persists a SensorData instance. If SensorData with this [Source, timestamp] already exists in
   * the storage system, no action is performed and the method returns false.
   * 
   * @param data The sensor data.
   * @return True if the sensor data was successfully stored.
   */
  public boolean storeSensorData(SensorData data) {
    return this.dbImpl.storeSensorData(data);
  }

  /**
   * Ensures that sensor data with the named Source and timestamp is no longer present in this
   * manager.
   * 
   * @param sourceName The name of the Source whose sensor data is to be deleted.
   * @param timestamp The timestamp associated with this sensor data.
   * @return True if the sensor data was deleted, or false if it was not deleted or the requested
   * sensor data or Source does not exist.
   */
  public boolean deleteSensorData(String sourceName, XMLGregorianCalendar timestamp) {
    return this.dbImpl.deleteSensorData(sourceName, timestamp);
  }

  /**
   * Ensures that all sensor data from the named Source is no longer present in storage.
   * 
   * @param sourceName The name of the Source whose sensor data is to be deleted.
   * @return True if all the sensor data was deleted, or false if it was not deleted or the
   * requested Source does not exist.
   */
  public boolean deleteSensorData(String sourceName) {
    return this.dbImpl.deleteSensorData(sourceName);
  }

  /**
   * Returns a SensorDataStraddle that straddles the given timestamp, using SensorData from the
   * given source. Note that a virtual source contains no SensorData directly, so this method will
   * always return null if the given sourceName is a virtual source. To obtain a list of
   * SensorDataStraddles for all the non-virtual subsources of a virtual source, see
   * getSensorDataStraddleList.
   * 
   * If the given timestamp corresponds to an actual SensorData, then return a degenerate
   * SensorDataStraddle with both ends of the straddle set to the actual SensorData.
   * 
   * @param sourceName The name of the source to generate the straddle from.
   * @param timestamp The timestamp of interest in the straddle.
   * @return A SensorDataStraddle that straddles the given timestamp. Returns null if: parameters
   * are null, the source doesn't exist, source has no sensor data, or there is no sensor data that
   * straddles the timestamp.
   * @see org.wattdepot.server.db.memory#getSensorDataStraddleList
   */
  public SensorDataStraddle getSensorDataStraddle(String sourceName,
      XMLGregorianCalendar timestamp) {
    return this.dbImpl.getSensorDataStraddle(sourceName, timestamp);
  }

  /**
   * Returns a list of SensorDataStraddles that straddle the given timestamp, using SensorData from
   * all non-virtual subsources of the given source. If the given source is non-virtual, then the
   * result will be a list containing at a single SensorDataStraddle, or null. In the case of a
   * non-virtual source, you might as well use getSensorDataStraddle.
   * 
   * @param sourceName The name of the source to generate the straddle from.
   * @param timestamp The timestamp of interest in the straddle.
   * @return A list of SensorDataStraddles that straddle the given timestamp. Returns null if:
   * parameters are null, the source doesn't exist, or there is no sensor data that straddles the
   * timestamp.
   * @see org.wattdepot.server.db.memory#getSensorDataStraddle
   */
  public List<SensorDataStraddle> getSensorDataStraddleList(String sourceName,
      XMLGregorianCalendar timestamp) {
    return this.dbImpl.getSensorDataStraddleList(sourceName, timestamp);
  }
  
  /**
   * Returns a UserIndex of all Users in the system.
   * 
   * @return a UserIndex object containing a List of UserRef objects for all User resources.
   */
  public UserIndex getUsers() {
    return this.dbImpl.getUsers();
  }

  /**
   * Returns the User instance for a particular username, or null if not found.
   * 
   * @param username The user's username.
   * @return The requested User object, or null.
   */
  public User getUser(String username) {
    return this.dbImpl.getUser(username);
  }

  /**
   * Persists a User instance. If a User with this name already exists in the storage system, no
   * action is performed and the method returns false.
   * 
   * @param user The user to be stored.
   * @return True if the user was successfully stored.
   */
  public boolean storeUser(User user) {
    return this.dbImpl.storeUser(user);
  }

  /**
   * Ensures that the User with the given username is no longer present in storage. All Sources
   * owned by the given User and their associated Sensor Data will be deleted as well.
   * 
   * @param username The user's username.
   * @return True if the User was deleted, or false if it was not deleted or the requested User does
   * not exist.
   */
  public boolean deleteUser(String username) {
    return this.dbImpl.deleteUser(username);
  }

  /**
   * Some databases require periodic maintenance (ex. Derby requires an explicit compress command to
   * release disk space after a large number of rows have been deleted). This operation instructs
   * the underlying database to perform this maintenance. If a database implementation does not
   * support maintenance, then this command should do nothing but return true.
   * 
   * @return True if the maintenance succeeded or if the database does not support maintenance.
   */
  public boolean performMaintenance() {
    return this.dbImpl.performMaintenance();
  }

  /**
   * The most appropriate set of indexes for the database has been evolving over time as we develop
   * new queries. This command sets up the appropriate set of indexes. It should be able to be
   * called repeatedly without error.
   * 
   * @return True if the index commands succeeded.
   */
  public boolean indexTables() {
    return this.dbImpl.indexTables();
  }
}

package org.wattdepot.server.db;

import static org.wattdepot.server.ServerProperties.DB_IMPL_KEY;
import java.lang.reflect.Constructor;
import javax.xml.datatype.XMLGregorianCalendar;
import org.hackystat.utilities.stacktrace.StackTrace;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.resource.sensordata.jaxb.SensorDataIndex;
import org.wattdepot.resource.source.jaxb.Source;
import org.wattdepot.resource.source.jaxb.SourceIndex;
import org.wattdepot.resource.user.jaxb.User;
import org.wattdepot.resource.user.jaxb.UserIndex;
import org.wattdepot.server.Server;

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

  /**
   * Creates a new DbManager which manages access to the underlying persistency layer(s).
   * Instantiates the underlying storage system for use.
   * 
   * @param server The Restlet server instance.
   */
  public DbManager(Server server) {
    String dbClassName = server.getServerProperties().get(DB_IMPL_KEY);
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
      String msg = "DB error instantiating " + dbClassName
          + ". Could not find Constructor(server)";
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
    this.dbImpl.initialize();
  }

  /**
   * Returns a list of all Sources in the system.
   * 
   * @return a SourceIndex object containing a List of SourceRefs to all Source objects.
   */
  public SourceIndex getSources() {
    return this.dbImpl.getSourceIndex();
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
   * Ensures that the Source with the given name is no longer present in storage.
   * 
   * @param sourceName The name of the Source.
   * @return True if the Source was deleted, or false if it was not deleted or the requested Source
   * does not exist.
   */
  public boolean deleteSource(String sourceName) {
    return this.dbImpl.deleteSource(sourceName);
  }

  /**
   * Returns the SensorDataIndex listing all sensor data for the named Source.
   * 
   * @param sourceName The name of the Source whose sensor data is to be returned.
   * @return a SensorDataIndex object containing all relevant sensor data resources.
   */
  public SensorDataIndex getSensorDataIndex(String sourceName) {
    return this.dbImpl.getSensorDataIndex(sourceName);
  }

  /**
   * Returns the SensorDataIndex representing all the SensorData resources for the named Source
   * between the given start and end times.
   * 
   * @param sourceName The name of the Source whose sensor data is to be returned.
   * @param startTime The earliest Sensor Data to be returned.
   * @param endTime The latest SensorData to be returned.
   * @return a SensorDataIndex object containing all relevant sensor data resources.
   */
  public SensorDataIndex getSensorDataIndex(String sourceName, XMLGregorianCalendar startTime,
      XMLGregorianCalendar endTime) {
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
   * sensor data does not exist.
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
   * Ensures that the User with the given username is no longer present in storage.
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

package org.wattdepot.server.db;

import java.util.logging.Logger;
import javax.xml.datatype.XMLGregorianCalendar;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.resource.sensordata.jaxb.SensorDatas;
import org.wattdepot.resource.source.jaxb.Source;
import org.wattdepot.resource.source.jaxb.Sources;
import org.wattdepot.resource.user.jaxb.User;
import org.wattdepot.resource.user.jaxb.Users;
import org.wattdepot.server.Server;

/**
 * Provides a specification of the operations that must be implemented by every WattDepot server
 * storage system. Portions of this code are adapted from
 * http://hackystat-sensorbase-uh.googlecode.com/
 * 
 * @author Philip Johnson
 * @author Robert Brewer
 */

public abstract class DbImplementation {

  /**
   * To be called as part of the startup process for a storage system. This method should:
   * <ul>
   * <li>Check to see if this storage system has already been created during a previous session.
   * <li>If no storage system exists, it should create one and initialize it appropriately.
   * </ul>
   */
  public abstract void initialize();

  /**
   * Returns true if the initialize() method did indeed create a fresh storage system.
   * 
   * @return True if the storage system is freshly created.
   */
  public abstract boolean isFreshlyCreated();

  /**
   * Returns a list of all Sources in the system.
   * 
   * @return a Sources object containing a List of all Source objects.
   */
  public abstract Sources getSources();

  /**
   * Returns the named Source instance, or null if not found.
   * 
   * @param sourceName The name of the Source.
   * @return The requested Source, or null.
   */
  public abstract Source getSource(String sourceName);

  /**
   * Persists a Source instance. If a Source with this name already exists in the storage system, no
   * action is performed and the method returns false.
   * 
   * @param source The Source to store.
   * @return True if the user was successfully stored.
   */
  public abstract boolean storeSource(Source source);

  /**
   * Ensures that the Source with the given name is no longer present in storage.
   * 
   * @param sourceName The name of the Source.
   * @return True if the Source was deleted, or false if it was not deleted or the requested Source
   * does not exist.
   */
  public abstract boolean deleteSource(String sourceName);

  /**
   * Returns the SensorDatas including all sensor data for this Source.
   * 
   * @param source The Source whose sensor data is to be returned.
   * @return a SensorDatas object containing a List of all relevant sensor data resources.
   */
  public abstract SensorDatas getSensorDatas(Source source);

  /**
   * Returns the SensorDatas representing the SensorData for the given Source between the given
   * start and end times.
   * 
   * @param source The Source whose sensor data is to be returned.
   * @param startTime The earliest Sensor Data to be returned.
   * @param endTime The latest SensorData to be returned.
   * @return a SensorDatas object containing a List of all relevant sensor data resources.
   */
  public abstract SensorDatas getSensorDatas(Source source, XMLGregorianCalendar startTime,
      XMLGregorianCalendar endTime);

  /**
   * Returns the SensorData instance for a particular Source and timestamp, or null if not found.
   * 
   * @param source The Source whose sensor data is to be returned.
   * @param timestamp The timestamp associated with this sensor data.
   * @return The SensorData resource, or null.
   */
  public abstract SensorData getSensorData(Source source, XMLGregorianCalendar timestamp);

  /**
   * Returns true if the passed [Source, timestamp] has sensor data defined for it.
   * 
   * @param source The Source being examined.
   * @param timestamp The timestamp
   * @return True if there is any sensor data for this [key, sdtName, timestamp].
   */
  public abstract boolean hasSensorData(Source source, XMLGregorianCalendar timestamp);

  /**
   * Persists a SensorData instance. If SensorData with this [Source, timestamp] already exists in
   * the storage system, no action is performed and the method returns false.
   * 
   * @param data The sensor data.
   * @return True if the sensor data was successfully stored.
   */
  public abstract boolean storeSensorData(SensorData data);

  /**
   * Ensures that sensor data with the given Source and timestamp is no longer present in this
   * manager.
   * 
   * @param source The Source being deleted.
   * @param timestamp The timestamp associated with this sensor data.
   * @return True if the sensor data was deleted, or false if it was not deleted or the requested
   * sensor data does not exist.
   */
  public abstract boolean deleteSensorData(Source source, XMLGregorianCalendar timestamp);

  /**
   * Ensures that all sensor data from the given Source is no longer present in storage.
   * 
   * @param source The Source being deleted.
   * @return True if all the sensor data was deleted, or false if it was not deleted or the
   * requested Source does not exist.
   */
  public abstract boolean deleteSensorData(Source source);

  /**
   * Returns a list of all Users in the system.
   * 
   * @return a Users object containing a List of all User objects.
   */
  public abstract Users getUsers();

  /**
   * Returns the User instance for a particular username, or null if not found.
   * 
   * @param username The user's username.
   * @return The requested User object, or null.
   */
  public abstract User getUser(String username);

  /**
   * Persists a User instance. If a User with this name already exists in the storage system, no
   * action is performed and the method returns false.
   * 
   * @param user The user to be stored.
   * @return True if the user was successfully stored.
   */
  public abstract boolean storeUser(User user);

  /**
   * Ensures that the User with the given username is no longer present in storage.
   * 
   * @param username The user's username.
   * @return True if the User was deleted, or false if it was not deleted or the requested User does
   * not exist.
   */
  public abstract boolean deleteUser(String username);

  /** Keeps a pointer to this Server for use in accessing the managers. */
  protected Server server;

  /** Keep a pointer to the Logger. */
  protected Logger logger;

  /**
   * Constructs a new DbImplementation.
   * 
   * @param server The server.
   */
  public DbImplementation(Server server) {
    this.server = server;
    this.logger = server.getLogger();
  }

  /**
   * Some databases require periodic maintenance (ex. Derby requires an explicit compress command to
   * release disk space after a large number of rows have been deleted). This operation instructs
   * the underlying database to perform this maintenance. If a database implementation does not
   * support maintenance, then this command should do nothing but return true.
   * 
   * @return True if the maintenance succeeded or if the database does not support maintenance.
   */
  public abstract boolean performMaintenance();

  /**
   * The most appropriate set of indexes for the database has been evolving over time as we develop
   * new queries. This command sets up the appropriate set of indexes. It should be able to be
   * called repeatedly without error.
   * 
   * @return True if the index commands succeeded.
   */
  public abstract boolean indexTables();

  // /**
  // * Returns the current number of rows in the specified table.
  // *
  // * @param table The table whose rows are to be counted.
  // * @return The number of rows in the table, or -1 if the table does not exist or an error
  // occurs.
  // */
  // public abstract int getRowCount(String table);

  // /**
  // * Returns a set containing the names of all tables in this database. Used by clients to invoke
  // * getRowCount with a legal table name.
  // *
  // * @return A set of table names.
  // */
  // public abstract Set<String> getTableNames();

}

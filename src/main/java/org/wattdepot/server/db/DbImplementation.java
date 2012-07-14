package org.wattdepot.server.db;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.xml.datatype.XMLGregorianCalendar;
import org.wattdepot.resource.carbon.Carbon;
import org.wattdepot.resource.energy.Energy;
import org.wattdepot.resource.energy.EnergyCounterException;
import org.wattdepot.resource.sensordata.SensorDataStraddle;
import org.wattdepot.resource.sensordata.StraddleList;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.resource.sensordata.jaxb.SensorDataIndex;
import org.wattdepot.resource.sensordata.jaxb.SensorDatas;
import org.wattdepot.resource.source.jaxb.Source;
import org.wattdepot.resource.source.jaxb.SourceIndex;
import org.wattdepot.resource.source.jaxb.Sources;
import org.wattdepot.resource.source.summary.jaxb.SourceSummary;
import org.wattdepot.resource.user.jaxb.User;
import org.wattdepot.resource.user.jaxb.UserIndex;
import org.wattdepot.server.Server;
import org.wattdepot.util.UriUtils;
import org.wattdepot.util.tstamp.Tstamp;

/**
 * Provides a specification of the operations that must be implemented by every WattDepot server
 * storage system. Portions of this code are adapted from
 * http://hackystat-sensorbase-uh.googlecode.com/
 * 
 * @author Philip Johnson
 * @author Robert Brewer
 */

public abstract class DbImplementation {

  /** Keeps a pointer to this Server for use in accessing the managers. */
  protected Server server;

  /** Keeps a pointer to the DbManager for this server. */
  protected DbManager dbManager;

  /** Keep a pointer to the Logger. */
  protected Logger logger;

  /**
   * Constructs a new DbImplementation.
   * 
   * @param server The server.
   * @param dbManager The dbManager.
   */
  public DbImplementation(Server server, DbManager dbManager) {
    this.server = server;
    this.logger = server.getLogger();

    this.dbManager = dbManager;
  }

  /**
   * To be called as part of the startup process for a storage system. This method should:
   * <ul>
   * <li>Check to see if this storage system has already been created during a previous session.
   * <li>If no storage system exists, it should create one and initialize it appropriately.
   * </ul>
   * 
   * @param wipe If true, all stored data in the system should be discarded and reinitialized.
   */
  public abstract void initialize(boolean wipe);

  /**
   * Returns true if the initialize() method did indeed create a fresh storage system.
   * 
   * @return True if the storage system is freshly created.
   */
  public abstract boolean isFreshlyCreated();

  /**
   * Wipes all data from the underlying storage implementation. Obviously, this is serious
   * operation. Exposed so that tests can ensure that the database is clean before testing.
   * 
   * @return True if data could be wiped, or false if there was a problem wiping data.
   */
  public abstract boolean wipeData();

  // Start of methods based on REST API

  /**
   * Returns a list of all Sources in the system. An empty index will be returned if there are no
   * Sources in the system. The list is sorted by source name.
   * 
   * @return a SourceIndex object containing a List of SourceRefs to all Source objects.
   */
  public abstract SourceIndex getSourceIndex();

  /**
   * Returns a list of all Sources in the system as a Sources element. An empty Sources element will
   * be returned if there are no Sources in the system. The list is sorted by source name.
   * 
   * @return a Sources object containing Source objects.
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
   * Returns a SourceSummary for the named Source instance, or null if not found.
   * 
   * @param sourceName The name of the Source.
   * @return The requested SourceSummary, or null.
   */
  public abstract SourceSummary getSourceSummary(String sourceName);

  /**
   * Persists a Source instance. If a Source with this name already exists in the storage system, no
   * action is performed and the method returns false. If you wish to overwrite the resource, see
   * the two argument version of this method.
   * 
   * @param source The Source to store.
   * @return True if the user was successfully stored.
   */
  public boolean storeSource(Source source) {
    return storeSource(source, false);
  }

  /**
   * Persists a Source instance. If a Source with this name already exists in the storage system, no
   * action is performed and the method returns false, unless the overwrite parameter is true, in
   * which case the existing resource is overwritten.
   * 
   * @param source The Source to store.
   * @param overwrite False in the normal case, set to true if you wish to overwrite the resource.
   * @return True if the user was successfully stored.
   */
  public abstract boolean storeSource(Source source, boolean overwrite);

  /**
   * Ensures that the Source with the given name is no longer present in storage. All sensor data
   * associated with this Source will also be deleted.
   * 
   * @param sourceName The name of the Source.
   * @return True if the Source was deleted, or false if it was not deleted or the requested Source
   * does not exist.
   */
  public abstract boolean deleteSource(String sourceName);

  /**
   * Returns the SensorDataIndex listing all sensor data for the named Source. If the Source has no
   * SensorData resources, the index will be empty (not null). The list will be sorted in order of
   * increasing timestamp values.
   * 
   * @param sourceName The name of the Source whose sensor data is to be returned.
   * @return a SensorDataIndex object containing all relevant sensor data resources, or null if
   * sourceName is invalid.
   */
  public abstract SensorDataIndex getSensorDataIndex(String sourceName);

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
  public abstract SensorDataIndex getSensorDataIndex(String sourceName,
      XMLGregorianCalendar startTime, XMLGregorianCalendar endTime) throws DbBadIntervalException;

  /**
   * Returns a list of all SensorData resources representing all the SensorData resources for the
   * named Source such that their timestamp is greater than or equal to the given start time and
   * less than or equal to the given end time. If the Source has no appropriate SensorData
   * resources, the SensorDatas will be empty (not null). The list will be sorted in order of
   * increasing timestamp values.
   * 
   * @param sourceName The name of the Source whose sensor data is to be returned.
   * @param startTime The earliest Sensor Data to be returned.
   * @param endTime The latest SensorData to be returned.
   * @throws DbBadIntervalException if startTime is later than endTime.
   * @return a SensorDatas object containing all relevant sensor data resources, or null if
   * sourceName, startTime, or endTime are invalid.
   */
  public abstract SensorDatas getSensorDatas(String sourceName, XMLGregorianCalendar startTime,
      XMLGregorianCalendar endTime) throws DbBadIntervalException;

  /**
   * Returns the SensorData instance for a particular named Source and timestamp, or null if not
   * found.
   * 
   * @param sourceName The name of the Source whose sensor data is to be returned.
   * @param timestamp The timestamp associated with this sensor data.
   * @return The SensorData resource, or null.
   */
  public abstract SensorData getSensorData(String sourceName, XMLGregorianCalendar timestamp);

  /**
   * Returns the latest SensorData instance for a the named non-virtual Source, or null if not
   * found. If a virtual source name is provided, null is returned.
   * 
   * @param sourceName The name of the Source whose sensor data is to be returned.
   * @return The SensorData resource, or null.
   */
  protected abstract SensorData getLatestNonVirtualSensorData(String sourceName);

  /**
   * Returns true if the passed [Source name, timestamp] has sensor data defined for it.
   * 
   * @param sourceName The name of the Source whose sensor data is to be returned.
   * @param timestamp The timestamp
   * @return True if there is any sensor data for this timestamp.
   */
  public abstract boolean hasSensorData(String sourceName, XMLGregorianCalendar timestamp);

  /**
   * Persists a SensorData instance. If SensorData with this [Source, timestamp] already exists in
   * the storage system, no action is performed and the method returns false.
   * 
   * @param data The sensor data.
   * @return True if the sensor data was successfully stored.
   */
  public abstract boolean storeSensorData(SensorData data);

  /**
   * Ensures that sensor data with the named Source and timestamp is no longer present in this
   * manager.
   * 
   * @param sourceName The name of the Source whose sensor data is to be deleted.
   * @param timestamp The timestamp associated with this sensor data.
   * @return True if the sensor data was deleted, or false if it was not deleted or the requested
   * sensor data or Source does not exist.
   */
  public abstract boolean deleteSensorData(String sourceName, XMLGregorianCalendar timestamp);

  /**
   * Ensures that all sensor data from the named Source is no longer present in storage.
   * 
   * @param sourceName The name of the Source whose sensor data is to be deleted.
   * @return True if all the sensor data was deleted, or false if it was not deleted or the
   * requested Source does not exist.
   */
  public abstract boolean deleteSensorData(String sourceName);

  /**
   * Returns a UserIndex of all Users in the system. The list is sorted by username.
   * 
   * @return a UserIndex object containing a List of UserRef objects for all User resources.
   */
  public abstract UserIndex getUsers();

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
   * Ensures that the User with the given username is no longer present in storage. All Sources
   * owned by the given User and their associated Sensor Data will be deleted as well.
   * 
   * @param username The user's username.
   * @return True if the User was deleted, or false if it was not deleted or the requested User does
   * not exist.
   */
  public abstract boolean deleteUser(String username);

  // End of methods based on REST API

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
   * @param source The source object to generate the straddle from.
   * @param timestamp The timestamp of interest in the straddle.
   * @return A SensorDataStraddle that straddles the given timestamp. Returns null if: parameters
   * are null, the source doesn't exist, source has no sensor data, or there is no sensor data that
   * straddles the timestamp.
   * @see org.wattdepot.server.db.memory#getSensorDataStraddleList
   */
  public abstract SensorDataStraddle getSensorDataStraddle(Source source,
      XMLGregorianCalendar timestamp);

  /**
   * Returns a list of SensorDataStraddles that straddle the given timestamp, using SensorData from
   * all non-virtual subsources of the given source. If the given source is non-virtual, then the
   * result will be a list containing a single SensorDataStraddle, or null. In the case of a
   * non-virtual source, you might as well use getSensorDataStraddle.
   * 
   * @param source The source object to generate the straddle from.
   * @param timestamp The timestamp of interest in the straddle.
   * @return A list of SensorDataStraddles that straddle the given timestamp. Returns null if:
   * parameters are null, the source doesn't exist, or there is no sensor data that straddles the
   * timestamp.
   * @see org.wattdepot.server.db.memory#getSensorDataStraddle
   */
  public List<SensorDataStraddle> getSensorDataStraddleList(Source source,
      XMLGregorianCalendar timestamp) {
    if ((source == null) || (timestamp == null)) {
      return null;
    }

    // Want to go through sensordata for base source, and all subsources recursively
    List<Source> sourceList = getAllNonVirtualSubSources(source);
    List<SensorDataStraddle> straddleList = new ArrayList<SensorDataStraddle>(sourceList.size());
    for (Source subSource : sourceList) {
      SensorDataStraddle straddle = this.dbManager.getSensorDataStraddle(subSource, timestamp);
      if (straddle == null) {
        // No straddle for this timestamp on this source, abort
        return null;
      }
      else {
        straddleList.add(straddle);
      }
    }
    if (straddleList.isEmpty()) {
      return null;
    }
    else {
      return straddleList;
    }
  }

  /**
   * Returns a list of StraddleLists each of which corresponds to the straddles from source (or
   * subsources of the source) for the given list of timestamps. If the given source is non-virtual,
   * then the result will be a list containing at a single StraddleList, or null. In the case of a
   * virtual source, the result is a list of StraddleLists, one for each non-virtual subsource
   * (determined recursively).
   * 
   * @param source The source object to generate the straddle from.
   * @param timestampList The list of timestamps of interest in each straddle.
   * @return A list of StraddleLists. Returns null if: parameters are null, the source doesn't
   * exist, or there is no sensor data that straddles any of the timestamps.
   * @see org.wattdepot.server.db.memory#getSensorDataStraddle
   */
  public List<StraddleList> getStraddleLists(Source source, List<XMLGregorianCalendar> timestampList) {
    if ((source == null) || (timestampList == null)) {
      return null;
    }

    // Want to go through sensordata for base source, and all subsources recursively
    List<Source> sourceList = getAllNonVirtualSubSources(source);
    List<StraddleList> masterList = new ArrayList<StraddleList>(sourceList.size());
    List<SensorDataStraddle> straddleList;
    for (Source subSource : sourceList) {
      straddleList = new ArrayList<SensorDataStraddle>(timestampList.size());
      for (XMLGregorianCalendar timestamp : timestampList) {
        SensorDataStraddle straddle = this.dbManager.getSensorDataStraddle(subSource, timestamp);
        if (straddle == null) {
          // No straddle for this timestamp on this source, abort
          return null;
        }
        else {
          straddleList.add(straddle);
        }
      }
      if (straddleList.isEmpty()) {
        return null;
      }
      else {
        masterList.add(new StraddleList(subSource, straddleList));
      }
    }
    return masterList;
  }

  /**
   * Given a virtual source and a List of timestamps, returns a List (one member for each
   * non-virtual subsource) that contains Lists of SensorDataStraddles that straddle each of the
   * given timestamps. If the given source is non-virtual, then the result will be a list containing
   * a single List of SensorDataStraddles, or null.
   * 
   * @param source The source object to generate the straddle from.
   * @param timestampList The list of timestamps of interest in each straddle.
   * @return A list of lists of SensorDataStraddles that straddle the given timestamp. Returns null
   * if: parameters are null, the source doesn't exist, or there is no sensor data that straddles
   * any of the timestamps.
   * @see org.wattdepot.server.db.memory#getSensorDataStraddle getSensorDataStraddle
   */
  public List<List<SensorDataStraddle>> getSensorDataStraddleListOfLists(Source source,
      List<XMLGregorianCalendar> timestampList) {
    List<List<SensorDataStraddle>> masterList = new ArrayList<List<SensorDataStraddle>>();
    if ((source == null) || (timestampList == null)) {
      return null;
    }

    // Want to go through sensordata for base source, and all subsources recursively
    List<Source> sourceList = getAllNonVirtualSubSources(source);
    for (Source subSource : sourceList) {
      List<SensorDataStraddle> straddleList = new ArrayList<SensorDataStraddle>();
      for (XMLGregorianCalendar timestamp : timestampList) {
        SensorDataStraddle straddle = this.dbManager.getSensorDataStraddle(subSource, timestamp);
        if (straddle == null) {
          // No straddle for this timestamp on this source, abort
          return null;
        }
        else {
          straddleList.add(straddle);
        }
      }
      masterList.add(straddleList);
    }
    if (masterList.isEmpty()) {
      return null;
    }
    else {
      return masterList;
    }
  }

  /**
   * Returns the power in SensorData format for the given Source and timestamp, or null if no power
   * data exists.
   * 
   * @param source The source object.
   * @param timestamp The timestamp requested.
   * @return The requested power in SensorData format, or null if it cannot be found/calculated.
   */
  public SensorData getPower(Source source, XMLGregorianCalendar timestamp) {
    if (source.isVirtual()) {
      List<SensorDataStraddle> straddleList = getSensorDataStraddleList(source, timestamp);
      return SensorDataStraddle.getPowerFromList(straddleList,
          Source.sourceToUri(source.getName(), this.server));
    }
    else {
      SensorDataStraddle straddle = this.dbManager.getSensorDataStraddle(source, timestamp);
      if (straddle == null) {
        return null;
      }
      else {
        return straddle.getPower();
      }
    }
  }

  /**
   * Returns the energy in SensorData format for the given non virtual Source over the range of time
   * between startTime and endTime, or null if no energy data exists.
   * 
   * @param source The source object.
   * @param startTime The start of the range requested.
   * @param endTime The end of the range requested.
   * @param interval The sampling interval requested in minutes (ignored if all sources support
   * energy counters).
   * @return The requested energy in SensorData format, or null if it cannot be found/calculated.
   */
  protected SensorData getNonVirtualEnergy(Source source, XMLGregorianCalendar startTime,
      XMLGregorianCalendar endTime, int interval) {
    if (endTime == null) {
      endTime = getLatestNonVirtualSensorData(source.getName()).getTimestamp();

      long minutesToMilliseconds = 60L * 1000L;
      if ((interval * minutesToMilliseconds) > Tstamp.diff(startTime, endTime)) {
        return null;
      }
    }

    if (source.isPropertyTrue(Source.SUPPORTS_ENERGY_COUNTERS)) {
      List<Energy> energyList = new ArrayList<Energy>();
      // calculate energy using counters
      List<XMLGregorianCalendar> timestampList = new ArrayList<XMLGregorianCalendar>(2);
      timestampList.add(startTime);

      timestampList.add(endTime);
      List<StraddleList> sourceList = getStraddleLists(source, timestampList);
      if ((sourceList == null) || (sourceList.isEmpty())) {
        return null;
      }
      // straddleList should contain a list (one for each non-virtual subsource) of StraddleLists
      // each of length 2
      for (StraddleList straddlePair : sourceList) {
        if (straddlePair.getStraddleList().size() == 2) {
          energyList.add(new Energy(straddlePair.getStraddleList().get(0), straddlePair
              .getStraddleList().get(1), true));
        }
        else {
          // One of the subsources did not have matching SensorData, so just abort
          return null;
        }
      }
      try {
        return Energy.getEnergyFromList(energyList,
            Source.sourceToUri(source.getName(), this.server));
      }
      catch (EnergyCounterException e) {
        // some sort of counter problem. For now, we just bail and return an error
        // TODO add rollover support
        return null;
      }
    }
    else {
      List<List<SensorDataStraddle>> masterList =
          getSensorDataStraddleListOfLists(source,
              Tstamp.getTimestampList(startTime, endTime, interval));
      if ((masterList == null) || (masterList.isEmpty())) {
        return null;
      }
      else {
        return Energy.getEnergyFromListOfLists(masterList,
            Source.sourceToUri(source.getName(), this.server));
      }
    }
  }

  /**
   * Returns the energy in SensorData format for the given Source over the range of time between
   * startTime and endTime, or null if no energy data exists.
   * 
   * @param source The source object.
   * @param startTime The start of the range requested.
   * @param endTime The end of the range requested.
   * @param interval The sampling interval requested in minutes (ignored if all sources support
   * energy counters).
   * @return The requested energy in SensorData format, or null if it cannot be found/calculated.
   */
  public SensorData getEnergy(Source source, XMLGregorianCalendar startTime,
      XMLGregorianCalendar endTime, int interval) {

    if (!source.isVirtual()) {
      return getNonVirtualEnergy(source, startTime, endTime, interval);
    }

    List<SensorData> energyList = new ArrayList<SensorData>();
    List<Source> nonVirtualSources = getAllNonVirtualSubSources(source);
    for (Source s : nonVirtualSources) {
      SensorData d = getNonVirtualEnergy(s, startTime, endTime, interval);
      if (d != null) {
        energyList.add(d);
      }
      else {
        return null;
      }
    }
    try {

      double totalEnergyGenerated = 0, totalEnergyConsumed = 0;
      double energyGenerated, energyConsumed;
      boolean wasInterpolated = true;
      XMLGregorianCalendar timestamp;
      if (energyList.isEmpty()) {
        return null;
      }
      else {
        timestamp = energyList.get(0).getTimestamp();
        // iterate over list of Energy objects
        for (SensorData energy : energyList) {
          energyGenerated = energy.getPropertyAsDouble(SensorData.ENERGY_GENERATED);
          energyConsumed = energy.getPropertyAsDouble(SensorData.ENERGY_CONSUMED);
          if (energyGenerated < 0) {
            throw new EnergyCounterException("computed energyGenerated was < 0: " + energyGenerated);
          }
          else if (energyConsumed < 0) {
            throw new EnergyCounterException("computed energyConsumed was < 0: " + energyConsumed);
          }
          else {
            totalEnergyGenerated += energyGenerated;
            totalEnergyConsumed += energyConsumed;
          }
        }
        return Energy.makeEnergySensorData(timestamp, source.getName(), totalEnergyGenerated,
            totalEnergyConsumed, wasInterpolated);
      }
    }
    catch (EnergyCounterException e) {
      // some sort of counter problem. For now, we just bail and return an error
      // TODO add rollover support
      return null;
    }
  }

  /**
   * Given a base Source, return a list of all non-virtual Sources that are subsources of the base
   * Source. This is done recursively, so virtual sources can point to other virtual sources.
   * 
   * @param baseSource The Source to start from.
   * @return A list of all non-virtual Sources that are subsources of the base Source.
   */
  public List<Source> getAllNonVirtualSubSources(Source baseSource) {
    List<Source> sourceList = new ArrayList<Source>();
    if (baseSource.isVirtual()) {
      List<Source> subSources = getAllSubSources(baseSource);
      for (Source subSource : subSources) {
        sourceList.addAll(getAllNonVirtualSubSources(subSource));
      }
      return sourceList;
    }
    else {
      sourceList.add(baseSource);
      return sourceList;
    }
  }

  /**
   * Given a Source, returns a List of Sources corresponding to any subsources of the given Source.
   * 
   * @param source The parent Source.
   * @return A List of Sources that are subsources of the given Source.
   */
  public List<Source> getAllSubSources(Source source) {
    List<Source> sourceList = new ArrayList<Source>();

    if (source.isSetSubSources()) {
      // List<Source> sourceList = source.getSubSourceList();
      // if (sourceList == null) {
      for (String subSourceUri : source.getSubSources().getHref()) {
        Source subSource = getSource(UriUtils.getUriSuffix(subSourceUri));
        if (subSource != null) {
          sourceList.add(subSource);
        }
      }
      // source.setSubSourceList(sourceList);
      // }
    }
    return sourceList;

  }

  /**
   * Returns the carbon emitted in SensorData format for the virtual source Source given over the
   * range of time between startTime and endTime, or null if no carbon data exists.
   * 
   * @param source The source object.
   * @param startTime The start of the range requested.
   * @param endTime The start of the range requested.
   * @param interval The sampling interval requested.
   * @return The requested carbon in SensorData format, or null if it cannot be found/calculated.
   */
  protected SensorData getNonVirtualCarbon(Source source, XMLGregorianCalendar startTime,
      XMLGregorianCalendar endTime, int interval) {

    if (endTime == null) {
      endTime = getLatestNonVirtualSensorData(source.getName()).getTimestamp();

      long minutesToMilliseconds = 60L * 1000L;
      if ((interval * minutesToMilliseconds) > Tstamp.diff(startTime, endTime)) {
        return null;
      }
    }

    List<StraddleList> masterList =
        getStraddleLists(source, Tstamp.getTimestampList(startTime, endTime, interval));
    if ((masterList == null) || (masterList.isEmpty())) {
      return null;
    }
    else {
      // Make list of carbon intensities, one from each source
      return Carbon.getCarbonFromStraddleList(masterList,
          Source.sourceToUri(source.getName(), server));
    }
  }

  /**
   * Returns the carbon emitted in SensorData format for the Source given over the range of time
   * between startTime and endTime, or null if no carbon data exists.
   * 
   * @param source The source object.
   * @param startTime The start of the range requested.
   * @param endTime The start of the range requested.
   * @param interval The sampling interval requested.
   * @return The requested carbon in SensorData format, or null if it cannot be found/calculated.
   */
  public SensorData getCarbon(Source source, XMLGregorianCalendar startTime,
      XMLGregorianCalendar endTime, int interval) {

    if (!source.isVirtual()) {
      return getNonVirtualCarbon(source, startTime, endTime, interval);
    }

    List<SensorData> carbonList = new ArrayList<SensorData>();
    List<Source> nonVirtualSources = getAllNonVirtualSubSources(source);
    for (Source s : nonVirtualSources) {
      SensorData d = getNonVirtualCarbon(s, startTime, endTime, interval);
      if (d != null) {
        carbonList.add(d);
      }
      else {
        return null;
      }
    }
    double totalCarbonEmitted = 0;
    boolean wasInterpolated = true;
    XMLGregorianCalendar timestamp;
    if (carbonList.isEmpty()) {
      return null;
    }
    else {
      timestamp = carbonList.get(0).getTimestamp();
      // iterate over list of Carbon objects
      for (SensorData energy : carbonList) {
        totalCarbonEmitted += energy.getPropertyAsDouble(SensorData.CARBON_EMITTED);
      }
      return Carbon.makeCarbonSensorData(timestamp, source.getName(), totalCarbonEmitted,
          wasInterpolated);
    }

  }

  /**
   * Returns the latest SensorData instance for a particular named Source, or null if not found. If
   * the Source is virtual, the latest SensorData returned is the union of all the properties of the
   * latest SensorData for each SubSource, and for any properties in common with numeric values, the
   * returned value will be the sum of all the values. The timestamp of the SensorData for a virtual
   * Source will be the <b>earliest</b> of the timestamps of the latest SensorData from each
   * SubSource, as this ensures that any subsequent requests for ranges of data using that timestamp
   * will succeed (since all SubSources have valid data up to that endpoint).
   * 
   * @param sourceName The name of the Source whose sensor data is to be returned.
   * @return The SensorData resource, or null.
   */
  public SensorData getLatestSensorData(String sourceName) {
    // Since virtual sources are valid inputs for this method, moved logic into DbManager where
    // cache can be consulted for virtual sources.
    return this.dbManager.getLatestSensorData(sourceName);
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

  /**
   * Creates a snapshot of the database in the directory specified by the Server Properties.
   * 
   * @return True if the snapshot succeeded.
   */
  public abstract boolean makeSnapshot();

  /**
   * Provides ability to stop or close database connection if necessary. By default, does nothing.
   */
  public void stop() {

  }

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

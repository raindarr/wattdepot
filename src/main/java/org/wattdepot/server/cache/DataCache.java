package org.wattdepot.server.cache;

import java.util.Set;
import javax.xml.datatype.XMLGregorianCalendar;
import org.apache.jcs.JCS;
import org.apache.jcs.access.exception.CacheException;
import org.apache.jcs.engine.behavior.IElementAttributes;
import org.wattdepot.resource.sensordata.SensorDataStraddle;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.resource.sensordata.jaxb.SensorDataIndex;
import org.wattdepot.resource.sensordata.jaxb.SensorDataRef;
import org.wattdepot.resource.sensordata.jaxb.SensorDatas;
import org.wattdepot.util.UriUtils;
import org.wattdepot.util.tstamp.Tstamp;

/**
 * Provides ephemeral caching for sensor data objects.
 * 
 * @author Andrea Connell
 * 
 */
public class DataCache {

  private JCS sensorDataCache = null;
  private JCS sourceCheckpointCache = null;

  /**
   * Instantiate the DataCache with two regions - one for sensor data objects and one for checkpoint
   * timestamps.
   */
  public DataCache() {
    try {
      sensorDataCache = JCS.getInstance("sensorData");
      sourceCheckpointCache = JCS.getInstance("sourceCheckpoint");
    }
    catch (CacheException e) {
      System.out.println(e.getMessage());
    }
  }

  /**
   * Returns a sensor data with the given sourceName and timestamp, if one exists in the cache.
   * 
   * @param sourceName The sourceName of the desired sensor data.
   * @param timestamp The timestamp of the desired sensor data.
   * @return A sensor data object matching the given sourceName and timestamp, or null if it does
   * not exist within the cache.
   */
  public SensorData getSensorData(String sourceName, XMLGregorianCalendar timestamp) {
    if (sourceName == null || timestamp == null) {
      return null;
    }

    String key = sourceName + ":" + timestamp.toString();
    return (SensorData) sensorDataCache.get(key);
  }

  /**
   * Returns an index containing all sensor datas for the given sourceName.
   * 
   * @param sourceName The name of the source to generate an index for.
   * @return A SensorDataIndex containing all sensor datas for the given sourceName.
   */
  public SensorDataIndex getSensorDataIndex(String sourceName) {
    if (sourceName == null) {
      return null;
    }

    @SuppressWarnings("unchecked")
    Set<String> group = sensorDataCache.getGroupKeys(sourceName);
    SensorDataIndex index = new SensorDataIndex();

    for (String key : group) {
      SensorData data = (SensorData) sensorDataCache.get(key);
      if (data != null) {
        SensorDataRef ref = new SensorDataRef(data);
        index.getSensorDataRef().add(ref);
      }
    }

    return index;
  }

  /**
   * Returns an index containing all sensor datas for the given sourceName between startTime and
   * endTime.
   * 
   * @param sourceName The name of the source to generate an index for
   * @param startTime The earliest timestamp that will be included in the index.
   * @param endTime The latest timestamp that will be included in the index.
   * @return A SensorDataIndex containing all sensor datas for the given sourceName within the
   * startTime and endTime.
   */
  public SensorDataIndex getSensorDataIndex(String sourceName, XMLGregorianCalendar startTime,
      XMLGregorianCalendar endTime) {
    if (sourceName == null || startTime == null) {
      return null;
    }

    @SuppressWarnings("unchecked")
    Set<String> group = sensorDataCache.getGroupKeys(sourceName);
    SensorDataIndex index = new SensorDataIndex();

    if (endTime == null) {
      SensorData latest = getLatestSensorData(sourceName);
      if (latest != null) {
        endTime = latest.getTimestamp();
      }
      else {
        return index;
      }
    }

    for (String key : group) {
      SensorData data = (SensorData) sensorDataCache.get(key);
      if (data != null && Tstamp.inBetween(startTime, data.getTimestamp(), endTime)) {
        SensorDataRef ref = new SensorDataRef(data);
        index.getSensorDataRef().add(ref);
      }
    }

    return index;
  }

  /**
   * Returns all sensor datas for the given sourceName between startTime and endTime.
   * 
   * @param sourceName The name of the source to find sensor datas for.
   * @param startTime The earliest timestamp that will be included in the result.
   * @param endTime The latest timestamp that will be included in the result.
   * @return All sensor datas for the given sourceName within the startTime and endTime.
   */
  public SensorDatas getSensorDatas(String sourceName, XMLGregorianCalendar startTime,
      XMLGregorianCalendar endTime) {
    if (sourceName == null || startTime == null) {
      return null;
    }

    SensorDatas datas = new SensorDatas();

    if (endTime == null) {
      SensorData latest = getLatestSensorData(sourceName);
      if (latest != null) {
        endTime = latest.getTimestamp();
      }
      else {
        return datas;
      }
    }

    @SuppressWarnings("unchecked")
    Set<String> group = sensorDataCache.getGroupKeys(sourceName);

    for (String key : group) {
      SensorData data = (SensorData) sensorDataCache.get(key);
      if (data != null && Tstamp.inBetween(startTime, data.getTimestamp(), endTime)) {
        datas.getSensorData().add(data);
      }
    }
    return datas;
  }

  /**
   * Finds the most recent sensor data object for the source with the given name.
   * 
   * @param sourceName The name of the source to find the most recent sensor data for.
   * @return The most recent sensor data for the source with the given name.
   */
  public SensorData getLatestSensorData(String sourceName) {
    if (sourceName == null) {
      return null;
    }

    SensorData latestData = null;
    XMLGregorianCalendar latestTime = null;
    @SuppressWarnings("unchecked")
    Set<String> group = sensorDataCache.getGroupKeys(sourceName);

    for (String key : group) {
      SensorData data = (SensorData) sensorDataCache.get(key);
      if (data != null
          && (latestData == null || Tstamp.greaterThan(data.getTimestamp(), latestTime))) {
        latestData = data;
        latestTime = data.getTimestamp();
      }
    }
    return latestData;
  }

  /**
   * Returns a SensorDataStraddle that straddles the given timestamp, using SensorData in the cache
   * from the given source name. If the given timestamp is outside of the window length for the
   * source, return null without looking through each data item. If the given timestamp corresponds
   * to an actual SensorData, then return a degenerate SensorDataStraddle with both ends of the
   * straddle set to the actual SensorData.
   * 
   * @param sourceName The name of the source to generate the straddle from.
   * @param timestamp The timestamp of interest in the straddle.
   * @return A SensorDataStraddle that straddles the given timestamp. Returns null if: parameters
   * are null, the source has no sensor data in the cache, or there is no sensor data that straddles
   * the timestamp in the cache.
   */
  public SensorDataStraddle getSensorDataStraddle(String sourceName, XMLGregorianCalendar timestamp) {
    if (sourceName == null) {
      return null;
    }

    SensorData exact = (SensorData) sensorDataCache.get(sourceName + ":" + timestamp);
    if (exact != null) {
      return new SensorDataStraddle(timestamp, exact, exact);
    }

    SensorData before = null;
    SensorData after = null;

    @SuppressWarnings("unchecked")
    Set<String> group = sensorDataCache.getGroupKeys(sourceName);

    for (String key : group) {
      SensorData data = (SensorData) sensorDataCache.get(key);
      if (data != null) {
        XMLGregorianCalendar dataTime = data.getTimestamp();
        // If this data is at or before the desired timestamp
        // AND is greater than the currently known "before" time
        // then set "before" to be this data.
        if ((Tstamp.lessThan(dataTime, timestamp) || Tstamp.equal(dataTime, timestamp))
            && (before == null || Tstamp.greaterThan(dataTime, before.getTimestamp()))) {
          before = data;
        }
        // If this data is at or after the desired timestamp
        // AND is less than the currently known "after" time
        // then set "after" to be this data.
        if ((Tstamp.greaterThan(dataTime, timestamp) || Tstamp.equal(dataTime, timestamp))
            && (after == null || Tstamp.lessThan(data.getTimestamp(), after.getTimestamp()))) {
          after = data;
        }
      }
    }

    if (before != null && after != null) {
      return new SensorDataStraddle(timestamp, before, after);
    }
    else {
      return null;
    }
  }

  /**
   * Stores a sensor data in the cache. The sensor data will be stored for windowLength minutes. If
   * windowLength is less than or equal to zero, the cache will use default properties from the jcs
   * configuration file.
   * 
   * @param data The sensor data object to be stored.
   * @param windowLength The number of minutes to cache this data for.
   * @return True if the sensor data was stored successfully, false if an exception occurred.
   */
  public boolean storeSensorData(SensorData data, int windowLength) {
    if (data == null) {
      return false;
    }
    try {
      String sourceName = UriUtils.getUriSuffix(data.getSource());
      String key = sourceName + ":" + data.getTimestamp().toString();

      if (sensorDataCache.get(key) != null) {
        return false;
      }

      // How long should the element be cached?
      IElementAttributes attributes = sensorDataCache.getDefaultElementAttributes();
      if (windowLength > 0) {
        attributes.setMaxLifeSeconds(windowLength * 60);
      }

      sensorDataCache.put(key, data, attributes);
      sensorDataCache.putInGroup(key, sourceName, data, attributes);
      return true;
    }
    catch (CacheException e) {
      System.out.println(e.getMessage());

      return false;
    }
  }

  /**
   * Removes the sensor data with the given sourceName and timestamp from the cache.
   * 
   * @param sourceName The sourceName of the sensor data to be deleted.
   * @param timestamp The timestamp of the sensor data to be deleted.
   * @return True if the sensor data was deleted successfully; false if an exception occurred.
   */
  public boolean deleteSensorData(String sourceName, XMLGregorianCalendar timestamp) {
    if (sourceName == null || timestamp == null) {
      return false;
    }
    try {
      String key = sourceName + ":" + timestamp.toString();
      if (sensorDataCache.get(key) == null) {
        return false;
      }
      sensorDataCache.remove(key);
      return true;
    }
    catch (CacheException e) {
      System.out.println(e.getMessage());
      return false;
    }
  }

  /**
   * Removes all sensor datas with the given sourceName from the cache.
   * 
   * @param sourceName The sourceName of the sensor data to be deleted.
   * @return True if the sensor data was deleted successfully; false if an exception occurred.
   */
  public boolean deleteSensorData(String sourceName) {
    if (sourceName == null) {
      return false;
    }
    try {
      String key = sourceName + ":";
      sensorDataCache.remove(key);
      return true;
    }
    catch (CacheException e) {
      System.out.println(e.getMessage());
      return false;
    }
  }

  /**
   * Returns the timestamp of when sensor data for the given sourceName was persisted to the
   * database.
   * 
   * @param sourceName The name of the source to find the last checkpoint time for.
   * @return A timestamp indicating when a sensor data for the source was saved to the database, or
   * null if the event has not happened since this cache was created.
   */
  public XMLGregorianCalendar getSourceCheckpointTimestamp(String sourceName) {
    if (sourceName == null) {
      return null;
    }
    return (XMLGregorianCalendar) sourceCheckpointCache.get(sourceName);
  }

  /**
   * Caches a timestamp indicating that sensor data for the given sourceName has been persisted to
   * the database.
   * 
   * @param sourceName The name of the source for which sensor data is being checkpointed.
   * @param timestamp The timestamp of when sensor data was saved to the database.
   * @return True if the checkpoint timestamp was cached successfully, false if an exception occurs.
   */
  public boolean putSourceCheckpointTimestamp(String sourceName, XMLGregorianCalendar timestamp) {
    if (sourceName == null || timestamp == null) {
      return false;
    }
    try {
      sourceCheckpointCache.put(sourceName, timestamp);
      return true;
    }
    catch (CacheException e) {
      System.out.println(e.getMessage());

      return false;
    }
  }

  /**
   * Removes a timestamp for when sensor data for the given sourceName was persisted to the
   * database. Ensures that the next sensor data will be persisted.
   * 
   * @param sourceName The name of the source to remove a checkpoint timestamp for.
   * @return True if the checkpoint timestamp was removed successfully, false if an exception
   * occurs.
   */
  public boolean deleteSourceCheckpointTimestamp(String sourceName) {
    if (sourceName == null) {
      return false;
    }
    try {
      sourceCheckpointCache.remove(sourceName);
      return true;
    }
    catch (CacheException e) {
      System.out.println(e.getMessage());

      return false;
    }
  }

  /**
   * Removes all cached sensor data and source checkpoint timestamps.
   */
  public void wipeData() {
    try {
      sensorDataCache.clear();
      sourceCheckpointCache.clear();
    }
    catch (CacheException e) {
      System.out.println(e.getMessage());
    }
  }

  /**
   * Determines whether it is time to persist a sensor data to storage based on the checkpoint
   * frequency and the last checkpoint time.
   * 
   * @param sourceName The name of the source we are considering checkpointing.
   * @param timestamp The time of the sensor data we are considering checkpointing.
   * @param checkpointFrequency The number of minutes that should be between checkpoints.
   * @return True if there is no known last checkpoint time, if the given timestamp is before the
   * last checkpoint time, or if checkpointFrequency minutes have passed between the last checkpoint
   * time and the given timestamp.
   */
  public boolean shouldPersist(String sourceName, XMLGregorianCalendar timestamp,
      int checkpointFrequency) {
    if (checkpointFrequency <= 0) {
      return true;
    }

    XMLGregorianCalendar lastStore = getSourceCheckpointTimestamp(sourceName);
    if (lastStore == null) {
      return true;
    }

    // if this timestamp is before our last store time, we'll just retroactively persist it.
    if (Tstamp.lessThan(timestamp, lastStore)) {
      return true;
    }

    // otherwise, check if this sensor data is far enough after the last checkpoint time that we
    // should persist again.
    lastStore = Tstamp.incrementMinutes(lastStore, checkpointFrequency);
    return Tstamp.lessThan(lastStore, timestamp) || Tstamp.equal(lastStore, timestamp);
  }

}

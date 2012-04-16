package org.wattdepot.server.db.berkeleydb;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;
import org.wattdepot.resource.property.jaxb.Property;
import org.wattdepot.resource.sensordata.SensorDataStraddle;
import org.wattdepot.resource.sensordata.StraddleList;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.resource.sensordata.jaxb.SensorDataIndex;
import org.wattdepot.resource.sensordata.jaxb.SensorDataRef;
import org.wattdepot.resource.sensordata.jaxb.SensorDatas;
import org.wattdepot.resource.source.jaxb.Source;
import org.wattdepot.resource.source.jaxb.SourceIndex;
import org.wattdepot.resource.source.jaxb.Sources;
import org.wattdepot.resource.source.jaxb.SubSources;
import org.wattdepot.resource.source.summary.jaxb.SourceSummary;
import org.wattdepot.resource.user.jaxb.User;
import org.wattdepot.resource.user.jaxb.UserIndex;
import org.wattdepot.resource.user.jaxb.UserRef;
import org.wattdepot.server.Server;
import org.wattdepot.server.ServerProperties;
import org.wattdepot.server.db.DbBadIntervalException;
import org.wattdepot.server.db.DbImplementation;
import org.wattdepot.util.UriUtils;
import org.wattdepot.util.tstamp.Tstamp;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.util.DbBackup;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.SecondaryIndex;
import com.sleepycat.persist.StoreConfig;

/**
 * WattDepot DbImplementation using BerkeleyDB as the data store. This is an alternative
 * implementation to be used to compare against other DbImplementations.
 * 
 * @author George Lee
 * 
 */
public class BerkeleyDbImplementation extends DbImplementation {

  private boolean isFreshlyCreated;
  private PrimaryIndex<CompositeSensorDataKey, BerkeleyDbSensorData> sensorDataIndex;
  private PrimaryIndex<CompositeSensorDataPropertyKey, BerkeleyDbSensorDataProperty> sensorDataPropertyPrimaryIndex;
  private SecondaryIndex<CompositeSensorDataKey, CompositeSensorDataPropertyKey, BerkeleyDbSensorDataProperty> sensorDataPropertyIndex;
  private PrimaryIndex<String, BerkeleyDbUser> userIndex;
  private PrimaryIndex<CompositeUserPropertyKey, BerkeleyDbUserProperty> userPropertyPrimaryIndex;
  private SecondaryIndex<String, CompositeUserPropertyKey, BerkeleyDbUserProperty> userPropertyIndex;
  private PrimaryIndex<String, BerkeleyDbSource> sourceIndex;
  private PrimaryIndex<CompositeSourcePropertyKey, BerkeleyDbSourceProperty> sourcePropertyPrimaryIndex;
  private SecondaryIndex<String, CompositeSourcePropertyKey, BerkeleyDbSourceProperty> sourcePropertyIndex;
  private PrimaryIndex<CompositeSourceHierarchyKey, BerkeleyDbSourceHierarchy> sourceHierarchyIndex;
  private SecondaryIndex<String, CompositeSourceHierarchyKey, BerkeleyDbSourceHierarchy> sourceHierarchyParentIndex;
  private SecondaryIndex<String, CompositeSourceHierarchyKey, BerkeleyDbSourceHierarchy> sourceHierarchySubSourceIndex;
  private Environment environment;
  private long lastBackupFileId;
  private File topDir;
  private File backupDir;

  /**
   * Instantiates the BerkeleyDB installation.
   * 
   * @param server The server this implementation is associated with.
   */
  public BerkeleyDbImplementation(Server server) {
    super(server);
  }

  @Override
  public void initialize(boolean wipe) {
    // Construct directories.
    ServerProperties props = server.getServerProperties();
    topDir = new File(props.get(ServerProperties.BERKELEYDB_DIR_KEY));
    boolean success = topDir.mkdirs();
    if (success) {
      System.out.println("Created the berkeleyDb directory.");
    }

    File dir = new File(topDir, "sensorDataDb");
    success = dir.mkdirs();
    if (success) {
      System.out.println("Created the sensorData directory.");
    }

    dir = new File(topDir, "sourceDb");
    success = dir.mkdirs();
    if (success) {
      System.out.println("Created the source directory.");
    }

    dir = new File(topDir, "sourcePropertyDb");
    success = dir.mkdirs();
    if (success) {
      System.out.println("Created the source property directory.");
    }

    dir = new File(topDir, "sourceHierarchyDb");
    success = dir.mkdirs();
    if (success) {
      System.out.println("Created the source hierarchy directory.");
    }

    dir = new File(topDir, "userDb");
    success = dir.mkdirs();
    if (success) {
      System.out.println("Created the user directory.");
    }

    dir = new File(topDir, "userPropertyDb");
    success = dir.mkdirs();
    if (success) {
      System.out.println("Created the user property directory.");
    }

    this.backupDir = new File(topDir, "backup");
    success = this.backupDir.mkdirs();
    if (success) {
      System.out.println("Created the backup directory.");
    }

    // If any directory was created, then this was previously uninitialized.
    this.isFreshlyCreated = success;
    String dbStatusMsg =
        (this.isFreshlyCreated) ? "BerkeleyDB: uninitialized."
            : "BerkeleyDB: previously initialized.";
    this.logger.info(dbStatusMsg);

    // Check if we have any pre-existing backups.
    if (this.isFreshlyCreated) {
      // No backups, so start from zero.
      this.lastBackupFileId = 0;
    }
    else {
      this.lastBackupFileId = this.getLastBackedUpFile();
    }

    // Configure BerkeleyDB.
    EnvironmentConfig envConfig = new EnvironmentConfig();
    StoreConfig storeConfig = new StoreConfig();
    envConfig.setAllowCreate(true);
    storeConfig.setAllowCreate(true);
    this.environment = new Environment(topDir, envConfig);

    // Initialize data stores for sensor data
    EntityStore sensorDataStore = new EntityStore(this.environment, "EntityStore", storeConfig);
    this.sensorDataIndex =
        sensorDataStore.getPrimaryIndex(CompositeSensorDataKey.class, BerkeleyDbSensorData.class);
    EntityStore sensorDataPropertyStore =
        new EntityStore(this.environment, "EntityStore", storeConfig);
    this.sensorDataPropertyPrimaryIndex =
        sensorDataPropertyStore.getPrimaryIndex(CompositeSensorDataPropertyKey.class,
            BerkeleyDbSensorDataProperty.class);
    // Add secondary index so we can search by source name and timestamp only, without property key.
    this.sensorDataPropertyIndex =
        sensorDataPropertyStore.getSecondaryIndex(sensorDataPropertyPrimaryIndex,
            CompositeSensorDataKey.class, "sensorDataKey");

    // Initialize data stores for users
    EntityStore userStore = new EntityStore(this.environment, "EntityStore", storeConfig);
    this.userIndex = userStore.getPrimaryIndex(String.class, BerkeleyDbUser.class);
    EntityStore userPropertyStore = new EntityStore(this.environment, "EntityStore", storeConfig);
    this.userPropertyPrimaryIndex =
        userPropertyStore.getPrimaryIndex(CompositeUserPropertyKey.class,
            BerkeleyDbUserProperty.class);
    // Add secondary index so we can search/delete by username only, without property key.
    this.userPropertyIndex =
        userPropertyStore.getSecondaryIndex(userPropertyPrimaryIndex, String.class, "username");

    // Initialize data stores for sources
    EntityStore sourceStore = new EntityStore(this.environment, "EntityStore", storeConfig);
    this.sourceIndex = sourceStore.getPrimaryIndex(String.class, BerkeleyDbSource.class);
    EntityStore sourcePropertyStore =
        new EntityStore(this.environment, "EntityStore", storeConfig);
    this.sourcePropertyPrimaryIndex =
        sourcePropertyStore.getPrimaryIndex(CompositeSourcePropertyKey.class,
            BerkeleyDbSourceProperty.class);
    // Add secondary index so we can search/delete by source name only, without property key.
    this.sourcePropertyIndex =
        sourcePropertyStore.getSecondaryIndex(sourcePropertyPrimaryIndex, String.class,
            "sourceName");
    EntityStore sourceHierarchyStore =
        new EntityStore(this.environment, "EntityStore", storeConfig);
    this.sourceHierarchyIndex =
        sourceHierarchyStore.getPrimaryIndex(CompositeSourceHierarchyKey.class,
            BerkeleyDbSourceHierarchy.class);
    // Add secondary indexes so we can search/delete by either parent or sub source name alone.
    this.sourceHierarchyParentIndex =
        sourceHierarchyStore.getSecondaryIndex(sourceHierarchyIndex, String.class,
            "parentSourceName");
    this.sourceHierarchySubSourceIndex =
        sourceHierarchyStore
            .getSecondaryIndex(sourceHierarchyIndex, String.class, "subSourceName");

    // Guarantee that the environment is closed upon system exit.
    List<EntityStore> stores = new ArrayList<EntityStore>();
    stores.add(sensorDataStore);
    stores.add(sensorDataPropertyStore);
    stores.add(sourceStore);
    stores.add(sourcePropertyStore);
    stores.add(sourceHierarchyStore);
    stores.add(userStore);
    stores.add(userPropertyStore);
    DbShutdownHook shutdownHook = new DbShutdownHook(this.environment, stores);
    Runtime.getRuntime().addShutdownHook(shutdownHook);

    if (wipe) {
      this.wipeData();
    }
  }

  /**
   * Goes through the backup folder to find the last backed up file.
   * 
   * @return The id of the last backed up file.
   */
  private long getLastBackedUpFile() {
    // Open the backup folder
    long lastBackup = 0;
    File[] files = this.backupDir.listFiles();
    String filename, substring;

    // Go through the files and find files that end with .jdb.
    for (File file : files) {
      if (!file.isDirectory() && file.getName().endsWith(".jdb")) {
        // Parse filename to get the id.
        filename = file.getName();
        substring = filename.substring(0, filename.length() - 4);
        if (lastBackup < Long.parseLong(substring, 16)) {
          lastBackup = Long.parseLong(substring, 16);
        }
      }
    }

    return lastBackup;
  }

  @Override
  public boolean isFreshlyCreated() {
    return this.isFreshlyCreated;
  }

  @Override
  public boolean deleteSensorData(String sourceName, XMLGregorianCalendar timestamp) {
    if (sourceName == null || timestamp == null) {
      return false;
    }

    CompositeSensorDataKey key = new CompositeSensorDataKey(sourceName, timestamp);
    return sensorDataIndex.delete(key);
  }

  @Override
  public boolean deleteSensorData(String sourceName) {
    if (sourceName == null) {
      return false;
    }

    // Construct the range of sensor data.
    CompositeSensorDataKey start = new CompositeSensorDataKey(sourceName, Tstamp.makeTimestamp(0));
    CompositeSensorDataKey end = new CompositeSensorDataKey(sourceName, Tstamp.makeTimestamp());
    EntityCursor<BerkeleyDbSensorDataProperty> pcursor =
        sensorDataPropertyIndex.entities(start, true, end, true);
    int count = 0;
    while (pcursor.next() != null) {
      pcursor.delete();
      count++;
    }
    pcursor.close();

    EntityCursor<BerkeleyDbSensorData> cursor = sensorDataIndex.entities(start, true, end, true);
    count = 0;
    while (cursor.next() != null) {
      cursor.delete();
      count++;
    }
    cursor.close();
    return count > 0;
  }

  @Override
  public boolean deleteSource(String sourceName) {
    if (sourceName == null) {
      return false;
    }
    sourceHierarchyParentIndex.delete(sourceName);
    sourceHierarchySubSourceIndex.delete(sourceName);
    sourcePropertyIndex.delete(sourceName);
    return sourceIndex.delete(sourceName);
  }

  @Override
  public boolean deleteUser(String username) {
    if (username == null) {
      return false;
    }
    userPropertyIndex.delete(username);
    return userIndex.delete(username);
  }

  @Override
  protected SensorData getLatestNonVirtualSensorData(String sourceName) {
    if (sourceName == null) {
      return null;
    }
    CompositeSensorDataKey start = new CompositeSensorDataKey(sourceName, Tstamp.makeTimestamp(0));
    CompositeSensorDataKey end = new CompositeSensorDataKey(sourceName, Tstamp.makeTimestamp());
    EntityCursor<BerkeleyDbSensorData> cursor = sensorDataIndex.entities(start, true, end, true);
    BerkeleyDbSensorData dbData = cursor.last();
    cursor.close();

    if (dbData == null) {
      this.logger.warning("Could not find any db data.");
      return null;
    }

    return dbData.asSensorData(this.server);
  }

  @Override
  public SensorData getSensorData(String sourceName, XMLGregorianCalendar timestamp) {
    if (sourceName == null || timestamp == null) {
      return null;
    }
    CompositeSensorDataKey key = new CompositeSensorDataKey(sourceName, timestamp);
    BerkeleyDbSensorData data = sensorDataIndex.get(key);
    if (data == null) {
      return null;
    }

    SensorData wdData = data.asSensorData(this.server);
    wdData = getSensorDataProperties(key, wdData);
    return wdData;
  }

  /**
   * Select sensor data properties for a given sensor data key and add them to the given jaxb
   * SensorData object.
   * 
   * @param key The sensor data key to find properties for.
   * @param wdData The jaxb SensorData object to add properties to.
   * @return The jaxb SensorData object with properties added.
   */
  private SensorData getSensorDataProperties(CompositeSensorDataKey key, SensorData wdData) {
    EntityCursor<BerkeleyDbSensorDataProperty> cursor =
        sensorDataPropertyIndex.entities(key, true, key, true);

    for (BerkeleyDbSensorDataProperty dbProp : cursor) {
      if (dbProp != null
          && !dbProp.getCompositeKey().getPropertyKey().equals(SensorData.POWER_CONSUMED)
          && !dbProp.getCompositeKey().getPropertyKey().equals(SensorData.ENERGY_CONSUMED_TO_DATE)
          && !dbProp.getCompositeKey().getPropertyKey().equals(SensorData.POWER_GENERATED)
          && !dbProp.getCompositeKey().getPropertyKey()
              .equals(SensorData.ENERGY_GENERATED_TO_DATE)) {

        wdData.addProperty(dbProp.asSensorDataProperty());
      }
    }
    cursor.close();
    return wdData;
  }

  @Override
  public SensorDataIndex getSensorDataIndex(String sourceName) {
    if (sourceName == null) {
      return null;
    }

    try {
      return this.getSensorDataIndex(sourceName, Tstamp.makeTimestamp(0), Tstamp.makeTimestamp());
    }
    catch (DbBadIntervalException e) {
      // This shouldn't happen.
      this.logger.warning("getSensorDataIndex failed for an interval from 0 to today.");
      return null;
    }
  }

  @Override
  public SensorDataIndex getSensorDataIndex(String sourceName, XMLGregorianCalendar startTime,
      XMLGregorianCalendar endTime) throws DbBadIntervalException {
    if (sourceName == null || startTime == null || endTime == null) {
      return null;
    }
    else if (this.getSource(sourceName) == null) {
      // Unknown Source name, therefore no possibility of SensorData
      return null;
    }
    else if (startTime.compare(endTime) == DatatypeConstants.GREATER) {
      // startTime > endTime, which is bogus
      throw new DbBadIntervalException(startTime, endTime);
    }

    // Construct the range
    CompositeSensorDataKey start = new CompositeSensorDataKey(sourceName, startTime);
    CompositeSensorDataKey end = new CompositeSensorDataKey(sourceName, endTime);
    EntityCursor<BerkeleyDbSensorData> cursor = sensorDataIndex.entities(start, true, end, true);

    // Iterate over the results and add refs.
    SensorDataIndex index = new SensorDataIndex();
    for (BerkeleyDbSensorData data : cursor) {
      String sourceUri = Source.sourceToUri(data.getSourceName(), this.server);
      SensorDataRef ref = new SensorDataRef(data.getTimestamp(), data.getTool(), sourceUri);
      index.getSensorDataRef().add(ref);
    }
    cursor.close();
    return index;
  }

  @Override
  public SensorDataStraddle getSensorDataStraddle(Source source, XMLGregorianCalendar timestamp) {
    if (source == null || timestamp == null) {
      return null;
    }

    SensorData data = getSensorData(source.getName(), timestamp);
    if (data == null) {
      BerkeleyDbSensorData dbData;
      SensorData beforeData, afterData;

      // Grab the item immediately previous.
      CompositeSensorDataKey start =
          new CompositeSensorDataKey(source.getName(), Tstamp.makeTimestamp(0));
      CompositeSensorDataKey end = new CompositeSensorDataKey(source.getName(), timestamp);
      EntityCursor<BerkeleyDbSensorData> cursor = sensorDataIndex.entities(start, true, end, true);
      dbData = cursor.last();
      cursor.close();
      if (dbData == null) {
        // No previous data, so no straddle.
        return null;
      }
      beforeData = dbData.asSensorData(this.server);

      // Grab the item immediately after.
      start = new CompositeSensorDataKey(source.getName(), timestamp);
      end = new CompositeSensorDataKey(source.getName(), Tstamp.makeTimestamp());
      cursor = sensorDataIndex.entities(start, true, end, true);
      dbData = cursor.first();
      cursor.close();
      if (dbData == null) {
        // No post data, so no straddle.
        return null;
      }
      afterData = dbData.asSensorData(this.server);

      return new SensorDataStraddle(timestamp, beforeData, afterData);
    }

    // We have data for this timestamp, so just return the same data twice.
    return new SensorDataStraddle(timestamp, data, data);
  }

  @Override
  public List<SensorDataStraddle> getSensorDataStraddleList(Source source,
      XMLGregorianCalendar timestamp) {
    if ((source == null) || (timestamp == null)) {
      return null;
    }

    // Want to go through sensordata for base source, and all subsources recursively
    List<Source> sourceList = getAllNonVirtualSubSources(source);
    List<SensorDataStraddle> straddleList = new ArrayList<SensorDataStraddle>(sourceList.size());
    for (Source subSource : sourceList) {
      SensorDataStraddle straddle = getSensorDataStraddle(subSource, timestamp);
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

  @Override
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
        SensorDataStraddle straddle = getSensorDataStraddle(subSource, timestamp);
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

  @Override
  public SensorDatas getSensorDatas(String sourceName, XMLGregorianCalendar startTime,
      XMLGregorianCalendar endTime) throws DbBadIntervalException {
    if ((sourceName == null) || (startTime == null) || (endTime == null)) {
      return null;
    }
    else if (getSource(sourceName) == null) {
      // Unknown Source name, therefore no possibility of SensorData
      return null;
    }
    else if (startTime.compare(endTime) == DatatypeConstants.GREATER) {
      // startTime > endTime, which is bogus
      throw new DbBadIntervalException(startTime, endTime);
    }

    CompositeSensorDataKey start = new CompositeSensorDataKey(sourceName, startTime);
    CompositeSensorDataKey end = new CompositeSensorDataKey(sourceName, endTime);
    EntityCursor<BerkeleyDbSensorData> cursor = sensorDataIndex.entities(start, true, end, true);

    SensorDatas datas = new SensorDatas();
    for (BerkeleyDbSensorData data : cursor) {
      SensorData wdData = data.asSensorData(this.server);
      wdData = getSensorDataProperties(data.getCompositeKey(), wdData);
      datas.getSensorData().add(wdData);
    }
    cursor.close();
    return datas;
  }

  @Override
  public Source getSource(String sourceName) {
    if (sourceName == null) {
      return null;
    }

    BerkeleyDbSource dbSource = sourceIndex.get(sourceName);
    if (dbSource == null) {
      return null;
    }

    Source wdSource = dbSource.asSource(this.server);
    wdSource = getSourceProperties(sourceName, wdSource);
    wdSource.setSubSources(getSubSources(sourceName));
    return wdSource;
  }

  @Override
  public SourceIndex getSourceIndex() {
    SourceIndex index = new SourceIndex();
    EntityCursor<BerkeleyDbSource> cursor = sourceIndex.entities();

    for (BerkeleyDbSource source : cursor) {
      index.getSourceRef().add(source.asSourceRef(this.server));
    }
    cursor.close();
    return index;
  }

  @Override
  public SourceSummary getSourceSummary(String sourceName) {
    if (sourceName == null) {
      // null or non-existent source name
      return null;
    }
    Source baseSource = getSource(sourceName);
    if (baseSource == null) {
      return null;
    }
    SourceSummary summary = new SourceSummary();
    summary.setHref(Source.sourceToUri(sourceName, this.server.getHostName()));

    // Want to go through sensordata for base source, and all subsources recursively
    List<Source> sourceList = getAllNonVirtualSubSources(baseSource);
    XMLGregorianCalendar firstTimestamp = null, lastTimestamp = null;
    int dataCount = 0;
    BerkeleyDbSensorData temp;
    CompositeSensorDataKey start, end;
    EntityCursor<BerkeleyDbSensorData> cursor;
    for (Source subSource : sourceList) {
      // Create cursor for getting data.
      start = new CompositeSensorDataKey(subSource.getName(), Tstamp.makeTimestamp(0));
      end = new CompositeSensorDataKey(subSource.getName(), Tstamp.makeTimestamp());
      cursor = sensorDataIndex.entities(start, true, end, true);

      // Get first timestamp of sensor data.
      if ((temp = cursor.first()) != null
          && (firstTimestamp == null || Tstamp.lessThan(temp.getTimestamp(), firstTimestamp))) {
        firstTimestamp = temp.getTimestamp();
      }

      // Iterate through and count up the number of items
      // Note that we are already starting from the first, so we add one extra.
      dataCount++;
      while (cursor.next() != null) {
        dataCount++;
      }

      // Get last timestamp of sensor data.
      if ((temp = cursor.last()) != null
          && (lastTimestamp == null || Tstamp.greaterThan(temp.getTimestamp(), lastTimestamp))) {
        lastTimestamp = temp.getTimestamp();
      }

      // Clean up
      cursor.close();
    }

    summary.setFirstSensorData(firstTimestamp);
    summary.setLastSensorData(lastTimestamp);
    summary.setTotalSensorDatas(dataCount);
    return summary;
  }

  @Override
  public Sources getSources() {
    Sources sources = new Sources();
    EntityCursor<BerkeleyDbSource> cursor = sourceIndex.entities();
    for (BerkeleyDbSource source : cursor) {
      Source wdSource = source.asSource(this.server);
      wdSource = getSourceProperties(source.getName(), wdSource);
      wdSource.setSubSources(getSubSources(source.getName()));
      sources.getSource().add(wdSource);
    }

    cursor.close();
    return sources;
  }

  /**
   * Select source properties for a given source name and add them to the given jaxb Source object.
   * 
   * @param name The source name to find properties for.
   * @param wdSource The jaxb Source object to add properties to.
   * @return The jaxb Source object with properties added.
   */
  private Source getSourceProperties(String name, Source wdSource) {
    EntityCursor<BerkeleyDbSourceProperty> pcursor =
        sourcePropertyIndex.entities(name, true, name, true);

    for (BerkeleyDbSourceProperty dbProp : pcursor) {
      if (dbProp != null
          && !dbProp.getCompositeKey().getPropertyKey().equals(Source.CARBON_INTENSITY)
          && !dbProp.getCompositeKey().getPropertyKey().equals(Source.FUEL_TYPE)
          && !dbProp.getCompositeKey().getPropertyKey().equals(Source.UPDATE_INTERVAL)
          && !dbProp.getCompositeKey().getPropertyKey().equals(Source.ENERGY_DIRECTION)
          && !dbProp.getCompositeKey().getPropertyKey().equals(Source.SUPPORTS_ENERGY_COUNTERS)) {
        wdSource.addProperty(dbProp.asSourceProperty());
      }
    }
    pcursor.close();

    return wdSource;
  }

  /**
   * Select sub-sources for the given source name and return them as a jaxb SubSources object.
   * 
   * @param name The source name to find sub-sources for.
   * @return The jaxb SubSources object containing the sub-sources for the given source name.
   */
  private SubSources getSubSources(String name) {
    SubSources subSources = new SubSources();
    EntityCursor<BerkeleyDbSourceHierarchy> cursor =
        sourceHierarchyParentIndex.entities(name, true, name, true);
    for (BerkeleyDbSourceHierarchy h : cursor) {
      if (h != null) {
        String subSourceName = h.getCompositeKey().getSubSourceName();
        subSources.getHref().add(Source.sourceToUri(subSourceName, this.server));
      }
    }
    cursor.close();
    if (subSources.isSetHref()) {
      return subSources;
    }
    else {
      return null;
    }
  }

  @Override
  public List<StraddleList> getStraddleLists(Source source,
      List<XMLGregorianCalendar> timestampList) {
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
        SensorDataStraddle straddle = getSensorDataStraddle(subSource, timestamp);
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

  @Override
  public User getUser(String username) {
    if (username == null) {
      return null;
    }

    BerkeleyDbUser user = userIndex.get(username);
    if (user == null) {
      return null;
    }

    User wdUser = user.asUser();

    EntityCursor<BerkeleyDbUserProperty> cursor =
        userPropertyIndex.entities(username, true, username, true);

    for (BerkeleyDbUserProperty dbProp : cursor) {
      if (dbProp != null) {
        wdUser.addProperty(dbProp.asUserProperty());
      }
    }
    cursor.close();

    return wdUser;
  }

  @Override
  public UserIndex getUsers() {
    UserIndex index = new UserIndex();
    EntityCursor<BerkeleyDbUser> cursor = userIndex.entities();
    for (BerkeleyDbUser user : cursor) {
      index.getUserRef().add(new UserRef(user.getUsername(), this.server));
    }
    cursor.close();
    return index;
  }

  @Override
  public boolean hasSensorData(String sourceName, XMLGregorianCalendar timestamp) {
    return this.getSensorData(sourceName, timestamp) != null;
  }

  @Override
  public boolean indexTables() {
    // No need to index.
    return true;
  }

  @Override
  public boolean makeSnapshot() {
    // Use the DbBackup helper class to backup our database.
    // See:
    // http://download.oracle.com/docs/cd/E17277_02/html/GettingStartedGuide/backup.html#dbbackuphelper
    DbBackup backupHelper = new DbBackup(this.environment, this.lastBackupFileId);

    // Determine what was the last backup file.
    boolean success = false;
    backupHelper.startBackup();
    try {
      String[] filesForBackup = backupHelper.getLogFilesInBackupSet();
      success = this.writeBackup(filesForBackup);

      // Update our last known backup file.
      this.lastBackupFileId = backupHelper.getLastFileInBackupSet();
    }
    finally {
      // Exit backup mode to clean up.
      backupHelper.endBackup();
    }
    return success;
  }

  @Override
  public boolean storeSensorData(SensorData data) {
    if (data == null) {
      return false;
    }

    BerkeleyDbSensorData dbData;
    String sourceName = UriUtils.getUriSuffix(data.getSource());
    if (data.isSetProperties()) {
      dbData =
          new BerkeleyDbSensorData(data.getTimestamp(), data.getTool(), sourceName,
              data.getProperties());

      for (Property p : data.getProperties().getProperty()) {
        if (!p.getKey().equals(SensorData.POWER_CONSUMED)
            && !p.getKey().equals(SensorData.ENERGY_CONSUMED_TO_DATE)
            && !p.getKey().equals(SensorData.POWER_GENERATED)
            && !p.getKey().equals(SensorData.ENERGY_GENERATED_TO_DATE)) {

          BerkeleyDbSensorDataProperty dbProp =
              new BerkeleyDbSensorDataProperty(sourceName, dbData.getTimestamp(), p.getKey(),
                  p.getValue());
          sensorDataPropertyPrimaryIndex.put(dbProp);
        }
      }
    }
    else {
      dbData = new BerkeleyDbSensorData(data.getTimestamp(), data.getTool(), sourceName);
    }
    return sensorDataIndex.putNoOverwrite(dbData);
  }

  @Override
  public boolean storeSource(Source source, boolean overwrite) {
    if (source == null) {
      return false;
    }

    String owner = UriUtils.getUriSuffix(source.getOwner());

    // Before storing anything, check that owner and subsources are all valid
    if (!userIndex.contains(owner)) {
      return false;
    }
    if (source.isSetSubSources() && source.getSubSources().isSetHref()) {
      for (String subSource : source.getSubSources().getHref()) {
        if (!sourceIndex.contains(UriUtils.getUriSuffix(subSource))) {
          return false;
        }
      }
    }

    BerkeleyDbSource dbSource =
        new BerkeleyDbSource(source.getName(), owner, source.isPublic(), source.isVirtual(),
            source.getLocation(), source.getDescription(), source.getCoordinates(),
            source.getProperties());

    if (source.isSetProperties()) {
      for (Property p : source.getProperties().getProperty()) {
        if (!p.getKey().equals(Source.CARBON_INTENSITY) && !p.getKey().equals(Source.FUEL_TYPE)
            && !p.getKey().equals(Source.UPDATE_INTERVAL)
            && !p.getKey().equals(Source.ENERGY_DIRECTION)
            && !p.getKey().equals(Source.SUPPORTS_ENERGY_COUNTERS)) {
          BerkeleyDbSourceProperty dbProp =
              new BerkeleyDbSourceProperty(dbSource.getName(), p.getKey(), p.getValue());
          sourcePropertyPrimaryIndex.put(dbProp);
        }
      }
    }

    if (source.isSetSubSources() && source.getSubSources().isSetHref()) {
      for (String subSource : source.getSubSources().getHref()) {
        sourceHierarchyIndex.put(new BerkeleyDbSourceHierarchy(dbSource.getName(), subSource));
      }
    }

    if (!overwrite) {
      return sourceIndex.putNoOverwrite(dbSource);
    }

    sourceIndex.put(dbSource);
    return true;
  }

  @Override
  public boolean storeUser(User user) {
    if (user == null) {
      return false;
    }

    BerkeleyDbUser dbUser;
    dbUser = new BerkeleyDbUser(user.getEmail(), user.getPassword(), user.isAdmin());

    boolean success = userIndex.putNoOverwrite(dbUser);

    if (user.isSetProperties()) {
      for (Property p : user.getProperties().getProperty()) {
        BerkeleyDbUserProperty dbProp =
            new BerkeleyDbUserProperty(user.getEmail(), p.getKey(), p.getValue());
        userPropertyPrimaryIndex.put(dbProp);
      }
    }

    if (!success) {
      this.logger.fine("BerkeleyDB: Attempted to overwrite User " + user.getEmail());
    }

    return success;
  }

  @Override
  public boolean wipeData() {
    EntityCursor<BerkeleyDbSensorDataProperty> sensorDataPropCursor =
        sensorDataPropertyPrimaryIndex.entities();
    while (sensorDataPropCursor.next() != null) {
      sensorDataPropCursor.delete();
    }
    sensorDataPropCursor.close();

    EntityCursor<BerkeleyDbSensorData> sensorDataCursor = sensorDataIndex.entities();
    while (sensorDataCursor.next() != null) {
      sensorDataCursor.delete();
    }
    sensorDataCursor.close();

    EntityCursor<BerkeleyDbSourceProperty> sourcePropCursor =
        sourcePropertyPrimaryIndex.entities();
    while (sourcePropCursor.next() != null) {
      sourcePropCursor.delete();
    }
    sourcePropCursor.close();

    EntityCursor<BerkeleyDbSourceHierarchy> sourceHierarchyCursor =
        sourceHierarchyIndex.entities();
    while (sourceHierarchyCursor.next() != null) {
      sourceHierarchyCursor.delete();
    }
    sourceHierarchyCursor.close();

    EntityCursor<BerkeleyDbSource> sourceCursor = sourceIndex.entities();
    while (sourceCursor.next() != null) {
      sourceCursor.delete();
    }
    sourceCursor.close();

    EntityCursor<BerkeleyDbUserProperty> userPropCursor = userPropertyPrimaryIndex.entities();
    while (userPropCursor.next() != null) {
      userPropCursor.delete();
    }
    userPropCursor.close();

    EntityCursor<BerkeleyDbUser> userCursor = userIndex.entities();
    while (userCursor.next() != null) {
      userCursor.delete();
    }
    userCursor.close();

    return true;
  }

  /**
   * Write the files to the backup folder.
   * 
   * @param filenames List of files to backup.
   * @return True if the backup is successful, false otherwise.
   */
  private boolean writeBackup(String[] filenames) {
    boolean success = false;
    File sourceFile, destFile;
    FileChannel source = null;
    FileChannel dest = null;

    if (filenames.length == 0) {
      // Nothing to back up.
      return true;
    }

    for (String filename : filenames) {
      // Filenames are rooted in berkeleyDb folder.
      sourceFile = new File(topDir, filename);
      destFile = new File(this.backupDir, sourceFile.getName());

      try {
        source = new FileInputStream(sourceFile).getChannel();
        dest = new FileOutputStream(destFile).getChannel();
        dest.transferFrom(source, 0, source.size());
        success = true;
      }
      catch (FileNotFoundException e) {
        this.logger.warning("BerkeleyDB: Could not backup file " + filename);
      }
      catch (IOException e) {
        this.logger.warning("BerkeleyDB: Could not copy file " + filename);
      }
      finally {
        try {
          if (source != null) {
            source.close();
          }
          if (dest != null) {
            dest.close();
          }
        }
        catch (IOException e) {
          this.logger.warning("BerkeleyDB: Could not close source and dest channels.");
        }
      }
    }
    return success;
  }

  @Override
  public boolean performMaintenance() {
    // Apparently, there's no need for me to manage the compression of the database.
    // See:
    // http://download.oracle.com/docs/cd/E17277_02/html/GettingStartedGuide/backgroundthreads.html
    return true;
  }
}

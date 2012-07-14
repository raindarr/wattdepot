package org.wattdepot.server.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.wattdepot.resource.property.jaxb.Properties;
import javax.xml.datatype.XMLGregorianCalendar;
import org.junit.Test;
import org.wattdepot.resource.property.jaxb.Property;
import org.wattdepot.resource.sensordata.SensorDataStraddle;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.resource.sensordata.jaxb.SensorDataIndex;
import org.wattdepot.resource.sensordata.jaxb.SensorDatas;
import org.wattdepot.resource.source.jaxb.Source;
import org.wattdepot.resource.user.jaxb.User;
import org.wattdepot.util.tstamp.Tstamp;

public class TestDbManagerCache extends DbManagerTestHelper {

  /** Test Users used by the tests, but never changed. */
  private final User user1 = makeTestUser1(), user2 = makeTestUser2();

  /** Test Sources used by the tests. */
  private Source source1 = makeTestSource1(), source2 = makeTestSource2();
  private final String source1name = source1.getName(), source2name = source2.getName();

  /** Test SensorData used by the tests, but never changed. */
  private final SensorData data1 = makeTestSensorData1(), data2 = makeTestSensorData2(),
      data3 = makeTestSensorData3(), data4 = makeTestSensorData4(), data5 = makeTestSensorData5(),
      data6 = makeTestSensorData6(), data7 = makeTestSensorData7(), data8 = makeTestSensorData8(),
      data9 = makeTestSensorData9(), data10 = makeTestSensorData10();

  /**
   * Creates the test object, and throws exceptions if needed.
   * 
   * @throws Exception If there is a problem creating test data objects.
   */
  public TestDbManagerCache() throws Exception {
    // setup();
  }

  /**
   * Adds test Users and test Sources.
   */
  private void createTestData() {
    assertTrue("Unable to store a User in DB", manager.storeUser(this.user1));
    assertTrue("Unable to store a User in DB", manager.storeUser(this.user2));

    source1.addProperty(new Property(Source.SUPPORTS_ENERGY_COUNTERS, "true"));
    source1.addProperty(new Property(Source.CACHE_CHECKPOINT_INTERVAL, "1"));
    assertTrue("Unable to store a Source in DB", manager.storeSource(this.source1));

    source2.addProperty(new Property(Source.SUPPORTS_ENERGY_COUNTERS, "true"));
    source2.addProperty(new Property(Source.CACHE_CHECKPOINT_INTERVAL, "2"));
    assertTrue("Unable to store a Source in DB", manager.storeSource(this.source2));

    assertTrue("Unable to store a SensorData in DB", manager.storeSensorData(this.data1, source1));
    assertTrue("Unable to store a SensorData in DB", manager.storeSensorData(this.data2, source1));
    assertTrue("Unable to store a SensorData in DB", manager.storeSensorData(this.data3, source1));
    assertTrue("Unable to store a SensorData in DB", manager.storeSensorData(this.data4, source1));
    assertTrue("Unable to store a SensorData in DB", manager.storeSensorData(this.data5, source1));

    assertTrue("Unable to store a SensorData in DB", manager.storeSensorData(this.data6, source2));
    assertTrue("Unable to store a SensorData in DB", manager.storeSensorData(this.data7, source2));
    assertTrue("Unable to store a SensorData in DB", manager.storeSensorData(this.data8, source2));
    assertTrue("Unable to store a SensorData in DB", manager.storeSensorData(this.data9, source2));
    assertTrue("Unable to store a SensorData in DB", manager.storeSensorData(this.data10, source2));
  }

  /**
   * Test that the SensorDataIndex returns the correct number of elements. Check the full index from
   * dbManager, the index from the cache (which should be the same as the full index), and the index
   * from storage (which will have fewer elements).
   */
  @Test
  public void testCacheIndex() {
    createTestData();

    // Source 1 will store 3 sensor datas to disk
    SensorDataIndex fullIndex = manager.getSensorDataIndex(this.source1name);
    assertEquals("DbManager returns wrong number of sensor datas", 5, fullIndex.getSensorDataRef()
        .size());

    SensorDataIndex dbIndex = manager.dbImpl.getSensorDataIndex(this.source1name);
    assertEquals("DbImplementation returns wrong number of sensor datas", 3, dbIndex
        .getSensorDataRef().size());

    SensorDataIndex cacheIndex = manager.cache.getSensorDataIndex(this.source1name);
    assertEquals("DataCache returns wrong number of sensor datas", 5, cacheIndex
        .getSensorDataRef().size());
  }

  /**
   * Test that varying the checkpoint interval results in different amounts of data going to
   * storage.
   */
  @Test
  public void testCacheCheckpointInterval() {
    createTestData();

    SensorDataIndex dbIndex = manager.dbImpl.getSensorDataIndex(this.source1name);
    assertEquals("DbImplementation returns wrong number of sensor datas", 3, dbIndex
        .getSensorDataRef().size());

    SensorDataIndex dbIndex2 = manager.dbImpl.getSensorDataIndex(this.source2name);
    assertEquals("DbImplementation returns wrong number of sensor datas", 2, dbIndex2
        .getSensorDataRef().size());
  }

  /**
   * Test that the cache is empty upon startup.
   */
  @Test
  public void testEmptyCache() {
    SensorDataIndex cacheIndex = manager.cache.getSensorDataIndex(this.source1name);
    assertEquals("DataCache returns wrong number of sensor datas", 0, cacheIndex
        .getSensorDataRef().size());
  }

  /**
   * Test that getSensorDatas includes the cached elements.
   * 
   * @throws DbBadIntervalException If the timestamps are passed in the wrong order.
   */
  @Test
  public void testCacheSensorDatas() throws DbBadIntervalException {
    createTestData();

    SensorDatas fullDatas =
        manager.getSensorDatas(this.source1name, this.data1.getTimestamp(),
            this.data5.getTimestamp());
    assertEquals("DbManager returns wrong number of sensor datas", 5, fullDatas.getSensorData()
        .size());
    SensorDatas dbDatas =
        manager.dbImpl.getSensorDatas(this.source1name, this.data1.getTimestamp(),
            this.data5.getTimestamp());
    assertEquals("DbImplementation returns wrong number of sensor datas", 3, dbDatas
        .getSensorData().size());
    SensorDatas cacheDatas =
        manager.cache.getSensorDatas(this.source1name, this.data1.getTimestamp(),
            this.data5.getTimestamp());
    assertEquals("DataCache returns wrong number of sensor datas", 5, cacheDatas.getSensorData()
        .size());
  }

  /**
   * Test that SensorDataStraddle includes the cached elements.
   * 
   * @throws Exception If there are problems converting timestamp string to XMLGregorianCalendar
   */
  @Test
  public void testCacheStraddle() throws Exception {
    createTestData();

    XMLGregorianCalendar time = Tstamp.makeTimestamp("2009-07-28T09:00:45.000-10:00");
    SensorDataStraddle straddle = manager.getSensorDataStraddle(this.source2, time);
    assertEquals("DbManager returns wrong SensorDataStraddle", this.data7,
        straddle.getBeforeData());
    assertEquals("DbManager returns wrong SensorDataStraddle", this.data8, straddle.getAfterData());

    SensorDataStraddle dbStraddle = manager.dbImpl.getSensorDataStraddle(this.source2, time);
    assertEquals("DbImplementation returns wrong SensorDataStraddle", this.data6,
        dbStraddle.getBeforeData());
    assertEquals("DbImplementation returns wrong SensorDataStraddle", this.data10,
        dbStraddle.getAfterData());

    SensorDataStraddle cacheStraddle = manager.getSensorDataStraddle(this.source2, time);
    assertEquals("DbManager returns wrong SensorDataStraddle", this.data7,
        cacheStraddle.getBeforeData());
    assertEquals("DbManager returns wrong SensorDataStraddle", this.data8,
        cacheStraddle.getAfterData());
  }

  /**
   * Test that the getLatestSensorData includes the cached elements.
   */
  @Test
  public void testCacheLatest() {
    createTestData();

    assertEquals("DbManager retrieved incorrect latest sensor data", this.data5,
        this.manager.getLatestSensorData(source1name));
    assertEquals("Cache retrieved incorrect latest sensor data", this.data5,
        this.manager.cache.getLatestSensorData(source1name));
    assertEquals("DbImpl retrieved incorrect latest sensor data", this.data5,
        this.manager.dbImpl.getLatestNonVirtualSensorData(source1name));

    assertTrue("Could not delete sensor data",
        this.manager.deleteSensorData(source1name, this.data5.getTimestamp()));

    assertEquals("DbManager retrieved incorrect latest sensor data", this.data4,
        this.manager.getLatestSensorData(source1name));
    assertEquals("Cache retrieved incorrect latest sensor data", this.data4,
        this.manager.cache.getLatestSensorData(source1name));
    assertEquals("DbImpl retrieved incorrect latest sensor data", this.data3,
        this.manager.dbImpl.getLatestNonVirtualSensorData(source1name));
  }

  /**
   * Test that getPower, getEnergy, and getCarbon include the cached elements.
   */
  @Test
  public void testCacheProperties() {
    createTestData();

    SensorData power = manager.getPower(this.source2, this.data3.getTimestamp());
    assertEquals("DbManager returned wrong power value", 9000.0, power.getProperties()
        .getPropertyAsDouble(SensorData.POWER_GENERATED), 0);

    SensorData energy =
        manager.getEnergy(this.source2, this.data2.getTimestamp(), this.data4.getTimestamp(), 0);
    assertEquals("DbManager returned wrong energy value", 80.0, energy.getProperties()
        .getPropertyAsDouble(SensorData.ENERGY_GENERATED), 0);

    SensorData carbon =
        manager.getCarbon(this.source2, this.data2.getTimestamp(), this.data4.getTimestamp(), 0);
    assertEquals("DbManager returned wrong carbon value", .0176, carbon.getProperties()
        .getPropertyAsDouble(SensorData.CARBON_EMITTED), 0);
  }

  /**
   * Test that deleting an element removes it from the cache.
   */
  @Test
  public void testCacheDelete() {
    createTestData();

    SensorDataIndex fullIndex = manager.getSensorDataIndex(this.source1name);
    assertEquals("Full index has wrong number of sensor datas", 5, fullIndex.getSensorDataRef()
        .size());
    SensorDataIndex dbIndex = manager.dbImpl.getSensorDataIndex(this.source1name);
    assertEquals("dbImpl index has wrong number of sensor datas", 3, dbIndex.getSensorDataRef()
        .size());
    SensorDataIndex cacheIndex = manager.cache.getSensorDataIndex(this.source1name);
    assertEquals("Cache index has wrong number of sensor datas", 5, cacheIndex.getSensorDataRef()
        .size());

    assertEquals("Could not retrieve cached sensor data", this.data2,
        manager.cache.getSensorData(this.source1name, this.data2.getTimestamp()));

    // This should delete from cache only
    assertTrue("Sensor Data could not be deleted",
        manager.deleteSensorData(this.source1name, this.data2.getTimestamp()));

    assertNull("Could retrieve deleted sensor data",
        manager.cache.getSensorData(this.source1name, this.data2.getTimestamp()));

    fullIndex = manager.getSensorDataIndex(this.source1name);
    assertEquals("Full index has wrong number of sensor datas", 4, fullIndex.getSensorDataRef()
        .size());
    dbIndex = manager.dbImpl.getSensorDataIndex(this.source1name);
    assertEquals("dbImpl index has wrong number of sensor datas", 3, dbIndex.getSensorDataRef()
        .size());
    cacheIndex = manager.cache.getSensorDataIndex(this.source1name);
    assertEquals("Cache index has wrong number of sensor datas", 4, cacheIndex.getSensorDataRef()
        .size());

    // This should delete from cache and db
    assertTrue("Sensor Data could not be deleted",
        manager.deleteSensorData(this.source1name, this.data1.getTimestamp()));

    fullIndex = manager.getSensorDataIndex(this.source1name);
    assertEquals("Full index has wrong number of sensor datas", 3, fullIndex.getSensorDataRef()
        .size());
    dbIndex = manager.dbImpl.getSensorDataIndex(this.source1name);
    assertEquals("dbImpl index has wrong number of sensor datas", 2, dbIndex.getSensorDataRef()
        .size());
    cacheIndex = manager.cache.getSensorDataIndex(this.source1name);
    assertEquals("Cache index has wrong number of sensor datas", 3, cacheIndex.getSensorDataRef()
        .size());

    // This should delete everything from cache and db
    assertTrue("Sensor Data could not be deleted", manager.deleteSensorData(this.source1name));

    fullIndex = manager.getSensorDataIndex(this.source1name);
    assertEquals("Full index has wrong number of sensor datas", 0, fullIndex.getSensorDataRef()
        .size());
    dbIndex = manager.dbImpl.getSensorDataIndex(this.source1name);
    assertEquals("dbImpl index has wrong number of sensor datas", 0, dbIndex.getSensorDataRef()
        .size());
    cacheIndex = manager.cache.getSensorDataIndex(this.source1name);
    assertEquals("Cache index has wrong number of sensor datas", 0, cacheIndex.getSensorDataRef()
        .size());
  }

  /**
   * Test that the storage isn't used if a data point is found in the cache.
   */
  @Test
  public void testCacheUse() {
    assertTrue("Unable to store a User in DB", manager.storeUser(this.user1));

    source1.addProperty(new Property(Source.SUPPORTS_ENERGY_COUNTERS, "true"));
    source1.addProperty(new Property(Source.CACHE_CHECKPOINT_INTERVAL, "1"));
    assertTrue("Unable to store a Source in DB", manager.storeSource(this.source1));

    assertTrue("Unable to store a SensorData in DB", manager.storeSensorData(this.data1, source1));
    assertTrue("Unable to store a SensorData in DB", manager.cache.storeSensorData(this.data2, 1));
    assertTrue("Unable to store a SensorData in DB", manager.dbImpl.storeSensorData(this.data3));

    assertEquals("Cache returned incorrect latest sensor data", data2,
        manager.cache.getLatestSensorData(source1name));
    assertEquals("Storage returned incorrect latest sensor data", data3,
        manager.dbImpl.getLatestNonVirtualSensorData(source1name));
    // When we go through dbManager, this doesn't actually return the latest data available.
    // It returns the latest data point in the cache, and since it found something it doesn't bother
    // checking storage. (Usually they'll be the same, but we fudge it here to prove a point.)
    assertEquals("DbManager returned incorrect latest sensor data", data2,
        manager.getLatestSensorData(source1name));
  }

  /**
   * Creates a SensorData for use in testing, 1 in a series.
   * 
   * @return The freshly created SensorData object.
   * @throws Exception If there are problems converting timestamp string to XMLGregorianCalendar
   */
  @Override
  protected SensorData makeTestSensorData1() throws Exception {
    Properties p = new Properties();
    p.getProperty().add(new Property(SensorData.POWER_GENERATED, "10000.0"));
    p.getProperty().add(new Property(SensorData.ENERGY_GENERATED_TO_DATE, "50.0"));

    return new SensorData(Tstamp.makeTimestamp("2009-07-28T09:00:00.000-10:00"), "JUnit",
        makeTestSource1().toUri(server), p);
  }

  /**
   * Creates a SensorData for use in testing, 2 in a series.
   * 
   * @return The freshly created SensorData object.
   * @throws Exception If there are problems converting timestamp string to XMLGregorianCalendar
   */
  @Override
  protected SensorData makeTestSensorData2() throws Exception {
    Properties p = new Properties();
    p.getProperty().add(new Property(SensorData.POWER_GENERATED, "8000.0"));
    p.getProperty().add(new Property(SensorData.ENERGY_GENERATED_TO_DATE, "90.0"));

    return new SensorData(Tstamp.makeTimestamp("2009-07-28T09:00:30.000-10:00"), "FooTool",
        makeTestSource1().toUri(server), p);
  }

  /**
   * Creates a SensorData for use in testing, 3 in a series.
   * 
   * @return The freshly created SensorData object.
   * @throws Exception If there are problems converting timestamp string to XMLGregorianCalendar
   */
  @Override
  protected SensorData makeTestSensorData3() throws Exception {
    Properties p = new Properties();
    p.getProperty().add(new Property(SensorData.POWER_GENERATED, "9000.0"));
    p.getProperty().add(new Property(SensorData.ENERGY_GENERATED_TO_DATE, "135.0"));

    return new SensorData(Tstamp.makeTimestamp("2009-07-28T09:01:00.000-10:00"), "JUnit",
        makeTestSource1().toUri(server), p);
  }

  /**
   * Creates a SensorData for use in testing, 4 in a series.
   * 
   * @return The freshly created SensorData object.
   * @throws Exception If there are problems converting timestamp string to XMLGregorianCalendar
   */
  protected SensorData makeTestSensorData4() throws Exception {
    Properties p = new Properties();
    p.getProperty().add(new Property(SensorData.POWER_GENERATED, "7000.0"));
    p.getProperty().add(new Property(SensorData.ENERGY_GENERATED_TO_DATE, "170.0"));

    return new SensorData(Tstamp.makeTimestamp("2009-07-28T09:01:30.000-10:00"), "JUnit",
        makeTestSource1().toUri(server), p);
  }

  /**
   * Creates a SensorData for use in testing, 5 in a series.
   * 
   * @return The freshly created SensorData object.
   * @throws Exception If there are problems converting timestamp string to XMLGregorianCalendar
   */
  protected SensorData makeTestSensorData5() throws Exception {
    Properties p = new Properties();
    p.getProperty().add(new Property(SensorData.POWER_GENERATED, "10000.0"));
    p.getProperty().add(new Property(SensorData.ENERGY_GENERATED_TO_DATE, "220.0"));

    return new SensorData(Tstamp.makeTimestamp("2009-07-28T09:02:00.000-10:00"), "JUnit",
        makeTestSource1().toUri(server), p);
  }

  /**
   * Creates a SensorData for use in testing, 1 in a series.
   * 
   * @return The freshly created SensorData object.
   * @throws Exception If there are problems converting timestamp string to XMLGregorianCalendar
   */
  protected SensorData makeTestSensorData6() throws Exception {
    Properties p = new Properties();
    p.getProperty().add(new Property(SensorData.POWER_GENERATED, "10000.0"));
    p.getProperty().add(new Property(SensorData.ENERGY_GENERATED_TO_DATE, "50.0"));

    return new SensorData(Tstamp.makeTimestamp("2009-07-28T09:00:00.000-10:00"), "JUnit",
        makeTestSource2().toUri(server), p);
  }

  /**
   * Creates a SensorData for use in testing, 2 in a series.
   * 
   * @return The freshly created SensorData object.
   * @throws Exception If there are problems converting timestamp string to XMLGregorianCalendar
   */
  protected SensorData makeTestSensorData7() throws Exception {
    Properties p = new Properties();
    p.getProperty().add(new Property(SensorData.POWER_GENERATED, "8000.0"));
    p.getProperty().add(new Property(SensorData.ENERGY_GENERATED_TO_DATE, "90.0"));

    return new SensorData(Tstamp.makeTimestamp("2009-07-28T09:00:30.000-10:00"), "FooTool",
        makeTestSource2().toUri(server), p);
  }

  /**
   * Creates a SensorData for use in testing, 3 in a series.
   * 
   * @return The freshly created SensorData object.
   * @throws Exception If there are problems converting timestamp string to XMLGregorianCalendar
   */
  protected SensorData makeTestSensorData8() throws Exception {
    Properties p = new Properties();
    p.getProperty().add(new Property(SensorData.POWER_GENERATED, "9000.0"));
    p.getProperty().add(new Property(SensorData.ENERGY_GENERATED_TO_DATE, "135.0"));

    return new SensorData(Tstamp.makeTimestamp("2009-07-28T09:01:00.000-10:00"), "JUnit",
        makeTestSource2().toUri(server), p);
  }

  /**
   * Creates a SensorData for use in testing, 4 in a series.
   * 
   * @return The freshly created SensorData object.
   * @throws Exception If there are problems converting timestamp string to XMLGregorianCalendar
   */
  protected SensorData makeTestSensorData9() throws Exception {
    Properties p = new Properties();
    p.getProperty().add(new Property(SensorData.POWER_GENERATED, "7000.0"));
    p.getProperty().add(new Property(SensorData.ENERGY_GENERATED_TO_DATE, "170.0"));

    return new SensorData(Tstamp.makeTimestamp("2009-07-28T09:01:30.000-10:00"), "JUnit",
        makeTestSource2().toUri(server), p);
  }

  /**
   * Creates a SensorData for use in testing, 5 in a series.
   * 
   * @return The freshly created SensorData object.
   * @throws Exception If there are problems converting timestamp string to XMLGregorianCalendar
   */
  protected SensorData makeTestSensorData10() throws Exception {
    Properties p = new Properties();
    p.getProperty().add(new Property(SensorData.POWER_GENERATED, "10000.0"));
    p.getProperty().add(new Property(SensorData.ENERGY_GENERATED_TO_DATE, "220.0"));

    return new SensorData(Tstamp.makeTimestamp("2009-07-28T09:02:00.000-10:00"), "JUnit",
        makeTestSource2().toUri(server), p);
  }
}

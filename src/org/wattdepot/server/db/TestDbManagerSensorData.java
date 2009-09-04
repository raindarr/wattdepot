package org.wattdepot.server.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.wattdepot.resource.sensordata.SensorDataUtils.compareSensorDataRefsToSensorDatas;
import static org.wattdepot.resource.sensordata.SensorDataUtils.sensorDataRefEqualsSensorData;
import java.util.ArrayList;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;
import org.hackystat.utilities.tstamp.Tstamp;
import org.junit.Test;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.resource.sensordata.jaxb.SensorDataRef;
import org.wattdepot.resource.source.jaxb.Source;
import org.wattdepot.resource.user.jaxb.User;

/**
 * Instantiates a DbManager and tests the database methods related to SensorData resources.
 * 
 * @author Robert Brewer
 */
public class TestDbManagerSensorData extends DbManagerTestHelper {

  private static final String REFS_DONT_MATCH_SENSORDATA =
    "SensorDataRefs from getSensorDataIndex do not match input SensorData";

  /** Test Users used by the tests, but never changed. */
  private final User user1 = makeTestUser1(), user2 = makeTestUser2(), user3 = makeTestUser3();

  /** Test Sources used by the tests, but never changed. */
  private final Source source1 = makeTestSource1(), source2 = makeTestSource2(),
      source3 = makeTestSource3();

  /** Test SensorData used by the tests, but never changed. */
  private final SensorData data1 = makeTestSensorData1(), data2 = makeTestSensorData2(),
      data3 = makeTestSensorData3();

  private final XMLGregorianCalendar unknownTimestamp = Tstamp
      .makeTimestamp("2009-07-28T08:00:00.000-10:00");

  /** Make PMD happy. */
  private static final String UNABLE_TO_STORE_DATA = "Unable to store SensorData";

  /** Make PMD happy. */
  private static final String DATA_DOES_NOT_MATCH =
    "Retrieved SensorData does not match original stored SensorData";

  /**
   * Creates the test object, and throws exceptions if needed.
   * 
   * @throws Exception If there is a problem creating test data objects.
   */
  public TestDbManagerSensorData() throws Exception {
    // setup();
  }

  /**
   * Adds test Users and test Sources. Currently the DB level doesn't check that Owners for Sources
   * and SensorData exist, but might in the future.
   */
  private void createTestData() {
    assertTrue("Unable to store a User in DB", manager.storeUser(this.user1));
    assertTrue("Unable to store a User in DB", manager.storeUser(this.user2));
    assertTrue("Unable to store a User in DB", manager.storeUser(this.user3));
    assertTrue("Unable to store a Source in DB", manager.storeSource(this.source1));
    assertTrue("Unable to store a Source in DB", manager.storeSource(this.source2));
    assertTrue("Unable to store a Source in DB", manager.storeSource(this.source3));
  }

  /**
   * Tests the getSensorDataIndex method.
   */
  @Test
  public void testGetSensorDataIndex() {
    // Set up test data
    createTestData();

    // case #1: empty database should have no SensorData
    assertTrue("Freshly created database contains SensorData", manager.getSensorDataIndex(
        this.source1.getName()).getSensorDataRef().isEmpty());

    // case #2: after storing a single SensorData should have SensorDataIndex with one SensorDataRef
    // that matches original SensorData
    assertTrue(UNABLE_TO_STORE_DATA, manager.storeSensorData(this.data1));
    assertSame("getSensorDataIndex returned wrong number of SensorDataRefs", manager
        .getSensorDataIndex(this.source1.getName()).getSensorDataRef().size(), 1);
    assertTrue("getSensorDataIndex didn't return expected SensorDataRef",
        sensorDataRefEqualsSensorData(manager.getSensorDataIndex(this.source1.getName())
            .getSensorDataRef().get(0), this.data1));

    // case #3: after storing three SensorDatas should have SensorDataIndex with three
    // SensorDataRefs that match original SensorDatas
    assertTrue(UNABLE_TO_STORE_DATA, manager.storeSensorData(data2));
    assertTrue(UNABLE_TO_STORE_DATA, manager.storeSensorData(data3));
    assertSame("getSensorData returned wrong number of SensorDataRefs", manager
        .getSensorDataIndex(this.source1.getName()).getSensorDataRef().size(), 3);
    // Now compare the SensorDataRefs to the original SensorData
    List<SensorDataRef> retrievedRefs = manager.getSensorDataIndex(this.source1.getName())
        .getSensorDataRef();
    List<SensorData> origData = new ArrayList<SensorData>();
    origData.add(this.data1);
    origData.add(this.data2);
    origData.add(this.data3);
    assertTrue(REFS_DONT_MATCH_SENSORDATA,
        compareSensorDataRefsToSensorDatas(retrievedRefs, origData));

    // case #4: deleting a SensorData should leave two SensorDataRefs in SensorDataIndex
    assertTrue("Unable to delete data1", manager.deleteSensorData(this.source1.getName(), data1
        .getTimestamp()));
    assertSame("getSensorDataIndex returned wrong number of SensorDataRefs", manager
        .getSensorDataIndex(this.source1.getName()).getSensorDataRef().size(), 2);

    // case #5: retrieving SensorDataIndex for bogus Source name
    assertNull("Found SensorDataIndex for bogus Source name", manager
        .getSensorDataIndex("bogus-source-1"));

    // case #6: retrieving SensorDataIndex for empty Source name
    assertNull("Found SensorDataIndex for empty Source name", manager.getSensorDataIndex(""));

    // case #7: retrieving SensorDataIndex for null Source name
    assertNull("Found SensorDataIndex for empty Source name", manager.getSensorDataIndex(null));
  }

  /**
   * Tests the getSensorDataIndex method with startTime and endTime.
   * 
   * @throws Exception if calendar conversion fails.
   */
  @Test
  public void testGetSensorDataIndexStartEnd() throws Exception {
    // Set up test data
    createTestData();

    // before all three of the test data items
    XMLGregorianCalendar before1 = Tstamp.makeTimestamp("2009-07-28T08:00:00.000-10:00"),
    // between data1 and data2
    between1And2 = Tstamp.makeTimestamp("2009-07-28T09:07:00.000-10:00"),
    // between data2 and data3
    between2And3 = Tstamp.makeTimestamp("2009-07-28T09:23:00.000-10:00"),
    // after all three test data items
    after3 = Tstamp.makeTimestamp("2009-07-28T10:00:00.000-10:00"),
    // Ever later than after3
    moreAfter3 = Tstamp.makeTimestamp("2009-07-29T10:00:00.000-10:00");

    // Add data to source
    assertTrue(UNABLE_TO_STORE_DATA, manager.storeSensorData(this.data1));
    assertTrue(UNABLE_TO_STORE_DATA, manager.storeSensorData(this.data2));
    assertTrue(UNABLE_TO_STORE_DATA, manager.storeSensorData(this.data3));

    // case #1: start time after end time
    assertNull("SensorDataIndex generated for bogus start and end times", manager
        .getSensorDataIndex(this.source1.getName(), after3, before1));

    // case #2: valid range covering no data
    assertTrue("SensorDataIndex generated for period containing no SensorData", manager
        .getSensorDataIndex(this.source1.getName(), after3, moreAfter3).getSensorDataRef()
        .isEmpty());

    // case #3: range covering all three data items
    List<SensorDataRef> retrievedRefs = manager.getSensorDataIndex(this.source1.getName(),
        before1, after3).getSensorDataRef();
    List<SensorData> origData = new ArrayList<SensorData>();
    origData.add(this.data1);
    origData.add(this.data2);
    origData.add(this.data3);
    assertTrue(REFS_DONT_MATCH_SENSORDATA,
        compareSensorDataRefsToSensorDatas(retrievedRefs, origData));

    // case #4: range covering only data1
    assertSame("getSensorDataIndex didn't contain all expected SensorData", manager
        .getSensorDataIndex(this.source1.getName(), before1, between1And2).getSensorDataRef()
        .size(), 1);
    assertTrue("getSensorDataIndex didn't return expected ", sensorDataRefEqualsSensorData(manager
        .getSensorDataIndex(this.source1.getName(), before1, between1And2).getSensorDataRef().get(
            0), this.data1));

    // case #5: range covering only data2
    assertSame("getSensorDataIndex didn't contain all expected SensorData", manager
        .getSensorDataIndex(this.source1.getName(), between1And2, between2And3).getSensorDataRef()
        .size(), 1);
    assertTrue("getSensorDataIndex didn't return expected ", sensorDataRefEqualsSensorData(manager
        .getSensorDataIndex(this.source1.getName(), between1And2, between2And3).getSensorDataRef()
        .get(0), this.data2));

    // case #6: range covering data1 & data2
    retrievedRefs = manager.getSensorDataIndex(this.source1.getName(), before1, between2And3)
        .getSensorDataRef();
    origData.clear();
    origData.add(this.data1);
    origData.add(this.data2);
    assertTrue(REFS_DONT_MATCH_SENSORDATA,
        compareSensorDataRefsToSensorDatas(retrievedRefs, origData));

    // case #7: range covering data2 & data3
    retrievedRefs = manager.getSensorDataIndex(this.source1.getName(), between1And2, after3)
        .getSensorDataRef();
    origData.clear();
    origData.add(this.data2);
    origData.add(this.data3);
    assertTrue(REFS_DONT_MATCH_SENSORDATA,
        compareSensorDataRefsToSensorDatas(retrievedRefs, origData));

    // case #8: deleting data2 should leave data1 & data3 if interval covers all three
    assertTrue("Unable to delete data2", manager.deleteSensorData(this.source1.getName(), data2
        .getTimestamp()));
    retrievedRefs = manager.getSensorDataIndex(this.source1.getName(), before1, after3)
        .getSensorDataRef();
    origData.clear();
    origData.add(this.data1);
    origData.add(this.data3);
    assertTrue(REFS_DONT_MATCH_SENSORDATA,
        compareSensorDataRefsToSensorDatas(retrievedRefs, origData));

    // case #9: retrieving SensorDataIndex for bogus Source name
    assertNull("Found SensorDataIndex for bogus Source name", manager.getSensorDataIndex(
        "bogus-source-2", before1, after3));

    // case #10: retrieving SensorDataIndex for empty Source name
    assertNull("Found SensorDataIndex for empty Source name", manager.getSensorDataIndex("",
        before1, after3));

    // case #11: retrieving SensorDataIndex for null Source name
    assertNull("Found SensorDataIndex for null Source name", manager.getSensorDataIndex(null,
        before1, after3));

    // case #12: retrieving SensorDataIndex for null startTime
    assertNull("Found SensorDataIndex for null startTime", manager.getSensorDataIndex(
        this.source1.getName(), null, after3));

    // case #13: retrieving SensorDataIndex for null endTime
    assertNull("Found SensorDataIndex for null endTime", manager.getSensorDataIndex(
        this.source1.getName(), before1, null));
  }

  /**
   * Tests the getSensorData method.
   * 
   * @throws Exception if timestamp creation throws exception.
   */
  @Test
  public void testGetSensorData() throws Exception {
    // Add Users that own test Sources.
    createTestData();

    // case #1: retrieve SensorData from empty DB
    assertNull("Able to retrieve SensorData from empty DB", manager.getSensorData(this.source1
        .getName(), this.data1.getTimestamp()));

    // case #2: retrieve stored SensorData
    // Add data to source
    assertTrue(UNABLE_TO_STORE_DATA, manager.storeSensorData(this.data1));
    assertEquals(DATA_DOES_NOT_MATCH, this.data1, manager.getSensorData(source1.getName(),
        this.data1.getTimestamp()));

    // case #3: store second SensorData, verify retrieval
    assertTrue(UNABLE_TO_STORE_DATA, manager.storeSensorData(this.data2));
    assertEquals(DATA_DOES_NOT_MATCH, this.data2, manager.getSensorData(source1.getName(),
        this.data2.getTimestamp()));

    // case #4: retrieving SensorData for bogus Source name
    assertNull("Found SensorData for bogus Source name", manager.getSensorData("bogus-source-3",
        this.data1.getTimestamp()));

    // case #5: retrieving SensorData for empty Source name
    assertNull("Found SensorData for empty Source name", manager.getSensorData("", this.data1
        .getTimestamp()));

    // case #6: retrieving SensorData for null Source name
    assertNull("Found SensorData for null Source name", manager.getSensorData(null, this.data1
        .getTimestamp()));

    // case #7: retrieving SensorData for timestamp that doesn't correspond to any stored data
    assertNull("Found SensorData for unknown timestamp", manager.getSensorData(this.source1
        .getName(), this.unknownTimestamp));

    // case #8: retrieving SensorData for null timestamp
    assertNull("Found SensorData for null timestamp", manager.getSensorData(this.source1
        .getName(), null));
  }

  /**
   * Tests the hasSensorData method.
   * 
   * @throws Exception if timestamp creation throws exception.
   */
  @Test
  public void testHasSensorData() throws Exception {
    // Add Users that own test Sources.
    createTestData();

    // case #1: retrieve SensorData from empty DB
    assertFalse("Able to retrieve SensorData from empty DB", manager.hasSensorData(this.source1
        .getName(), this.data1.getTimestamp()));

    // case #2: retrieve stored SensorData
    // Add data to source
    assertTrue(UNABLE_TO_STORE_DATA, manager.storeSensorData(this.data1));
    assertTrue(DATA_DOES_NOT_MATCH, manager.hasSensorData(source1.getName(), this.data1
        .getTimestamp()));

    // case #3: store second SensorData, verify retrieval
    assertTrue(UNABLE_TO_STORE_DATA, manager.storeSensorData(this.data2));
    assertTrue(DATA_DOES_NOT_MATCH, manager.hasSensorData(source1.getName(), this.data2
        .getTimestamp()));

    // case #4: retrieving SensorData for bogus Source name
    assertFalse("Found SensorData for bogus Source name", manager.hasSensorData("bogus-source-4",
        this.data1.getTimestamp()));

    // case #5: retrieving SensorData for empty Source name
    assertFalse("Found SensorData for empty Source name", manager.hasSensorData("", this.data1
        .getTimestamp()));

    // case #6: retrieving SensorData for null Source name
    assertFalse("Found SensorData for null Source name", manager.hasSensorData(null, this.data1
        .getTimestamp()));

    // case #7: retrieving SensorData for timestamp that doesn't correspond to any stored data
    assertFalse("Found SensorData for unknown timestamp", manager.hasSensorData(this.source1
        .getName(), this.unknownTimestamp));

    // case #8: retrieving SensorData for null timestamp
    assertFalse("Found SensorData for null timestamp", manager.hasSensorData(this.source1
        .getName(), null));
  }

  /**
   * Tests the storeSensorData method.
   */
  @Test
  public void testStoreSensorData() {
    // Add test data.
    createTestData();

    // case #1: store and retrieve SensorData
    assertTrue(UNABLE_TO_STORE_DATA, manager.storeSensorData(this.data1));
    assertEquals(DATA_DOES_NOT_MATCH, this.data1, manager.getSensorData(source1.getName(),
        this.data1.getTimestamp()));

    // case #2: attempt to overwrite existing SensorData
    assertFalse("Able to overwrite SensorData", manager.storeSensorData(this.data1));

    // case #3: store second SensorData and verify retrieval
    assertTrue(UNABLE_TO_STORE_DATA, manager.storeSensorData(this.data2));
    assertEquals(DATA_DOES_NOT_MATCH, this.data2, manager.getSensorData(source1.getName(),
        this.data2.getTimestamp()));

    // case #4: store null SensorData
    assertFalse("Able to store null SensorData", manager.storeSensorData(null));
  }

  /**
   * Tests the deleteSensorData method that takes a timestamp argument.
   * 
   * @throws Exception if timestamp creation throws exception.
   */
  @Test
  public void testDeleteSensorDataTimestamp() throws Exception {
    // Add test data
    createTestData();

    // case #1: delete SensorData from empty database
    assertFalse("Able to delete SensorData from empty DB", manager.deleteSensorData(this.source1
        .getName(), this.data1.getTimestamp()));

    // case #2: delete stored SensorData
    assertTrue(UNABLE_TO_STORE_DATA, manager.storeSensorData(this.data1));
    assertEquals(DATA_DOES_NOT_MATCH, this.data1, manager.getSensorData(this.source1.getName(),
        this.data1.getTimestamp()));
    assertTrue("Unable to delete data1", manager.deleteSensorData(this.source1.getName(),
        this.data1.getTimestamp()));
    assertNull("Able to retrieve deleted SensorData", manager.getSensorData(
        this.source1.getName(), this.data1.getTimestamp()));

    // case #3: delete deleted SensorData
    assertTrue(UNABLE_TO_STORE_DATA, manager.storeSensorData(this.data2));
    assertTrue("Unable to delete data2", manager.deleteSensorData(this.source1.getName(),
        this.data2.getTimestamp()));
    assertFalse("Able to delete data2 a second time", manager.deleteSensorData(this.source1
        .getName(), this.data2.getTimestamp()));

    // case #4: delete SensorData with unknown Source name
    assertFalse("Able to delete ficticiously-named Source", manager.deleteSensorData(
        "bogus-source2", this.data1.getTimestamp()));

    // case #5: delete SensorData with empty Source name
    assertFalse("Able to delete empty Source name", manager.deleteSensorData("", this.data1
        .getTimestamp()));

    // case #6: delete SensorData with null Source name
    assertFalse("Able to delete null Source name", manager.deleteSensorData(null, this.data1
        .getTimestamp()));

    // case #7: deleting SensorData for timestamp that doesn't correspond to any stored data
    assertFalse("Deleted SensorData for unknown timestamp", manager.deleteSensorData(this.source1
        .getName(), this.unknownTimestamp));

    // case #8: deleting SensorData for null timestamp
    assertFalse("Deleted SensorData for null timestamp", manager.deleteSensorData(this.source1
        .getName(), null));

    // case #9: no more SensorData after all SensorData has been deleted
    assertTrue("After deleting all known SensorData for Source, SensorData remains in DB", manager
        .getSensorDataIndex(this.source1.getName()).getSensorDataRef().isEmpty());
  }

  /**
   * Tests the deleteSensorData method.
   */
  @Test
  public void testDeleteSensorData() {
    // Add test data
    createTestData();

    // case #1: delete SensorData from empty database
    assertFalse("Able to delete SensorData from empty DB", manager.deleteSensorData(this.source1
        .getName()));

    // case #2: delete stored SensorData
    assertTrue(UNABLE_TO_STORE_DATA, manager.storeSensorData(this.data1));
    assertEquals(DATA_DOES_NOT_MATCH, this.data1, manager.getSensorData(this.source1.getName(),
        this.data1.getTimestamp()));
    assertTrue("Unable to delete data1", manager.deleteSensorData(this.source1.getName()));
    assertNull("Able to retrieve deleted SensorData", manager.getSensorData(
        this.source1.getName(), this.data1.getTimestamp()));

    // case #3: delete deleted SensorData
    assertTrue(UNABLE_TO_STORE_DATA, manager.storeSensorData(this.data2));
    assertTrue("Unable to delete data2", manager.deleteSensorData(this.source1.getName()));
    assertFalse("Able to delete data2 a second time", manager.deleteSensorData(this.source1
        .getName()));

    // case #4: delete SensorData with unknown Source name
    assertFalse("Able to delete ficticiously-named Source", manager
        .deleteSensorData("bogus-source-5"));

    // case #5: delete SensorData with empty Source name
    assertFalse("Able to delete empty Source name", manager.deleteSensorData(""));

    // case #6: delete SensorData with null Source name
    assertFalse("Able to delete null Source name", manager.deleteSensorData(null));

    // case #9: no more SensorData after all SensorData has been deleted
    assertTrue("After deleting all known SensorData for Source, SensorData remains in DB", manager
        .getSensorDataIndex(this.source1.getName()).getSensorDataRef().isEmpty());
  }
}

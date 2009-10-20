package org.wattdepot.server.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.wattdepot.resource.source.SourceUtils.sourceRefEqualsSource;
import java.util.ArrayList;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;
import org.hackystat.utilities.tstamp.Tstamp;
import org.junit.Before;
import org.junit.Test;
import org.wattdepot.resource.sensordata.SensorDataUtils;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.resource.source.SourceUtils;
import org.wattdepot.resource.source.jaxb.Source;
import org.wattdepot.resource.source.jaxb.SourceRef;
import org.wattdepot.resource.source.summary.jaxb.SourceSummary;
import org.wattdepot.resource.user.jaxb.User;

/**
 * Instantiates a DbManager and tests the database methods related to Source resources.
 * 
 * @author Robert Brewer
 */
public class TestDbManagerSources extends DbManagerTestHelper {

  /** Test Users used by the tests, but never changed. */
  private final User user1 = makeTestUser1(), user2 = makeTestUser2(), user3 = makeTestUser3();

  /** Test Sources used by the tests, but never changed. */
  private final Source source1 = makeTestSource1(), source2 = makeTestSource2(),
      source3 = makeTestSource3();

  /** Make PMD happy. */
  private static final String UNABLE_TO_STORE_SOURCE = "Unable to store a Source in DB";

  /** Make PMD happy. */
  private static final String SOURCE_DOES_NOT_MATCH =
      "Retrieved Source does not match original stored Source";

  /** Make PMD happy. */
  private static final String UNABLE_TO_STORE_DATA = "Unable to store SensorData";

  /**
   * Adds Users that own test Sources. Currently the DB level doesn't check that Owners for Sources
   * exist, but might in the future.
   */
  private void storeTestUsers() {
    assertTrue("Unable to store a User in DB", manager.storeUser(this.user1));
    assertTrue("Unable to store a User in DB", manager.storeUser(this.user2));
    assertTrue("Unable to store a User in DB", manager.storeUser(this.user3));
  }

  /**
   * We are temporarily creating some default sources for a demo, which throws off tests that expect
   * a fresh database to be empty. To solve this, before the Source tests run we will delete the
   * default sources.
   */
  @Before
  public void deleteDefaultSources() {
    assertTrue("Unable to delete default public source", this.manager
        .deleteSource(DbManager.defaultPublicSource));
    assertTrue("Unable to delete default private source", this.manager
        .deleteSource(DbManager.defaultPrivateSource));
    assertTrue("Unable to delete default virtual source", this.manager
        .deleteSource(DbManager.defaultVirtualSource));
  }

  /**
   * Tests the getSources method.
   */
  @Test
  public void testGetSources() {
    // Test cases: empty database should have no Sources, after storing a single Source should
    // have SourceIndex with one SourceRef that matches Source, after storing three Sources should
    // have SourceIndex with three SourceRefs that match Sources, after deleting a Source should
    // have two SourceRefs in SourceIndex.

    // Add Users that own test Sources.
    storeTestUsers();

    // case #1: empty database should have no Sources
    assertTrue("Freshly created database contains Sources", manager.getSources().getSourceRef()
        .isEmpty());

    // case #2: after storing a single Source should have SourceIndex with one SourceRef that
    // matches Source
    assertTrue(UNABLE_TO_STORE_SOURCE, manager.storeSource(this.source1));
    assertSame("getSources returned wrong number of SourceRefs", manager.getSources()
        .getSourceRef().size(), 1);
    assertTrue("getSources didn't return expected SourceRef", sourceRefEqualsSource(manager
        .getSources().getSourceRef().get(0), this.source1));

    // case #3:after storing three Sources should have SourceIndex with three SourceRefs that match
    // Sources
    assertTrue(UNABLE_TO_STORE_SOURCE, manager.storeSource(this.source2));
    assertTrue(UNABLE_TO_STORE_SOURCE, manager.storeSource(this.source3));
    assertSame("getSources returned wrong number of SourceRefs", manager.getSources()
        .getSourceRef().size(), 3);
    // Now compare the SourceRefs to the original Sources
    List<SourceRef> retrievedRefs = manager.getSources().getSourceRef();
    List<Source> origSources = new ArrayList<Source>();
    origSources.add(this.source1);
    origSources.add(this.source2);
    origSources.add(this.source3);
    for (SourceRef ref : retrievedRefs) {
      int found = 0;
      for (Source source : origSources) {
        if (sourceRefEqualsSource(ref, source)) {
          found++;
        }
      }
      assertSame("SourceRefs from getSources do not match input Sources", found, 1);
    }

    // TODO Should test that SourceRefs come in sorted order

    // case #4: deleting a Source should leave two SourceRefs in SourceIndex
    assertTrue("Unable to delete source1", manager.deleteSource(this.source1.getName()));
    assertSame("getSources returned wrong number of SourceRefs", manager.getSources()
        .getSourceRef().size(), 2);
  }

  /**
   * Tests the getSource method.
   */
  @Test
  public void testGetSource() {
    // Test cases: retrieve Source from empty DB, retrieve stored Source, retrieve unknown Source
    // name, store 3 Sources and verify retrieval, retrieve empty Source name, retrieve null Source
    // name

    // Add Users that own test Sources.
    storeTestUsers();

    // case #1: retrieve Source from empty DB
    assertNull("Able to retrieve Source from empty DB", manager.getSource(this.source1.getName()));

    // case #2: retrieve stored Source
    assertTrue(UNABLE_TO_STORE_SOURCE, manager.storeSource(this.source1));
    assertEquals(SOURCE_DOES_NOT_MATCH, source1, manager.getSource(source1.getName()));

    // case #3: retrieve unknown Source name
    assertNull("Able to retrieve ficticiously-named Source", manager.getSource("bogus-source"));

    // case #4: store 3 Sources and verify retrieval
    assertTrue(UNABLE_TO_STORE_SOURCE, manager.storeSource(source2));
    assertTrue(UNABLE_TO_STORE_SOURCE, manager.storeSource(source3));
    assertEquals(SOURCE_DOES_NOT_MATCH, source1, manager.getSource(source1.getName()));
    assertEquals(SOURCE_DOES_NOT_MATCH, source2, manager.getSource(source2.getName()));
    assertEquals(SOURCE_DOES_NOT_MATCH, source3, manager.getSource(source3.getName()));

    // case #5: retrieve empty Source name
    assertNull("Able to retrieve empty-named Source", manager.getSource(""));

    // case #6: retrieve null Source name
    assertNull("Able to retrieve from null Source", manager.getSource(null));
  }

  /**
   * Tests the getSourceSummary method.
   * 
   * @throws Exception if there are problems creating test data.
   */
  @Test
  public void testGetSourceSummary() throws Exception {
    // Add Users that own test Sources.
    storeTestUsers();

    assertTrue(UNABLE_TO_STORE_SOURCE, manager.storeSource(this.source1));
    SensorData data1 = makeTestSensorData1(), data2 = makeTestSensorData2(), data3 =
        makeTestSensorData3();

    assertTrue(UNABLE_TO_STORE_DATA, manager.storeSensorData(data1));
    assertTrue(UNABLE_TO_STORE_DATA, manager.storeSensorData(data2));
    assertTrue(UNABLE_TO_STORE_DATA, manager.storeSensorData(data3));

    // retrieve summary for stored Source
    SourceSummary expectedSummary = new SourceSummary();
    expectedSummary.setHref(SourceUtils.sourceToUri(this.source1, server));
    expectedSummary.setFirstSensorData(data1.getTimestamp());
    expectedSummary.setLastSensorData(data3.getTimestamp());
    expectedSummary.setTotalSensorDatas(3);
    SourceSummary retreivedSummary = manager.getSourceSummary(this.source1.getName());
    assertEquals("SourceSummary does not match expected value", expectedSummary, retreivedSummary);

    // retrieve summary for virtual source that contains two real sources with data
    XMLGregorianCalendar earlyTimestamp = Tstamp.makeTimestamp("2009-07-27T09:00:00.000-10:00");
    XMLGregorianCalendar lateTimestamp = Tstamp.makeTimestamp("2009-07-30T09:00:00.000-10:00");

    assertTrue(UNABLE_TO_STORE_SOURCE, manager.storeSource(this.source2));
    assertTrue(UNABLE_TO_STORE_SOURCE, manager.storeSource(this.source3));
    SensorData data4 =
        SensorDataUtils.makeSensorData(earlyTimestamp, "JUnit", SourceUtils.sourceToUri(
            this.source2, server), null);
    SensorData data5 =
        SensorDataUtils.makeSensorData(lateTimestamp, "JUnit", SourceUtils.sourceToUri(
            this.source2, server), null);
    assertTrue(UNABLE_TO_STORE_DATA, manager.storeSensorData(data4));
    assertTrue(UNABLE_TO_STORE_DATA, manager.storeSensorData(data5));
    retreivedSummary = manager.getSourceSummary(this.source3.getName());
    assertEquals("Number of sensordata in summary for virtual source is wrong", 5, retreivedSummary
        .getTotalSensorDatas());
    assertEquals("First sensordata in summary for virtual source is wrong", earlyTimestamp,
        retreivedSummary.getFirstSensorData());
    assertEquals("Last sensordata in summary for virtual source is wrong", lateTimestamp,
        retreivedSummary.getLastSensorData());

    // retrieve summary for unknown Source name
    assertNull("Able to retrieve ficticiously-named Source", manager
        .getSourceSummary("bogus-source"));

    // retrieve summary for empty Source name
    assertNull("Able to retrieve empty-named Source", manager.getSourceSummary(""));

    // retrieve summary for null Source name
    assertNull("Able to retrieve from null Source", manager.getSourceSummary(null));
  }

  /**
   * Tests the storeSource method.
   */
  @Test
  public void testStoreSource() {
    // Test cases: store and retrieve Source, attempt to overwrite existing Source,
    // store 2 Sources and verify retrieval, store null Source.

    // Add Users that own test Sources.
    storeTestUsers();

    // case #1: store and retrieve Source
    assertTrue(UNABLE_TO_STORE_SOURCE, manager.storeSource(this.source1));
    assertEquals(SOURCE_DOES_NOT_MATCH, source1, manager.getSource(source1.getName()));

    // case #2: attempt to overwrite existing Source
    assertFalse("Overwriting Source succeeded, but should fail", manager.storeSource(source1));

    // case #3: store 2 Sources and verify retrieval
    assertTrue(UNABLE_TO_STORE_SOURCE, manager.storeSource(source2));
    assertEquals(SOURCE_DOES_NOT_MATCH, source1, manager.getSource(source1.getName()));
    assertEquals(SOURCE_DOES_NOT_MATCH, source2, manager.getSource(source2.getName()));

    // case #4: store null Source
    assertFalse("Storing null Source succeeded", manager.storeSource(null));
  }

  /**
   * Tests the deleteSource method.
   */
  @Test
  public void testDeleteSource() {
    // Test cases: delete Source from empty database, delete stored Source, delete deleted Source,
    // delete unknown Source name, delete empty Source name, delete null Source name.

    // Add Users that own test Sources.
    storeTestUsers();

    // case #1: delete Source from empty database
    assertFalse("Able to delete Source from empty DB", manager.deleteSource(source1.getName()));

    // case #2: delete stored Source
    assertTrue(UNABLE_TO_STORE_SOURCE, manager.storeSource(source1));
    assertEquals(SOURCE_DOES_NOT_MATCH, source1, manager.getSource(source1.getName()));
    assertTrue("Unable to delete source1", manager.deleteSource(source1.getName()));
    assertNull("Able to retrieve deleted Source", manager.getSource(source1.getName()));

    // case #3: delete deleted Source
    assertTrue(UNABLE_TO_STORE_SOURCE, manager.storeSource(source2));
    assertTrue("Unable to delete source2", manager.deleteSource(source2.getName()));
    assertFalse("Able to delete source2 a second time", manager.deleteSource(source2.getName()));

    // case #4: delete unknown Source name
    assertFalse("Able to delete ficticiously-named Source", manager.deleteSource("bogus-source2"));

    // case #5: delete empty Source name
    assertFalse("Able to delete empty Source name", manager.deleteSource(""));

    // case #6: delete null Source name
    assertFalse("Able to delete null Source name", manager.deleteSource(null));

    // case #7: no more Sources after all Sources have been deleted
    assertTrue("After deleting all known Sources, Sources remain in DB", manager.getSources()
        .getSourceRef().isEmpty());

    // TODO add case to check that sensor data for source is deleted when source is deleted
  }
}

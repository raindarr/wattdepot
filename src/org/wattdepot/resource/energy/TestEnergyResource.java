package org.wattdepot.resource.energy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.wattdepot.resource.source.SourceUtils.sourceToUri;
import javax.xml.datatype.XMLGregorianCalendar;
import org.junit.Before;
import org.junit.Test;
import org.wattdepot.client.BadXmlException;
import org.wattdepot.client.WattDepotClient;
import org.wattdepot.resource.sensordata.SensorDataStraddle;
import org.wattdepot.resource.sensordata.SensorDataUtils;
import org.wattdepot.resource.sensordata.jaxb.Property;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.server.db.DbManager;
import org.wattdepot.test.ServerTestHelper;
import org.wattdepot.util.tstamp.Tstamp;

/**
 * Tests the Energy resource API at the HTTP level using WattDepotClient.
 * 
 * @author Robert Brewer
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class TestEnergyResource extends ServerTestHelper {

  /**
   * Creates a fresh DbManager for each test. This might prove too expensive for some
   * DbImplementations, but we'll cross that bridge when we get there.
   */
  @Before
  public void makeDb() {
    DbManager manager =
        new DbManager(server, "org.wattdepot.server.db.memory.MemoryStorageImplementation", true);
    // Need to create default data for each fresh DbManager
    assertTrue("Unable to create default data", manager.createDefaultData());
    server.getContext().getAttributes().put("DbManager", manager);
  }

  // TODO Skipping authentication tests for now
  // /**
  // * Tests retrieval of all SensorData from a Source. Type: public Source with no credentials.
  // *
  // * @throws WattDepotClientException If problems are encountered
  // */
  // @Test
  // public void testPowerPublicWithNoCredentials() throws WattDepotClientException {
  // WattDepotClient client = new WattDepotClient(getHostName());
  // assertNotNull(MISSING_SENSORDATAREFS, client.getSensorDataIndex(DbManager.defaultPublicSource)
  // .getSensorDataRef());
  // }
  //
  // /**
  // * Tests retrieval of all SensorData from a Source. Type: public Source with invalid
  // credentials.
  // *
  // * @throws WattDepotClientException If problems are encountered
  // */
  // @Test(expected = NotAuthorizedException.class)
  // public void testFullIndexPublicBadAuth() throws WattDepotClientException {
  // // Shouldn't authenticate with invalid credentials.
  // WattDepotClient client = new WattDepotClient(getHostName(), adminEmail, "foo");
  // client.getSensorDataIndex(DbManager.defaultPublicSource);
  // }
  //
  // /**
  // * Tests retrieval of all SensorData from a Source. Type: public Source with valid admin
  // * credentials.
  // *
  // * @throws WattDepotClientException If problems are encountered
  // */
  // @Test
  // public void testFullIndexPublicWithAdminCredentials() throws WattDepotClientException {
  // WattDepotClient client = new WattDepotClient(getHostName(), adminEmail, adminPassword);
  // assertNotNull(MISSING_SENSORDATAREFS, client.getSensorDataIndex(DbManager.defaultPublicSource)
  // .getSensorDataRef());
  // }
  //
  // /**
  // * Tests retrieval of all SensorData from a Source. Type: public Source with valid owner
  // * credentials.
  // *
  // * @throws WattDepotClientException If problems are encountered
  // */
  // @Test
  // public void testFullIndexPublicWithOwnerCredentials() throws WattDepotClientException {
  // WattDepotClient client =
  // new WattDepotClient(getHostName(), DbManager.defaultOwnerUsername,
  // DbManager.defaultOwnerPassword);
  // assertNotNull(MISSING_SENSORDATAREFS, client.getSensorDataIndex(DbManager.defaultPublicSource)
  // .getSensorDataRef());
  // }
  //
  // /**
  // * Tests retrieval of all SensorData from a Source. Type: public Source with valid non-owner
  // * credentials.
  // *
  // * @throws WattDepotClientException If problems are encountered
  // */
  // @Test
  // public void testFullIndexPublicWithNonOwnerCredentials() throws WattDepotClientException {
  // WattDepotClient client =
  // new WattDepotClient(getHostName(), DbManager.defaultNonOwnerUsername,
  // DbManager.defaultNonOwnerPassword);
  // assertNotNull(MISSING_SENSORDATAREFS, client.getSensorDataIndex(DbManager.defaultPublicSource)
  // .getSensorDataRef());
  // }
  //
  // /**
  // * Tests retrieval of all SensorData from a Source. Type: private Source with no credentials.
  // *
  // * @throws WattDepotClientException If problems are encountered
  // */
  // @Test(expected = NotAuthorizedException.class)
  // public void testFullIndexPrivateWithNoCredentials() throws WattDepotClientException {
  // WattDepotClient client = new WattDepotClient(getHostName());
  // client.getSensorDataIndex(DbManager.defaultPrivateSource);
  // }
  //
  // /**
  // * Tests retrieval of all SensorData from a Source. Type: private Source with invalid
  // credentials.
  // *
  // * @throws WattDepotClientException If problems are encountered
  // */
  // @Test(expected = NotAuthorizedException.class)
  // public void testFullIndexPrivateBadAuth() throws WattDepotClientException {
  // // Shouldn't authenticate with no username or password
  // WattDepotClient client = new WattDepotClient(getHostName(), adminEmail, "wrong-password");
  // client.getSensorDataIndex(DbManager.defaultPrivateSource);
  // }
  //
  // /**
  // * Tests retrieval of all SensorData from a Source. Type: private Source with admin credentials.
  // *
  // * @throws WattDepotClientException If problems are encountered
  // */
  // @Test
  // public void testFullIndexPrivateAdminAuth() throws WattDepotClientException {
  // // Shouldn't authenticate with no username or password
  // WattDepotClient client = new WattDepotClient(getHostName(), adminEmail, adminPassword);
  // assertNotNull(MISSING_SENSORDATAREFS,
  // client.getSensorDataIndex(DbManager.defaultPrivateSource));
  // }
  //
  // /**
  // * Tests retrieval of all SensorData from a Source. Type: private Source with owner credentials.
  // *
  // * @throws WattDepotClientException If problems are encountered
  // */
  // @Test
  // public void testFullIndexPrivateOwnerAuth() throws WattDepotClientException {
  // // Shouldn't authenticate with no username or password
  // WattDepotClient client =
  // new WattDepotClient(getHostName(), DbManager.defaultOwnerUsername,
  // DbManager.defaultOwnerPassword);
  // assertNotNull(MISSING_SENSORDATAREFS,
  // client.getSensorDataIndex(DbManager.defaultPrivateSource));
  // }
  //
  // /**
  // * Tests retrieval of all SensorData from a Source. Type: private Source with non-owner
  // * credentials.
  // *
  // * @throws WattDepotClientException If problems are encountered
  // */
  // @Test(expected = NotAuthorizedException.class)
  // public void testFullIndexPrivateNonOwnerAuth() throws WattDepotClientException {
  // // Shouldn't authenticate with no username or password
  // WattDepotClient client =
  // new WattDepotClient(getHostName(), DbManager.defaultNonOwnerUsername,
  // DbManager.defaultNonOwnerPassword);
  // client.getSensorDataIndex(DbManager.defaultPrivateSource);
  // }
  //
  // /**
  // * Tests retrieval of all SensorData from a Source. Type: unknown Source name with no
  // credentials.
  // *
  // * @throws WattDepotClientException If problems are encountered
  // */
  // @Test(expected = ResourceNotFoundException.class)
  // public void testFullIndexBadSourceNameAnon() throws WattDepotClientException {
  // // Shouldn't authenticate with no username or password
  // WattDepotClient client = new WattDepotClient(getHostName());
  // client.getSensorDataIndex("bogus-source-name");
  // }
  //
  // /**
  // * Tests retrieval of all SensorData from a Source. Type: unknown Source name with valid
  // * credentials.
  // *
  // * @throws WattDepotClientException If problems are encountered
  // */
  // @Test(expected = ResourceNotFoundException.class)
  // public void testFullIndexBadSourceNameAuth() throws WattDepotClientException {
  // // Shouldn't authenticate with no username or password
  // WattDepotClient client = new WattDepotClient(getHostName(), adminEmail, adminPassword);
  // client.getSensorDataIndex("bogus-source-name");
  // }
  //
  // /**
  // * Tests that a Source starts with no SensorData. Type: public Source with valid owner
  // * credentials.
  // *
  // * @throws WattDepotClientException If problems are encountered
  // */
  // @Test
  // public void testFullIndexStartsEmpty() throws WattDepotClientException {
  // WattDepotClient client =
  // new WattDepotClient(getHostName(), DbManager.defaultOwnerUsername,
  // DbManager.defaultOwnerPassword);
  // assertTrue("Fresh DB contains SensorData", client.getSensorDataIndex(
  // DbManager.defaultPublicSource).getSensorDataRef().isEmpty());
  // }
  //
  // /**
  // * Tests that after storing SensorData to a Source, the SensorDataIndex corresponds to the data
  // * that has been stored. Type: public Source with valid owner credentials.
  // *
  // * @throws Exception If stuff goes wrong.
  // */
  // @Test
  // public void testFullIndexAfterStores() throws Exception {
  // WattDepotClient client =
  // new WattDepotClient(getHostName(), DbManager.defaultOwnerUsername,
  // DbManager.defaultOwnerPassword);
  // SensorData data1 = makeTestSensorData1(), data2 = makeTestSensorData2(), data3 =
  // makeTestSensorData3();
  // assertTrue(DATA_STORE_FAILED, client.storeSensorData(data1));
  // List<SensorDataRef> index =
  // client.getSensorDataIndex(DbManager.defaultPublicSource).getSensorDataRef();
  // assertEquals("Wrong number of SensorDataRefs after store", 1, index.size());
  // assertTrue("getSensorDataIndex didn't return expected SensorDataRef",
  // sensorDataRefEqualsSensorData(index.get(0), data1));
  // assertTrue(DATA_STORE_FAILED, client.storeSensorData(data2));
  // index = client.getSensorDataIndex(DbManager.defaultPublicSource).getSensorDataRef();
  // assertEquals("Wrong number of SensorDataRefs after store", 2, index.size());
  // List<SensorData> origData = new ArrayList<SensorData>();
  // origData.add(data1);
  // origData.add(data2);
  // assertTrue("getSensorDataIndex didn't return expected SensorDataRefs",
  // compareSensorDataRefsToSensorDatas(index, origData));
  // assertTrue(DATA_STORE_FAILED, client.storeSensorData(data3));
  // index = client.getSensorDataIndex(DbManager.defaultPublicSource).getSensorDataRef();
  // assertEquals("Wrong number of SensorDataRefs after store", 3, index.size());
  // origData.add(data3);
  // assertTrue("getSensorDataIndex didn't return expected SensorDataRefs",
  // compareSensorDataRefsToSensorDatas(index, origData));
  // }

  // Tests for GET {host}/sources/{source}/power/{timestamp}

  /**
   * Tests the energy resource on a non-virtual source.
   * 
   * @throws Exception If there are problems creating timestamps, or if the client has problems.
   */
  @Test
  public void testGetEnergy() throws Exception {
    WattDepotClient client =
        new WattDepotClient(getHostName(), DbManager.defaultOwnerUsername,
            DbManager.defaultOwnerPassword);

    XMLGregorianCalendar beforeTime, afterTime, timestamp1, timestamp2;
    SensorData beforeData, afterData;
    String source = sourceToUri(DbManager.defaultPublicSource, server);
    String sourceName = DbManager.defaultPublicSource;

    // timestamp = range for flat power, getEnergy should just return simple energy value
    beforeTime = Tstamp.makeTimestamp("2009-07-28T08:00:00.000-10:00");
    beforeData = SensorDataStraddle.makePowerSensorData(beforeTime, source, 100, 0, false);
    client.storeSensorData(beforeData);
    afterTime = Tstamp.makeTimestamp("2009-07-28T09:00:00.000-10:00");
    afterData = SensorDataStraddle.makePowerSensorData(afterTime, source, 100, 0, false);
    client.storeSensorData(afterData);
    assertEquals("getEnergy on degenerate range with default interval gave wrong value", 100,
        client.getEnergyGenerated(sourceName, beforeTime, afterTime, 0), 0.01);
    assertEquals("getEnergy on degenerate range with 2 minute interval gave wrong value", 100,
        client.getEnergyGenerated(sourceName, beforeTime, afterTime, 2), 0.01);
    assertEquals("getEnergy on degenerate range with 5 minute interval gave wrong value", 100,
        client.getEnergyGenerated(sourceName, beforeTime, afterTime, 5), 0.01);
    assertEquals("getEnergy on degenerate range with 5 minute interval gave wrong value", 100,
        client.getEnergyGenerated(sourceName, beforeTime, afterTime, 30), 0.01);
    assertEquals("getEnergy on degenerate range with 5 minute interval gave wrong value", 100,
        client.getEnergyGenerated(sourceName, beforeTime, afterTime, 29), 0.01);
    try {
      client.getEnergyGenerated(sourceName, beforeTime, afterTime, 61);
      fail("getEnergy worked with interval longer than range");
    }
    catch (BadXmlException e) { // NOPMD
      // Expected in this case
    }
    client.deleteSensorData(sourceName, beforeData.getTimestamp());
    client.deleteSensorData(sourceName, afterData.getTimestamp());

    // slope is 2 (100 W difference in 1 hour)
    beforeTime = Tstamp.makeTimestamp("2009-07-28T08:00:00.000-10:00");
    beforeData = SensorDataStraddle.makePowerSensorData(beforeTime, source, 100, 0, false);
    afterTime = Tstamp.makeTimestamp("2009-07-28T09:00:00.000-10:00");
    afterData = SensorDataStraddle.makePowerSensorData(afterTime, source, 200, 0, false);
    client.storeSensorData(beforeData);
    client.storeSensorData(afterData);
    assertEquals("getEnergy on degenerate range with default interval gave wrong value", 150,
        client.getEnergyGenerated(sourceName, beforeTime, afterTime, 0), 0.01);
    assertEquals("getEnergy on degenerate range with default interval gave wrong value", 150,
        client.getEnergyGenerated(sourceName, beforeTime, afterTime, 5), 0.01);
    Property interpolatedProp = SensorDataUtils.makeSensorDataProperty("interpolated", "true");
    assertTrue("Interpolated property not found", client.getEnergy(sourceName, beforeTime,
        afterTime, 0).getProperties().getProperty().contains(interpolatedProp));
    client.deleteSensorData(sourceName, beforeData.getTimestamp());
    client.deleteSensorData(sourceName, afterData.getTimestamp());

    // Computed by hand from Oscar data
    beforeTime = Tstamp.makeTimestamp("2009-10-12T00:00:00.000-10:00");
    afterTime = Tstamp.makeTimestamp("2009-10-12T00:15:00.000-10:00");
    beforeData = SensorDataStraddle.makePowerSensorData(beforeTime, source, 5.5E7, 0, false);
    afterData = SensorDataStraddle.makePowerSensorData(afterTime, source, 6.4E7, 0, false);
    client.storeSensorData(beforeData);
    client.storeSensorData(afterData);
    timestamp1 = Tstamp.makeTimestamp("2009-10-12T00:13:00.000-10:00");
    beforeTime = Tstamp.makeTimestamp("2009-10-12T00:30:00.000-10:00");
    afterTime = Tstamp.makeTimestamp("2009-10-12T00:45:00.000-10:00");
    beforeData = SensorDataStraddle.makePowerSensorData(beforeTime, source, 5.0E7, 0, false);
    afterData = SensorDataStraddle.makePowerSensorData(afterTime, source, 5.4E7, 0, false);
    timestamp2 = Tstamp.makeTimestamp("2009-10-12T00:42:00.000-10:00");
    client.storeSensorData(beforeData);
    client.storeSensorData(afterData);
    // double interpolatedPower = straddle2.getPowerGenerated();
    // System.out.println(interpolatedPower);
    // TODO Seems like these should be closer, but haven't done the hand calculations to confirm
    assertEquals("getEnergyGenerated on on Oscar data was wrong", 2.8033333333333332E7, client
        .getEnergyGenerated(sourceName, timestamp1, timestamp2, 0), 0.2E7);
    assertEquals("getEnergyConsumed on on Oscar data was wrong", 0, client.getEnergyConsumed(
        sourceName, timestamp1, timestamp2, 0), 0.01);
    SensorData energyData = client.getEnergy(sourceName, timestamp1, timestamp2, 1);
    assertEquals("getEnergy on on Oscar data was wrong", 2.8033333333333332E7, energyData
        .getProperties().getPropertyAsDouble("energyGenerated"), 0.2E7);
    assertEquals("getEnergy on on Oscar data was wrong", 0, energyData.getProperties()
        .getPropertyAsDouble("energyConsumed"), 0.01);
  }

  /**
   * Tests the energy resource on a virtual source.
   * 
   * @throws Exception If there are problems creating timestamps, or if the client has problems.
   */
  @Test
  public void testGetVirtualSourceEnergy() throws Exception {
    WattDepotClient client =
        new WattDepotClient(getHostName(), DbManager.defaultOwnerUsername,
            DbManager.defaultOwnerPassword);

    XMLGregorianCalendar beforeTime, afterTime, timestamp1, timestamp2;
    SensorData beforeData, afterData;
    String source1Name = DbManager.defaultPublicSource;
    String source2Name = DbManager.defaultPrivateSource;
    String virtualSourceName = DbManager.defaultVirtualSource;
    String source1 = sourceToUri(source1Name, server);
    String source2 = sourceToUri(source2Name, server);
    Property interpolatedProp = SensorDataUtils.makeSensorDataProperty("interpolated", "true");

    // timestamp = range for flat power on both sources, getEnergy should just return double
    beforeTime = Tstamp.makeTimestamp("2009-07-28T08:00:00.000-10:00");
    beforeData = SensorDataStraddle.makePowerSensorData(beforeTime, source1, 100, 0, false);
    client.storeSensorData(beforeData);
    beforeData = SensorDataStraddle.makePowerSensorData(beforeTime, source2, 100, 0, false);
    client.storeSensorData(beforeData);
    afterTime = Tstamp.makeTimestamp("2009-07-28T09:00:00.000-10:00");
    afterData = SensorDataStraddle.makePowerSensorData(afterTime, source1, 100, 0, false);
    client.storeSensorData(afterData);
    afterData = SensorDataStraddle.makePowerSensorData(afterTime, source2, 100, 0, false);
    client.storeSensorData(afterData);
    assertEquals("getEnergy on degenerate range with default interval gave wrong value", 200,
        client.getEnergyGenerated(virtualSourceName, beforeTime, afterTime, 0), 0.01);
    assertEquals("getEnergy on degenerate range with 2 minute interval gave wrong value", 200,
        client.getEnergyGenerated(virtualSourceName, beforeTime, afterTime, 2), 0.01);
    assertEquals("getEnergy on degenerate range with 5 minute interval gave wrong value", 200,
        client.getEnergyGenerated(virtualSourceName, beforeTime, afterTime, 5), 0.01);
    assertEquals("getEnergy on degenerate range with 5 minute interval gave wrong value", 200,
        client.getEnergyGenerated(virtualSourceName, beforeTime, afterTime, 30), 0.01);
    assertEquals("getEnergy on degenerate range with 5 minute interval gave wrong value", 200,
        client.getEnergyGenerated(virtualSourceName, beforeTime, afterTime, 29), 0.01);
    try {
      client.getEnergyGenerated(virtualSourceName, beforeTime, afterTime, 61);
      fail("getEnergy worked with interval longer than range");
    }
    catch (BadXmlException e) { // NOPMD
      // Expected in this case
    }
    client.deleteSensorData(source1Name, beforeData.getTimestamp());
    client.deleteSensorData(source1Name, afterData.getTimestamp());
    client.deleteSensorData(source2Name, beforeData.getTimestamp());
    client.deleteSensorData(source2Name, afterData.getTimestamp());

    // Simple, in the middle of interval
    beforeTime = Tstamp.makeTimestamp("2009-10-12T00:12:35.000-10:00");
    beforeData = SensorDataStraddle.makePowerSensorData(beforeTime, source1, 1.0E7, 0, false);
    afterTime = Tstamp.makeTimestamp("2009-10-12T00:13:25.000-10:00");
    afterData = SensorDataStraddle.makePowerSensorData(afterTime, source1, 2.0E7, 0, false);
    client.storeSensorData(beforeData);
    client.storeSensorData(afterData);
    timestamp1 = Tstamp.makeTimestamp("2009-10-12T00:13:00.000-10:00");

    beforeTime = Tstamp.makeTimestamp("2009-10-12T01:12:35.000-10:00");
    beforeData = SensorDataStraddle.makePowerSensorData(beforeTime, source1, 1.0E7, 0, false);
    afterTime = Tstamp.makeTimestamp("2009-10-12T01:13:25.000-10:00");
    afterData = SensorDataStraddle.makePowerSensorData(afterTime, source1, 2.0E7, 0, false);
    client.storeSensorData(beforeData);
    client.storeSensorData(afterData);
    timestamp2 = Tstamp.makeTimestamp("2009-10-12T01:13:00.000-10:00");
    assertEquals("getEnergyGenerated on for simple gave wrong value", 1.5E7, client
        .getEnergyGenerated(source1Name, timestamp1, timestamp2, 0), 0.01);

    // Computed by hand from Oscar data
    beforeTime = Tstamp.makeTimestamp("2009-10-12T00:00:00.000-10:00");
    beforeData = SensorDataStraddle.makePowerSensorData(beforeTime, source2, 5.5E7, 0, false);
    client.storeSensorData(beforeData);
    afterTime = Tstamp.makeTimestamp("2009-10-12T00:15:00.000-10:00");
    afterData = SensorDataStraddle.makePowerSensorData(afterTime, source2, 6.4E7, 0, false);
    client.storeSensorData(afterData);

    beforeTime = Tstamp.makeTimestamp("2009-10-12T01:00:00.000-10:00");
    beforeData = SensorDataStraddle.makePowerSensorData(beforeTime, source2, 5.5E7, 0, false);
    client.storeSensorData(beforeData);
    afterTime = Tstamp.makeTimestamp("2009-10-12T01:15:00.000-10:00");
    afterData = SensorDataStraddle.makePowerSensorData(afterTime, source2, 6.4E7, 0, false);
    client.storeSensorData(afterData);
    assertEquals("getEnergyGenerated on for simple gave wrong value", 5.948E7, client
        .getEnergyGenerated(source2Name, timestamp1, timestamp2, 0), 0.2E7);

    // Virtual source should get the sum of the two previous power values
    assertEquals("energy for virtual source did not equal expected value", 7.448E7, client
        .getEnergyGenerated(virtualSourceName, timestamp1, timestamp2, 0), 0.01);
    assertTrue("Interpolated property not found", client.getEnergy(virtualSourceName, beforeTime,
        afterTime, 0).getProperties().getProperty().contains(interpolatedProp));
  }
}

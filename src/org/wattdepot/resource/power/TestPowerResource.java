package org.wattdepot.resource.power;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.wattdepot.resource.source.SourceUtils.sourceToUri;
import javax.xml.datatype.XMLGregorianCalendar;
import org.hackystat.utilities.tstamp.Tstamp;
import org.junit.Before;
import org.junit.Test;
import org.wattdepot.client.WattDepotClient;
import org.wattdepot.resource.sensordata.SensorDataStraddle;
import org.wattdepot.resource.sensordata.SensorDataUtils;
import org.wattdepot.resource.sensordata.jaxb.Properties;
import org.wattdepot.resource.sensordata.jaxb.Property;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.server.db.DbManager;
import org.wattdepot.test.ServerTestHelper;

/**
 * Tests the Power resource API at the HTTP level using WattDepotClient.
 * 
 * @author Robert Brewer
 */
public class TestPowerResource extends ServerTestHelper {

  /** Making PMD happy. */
  private static final String POWER_GENERATED = "powerGenerated";

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
   * Tests the power resource on a non-virtual source.
   * 
   * @throws Exception If there are problems creating timestamps, or if the client has problems.
   */
  @Test
  @SuppressWarnings("PMD.AvoidDuplicateLiterals")
  public void testGetPower() throws Exception {
    WattDepotClient client =
        new WattDepotClient(getHostName(), DbManager.defaultOwnerUsername,
            DbManager.defaultOwnerPassword);

    XMLGregorianCalendar beforeTime, afterTime, timestamp;
    SensorData beforeData, afterData, powerData;
    String tool = "JUnit";
    String source = sourceToUri(DbManager.defaultPublicSource, server);
    String sourceName = DbManager.defaultPublicSource;
    Properties beforeProps, afterProps;
    Property beforeProp, afterProp;
    double interpolatedPower;

    // timestamp == beforeData == afterData, getPower should just return beforeData
    beforeTime = Tstamp.makeTimestamp("2009-07-28T08:00:00.000-10:00");
    beforeProp = SensorDataUtils.makeSensorDataProperty(POWER_GENERATED, "100");
    beforeProps = new Properties();
    beforeProps.getProperty().add(beforeProp);
    beforeData = SensorDataUtils.makeSensorData(beforeTime, tool, source, beforeProps);
    client.storeSensorData(beforeData);
    timestamp = beforeTime;
    assertEquals("getPower on degenerate straddle did not return beforeData", client.getPower(
        sourceName, timestamp), beforeData);
    client.deleteSensorData(sourceName, beforeData.getTimestamp());

    // slope is 2 (100 W difference in 50 seconds)
    beforeTime = Tstamp.makeTimestamp("2009-07-28T08:00:00.000-10:00");
    afterTime = Tstamp.makeTimestamp("2009-07-28T08:00:50.000-10:00");
    beforeProp = SensorDataUtils.makeSensorDataProperty(POWER_GENERATED, "100");
    beforeProps = new Properties();
    beforeProps.getProperty().add(beforeProp);
    afterProp = SensorDataUtils.makeSensorDataProperty(POWER_GENERATED, "200");
    afterProps = new Properties();
    afterProps.getProperty().add(afterProp);
    beforeData = SensorDataUtils.makeSensorData(beforeTime, tool, source, beforeProps);
    client.storeSensorData(beforeData);
    afterData = SensorDataUtils.makeSensorData(afterTime, tool, source, afterProps);
    client.storeSensorData(afterData);
    timestamp = Tstamp.makeTimestamp("2009-07-28T08:00:25.000-10:00");
    powerData = client.getPower(sourceName, timestamp);
    interpolatedPower = Double.valueOf(powerData.getProperties().getProperty().get(0).getValue());
    Property interpolatedProp = SensorDataUtils.makeSensorDataProperty("interpolated", "true");
    assertEquals("Interpolated power did not equal expected value", 150, interpolatedPower, 0.01);
    assertTrue("Interpolated property not found", powerData.getProperties().getProperty().contains(
        interpolatedProp));
    client.deleteSensorData(sourceName, beforeData.getTimestamp());
    client.deleteSensorData(sourceName, afterData.getTimestamp());

    // Computed by hand from Oscar data
    beforeTime = Tstamp.makeTimestamp("2009-10-12T00:00:00.000-10:00");
    afterTime = Tstamp.makeTimestamp("2009-10-12T00:15:00.000-10:00");
    beforeProp = SensorDataUtils.makeSensorDataProperty(POWER_GENERATED, "5.5E7");
    beforeProps = new Properties();
    beforeProps.getProperty().add(beforeProp);
    afterProp = SensorDataUtils.makeSensorDataProperty(POWER_GENERATED, "6.4E7");
    afterProps = new Properties();
    afterProps.getProperty().add(afterProp);
    beforeData = SensorDataUtils.makeSensorData(beforeTime, tool, source, beforeProps);
    client.storeSensorData(beforeData);
    afterData = SensorDataUtils.makeSensorData(afterTime, tool, source, afterProps);
    client.storeSensorData(afterData);
    timestamp = Tstamp.makeTimestamp("2009-10-12T00:13:00.000-10:00");
    powerData = client.getPower(sourceName, timestamp);
    interpolatedPower = Double.valueOf(powerData.getProperties().getProperty().get(0).getValue());
    assertEquals("Interpolated power did not equal expected value", 6.28E7, interpolatedPower, 0.01);
    // Test getPowerGenerated
    assertEquals("Interpolated generated power did not equal expected value", 6.28E7, client
        .getPowerGenerated(sourceName, timestamp), 0.01);
    assertEquals("Interpolated consumed power did not equal expected value", 0, client
        .getPowerConsumed(sourceName, timestamp), 0.01);
  }

  /**
   * Tests the power resource on a virtual source.
   * 
   * @throws Exception If there are problems creating timestamps, or if the client has problems.
   */
  @Test
  @SuppressWarnings("PMD.AvoidDuplicateLiterals")
  public void testGetVirtualSourcePower() throws Exception {
    WattDepotClient client =
        new WattDepotClient(getHostName(), DbManager.defaultOwnerUsername,
            DbManager.defaultOwnerPassword);

    XMLGregorianCalendar beforeTime, afterTime, timestamp;
    SensorData beforeData, afterData, powerData;
    String tool = "JUnit";
    String source1Name = DbManager.defaultPublicSource;
    String source2Name = DbManager.defaultPrivateSource;
    String virtualSourceName = DbManager.defaultVirtualSource;
    String source1 = sourceToUri(source1Name, server);
    String source2 = sourceToUri(source2Name, server);
    Properties beforeProps, afterProps;
    Property beforeProp, afterProp;
    double interpolatedPower = -1;
    Property interpolatedProp = SensorDataUtils.makeSensorDataProperty("interpolated", "true");

    // timestamp == beforeData == afterData on both sources, getPower should return beforeData * 2
    beforeTime = Tstamp.makeTimestamp("2009-07-28T08:00:00.000-10:00");
    beforeProp = SensorDataUtils.makeSensorDataProperty(POWER_GENERATED, "100");
    beforeProps = new Properties();
    beforeProps.getProperty().add(beforeProp);
    // data for source1
    beforeData = SensorDataUtils.makeSensorData(beforeTime, tool, source1, beforeProps);
    client.storeSensorData(beforeData);
    // data for source2
    beforeData = SensorDataUtils.makeSensorData(beforeTime, tool, source2, beforeProps);
    client.storeSensorData(beforeData);
    timestamp = beforeTime;
    powerData = client.getPower(virtualSourceName, timestamp);
    interpolatedPower =
        SensorDataStraddle.getPropertyAsDouble(powerData.getProperties(), POWER_GENERATED);
    assertEquals("getPower for virtual source on degenerate straddle did not return beforeData",
        200, interpolatedPower, 0.1);
    assertFalse("Interpolated property found on non-interpolated data", powerData.getProperties()
        .getProperty().contains(interpolatedProp));
    // Delete sensordata for next test
    client.deleteSensorData(source1Name, beforeData.getTimestamp());
    client.deleteSensorData(source2Name, beforeData.getTimestamp());

    // Simple, in the middle of interval
    beforeTime = Tstamp.makeTimestamp("2009-10-12T00:12:35.000-10:00");
    afterTime = Tstamp.makeTimestamp("2009-10-12T00:13:25.000-10:00");
    beforeProp = SensorDataUtils.makeSensorDataProperty(POWER_GENERATED, "1.0E7");
    beforeProps = new Properties();
    beforeProps.getProperty().add(beforeProp);
    afterProp = SensorDataUtils.makeSensorDataProperty(POWER_GENERATED, "2.0E7");
    afterProps = new Properties();
    afterProps.getProperty().add(afterProp);
    beforeData = SensorDataUtils.makeSensorData(beforeTime, tool, source1, beforeProps);
    client.storeSensorData(beforeData);
    afterData = SensorDataUtils.makeSensorData(afterTime, tool, source1, afterProps);
    client.storeSensorData(afterData);
    timestamp = Tstamp.makeTimestamp("2009-10-12T00:13:00.000-10:00");
    powerData = client.getPower(source1Name, timestamp);
    interpolatedPower = Double.valueOf(powerData.getProperties().getProperty().get(0).getValue());
    assertEquals("Interpolated power did not equal expected value", 1.5E7, interpolatedPower, 0.01);
    assertTrue("Interpolated property not found", powerData.getProperties().getProperty().contains(
        interpolatedProp));
    // Test getPowerGenerated
    assertEquals("Interpolated power did not equal expected value", 1.5E7, client
        .getPowerGenerated(source1Name, timestamp), 0.01);

    // Computed by hand from Oscar data
    beforeTime = Tstamp.makeTimestamp("2009-10-12T00:00:00.000-10:00");
    afterTime = Tstamp.makeTimestamp("2009-10-12T00:15:00.000-10:00");
    beforeProp = SensorDataUtils.makeSensorDataProperty(POWER_GENERATED, "5.5E7");
    beforeProps = new Properties();
    beforeProps.getProperty().add(beforeProp);
    afterProp = SensorDataUtils.makeSensorDataProperty(POWER_GENERATED, "6.4E7");
    afterProps = new Properties();
    afterProps.getProperty().add(afterProp);
    beforeData = SensorDataUtils.makeSensorData(beforeTime, tool, source2, beforeProps);
    client.storeSensorData(beforeData);
    afterData = SensorDataUtils.makeSensorData(afterTime, tool, source2, afterProps);
    client.storeSensorData(afterData);
    timestamp = Tstamp.makeTimestamp("2009-10-12T00:13:00.000-10:00");
    powerData = client.getPower(source2Name, timestamp);
    interpolatedPower = Double.valueOf(powerData.getProperties().getProperty().get(0).getValue());
    assertEquals("Interpolated power did not equal expected value", 6.28E7, interpolatedPower, 0.01);
    assertEquals("Interpolated power did not equal expected value", 6.28E7, client
        .getPowerGenerated(source2Name, timestamp), 0.01);
    
    // Virtual source should get the sum of the two previous power values
    powerData = client.getPower(virtualSourceName, timestamp);
    interpolatedPower = Double.valueOf(powerData.getProperties().getProperty().get(0).getValue());
    assertEquals("Interpolated power did not equal expected value", 7.78E7, interpolatedPower, 0.01);
    assertEquals("Interpolated power did not equal expected value", 7.78E7, client
        .getPowerGenerated(virtualSourceName, timestamp), 0.01);
  }
}

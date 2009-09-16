package org.wattdepot.resource.sensordata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.wattdepot.resource.sensordata.SensorDataUtils.compareSensorDataRefsToSensorDatas;
import static org.wattdepot.resource.sensordata.SensorDataUtils.sensorDataRefEqualsSensorData;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.wattdepot.client.NotAuthorizedException;
import org.wattdepot.client.ResourceNotFoundException;
import org.wattdepot.client.WattDepotClient;
import org.wattdepot.client.WattDepotClientException;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.resource.sensordata.jaxb.SensorDataRef;
import org.wattdepot.server.db.DbManager;
import org.wattdepot.test.ServerTestHelper;

/**
 * Tests the SensorData resource API at the HTTP level using WattDepotClient.
 * 
 * @author Robert Brewer
 */
public class TestSensorDataResource extends ServerTestHelper {

  /** Making PMD happy. */
  private static final String MISSING_SENSORDATAREFS =
      "SensorDataIndex did not contain list of SensorDataRefs";

  /**
   * Creates a fresh DbManager for each test. This might prove too expensive for some
   * DbImplementations, but we'll cross that bridge when we get there.
   */
  @Before
  public void makeDb() {
    DbManager manager =
        new DbManager(server, "org.wattdepot.server.db.memory.MemoryStorageImplementation", true);
    assertTrue("Unable to create default data", manager.createDefaultData());
    server.getContext().getAttributes().put("DbManager", manager);
    // Need to create default data for each fresh DbManager
  }

  // Tests for GET {host}/sources/{source}/sensordata

  /**
   * Tests retrieval of all SensorData from a Source. Type: public Source with no credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test
  public void testFullIndexPublicWithNoCredentials() throws WattDepotClientException {
    WattDepotClient client = new WattDepotClient(getHostName(), null, null);
    assertNotNull(MISSING_SENSORDATAREFS, client.getSensorDataIndex(DbManager.defaultPublicSource)
        .getSensorDataRef());
  }

  /**
   * Tests retrieval of all SensorData from a Source. Type: public Source with invalid credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test(expected = NotAuthorizedException.class)
  public void testFullIndexPublicBadAuth() throws WattDepotClientException {
    // Shouldn't authenticate with no username or password
    WattDepotClient client = new WattDepotClient(getHostName(), adminEmail, "foo");
    client.getSensorDataIndex(DbManager.defaultPublicSource);
  }

  /**
   * Tests retrieval of all SensorData from a Source. Type: public Source with valid admin
   * credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test
  public void testFullIndexPublicWithAdminCredentials() throws WattDepotClientException {
    WattDepotClient client = new WattDepotClient(getHostName(), adminEmail, adminPassword);
    assertNotNull(MISSING_SENSORDATAREFS, client.getSensorDataIndex(DbManager.defaultPublicSource)
        .getSensorDataRef());
  }

  /**
   * Tests retrieval of all SensorData from a Source. Type: public Source with valid owner
   * credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test
  public void testFullIndexPublicWithOwnerCredentials() throws WattDepotClientException {
    WattDepotClient client =
        new WattDepotClient(getHostName(), DbManager.defaultOwnerUsername,
            DbManager.defaultOwnerPassword);
    assertNotNull(MISSING_SENSORDATAREFS, client.getSensorDataIndex(DbManager.defaultPublicSource)
        .getSensorDataRef());
  }

  /**
   * Tests retrieval of all SensorData from a Source. Type: public Source with valid non-owner
   * credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test
  public void testFullIndexPublicWithNonOwnerCredentials() throws WattDepotClientException {
    WattDepotClient client =
        new WattDepotClient(getHostName(), DbManager.defaultNonOwnerUsername,
            DbManager.defaultNonOwnerPassword);
    assertNotNull(MISSING_SENSORDATAREFS, client.getSensorDataIndex(DbManager.defaultPublicSource)
        .getSensorDataRef());
  }

  /**
   * Tests retrieval of all SensorData from a Source. Type: private Source with no credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test(expected = NotAuthorizedException.class)
  public void testFullIndexPrivateWithNoCredentials() throws WattDepotClientException {
    WattDepotClient client = new WattDepotClient(getHostName(), null, null);
    client.getSensorDataIndex(DbManager.defaultPrivateSource);
  }

  /**
   * Tests retrieval of all SensorData from a Source. Type: private Source with invalid credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test(expected = NotAuthorizedException.class)
  public void testFullIndexPrivateBadAuth() throws WattDepotClientException {
    // Shouldn't authenticate with no username or password
    WattDepotClient client = new WattDepotClient(getHostName(), adminEmail, "wrong-password");
    client.getSensorDataIndex(DbManager.defaultPrivateSource);
  }

  /**
   * Tests retrieval of all SensorData from a Source. Type: private Source with admin credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test
  public void testFullIndexPrivateAdminAuth() throws WattDepotClientException {
    // Shouldn't authenticate with no username or password
    WattDepotClient client = new WattDepotClient(getHostName(), adminEmail, adminPassword);
    assertNotNull(MISSING_SENSORDATAREFS,
        client.getSensorDataIndex(DbManager.defaultPrivateSource));
  }

  /**
   * Tests retrieval of all SensorData from a Source. Type: private Source with owner credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test
  public void testFullIndexPrivateOwnerAuth() throws WattDepotClientException {
    // Shouldn't authenticate with no username or password
    WattDepotClient client =
        new WattDepotClient(getHostName(), DbManager.defaultOwnerUsername,
            DbManager.defaultOwnerPassword);
    assertNotNull(MISSING_SENSORDATAREFS,
        client.getSensorDataIndex(DbManager.defaultPrivateSource));
  }

  /**
   * Tests retrieval of all SensorData from a Source. Type: private Source with non-owner
   * credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test(expected = NotAuthorizedException.class)
  public void testFullIndexPrivateNonOwnerAuth() throws WattDepotClientException {
    // Shouldn't authenticate with no username or password
    WattDepotClient client =
        new WattDepotClient(getHostName(), DbManager.defaultNonOwnerUsername,
            DbManager.defaultNonOwnerPassword);
    client.getSensorDataIndex(DbManager.defaultPrivateSource);
  }

  /**
   * Tests retrieval of all SensorData from a Source. Type: unknown Source name with no credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test(expected = ResourceNotFoundException.class)
  public void testFullIndexBadSourceNameAnon() throws WattDepotClientException {
    // Shouldn't authenticate with no username or password
    WattDepotClient client = new WattDepotClient(getHostName(), null, null);
    client.getSensorDataIndex("bogus-source-name");
  }

  /**
   * Tests retrieval of all SensorData from a Source. Type: unknown Source name with valid
   * credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test(expected = ResourceNotFoundException.class)
  public void testFullIndexBadSourceNameAuth() throws WattDepotClientException {
    // Shouldn't authenticate with no username or password
    WattDepotClient client = new WattDepotClient(getHostName(), adminEmail, adminPassword);
    client.getSensorDataIndex("bogus-source-name");
  }

  /**
   * Tests that a Source starts with no SensorData. Type: public Source with valid owner
   * credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test
  public void testFullIndexStartsEmpty() throws WattDepotClientException {
    WattDepotClient client =
        new WattDepotClient(getHostName(), DbManager.defaultOwnerUsername,
            DbManager.defaultOwnerPassword);
    assertTrue("Fresh DB contains SensorData", client.getSensorDataIndex(
        DbManager.defaultPublicSource).getSensorDataRef().isEmpty());
  }

  /**
   * Tests that after storing SensorData to a Source, the SensorDataIndex corresponds to the data
   * that has been stored. Type: public Source with valid valid owner credentials
   * 
   * @throws Exception If stuff goes wrong.
   */
  @Test
  public void testFullIndexAfterStores() throws Exception {
    WattDepotClient client =
        new WattDepotClient(getHostName(), DbManager.defaultOwnerUsername,
            DbManager.defaultOwnerPassword);
    SensorData data1 = makeTestSensorData1(), data2 = makeTestSensorData2(), data3 =
        makeTestSensorData3();
    assertTrue("SensorData store failed", client.storeSensorData(data1));
    List<SensorDataRef> index =
        client.getSensorDataIndex(DbManager.defaultPublicSource).getSensorDataRef();
    assertEquals("Wrong number of SensorDataRefs after store", 1, index.size());
    assertTrue("getSensorDataIndex didn't return expected SensorDataRef",
        sensorDataRefEqualsSensorData(index.get(0), data1));
    assertTrue("SensorData store failed", client.storeSensorData(data2));
    index = client.getSensorDataIndex(DbManager.defaultPublicSource).getSensorDataRef();
    assertEquals("Wrong number of SensorDataRefs after store", 2, index.size());
    List<SensorData> origData = new ArrayList<SensorData>();
    origData.add(data1);
    origData.add(data2);
    assertTrue("getSensorDataIndex didn't return expected SensorDataRefs",
        compareSensorDataRefsToSensorDatas(index, origData));
    assertTrue("SensorData store failed", client.storeSensorData(data3));
    index = client.getSensorDataIndex(DbManager.defaultPublicSource).getSensorDataRef();
    assertEquals("Wrong number of SensorDataRefs after store", 3, index.size());
    origData.add(data3);
    assertTrue("getSensorDataIndex didn't return expected SensorDataRefs",
        compareSensorDataRefsToSensorDatas(index, origData));
  }

  // Tests for GET {host}/sources/{source}/sensordata/?startTime={timestamp}&endTime={timestamp}
  // not yet implemented in WattDepotClient
  // TODO

  // Tests for GET {host}/sources/{source}/sensordata/{timestamp}
  // TODO

  // Tests for PUT {host}/sources/{source}/sensordata/{timestamp}
  // TODO

  /**
   * Tests storing SensorData to a Source. Type: public Source with no credentials.
   * 
   * @throws Exception If things go wrong during data setup.
   */
  @Test(expected = NotAuthorizedException.class)
  public void testStorePublicWithNoCredentials() throws Exception {
    WattDepotClient client = new WattDepotClient(getHostName(), null, null);
    SensorData data = makeTestSensorData1();
    assertFalse("Able to store SensorData without credentials", client.storeSensorData(data));
  }

  /**
   * Tests storing SensorData to a Source. Type: public Source with invalid credentials.
   * 
   * @throws Exception If things go wrong during data setup.
   */
  @Test(expected = NotAuthorizedException.class)
  public void testStorePublicWithBadAuth() throws Exception {
    // Shouldn't authenticate with no username or password
    WattDepotClient client = new WattDepotClient(getHostName(), adminEmail, "foo");
    SensorData data = makeTestSensorData1();
    assertFalse("Able to store SensorData with bad credentials", client.storeSensorData(data));
  }

  /**
   * Tests storing SensorData to a Source. Type: public Source with valid admin credentials.
   * 
   * @throws Exception If things go wrong during data setup.
   */
  @Test
  public void testStorePublicWithAdminCredentials() throws Exception {
    WattDepotClient client = new WattDepotClient(getHostName(), adminEmail, adminPassword);
    SensorData data = makeTestSensorData1();
    assertTrue("Unable to store SensorData with admin credentials", client.storeSensorData(data));
    assertEquals("Retrieved SensorData does not match stored SensorData", data, client
        .getSensorData(DbManager.defaultPublicSource, data.getTimestamp()));
  }

  /**
   * Tests storing SensorData to a Source. Type: public Source with valid owner credentials.
   * 
   * @throws Exception If problems are encountered.
   */
  @Test
  public void testStorePublicWithOwnerCredentials() throws Exception {
    WattDepotClient client =
        new WattDepotClient(getHostName(), DbManager.defaultOwnerUsername,
            DbManager.defaultOwnerPassword);
    SensorData data = makeTestSensorData1();
    assertTrue("Unable to store SensorData with valid owner credentials", client
        .storeSensorData(data));
    assertEquals("Retrieved SensorData does not match stored SensorData", data, client
        .getSensorData(DbManager.defaultPublicSource, data.getTimestamp()));
  }

  /**
   * Tests storing SensorData to a Source. Type: public Source with valid non-owner credentials.
   * 
   * @throws Exception If problems are encountered.
   */
  @Test(expected = NotAuthorizedException.class)
  public void testStorePublicWithNonOwnerCredentials() throws Exception {
    WattDepotClient client =
        new WattDepotClient(getHostName(), DbManager.defaultNonOwnerUsername,
            DbManager.defaultNonOwnerPassword);
    SensorData data = makeTestSensorData1();
    assertFalse("Able to store SensorData with valid non-owner credentials", client
        .storeSensorData(data));
  }

  /**
   * Tests storing SensorData to a Source. Type: private Source with no credentials.
   * 
   * @throws Exception If problems are encountered
   */
  @Test(expected = NotAuthorizedException.class)
  public void testStorePrivateWithNoCredentials() throws Exception {
    WattDepotClient client = new WattDepotClient(getHostName(), null, null);
    SensorData data = makeTestSensorDataPrivateSource();
    assertFalse("Able to store SensorData with no credentials", client.storeSensorData(data));
  }

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
  // assertNotNull(MISSING_SENSORDATAREFS, client
  // .getSensorDataIndex(DbManager.defaultPrivateSource));
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
  // assertNotNull(MISSING_SENSORDATAREFS, client
  // .getSensorDataIndex(DbManager.defaultPrivateSource));
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
  // WattDepotClient client = new WattDepotClient(getHostName(), null, null);
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

  // Tests for DELETE {host}/sources/{source}/sensordata/{timestamp}
  // TODO

}

package org.wattdepot.resource.sensordata;

import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import org.wattdepot.client.NotAuthorizedException;
import org.wattdepot.client.ResourceNotFoundException;
import org.wattdepot.client.WattDepotClient;
import org.wattdepot.client.WattDepotClientException;
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
   * Tests retrieval of all SensorData from a Source. Type: public Source with no credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test
  public void testFullIndexPublicWithNoCredentials() throws WattDepotClientException {
    WattDepotClient client = new WattDepotClient(getHostName(), null, null);
    assertNotNull(MISSING_SENSORDATAREFS, client
        .getSensorDataIndex(DbManager.defaultPublicSource).getSensorDataRef());
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
    assertNotNull(MISSING_SENSORDATAREFS, client
        .getSensorDataIndex(DbManager.defaultPublicSource).getSensorDataRef());
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
    assertNotNull(MISSING_SENSORDATAREFS, client
        .getSensorDataIndex(DbManager.defaultPublicSource).getSensorDataRef());
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
    assertNotNull(MISSING_SENSORDATAREFS, client
        .getSensorDataIndex(DbManager.defaultPublicSource).getSensorDataRef());
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
    assertNotNull(MISSING_SENSORDATAREFS, client
        .getSensorDataIndex(DbManager.defaultPrivateSource));
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
    assertNotNull(MISSING_SENSORDATAREFS, client
        .getSensorDataIndex(DbManager.defaultPrivateSource));
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
}

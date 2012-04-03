package org.wattdepot.resource;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.wattdepot.client.NotAuthorizedException;
import org.wattdepot.client.ResourceNotFoundException;
import org.wattdepot.client.WattDepotClient;
import org.wattdepot.client.WattDepotClientException;
import org.wattdepot.test.ServerTestHelper;

public class TestWattDepotResource extends ServerTestHelper {

  // --------------------------------------
  // Test Authentication
  // Authentication is performed in the WattDepotVerifier, which all resources use.
  // --------------------------------------

  /**
   * Tests authentication with no credentials by attempting to retrieve all sources.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test
  public void testFullIndexPublicWithNoCredentials() throws WattDepotClientException {
    WattDepotClient client = new WattDepotClient(getHostName());
    assertNotNull("Could not access resources anonymously", client.getSources());
  }

  /**
   * Tests authentication with bad credentials by attempting to retrieve all sources.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test(expected = NotAuthorizedException.class)
  public void testFullIndexPublicBadAuth() throws WattDepotClientException {
    // Shouldn't authenticate with invalid credentials.
    WattDepotClient client = new WattDepotClient(getHostName(), adminEmail, "foo");
    client.getSources();
    fail("Could access resources with bad credentials");
  }

  /**
   * Tests authentication with valid credentials by attempting to retrieve all sources.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test
  public void testFullIndexPublicWithUserCredentials() throws WattDepotClientException {
    WattDepotClient client =
        new WattDepotClient(getHostName(), defaultNonOwnerUsername, defaultNonOwnerPassword);
    assertNotNull("Could not access resources with user credentials", client.getSources());
  }

  // -------------------------------
  // Test Authorization
  // Authorization for all GETs that have a source name in the URI is done in the WattDepotResource.
  // The source must be public, or the user must be the owner of the source or an admin.
  // This is used in Carbon, Energy, GViz, Power, Source, SensorData, and SourceSummary
  // --------------------------------------

  /**
   * Tests retrieval of a public source with admin credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test
  public void testPublicWithAdminCredentials() throws WattDepotClientException {
    WattDepotClient client = new WattDepotClient(getHostName(), adminEmail, adminPassword);
    assertNotNull("Couldn't retrieve public source from admin",
        client.getSource(defaultPublicSource));
  }

  /**
   * Tests retrieval of a public source with owner credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test
  public void testPublicWithOwnerCredentials() throws WattDepotClientException {
    WattDepotClient client =
        new WattDepotClient(getHostName(), defaultOwnerUsername, defaultOwnerPassword);
    assertNotNull("Couldn't retrieve public source from owner",
        client.getSource(defaultPublicSource));
  }

  /**
   * Tests retrieval of a public source with non-owner credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test
  public void testPublicWithNonOwnerCredentials() throws WattDepotClientException {
    WattDepotClient client =
        new WattDepotClient(getHostName(), defaultNonOwnerUsername, defaultNonOwnerPassword);
    assertNotNull("Couldn't retrieve public source from non-owner",
        client.getSource(defaultPublicSource));
  }

  /**
   * Tests retrieval of a private source with no credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test(expected = NotAuthorizedException.class)
  public void testPrivateWithNoCredentials() throws WattDepotClientException {
    WattDepotClient client = new WattDepotClient(getHostName());
    client.getSource(defaultPrivateSource);
    fail("Could retrieve private source from anonymous");
  }

  /**
   * Tests retrieval of a private source with admin credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test
  public void testPrivateAdminAuth() throws WattDepotClientException {
    // Shouldn't authenticate with no username or password
    WattDepotClient client = new WattDepotClient(getHostName(), adminEmail, adminPassword);
    assertNotNull("Couldn't retrieve private source from admin",
        client.getSource(defaultPrivateSource));
  }

  /**
   * Tests retrieval of a private source with owner credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test
  public void testPrivateOwnerAuth() throws WattDepotClientException {
    // Shouldn't authenticate with no username or password
    WattDepotClient client =
        new WattDepotClient(getHostName(), defaultOwnerUsername, defaultOwnerPassword);
    assertNotNull("Couldn't retrieve private source from owner",
        client.getSource(defaultPrivateSource));
  }

  /**
   * Tests retrieval of a private source with non-owner credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test(expected = NotAuthorizedException.class)
  public void testPrivateNonOwnerAuth() throws WattDepotClientException {
    // Shouldn't authenticate with no username or password
    WattDepotClient client =
        new WattDepotClient(getHostName(), defaultNonOwnerUsername, defaultNonOwnerPassword);
    client.getSource(defaultPrivateSource);
    fail("Could retrieve private source from non-owner");
  }

  // --------------------------------------
  // Test valid source name.
  // Validation for all GETs that have a source name in the URI is done in the WattDepotResource.
  // This is used in Carbon, Energy, GViz, Power, Source, SensorData, and SourceSummary
  // --------------------------------------

  /**
   * Tests retrieval of an invalid source with no credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test(expected = ResourceNotFoundException.class)
  public void testBadSourceNameAnon() throws WattDepotClientException {
    WattDepotClient client = new WattDepotClient(getHostName());
    client.getSource("bogus-source-name");
    fail("Invalid source name didn't throw ResourceNotFound");
  }

  /**
   * Tests retrieval of an invalid source with valid credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test(expected = ResourceNotFoundException.class)
  public void testBadSourceNameAuth() throws WattDepotClientException {
    WattDepotClient client = new WattDepotClient(getHostName(), adminEmail, adminPassword);
    client.getSource("bogus-source-name");
    fail("Invalid source name didn't throw ResourceNotFound");
  }

}

package org.wattdepot.resource.user;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.wattdepot.client.NotAuthorizedException;
import org.wattdepot.client.WattDepotClient;
import org.wattdepot.client.WattDepotClientException;
import org.wattdepot.test.ServerTestHelper;

/**
 * Tests the User resource API at the HTTP level using WattDepotClient.
 * 
 * @author Robert Brewer
 */
public class TestUserResource extends ServerTestHelper {

  /**
   * Test that authentication fails without username and password.
   * 
   * @throws WattDepotClientException If problems occur.
   */
  @Test
  public void testAuthenticationWithNoCredentials() throws WattDepotClientException {
    // Shouldn't authenticate with no username or password
    WattDepotClient client = new WattDepotClient(getHostName());
    assertFalse("Authentication worked with no credentials!!", client.isAuthenticated());
  }

  /**
   * Test that authentication works with admin username and password.
   * 
   * @throws WattDepotClientException If problems occur.
   */
  @Test
  public void testAuthenticationWithAdminCredentials() throws WattDepotClientException {
    // Should authenticate with admin username and password
    WattDepotClient client = new WattDepotClient(getHostName(), adminEmail, adminPassword);
    // System.out.format("admin email: %s, admin password: %s\n", adminEmail, adminPassword);
    assertTrue("Authentication failed with admin credentials!", client.isAuthenticated());
  }

  /**
   * Test that after authentication, can get placeholder User string.
   * 
   * @throws WattDepotClientException If problems occur.
   */
  @Test
  public void testUserResource() throws WattDepotClientException {
    // Currently authenticating as admin user
    WattDepotClient client = new WattDepotClient(getHostName(), adminEmail, adminPassword);
    assertTrue("User resource returned incorrect string", client.getUserString("foo").equals(
        "User resource"));
  }

  /**
   * Test that without authentication, cannot retrieve user list.
   * 
   * @throws WattDepotClientException If there are problems retrieving User list.
   */
  @Test(expected = NotAuthorizedException.class)
  public void testUsersResourceAnonymous() throws WattDepotClientException {
    WattDepotClient client = new WattDepotClient(getHostName());
    assertTrue("Able to retrieve users list anonymously", client.getUserIndex().getUserRef()
        .isEmpty());
  }

  /**
   * Test that after authentication, can retrieve user list.
   * 
   * @throws WattDepotClientException If problems occur.
   */
  @Test
  public void testUsersResource() throws WattDepotClientException {
    // Currently authenticating as admin user
    WattDepotClient client = new WattDepotClient(getHostName(), adminEmail, adminPassword);
    assertNotNull("Unable to retrieve user list with admin account", client.getUserIndex()
        .getUserRef());
  }
}

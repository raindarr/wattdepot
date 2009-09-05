package org.wattdepot.resource.user;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.wattdepot.client.WattDepotClient;
import org.wattdepot.client.WattDepotClientException;
import org.wattdepot.test.ServerTestHelper;

/**
 * Tests the Ping API, which is very simple.
 * 
 * @author Robert Brewer
 */
public class TestUserResource extends ServerTestHelper {

  /**
   * Test that authentication fails without username and password, and works with admin username and
   * password.
   * 
   * @throws Exception If problems occur.
   */
  @Test
  public void testAuthentication() throws Exception {
    // Currently authenticating as admin user
    // System.out.println("admin email: " + adminEmail + ", admin password: " + adminPassword);

    // Shouldn't authenticate with no username or password
    WattDepotClient client = new WattDepotClient(getHostName(), null, null);
    assertFalse("Authentication worked with no credentials!!", client.isAuthenticated());

    // Should authenticate with admin username and password
    client = new WattDepotClient(getHostName(), adminEmail, adminPassword);
    assertTrue("Authentication failed with admin credentials!", client.isAuthenticated());
  }

  /**
   * Test that after authentication, can get placeholder User string.
   * 
   * @throws Exception If problems occur.
   */
  @Test
  public void testUserResource() throws Exception {
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
  @Test(expected = WattDepotClientException.class)
  public void testUsersResourceAnonymous() throws WattDepotClientException {
    WattDepotClient client = new WattDepotClient(getHostName(), null, null);
    assertTrue("Able to retrieve users list anonymously", client.getUserIndex().getUserRef()
        .isEmpty());
  }

  /**
   * Test that after authentication, can retrieve user list.
   * 
   * @throws Exception If problems occur.
   */
  @Test
  public void testUsersResource() throws Exception {
    // Currently authenticating as admin user
    WattDepotClient client = new WattDepotClient(getHostName(), adminEmail, adminPassword);
    assertTrue("Empty DB has Users", client.getUserIndex().getUserRef().isEmpty());
  }
}

package org.wattdepot.resource.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.util.List;
import javax.xml.bind.JAXBException;
import org.junit.Test;
import org.wattdepot.client.NotAuthorizedException;
import org.wattdepot.client.OverwriteAttemptedException;
import org.wattdepot.client.WattDepotClient;
import org.wattdepot.client.WattDepotClientException;
import org.wattdepot.resource.user.jaxb.User;
import org.wattdepot.resource.user.jaxb.UserIndex;
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
   * Test that after authentication, can retrieve Users.
   * 
   * @throws WattDepotClientException If problems occur.
   */
  @Test
  public void testUserResource() throws WattDepotClientException {
    // Currently authenticating as admin user
    WattDepotClient client = new WattDepotClient(getHostName(), adminEmail, adminPassword);
    // Fresh database should have admin user in it
    User adminTestUser = new User(adminEmail, adminPassword, true, null);
    assertEquals("Unable to retrieve proper admin user", adminTestUser, client.getUser(adminEmail));

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
    User adminUser = client.getUser(adminEmail);
    UserIndex index = client.getUserIndex();
    assertNotNull("Unable to retrieve user list with admin account", index.getUserRef());
    assertEquals("Admin user ref didn't correspond to actual admin user", adminUser,
        client.getUser(index.getUserRef().get(0)));
    List<User> users = client.getUsers();
    assertEquals("Expected just admin user from getUsers", adminUser, users.get(0));
  }

  /**
   * Test that when we are authenticated as one user, we cannot get the details for another user.
   * 
   * @throws WattDepotClientException If problems occur.
   */
  @Test(expected = NotAuthorizedException.class)
  public void testGetOtheruser() throws WattDepotClientException {
    WattDepotClient client =
        new WattDepotClient(getHostName(), defaultOwnerUsername, defaultOwnerPassword);
    client.getUser(defaultNonOwnerUsername);
    fail("Able to retrieve a different user");
  }

  /**
   * Test that we can add a user through the client.
   * 
   * @throws WattDepotClientException If problems occur.
   * @throws JAXBException If problems are encountered with XML.
   */
  @Test
  public void testStoreUser() throws WattDepotClientException, JAXBException {
    User newUser = new User("new-user", "new-password", false, null);
    WattDepotClient client = new WattDepotClient(getHostName(), adminEmail, adminPassword);
    assertTrue("Could not store new user", client.storeUser(newUser));
    assertEquals("Not able to retrieve new user", newUser, client.getUser(newUser.getEmail()));
  }

  /**
   * Test that we cannot overwrite another user.
   * 
   * @throws WattDepotClientException If problems occur.
   * @throws JAXBException If problems are encountered with XML.
   */
  @Test(expected = OverwriteAttemptedException.class)
  public void testStoreUserOverwrite() throws WattDepotClientException, JAXBException {
    User oldUser = new User(defaultOwnerUsername, "new-password", false, null);
    WattDepotClient client = new WattDepotClient(getHostName(), adminEmail, adminPassword);
    client.storeUser(oldUser);
    fail("Able to overwrite user");
  }

  /**
   * Test that non-administrators cannot create new users.
   * 
   * @throws WattDepotClientException If problems occur.
   * @throws JAXBException If problems are encountered with XML.
   */
  @Test(expected = NotAuthorizedException.class)
  public void testStoreUserFromNonAdmin() throws WattDepotClientException, JAXBException {
    User newUser = new User("new-user", "new-password", false, null);
    WattDepotClient client =
        new WattDepotClient(getHostName(), defaultOwnerUsername, defaultOwnerPassword);
    client.storeUser(newUser);
    fail("Able to create user from non-admin account");
  }

  /**
   * Test that adminstrators can create new administrators.
   * 
   * @throws WattDepotClientException If problems occur.
   * @throws JAXBException If problems are encountered with XML.
   */
  @Test
  public void testStoreAdminUser() throws WattDepotClientException, JAXBException {
    User newUser = new User("new-admin", "new-password", true, null);
    WattDepotClient client = new WattDepotClient(getHostName(), adminEmail, adminPassword);
    assertTrue("Could not store new admin", client.storeUser(newUser));
    assertEquals("Not able to retrieve new admin", newUser, client.getUser(newUser.getEmail()));
  }

  /**
   * Tests deleting a user. Note that the User Resource returns NotAuthorized instead of
   * ResourceNotFound to avoid leaking data.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test(expected = NotAuthorizedException.class)
  public void testUserDelete() throws WattDepotClientException {
    WattDepotClient client = new WattDepotClient(getHostName(), adminEmail, adminPassword);
    assertTrue("Unable to delete user", client.deleteUser(defaultOwnerUsername));
    assertNull("Able to retrieve deleted user", client.getUser(defaultOwnerUsername));
  }

  /**
   * Tests deleting a user that has already been deleted. Note that the User Resource returns
   * NotAuthorized instead of ResourceNotFound to avoid leaking data.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test(expected = NotAuthorizedException.class)
  public void testUserDoubleDelete() throws WattDepotClientException {
    WattDepotClient client = new WattDepotClient(getHostName(), adminEmail, adminPassword);
    assertTrue("Unable to delete user", client.deleteUser(defaultOwnerUsername));
    assertFalse("Able to delete already deleted user", client.deleteUser(defaultOwnerUsername));
  }

  /**
   * Tests attempting to delete a user from a non-admin.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test(expected = NotAuthorizedException.class)
  public void testOwnerDeleteNonAdmin() throws WattDepotClientException {
    WattDepotClient client =
        new WattDepotClient(getHostName(), defaultNonOwnerUsername, defaultNonOwnerPassword);
    assertFalse("Able to delete source from non-admin", client.deleteUser(defaultOwnerUsername));
  }
}

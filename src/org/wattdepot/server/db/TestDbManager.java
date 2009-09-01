package org.wattdepot.server.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.wattdepot.resource.user.UserUtils.makeUser;
import static org.wattdepot.resource.user.UserUtils.makeUserProperty;
import static org.wattdepot.resource.user.UserUtils.userRefEqualsUser;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wattdepot.resource.user.jaxb.Properties;
import org.wattdepot.resource.user.jaxb.User;
import org.wattdepot.resource.user.jaxb.UserRef;
import org.wattdepot.server.Server;

/**
 * Instantiates a DbManager and tests the database methods.
 * 
 * @author Robert Brewer
 */
public class TestDbManager {

  /**
   * The DbManager under test.
   */
  protected static DbManager manager;

  /**
   * Creates the DbManager that will be used for all the tests. Since the same instance is used for
   * all the tests, each test should leave the DB in a clean state.
   * 
   * @throws Exception If a problem is encountered.
   */
  @BeforeClass
  public static void startDb() throws Exception {
    Server server = Server.newTestInstance();
    // TODO should loop over all DbImplementations once we have more than one
    TestDbManager.manager = new DbManager(server,
        "org.wattdepot.server.db.memory.MemoryStorageImplementation");
  }

  /**
   * Tests all of the methods relating to the User resource. Bundled into one method since it is
   * easier to test CRUD methods in sequence.
   */
  @Test
  public void testUsers() {
    User user1, user2;
    Properties userProps;

    // Database starts fresh with no Users. Might change if admin user is pre-added.
    assertTrue("Freshly created database contains users", manager.getUsers().getUserRef()
        .isEmpty());
    // Create a User to add to system
    userProps = new Properties();
    userProps.getProperty().add(makeUserProperty("aweseomness", "total"));
    user1 = makeUser("foo@example.com", "secret", false, userProps);
    // Store user1
    assertTrue("Unable to store a User in DB", manager.storeUser(user1));
    // Trying to store user1 again should fail
    assertFalse("Overwriting user succeeded, but should fail", manager.storeUser(user1));
    // Retrieve user1 and compare to existing value
    assertEquals("Retrieved user does not match original stored user", user1, manager
        .getUser(user1.getEmail()));
    // Confirm that getUsers returns a single UserRef now
    int usersListSize = manager.getUsers().getUserRef().size();
    assertSame("Number of Users = " + usersListSize + ", instead of 1.", usersListSize, 1);
    // Confirm that the single UserRef is from user1
    assertTrue("getUsers didn't return expected UserRef", userRefEqualsUser(manager.getUsers()
        .getUserRef().get(0), user1));
    // Now delete user1
    assertTrue("Unable to delete user1", manager.deleteUser(user1.getEmail()));
    // Try deleting user1 again
    assertFalse("Able to delete user1 a second time", manager.deleteUser(user1.getEmail()));
    // Should not be able to get deleted user
    assertNull("Able to retrieve deleted user", manager.getUser(user1.getEmail()));
    // Now put 2 users in DB
    user2 = makeUser("bar@example.com", "hidden", true, null);
    assertTrue("Unable to store a User in DB", manager.storeUser(user1));
    assertTrue("Unable to store a User in DB", manager.storeUser(user2));
    // Confirm that getUsers returns two UserRefs now
    usersListSize = manager.getUsers().getUserRef().size();
    assertSame("Number of Users = " + usersListSize + ", instead of 2.", usersListSize, 2);
    // Confirm that the two Users are user1 and user2
    List<UserRef> refs = manager.getUsers().getUserRef();
    if (userRefEqualsUser(refs.get(0), user1)) {
      assertTrue("UserRefs from getUsers do not match input Users", userRefEqualsUser(refs.get(1),
          user2));
    }
    else if (userRefEqualsUser(refs.get(0), user2)) {
      assertTrue("UserRefs from getUsers do not match input Users", userRefEqualsUser(refs.get(1),
          user1));
    }
    else {
      fail("UserRefs from getUsers do not match input Users");
    }
    // Retrieve user1 and compare to existing value
    assertEquals("Retrieved user does not match original stored user", user1, manager
        .getUser(user1.getEmail()));
    // Retrieve user2 and compare to existing value
    assertEquals("Retrieved user does not match original stored user", user2, manager
        .getUser(user2.getEmail()));
    // Shouldn't be able to retrieve non-existent user
    assertNull("Able to retrieve non-existent user", manager
        .getUser("boguslongusername@example.com"));
    // Try deleting non-existent user
    assertFalse("Able to delete non-existent user", manager
        .deleteUser("boguslongusername@example.com"));
    // Now delete user2
    assertTrue("Unable to delete user2", manager.deleteUser(user2.getEmail()));
    // Confirm that getUsers returns a single UserRef now
    usersListSize = manager.getUsers().getUserRef().size();
    assertSame("Number of Users = " + usersListSize + ", instead of 1.", usersListSize, 1);
    // Now delete user1
    assertTrue("Unable to delete user1", manager.deleteUser(user1.getEmail()));
    // Confirm no users in DB now
    assertTrue("After deleting all known users, users remain in DB", manager.getUsers()
        .getUserRef().isEmpty());
  }

  /**
   * Tests the performMaintenance method. All that really can be done is to call
   * performMaintenance() and expect it to return true.
   */
  @Test
  public void testPerformMaintenance() {
    assertTrue("Unable to perform DB maintenance", manager.performMaintenance());
  }

  /**
   * Tests the indexTables method. All that really can be done is to call indexTables() and expect
   * it to return true.
   */
  @Test
  public void testIndexTables() {
    assertTrue("Unable to index DB tables", manager.indexTables());
  }
}

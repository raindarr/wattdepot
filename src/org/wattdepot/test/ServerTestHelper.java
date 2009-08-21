package org.wattdepot.test;

import static org.wattdepot.server.ServerProperties.ADMIN_EMAIL_KEY;
import static org.wattdepot.server.ServerProperties.ADMIN_PASSWORD_KEY;
import org.junit.BeforeClass;
import org.wattdepot.server.Server;


/**
 * Provides helpful utility methods to WattDepot test classes, which will normally want to
 * extend this class. Portions of this code are adapted from
 * http://hackystat-sensorbase-uh.googlecode.com/
 *
 * @author Robert Brewer
 * @author Philip Johnson
 */

public class ServerTestHelper {
  /** The WattDepot server used in these tests. */
  protected static Server server = null;

  /** The admin email. */
  protected static String adminEmail;
  /** The admin password. */
  protected static String adminPassword;

  /**
   * Starts the server going for these tests. 
   * @throws Exception If problems occur setting up the server. 
   */
  @BeforeClass public static void setupServer() throws Exception {
    // Instantiate the server parameterized for testing purposes. 
    if (ServerTestHelper.server == null) {
      ServerTestHelper.server = Server.newTestInstance();
    }
    
    ServerTestHelper.adminEmail = server.getServerProperties().get(ADMIN_EMAIL_KEY);
    ServerTestHelper.adminPassword = server.getServerProperties().get(ADMIN_PASSWORD_KEY);
  }

//  /**
//   * Starts the server going for these tests. 
//   * @throws Exception If problems occur setting up the server. 
//   */
//  @AfterClass public static void shutdownServer() throws Exception {
//    // Instantiate the server parameterized for testing purposes. 
//    ServerTestHelper.server.shutdown();
//  }

  /**
   * Returns the hostname associated with this test server.
   * 
   * @return The host name, including the context root and ending in "/".
   */
  protected static String getHostName() {
    return ServerTestHelper.server.getHostName();
  }

}

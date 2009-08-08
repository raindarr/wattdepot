package org.wattdepot.test;

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
  protected static Server server;

  /**
   * Starts the server going for these tests. 
   * @throws Exception If problems occur setting up the server. 
   */
  @BeforeClass public static void setupServer() throws Exception {
    // Instantiate the server parameterized for testing purposes. 
    ServerTestHelper.server = Server.newTestInstance();
  }

  /**
   * Returns the hostname associated with this test server.
   * 
   * @return The host name, including the context root and ending in "/".
   */
  protected static String getHostName() {
    return ServerTestHelper.server.getHostName();
  }

}

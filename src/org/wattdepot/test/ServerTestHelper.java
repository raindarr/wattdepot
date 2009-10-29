package org.wattdepot.test;

import static org.wattdepot.resource.sensordata.SensorDataUtils.makeSensorData;
import static org.wattdepot.resource.sensordata.SensorDataUtils.makeSensorDataProperty;
import static org.wattdepot.server.ServerProperties.ADMIN_EMAIL_KEY;
import static org.wattdepot.server.ServerProperties.ADMIN_PASSWORD_KEY;
import org.wattdepot.util.tstamp.Tstamp;
import org.junit.BeforeClass;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.resource.source.jaxb.Source;
import org.wattdepot.server.Server;
import org.wattdepot.server.db.DbManager;

/**
 * Provides helpful utility methods to WattDepot resource test classes, which will normally want to
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
   * 
   * @throws Exception If problems occur setting up the server.
   */
  @BeforeClass
  public static void setupServer() throws Exception {
    // Instantiate the server parameterized for testing purposes.
    // if (ServerTestHelper.server == null) {
    ServerTestHelper.server = Server.newTestInstance();
    // }

    ServerTestHelper.adminEmail = server.getServerProperties().get(ADMIN_EMAIL_KEY);
    ServerTestHelper.adminPassword = server.getServerProperties().get(ADMIN_PASSWORD_KEY);
  }

  // /**
  // * Starts the server going for these tests.
  // * @throws Exception If problems occur setting up the server.
  // */
  // @AfterClass public static void shutdownServer() throws Exception {
  // // Instantiate the server parameterized for testing purposes.
  // ServerTestHelper.server.shutdown();
  // ServerTestHelper.server = null;
  // }

  /**
   * Returns the hostname associated with this test server.
   * 
   * @return The host name, including the context root and ending in "/".
   */
  protected static String getHostName() {
    return ServerTestHelper.server.getHostName();
  }

  /**
   * Creates a SensorData for use in testing, 1 in a series.
   * 
   * @return The freshly created SensorData object.
   * @throws Exception If there are problems converting timestamp string to XMLGregorianCalendar
   * (should never happen)
   */
  protected SensorData makeTestSensorData1() throws Exception {
    org.wattdepot.resource.sensordata.jaxb.Properties props =
      new org.wattdepot.resource.sensordata.jaxb.Properties();
    props.getProperty().add(makeSensorDataProperty("powerConsumed", "10000"));
    return makeSensorData(Tstamp.makeTimestamp("2009-07-28T09:00:00.000-10:00"), "JUnit",
        Source.sourceToUri(DbManager.defaultPublicSource, server), props);
  }
  
  /**
   * Creates a SensorData for use in testing, 2 in a series.
   * 
   * @return The freshly created SensorData object.
   * @throws Exception If there are problems converting timestamp string to XMLGregorianCalendar
   * (should never happen)
   */
  protected SensorData makeTestSensorData2() throws Exception {
    org.wattdepot.resource.sensordata.jaxb.Properties props =
      new org.wattdepot.resource.sensordata.jaxb.Properties();
    props.getProperty().add(makeSensorDataProperty("powerConsumed", "11000"));
    return makeSensorData(Tstamp.makeTimestamp("2009-07-28T09:15:00.000-10:00"), "FooTool",
        Source.sourceToUri(DbManager.defaultPublicSource, server), props);
  }

  /**
   * Creates a SensorData for use in testing, 3 in a series.
   * 
   * @return The freshly created SensorData object.
   * @throws Exception If there are problems converting timestamp string to XMLGregorianCalendar
   * (should never happen)
   */
  protected SensorData makeTestSensorData3() throws Exception {
    org.wattdepot.resource.sensordata.jaxb.Properties props =
      new org.wattdepot.resource.sensordata.jaxb.Properties();
    props.getProperty().add(makeSensorDataProperty("powerConsumed", "9500"));
    return makeSensorData(Tstamp.makeTimestamp("2009-07-28T09:30:00.000-10:00"), "JUnit",
        Source.sourceToUri(DbManager.defaultPublicSource, server), props);
  }

  /**
   * Creates a SensorData for use in testing, for the default private Source.
   * 
   * @return The freshly created SensorData object.
   * @throws Exception If there are problems converting timestamp string to XMLGregorianCalendar
   * (should never happen)
   */
  protected SensorData makeTestSensorDataPrivateSource() throws Exception {
    org.wattdepot.resource.sensordata.jaxb.Properties props =
      new org.wattdepot.resource.sensordata.jaxb.Properties();
    props.getProperty().add(makeSensorDataProperty("powerProduced", "3000"));
    return makeSensorData(Tstamp.makeTimestamp("2009-07-28T09:40:00.000-10:00"), "JUnit",
        Source.sourceToUri(DbManager.defaultPrivateSource, server), props);
  }
}

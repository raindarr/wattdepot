package org.wattdepot.server.db;

import static org.junit.Assert.assertTrue;
import static org.wattdepot.resource.sensordata.SensorDataUtils.makeSensorData;
import static org.wattdepot.resource.sensordata.SensorDataUtils.makeSensorDataProperty;
import org.junit.Before;
import org.junit.BeforeClass;
import org.wattdepot.resource.property.jaxb.Property;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.resource.source.jaxb.Source;
import org.wattdepot.resource.source.jaxb.SubSources;
import org.wattdepot.resource.user.jaxb.User;
import org.wattdepot.server.Server;
import org.wattdepot.util.tstamp.Tstamp;

/**
 * Provides helpful utility methods to DbManager test classes, which will normally want to extend
 * this class.
 * 
 * @author Robert Brewer
 */
public class DbManagerTestHelper {

  /**
   * The DbManager under test.
   */
  protected DbManager manager;

  /** The server being used for these tests. */
  protected static Server server;

  /**
   * Creates a test server to use for this set of tests. The DbManager is created separately.
   * 
   * @throws Exception If a problem is encountered.
   */
  @BeforeClass
  public static void startServer() throws Exception {
    DbManagerTestHelper.server = Server.newTestInstance();
  }

  /**
   * Creates a fresh DbManager for each test. This might prove too expensive for some
   * DbImplementations, but we'll cross that bridge when we get there.
   */
  @Before
  public void makeDb() {
    // TODO should loop over all DbImplementations once we have more than one
    this.manager =
        new DbManager(server, "org.wattdepot.server.db.memory.MemoryStorageImplementation", true);
    // Need to create default data for each fresh DbManager
    assertTrue("Unable to create default data", this.manager.createDefaultData());
  }

  /**
   * Creates a user for use in testing, 1 in a series.
   * 
   * @return The freshly created User object.
   */
  protected User makeTestUser1() {
    User user = new User("foo@example.com", "secret", false, null);
    user.addProperty(new Property("aweseomness", "total"));
    return user;
  }

  /**
   * Creates a user for use in testing, 2 in a series.
   * 
   * @return The freshly created User object.
   */
  protected User makeTestUser2() {
    return new User("bar@example.com", "hidden", true, null);
  }

  /**
   * Creates a user for use in testing, 3 in a series.
   * 
   * @return The freshly created User object.
   */
  protected User makeTestUser3() {
    return new User("baz@example.com", "extra-secret", false, null);
  }

  /**
   * Creates a Source for use in testing, 1 in a series.
   * 
   * @return The freshly created Source object.
   */
  protected Source makeTestSource1() {
    Source source =
        new Source("hale-foo", makeTestUser1().toUri(server), true, false,
            "21.30078,-157.819129,41", "Made up location", "Obvius-brand power meter", null, null);
    source.addProperty(new Property("carbonIntensity", "294"));
    return source;
  }

  /**
   * Creates a Source for use in testing, 2 in a series.
   * 
   * @return The freshly created Source object.
   */
  protected Source makeTestSource2() {
    Source source =
        new Source("hale-bar", makeTestUser2().toUri(server), false, false,
            "31.30078,-157.819129,41", "Made up location 2", "Bogus-brand power meter", null, null);
    source.addProperty(new Property("carbonIntensity", "128"));
    return source;
  }

  /**
   * Creates a virtual Source for use in testing, 3 in a series.
   * 
   * @return The freshly created Source object.
   */
  protected Source makeTestSource3() {
    SubSources subSources = new SubSources();
    subSources.getHref().add(makeTestSource1().toUri(server));
    subSources.getHref().add(makeTestSource2().toUri(server));
    return new Source("virtual-hales", makeTestUser3().toUri(server), false, true,
        "31.30078,-157.819129,41", "Made up location 3", "Virtual source", null, subSources);
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
        makeTestSource1().toUri(server), props);
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
        makeTestSource1().toUri(server), props);
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
        makeTestSource1().toUri(server), props);
  }
}

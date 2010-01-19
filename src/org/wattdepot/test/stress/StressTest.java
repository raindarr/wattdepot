package org.wattdepot.test.stress;

import static org.wattdepot.server.ServerProperties.ADMIN_EMAIL_KEY;
import static org.wattdepot.server.ServerProperties.ADMIN_PASSWORD_KEY;
import java.util.Date;
import javax.xml.datatype.XMLGregorianCalendar;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wattdepot.client.WattDepotClient;
import org.wattdepot.server.Server;
import org.wattdepot.server.db.DbManager;
import org.wattdepot.test.DataGenerator;
import org.wattdepot.util.tstamp.Tstamp;

/**
 * Test the WattDepot server in ways that are designed to make it do hard things (like interpolate
 * and sum up virtual sources).
 * 
 * @author Robert Brewer
 */
public class StressTest {

  /** The WattDepot server used in these tests. */
  protected static Server server = null;
  /** The DbManager used by these tests. */
  protected static DbManager manager;

  /** The admin email. */
  protected static String adminEmail;
  /** The admin password. */
  protected static String adminPassword;

  private WattDepotClient client;

  /**
   * Starts the server going for these tests.
   * 
   * @throws Exception If problems occur setting up the server.
   */
  @BeforeClass
  public static void setupServer() throws Exception {
    StressTest.server = Server.newTestInstance();

    StressTest.adminEmail = server.getServerProperties().get(ADMIN_EMAIL_KEY);
    StressTest.adminPassword = server.getServerProperties().get(ADMIN_PASSWORD_KEY);

    StressTest.manager = (DbManager) server.getContext().getAttributes().get("DbManager");
    String adminUserUri = manager.getUser(adminEmail).toUri(server);
    DataGenerator test = new DataGenerator(manager, adminUserUri, server);
    System.out.print("Creating test data...");
    test.storeData(Tstamp.makeTimestamp("2010-01-08T00:00:00.000-10:00"), Tstamp
        .makeTimestamp("2010-01-09T00:00:00.000-10:00"), 5);
    System.out.println("done");
  }

  /**
   * Creates new object and creates a client for testing.
   */
  public StressTest() {
    super();
    this.client = new WattDepotClient(StressTest.server.getHostName());
  }

  /**
   * Computes interpolated power for a single source many times and reports how long it took.
   * 
   * @throws Exception If there are problems.
   */
  @Test
  @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
  public void testInterpolatedPowerSingleSource() throws Exception {
    XMLGregorianCalendar timestamp = Tstamp.makeTimestamp("2010-01-08T12:03:07.000-10:00");
    Date testStart = new Date();
    int iterations = 100;
    for (int i = 0; i < iterations; i++) {
      client.getPowerGenerated(DataGenerator.getSourceName(0), timestamp);
    }
    Date testEnd = new Date();
    double msElapsed = testEnd.getTime() - testStart.getTime();
    System.out.format("Time to calculate interpolated power generated %d times: %.1f ms%n",
        iterations, msElapsed);
    System.out.format("Mean time to calculate interpolated power: %.1f ms%n", msElapsed
        / iterations);
  }

  /**
   * Computes interpolated power for a single source many times and reports how long it took.
   * 
   * @throws Exception If there are problems.
   */
  @Test
  @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
  public void testInterpolatedPowerVirtualSource() throws Exception {
    XMLGregorianCalendar timestamp = Tstamp.makeTimestamp("2010-01-08T12:03:07.000-10:00");
    Date testStart = new Date();
    int iterations = 100;
    for (int i = 0; i < iterations; i++) {
      client.getPowerGenerated(DataGenerator.source11Name, timestamp);
    }
    Date testEnd = new Date();
    double msElapsed = testEnd.getTime() - testStart.getTime();
    System.out.format(
        "Time to calculate interpolated power for virtual source generated %d times: %.1f ms%n",
        iterations, msElapsed);
    System.out.format("Mean time to calculate interpolated power for virtual source: %.1f ms%n",
        msElapsed / iterations);
  }
}

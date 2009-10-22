package org.wattdepot.resource.source.summary;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import org.wattdepot.client.NotAuthorizedException;
import org.wattdepot.client.WattDepotClient;
import org.wattdepot.client.WattDepotClientException;
import org.wattdepot.resource.source.summary.jaxb.SourceSummary;
import org.wattdepot.server.db.DbManager;
import org.wattdepot.test.ServerTestHelper;

/**
 * Tests the SourceSummary resource API at the HTTP level using WattDepotClient.
 * 
 * @author Robert Brewer
 */
public class TestSourceSummaryResource extends ServerTestHelper {

  private static final String PUBLIC_SUMMARY_NOT_FOUND = "Expected public SourceSummary not found";

  /** Holds SourceRefs of the default Sources for comparison in test cases. */
  private SourceSummary publicSourceSummary, privateSourceSummary;

  /**
   * Creates a fresh DbManager for each test. This might prove too expensive for some
   * DbImplementations, but we'll cross that bridge when we get there.
   */
  @Before
  public void makeDb() {
    DbManager manager =
        new DbManager(server, "org.wattdepot.server.db.memory.MemoryStorageImplementation", true);
    // Need to create default data for each fresh DbManager
    assertTrue("Unable to create default data", manager.createDefaultData());
    this.publicSourceSummary = manager.getSourceSummary(DbManager.defaultPublicSource);
    this.privateSourceSummary = manager.getSourceSummary(DbManager.defaultPrivateSource);
    server.getContext().getAttributes().put("DbManager", manager);
  }

  // Tests for GET {host}/sources/{source}/summary

  /**
   * Tests retrieval of public SourceSummary. Type: no credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test
  public void testPublicSourceWithNoCredentials() throws WattDepotClientException {
    WattDepotClient client = new WattDepotClient(getHostName());
    assertEquals(PUBLIC_SUMMARY_NOT_FOUND, client.getSourceSummary(DbManager.defaultPublicSource),
        this.publicSourceSummary);
  }

  /**
   * Tests retrieval of private SourceSummary. Type: no credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test(expected = NotAuthorizedException.class)
  public void testPrivateSourceWithNoCredentials() throws WattDepotClientException {
    WattDepotClient client = new WattDepotClient(getHostName());
    client.getSourceSummary(DbManager.defaultPrivateSource);
    fail("Able to get private SourceSummary with no credentials");
  }

  /**
   * Tests retrieval of public SourceSummary. Type: invalid credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test(expected = NotAuthorizedException.class)
  public void testPublicSourceBadAuth() throws WattDepotClientException {
    // Shouldn't authenticate with invalid username or password
    WattDepotClient client = new WattDepotClient(getHostName(), adminEmail, "foo");
    client.getSourceSummary(DbManager.defaultPublicSource);
    fail("Able to get SourceSummary with invalid credentials");
  }

  /**
   * Tests retrieval of private SourceSummary. Type: invalid credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test(expected = NotAuthorizedException.class)
  public void testPrivateSourceBadAuth() throws WattDepotClientException {
    // Shouldn't authenticate with invalid username or password
    WattDepotClient client = new WattDepotClient(getHostName(), adminEmail, "foo");
    client.getSourceSummary(DbManager.defaultPrivateSource);
    fail("Able to get SourceSummary with invalid credentials");
  }

  /**
   * Tests retrieval of public & private SourceSummaries. Type: valid admin credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test
  public void testSourceWithAdminCredentials() throws WattDepotClientException {
    WattDepotClient client = new WattDepotClient(getHostName(), adminEmail, adminPassword);
    assertEquals(PUBLIC_SUMMARY_NOT_FOUND, client.getSourceSummary(DbManager.defaultPublicSource),
        this.publicSourceSummary);
    assertEquals("Expected private source not found", client
        .getSourceSummary(DbManager.defaultPrivateSource), this.privateSourceSummary);
  }

  /**
   * Tests retrieval of public & private SourceSummaries. Type: valid owner credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test
  public void testSourceWithOwnerCredentials() throws WattDepotClientException {
    WattDepotClient client =
        new WattDepotClient(getHostName(), DbManager.defaultOwnerUsername,
            DbManager.defaultOwnerPassword);
    assertEquals(PUBLIC_SUMMARY_NOT_FOUND, client.getSourceSummary(DbManager.defaultPublicSource),
        this.publicSourceSummary);
    assertEquals("Expected private source not found", client
        .getSourceSummary(DbManager.defaultPrivateSource), this.privateSourceSummary);
  }

  /**
   * Tests retrieval of public SourceSummary. Type: valid non-owner credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test
  public void testPublicSourceWithNonOwnerCredentials() throws WattDepotClientException {
    WattDepotClient client =
        new WattDepotClient(getHostName(), DbManager.defaultNonOwnerUsername,
            DbManager.defaultNonOwnerPassword);
    assertEquals(PUBLIC_SUMMARY_NOT_FOUND, client.getSourceSummary(DbManager.defaultPublicSource),
        this.publicSourceSummary);
  }

  /**
   * Tests retrieval of private SourceSummary. Type: valid non-owner credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test(expected = NotAuthorizedException.class)
  public void testPrivateSourceWithNonOwnerCredentials() throws WattDepotClientException {
    WattDepotClient client =
        new WattDepotClient(getHostName(), DbManager.defaultNonOwnerUsername,
            DbManager.defaultNonOwnerPassword);
    client.getSourceSummary(DbManager.defaultPrivateSource);
    fail("Able to get private SourceSummary with non-owner credentials");
  }
}

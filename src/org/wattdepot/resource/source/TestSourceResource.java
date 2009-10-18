package org.wattdepot.resource.source;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.wattdepot.client.NotAuthorizedException;
import org.wattdepot.client.WattDepotClient;
import org.wattdepot.client.WattDepotClientException;
import org.wattdepot.resource.source.jaxb.Source;
import org.wattdepot.resource.source.jaxb.SourceIndex;
import org.wattdepot.resource.source.jaxb.SourceRef;
import org.wattdepot.server.db.DbManager;
import org.wattdepot.test.ServerTestHelper;

/**
 * Tests the Source resource API at the HTTP level using WattDepotClient.
 * 
 * @author Robert Brewer
 */
public class TestSourceResource extends ServerTestHelper {

  private static final String PUBLIC_SOURCE_NOT_FOUND = "Expected public source not found";

  /** Holds the default Sources for comparison in test cases. */
  private Source publicSource, privateSource;

  /** Holds SourceRefs of the default Sources for comparison in test cases. */
  private SourceRef publicSourceRef, privateSourceRef;

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
    this.publicSource = manager.getSource(DbManager.defaultPublicSource);
    this.publicSourceRef = SourceUtils.makeSourceRef(this.publicSource, server);
    this.privateSource = manager.getSource(DbManager.defaultPrivateSource);
    this.privateSourceRef = SourceUtils.makeSourceRef(this.privateSource, server);
    server.getContext().getAttributes().put("DbManager", manager);
  }

  // Tests for GET {host}/sources

  /**
   * Tests retrieval of all Sources. Type: no credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test
  public void testFullIndexWithNoCredentials() throws WattDepotClientException {
    WattDepotClient client = new WattDepotClient(getHostName(), null, null);
    // index should just have the public source
    assertEquals(PUBLIC_SOURCE_NOT_FOUND, client.getSourceIndex().getSourceRef().get(0),
        this.publicSourceRef);
  }

  /**
   * Tests retrieval of all Sources. Type: invalid credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test(expected = NotAuthorizedException.class)
  public void testFullIndexBadAuth() throws WattDepotClientException {
    // Shouldn't authenticate with invalid username or password
    WattDepotClient client = new WattDepotClient(getHostName(), adminEmail, "foo");
    client.getSourceIndex();
    fail("Able to get SourceIndex with invalid credentials");
  }

  /**
   * Tests retrieval of all Sources. Type: valid admin credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test
  public void testFullIndexWithAdminCredentials() throws WattDepotClientException {
    WattDepotClient client = new WattDepotClient(getHostName(), adminEmail, adminPassword);
    SourceIndex index = new SourceIndex();
    index.getSourceRef().add(this.publicSourceRef);
    index.getSourceRef().add(this.privateSourceRef);
    // We added in sorted order, but just for safety's sake we sort the list
    Collections.sort(index.getSourceRef());
    // Admin should see both sources
    assertEquals("Expected sources not found", client.getSourceIndex().getSourceRef(), index
        .getSourceRef());
  }

  /**
   * Tests retrieval of all Sources. Type: valid owner credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test
  public void testFullIndexWithOwnerCredentials() throws WattDepotClientException {
    WattDepotClient client =
        new WattDepotClient(getHostName(), DbManager.defaultOwnerUsername,
            DbManager.defaultOwnerPassword);
    SourceIndex index = new SourceIndex();
    index.getSourceRef().add(this.publicSourceRef);
    index.getSourceRef().add(this.privateSourceRef);
    // We added in sorted order, but just for safety's sake we sort the list
    Collections.sort(index.getSourceRef());
    // Owner should see both sources
    assertEquals("Expected sources not found", client.getSourceIndex().getSourceRef(), index
        .getSourceRef());
  }

  /**
   * Tests retrieval of all Sources. Type: valid non-owner credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test
  public void testFullIndexWithNonOwnerCredentials() throws WattDepotClientException {
    WattDepotClient client =
        new WattDepotClient(getHostName(), DbManager.defaultNonOwnerUsername,
            DbManager.defaultNonOwnerPassword);
    // index should just have the public source
    assertEquals(PUBLIC_SOURCE_NOT_FOUND, client.getSourceIndex().getSourceRef().get(0),
        this.publicSourceRef);
  }

  // Tests for GET {host}/sources/{source}

  /**
   * Tests retrieval of public Source. Type: no credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test
  public void testPublicSourceWithNoCredentials() throws WattDepotClientException {
    WattDepotClient client = new WattDepotClient(getHostName(), null, null);
    assertEquals(PUBLIC_SOURCE_NOT_FOUND, client.getSource(publicSource.getName()),
        this.publicSource);
  }

  /**
   * Tests retrieval of private Source. Type: no credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test(expected = NotAuthorizedException.class)
  public void testPrivateSourceWithNoCredentials() throws WattDepotClientException {
    WattDepotClient client = new WattDepotClient(getHostName(), null, null);
    client.getSource(privateSource.getName());
    fail("Able to get private Source with no credentials");
  }

  /**
   * Tests retrieval of public Source. Type: invalid credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test(expected = NotAuthorizedException.class)
  public void testPublicSourceBadAuth() throws WattDepotClientException {
    // Shouldn't authenticate with invalid username or password
    WattDepotClient client = new WattDepotClient(getHostName(), adminEmail, "foo");
    client.getSource(publicSource.getName());
    fail("Able to get Source with invalid credentials");
  }

  /**
   * Tests retrieval of private Source. Type: invalid credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test(expected = NotAuthorizedException.class)
  public void testPrivateSourceBadAuth() throws WattDepotClientException {
    // Shouldn't authenticate with invalid username or password
    WattDepotClient client = new WattDepotClient(getHostName(), adminEmail, "foo");
    client.getSource(privateSource.getName());
    fail("Able to get Source with invalid credentials");
  }

  /**
   * Tests retrieval of public & private Source. Type: valid admin credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test
  public void testSourceWithAdminCredentials() throws WattDepotClientException {
    WattDepotClient client = new WattDepotClient(getHostName(), adminEmail, adminPassword);
    assertEquals(PUBLIC_SOURCE_NOT_FOUND, client.getSource(publicSource.getName()),
        this.publicSource);
    assertEquals("Expected private source not found", client.getSource(privateSource.getName()),
        this.privateSource);
  }

  /**
   * Tests retrieval of public & private Source. Type: valid owner credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test
  public void testSourceWithOwnerCredentials() throws WattDepotClientException {
    WattDepotClient client =
        new WattDepotClient(getHostName(), DbManager.defaultOwnerUsername,
            DbManager.defaultOwnerPassword);
    assertEquals(PUBLIC_SOURCE_NOT_FOUND, client.getSource(publicSource.getName()),
        this.publicSource);
    assertEquals("Expected private source not found", client.getSource(privateSource.getName()),
        this.privateSource);
  }

  /**
   * Tests retrieval of public Source. Type: valid non-owner credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test
  public void testPublicSourceWithNonOwnerCredentials() throws WattDepotClientException {
    WattDepotClient client =
        new WattDepotClient(getHostName(), DbManager.defaultNonOwnerUsername,
            DbManager.defaultNonOwnerPassword);
    assertEquals(PUBLIC_SOURCE_NOT_FOUND, client.getSource(publicSource.getName()),
        this.publicSource);
  }

  /**
   * Tests retrieval of private Source. Type: valid non-owner credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test(expected = NotAuthorizedException.class)
  public void testPrivateSourceWithNonOwnerCredentials() throws WattDepotClientException {
    WattDepotClient client =
        new WattDepotClient(getHostName(), DbManager.defaultNonOwnerUsername,
            DbManager.defaultNonOwnerPassword);
    client.getSource(privateSource.getName());
    fail("Able to get private Source with non-owner credentials");
  }
}

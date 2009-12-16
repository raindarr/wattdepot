package org.wattdepot.resource.source;

import static org.junit.Assert.assertEquals;
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
import org.wattdepot.test.ServerTestHelper;

/**
 * Tests the Source resource API at the HTTP level using WattDepotClient.
 * 
 * @author Robert Brewer
 */
public class TestSourceResource extends ServerTestHelper {

  private static final String PUBLIC_SOURCE_NOT_FOUND = "Expected public source not found";

  /** Holds the default Sources for comparison in test cases. */
  private Source publicSource, privateSource, virtualSource;

  /** Holds SourceRefs of the default Sources for comparison in test cases. */
  private SourceRef publicSourceRef, privateSourceRef, virtualSourceRef;

  /**
   * Initializes variables used for tests.
   */
  @Before
  public void initializeVars() {
    this.publicSource = manager.getSource(defaultPublicSource);
    this.publicSourceRef = new SourceRef(this.publicSource, server);
    this.privateSource = manager.getSource(defaultPrivateSource);
    this.privateSourceRef = new SourceRef(this.privateSource, server);
    this.virtualSource = manager.getSource(defaultVirtualSource);
    this.virtualSourceRef = new SourceRef(this.virtualSource, server);
  }

  // Tests for GET {host}/sources

  /**
   * Tests retrieval of all Sources. Type: no credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test
  public void testFullIndexWithNoCredentials() throws WattDepotClientException {
    WattDepotClient client = new WattDepotClient(getHostName());
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
    index.getSourceRef().add(this.virtualSourceRef);
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
        new WattDepotClient(getHostName(), defaultOwnerUsername,
            defaultOwnerPassword);
    SourceIndex index = new SourceIndex();
    index.getSourceRef().add(this.publicSourceRef);
    index.getSourceRef().add(this.privateSourceRef);
    index.getSourceRef().add(this.virtualSourceRef);
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
        new WattDepotClient(getHostName(), defaultNonOwnerUsername,
            defaultNonOwnerPassword);
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
    WattDepotClient client = new WattDepotClient(getHostName());
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
    WattDepotClient client = new WattDepotClient(getHostName());
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
        new WattDepotClient(getHostName(), defaultOwnerUsername,
            defaultOwnerPassword);
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
        new WattDepotClient(getHostName(), defaultNonOwnerUsername,
            defaultNonOwnerPassword);
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
        new WattDepotClient(getHostName(), defaultNonOwnerUsername,
            defaultNonOwnerPassword);
    client.getSource(privateSource.getName());
    fail("Able to get private Source with non-owner credentials");
  }
}

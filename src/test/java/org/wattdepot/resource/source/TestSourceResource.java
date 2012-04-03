package org.wattdepot.resource.source;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.bind.JAXBException;
import org.junit.Before;
import org.junit.Test;
import org.wattdepot.client.OverwriteAttemptedException;
import org.wattdepot.client.WattDepotClient;
import org.wattdepot.client.WattDepotClientException;
import org.wattdepot.resource.property.jaxb.Property;
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
    assertEquals(PUBLIC_SOURCE_NOT_FOUND, client.getSources().get(0), this.publicSource);
  }

  /**
   * Helper method to test that the full index was received using the client given.
   * 
   * @param client client to be tested.
   * @throws WattDepotClientException If problems are encountered
   */
  private void runFullIndex(WattDepotClient client) throws WattDepotClientException {
    SourceIndex index = new SourceIndex();
    index.getSourceRef().add(this.publicSourceRef);
    index.getSourceRef().add(this.privateSourceRef);
    index.getSourceRef().add(this.virtualSourceRef);
    // We added in sorted order, but just for safety's sake we sort the list
    Collections.sort(index.getSourceRef());
    // Admin should see both sources
    assertEquals("Expected sources not found", client.getSourceIndex().getSourceRef(),
        index.getSourceRef());

    List<Source> sourceList = new ArrayList<Source>();
    sourceList.add(this.publicSource);
    sourceList.add(this.privateSource);
    sourceList.add(this.virtualSource);
    // We added in sorted order, but just for safety's sake we sort the list
    Collections.sort(sourceList);
    // Admin should see both sources
    assertEquals("Expected sources not found", client.getSources(), sourceList);
  }

  /**
   * Tests retrieval of all Sources. Type: valid admin credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test
  @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
  public void testFullIndexWithAdminCredentials() throws WattDepotClientException {
    WattDepotClient client = new WattDepotClient(getHostName(), adminEmail, adminPassword);
    runFullIndex(client);
  }

  /**
   * Tests retrieval of all Sources. Type: valid owner credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test
  @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
  public void testFullIndexWithOwnerCredentials() throws WattDepotClientException {
    WattDepotClient client =
        new WattDepotClient(getHostName(), defaultOwnerUsername, defaultOwnerPassword);
    runFullIndex(client);
  }

  /**
   * Tests retrieval of all Sources. Type: valid non-owner credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test
  public void testFullIndexWithNonOwnerCredentials() throws WattDepotClientException {
    WattDepotClient client =
        new WattDepotClient(getHostName(), defaultNonOwnerUsername, defaultNonOwnerPassword);
    // index should just have the public source
    assertEquals(PUBLIC_SOURCE_NOT_FOUND, client.getSourceIndex().getSourceRef().get(0),
        this.publicSourceRef);
    assertEquals(PUBLIC_SOURCE_NOT_FOUND, client.getSources().get(0), this.publicSource);
  }

  // Tests for GET {host}/sources/{source}
  // Authorization tests are taken care of in WattDepotResource. Here just make sure the right
  // source comes back.

  /**
   * Tests retrieval of public Source.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test
  public void testPublicSource() throws WattDepotClientException {
    WattDepotClient client = new WattDepotClient(getHostName());
    assertEquals(PUBLIC_SOURCE_NOT_FOUND, client.getSource(publicSource.getName()),
        this.publicSource);
  }

  /**
   * Tests retrieval of private Source.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test
  public void testPrivateSource() throws WattDepotClientException {
    WattDepotClient client =
        new WattDepotClient(getHostName(), defaultOwnerUsername, defaultOwnerPassword);
    assertEquals("Expected private source not found", client.getSource(privateSource.getName()),
        privateSource);
  }

  // Tests for PUT {host}/sources/{source}?overwrite={overwrite}

  /**
   * Tests storing of Source that already exists without overwrite flag. Type: valid owner
   * credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   * @throws JAXBException If problems are encountered with XML
   */
  @Test(expected = OverwriteAttemptedException.class)
  public void testSourcePut() throws WattDepotClientException, JAXBException {
    WattDepotClient client =
        new WattDepotClient(getHostName(), defaultOwnerUsername, defaultOwnerPassword);
    assertFalse("Able to overwrite existing Source without overwrite flag",
        client.storeSource(publicSource, false));
  }

  /**
   * Tests storing of Source that already exists without overwrite flag. Type: valid owner
   * credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   * @throws JAXBException If problems are encountered with XML
   */
  @Test
  public void testSourcePutOverwrite() throws WattDepotClientException, JAXBException {
    WattDepotClient client =
        new WattDepotClient(getHostName(), defaultOwnerUsername, defaultOwnerPassword);
    assertTrue("Unable to overwrite existing Source even with overwrite flag",
        client.storeSource(publicSource, true));
    Source replacementSource =
        new Source(defaultPublicSource, publicSource.getOwner(), true, false,
            "21.30078,-157.819129,41",
            "Saunders Hall on the University of Hawaii at Manoa campus",
            "Obvius-brand power meter", null, null);
    replacementSource.addProperty(new Property(Source.SUPPORTS_ENERGY_COUNTERS, "true"));
    assertTrue("Property not added to source",
        replacementSource.isPropertyTrue(Source.SUPPORTS_ENERGY_COUNTERS));
    assertTrue("Unable to overwrite existing Source even with overwrite flag",
        client.storeSource(replacementSource, true));
    assertEquals("Retrieved overwritten Source does not match input value", replacementSource,
        client.getSource(defaultPublicSource));
  }
}
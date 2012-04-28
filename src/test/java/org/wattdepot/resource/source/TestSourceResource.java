package org.wattdepot.resource.source;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.bind.JAXBException;
import org.junit.Before;
import org.junit.Test;
import org.wattdepot.client.MiscClientException;
import org.wattdepot.client.NotAuthorizedException;
import org.wattdepot.client.OverwriteAttemptedException;
import org.wattdepot.client.ResourceNotFoundException;
import org.wattdepot.client.WattDepotClient;
import org.wattdepot.client.WattDepotClientException;
import org.wattdepot.resource.property.jaxb.Property;
import org.wattdepot.resource.source.jaxb.Source;
import org.wattdepot.resource.source.jaxb.SourceIndex;
import org.wattdepot.resource.source.jaxb.SourceRef;
import org.wattdepot.resource.source.jaxb.SubSources;
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

  // Tests for PUT {host}/sources/{source}

  /**
   * Tests storing of a Source. Type: valid owner credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   * @throws JAXBException If problems are encountered with XML
   */
  @Test
  public void testSourcePut() throws WattDepotClientException, JAXBException {
    WattDepotClient client =
        new WattDepotClient(getHostName(), defaultOwnerUsername, defaultOwnerPassword);
    String newSourceName = "new-source";
    Source newSource =
        new Source(newSourceName, publicSource.getOwner(), true, false, "0,0,100", "Null Island",
            "Just a test source", null, null);
    newSource.addProperty(new Property(Source.SUPPORTS_ENERGY_COUNTERS, "true"));
    assertTrue("Property not added to source",
        newSource.isPropertyTrue(Source.SUPPORTS_ENERGY_COUNTERS));
    assertTrue("Unable to create new Source", client.storeSource(newSource, false));
    assertEquals("Retrieved new Source does not match input value", newSource,
        client.getSource(newSourceName));
  }

  /**
   * Tests storing of Source with a non-existant SubSource. Type: valid owner credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   * @throws JAXBException If problems are encountered with XML
   */
  @Test(expected = MiscClientException.class)
  public void testSourceHierarchy() throws WattDepotClientException, JAXBException {
    WattDepotClient client =
        new WattDepotClient(getHostName(), defaultOwnerUsername, defaultOwnerPassword);
    SubSources subsources = new SubSources();
    subsources.getHref().add("http://localhost:8182/wattdepot/sources/bogus-source");
    Source newSource =
        new Source("new-source", publicSource.getOwner(), true, true, "", "", "", null, subsources);
    assertFalse("Able to store source with bogus subsource", client.storeSource(newSource, false));
    assertNull("Able to retrieve source with bogus subsource",
        client.getSource(newSource.getName()));
  }

  /**
   * Tests storing of Source with a non-existant owner. Type: valid owner credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   * @throws JAXBException If problems are encountered with XML
   */
  @Test(expected = MiscClientException.class)
  public void testSourceUser() throws WattDepotClientException, JAXBException {
    WattDepotClient client =
        new WattDepotClient(getHostName(), defaultOwnerUsername, defaultOwnerPassword);
    Source newSource =
        new Source("new-source", "http://localhost:8182/wattdepot/users/bogus-user", true, false,
            "", "", "", null, null);
    assertFalse("Able to store source with bogus owner", client.storeSource(newSource, false));
    assertNull("Able to retrieve source with bogus owner", client.getSource(newSource.getName()));
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
  public void testSourcePutNoOverwrite() throws WattDepotClientException, JAXBException {
    WattDepotClient client =
        new WattDepotClient(getHostName(), defaultOwnerUsername, defaultOwnerPassword);
    assertFalse("Able to overwrite existing Source without overwrite flag",
        client.storeSource(publicSource, false));
  }

  /**
   * Tests storing of Source that already exists with overwrite flag. Type: valid owner credentials.
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

  /**
   * Tests deleting a souce.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test(expected = ResourceNotFoundException.class)
  public void testSourceDelete() throws WattDepotClientException {
    WattDepotClient client =
        new WattDepotClient(getHostName(), defaultOwnerUsername, defaultOwnerPassword);
    assertTrue("Unable to delete source", client.deleteSource(defaultPublicSource));
    assertNull("Able to retrieve deleted source", client.getSource(defaultPublicSource));
  }

  /**
   * Tests deleting a souce that has already been deleted.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test(expected = ResourceNotFoundException.class)
  public void testSourceDoubleDelete() throws WattDepotClientException {
    WattDepotClient client =
        new WattDepotClient(getHostName(), defaultOwnerUsername, defaultOwnerPassword);
    assertTrue("Unable to delete source", client.deleteSource(defaultPublicSource));
    assertFalse("Able to delete already deleted source", client.deleteSource(defaultPublicSource));
  }

  
  /**
   * Tests attempting to delete a source from a non-owner.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test(expected = NotAuthorizedException.class)
  public void testSourceDeleteNonOwner() throws WattDepotClientException {
    WattDepotClient client =
        new WattDepotClient(getHostName(), defaultOwnerUsername, defaultOwnerPassword);

    client = new WattDepotClient(getHostName(), defaultNonOwnerUsername, defaultNonOwnerPassword);
    assertFalse("Able to delete source from non-owner", client.deleteSource(defaultPublicSource));
  }

}
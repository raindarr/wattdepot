package org.wattdepot.resource.source.summary;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.wattdepot.client.WattDepotClient;
import org.wattdepot.client.WattDepotClientException;
import org.wattdepot.test.ServerTestHelper;

/**
 * Tests the SourceSummary resource API at the HTTP level using WattDepotClient.
 * 
 * @author Robert Brewer
 */
public class TestSourceSummaryResource extends ServerTestHelper {

  // Tests for GET {host}/sources/{source}/summary
  // Authorization tests are taken care of in WattDepotResource.

  /**
   * Tests retrieval of public SourceSummary.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test
  public void testPublicSource() throws WattDepotClientException {
    WattDepotClient client = new WattDepotClient(getHostName());
    assertEquals("Expected public SourceSummary not found",
        client.getSourceSummary(defaultPublicSource),
        manager.getSourceSummary(defaultPublicSource));
  }

  /**
   * Tests retrieval of private SourceSummary.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test
  public void testPrivateSource() throws WattDepotClientException {
    WattDepotClient client =
        new WattDepotClient(getHostName(), defaultOwnerUsername, defaultOwnerPassword);
    assertEquals("Expected private source not found",
        client.getSourceSummary(defaultPrivateSource),
        manager.getSourceSummary(defaultPrivateSource));
  }

}

package org.wattdepot.resource.health;

import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.Component;
import org.restlet.data.Protocol;
import org.wattdepot.client.WattDepotClient;
import org.wattdepot.server.Server;

/**
 * Tests the Ping API, which is very simple.
 * 
 * @author Robert Brewer
 */
public class TestHealthResource {

  /**
   * Test that GET {host}/ping returns the hello world text.
   * 
   * @throws Exception If problems occur.
   */
  @Test
  public void testResource() throws Exception {
    WattDepotClient client = new WattDepotClient("http://127.0.0.1:8182/");
    assertTrue("Server is unhealthy", client.isHealthy());
    assertTrue("Unexpected server message",
        client.getHealthString().equals(HealthResource.HEALTH_MESSAGE_TEXT));
  }
  
  /**
   * Starts the server going for these tests. 
   * @throws Exception If problems occur setting up the server. 
   */
  @BeforeClass public static void setupServer() throws Exception {
    // Create a new Component.
    Component component = new Component();

    // Add a new HTTP server listening on port 8182.
    component.getServers().add(Protocol.HTTP, Server.SERVER_PORT);

    // Attach the sample application.
    component.getDefaultHost().attach(Server.URI_ROOT, new Server());

    // Start the component.
    component.start();
  }
}

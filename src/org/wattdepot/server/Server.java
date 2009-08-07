package org.wattdepot.server;

import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Restlet;
import org.restlet.Router;
import org.restlet.data.Protocol;
import org.wattdepot.resource.health.HealthResource;

/**
 * Sets up HTTP server and routes requests to appropriate resources.
 * 
 * @author Robert Brewer
 */
public class Server extends Application {

  /**
   * TCP port that the HTTP server will run on. 
   */
  public static final int SERVER_PORT = 8182;
  
  /**
   * Root of all URIs running on this server.
   */
  public static final String URI_ROOT = "/wattdepot";
  
  /**
   * URI for the health resource.
   */
  public static final String HEALTH_URI = "/health";
  
  /**
   * Starts up the web server to respond to ping requests.
   * 
   * @param args String array of command line arguments that are ignored
   */
  public static void main(String[] args) {
    // Create a new Component.
    Component component = new Component();

    // Add a new HTTP server listening on server port.
    component.getServers().add(Protocol.HTTP, SERVER_PORT);

    // Attach the sample application.
    component.getDefaultHost().attach(URI_ROOT, new Server());

    // Start the component.
    try {
      component.start();
    }
    catch (Exception e) {
      System.err.println("Unable to start main component, we are hosed.");
    }
  }
  
  /**
   * Creates a root Restlet that will receive all incoming calls.
   * 
   * @return the newly created Restlet object
   */
  @Override
  public synchronized Restlet createRoot() {
    Router router = new Router(getContext());

    // Defines only one route
    router.attach(HEALTH_URI, HealthResource.class);

    return router;
  }

}

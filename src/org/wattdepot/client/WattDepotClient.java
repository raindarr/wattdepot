package org.wattdepot.client;

import java.io.IOException;
import org.restlet.Client;
import org.restlet.data.Method;
import org.restlet.data.Protocol;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.wattdepot.server.Server;

/**
 * Provides a high-level interface for Clients wishing to communicate with a WattDepot server.
 * 
 * @author Philip Johnson
 * @author Robert Brewer
 */
public class WattDepotClient {

  private String wattDepotHost;
  
  /**
   * Initializes a new WattDepotClient.
   * 
   * @param host The host, such as 'http://localhost:9876/wattdepot'.
   */
  public WattDepotClient(String host) {
    Client client;

    validateArg(host);
    if (host.endsWith("/")) {
      this.wattDepotHost = host.substring(0, host.length() - 1);
    }
    client = new Client(Protocol.HTTP);
    client.setConnectTimeout(2000);
  }

  /**
   * Throws an unchecked illegal argument exception if the arg is null or empty.
   * 
   * @param arg The String that must be non-null and non-empty.
   */
  private void validateArg(String arg) {
    if ((arg == null) || ("".equals(arg))) {
      throw new IllegalArgumentException(arg + " cannot be null or the empty string.");
    }
  }

  /**
   * Determines the health of a WattDepot server.
   * 
   * @return true if the server is healthy, false if not healthy
   */
  public boolean isHealthy() {
    // URI is host with health resource URL appended
    String registerUri = this.wattDepotHost + Server.URI_ROOT + Server.HEALTH_URI;

    Request request = new Request();
    request.setResourceRef(registerUri);
    request.setMethod(Method.GET);
    Client client = new Client(Protocol.HTTP);
    Response response = client.handle(request);
    // If we get a success status code, then server is healthy
    return response.getStatus().isSuccess();
  }

  /**
   * Returns the health string from WattDepot server.
   * 
   * @return the health message from server, or null if unable to retrieve any message
   */
  public String getHealthString() {
    // URI is host with health resource URL appended
    String registerUri = this.wattDepotHost + Server.URI_ROOT + Server.HEALTH_URI;

    Request request = new Request();
    request.setResourceRef(registerUri);
    request.setMethod(Method.GET);
    Client client = new Client(Protocol.HTTP);
    Response response = client.handle(request);
    try {
      return response.getEntity().getText();
    }
    catch (IOException e) {
      return null;
    }
  }
}
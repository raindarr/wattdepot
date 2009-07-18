package org.wattdepot.client;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.hackystat.utilities.logger.RestletLoggerUtil;
import org.restlet.Client;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Preference;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;

/**
 * Provides a high-level interface for Clients wishing to communicate with a WattDepot server.
 * 
 * @author Philip Johnson
 * @author Robert Brewer
 */
public class WattDepotClient {

  /** Holds the userEmail to be associated with this client. */
  private String userEmail;
  /** Holds the password to be associated with this client. */
  private String password;
  /** The SensorBase host, such as "http://localhost:9876/sensorbase". */
  private String wattDepotHost;
  /** For PMD. */
  private static final String wattdepotclient = "wattdepotclient";
  /** The Restlet Client instance used to communicate with the server. */
  private Client client;
  /** The preferred representation type. */
  private Preference<MediaType> xmlMedia = new Preference<MediaType>(MediaType.TEXT_XML);

  /** To facilitate debugging of problems using this system. */
  private boolean isTraceEnabled = false;

  /** Timestamp of last time we tried to contact a server and failed, since this is expensive. */
  private static Map<String, Long> lastHostNotAvailable = new HashMap<String, Long>();
  
  /**
   * Initializes a new WattDepotClient, given the host, userEmail, and password.
   * 
   * @param host The host, such as 'http://localhost:9876/wattdepot'.
   * @param email The user's email that we will use for authentication.
   * @param password The password we will use for authentication.
   */
  public WattDepotClient(String host, String email, String password) {
    validateArg(host);
    validateArg(email);
    validateArg(password);
    RestletLoggerUtil.useFileHandler(wattdepotclient);
    this.userEmail = email;
    this.password = password;
    this.wattDepotHost = host;
    if (!this.wattDepotHost.endsWith("/")) {
      this.wattDepotHost = this.wattDepotHost + "/";
    }
    if (this.isTraceEnabled) {
      System.out.println("WattDepotClient Tracing: INITIALIZE " + "host='" + host + "', email='"
          + email + "', password='" + password + "'");
    }
    this.client = new Client(Protocol.HTTP);
    setTimeout(2000);
  }

  /**
   * Attempts to provide a timeout value for this SensorBaseClient.
   * 
   * @param milliseconds The number of milliseconds to wait before timing out.
   */
  public final synchronized void setTimeout(int milliseconds) {
    setClientTimeout(this.client, milliseconds);
  }

  /**
   * When passed true, future HTTP calls using this client instance will print out information on
   * the request and response.
   * 
   * @param enable If true, trace output will be generated.
   */
  public synchronized void enableHttpTracing(boolean enable) {
    this.isTraceEnabled = enable;
  }

  /**
   * Sets the lastHostNotAvailable timestamp for the passed host.
   * 
   * @param host The host that was determined to be not available.
   */
  private static void setLastHostNotAvailable(String host) {
    lastHostNotAvailable.put(host, (new Date()).getTime());
  }

  /**
   * Gets the lastHostNotAvailable timestamp associated with host. Returns 0 if there is no
   * lastHostNotAvailable timestamp.
   * 
   * @param host The host whose lastNotAvailable timestamp is to be retrieved.
   * @return The timestamp.
   */
  private static long getLastHostNotAvailable(String host) {
    Long time = lastHostNotAvailable.get(host);
    return (time == null) ? 0 : time;
  }

  /**
   * Returns true if the passed host is a WattDepot host. The timeout is set at the default timeout
   * value. Since checking isHost() when the host is not available is expensive, we cache the
   * timestamp whenever we find the host to be unavailable and if there is another call to isHost()
   * within two seconds, we will immediately return false. This makes startup of clients like
   * SensorShell go much faster, since they call isHost() several times during startup.
   * 
   * @param host The URL of a WattDepot host, such as "http://localhost:9876/wattdepot".
   * @return True if this URL responds as a WattDepot server.
   */
  public static boolean isHost(String host) {
    RestletLoggerUtil.useFileHandler(wattdepotclient);
    // We return false immediately if we failed to contact the host within the last two seconds.
    long currTime = (new Date()).getTime();
    if ((currTime - getLastHostNotAvailable(host)) < 2 * 1000) {
      return false;
    }

    // All WattDepot hosts use the HTTP protocol.
    if (!host.startsWith("http://")) {
      setLastHostNotAvailable(host);
      return false;
    }
    // Create the host/register URL.
    try {
      String registerUri = host.endsWith("/") ? host + "ping" : host + "/ping";
      Request request = new Request();
      request.setResourceRef(registerUri);
      request.setMethod(Method.GET);
      Client client = new Client(Protocol.HTTP);
//      setClientTimeout(client, getDefaultTimeout());
      Response response = client.handle(request);
      String pingText = response.getEntity().getText();
      boolean isAvailable = (response.getStatus().isSuccess() && "WattDepot".equals(pingText));
      if (!isAvailable) {
        setLastHostNotAvailable(host);
      }
      return isAvailable;
    }
    catch (Exception e) {
      setLastHostNotAvailable(host);
      return false;
    }
  }

  /**
   * Pings the specified host to determine if WattDepot is running on that host.
   * 
   * @return true if WattDepot server is running, false otherwise.
   */
  public static boolean ping(String hostname) {
    String registerUri = hostname.endsWith("/") ? hostname + "ping" : hostname + "/ping";
    Request request = new Request();
    request.setResourceRef(registerUri);
    request.setMethod(Method.GET);
    Client client = new Client(Protocol.HTTP);
//    setClientTimeout(client, getDefaultTimeout());
    Response response = client.handle(request);
    String pingText;
    try {
      pingText = response.getEntity().getText();
    }
    catch (IOException e) {
      return false;
    }
    boolean isAvailable = (response.getStatus().isSuccess() && "WattDepot".equals(pingText));
    if (!isAvailable) {
      setLastHostNotAvailable(hostname);
    }
    return isAvailable;
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
   * Does the housekeeping for making HTTP requests to the SensorBase by a test or admin user.
   * 
   * @param method The type of Method.
   * @param requestString A string, such as "users". No preceding slash.
   * @param entity The representation to be sent with the request, or null if not needed.
   * @return The Response instance returned from the server.
   */
  private Response makeRequest(Method method, String requestString, Representation entity) {
    Reference reference = new Reference(this.wattDepotHost + requestString);
    Request request = (entity == null) ? new Request(method, reference) : new Request(method,
        reference, entity);
    request.getClientInfo().getAcceptedMediaTypes().add(xmlMedia);
//    ChallengeResponse authentication = new ChallengeResponse(scheme, this.userEmail, this.password);
//    request.setChallengeResponse(authentication);
    if (this.isTraceEnabled) {
      System.out.println("WattDepotClient Tracing: " + method + " " + reference);
      if (entity != null) {
        try {
          System.out.println(entity.getText());
        }
        catch (Exception e) {
          System.out.println("  Problems with getText() on entity.");
        }
      }
    }
    Response response = this.client.handle(request);
    if (this.isTraceEnabled) {
      Status status = response.getStatus();
      System.out.println("  => " + status.getCode() + " " + status.getDescription());
    }
    return response;
  }

  /**
   * Attempts to set timeout values for the passed client.
   * 
   * @param client The client .
   * @param milliseconds The timeout value.
   */
  private static void setClientTimeout(Client client, int milliseconds) {
    client.setConnectTimeout(milliseconds);
    // client.getContext().getParameters().removeAll("connectTimeout");
    // client.getContext().getParameters().add("connectTimeout", String.valueOf(milliseconds));
    // // For the Apache Commons client.
    // client.getContext().getParameters().removeAll("readTimeout");
    // client.getContext().getParameters().add("readTimeout", String.valueOf(milliseconds));
    // client.getContext().getParameters().removeAll("connectionManagerTimeout");
    // client.getContext().getParameters().add("connectionManagerTimeout",
    // String.valueOf(milliseconds));
  }

}

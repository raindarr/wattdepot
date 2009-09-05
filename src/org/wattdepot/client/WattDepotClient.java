package org.wattdepot.client;

import java.io.IOException;
import java.io.StringReader;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import org.restlet.Client;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Preference;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.wattdepot.resource.user.jaxb.UserIndex;
import org.wattdepot.server.Server;

/**
 * Provides a high-level interface for Clients wishing to communicate with a WattDepot server.
 * 
 * @author Philip Johnson
 * @author Robert Brewer
 */
public class WattDepotClient {

  /** The representation type for XML. */
  private Preference<MediaType> XML_MEDIA = new Preference<MediaType>(MediaType.TEXT_XML);

  /** The representation type for plain text. */
  private Preference<MediaType> TEXT_MEDIA = new Preference<MediaType>(MediaType.TEXT_PLAIN);

  /** The HTTP authentication approach. */
  private ChallengeScheme scheme = ChallengeScheme.HTTP_BASIC;

  private String wattDepotUri;
  private String username;
  private String password;
  /** The Restlet Client instance used to communicate with the server. */
  private Client client;

  /** Users JAXBContext. */
  private static final JAXBContext userJAXB;
//  /** SensorData JAXBContext. */
//  private static final JAXBContext sensorDataJAXB;
//  /** Source JAXBContext. */
//  private static final JAXBContext sourceJAXB;

  // JAXBContexts are thread safe, so we can share them across all instances and threads.
  // https://jaxb.dev.java.net/guide/Performance_and_thread_safety.html
  static {
    try {
      userJAXB = JAXBContext.newInstance(org.wattdepot.resource.user.jaxb.ObjectFactory.class);
//      sensorDataJAXB = JAXBContext
//          .newInstance(org.wattdepot.resource.sensordata.jaxb.ObjectFactory.class);
//     sourceJAXB = JAXBContext.newInstance(org.wattdepot.resource.source.jaxb.ObjectFactory.class);
    }
    catch (Exception e) {
      throw new RuntimeException("Couldn't create JAXB context instances.", e);
    }
  }

  /**
   * Initializes a new WattDepotClient.
   * 
   * @param hostUri The URI of the WattDepot server, such as 'http://localhost:9876/wattdepot/'.
   * Must end in "/"
   * @param username The username that we will use for authentication. Can be null if no
   * authentication is to be attempted.
   * @param password The password we will use for authentication. Can be null if no authentication
   * is to be attempted.
   */
  public WattDepotClient(String hostUri, String username, String password) {
    if ((hostUri == null) || ("".equals(hostUri))) {
      throw new IllegalArgumentException("hostname cannot be null or the empty string.");
    }
    // We allow null usernames and passwords, but if they are empty string, throw exception
    if ("".equals(username)) {
      throw new IllegalArgumentException("username cannot be the empty string.");
    }
    if ("".equals(password)) {
      throw new IllegalArgumentException("password cannot be the empty string.");
    }

    this.wattDepotUri = hostUri;
    this.username = username;
    this.password = password;

    this.client = new Client(Protocol.HTTP);
    this.client.setConnectTimeout(2000);
  }

  /**
   * Determines whether this client has been configured for anonymous access or not. If no
   * credentials (or partially missing credentials) were provided to the constructor, then it is
   * assumed that the client will only attempt API calls at the Access Control Level of "None".
   * 
   * @return true if client is configured for anonymous access, false otherwise.
   */
  private boolean isAnonymous() {
    return (this.username == null) || (this.password == null);
  }

  /**
   * Does the housekeeping for making HTTP requests to WattDepot by a test or admin user, including
   * authentication if requested.
   * 
   * @param method the HTTP method requested.
   * @param requestString A string, such as "users". No preceding slash.
   * @param mediaPref Indication of what type of media the client prefers from the server. See
   * XML_MEDIA and TEXT_MEDIA constants.
   * @param entity The representation to be sent with the request, or null if not needed.
   * @return The Response instance returned from the server.
   */
  private Response makeRequest(Method method, String requestString,
      Preference<MediaType> mediaPref, Representation entity) {
    Reference reference = new Reference(this.wattDepotUri + requestString);
    Request request = (entity == null) ? new Request(method, reference) : new Request(method,
        reference, entity);
    request.getClientInfo().getAcceptedMediaTypes().add(mediaPref);
    if (!isAnonymous()) {
      ChallengeResponse authentication = new ChallengeResponse(scheme, this.username,
          this.password);
      request.setChallengeResponse(authentication);
    }
    return this.client.handle(request);
  }

  /**
   * Determines the health of a WattDepot server.
   * 
   * @return true if the server is healthy, false if not healthy
   */
  public boolean isHealthy() {
    // Make HEAD request, since we only care about status code
    Response response = makeRequest(Method.HEAD, Server.HEALTH_URI, TEXT_MEDIA, null);

    // If we get a success status code, then server is healthy
    return response.getStatus().isSuccess();
  }

  /**
   * Returns the health string from WattDepot server.
   * 
   * @return the health message from server, or null if unable to retrieve any message
   */
  public String getHealthString() {
    // Make the request
    Response response = makeRequest(Method.GET, Server.HEALTH_URI, TEXT_MEDIA, null);

    // Try to extract the response text and return it
    try {
      return response.getEntity().getText();
    }
    catch (IOException e) {
      return null;
    }
  }

  /**
   * Determines whether or not the credentials provided in the constructor are valid by attempting
   * to retrieve the User resource that corresponds to the username from the constructor.
   * 
   * @return true if the constructor credentials are valid, and false if they are invalid or object
   * was called with null credentials.
   */
  public boolean isAuthenticated() {
    // URI is host with user resource URL appended, followed by the configured username
    String usersUri = Server.USERS_URI + "/" + this.username;

    if (isAnonymous()) {
      return false;
    }
    else {
      Response response = makeRequest(Method.HEAD, usersUri, TEXT_MEDIA, null);
      return response.getStatus().isSuccess();
    }
  }

  /**
   * Returns the UserIndex containing all Users on the server.
   * 
   * @return The UserIndex for the server.
   * @throws WattDepotClientException If error is encountered retrieving the resource, or
   * unmarshalling the XML
   */
  public UserIndex getUserIndex() throws WattDepotClientException {
    Response response = makeRequest(Method.GET, Server.USERS_URI, XML_MEDIA, null);
    if (!response.getStatus().isSuccess()) {
      throw new WattDepotClientException(response.getStatus());
    }
    try {
      String xmlString = response.getEntity().getText();
      System.err.println("UserIndex in client: " + xmlString); // DEBUG
      Unmarshaller unmarshaller = userJAXB.createUnmarshaller();
      return (UserIndex) unmarshaller.unmarshal(new StringReader(xmlString));
    }
    catch (Exception e) {
      throw new WattDepotClientException(response.getStatus(), e);
    }
  }

  /**
   * Returns the stub string from User resource.
   * 
   * @param username The username of the User resource to be retrieved.
   * @return a String that is just a placeholding stub for the User resource.
   */
  public String getUserString(String username) {
    Response response = makeRequest(Method.GET, Server.USERS_URI + "/" + username, TEXT_MEDIA,
        null);
    try {
      return response.getEntity().getText();
    }
    catch (IOException e) {
      return null;
    }
  }

}
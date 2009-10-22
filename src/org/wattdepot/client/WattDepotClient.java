package org.wattdepot.client;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.XMLGregorianCalendar;
import org.restlet.Client;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.CharacterSet;
import org.restlet.data.Language;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Preference;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.StringRepresentation;
import org.wattdepot.resource.sensordata.jaxb.Property;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.resource.sensordata.jaxb.SensorDataIndex;
import org.wattdepot.resource.sensordata.jaxb.SensorDataRef;
import org.wattdepot.resource.source.jaxb.Source;
import org.wattdepot.resource.source.jaxb.SourceIndex;
import org.wattdepot.resource.source.jaxb.SourceRef;
import org.wattdepot.resource.source.summary.jaxb.SourceSummary;
import org.wattdepot.resource.user.jaxb.UserIndex;
import org.wattdepot.server.Server;
import org.wattdepot.util.UriUtils;
import org.wattdepot.util.logger.RestletLoggerUtil;

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
  /** SensorData JAXBContext. */
  private static final JAXBContext sensorDataJAXB;
  /** Source JAXBContext. */
  private static final JAXBContext sourceJAXB;
  /** SourceSummary JAXBContext. */
  private static final JAXBContext sourceSummaryJAXB;

  // JAXBContexts are thread safe, so we can share them across all instances and threads.
  // https://jaxb.dev.java.net/guide/Performance_and_thread_safety.html
  static {
    try {
      userJAXB = JAXBContext.newInstance(org.wattdepot.resource.user.jaxb.ObjectFactory.class);
      sensorDataJAXB =
          JAXBContext.newInstance(org.wattdepot.resource.sensordata.jaxb.ObjectFactory.class);
      sourceJAXB = JAXBContext.newInstance(org.wattdepot.resource.source.jaxb.ObjectFactory.class);
      sourceSummaryJAXB =
          JAXBContext.newInstance(org.wattdepot.resource.source.summary.jaxb.ObjectFactory.class);
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
    // nuke the Restlet loggers
    RestletLoggerUtil.removeRestletLoggers();
    this.client = new Client(Protocol.HTTP);
    this.client.setConnectTimeout(2000);
  }

  /**
   * Initializes a new WattDepotClient using anonymous (public) access to the provided server URI.
   * 
   * @param hostUri The URI of the WattDepot server, such as 'http://localhost:9876/wattdepot/'.
   * Must end in "/"
   */
  public WattDepotClient(String hostUri) {
    this(hostUri, null, null);
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
   * @param requestString A string, such as "users". Do not start the string with a '/' (it is
   * unneeded).
   * @param mediaPref Indication of what type of media the client prefers from the server. See
   * XML_MEDIA and TEXT_MEDIA constants.
   * @param entity The representation to be sent with the request, or null if not needed.
   * @return The Response instance returned from the server.
   */
  public Response makeRequest(Method method, String requestString, Preference<MediaType> mediaPref,
      Representation entity) {
    Reference reference = new Reference(this.wattDepotUri + requestString);
    Request request =
        (entity == null) ? new Request(method, reference) : new Request(method, reference, entity);
    request.getClientInfo().getAcceptedMediaTypes().add(mediaPref);
    if (!isAnonymous()) {
      ChallengeResponse authentication =
          new ChallengeResponse(scheme, this.username, this.password);
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
   * Requests a SensorDataIndex containing all the SensorData available for this source from the
   * server.
   * 
   * @param source The name of the Source.
   * @return The SensorDataIndex.
   * @throws NotAuthorizedException If the client is not authorized to retrieve the SensorData
   * index.
   * @throws ResourceNotFoundException If the source name provided doesn't exist on the server.
   * @throws BadXmlException If error is encountered unmarshalling the XML from the server.
   * @throws MiscClientException If error is encountered retrieving the resource, or some unexpected
   * problem is encountered.
   */
  public SensorDataIndex getSensorDataIndex(String source) throws NotAuthorizedException,
      ResourceNotFoundException, BadXmlException, MiscClientException {
    Response response =
        makeRequest(Method.GET, Server.SOURCES_URI + "/" + source + "/" + Server.SENSORDATA_URI,
            XML_MEDIA, null);
    Status status = response.getStatus();

    if (status.equals(Status.CLIENT_ERROR_UNAUTHORIZED)) {
      // credentials were unacceptable to server
      throw new NotAuthorizedException(status);
    }
    if (status.equals(Status.CLIENT_ERROR_NOT_FOUND)) {
      // an unknown source name was specified
      throw new ResourceNotFoundException(status);
    }
    if (status.isSuccess()) {
      try {
        String xmlString = response.getEntity().getText();
        Unmarshaller unmarshaller = sensorDataJAXB.createUnmarshaller();
        return (SensorDataIndex) unmarshaller.unmarshal(new StringReader(xmlString));
      }
      catch (IOException e) {
        // Error getting the text from the entity body, bad news
        throw new MiscClientException(status, e);
      }
      catch (JAXBException e) {
        // Got some XML we can't parse
        throw new BadXmlException(status, e);
      }
    }
    else {
      // Some totally unexpected non-success status code, just throw generic client exception
      throw new MiscClientException(status);
    }
  }

  /**
   * Requests a SensorDataIndex representing all the SensorData resources for the named Source such
   * that their timestamp is greater than or equal to the given start time and less than or equal to
   * the given end time from the server.
   * 
   * @param source The name of the Source.
   * @param startTime The start of the range.
   * @param endTime The end of the range.
   * @return The SensorDataIndex.
   * @throws NotAuthorizedException If the client is not authorized to retrieve the SensorData
   * index.
   * @throws ResourceNotFoundException If the source name provided doesn't exist on the server.
   * @throws BadXmlException If error is encountered unmarshalling the XML from the server.
   * @throws MiscClientException If error is encountered retrieving the resource, or some unexpected
   * problem is encountered.
   */
  public SensorDataIndex getSensorDataIndex(String source, XMLGregorianCalendar startTime,
      XMLGregorianCalendar endTime) throws NotAuthorizedException, ResourceNotFoundException,
      BadXmlException, MiscClientException {
    String uri =
        Server.SOURCES_URI + "/" + source + "/" + Server.SENSORDATA_URI + "/" + "?startTime="
            + startTime.toXMLFormat() + "&" + "endTime=" + endTime.toXMLFormat();
    Response response = makeRequest(Method.GET, uri, XML_MEDIA, null);
    Status status = response.getStatus();

    if (status.equals(Status.CLIENT_ERROR_UNAUTHORIZED)) {
      // credentials were unacceptable to server
      throw new NotAuthorizedException(status);
    }
    if (status.equals(Status.CLIENT_ERROR_NOT_FOUND)) {
      // an unknown source name was specified
      throw new ResourceNotFoundException(status);
    }
    if (status.equals(Status.CLIENT_ERROR_BAD_REQUEST)) {
      // bad timestamp provided in URI
      throw new BadXmlException(status);
    }
    if (status.isSuccess()) {
      try {
        String xmlString = response.getEntity().getText();
        Unmarshaller unmarshaller = sensorDataJAXB.createUnmarshaller();
        return (SensorDataIndex) unmarshaller.unmarshal(new StringReader(xmlString));
      }
      catch (IOException e) {
        // Error getting the text from the entity body, bad news
        throw new MiscClientException(status, e);
      }
      catch (JAXBException e) {
        // Got some XML we can't parse
        throw new BadXmlException(status, e);
      }
    }
    else {
      // Some totally unexpected non-success status code, just throw generic client exception
      throw new MiscClientException(status);
    }
  }

  /**
   * Requests a List of SensorData representing all the SensorData resources for the named Source
   * such that their timestamp is greater than or equal to the given start time and less than or
   * equal to the given end time from the server. Currently just a convenience method, but if the
   * REST API is extended to support getting SensorData directly without going through a
   * SensorDataIndex, this method will use that more efficient API call.
   * 
   * @param source The name of the Source.
   * @param startTime The start of the range.
   * @param endTime The end of the range.
   * @return The List of SensorData in the range.
   * @throws NotAuthorizedException If the client is not authorized to retrieve the SensorData
   * index.
   * @throws ResourceNotFoundException If the source name provided doesn't exist on the server.
   * @throws BadXmlException If error is encountered unmarshalling the XML from the server.
   * @throws MiscClientException If error is encountered retrieving the resource, or some unexpected
   * problem is encountered.
   */
  public List<SensorData> getSensorDatas(String source, XMLGregorianCalendar startTime,
      XMLGregorianCalendar endTime) throws NotAuthorizedException, ResourceNotFoundException,
      BadXmlException, MiscClientException {
    List<SensorData> dataList = new ArrayList<SensorData>();
    SensorDataIndex index = getSensorDataIndex(source, startTime, endTime);
    for (SensorDataRef ref : index.getSensorDataRef()) {
      dataList.add(getSensorData(ref));
    }
    return dataList;
  }

  /**
   * Requests the SensorData from a given Source corresponding to the given timestamp.
   * 
   * @param source The name of the Source.
   * @param timestamp The timestamp of the desired SensorData.
   * @return The SensorData.
   * @throws NotAuthorizedException If the client is not authorized to retrieve the SensorData
   * index.
   * @throws ResourceNotFoundException If the source name provided doesn't exist on the server.
   * @throws BadXmlException If error is encountered unmarshalling the XML from the server.
   * @throws MiscClientException If error is encountered retrieving the resource, or some unexpected
   * problem is encountered.
   */
  public SensorData getSensorData(String source, XMLGregorianCalendar timestamp)
      throws NotAuthorizedException, ResourceNotFoundException, BadXmlException,
      MiscClientException {
    Response response =
        makeRequest(Method.GET, Server.SOURCES_URI + "/" + source + "/" + Server.SENSORDATA_URI
            + "/" + timestamp.toXMLFormat(), XML_MEDIA, null);
    Status status = response.getStatus();

    if (status.equals(Status.CLIENT_ERROR_UNAUTHORIZED)) {
      // credentials were unacceptable to server
      throw new NotAuthorizedException(status);
    }
    if (status.equals(Status.CLIENT_ERROR_BAD_REQUEST)) {
      // bad timestamp provided in URI
      throw new BadXmlException(status);
    }
    if (status.equals(Status.CLIENT_ERROR_NOT_FOUND)) {
      // an unknown source name was specified, or timestamp could not be found
      throw new ResourceNotFoundException(status);
    }
    if (status.isSuccess()) {
      try {
        String xmlString = response.getEntity().getText();
        Unmarshaller unmarshaller = sensorDataJAXB.createUnmarshaller();
        return (SensorData) unmarshaller.unmarshal(new StringReader(xmlString));
      }
      catch (IOException e) {
        // Error getting the text from the entity body, bad news
        throw new MiscClientException(status, e);
      }
      catch (JAXBException e) {
        // Got some XML we can't parse
        throw new BadXmlException(status, e);
      }
    }
    else {
      // Some totally unexpected non-success status code, just throw generic client exception
      throw new MiscClientException(status);
    }
  }

  /**
   * Requests the power in SensorData format from a given Source corresponding to the given
   * timestamp.
   * 
   * @param source The name of the Source.
   * @param timestamp The timestamp of the desired power reading.
   * @return The SensorData.
   * @throws NotAuthorizedException If the client is not authorized to retrieve the SensorData.
   * @throws ResourceNotFoundException If the source name provided doesn't exist on the server.
   * @throws BadXmlException If error is encountered unmarshalling the XML from the server.
   * @throws MiscClientException If error is encountered retrieving the resource, or some unexpected
   * problem is encountered.
   */
  public SensorData getPower(String source, XMLGregorianCalendar timestamp)
      throws NotAuthorizedException, ResourceNotFoundException, BadXmlException,
      MiscClientException {
    Response response =
        makeRequest(Method.GET, Server.SOURCES_URI + "/" + source + "/" + Server.POWER_URI + "/"
            + timestamp.toXMLFormat(), XML_MEDIA, null);
    Status status = response.getStatus();

    if (status.equals(Status.CLIENT_ERROR_UNAUTHORIZED)) {
      // credentials were unacceptable to server
      throw new NotAuthorizedException(status);
    }
    if (status.equals(Status.CLIENT_ERROR_BAD_REQUEST)) {
      // bad timestamp provided in URI
      throw new BadXmlException(status);
    }
    if (status.equals(Status.CLIENT_ERROR_NOT_FOUND)) {
      // an unknown source name was specified
      throw new ResourceNotFoundException(status);
    }
    if (status.isSuccess()) {
      try {
        String xmlString = response.getEntity().getText();
        Unmarshaller unmarshaller = sensorDataJAXB.createUnmarshaller();
        return (SensorData) unmarshaller.unmarshal(new StringReader(xmlString));
      }
      catch (IOException e) {
        // Error getting the text from the entity body, bad news
        throw new MiscClientException(status, e);
      }
      catch (JAXBException e) {
        // Got some XML we can't parse
        throw new BadXmlException(status, e);
      }
    }
    else {
      // Some totally unexpected non-success status code, just throw generic client exception
      throw new MiscClientException(status);
    }
  }

  /**
   * Requests the power from a given Source corresponding to the given timestamp, and extracts the
   * provided property key, converts it to double and returns the value.
   * 
   * @param source The name of the Source.
   * @param timestamp The timestamp of the desired power reading.
   * @param key The property key to search for.
   * @return A double representing the the property at the given timestamp, or 0 if there is no
   * data.
   * @throws NotAuthorizedException If the client is not authorized to retrieve the power.
   * @throws ResourceNotFoundException If the source name provided doesn't exist on the server.
   * @throws BadXmlException If error is encountered unmarshalling the XML from the server.
   * @throws MiscClientException If error is encountered retrieving the resource, or some unexpected
   * problem is encountered.
   */
  private double getPowerValue(String source, XMLGregorianCalendar timestamp, String key)
      throws NotAuthorizedException, ResourceNotFoundException, BadXmlException,
      MiscClientException {
    SensorData data = getPower(source, timestamp);
    for (Property prop : data.getProperties().getProperty()) {
      if (key.equals(prop.getKey())) {
        return Double.valueOf(prop.getValue());
      }
    }
    return 0;
  }

  /**
   * Requests the power generated from a given Source corresponding to the given timestamp.
   * 
   * @param source The name of the Source.
   * @param timestamp The timestamp of the desired power reading.
   * @return A double representing the power generated at the given timestamp, or 0 if there is no
   * data.
   * @throws NotAuthorizedException If the client is not authorized to retrieve the power.
   * @throws ResourceNotFoundException If the source name provided doesn't exist on the server.
   * @throws BadXmlException If error is encountered unmarshalling the XML from the server.
   * @throws MiscClientException If error is encountered retrieving the resource, or some unexpected
   * problem is encountered.
   */
  public double getPowerGenerated(String source, XMLGregorianCalendar timestamp)
      throws NotAuthorizedException, ResourceNotFoundException, BadXmlException,
      MiscClientException {
    return getPowerValue(source, timestamp, "powerGenerated");
  }

  /**
   * Requests the power consumed by a given Source corresponding to the given timestamp.
   * 
   * @param source The name of the Source.
   * @param timestamp The timestamp of the desired power reading.
   * @return A double representing the power consumed at the given timestamp, or 0 if there is no
   * data.
   * @throws NotAuthorizedException If the client is not authorized to retrieve the power.
   * @throws ResourceNotFoundException If the source name provided doesn't exist on the server.
   * @throws BadXmlException If error is encountered unmarshalling the XML from the server.
   * @throws MiscClientException If error is encountered retrieving the resource, or some unexpected
   * problem is encountered.
   */
  public double getPowerConsumed(String source, XMLGregorianCalendar timestamp)
      throws NotAuthorizedException, ResourceNotFoundException, BadXmlException,
      MiscClientException {
    return getPowerValue(source, timestamp, "powerConsumed");
  }

  /**
   * Convenience method for retrieving a SensorData given its SensorDataRef.
   * 
   * @param ref The SensorDataRef of the desired SensorData.
   * @return The SensorData.
   * @throws NotAuthorizedException If the client is not authorized to retrieve the SensorData
   * index.
   * @throws ResourceNotFoundException If the source name in the ref doesn't exist on the server.
   * @throws BadXmlException If error is encountered unmarshalling the XML from the server.
   * @throws MiscClientException If error is encountered retrieving the resource, or some unexpected
   * problem is encountered.
   */
  public SensorData getSensorData(SensorDataRef ref) throws NotAuthorizedException,
      ResourceNotFoundException, BadXmlException, MiscClientException {
    return getSensorData(UriUtils.getUriSuffix(ref.getSource()), ref.getTimestamp());
  }

  /**
   * Stores a SensorData object in the server.
   * 
   * @param data The SensorData object to be stored.
   * @return True if the SensorData could be stored, false otherwise.
   * @throws JAXBException If there are problems marshalling the object for upload.
   * @throws NotAuthorizedException If the client is not authorized to store the SensorData.
   * @throws ResourceNotFoundException If the source name referenced in the SensorData doesn't exist
   * on the server.
   * @throws BadXmlException If the server reports that the XML sent was bad, or the timestamp
   * specified was bad, or there was no XML, or the fields in the XML don't match the URI.
   * @throws OverwriteAttemptedException If there is already SensorData on the server with the given
   * timestamp.
   * @throws MiscClientException If the server indicates an unexpected problem has occurred.
   */
  public boolean storeSensorData(SensorData data) throws JAXBException, NotAuthorizedException,
      ResourceNotFoundException, BadXmlException, OverwriteAttemptedException, MiscClientException {
    Marshaller marshaller = sensorDataJAXB.createMarshaller();
    StringWriter writer = new StringWriter();
    if (data == null) {
      return false;
    }
    else {
      marshaller.marshal(data, writer);
    }
    Representation rep =
        new StringRepresentation(writer.toString(), MediaType.TEXT_XML, Language.ALL,
            CharacterSet.UTF_8);
    Response response =
        makeRequest(Method.PUT, Server.SOURCES_URI + "/" + UriUtils.getUriSuffix(data.getSource())
            + "/" + Server.SENSORDATA_URI + "/" + data.getTimestamp().toXMLFormat(), XML_MEDIA, rep);
    Status status = response.getStatus();
    if (status.equals(Status.CLIENT_ERROR_UNAUTHORIZED)) {
      // credentials were unacceptable to server
      throw new NotAuthorizedException(status);
    }
    if (status.equals(Status.CLIENT_ERROR_NOT_FOUND)) {
      // an unknown source name was specified
      throw new ResourceNotFoundException(status);
    }
    if (status.equals(Status.CLIENT_ERROR_BAD_REQUEST)) {
      // either bad timestamp provided in URI or bad XML in entity body
      throw new BadXmlException(status);
    }
    if (status.equals(Status.CLIENT_ERROR_CONFLICT)) {
      // client attempted to overwrite existing data
      throw new OverwriteAttemptedException(status);
    }
    if (status.isSuccess()) {
      return true;
    }
    else {
      // Some unexpected type of error received, so punt
      throw new MiscClientException(status);
    }
  }

  /**
   * Deletes a SensorData resource from the server.
   * 
   * @param source The name of the source containing the resource to be deleted.
   * @param timestamp The timestamp of the resource to be deleted.
   * @return True if the SensorData could be deleted, false otherwise.
   * @throws NotAuthorizedException If the client is not authorized to store the SensorData.
   * @throws BadXmlException If the server reports that the timestamp specified was bad
   * @throws ResourceNotFoundException If the source name doesn't exist on the server, or there is
   * no data with the given timestamp
   * @throws MiscClientException If the server indicates an unexpected problem has occurred.
   */
  public boolean deleteSensorData(String source, XMLGregorianCalendar timestamp)
      throws NotAuthorizedException, ResourceNotFoundException, BadXmlException,
      MiscClientException {
    Response response =
        makeRequest(Method.DELETE, Server.SOURCES_URI + "/" + source + "/" + Server.SENSORDATA_URI
            + "/" + timestamp.toXMLFormat(), XML_MEDIA, null);
    Status status = response.getStatus();

    if (status.equals(Status.CLIENT_ERROR_UNAUTHORIZED)) {
      // credentials were unacceptable to server
      throw new NotAuthorizedException(status);
    }
    if (status.equals(Status.CLIENT_ERROR_BAD_REQUEST)) {
      // bad timestamp provided in URI
      throw new BadXmlException(status);
    }
    if (status.equals(Status.CLIENT_ERROR_NOT_FOUND)) {
      // an unknown source name was specified, or timestamp could not be found
      throw new ResourceNotFoundException(status);
    }
    if (status.isSuccess()) {
      return true;
    }
    else {
      // Some unexpected type of error received, so punt
      throw new MiscClientException(status);
    }
  }

  /**
   * Returns the UserIndex containing all Users on the server.
   * 
   * @return The UserIndex for the server.
   * @throws NotAuthorizedException If the client is not authorized to retrieve the user index.
   * @throws BadXmlException If error is encountered unmarshalling the XML from the server.
   * @throws MiscClientException If error is encountered retrieving the resource, or some unexpected
   * problem is encountered.
   */
  public UserIndex getUserIndex() throws NotAuthorizedException, BadXmlException,
      MiscClientException {
    Response response = makeRequest(Method.GET, Server.USERS_URI, XML_MEDIA, null);
    Status status = response.getStatus();

    if (status.equals(Status.CLIENT_ERROR_UNAUTHORIZED)) {
      // User credentials did not correspond to an admin
      throw new NotAuthorizedException(status);
    }
    if (status.isSuccess()) {
      try {
        String xmlString = response.getEntity().getText();
        // System.err.println("UserIndex in client: " + xmlString); // DEBUG
        Unmarshaller unmarshaller = userJAXB.createUnmarshaller();
        return (UserIndex) unmarshaller.unmarshal(new StringReader(xmlString));
      }
      catch (IOException e) {
        // Error getting the text from the entity body, bad news
        throw new MiscClientException(status, e);
      }
      catch (JAXBException e) {
        // Got some XML we can't parse
        throw new BadXmlException(status, e);
      }
    }
    else {
      // Some totally unexpected non-success status code, just throw generic client exception
      throw new MiscClientException(status);
    }
  }

  /**
   * Returns the stub string from User resource.
   * 
   * @param username The username of the User resource to be retrieved.
   * @return a String that is just a placeholding stub for the User resource.
   */
  public String getUserString(String username) {
    Response response =
        makeRequest(Method.GET, Server.USERS_URI + "/" + username, TEXT_MEDIA, null);
    try {
      return response.getEntity().getText();
    }
    catch (IOException e) {
      return null;
    }
  }

  /**
   * Returns the SourceIndex containing all Sources accessible to the authenticated user on the
   * server.
   * 
   * @return The SourceIndex for the server.
   * @throws NotAuthorizedException If the client's credentials are bad.
   * @throws BadXmlException If error is encountered unmarshalling the XML from the server.
   * @throws MiscClientException If error is encountered retrieving the resource, or some unexpected
   * problem is encountered.
   */
  public SourceIndex getSourceIndex() throws NotAuthorizedException, BadXmlException,
      MiscClientException {
    Response response = makeRequest(Method.GET, Server.SOURCES_URI, XML_MEDIA, null);
    Status status = response.getStatus();

    if (status.equals(Status.CLIENT_ERROR_UNAUTHORIZED)) {
      // User credentials are invalid
      throw new NotAuthorizedException(status);
    }
    if (status.isSuccess()) {
      try {
        String xmlString = response.getEntity().getText();
        Unmarshaller unmarshaller = sourceJAXB.createUnmarshaller();
        return (SourceIndex) unmarshaller.unmarshal(new StringReader(xmlString));
      }
      catch (IOException e) {
        // Error getting the text from the entity body, bad news
        throw new MiscClientException(status, e);
      }
      catch (JAXBException e) {
        // Got some XML we can't parse
        throw new BadXmlException(status, e);
      }
    }
    else {
      // Some totally unexpected non-success status code, just throw generic client exception
      throw new MiscClientException(status);
    }
  }

  /**
   * Requests a List of Sources representing all Sources accessible to the authenticated user on the
   * server. Currently just a convenience method, but if the REST API is extended to support getting
   * Sources directly without going through a SourceIndex, this method will use that more efficient
   * API call.
   * 
   * @return The List of Sources accessible to the user.
   * @throws NotAuthorizedException If the client's credentials are bad.
   * @throws BadXmlException If error is encountered unmarshalling the XML from the server.
   * @throws MiscClientException If error is encountered retrieving the resource, or some unexpected
   * problem is encountered.
   */
  public List<Source> getSources() throws NotAuthorizedException, BadXmlException,
      MiscClientException {
    List<Source> sourceList = new ArrayList<Source>();
    SourceIndex index = getSourceIndex();
    for (SourceRef ref : index.getSourceRef()) {
      try {
        sourceList.add(getSource(ref));
      }
      catch (ResourceNotFoundException e) {
        // We got the SourceIndex already, so we know the source exists. this should never happen
        throw new MiscClientException("SourceRef from Source index had non-existent source name", e);
      }
    }
    return sourceList;
  }

  /**
   * Requests the Source with the given name from the server.
   * 
   * @param source The name of the Source.
   * @return The Source.
   * @throws NotAuthorizedException If the client is not authorized to retrieve the Source.
   * @throws ResourceNotFoundException If the source name provided doesn't exist on the server.
   * @throws BadXmlException If error is encountered unmarshalling the XML from the server.
   * @throws MiscClientException If error is encountered retrieving the resource, or some unexpected
   * problem is encountered.
   */
  public Source getSource(String source) throws NotAuthorizedException, ResourceNotFoundException,
      BadXmlException, MiscClientException {
    Response response = makeRequest(Method.GET, Server.SOURCES_URI + "/" + source, XML_MEDIA, null);
    Status status = response.getStatus();

    if (status.equals(Status.CLIENT_ERROR_UNAUTHORIZED)) {
      // credentials were unacceptable to server
      throw new NotAuthorizedException(status);
    }
    if (status.equals(Status.CLIENT_ERROR_NOT_FOUND)) {
      // an unknown source name was specified
      throw new ResourceNotFoundException(status);
    }
    if (status.isSuccess()) {
      try {
        String xmlString = response.getEntity().getText();
        Unmarshaller unmarshaller = sourceJAXB.createUnmarshaller();
        return (Source) unmarshaller.unmarshal(new StringReader(xmlString));
      }
      catch (IOException e) {
        // Error getting the text from the entity body, bad news
        throw new MiscClientException(status, e);
      }
      catch (JAXBException e) {
        // Got some XML we can't parse
        throw new BadXmlException(status, e);
      }
    }
    else {
      // Some totally unexpected non-success status code, just throw generic client exception
      throw new MiscClientException(status);
    }
  }

  /**
   * Convenience method for retrieving a Source given its SourceRef.
   * 
   * @param ref The SourceRef of the desired Source.
   * @return The Source.
   * @throws NotAuthorizedException If the client is not authorized to retrieve the Source.
   * @throws ResourceNotFoundException If the source name provided doesn't exist on the server.
   * @throws BadXmlException If error is encountered unmarshalling the XML from the server.
   * @throws MiscClientException If error is encountered retrieving the resource, or some unexpected
   * problem is encountered.
   */
  public Source getSource(SourceRef ref) throws NotAuthorizedException, ResourceNotFoundException,
      BadXmlException, MiscClientException {
    return getSource(ref.getName());
  }

  /**
   * Requests the SourceSummary for the Source with the given name from the server.
   * 
   * @param source The name of the Source.
   * @return The SourceSummary.
   * @throws NotAuthorizedException If the client is not authorized to retrieve the SourceSummary.
   * @throws ResourceNotFoundException If the source name provided doesn't exist on the server.
   * @throws BadXmlException If error is encountered unmarshalling the XML from the server.
   * @throws MiscClientException If error is encountered retrieving the resource, or some unexpected
   * problem is encountered.
   */
  public SourceSummary getSourceSummary(String source) throws NotAuthorizedException,
      ResourceNotFoundException, BadXmlException, MiscClientException {
    Response response =
        makeRequest(Method.GET, Server.SOURCES_URI + "/" + source + "/" + Server.SUMMARY_URI,
            XML_MEDIA, null);
    Status status = response.getStatus();

    if (status.equals(Status.CLIENT_ERROR_UNAUTHORIZED)) {
      // credentials were unacceptable to server
      throw new NotAuthorizedException(status);
    }
    if (status.equals(Status.CLIENT_ERROR_NOT_FOUND)) {
      // an unknown source name was specified
      throw new ResourceNotFoundException(status);
    }
    if (status.isSuccess()) {
      try {
        String xmlString = response.getEntity().getText();
        Unmarshaller unmarshaller = sourceSummaryJAXB.createUnmarshaller();
        return (SourceSummary) unmarshaller.unmarshal(new StringReader(xmlString));
      }
      catch (IOException e) {
        // Error getting the text from the entity body, bad news
        throw new MiscClientException(status, e);
      }
      catch (JAXBException e) {
        // Got some XML we can't parse
        throw new BadXmlException(status, e);
      }
    }
    else {
      // Some totally unexpected non-success status code, just throw generic client exception
      throw new MiscClientException(status);
    }
  }

  /**
   * Retrieves the WattDepot URI used by this client. This is useful for creating resource objects
   * that have URIs in their fields (and thus need the WattDepot URI to construct those URIs).
   * 
   * @return The URI of the WattDepot server used by this client.
   */
  public String getWattDepotUri() {
    return wattDepotUri;
  }
}
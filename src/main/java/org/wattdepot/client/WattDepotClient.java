package org.wattdepot.client;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.XMLGregorianCalendar;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;
import org.wattdepot.resource.ResourceInterface;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.resource.sensordata.jaxb.SensorDataIndex;
import org.wattdepot.resource.sensordata.jaxb.SensorDataRef;
import org.wattdepot.resource.sensordata.jaxb.SensorDatas;
import org.wattdepot.resource.source.jaxb.Source;
import org.wattdepot.resource.source.jaxb.SourceIndex;
import org.wattdepot.resource.source.jaxb.SourceRef;
import org.wattdepot.resource.source.jaxb.Sources;
import org.wattdepot.resource.source.summary.jaxb.SourceSummary;
import org.wattdepot.resource.user.jaxb.User;
import org.wattdepot.resource.user.jaxb.UserIndex;
import org.wattdepot.resource.user.jaxb.UserRef;
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

  private static final String START_TIME_PARAM = "?startTime=";

  /** The HTTP authentication approach. */
  private ChallengeScheme scheme = ChallengeScheme.HTTP_BASIC;

  private String wattDepotUri;
  private String username;
  private String password;

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
   * Creates a ClientResource for the given request, with authentication where appropriate. Calling
   * code MUST release the ClientResource when finished.
   * 
   * @param requestString A string, such as "users". Do not start the string with a '/' (it is
   * unneeded).
   * @return The client resource
   */
  public ClientResource makeClient(String requestString) {
    Reference reference = new Reference(this.wattDepotUri + requestString);
    ClientResource client = new ClientResource(reference);

    if (!isAnonymous()) {
      ChallengeResponse authentication =
          new ChallengeResponse(scheme, this.username, this.password);
      client.setChallengeResponse(authentication);
    }

    return client;
  }

  /**
   * Determines the health of a WattDepot server.
   * 
   * @return true if the server is healthy, false if not healthy
   */
  public boolean isHealthy() {
    ClientResource client = null;
    try {
      client = makeClient(Server.HEALTH_URI);
      client.head();
      boolean healthy = client.getStatus().isSuccess();
      client.release();
      return healthy;
    }
    catch (ResourceException e) {
      // Unexpected ResourceException in getting HEAD, so server is not healthy
      return false;
    }
    finally {
      // Just making sure client is always released
      if (client != null) {
        client.release();
      }
    }
  }

  /**
   * Returns the health string from WattDepot server.
   * 
   * @return the health message from server, or null if unable to retrieve any message
   */
  public String getHealthString() {
    ClientResource client = makeClient(Server.HEALTH_URI);
    ResourceInterface resource = client.wrap(ResourceInterface.class);
    String xmlString = resource.getXml();
    client.release();
    return xmlString;
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
      ClientResource client = null;
      try {
        client = makeClient(usersUri);
        client.head();
        boolean healthy = client.getStatus().isSuccess();
        client.release();
        return healthy;
      }
      catch (ResourceException e) {
        // Unexpected ResourceException in getting HEAD, so assume not authenticated
        return false;
      }
      finally {
        // Just making sure client is always released
        if (client != null) {
          client.release();
        }
      }
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

    ClientResource client =
        makeClient(Server.SOURCES_URI + "/" + source + "/" + Server.SENSORDATA_URI);
    ResourceInterface resource = client.wrap(ResourceInterface.class);
    String xmlString = null;
    try {
      xmlString = resource.getXml();
    }
    catch (ResourceException e) {
      Status status = e.getStatus();

      if (status.equals(Status.CLIENT_ERROR_UNAUTHORIZED)) {
        // credentials were unacceptable to server
        throw new NotAuthorizedException(status);
      }
      if (status.equals(Status.CLIENT_ERROR_NOT_FOUND)) {
        // an unknown source name was specified
        throw new ResourceNotFoundException(status);
      }
      else {
        // Some totally unexpected non-success status code, just throw generic client exception
        throw new MiscClientException(status);
      }
    }
    finally {
      client.release();
    }

    Status status = client.getStatus();
    client.release();
    if (status.isSuccess()) {
      try {
        Unmarshaller unmarshaller = sensorDataJAXB.createUnmarshaller();
        return (SensorDataIndex) unmarshaller.unmarshal(new StringReader(xmlString));
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
   * the given end time from the server. When looking to retrieve all the sensor data objects from
   * the index, getSensorDatas is preferable to this method.
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
   * @see org.wattdepot.client.WattDepotClient#getSensorDatas getSensorDatas
   */
  public SensorDataIndex getSensorDataIndex(String source, XMLGregorianCalendar startTime,
      XMLGregorianCalendar endTime) throws NotAuthorizedException, ResourceNotFoundException,
      BadXmlException, MiscClientException {
    String uri =
        Server.SOURCES_URI + "/" + source + "/" + Server.SENSORDATA_URI + "/" + START_TIME_PARAM
            + startTime.toXMLFormat() + "&" + "endTime=" + endTime.toXMLFormat();

    ClientResource client = makeClient(uri);
    ResourceInterface resource = client.wrap(ResourceInterface.class);
    String xmlString = null;
    try {
      xmlString = resource.getXml();
    }
    catch (ResourceException e) {
      Status status = e.getStatus();
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
      else {
        // Some totally unexpected non-success status code, just throw generic client exception
        throw new MiscClientException(status);
      }
    }
    finally {
      client.release();
    }
    Status status = client.getStatus();
    client.release();
    if (status.isSuccess()) {
      try {
        Unmarshaller unmarshaller = sensorDataJAXB.createUnmarshaller();
        return (SensorDataIndex) unmarshaller.unmarshal(new StringReader(xmlString));
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
   * that their timestamp is greater than or equal to the given start time from the server. When
   * looking to retrieve all the sensor data objects from the index, getSensorDatas is preferable to
   * this method.
   * 
   * @param source The name of the Source.
   * @param startTime The start of the range.
   * @return The SensorDataIndex.
   * @throws NotAuthorizedException If the client is not authorized to retrieve the SensorData
   * index.
   * @throws ResourceNotFoundException If the source name provided doesn't exist on the server.
   * @throws BadXmlException If error is encountered unmarshalling the XML from the server.
   * @throws MiscClientException If error is encountered retrieving the resource, or some unexpected
   * problem is encountered.
   * @see org.wattdepot.client.WattDepotClient#getSensorDatas getSensorDatas
   */
  public SensorDataIndex getSensorDataIndex(String source, XMLGregorianCalendar startTime)
      throws NotAuthorizedException, ResourceNotFoundException, BadXmlException,
      MiscClientException {
    String uri =
        Server.SOURCES_URI + "/" + source + "/" + Server.SENSORDATA_URI + "/" + START_TIME_PARAM
            + startTime.toXMLFormat() + "&" + "endTime=" + Server.LATEST;

    ClientResource client = makeClient(uri);
    ResourceInterface resource = client.wrap(ResourceInterface.class);
    String xmlString = null;
    try {
      xmlString = resource.getXml();
    }
    catch (ResourceException e) {
      Status status = e.getStatus();
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
      else {
        // Some totally unexpected non-success status code, just throw generic client exception
        throw new MiscClientException(status);
      }
    }
    finally {
      client.release();
    }
    Status status = client.getStatus();
    client.release();
    if (status.isSuccess()) {
      try {
        Unmarshaller unmarshaller = sensorDataJAXB.createUnmarshaller();
        return (SensorDataIndex) unmarshaller.unmarshal(new StringReader(xmlString));
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
   * equal to the given end time from the server. Fetches the data using the "fetchAll" REST API
   * parameter.
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

    String uri =
        Server.SOURCES_URI + "/" + source + "/" + Server.SENSORDATA_URI + "/" + START_TIME_PARAM
            + startTime.toXMLFormat() + "&" + "endTime=" + endTime.toXMLFormat() + "&"
            + "fetchAll=true";
    ClientResource client = makeClient(uri);
    ResourceInterface resource = client.wrap(ResourceInterface.class);
    String xmlString = null;
    try {
      xmlString = resource.getXml();
    }
    catch (ResourceException e) {
      Status status = e.getStatus();
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
      else {
        // Some totally unexpected non-success status code, just throw generic client exception
        throw new MiscClientException(status);
      }
    }
    finally {
      client.release();
    }
    Status status = client.getStatus();
    client.release();
    if (status.isSuccess()) {
      try {
        Unmarshaller unmarshaller = sensorDataJAXB.createUnmarshaller();
        return ((SensorDatas) unmarshaller.unmarshal(new StringReader(xmlString))).getSensorData();
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
   * such that their timestamp is greater than or equal to the given start time from the server.
   * Fetches the data using the "fetchAll" REST API parameter.
   * 
   * @param source The name of the Source.
   * @param startTime The start of the range.
   * @return The List of SensorData in the range.
   * @throws NotAuthorizedException If the client is not authorized to retrieve the SensorData
   * index.
   * @throws ResourceNotFoundException If the source name provided doesn't exist on the server.
   * @throws BadXmlException If error is encountered unmarshalling the XML from the server.
   * @throws MiscClientException If error is encountered retrieving the resource, or some unexpected
   * problem is encountered.
   */
  public List<SensorData> getSensorDatas(String source, XMLGregorianCalendar startTime)
      throws NotAuthorizedException, ResourceNotFoundException, BadXmlException,
      MiscClientException {

    String uri =
        Server.SOURCES_URI + "/" + source + "/" + Server.SENSORDATA_URI + "/" + START_TIME_PARAM
            + startTime.toXMLFormat() + "&" + "endTime=" + Server.LATEST + "&" + "fetchAll=true";
    ClientResource client = makeClient(uri);
    ResourceInterface resource = client.wrap(ResourceInterface.class);
    String xmlString = null;
    try {
      xmlString = resource.getXml();
    }
    catch (ResourceException e) {
      Status status = e.getStatus();
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
      else {
        // Some totally unexpected non-success status code, just throw generic client exception
        throw new MiscClientException(status);
      }
    }

    finally {
      client.release();
    }
    Status status = client.getStatus();
    client.release();
    if (status.isSuccess()) {
      try {
        Unmarshaller unmarshaller = sensorDataJAXB.createUnmarshaller();
        return ((SensorDatas) unmarshaller.unmarshal(new StringReader(xmlString))).getSensorData();
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

    String uri =
        Server.SOURCES_URI + "/" + source + "/" + Server.SENSORDATA_URI + "/"
            + timestamp.toXMLFormat();
    ClientResource client = makeClient(uri);
    ResourceInterface resource = client.wrap(ResourceInterface.class);
    String xmlString = null;
    try {
      xmlString = resource.getXml();
    }
    catch (ResourceException e) {
      Status status = e.getStatus();
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
      else {
        // Some totally unexpected non-success status code, just throw generic client exception
        throw new MiscClientException(status);
      }
    }
    finally {
      client.release();
    }
    Status status = client.getStatus();
    client.release();
    if (status.isSuccess()) {
      try {
        Unmarshaller unmarshaller = sensorDataJAXB.createUnmarshaller();
        return (SensorData) unmarshaller.unmarshal(new StringReader(xmlString));
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
   * Returns the latest SensorData instance for a particular named Source, or throws an exception if
   * the data cannot be retrieved. If the Source is virtual, the latest SensorData returned is the
   * union of all the properties of the latest SensorData for each SubSource, and for any properties
   * in common with numeric values, the returned value will be the sum of all the values. The
   * timestamp of the SensorData for a virtual Source will be the <b>earliest</b> of the timestamps
   * of the latest SensorData from each SubSource, as this ensures that any subsequent requests for
   * ranges of data using that timestamp will succeed (since all SubSources have valid data up to
   * that endpoint).
   * 
   * @param source The name of the Source.
   * @return The SensorData.
   * @throws NotAuthorizedException If the client is not authorized to retrieve the SensorData
   * index.
   * @throws ResourceNotFoundException If the source name provided doesn't exist on the server, or
   * the source has no sensor data.
   * @throws BadXmlException If error is encountered unmarshalling the XML from the server.
   * @throws MiscClientException If error is encountered retrieving the resource, or some unexpected
   * problem is encountered.
   */
  public SensorData getLatestSensorData(String source) throws NotAuthorizedException,
      ResourceNotFoundException, BadXmlException, MiscClientException {

    String uri =
        Server.SOURCES_URI + "/" + source + "/" + Server.SENSORDATA_URI + "/" + Server.LATEST;
    ClientResource client = makeClient(uri);
    ResourceInterface resource = client.wrap(ResourceInterface.class);
    String xmlString = null;
    try {
      xmlString = resource.getXml();
    }
    catch (ResourceException e) {
      Status status = e.getStatus();
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
      else {
        // Some totally unexpected non-success status code, just throw generic client exception
        throw new MiscClientException(status);
      }
    }
    finally {
      client.release();
    }
    Status status = client.getStatus();
    client.release();
    if (status.isSuccess()) {
      try {
        Unmarshaller unmarshaller = sensorDataJAXB.createUnmarshaller();
        return (SensorData) unmarshaller.unmarshal(new StringReader(xmlString));
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
   * Requests the latest SensorData from a given Source, and extracts the provided property key,
   * converts it to double and returns the value.
   * 
   * @param source The name of the Source.
   * @param key The property key to search for.
   * @return A double representing the the property at the given timestamp, or 0 if there is no
   * data.
   * @throws NotAuthorizedException If the client is not authorized to retrieve the SensorData
   * index.
   * @throws ResourceNotFoundException If the source name provided doesn't exist on the server, or
   * the source has no sensor data.
   * @throws BadXmlException If error is encountered unmarshalling the XML from the server.
   * @throws MiscClientException If error is encountered retrieving the resource, or some unexpected
   * problem is encountered.
   */
  private double getLatestSensorDataValue(String source, String key) throws NotAuthorizedException,
      ResourceNotFoundException, BadXmlException, MiscClientException {
    SensorData data = getLatestSensorData(source);
    return data.getProperties().getPropertyAsDouble(key);
  }

  /**
   * Requests the latest power generated from a given Source. The resulting value is in watts.
   * 
   * @param source The name of the Source.
   * @return A double representing the latest power generated, or 0 if there is no data.
   * @throws NotAuthorizedException If the client is not authorized to retrieve the power.
   * @throws ResourceNotFoundException If the source name provided doesn't exist on the server, or
   * the source has no sensor data.
   * @throws BadXmlException If error is encountered unmarshalling the XML from the server.
   * @throws MiscClientException If error is encountered retrieving the resource, or some unexpected
   * problem is encountered.
   */
  public double getLatestPowerGenerated(String source) throws NotAuthorizedException,
      ResourceNotFoundException, BadXmlException, MiscClientException {
    return getLatestSensorDataValue(source, SensorData.POWER_GENERATED);
  }

  /**
   * Requests the latest power consumed by a given Source. The resulting value is in watts.
   * 
   * @param source The name of the Source.
   * @return A double representing the latest power consumed, or 0 if there is no data.
   * @throws NotAuthorizedException If the client is not authorized to retrieve the power.
   * @throws ResourceNotFoundException If the source name provided doesn't exist on the server, or
   * the source has no sensor data.
   * @throws BadXmlException If error is encountered unmarshalling the XML from the server.
   * @throws MiscClientException If error is encountered retrieving the resource, or some unexpected
   * problem is encountered.
   */
  public double getLatestPowerConsumed(String source) throws NotAuthorizedException,
      ResourceNotFoundException, BadXmlException, MiscClientException {
    return getLatestSensorDataValue(source, SensorData.POWER_CONSUMED);
  }

  /**
   * Requests the latest energy generated to date from a given Source. The resulting value is in
   * watt hours.
   * 
   * @param source The name of the Source.
   * @return A double representing the latest energy generated to date, or 0 if there is no data.
   * @throws NotAuthorizedException If the client is not authorized to retrieve the power.
   * @throws ResourceNotFoundException If the source name provided doesn't exist on the server, or
   * the source has no sensor data.
   * @throws BadXmlException If error is encountered unmarshalling the XML from the server.
   * @throws MiscClientException If error is encountered retrieving the resource, or some unexpected
   * problem is encountered.
   */
  public double getLatestEnergyGeneratedToDate(String source) throws NotAuthorizedException,
      ResourceNotFoundException, BadXmlException, MiscClientException {
    return getLatestSensorDataValue(source, SensorData.ENERGY_GENERATED_TO_DATE);
  }

  /**
   * Requests the latest energy consumed to date from a given Source. The resulting value is in watt
   * hours.
   * 
   * @param source The name of the Source.
   * @return A double representing the latest energy consumed to date, or 0 if there is no data.
   * @throws NotAuthorizedException If the client is not authorized to retrieve the power.
   * @throws ResourceNotFoundException If the source name provided doesn't exist on the server, or
   * the source has no sensor data.
   * @throws BadXmlException If error is encountered unmarshalling the XML from the server.
   * @throws MiscClientException If error is encountered retrieving the resource, or some unexpected
   * problem is encountered.
   */
  public double getLatestEnergyConsumedToDate(String source) throws NotAuthorizedException,
      ResourceNotFoundException, BadXmlException, MiscClientException {
    return getLatestSensorDataValue(source, SensorData.ENERGY_CONSUMED_TO_DATE);
  }

  /**
   * Requests the power in SensorData format from a given Source corresponding to the given
   * timestamp. If you are just looking to retrieve the power values as a double, the
   * getPowerGenerated and getPowerConsumed methods are preferable to this one.
   * 
   * @param source The name of the Source.
   * @param timestamp The timestamp of the desired power reading.
   * @return The SensorData.
   * @throws NotAuthorizedException If the client is not authorized to retrieve the SensorData.
   * @throws ResourceNotFoundException If the source name provided doesn't exist on the server.
   * @throws BadXmlException If error is encountered unmarshalling the XML from the server.
   * @throws MiscClientException If error is encountered retrieving the resource, or some unexpected
   * problem is encountered.
   * @see org.wattdepot.client.WattDepotClient#getPowerGenerated getPowerGenerated
   * @see org.wattdepot.client.WattDepotClient#getPowerConsumed getPowerConsumed
   */
  public SensorData getPower(String source, XMLGregorianCalendar timestamp)
      throws NotAuthorizedException, ResourceNotFoundException, BadXmlException,
      MiscClientException {
    String uri =
        Server.SOURCES_URI + "/" + source + "/" + Server.POWER_URI + "/" + timestamp.toXMLFormat();

    ClientResource client = makeClient(uri);
    ResourceInterface resource = client.wrap(ResourceInterface.class);
    String xmlString = null;
    try {
      xmlString = resource.getXml();
    }
    catch (ResourceException e) {
      Status status = e.getStatus();
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
      else {
        // Some totally unexpected non-success status code, just throw generic client exception
        throw new MiscClientException(status);
      }
    }
    finally {
      client.release();
    }
    Status status = client.getStatus();
    client.release();
    if (status.isSuccess()) {
      try {
        Unmarshaller unmarshaller = sensorDataJAXB.createUnmarshaller();
        return (SensorData) unmarshaller.unmarshal(new StringReader(xmlString));
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
    return data.getProperties().getPropertyAsDouble(key);
  }

  /**
   * Requests the power generated from a given Source corresponding to the given timestamp. The
   * resulting value is in watts.
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
    return getPowerValue(source, timestamp, SensorData.POWER_GENERATED);
  }

  /**
   * Requests the power consumed by a given Source corresponding to the given timestamp. The
   * resulting value is in watts.
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
    return getPowerValue(source, timestamp, SensorData.POWER_CONSUMED);
  }

  /**
   * Requests the energy in SensorData format from a given Source corresponding to the given
   * startTime and endTime and sampling interval in minutes. If you are just looking to retrieve the
   * energy values as doubles, the getEnergyGenerated and getEnergyConsumed methods are preferable
   * to this one.
   * 
   * @param source The name of the Source.
   * @param startTime The timestamp of the start of the range.
   * @param endTime The timestamp of the end of the range.
   * @param samplingInterval The sampling interval in minutes. A value of 0 tells the server to use
   * a default interval.
   * @return The SensorData.
   * @throws NotAuthorizedException If the client is not authorized to retrieve the SensorData.
   * @throws ResourceNotFoundException If the source name provided doesn't exist on the server.
   * @throws BadXmlException If error is encountered unmarshalling the XML from the server.
   * @throws MiscClientException If error is encountered retrieving the resource, or some unexpected
   * problem is encountered.
   * @see org.wattdepot.client.WattDepotClient#getEnergyGenerated getEnergyGenerated
   * @see org.wattdepot.client.WattDepotClient#getEnergyConsumed getEnergyConsumed
   */
  public SensorData getEnergy(String source, XMLGregorianCalendar startTime,
      XMLGregorianCalendar endTime, int samplingInterval) throws NotAuthorizedException,
      ResourceNotFoundException, BadXmlException, MiscClientException {
    String uri =
        Server.SOURCES_URI + "/" + source + "/" + Server.ENERGY_URI + "/" + START_TIME_PARAM
            + startTime.toXMLFormat() + "&endTime=" + endTime.toXMLFormat();
    if (samplingInterval > 0) {
      // client provided sampling interval, so pass to server
      uri = uri + "&samplingInterval=" + Integer.toString(samplingInterval);
    }
    ClientResource client = makeClient(uri);
    ResourceInterface resource = client.wrap(ResourceInterface.class);
    String xmlString = null;
    try {
      xmlString = resource.getXml();
    }
    catch (ResourceException e) {
      Status status = e.getStatus();
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
      else {
        // Some totally unexpected non-success status code, just throw generic client exception
        throw new MiscClientException(status);
      }
    }
    finally {
      client.release();
    }
    Status status = client.getStatus();
    client.release();
    if (status.isSuccess()) {
      try {
        Unmarshaller unmarshaller = sensorDataJAXB.createUnmarshaller();
        return (SensorData) unmarshaller.unmarshal(new StringReader(xmlString));
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
   * Requests the energy in SensorData format from a given Source after the given startTime and with
   * the given sampling interval in minutes. If you are just looking to retrieve the energy values
   * as doubles, the getEnergyGenerated and getEnergyConsumed methods are preferable to this one.
   * 
   * @param source The name of the Source.
   * @param startTime The timestamp of the start of the range.
   * @param samplingInterval The sampling interval in minutes. A value of 0 tells the server to use
   * a default interval.
   * @return The SensorData.
   * @throws NotAuthorizedException If the client is not authorized to retrieve the SensorData.
   * @throws ResourceNotFoundException If the source name provided doesn't exist on the server.
   * @throws BadXmlException If error is encountered unmarshalling the XML from the server.
   * @throws MiscClientException If error is encountered retrieving the resource, or some unexpected
   * problem is encountered.
   * @see org.wattdepot.client.WattDepotClient#getEnergyGenerated getEnergyGenerated
   * @see org.wattdepot.client.WattDepotClient#getEnergyConsumed getEnergyConsumed
   */
  public SensorData getEnergy(String source, XMLGregorianCalendar startTime, int samplingInterval)
      throws NotAuthorizedException, ResourceNotFoundException, BadXmlException,
      MiscClientException {
    String uri =
        Server.SOURCES_URI + "/" + source + "/" + Server.ENERGY_URI + "/" + START_TIME_PARAM
            + startTime.toXMLFormat() + "&endTime=" + Server.LATEST;
    if (samplingInterval > 0) {
      // client provided sampling interval, so pass to server
      uri = uri + "&samplingInterval=" + Integer.toString(samplingInterval);
    }
    ClientResource client = makeClient(uri);
    ResourceInterface resource = client.wrap(ResourceInterface.class);
    String xmlString = null;
    try {
      xmlString = resource.getXml();
    }
    catch (ResourceException e) {
      Status status = e.getStatus();
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
      else {
        // Some totally unexpected non-success status code, just throw generic client exception
        throw new MiscClientException(status);
      }
    }
    finally {
      client.release();
    }
    Status status = client.getStatus();
    client.release();
    if (status.isSuccess()) {
      try {
        Unmarshaller unmarshaller = sensorDataJAXB.createUnmarshaller();
        return (SensorData) unmarshaller.unmarshal(new StringReader(xmlString));
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
   * Requests the energy from a given Source corresponding to the range from startTime to endTime
   * and sampling interval in minutes, and extracts the provided property key, converts it to double
   * and returns the value.
   * 
   * @param source The name of the Source.
   * @param startTime The timestamp of the start of the range.
   * @param endTime The timestamp of the end of the range.
   * @param samplingInterval The sampling interval in minutes.
   * @param key The property key to search for.
   * @return A double representing the property at the given timestamp, or 0 if there is no data.
   * @throws NotAuthorizedException If the client is not authorized to retrieve the power.
   * @throws ResourceNotFoundException If the source name provided doesn't exist on the server.
   * @throws BadXmlException If error is encountered unmarshalling the XML from the server.
   * @throws MiscClientException If error is encountered retrieving the resource, or some unexpected
   * problem is encountered.
   */
  private double getEnergyValue(String source, XMLGregorianCalendar startTime,
      XMLGregorianCalendar endTime, int samplingInterval, String key)
      throws NotAuthorizedException, ResourceNotFoundException, BadXmlException,
      MiscClientException {
    SensorData data = getEnergy(source, startTime, endTime, samplingInterval);
    return data.getProperties().getPropertyAsDouble(key);
  }

  /**
   * Requests the energy from a given Source after the given startTime and with the given sampling
   * interval in minutes, and extracts the provided property key, converts it to double and returns
   * the value.
   * 
   * @param source The name of the Source.
   * @param startTime The timestamp of the start of the range.
   * @param samplingInterval The sampling interval in minutes.
   * @param key The property key to search for.
   * @return A double representing the property at the given timestamp, or 0 if there is no data.
   * @throws NotAuthorizedException If the client is not authorized to retrieve the power.
   * @throws ResourceNotFoundException If the source name provided doesn't exist on the server.
   * @throws BadXmlException If error is encountered unmarshalling the XML from the server.
   * @throws MiscClientException If error is encountered retrieving the resource, or some unexpected
   * problem is encountered.
   */
  private double getEnergyValue(String source, XMLGregorianCalendar startTime,
      int samplingInterval, String key) throws NotAuthorizedException, ResourceNotFoundException,
      BadXmlException, MiscClientException {
    SensorData data = getEnergy(source, startTime, samplingInterval);
    return data.getProperties().getPropertyAsDouble(key);
  }

  /**
   * Requests the energy generated from a given Source corresponding to the range from startTime to
   * endTime and sampling interval in minutes, and returns the value as a double in units of
   * watt-hours.
   * 
   * @param source The name of the Source.
   * @param startTime The timestamp of the start of the range.
   * @param endTime The timestamp of the end of the range.
   * @param samplingInterval The sampling interval in minutes.
   * @return A double representing the energy generated over the given range, or 0 if there is no
   * data.
   * @throws NotAuthorizedException If the client is not authorized to retrieve the power.
   * @throws ResourceNotFoundException If the source name provided doesn't exist on the server.
   * @throws BadXmlException If error is encountered unmarshalling the XML from the server.
   * @throws MiscClientException If error is encountered retrieving the resource, or some unexpected
   * problem is encountered.
   */
  public double getEnergyGenerated(String source, XMLGregorianCalendar startTime,
      XMLGregorianCalendar endTime, int samplingInterval) throws NotAuthorizedException,
      ResourceNotFoundException, BadXmlException, MiscClientException {
    return getEnergyValue(source, startTime, endTime, samplingInterval, SensorData.ENERGY_GENERATED);
  }

  /**
   * Requests the energy generated from a given Source after the given startTime and with the given
   * sampling interval in minutes, and returns the value as a double in units of watt-hours.
   * 
   * @param source The name of the Source.
   * @param startTime The timestamp of the start of the range.
   * @param samplingInterval The sampling interval in minutes.
   * @return A double representing the energy generated over the given range, or 0 if there is no
   * data.
   * @throws NotAuthorizedException If the client is not authorized to retrieve the power.
   * @throws ResourceNotFoundException If the source name provided doesn't exist on the server.
   * @throws BadXmlException If error is encountered unmarshalling the XML from the server.
   * @throws MiscClientException If error is encountered retrieving the resource, or some unexpected
   * problem is encountered.
   */
  public double getEnergyGenerated(String source, XMLGregorianCalendar startTime,
      int samplingInterval) throws NotAuthorizedException, ResourceNotFoundException,
      BadXmlException, MiscClientException {
    return getEnergyValue(source, startTime, samplingInterval, SensorData.ENERGY_GENERATED);
  }

  /**
   * Requests the energy consumed by a given Source corresponding to the range from startTime to
   * endTime and sampling interval in minutes, and returns the value as a double in units of
   * watt-hours.
   * 
   * @param source The name of the Source.
   * @param startTime The timestamp of the start of the range.
   * @param endTime The timestamp of the end of the range.
   * @param samplingInterval The sampling interval in minutes.
   * @return A double representing the energy consumed over the given range, or 0 if there is no
   * data.
   * @throws NotAuthorizedException If the client is not authorized to retrieve the power.
   * @throws ResourceNotFoundException If the source name provided doesn't exist on the server.
   * @throws BadXmlException If error is encountered unmarshalling the XML from the server.
   * @throws MiscClientException If error is encountered retrieving the resource, or some unexpected
   * problem is encountered.
   */
  public double getEnergyConsumed(String source, XMLGregorianCalendar startTime,
      XMLGregorianCalendar endTime, int samplingInterval) throws NotAuthorizedException,
      ResourceNotFoundException, BadXmlException, MiscClientException {
    return getEnergyValue(source, startTime, endTime, samplingInterval, SensorData.ENERGY_CONSUMED);
  }

  /**
   * Requests the energy consumed by a given Source after the given startTime and with the given
   * sampling interval in minutes, and returns the value as a double in units of watt-hours.
   * 
   * @param source The name of the Source.
   * @param startTime The timestamp of the start of the range.
   * @param samplingInterval The sampling interval in minutes.
   * @return A double representing the energy consumed over the given range, or 0 if there is no
   * data.
   * @throws NotAuthorizedException If the client is not authorized to retrieve the power.
   * @throws ResourceNotFoundException If the source name provided doesn't exist on the server.
   * @throws BadXmlException If error is encountered unmarshalling the XML from the server.
   * @throws MiscClientException If error is encountered retrieving the resource, or some unexpected
   * problem is encountered.
   */
  public double getEnergyConsumed(String source, XMLGregorianCalendar startTime,
      int samplingInterval) throws NotAuthorizedException, ResourceNotFoundException,
      BadXmlException, MiscClientException {
    return getEnergyValue(source, startTime, samplingInterval, SensorData.ENERGY_CONSUMED);
  }

  /**
   * Requests the carbon emitted in SensorData format from a given Source corresponding to the given
   * startTime and endTime and sampling interval in minutes. If you are just looking to retrieve the
   * carbon emitted as a double, the getCarbonEmitted method is preferable to this one.
   * 
   * @param source The name of the Source.
   * @param startTime The timestamp of the start of the range.
   * @param endTime The timestamp of the end of the range.
   * @param samplingInterval The sampling interval in minutes. A value of 0 tells the server to use
   * a default interval.
   * @return The SensorData.
   * @throws NotAuthorizedException If the client is not authorized to retrieve the SensorData.
   * @throws ResourceNotFoundException If the source name provided doesn't exist on the server.
   * @throws BadXmlException If error is encountered unmarshalling the XML from the server.
   * @throws MiscClientException If error is encountered retrieving the resource, or some unexpected
   * problem is encountered.
   * @see org.wattdepot.client.WattDepotClient#getCarbonEmitted getCarbonEmitted
   */
  public SensorData getCarbon(String source, XMLGregorianCalendar startTime,
      XMLGregorianCalendar endTime, int samplingInterval) throws NotAuthorizedException,
      ResourceNotFoundException, BadXmlException, MiscClientException {
    String uri =
        Server.SOURCES_URI + "/" + source + "/" + Server.CARBON_URI + "/" + START_TIME_PARAM
            + startTime.toXMLFormat() + "&endTime=" + endTime.toXMLFormat();
    if (samplingInterval > 0) {
      // client provided sampling interval, so pass to server
      uri = uri + "&samplingInterval=" + Integer.toString(samplingInterval);
    }
    ClientResource client = makeClient(uri);
    ResourceInterface resource = client.wrap(ResourceInterface.class);
    String xmlString = null;
    try {
      xmlString = resource.getXml();
    }
    catch (ResourceException e) {
      Status status = e.getStatus();
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
      else {
        // Some totally unexpected non-success status code, just throw generic client exception
        throw new MiscClientException(status);
      }
    }
    finally {
      client.release();
    }

    Status status = client.getStatus();
    client.release();
    if (status.isSuccess()) {
      try {
        Unmarshaller unmarshaller = sensorDataJAXB.createUnmarshaller();
        return (SensorData) unmarshaller.unmarshal(new StringReader(xmlString));
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
   * Requests the carbon emitted in SensorData format from a given Source after the given startTime
   * and with the given sampling interval in minutes. If you are just looking to retrieve the carbon
   * emitted as a double, the getCarbonEmitted method is preferable to this one.
   * 
   * @param source The name of the Source.
   * @param startTime The timestamp of the start of the range.
   * @param samplingInterval The sampling interval in minutes. A value of 0 tells the server to use
   * a default interval.
   * @return The SensorData.
   * @throws NotAuthorizedException If the client is not authorized to retrieve the SensorData.
   * @throws ResourceNotFoundException If the source name provided doesn't exist on the server.
   * @throws BadXmlException If error is encountered unmarshalling the XML from the server.
   * @throws MiscClientException If error is encountered retrieving the resource, or some unexpected
   * problem is encountered.
   * @see org.wattdepot.client.WattDepotClient#getCarbonEmitted getCarbonEmitted
   */
  public SensorData getCarbon(String source, XMLGregorianCalendar startTime, int samplingInterval)
      throws NotAuthorizedException, ResourceNotFoundException, BadXmlException,
      MiscClientException {
    String uri =
        Server.SOURCES_URI + "/" + source + "/" + Server.CARBON_URI + "/" + START_TIME_PARAM
            + startTime.toXMLFormat() + "&endTime=" + Server.LATEST;
    if (samplingInterval > 0) {
      // client provided sampling interval, so pass to server
      uri = uri + "&samplingInterval=" + Integer.toString(samplingInterval);
    }
    ClientResource client = makeClient(uri);
    ResourceInterface resource = client.wrap(ResourceInterface.class);
    String xmlString = null;
    try {
      xmlString = resource.getXml();
    }
    catch (ResourceException e) {
      Status status = e.getStatus();
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
      else {
        // Some totally unexpected non-success status code, just throw generic client exception
        throw new MiscClientException(status);
      }
    }
    finally {
      client.release();
    }
    Status status = client.getStatus();
    client.release();
    if (status.isSuccess()) {
      try {
        Unmarshaller unmarshaller = sensorDataJAXB.createUnmarshaller();
        return (SensorData) unmarshaller.unmarshal(new StringReader(xmlString));
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
   * Requests the carbon emitted from a given Source corresponding to the range from startTime to
   * endTime and sampling interval in minutes, and returns the value as a double in units of lbs CO2
   * equivalent.
   * 
   * @param source The name of the Source.
   * @param startTime The timestamp of the start of the range.
   * @param endTime The timestamp of the end of the range.
   * @param samplingInterval The sampling interval in minutes.
   * @return A double representing the carbon emitted over the given range, or 0 if there is no
   * data.
   * @throws NotAuthorizedException If the client is not authorized to retrieve the power.
   * @throws ResourceNotFoundException If the source name provided doesn't exist on the server.
   * @throws BadXmlException If error is encountered unmarshalling the XML from the server.
   * @throws MiscClientException If error is encountered retrieving the resource, or some unexpected
   * problem is encountered.
   */
  public double getCarbonEmitted(String source, XMLGregorianCalendar startTime,
      XMLGregorianCalendar endTime, int samplingInterval) throws NotAuthorizedException,
      ResourceNotFoundException, BadXmlException, MiscClientException {
    SensorData data = getCarbon(source, startTime, endTime, samplingInterval);
    return data.getProperties().getPropertyAsDouble(SensorData.CARBON_EMITTED);
  }

  /**
   * Requests the carbon emitted from a given Source after the given startTime and with the given
   * sampling interval in minutes, and returns the value as a double in units of lbs CO2 equivalent.
   * 
   * @param source The name of the Source.
   * @param startTime The timestamp of the start of the range.
   * @param samplingInterval The sampling interval in minutes.
   * @return A double representing the carbon emitted over the given range, or 0 if there is no
   * data.
   * @throws NotAuthorizedException If the client is not authorized to retrieve the power.
   * @throws ResourceNotFoundException If the source name provided doesn't exist on the server.
   * @throws BadXmlException If error is encountered unmarshalling the XML from the server.
   * @throws MiscClientException If error is encountered retrieving the resource, or some unexpected
   * problem is encountered.
   */
  public double getCarbonEmitted(String source, XMLGregorianCalendar startTime, int samplingInterval)
      throws NotAuthorizedException, ResourceNotFoundException, BadXmlException,
      MiscClientException {
    SensorData data = getCarbon(source, startTime, samplingInterval);
    return data.getProperties().getPropertyAsDouble(SensorData.CARBON_EMITTED);
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

    String uri =
        Server.SOURCES_URI + "/" + UriUtils.getUriSuffix(data.getSource()) + "/"
            + Server.SENSORDATA_URI + "/" + data.getTimestamp().toXMLFormat();
    ClientResource client = null;
    ResourceInterface resource;
    Status status;

    try {
      client = makeClient(uri);
      resource = client.wrap(ResourceInterface.class);
      resource.store(writer.toString());
      status = client.getStatus();
    }
    catch (ResourceException e) {
      Status exceptionStatus = e.getStatus();

      if (exceptionStatus.equals(Status.CLIENT_ERROR_UNAUTHORIZED)) {
        // credentials were unacceptable to server
        throw new NotAuthorizedException(exceptionStatus);
      }
      if (exceptionStatus.equals(Status.CLIENT_ERROR_NOT_FOUND)) {
        // an unknown source name was specified
        throw new ResourceNotFoundException(exceptionStatus);
      }
      if (exceptionStatus.equals(Status.CLIENT_ERROR_BAD_REQUEST)) {
        // either bad timestamp provided in URI or bad XML in entity body
        throw new BadXmlException(exceptionStatus);
      }
      if (exceptionStatus.equals(Status.CLIENT_ERROR_CONFLICT)) {
        // client attempted to overwrite existing data
        throw new OverwriteAttemptedException(exceptionStatus);
      }
      else {
        // Some totally unexpected non-success status code, just throw generic client exception
        throw new MiscClientException(exceptionStatus);
      }
    }
    finally {
      if (client != null) {
        client.release();
      }
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

    String uri =
        Server.SOURCES_URI + "/" + source + "/" + Server.SENSORDATA_URI + "/"
            + timestamp.toXMLFormat();

    ClientResource client = makeClient(uri);
    ResourceInterface resource = client.wrap(ResourceInterface.class);

    try {
      resource.remove();
    }
    catch (ResourceException e) {
      Status status = e.getStatus();

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
      else {
        // Some totally unexpected non-success status code, just throw generic client exception
        throw new MiscClientException(status);
      }
    }
    finally {
      client.release();
    }

    Status status = client.getStatus();
    client.release();
    if (status.isSuccess()) {
      return true;
    }
    else {
      // Some unexpected type of error received, so punt
      throw new MiscClientException(status);
    }
  }

  /**
   * Deletes all SensorData resources for a particular Source from the server.
   * 
   * @param source The name of the source containing the resource to be deleted.
   * @return True if the SensorDatas could be deleted, false otherwise.
   * @throws NotAuthorizedException If the client is not authorized to store the SensorData.
   * @throws BadXmlException If the server reports the request was bad.
   * @throws ResourceNotFoundException If the source name doesn't exist on the server.
   * @throws MiscClientException If the server indicates an unexpected problem has occurred.
   */
  public boolean deleteAllSensorData(String source) throws NotAuthorizedException,
      ResourceNotFoundException, BadXmlException, MiscClientException {

    ClientResource client =
        makeClient(Server.SOURCES_URI + "/" + source + "/" + Server.SENSORDATA_URI + "/"
            + Server.ALL);
    ResourceInterface resource = client.wrap(ResourceInterface.class);
    try {
      resource.remove();
    }
    catch (ResourceException e) {
      Status status = e.getStatus();

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
      else {
        // Some totally unexpected non-success status code, just throw generic client exception
        throw new MiscClientException(status);
      }
    }
    finally {
      client.release();
    }

    Status status = client.getStatus();
    client.release();
    if (status.isSuccess()) {
      return true;
    }
    else {
      // Some unexpected type of error received, so punt
      throw new MiscClientException(status);
    }
  }

  /**
   * Returns the UserIndex containing all Users on the server. Note that only the admin user is
   * allowed to retrieve the UserIndex.
   * 
   * @return The UserIndex for the server.
   * @throws NotAuthorizedException If the client is not authorized to retrieve the user index.
   * @throws BadXmlException If error is encountered unmarshalling the XML from the server.
   * @throws MiscClientException If error is encountered retrieving the resource, or some unexpected
   * problem is encountered.
   */
  public UserIndex getUserIndex() throws NotAuthorizedException, BadXmlException,
      MiscClientException {

    ClientResource client = makeClient(Server.USERS_URI);
    ResourceInterface resource = client.wrap(ResourceInterface.class);
    String xmlString = null;
    try {
      xmlString = resource.getXml();
    }
    catch (ResourceException e) {
      Status status = e.getStatus();
      if (status.equals(Status.CLIENT_ERROR_UNAUTHORIZED)) {
        // User credentials did not correspond to an admin
        throw new NotAuthorizedException(status);
      }
      else {
        // Some totally unexpected non-success status code, just throw generic client exception
        throw new MiscClientException(status);
      }
    }
    finally {
      client.release();
    }

    Status status = client.getStatus();
    client.release();
    if (status.isSuccess()) {
      try {
        // System.err.println("UserIndex in client: " + xmlString); // DEBUG
        Unmarshaller unmarshaller = userJAXB.createUnmarshaller();
        return (UserIndex) unmarshaller.unmarshal(new StringReader(xmlString));
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
   * Requests a List of all Users on the server. Note that only the admin user is allowed to
   * retrieve the the list of all Users. Currently this is just a convenience method, but if the
   * REST API is extended to support getting Users directly without going through a UserIndex, this
   * method will use that more efficient API call.
   * 
   * @return The List of Users accessible to the user.
   * @throws NotAuthorizedException If the client's credentials are bad.
   * @throws BadXmlException If error is encountered unmarshalling the XML from the server.
   * @throws MiscClientException If error is encountered retrieving the resource, or some unexpected
   * problem is encountered.
   */
  public List<User> getUsers() throws NotAuthorizedException, BadXmlException, MiscClientException {
    List<User> userList = new ArrayList<User>();
    UserIndex index = getUserIndex();
    for (UserRef ref : index.getUserRef()) {
      try {
        userList.add(getUser(ref));
      }
      catch (ResourceNotFoundException e) {
        // We got the SourceIndex already, so we know the source exists. this should never happen
        throw new MiscClientException("SourceRef from Source index had non-existent source name", e);
      }
    }
    return userList;
  }

  /**
   * Returns the named User resource.
   * 
   * @param username The username of the User resource to be retrieved.
   * @return The requested User.
   * @throws NotAuthorizedException If the client is not authorized to retrieve the User.
   * @throws ResourceNotFoundException If the username provided doesn't exist on the server.
   * @throws BadXmlException If error is encountered unmarshalling the XML from the server.
   * @throws MiscClientException If error is encountered retrieving the resource, or some unexpected
   * problem is encountered.
   */
  public User getUser(String username) throws NotAuthorizedException, ResourceNotFoundException,
      BadXmlException, MiscClientException {

    ClientResource client = makeClient(Server.USERS_URI + "/" + username);
    ResourceInterface resource = client.wrap(ResourceInterface.class);
    String xmlString = null;
    try {
      xmlString = resource.getXml();
    }
    catch (ResourceException e) {
      Status status = e.getStatus();
      if (status.equals(Status.CLIENT_ERROR_UNAUTHORIZED)) {
        // credentials were unacceptable to server
        throw new NotAuthorizedException(status);
      }
      if (status.equals(Status.CLIENT_ERROR_NOT_FOUND)) {
        // an unknown username was specified
        throw new ResourceNotFoundException(status);
      }
      else {
        // Some totally unexpected non-success status code, just throw generic client exception
        throw new MiscClientException(status);
      }
    }
    finally {
      client.release();
    }

    Status status = client.getStatus();
    client.release();
    if (status.isSuccess()) {
      try {
        Unmarshaller unmarshaller = userJAXB.createUnmarshaller();
        return (User) unmarshaller.unmarshal(new StringReader(xmlString));
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
   * Stores a User resource in the server. If a User resource with this username already exists, no
   * action is performed and the method throws a OverwriteAttemptedException.
   * 
   * @param user The User resource to be stored.
   * @return True if the User could be stored, false otherwise.
   * @throws JAXBException If there are problems marshalling the object for upload.
   * @throws NotAuthorizedException If the client is not authorized to store the User.
   * @throws BadXmlException If the server reports that the XML sent was bad, or there was no XML,
   * or the fields in the XML don't match the URI.
   * @throws OverwriteAttemptedException If there is already a User on the server with the same
   * username.
   * @throws MiscClientException If the server indicates an unexpected problem has occurred.
   */
  public boolean storeUser(User user) throws JAXBException, NotAuthorizedException,
      BadXmlException, OverwriteAttemptedException, MiscClientException {
    Marshaller marshaller = userJAXB.createMarshaller();
    StringWriter writer = new StringWriter();
    if (user == null) {
      return false;
    }
    else {
      marshaller.marshal(user, writer);
    }

    ClientResource client = makeClient(Server.USERS_URI + "/" + user.getEmail());
    ResourceInterface resource = client.wrap(ResourceInterface.class);
    try {
      resource.store(writer.toString());
    }
    catch (ResourceException e) {
      Status status = e.getStatus();

      if (status.equals(Status.CLIENT_ERROR_UNAUTHORIZED)) {
        // credentials were unacceptable to server
        throw new NotAuthorizedException(status);
      }
      if (status.equals(Status.CLIENT_ERROR_BAD_REQUEST)) {
        // bad XML in entity body
        throw new BadXmlException(status);
      }
      if (status.equals(Status.CLIENT_ERROR_CONFLICT)) {
        // client attempted to overwrite existing data and didn't set overwrite to true
        throw new OverwriteAttemptedException(status);
      }
      else {
        // Some totally unexpected non-success status code, just throw generic client exception
        throw new MiscClientException(status);
      }
    }
    finally {
      client.release();
    }

    Status status = client.getStatus();
    client.release();
    if (status.isSuccess()) {
      return true;
    }
    else {
      // Some unexpected type of error received, so punt
      throw new MiscClientException(status);
    }
  }

  /**
   * Returns the User resource specified by a UserRef.
   * 
   * @param ref The UserRef of the User resource to be retrieved.
   * @return The requested User.
   * @throws NotAuthorizedException If the client is not authorized to retrieve the User.
   * @throws ResourceNotFoundException If the username provided doesn't exist on the server.
   * @throws BadXmlException If error is encountered unmarshalling the XML from the server.
   * @throws MiscClientException If error is encountered retrieving the resource, or some unexpected
   * problem is encountered.
   */
  public User getUser(UserRef ref) throws NotAuthorizedException, ResourceNotFoundException,
      BadXmlException, MiscClientException {
    return getUser(ref.getEmail());
  }

  /**
   * Deletes a User resource from the server.
   * 
   * @param username The username of the User resource to be deleted
   * @return True if the User could be deleted, false otherwise.
   * @throws NotAuthorizedException If the client is not authorized to delete the User.
   * @throws ResourceNotFoundException If the username doesn't exist on the server
   * @throws MiscClientException If the server indicates an unexpected problem has occurred.
   */
  public boolean deleteUser(String username) throws NotAuthorizedException,
      ResourceNotFoundException, MiscClientException {

    ClientResource client = makeClient(Server.USERS_URI + "/" + username);
    ResourceInterface resource = client.wrap(ResourceInterface.class);
    try {
      resource.remove();
    }
    catch (ResourceException e) {
      Status status = e.getStatus();

      if (status.equals(Status.CLIENT_ERROR_UNAUTHORIZED)) {
        // credentials were unacceptable to server
        throw new NotAuthorizedException(status);
      }
      if (status.equals(Status.CLIENT_ERROR_NOT_FOUND)) {
        // an unknown source name was specified, or timestamp could not be found
        throw new ResourceNotFoundException(status);
      }
      else {
        // Some totally unexpected non-success status code, just throw generic client exception
        throw new MiscClientException(status);
      }
    }
    finally {
      client.release();
    }

    Status status = client.getStatus();
    client.release();
    if (status.isSuccess()) {
      return true;
    }
    else {
      // Some unexpected type of error received, so punt
      throw new MiscClientException(status);
    }
  }

  /**
   * Returns the SourceIndex containing all Sources accessible to the authenticated user on the
   * server. When looking to retrieve all the Source objects from the index, getSources is
   * preferable to this method.
   * 
   * @return The SourceIndex for the server.
   * @throws NotAuthorizedException If the client's credentials are bad.
   * @throws BadXmlException If error is encountered unmarshalling the XML from the server.
   * @throws MiscClientException If error is encountered retrieving the resource, or some unexpected
   * problem is encountered.
   * @see org.wattdepot.client.WattDepotClient#getSources getSources
   */
  public SourceIndex getSourceIndex() throws NotAuthorizedException, BadXmlException,
      MiscClientException {

    ClientResource client = makeClient(Server.SOURCES_URI);
    ResourceInterface resource = client.wrap(ResourceInterface.class);
    String xmlString = null;
    try {
      xmlString = resource.getXml();
    }
    catch (ResourceException e) {
      Status status = e.getStatus();
      if (status.equals(Status.CLIENT_ERROR_UNAUTHORIZED)) {
        // User credentials are invalid
        throw new NotAuthorizedException(status);
      }
      else {
        // Some totally unexpected non-success status code, just throw generic client exception
        throw new MiscClientException(status);
      }
    }
    finally {
      client.release();
    }

    Status status = client.getStatus();
    client.release();
    if (status.isSuccess()) {
      try {
        Unmarshaller unmarshaller = sourceJAXB.createUnmarshaller();
        return (SourceIndex) unmarshaller.unmarshal(new StringReader(xmlString));
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
   * server. Uses the fetchAll parameter in the REST API, so only one HTTP request is made.
   * 
   * @return The List of Sources accessible to the user.
   * @throws NotAuthorizedException If the client's credentials are bad.
   * @throws BadXmlException If error is encountered unmarshalling the XML from the server.
   * @throws MiscClientException If error is encountered retrieving the resource, or some unexpected
   * problem is encountered.
   */
  public List<Source> getSources() throws NotAuthorizedException, BadXmlException,
      MiscClientException {
    String uri = Server.SOURCES_URI + "/?fetchAll=true";

    ClientResource client = makeClient(uri);
    ResourceInterface resource = client.wrap(ResourceInterface.class);
    String xmlString = null;
    try {
      xmlString = resource.getXml();
    }
    catch (ResourceException e) {
      Status status = e.getStatus();
      if (status.equals(Status.CLIENT_ERROR_UNAUTHORIZED)) {
        // User credentials are invalid
        throw new NotAuthorizedException(status);
      }
      else {
        // Some totally unexpected non-success status code, just throw generic client exception
        throw new MiscClientException(status);
      }
    }
    finally {
      client.release();
    }

    Status status = client.getStatus();
    client.release();
    if (status.isSuccess()) {
      try {
        Unmarshaller unmarshaller = sourceJAXB.createUnmarshaller();
        Sources sources = (Sources) unmarshaller.unmarshal(new StringReader(xmlString));
        if (sources == null) {
          return null;
        }
        else {
          return sources.getSource();
        }
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

    ClientResource client = null;
    ResourceInterface resource;
    String xmlString = null;
    Status status;
    try {
      client = makeClient(Server.SOURCES_URI + "/" + source);
      resource = client.wrap(ResourceInterface.class);
      xmlString = resource.getXml();
      status = client.getStatus();
    }
    catch (ResourceException e) {
      Status exceptionStatus = e.getStatus();
      if (exceptionStatus.equals(Status.CLIENT_ERROR_UNAUTHORIZED)) {
        // credentials were unacceptable to server
        throw new NotAuthorizedException(exceptionStatus);
      }
      if (exceptionStatus.equals(Status.CLIENT_ERROR_NOT_FOUND)) {
        // an unknown source name was specified
        throw new ResourceNotFoundException(exceptionStatus);
      }
      else {
        // Some totally unexpected non-success status code, just throw generic client exception
        throw new MiscClientException(exceptionStatus);
      }
    }
    finally {
      if (client != null) {
        client.release();
      }
    }

    if (status.isSuccess()) {
      try {
        Unmarshaller unmarshaller = sourceJAXB.createUnmarshaller();
        return (Source) unmarshaller.unmarshal(new StringReader(xmlString));
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
   * Stores a Source resource in the server. If a Source resource with this name already exists, no
   * action is performed and the method throws a OverwriteAttemptedException, unless the overwrite
   * parameter is true, in which case the existing resource is overwritten.
   * 
   * @param source The Source resource to be stored.
   * @param overwrite If true, then overwrite the any existing resource with the given name. If
   * false, return false and do nothing if there is an existing resource with the given name.
   * @return True if the Source could be stored, false otherwise.
   * @throws JAXBException If there are problems marshalling the object for upload.
   * @throws NotAuthorizedException If the client is not authorized to store the Source.
   * @throws BadXmlException If the server reports that the XML sent was bad, or there was no XML,
   * or the fields in the XML don't match the URI.
   * @throws OverwriteAttemptedException If there is already a Source on the server with the same
   * name.
   * @throws MiscClientException If the server indicates an unexpected problem has occurred.
   */
  public boolean storeSource(Source source, boolean overwrite) throws JAXBException,
      NotAuthorizedException, BadXmlException, OverwriteAttemptedException, MiscClientException {
    Marshaller marshaller = sourceJAXB.createMarshaller();
    StringWriter writer = new StringWriter();
    if (source == null) {
      return false;
    }
    else {
      marshaller.marshal(source, writer);
    }

    String overwriteFlag;
    if (overwrite) {
      overwriteFlag = "?overwrite=true";
    }
    else {
      overwriteFlag = "";
    }

    ClientResource client = makeClient(Server.SOURCES_URI + "/" + source.getName() + overwriteFlag);
    ResourceInterface resource = client.wrap(ResourceInterface.class);
    try {
      resource.store(writer.toString());
    }
    catch (ResourceException e) {
      Status status = e.getStatus();

      if (status.equals(Status.CLIENT_ERROR_UNAUTHORIZED)) {
        // credentials were unacceptable to server
        throw new NotAuthorizedException(status);
      }
      if (status.equals(Status.CLIENT_ERROR_BAD_REQUEST)) {
        // bad XML in entity body
        throw new BadXmlException(status);
      }
      if (status.equals(Status.CLIENT_ERROR_CONFLICT)) {
        // client attempted to overwrite existing data and didn't set overwrite to true
        throw new OverwriteAttemptedException(status);
      }
      else {
        // Some totally unexpected non-success status code, just throw generic client exception
        throw new MiscClientException(status);
      }
    }
    finally {
      client.release();
    }

    Status status = client.getStatus();
    client.release();
    if (status.isSuccess()) {
      return true;
    }
    else {
      // Some unexpected type of error received, so punt
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

    ClientResource client =
        makeClient(Server.SOURCES_URI + "/" + source + "/" + Server.SUMMARY_URI);
    ResourceInterface resource = client.wrap(ResourceInterface.class);
    String xmlString = null;
    try {
      xmlString = resource.getXml();
    }
    catch (ResourceException e) {
      Status status = e.getStatus();
      if (status.equals(Status.CLIENT_ERROR_UNAUTHORIZED)) {
        // credentials were unacceptable to server
        throw new NotAuthorizedException(status);
      }
      if (status.equals(Status.CLIENT_ERROR_NOT_FOUND)) {
        // an unknown source name was specified
        throw new ResourceNotFoundException(status);
      }
      else {
        // Some totally unexpected non-success status code, just throw generic client exception
        throw new MiscClientException(status);
      }
    }
    finally {
      client.release();
    }

    Status status = client.getStatus();
    client.release();
    if (status.isSuccess()) {
      try {
        Unmarshaller unmarshaller = sourceSummaryJAXB.createUnmarshaller();
        return (SourceSummary) unmarshaller.unmarshal(new StringReader(xmlString));
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
   * Deletes a Source resource from the server.
   * 
   * @param sourceName The name of the Source resource to be deleted
   * @return True if the Source could be deleted, false otherwise.
   * @throws NotAuthorizedException If the client is not authorized to delete the Source.
   * @throws ResourceNotFoundException If the sourceName doesn't exist on the server
   * @throws MiscClientException If the server indicates an unexpected problem has occurred.
   */
  public boolean deleteSource(String sourceName) throws NotAuthorizedException,
      ResourceNotFoundException, MiscClientException {

    ClientResource client = makeClient(Server.SOURCES_URI + "/" + sourceName);
    ResourceInterface resource = client.wrap(ResourceInterface.class);
    try {
      resource.remove();
    }
    catch (ResourceException e) {
      Status status = e.getStatus();

      if (status.equals(Status.CLIENT_ERROR_UNAUTHORIZED)) {
        // credentials were unacceptable to server
        throw new NotAuthorizedException(status);
      }
      if (status.equals(Status.CLIENT_ERROR_NOT_FOUND)) {
        // an unknown source name was specified, or timestamp could not be found
        throw new ResourceNotFoundException(status);
      }
      else {
        // Some totally unexpected non-success status code, just throw generic client exception
        throw new MiscClientException(status);
      }
    }
    finally {
      client.release();
    }

    Status status = client.getStatus();
    client.release();
    if (status.isSuccess()) {
      return true;
    }
    else {
      // Some unexpected type of error received, so punt
      throw new MiscClientException(status);
    }
  }

  /**
   * Attempts to make a snapshot of the database on the server. Requires admin privileges to
   * complete.
   * 
   * @return True if the snapshot could be created, false otherwise.
   * @throws NotAuthorizedException If the client is not authorized to create the snapshot.
   * @throws MiscClientException If the server rejected the snapshot request for some other reason.
   */
  public boolean makeSnapshot() throws NotAuthorizedException, MiscClientException {

    ClientResource client = makeClient(Server.DATABASE_URI + "/" + "snapshot");
    ResourceInterface resource = client.wrap(ResourceInterface.class);
    try {
      resource.store(null);
    }
    catch (ResourceException e) {
      Status status = e.getStatus();

      if (status.equals(Status.CLIENT_ERROR_UNAUTHORIZED)) {
        // credentials were unacceptable to server, perhaps not admin?
        throw new NotAuthorizedException(status);
      }
      if (status.equals(Status.CLIENT_ERROR_BAD_REQUEST)) {
        // Unexpected, perhaps snapshot method not accepted?
        throw new MiscClientException(status);
      }
      if (status.equals(Status.SERVER_ERROR_INTERNAL)) {
        // Server had a problem creating the snapshot
        return false;
      }
      else {
        // Some totally unexpected non-success status code, just throw generic client exception
        throw new MiscClientException(status);
      }
    }
    finally {
      client.release();
    }

    Status status = client.getStatus();
    client.release();
    if (status.isSuccess()) {
      return true;
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
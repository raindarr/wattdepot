package org.wattdepot.resource.sensordata;

import java.io.IOException;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.XMLGregorianCalendar;
import org.wattdepot.util.tstamp.Tstamp;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.Variant;
import org.wattdepot.resource.WattDepotResource;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.resource.source.jaxb.Source;
import org.wattdepot.server.Server;
import org.wattdepot.server.db.DbBadIntervalException;

/**
 * Represents sensed data about the world. The primary purpose of WattDepot is to allow the storage
 * and retrieval of sensor data.
 * 
 * @author Robert Brewer
 */

public class SensorDataResource extends WattDepotResource {

  /** To be retrieved from the URI, or else null if not found. */
  private String timestamp;
  /** To be retrieved from the URI, or else null if not found. */
  private String startTime;
  /** To be retrieved from the URI, or else null if not found. */
  private String endTime;
  /** fetchAll parameter from the URI, or else false if not found. */
  private boolean fetchAll = false;

  /**
   * Initialize with attributes from the Request.
   */
  @Override  
  protected void doInit() {  
    super.doInit();
    this.timestamp = (String) this.getRequest().getAttributes().get("timestamp");
    this.startTime = (String) this.getRequest().getAttributes().get("startTime");
    this.endTime = (String) this.getRequest().getAttributes().get("endTime");
    String fetchAllString = (String) this.getRequest().getAttributes().get("fetchAll");
    this.fetchAll = "true".equalsIgnoreCase(fetchAllString);
  }
  
  /**
   * Returns a full representation for a given variant.
   * 
   * @param variant the requested variant of this representation
   * @return the representation of this resource
   */
  @Override
  public Representation get(Variant variant) {
    String xmlString;
    // First check if source in URI exists
    if (!validateKnownSource()) {
      return null;
    }
    // If credentials are provided, they need to be valid
    if (!isAnonymous() && !validateCredentials()) {
      return null;
    }
    Source source = dbManager.getSource(uriSource);
    // If source is private, check if current user is allowed to view
    if ((!source.isPublic()) && (!validateSourceOwnerOrAdmin())) {
      return null;
    }
    // If we make it here, we're all clear to send the XML: either source is public or source is
    // private but user is authorized to GET.
    if (variant.getMediaType().equals(MediaType.TEXT_XML)) {
      // If no parameters, must be looking for index of all sensor data for this source
      if ((timestamp == null) && (startTime == null) && (endTime == null)) {
        try {
          xmlString = getSensorDataIndex();
          return getStringRepresentation(xmlString);
        }
        catch (JAXBException e) {
          setStatusInternalError(e);
          return null;
        }
      }
      // If only timestamp parameter provided
      else if ((timestamp != null) && (startTime == null) && (endTime == null)) {
        // Is it a request for latest sensor data?
        if (timestamp.equals(Server.LATEST)) {
          // build XML string
          try {
            xmlString = getLatestSensorData();
            // if we get a null, then there is no SensorData in this source
            if (xmlString == null) {
              setStatusSourceLacksSensorData();
              return null;
            }
            return super.getStringRepresentation(xmlString);
          }
          catch (JAXBException e) {
            setStatusInternalError(e);
            return null;
          }
        }
        // otherwise assume it is a request for a particular timestamp
        else {
          XMLGregorianCalendar timestampObj = null;
          // check if timestamp is OK
          try {
            timestampObj = Tstamp.makeTimestamp(this.timestamp);
          }
          catch (Exception e) {
            setStatusBadTimestamp(this.timestamp);
            return null;
          }
          // build XML string
          try {
            xmlString = getSensorData(timestampObj);
            // if we get a null, then there is no SensorData for this timestamp
            if (xmlString == null) {
              setStatusTimestampNotFound(timestampObj.toString());
              return null;
            }
            return super.getStringRepresentation(xmlString);
          }
          catch (JAXBException e) {
            setStatusInternalError(e);
            return null;
          }
        }
      }
      // If only start and end times are provided, must be looking for a range of sensor data
      else if ((timestamp == null) && (startTime != null) && (endTime != null)) {
        XMLGregorianCalendar startObj = null, endObj = null;
        // check if start timestamp is OK
        try {
          startObj = Tstamp.makeTimestamp(this.startTime);
        }
        catch (Exception e) {
          setStatusBadTimestamp(this.startTime);
          return null;
        }
        // check if end timestamp is OK
        try {
          endObj = Tstamp.makeTimestamp(this.endTime);
        }
        catch (Exception e) {
          setStatusBadTimestamp(this.endTime);
          return null;
        }
        try {
          // If fetchAll requested, return SensorDatas
          if (this.fetchAll) {
            xmlString = getSensorDatas(startObj, endObj);
            return super.getStringRepresentation(xmlString);
          }
          // Otherwise, return SensorDataIndex
          else {
            xmlString = getSensorDataIndex(startObj, endObj);
            return super.getStringRepresentation(xmlString);
          }
        }
        catch (DbBadIntervalException e) {
          setStatusBadInterval(startObj.toString(), endObj.toString());
          return null;
        }
        catch (JAXBException e) {
          setStatusInternalError(e);
          return null;
        }

      }
      // Some bad combination of options, so just fail
      else {
        setStatusMiscError("Request could not be understood.");
        return null;
      }
    }
    // Some MediaType other than text/xml requested
    else {
      return null;
    }
  }
  
  /**
   * Implement the DELETE method that deletes an existing SensorData given its timestamp. Only the
   * SourceOwner (or an admin) can delete a SensorData resource.
   * @param variant The type of Representation to deleted.
   * @return Returns a null Representation.
   */
  @Override
  public Representation delete(Variant variant) {
    // First check if source in URI exists
    if (!validateKnownSource()) {
      return null;
    }
    // If credentials are provided, they need to be valid
    if (!validateCredentials()) {
      return null;
    }
    if (validateSourceOwnerOrAdmin()) {
      // Is it a request to delete all sensor data?
      if (timestamp.equals(Server.ALL)) {
        if (super.dbManager.deleteSensorData(uriSource)) {
          getResponse().setStatus(Status.SUCCESS_OK);
        }
        else {
          // all inputs have been validated by this point, so must be internal error
          setStatusInternalError(String.format("Unable to delete SensorData for timestamp %s",
              this.timestamp));
          return null;
        }
      }
      else {
        XMLGregorianCalendar timestampObj = null;
        // check if timestamp is OK
        try {
          timestampObj = Tstamp.makeTimestamp(this.timestamp);
        }
        catch (Exception e) {
          setStatusBadTimestamp(this.timestamp);
          return null;
        }
        // check if there is any sensor data for given timestamp
        if (!super.dbManager.hasSensorData(uriSource, timestampObj)) {
          setStatusTimestampNotFound(this.timestamp);
          return null;
        }
        if (super.dbManager.deleteSensorData(uriSource, timestampObj)) {
          getResponse().setStatus(Status.SUCCESS_OK);
        }
        else {
          // all inputs have been validated by this point, so must be internal error
          setStatusInternalError(String.format("Unable to delete SensorData for timestamp %s",
              this.timestamp));
          return null;
        }
      }
    }
    else {
      return null;
    }
    return null;
  }

  /**
   * Implement the PUT method that creates a SensorData resource.
   * 
   * @param entity The entity to be put.
   * @param variant The type of Representation to put.
   * @return Returns a null Representation.
   */
  @Override
  public Representation put(Representation entity, Variant variant) {
    // First check if source in URI exists
    if (!validateKnownSource()) {
      return null;
    }
    // If credentials are provided, they need to be valid
    if (!validateCredentials()) {
      return null;
    }
    if (validateSourceOwnerOrAdmin()) {
      XMLGregorianCalendar timestampObj = null;
      // check if timestamp is OK
      try {
        timestampObj = Tstamp.makeTimestamp(this.timestamp);
      }
      catch (Exception e) {
        setStatusBadTimestamp(this.timestamp);
        return null;
      }
      // Get the payload.
      String entityString = null;
      try {
        entityString = entity.getText();
      }
      catch (IOException e) {
        setStatusMiscError("Bad or missing content");
        return null;
      }
      SensorData data;
      // Try to make the XML payload into sensor data, return failure if this fails.
      if ((entityString == null) || ("".equals(entityString))) {
        setStatusMiscError("Entity body was empty");
        return null;
      }
      try {
        data = makeSensorData(entityString);
      }
      catch (JAXBException e) {
        setStatusMiscError("Invalid SensorData representation: " + entityString);
        return null;
      }
      // Return failure if the payload XML timestamp doesn't match the URI timestamp.
      if ((this.timestamp == null) || (!this.timestamp.equals(data.getTimestamp().toString()))) {
        setStatusMiscError("Timestamp in URI does not match timestamp in sensor data instance.");
        return null;
      }
      // Return failure if the SensorData Source doesn't match the uriSource
      Source source = dbManager.getSource(uriSource);
      if (!source.toUri(server).equals(data.getSource())) {
        setStatusMiscError("SensorData payload Source field does not match source field in URI");
        return null;
      }
      // if there is any already existing sensor data for given timestamp, then PUT fails
      if (super.dbManager.hasSensorData(uriSource, timestampObj)) {
        setStatusResourceOverwrite(this.timestamp);
        return null;
      }
      if (dbManager.storeSensorData(data)) {
        getResponse().setStatus(Status.SUCCESS_CREATED);
      }
      else {
        // all inputs have been validated by this point, so must be internal error
        setStatusInternalError(String.format("Unable to create SensorData for timestamp %s",
            this.timestamp));
        return null;
      }
    }
    else {
      return null;
    }
    return null;
  }
}

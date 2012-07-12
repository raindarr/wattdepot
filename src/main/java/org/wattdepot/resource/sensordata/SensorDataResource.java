package org.wattdepot.resource.sensordata;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.XMLGregorianCalendar;
import org.restlet.data.Status;
import org.wattdepot.resource.ResourceInterface;
import org.wattdepot.resource.WattDepotResource;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.resource.source.jaxb.Source;
import org.wattdepot.server.Server;
import org.wattdepot.server.db.DbBadIntervalException;
import org.wattdepot.util.tstamp.Tstamp;

/**
 * Represents sensed data about the world. The primary purpose of WattDepot is to allow the storage
 * and retrieval of sensor data.
 * 
 * @author Robert Brewer
 */

public class SensorDataResource extends WattDepotResource implements ResourceInterface {

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
    this.startTime =
        (String) this.getRequest().getResourceRef().getQueryAsForm().getFirstValue("startTime");
    this.endTime =
        (String) this.getRequest().getResourceRef().getQueryAsForm().getFirstValue("endTime");
    String fetchAllString =
        (String) this.getRequest().getResourceRef().getQueryAsForm().getFirstValue("fetchAll");
    this.fetchAll = "true".equalsIgnoreCase(fetchAllString);
  }

  @Override
  public String getXml() {
    String xmlString;

    // If we make it here, we're all clear to send the XML: either source is public or source is
    // private but user is authorized to GET.
    // If no parameters, must be looking for index of all sensor data for this source
    if ((timestamp == null) && (startTime == null) && (endTime == null)) {
      try {
        return getSensorDataIndex();
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
          return xmlString;
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
          return xmlString;
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
      if (!this.endTime.equals(Server.LATEST)) {
        try {
          endObj = Tstamp.makeTimestamp(this.endTime);
        }
        catch (Exception e) {
          setStatusBadTimestamp(this.endTime);
          return null;
        }
      }
      try {
        // If fetchAll requested, return SensorDatas
        if (this.fetchAll) {
          if (endObj == null) {
            xmlString = getSensorDatas(startObj);
          }
          else {
            xmlString = getSensorDatas(startObj, endObj);
          }
          return xmlString;
        }
        // Otherwise, return SensorDataIndex
        else {
          if (endObj == null) {
            xmlString = getSensorDataIndex(startObj);
          }
          else {
            xmlString = getSensorDataIndex(startObj, endObj);
          }
          return xmlString;
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

  /**
   * Implement the DELETE method that deletes an existing SensorData given its timestamp. Only the
   * SourceOwner (or an admin) can delete a SensorData resource.
   */
  @Override
  public void remove() {
    Source source = validateKnownSource();
    // First check if source in URI exists
    if (source != null && validateSourceOwnerOrAdmin(source)) {
      // Is it a request to delete all sensor data?
      if (timestamp.equals(Server.ALL)) {
        if (super.dbManager.deleteSensorData(uriSource)) {
          getResponse().setStatus(Status.SUCCESS_OK);
        }
        else {
          // all inputs have been validated by this point, so must be internal error
          setStatusInternalError(String.format("Unable to delete SensorData for timestamp %s",
              this.timestamp));
          return;
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
          return;
        }
        // check if there is any sensor data for given timestamp
        if (!super.dbManager.hasSensorData(uriSource, timestampObj)) {
          setStatusTimestampNotFound(this.timestamp);
        }
        else if (super.dbManager.deleteSensorData(uriSource, timestampObj)) {
          getResponse().setStatus(Status.SUCCESS_OK);
        }
        else {
          // all inputs have been validated by this point, so must be internal error
          setStatusInternalError(String.format("Unable to delete SensorData for timestamp %s",
              this.timestamp));
        }
      }
    }
  }

  /**
   * Implement the PUT method that creates a SensorData resource.
   * 
   * @param entity The entity to be put.
   */
  @Override
  public void store(String entity) {
    Source source = validateKnownSource();

    if (source != null && validateSourceOwnerOrAdmin(source)) {
      XMLGregorianCalendar timestampObj = null;
      // check if timestamp is OK
      try {
        timestampObj = Tstamp.makeTimestamp(this.timestamp);
      }
      catch (Exception e) {
        setStatusBadTimestamp(this.timestamp);
        return;
      }
      // Get the payload.
      SensorData data;
      // Try to make the XML payload into sensor data, return failure if this fails.
      if ((entity == null) || ("".equals(entity))) {
        setStatusMiscError("Entity body was empty");
        return;
      }
      try {
        data = makeSensorData(entity);
      }
      catch (JAXBException e) {
        setStatusMiscError("Invalid SensorData representation: " + entity);
        return;
      }
      // Return failure if the payload XML timestamp doesn't match the URI timestamp.
      if ((this.timestamp == null) || (!this.timestamp.equals(data.getTimestamp().toString()))) {
        setStatusMiscError("Timestamp in URI does not match timestamp in sensor data instance.");
      }
      // Return failure if the SensorData Source doesn't match the uriSource
      else if (!source.toUri(server).equals(data.getSource())) {
        setStatusMiscError("The source given in the URI (" + source.toUri(server)
            + ") does not match the source given in the payload (" + data.getSource() + ")");
      }
      // if there is any already existing sensor data for given timestamp, then PUT fails
      else if (super.dbManager.hasSensorData(uriSource, timestampObj)) {
        setStatusResourceOverwrite(this.timestamp);
      }
      else if (dbManager.storeSensorData(data, source)) {
        getResponse().setStatus(Status.SUCCESS_CREATED);
      }
      else {
        // all inputs have been validated by this point, so must be internal error
        setStatusInternalError(String.format("Unable to create SensorData for timestamp %s",
            this.timestamp));
      }
    }
  }
}

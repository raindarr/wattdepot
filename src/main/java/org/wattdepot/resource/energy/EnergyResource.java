package org.wattdepot.resource.energy;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.XMLGregorianCalendar;
import org.wattdepot.util.tstamp.Tstamp;
import org.wattdepot.resource.ResourceInterface;
import org.wattdepot.resource.WattDepotResource;
import org.wattdepot.server.Server;

/**
 * Represents electrical energy determined by sensor data from a particular source.
 * 
 * @author Robert Brewer
 */

public class EnergyResource extends WattDepotResource implements ResourceInterface {

  /** To be retrieved from the URI, or else null if not found. */
  private String startTime, endTime, interval;

  /**
   * Initialize with attributes from the Request.
   */
  @Override
  protected void doInit() {
    super.doInit();
    this.startTime =
        (String) this.getRequest().getResourceRef().getQueryAsForm().getFirstValue("startTime");
    this.endTime =
        (String) this.getRequest().getResourceRef().getQueryAsForm().getFirstValue("endTime");
    this.interval =
        (String) this.getRequest().getResourceRef().getQueryAsForm()
            .getFirstValue("samplingInterval");
  }

  @Override
  public String getXml() {
    String xmlString;

    // If we make it here, we're all clear to send the XML: either source is public or source is
    // private but user is authorized to GET.
    if ((this.startTime == null) || (this.endTime == null)) {
      // Some bad combination of options, so just fail
      setStatusMiscError("Request could not be understood.");
      return null;
    }
    else {
      XMLGregorianCalendar startObj = null, endObj = null;
      int intervalMinutes = 0;
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

      if (this.interval != null) {
        // convert to integer
        try {
          intervalMinutes = Integer.valueOf(this.interval);
        }
        catch (NumberFormatException e) {
          setStatusBadSamplingInterval(this.interval);
        }
      }
      // build XML string
      try {
        if (endObj == null) {
          xmlString = getEnergy(startObj, intervalMinutes);
        }
        else {
          xmlString = getEnergy(startObj, endObj, intervalMinutes);
        }
        // if we get a null, then there is no SensorData for this range
        if (xmlString == null) {
          setStatusBadRange(startTime, endTime);
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

  @Override
  public void store(String entity) {
    setStatusMethodNotAllowed();
  }

  @Override
  public void remove() {
    setStatusMethodNotAllowed();
  }
}

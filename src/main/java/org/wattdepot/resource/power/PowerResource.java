package org.wattdepot.resource.power;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.XMLGregorianCalendar;
import org.wattdepot.util.tstamp.Tstamp;
import org.wattdepot.resource.ResourceInterface;
import org.wattdepot.resource.WattDepotResource;

/**
 * Represents electrical power determined by sensor data from a particular source.
 * 
 * @author Robert Brewer
 */

public class PowerResource extends WattDepotResource implements ResourceInterface {

  /** To be retrieved from the URI, or else null if not found. */
  private String timestamp;

  /**
   * Initialize with attributes from the Request.
   */
  @Override
  protected void doInit() {
    super.doInit();
    this.timestamp = (String) this.getRequest().getAttributes().get("timestamp");
  }

  @Override
  public String getXml() {
    String xmlString;

    // If we make it here, we're all clear to send the XML: either source is public or source is
    // private but user is authorized to GET.
    // If no timestamp, give up
    if (timestamp == null) {
      setStatusBadTimestamp(this.timestamp);
      return null;
    }
    // we have a timestamp parameter
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
        xmlString = getPower(timestampObj);
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

  @Override
  public void store(String entity) {
    setStatusMethodNotAllowed();
  }

  @Override
  public void remove() {
    setStatusMethodNotAllowed();
  }
}

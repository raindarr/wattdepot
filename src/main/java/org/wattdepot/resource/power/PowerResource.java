package org.wattdepot.resource.power;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.XMLGregorianCalendar;
import org.wattdepot.util.tstamp.Tstamp;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.Variant;
import org.wattdepot.resource.WattDepotResource;
import org.wattdepot.resource.source.jaxb.Source;

/**
 * Represents electrical power determined by sensor data from a particular source.
 * 
 * @author Robert Brewer
 */

public class PowerResource extends WattDepotResource {

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
          return super.getStringRepresentation(xmlString);
        }
        catch (JAXBException e) {
          setStatusInternalError(e);
          return null;
        }
      }
    }
    // Some MediaType other than text/xml requested
    else {
      return null;
    }
  }
}

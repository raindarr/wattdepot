package org.wattdepot.resource.source.summary;

import javax.xml.bind.JAXBException;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.Variant;
import org.wattdepot.resource.WattDepotResource;

/**
 * Represents a summary of a source.
 * 
 * @author Robert Brewer
 */

public class SourceSummaryResource extends WattDepotResource {

  /**
   * Returns a full representation for a given variant.
   * 
   * @param variant the requested variant of this representation
   * @return the representation of this resource
   */
  @Override
  public Representation get(Variant variant) {
    String xmlString;

    // If we make it here, we're all clear to send the XML: either source is public or source is
    // private but user is authorized to GET.
    if (variant.getMediaType().equals(MediaType.TEXT_XML)) {
      try {
        xmlString = getSourceSummary();
      }
      catch (JAXBException e) {
        setStatusInternalError(e);
        return null;
      }
      return getStringRepresentation(xmlString);
    }
    // Some MediaType other than text/xml requested
    else {
      return null;
    }

  }
}

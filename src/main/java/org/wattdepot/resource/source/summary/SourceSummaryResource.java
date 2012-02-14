package org.wattdepot.resource.source.summary;

import javax.xml.bind.JAXBException;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.Variant;
import org.wattdepot.resource.WattDepotResource;
import org.wattdepot.resource.source.jaxb.Source;

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
    // If credentials are provided, they need to be valid
    if (!isAnonymous() && !validateCredentials()) {
      return null;
    }
    // First check if source in URI exists
    if (validateKnownSource()) {
      Source source = dbManager.getSource(uriSource);
      // If source is private, check if current user is allowed to view
      if ((!source.isPublic()) && (!validateSourceOwnerOrAdmin())) {
        return null;
      }
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
    else {
      // unknown source
      return null;
    }
  }
}

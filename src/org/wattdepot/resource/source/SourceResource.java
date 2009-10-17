package org.wattdepot.resource.source;

import javax.xml.bind.JAXBException;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import org.wattdepot.resource.WattDepotResource;
import org.wattdepot.resource.source.jaxb.Source;

/**
 * Represents a source of sensor data, such a power meter. Sources can also be virtual, consisting
 * of an aggregation of other Sources.
 * 
 * @author Robert Brewer
 */

public class SourceResource extends WattDepotResource {

  /**
   * Creates a new SourceResource object with the provided parameters, and only a text/xml
   * representation.
   * 
   * @param context Restlet context for the resource
   * @param request Restlet request
   * @param response Restlet response
   */
  public SourceResource(Context context, Request request, Response response) {
    super(context, request, response);
  }

  /**
   * Returns a full representation for a given variant.
   * 
   * @param variant the requested variant of this representation
   * @return the representation of this resource
   * @throws ResourceException when the requested resource cannot be represented as requested.
   */
  @Override
  public Representation represent(Variant variant) throws ResourceException {
    String xmlString;
    // If credentials are provided, they need to be valid
    if (!isAnonymous() && !validateCredentials()) {
      return null;
    }
    if (uriSource == null) {
      if (variant.getMediaType().equals(MediaType.TEXT_XML)) {
        // URI had no source parameter, which means the request is for the list of all sources
        try {
          if (isAnonymous()) {
            // anonymous users get only the public sources
            xmlString = getPublicSources();
          }
          else if (isAdminUser()) {
            // admin user can see all sources
            xmlString = getAllSources();
          }
          else {
            // Authenticated as some user
            xmlString = getOwnerSources();
          }
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
      // First check if source in URI exists
      if (!validateKnownSource()) {
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
        try {
          xmlString = getSource();
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

  /**
   * Indicate the PUT method is not supported, until I have time to implement it.
   * 
   * @return False.
   */
  @Override
  public boolean allowPut() {
    return false;
  }

  /**
   * Indicate the DELETE method is not supported, until I have time to implement it.
   * 
   * @return False.
   */
  @Override
  public boolean allowDelete() {
    return false;
  }
}

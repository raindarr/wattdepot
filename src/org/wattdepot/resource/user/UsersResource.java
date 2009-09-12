package org.wattdepot.resource.user;

import javax.xml.bind.JAXBException;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import org.wattdepot.resource.WattDepotResource;

/**
 * Represents a set of WattDepot users.
 * 
 * @author Robert Brewer
 */

public class UsersResource extends WattDepotResource {
  /**
   * Creates a new UsersResource object with the provided parameters, and only a text/xml
   * representation.
   * 
   * @param context Restlet context for the resource
   * @param request Restlet request
   * @param response Restlet response
   */
  public UsersResource(Context context, Request request, Response response) {
    super(context, request, response);

    // This resource has only one type of representation.
    getVariants().add(new Variant(MediaType.TEXT_XML));
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
    try {
      if (variant.getMediaType().equals(MediaType.TEXT_XML)) {
        String xmlString = super.getUserIndex();
        server.getLogger().info("UserIndex in Resource: " + xmlString);
        return super.getStringRepresentation(xmlString);
      }
    }
    catch (JAXBException e) {
      setStatusInternalError(e);
    }
    return null;
  }
}

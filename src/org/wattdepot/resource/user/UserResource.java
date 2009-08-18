package org.wattdepot.resource.user;

import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;

/**
 * Represents a particular user of WattDepot. While most access to WattDepot can be done
 * anonymously, creating Sources and Sensor Data must be done by a registered User. The information
 * in the User resource is also used to authenticate and authorize access to the entire service via
 * HTTP authentication.
 * 
 * @author Robert Brewer
 */

public class UserResource extends Resource {
  /**
   * Creates a new UsersResource object with the provided parameters, and only a text/plain
   * representation.
   * 
   * @param context Restlet context for the resource
   * @param request Restlet request
   * @param response Restlet response
   */
  public UserResource(Context context, Request request, Response response) {
    super(context, request, response);

    // This resource has only one type of representation.
    getVariants().add(new Variant(MediaType.TEXT_PLAIN));
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
    return new StringRepresentation("User resource", MediaType.TEXT_PLAIN);
  }

}

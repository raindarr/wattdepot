package org.wattdepot.resource;

import org.restlet.data.MediaType;
import org.restlet.resource.ServerResource;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.representation.Variant;

/**
 * Represents the health of the WattDepot service. It is provided as a way to determine if the
 * WattDepot service is up and functioning normally (like a "ping" resource).
 * 
 * @author Robert Brewer
 */
public class NoResource extends ServerResource {

  /**
   * String to send as a response to the health request.
   */
  protected static final String MESSAGE_TEXT =
      "There are no WattDepot resources that match your request.<br/><br/>"
          + "See <a href=\"http://code.google.com/p/wattdepot/wiki/RestApi\">the RestApi</a> for help.";

  /**
   * Initialize with attributes from the Request.
   */
  @Override
  protected void doInit() {
    super.doInit();
    // This resource has only one type of representation.
    getVariants().add(new Variant(MediaType.TEXT_HTML));
  }

  /**
   * Returns a full representation for a given variant.
   * 
   * @param variant the requested variant of this representation
   * @return the representation of this resource
   */
  @Override
  public Representation get(Variant variant) {
    return new StringRepresentation(MESSAGE_TEXT, MediaType.TEXT_HTML);
  }

}

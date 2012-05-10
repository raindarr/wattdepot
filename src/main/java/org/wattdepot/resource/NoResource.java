package org.wattdepot.resource;

import org.restlet.data.MediaType;
import org.restlet.representation.Variant;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

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
      "<img src=\"http://code.google.com/p/wattdepot/logo?cct=1332883388\"/><br/>"
          + "Welcome to WattDepot!<br/><br/>"
          + "There are no WattDepot resources that match your request.<br/><br/>"
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
   * The GET method for HTML data.
   * 
   * @return An HTML message.
   */
  @Get("html")
  public String getTxt() {
    return MESSAGE_TEXT;
    // return new StringRepresentation(HEALTH_MESSAGE_TEXT, MediaType.TEXT_PLAIN);
  }

}

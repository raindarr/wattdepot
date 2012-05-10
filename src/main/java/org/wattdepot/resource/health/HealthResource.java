package org.wattdepot.resource.health;

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
public class HealthResource extends ServerResource {

  /**
   * String to send as a response to the health request.
   */
  protected static final String HEALTH_MESSAGE_TEXT = "WattDepot is alive.";

  /**
   * Initialize with attributes from the Request.
   */
  @Override
  protected void doInit() {
    super.doInit();
    // This resource has only one type of representation.
    getVariants().add(new Variant(MediaType.TEXT_PLAIN));
  }

  /**
   * The GET method for plain text data.
   * 
   * @return The text representation of this resource
   */
  @Get("txt")
  public String getTxt() {
    return HEALTH_MESSAGE_TEXT;
  }
}

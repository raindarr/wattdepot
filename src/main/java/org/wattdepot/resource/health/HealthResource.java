package org.wattdepot.resource.health;

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
   * Returns a full representation for a given variant.
   * 
   * @param variant the requested variant of this representation
   * @return the representation of this resource
   */
  @Override
  public Representation get(Variant variant)  {
    return new StringRepresentation(HEALTH_MESSAGE_TEXT, MediaType.TEXT_PLAIN);
  }

}

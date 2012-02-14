package org.wattdepot.tinker;

import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.resource.ServerResource;
import org.restlet.resource.ResourceException;
import org.restlet.representation.StringRepresentation;
import org.restlet.representation.Variant;

/**
 * It's the machine that goes ping! http://www.youtube.com/watch?v=NcHdF1eHhgc&feature=channel_page
 * Based on the code from the Restlet first steps tutorial:
 * 
 * http://www.restlet.org/documentation/1.1/firstSteps
 * 
 * @author Robert Brewer
 */
public class PingResource extends ServerResource {

  /**
   * String to send as a response to the ping. 
   */
  public static final String HELLO_WORLD_TEXT = "Hello World!";
  
  /**
   * Creates a new PingResource object with the provided parameters, and only a text/plain
   * representation.
   * 
   * @param context Restlet context for the resource
   * @param request Restlet request
   * @param response Restlet response
   */
/*  public PingResource(Context context, Request request, Response response) {
    super(context, request, response);
    super();
    this.setRequest(request);
    this.setResponse(response);

    // This representation has only one type of representation.
    getVariants().add(new Variant(MediaType.TEXT_PLAIN));
  } */
  
  /**
   * Initialize with only a text/plain representation.
   */
  @Override  
  protected void doInit() throws ResourceException {  
    getVariants().add(new Variant(MediaType.TEXT_PLAIN));
  }

  /**
   * Returns a full representation for a given variant.
   * 
   * @param variant the requested variant of this representation
   * @return the representation of this resource, which is always "Hello World!" in plain text.
   * @throws ResourceException when the requested resource cannot be represented as requested.
   */
  @Override
  public Representation get(Variant variant) {
    return new StringRepresentation(HELLO_WORLD_TEXT, MediaType.TEXT_PLAIN);
  }

}

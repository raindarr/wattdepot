package org.wattdepot.resource;

import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Put;

/**
 * Annotated interface representing the contract between the client and the server for the main
 * resources.
 * 
 * @author Andrea Connell
 * 
 */

public interface ResourceInterface {

  /**
   * The GET method for XML data.
   * 
   * @return The requested data as XML
   */
  @Get("xml")
  public String getXml();

  /**
   * The PUT method.
   * 
   * @param entity The entity to store
   */
  @Put()
  public void store(String entity);

  /**
   * The DELETE method.
   */
  @Delete
  public void remove();

}

/**
 * 
 */
package org.wattdepot.resource.db;

import org.restlet.data.Status;
import org.restlet.resource.Put;
import org.wattdepot.resource.WattDepotResource;

/**
 * The Database resource is used by an administrator to perform certain actions on the database that
 * persists resources in WattDepot.
 * 
 * @author Robert Brewer
 */
public class DatabaseResource extends WattDepotResource {

  /** Contains the database method desired. */
  protected String methodString;

  /**
   * Initialize with attributes from the Request.
   */
  @Override
  protected void doInit() {
    super.doInit();
    this.methodString = (String) this.getRequest().getAttributes().get("method");
  }

  /**
   * Implement the PUT method that executes the method provided.
   * 
   * @param entity The entity to be posted.
   */
  @Put
  public void snapshot(String entity) {
    if (isAdminUser()) {
      if ("snapshot".equalsIgnoreCase(this.methodString)) {
        if (super.dbManager.makeSnapshot()) {
          getResponse().setStatus(Status.SUCCESS_CREATED);
        }
        else {
          // all inputs have been validated by this point, so must be internal error
          setStatusInternalError("Unable to create database snapshot");
          return;
        }
      }
      else {
        // Unknown method requested, return error
        setStatusMiscError("Bad method passed to Database resource");
      }
    }
    else {
      setStatusBadCredentials();
      return;
    }
  }
}

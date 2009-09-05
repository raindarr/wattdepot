package org.wattdepot.client;

import org.restlet.data.Status;

/**
 * An exception that is thrown when the WattDepot server encounters an error when performing an
 * operation.
 * 
 * @author Philip Johnson
 * @author Robert Brewer
 */
public class WattDepotClientException extends Exception {

  /** The default serial version UID. */
  private static final long serialVersionUID = 1L;

  /**
   * Thrown when an unsuccessful status code is returned from the Server.
   * 
   * @param status The Status instance indicating the problem.
   */
  public WattDepotClientException(Status status) {
    super(status.getCode() + ": " + status.getDescription());
  }

  /**
   * Thrown when an unsuccessful status code is returned from the Server.
   * 
   * @param status The status instance indicating the problem.
   * @param error The previous error.
   */
  public WattDepotClientException(Status status, Throwable error) {
    super(status.getCode() + ": " + status.getDescription(), error);
  }

  /**
   * Thrown when some problem occurs with Client not involving the server.
   * 
   * @param description The problem description.
   * @param error The previous error.
   */
  public WattDepotClientException(String description, Throwable error) {
    super(description, error);
  }

  /**
   * Thrown when some problem occurs with Client not involving the server.
   * 
   * @param description The problem description.
   */
  public WattDepotClientException(String description) {
    super(description);
  }
}

package org.wattdepot.server;

import org.wattdepot.resource.user.jaxb.User;
import org.wattdepot.server.db.DbManager;
import org.restlet.security.SecretVerifier;

/**
 * Authenticates a user based on username and password credentials.
 * 
 * @author Andrea Connell
 * 
 */
public class WattDepotVerifier extends SecretVerifier {

  /** The DbManager to check user credentials against. */
  private DbManager dbManager;

  /**
   * Create a new WattDepotVerifier with a specific DbManager.
   * 
   * @param dbManager The DbManager to check user credentials against.
   */
  public WattDepotVerifier(DbManager dbManager) {
    this.dbManager = dbManager;
  }

  @Override
  public int verify(String identifier, char[] secret) throws IllegalArgumentException {
    User user = dbManager.getUser(identifier);
    if (user == null) {
      return RESULT_UNKNOWN;
    }
    else if (compare(user.getPassword().toCharArray(), secret)) {
      return RESULT_VALID;
    }
    else {
      return RESULT_INVALID;
    }
  }
}

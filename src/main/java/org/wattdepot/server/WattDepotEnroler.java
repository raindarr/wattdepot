package org.wattdepot.server;

import org.restlet.data.ClientInfo;
import org.restlet.security.Enroler;
import org.restlet.security.Role;
import org.wattdepot.resource.user.jaxb.User;
import org.wattdepot.server.db.DbManager;

/**
 * Adds security roles to a specific user.
 * 
 * @author Andrea Connell
 * 
 */
public class WattDepotEnroler implements Enroler {

  /** The Administrator role. */
  public static final Role ADMINISTRATOR = new Role("admin", "administrator");

  /** The User role. */
  public static final Role USER = new Role("user", "non-administrator user");

  /** The DbManager to check user credentials against. */
  private DbManager dbManager;

  /**
   * Create a new WattDepotEnroler with a specific DbManager.
   * 
   * @param dbManager The DbManager to check user credentials against.
   */
  public WattDepotEnroler(DbManager dbManager) {
    this.dbManager = dbManager;
  }

  @Override
  public void enrole(ClientInfo client) {
    User user = dbManager.getUser(client.getUser().getIdentifier());
    if (user == null) {
      client.setAuthenticated(false);
    }
    else if (user.isAdmin()) {
      client.getRoles().add(ADMINISTRATOR);
      client.getRoles().add(USER);
    }
    else {
      client.getRoles().add(USER);
    }

  }

}

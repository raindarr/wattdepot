package org.wattdepot.resource.user;

import org.wattdepot.resource.user.jaxb.Properties;
import org.wattdepot.resource.user.jaxb.Property;
import org.wattdepot.resource.user.jaxb.User;
import org.wattdepot.resource.user.jaxb.UserRef;
import org.wattdepot.server.Server;

/**
 * Some convenience methods for creating User and associated resources programmatically.
 * 
 * @author Robert Brewer
 */
public class UserUtils {

  /**
   * Returns a new User object with the provided parameters. Needs to be kept up to date with any
   * changes to the User schema, which is bogus.
   * 
   * @param username The username for the User.
   * @param password The password for the User.
   * @param adminp Whether the User is an administrator.
   * @param props The properties for the User.
   * @return The freshly created User object.
   */
  public static User makeUser(String username, String password, boolean adminp, Properties props) {
    User user = new User();
    user.setEmail(username);
    user.setPassword(password);
    user.setAdmin(adminp);
    user.setProperties(props);
    return user;
  }

  /**
   * Returns a new User Property object with the provided key and value.
   * 
   * @param key The key.
   * @param value The value.
   * @return The freshly created Property object.
   */
  public static Property makeUserProperty(String key, String value) {
    Property prop = new Property();
    prop.setKey(key);
    prop.setValue(value);
    return prop;
  }

  /**
   * Creates a UserRef object from a User object. The Server argument is required to build the URI
   * in the UserRef pointing to the full User resource. Needs to be kept up to date with any
   * changes to the User or UserRef schemas, which is bogus.
   * 
   * @param user The User to build the UserRef from.
   * @param server The Server where the User is located.
   * @return The new UserRef object.
   */
  public static UserRef makeUserRef(User user, Server server) {
    UserRef ref = new UserRef();
    ref.setEmail(user.getEmail());
    ref.setHref(server.getHostName() + Server.USERS_URI + "/" + user.getEmail());
    return ref;
  }

  /**
   * Determines if the subset of user information in a UserRef is equal to particular User object.
   * Note that only the final segment of the href field of the UserRef is compared to the User
   * object, as the User object does not contain its own URI. Thus if the UserRef was from a
   * different server than the User object, this test would return true even though the UserRef
   * points to a different copy of this User object.
   * 
   * @param ref The UserRef to be compared.
   * @param user The User to be compared.
   * @return True if all the fields in the UserRef correspond to the User
   */
  public static boolean userRefEqualsUser(UserRef ref, User user) {
    String hrefUsername = ref.getHref().substring(ref.getHref().lastIndexOf('/') + 1);

    return (ref.getEmail().equals(user.getEmail()) && (user.getEmail().equals(hrefUsername)));
  }
  
  /**
   * Given a User object and the Server it belongs to, returns the URI to that User resource.
   *  
   * @param user The User object under consideration.
   * @param server The Server user belongs to.
   * @return The URI to the User resource corresponding to the given User.
   */
  public static String userToUri(User user, Server server) {
    return server.getHostName() + Server.USERS_URI + "/" + user.getEmail();
  }
  
  /**
   * Given a username and the Server it belongs to, returns the URI to that User resource.
   *  
   * @param username The name of the User object under consideration.
   * @param server The Server user belongs to.
   * @return The URI to the User resource corresponding to the given User.
   */
  public static String userToUri(String username, Server server) {
    return server.getHostName() + Server.USERS_URI + "/" + username;
  }
  
  /**
   * Takes the URI to a User resource on an arbitrary WattDepot server, and turns it into a URI for
   * that user on the provided server. This is useful when reading a Source resource from a file,
   * where the stored URI might point to an owner resource that is on a different server.
   *  
   * @param uri The URI that is to be updated.
   * @param server The current server instance.
   * @return A URI String for the given user on the given server.
   */
  public static String updateUri(String uri, Server server) {
    // Grab out the username at the end of the URI
    String username = uri.substring(uri.lastIndexOf('/') + 1);
    return userToUri(username, server);
  }
}

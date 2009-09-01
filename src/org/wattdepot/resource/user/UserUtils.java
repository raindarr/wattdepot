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
   * Returns a new User object with the provided parameters.
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
   * in the UserRef pointing to the full User resource.
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
}

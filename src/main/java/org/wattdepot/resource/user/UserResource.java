package org.wattdepot.resource.user;

import javax.xml.bind.JAXBException;
import org.restlet.data.Status;
import org.wattdepot.resource.ResourceInterface;
import org.wattdepot.resource.WattDepotResource;
import org.wattdepot.resource.user.jaxb.User;

/**
 * Represents a particular user of WattDepot. While most access to WattDepot can be done
 * anonymously, creating Sources and Sensor Data must be done by a registered User. The information
 * in the User resource is also used to authenticate and authorize access to the entire service via
 * HTTP authentication.
 * 
 * @author Robert Brewer
 */

public class UserResource extends WattDepotResource implements ResourceInterface {

  @Override
  public String getXml() {
    if (uriUser == null) {
      // URI had no user parameter, which means the request is for the list of all users
      try {
        if (isAdminUser()) {
          // admin user can see all users
          return getUserIndex();
        }
        else {
          // Authenticated as some user
          setStatusBadCredentials();
          return null;
        }
      }
      catch (JAXBException e) {
        setStatusInternalError(e);
        return null;
      }
    }
    else {
      User user = dbManager.getUser(uriUser);
      if (user == null) {
        // Note that technically this doesn't represent bad credentials, it is a request for a
        // user that doesn't exist. However, if the user doesn't exist, then any credentials
        // provided will be invalid. If we returned a different status code (like 404) that
        // would leak information about what users exist, which is bad.
        setStatusBadCredentials();
        return null;
      }
      else {
        if (isAdminUser()
            || (user.getEmail().equals(authUsername) && user.getPassword().equals(authPassword))) {
          try {
            return getUser(user);
          }
          catch (JAXBException e) {
            setStatusInternalError(e);
            return null;
          }
        }
        else {
          setStatusBadCredentials();
          return null;
        }
      }
    }
  }

  /**
   * Implement the PUT method that creates a User resource.
   * 
   * @param entity The entity to be put.
   */
  @Override
  public void store(String entity) {
    // Need to be an admin to create a user
    if (!isAdminUser()) {
      setStatusBadCredentials();
      return;
    }
    // Get the payload.
    String entityString = null;
    entityString = entity;
    User user;
    String userName;
    // Try to make the XML payload into User, return failure if this fails.
    if ((entityString == null) || ("".equals(entityString))) {
      setStatusMiscError("Entity body was empty");
      return;
    }
    try {
      user = makeUser(entityString);
      userName = user.getEmail();
    }
    catch (JAXBException e) {
      setStatusMiscError("Invalid User representation: " + entityString);
      return;
    }
    // Return failure if the name of User doesn't match the name given in URI
    if (!uriUser.equals(userName)) {
      setStatusMiscError("Username field does not match user field in URI");
      return;
    }

    if (super.dbManager.getUser(uriUser) == null) {
      if (dbManager.storeUser(user)) {
        getResponse().setStatus(Status.SUCCESS_CREATED);
      }
      else {
        // all inputs have been validated by this point, so must be internal error
        setStatusInternalError(String.format("Unable to create User named %s", uriUser));
        return;
      }
    }
    else {
      // if User with given name already exists and not overwriting, then fail
      setStatusResourceOverwrite(uriUser);
      return;
    }
  }

  /**
   * Implement the DELETE method that deletes an existing User. Only the admin can delete a User
   * resource.
   */
  @Override
  public void remove() {
    if (isAdminUser() && dbManager.getUser(uriUser) != null) {
      if (super.dbManager.deleteUser(this.uriUser)) {
        getResponse().setStatus(Status.SUCCESS_OK);
      }
      else {
        setStatusInternalError(String.format("Unable to delete User %s", this.uriUser));
        return;
      }
    }
    else {
      setStatusBadCredentials();
      return;
    }
  }
}

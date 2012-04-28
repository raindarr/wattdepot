package org.wattdepot.resource.user;

import java.io.IOException;
import javax.xml.bind.JAXBException;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.Variant;
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

public class UserResource extends WattDepotResource {

  /**
   * Returns a full representation for a given variant.
   * 
   * @param variant the requested variant of this representation
   * @return the representation of this resource
   */
  @Override
  public Representation get(Variant variant) {
    String xmlString;
    if (variant.getMediaType().equals(MediaType.TEXT_XML)) {
      if (uriUser == null) {
        // URI had no user parameter, which means the request is for the list of all users
        try {
          if (isAdminUser()) {
            // admin user can see all users
            xmlString = getUserIndex();
            return getStringRepresentation(xmlString);
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
              xmlString = getUser(user);
              return getStringRepresentation(xmlString);
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
    // Some MediaType other than text/xml requested
    else {
      return null;
    }
  }

  /**
   * Implement the PUT method that creates a User resource.
   * 
   * @param entity The entity to be put.
   * @param variant The type of Representation to put.
   * @return Returns a null Representation.
   */
  @Override
  public Representation put(Representation entity, Variant variant) {
    // Need to be an admin to create a user
    if (!isAdminUser()) {
      setStatusBadCredentials();
      return null;
    }
    // Get the payload.
    String entityString = null;
    try {
      entityString = entity.getText();
    }
    catch (IOException e) {
      setStatusMiscError("Bad or missing content");
      return null;
    }
    User user;
    String userName;
    // Try to make the XML payload into User, return failure if this fails.
    if ((entityString == null) || ("".equals(entityString))) {
      setStatusMiscError("Entity body was empty");
      return null;
    }
    try {
      user = makeUser(entityString);
      userName = user.getEmail();
    }
    catch (JAXBException e) {
      setStatusMiscError("Invalid User representation: " + entityString);
      return null;
    }
    // Return failure if the name of User doesn't match the name given in URI
    if (!uriUser.equals(userName)) {
      setStatusMiscError("Username field does not match user field in URI");
      return null;
    }

    if (super.dbManager.getUser(uriUser) == null) {
      if (dbManager.storeUser(user)) {
        getResponse().setStatus(Status.SUCCESS_CREATED);
      }
      else {
        // all inputs have been validated by this point, so must be internal error
        setStatusInternalError(String.format("Unable to create User named %s", uriUser));
        return null;
      }
    }
    else {
      // if User with given name already exists and not overwriting, then fail
      setStatusResourceOverwrite(uriUser);
      return null;
    }

    return null;
  }

  /**
   * Implement the DELETE method that deletes an existing User. Only the admin can delete a User
   * resource.
   * 
   * @param variant The type of Representation to deleted.
   * @return Returns a null Representation.
   */
  @Override
  public Representation delete(Variant variant) {
    if (isAdminUser() && dbManager.getUser(uriUser) != null) {
      if (super.dbManager.deleteUser(this.uriUser)) {
        getResponse().setStatus(Status.SUCCESS_OK);
      }
      else {
        setStatusInternalError(String.format("Unable to delete User %s", this.uriUser));
        return null;
      }
    }
    else {
      setStatusBadCredentials();
      return null;
    }
    return null;
  }

}

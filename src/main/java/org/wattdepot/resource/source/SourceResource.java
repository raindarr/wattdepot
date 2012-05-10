package org.wattdepot.resource.source;

import javax.xml.bind.JAXBException;
import org.restlet.data.Status;
import org.wattdepot.resource.ResourceInterface;
import org.wattdepot.resource.WattDepotResource;
import org.wattdepot.resource.source.jaxb.Source;

/**
 * Represents a source of sensor data, such a power meter. Sources can also be virtual, consisting
 * of an aggregation of other Sources.
 * 
 * @author Robert Brewer
 */

public class SourceResource extends WattDepotResource implements ResourceInterface {

  /** fetchAll parameter from the URI, or else false if not found. */
  private boolean fetchAll = false;

  /** overwrite parameter from the URI, or else false if not found. */
  private boolean overwrite = false;

  /**
   * Initialize with attributes from the Request.
   */
  @Override
  protected void doInit() {
    super.doInit();
    String fetchAllString =
        (String) this.getRequest().getResourceRef().getQueryAsForm().getFirstValue("fetchAll");
    this.fetchAll = "true".equalsIgnoreCase(fetchAllString);
    String overwriteString =
        (String) this.getRequest().getResourceRef().getQueryAsForm().getFirstValue("overwrite");
    this.overwrite = "true".equalsIgnoreCase(overwriteString);
  }

  @Override
  public String getXml() {
    String xmlString;
    if (uriSource == null) {
      // URI had no source parameter, which means the request is for the list of all sources
      try {
        if (isAnonymous()) {
          // anonymous users get only the public sources
          xmlString = getPublicSources(fetchAll);
        }
        else if (isAdminUser()) {
          // admin user can see all sources
          xmlString = getAllSources(fetchAll);
        }
        else {
          // Authenticated as some user
          xmlString = getOwnerSources(fetchAll);
        }
      }
      catch (JAXBException e) {
        setStatusInternalError(e);
        return null;
      }
      return xmlString;
    }
    else {
      // If we make it here, we're all clear to send the XML: either source is public or source is
      // private but user is authorized to GET (checked in WattDepotResource.onInit).
      try {
        return getSource();
      }
      catch (JAXBException e) {
        setStatusInternalError(e);
        return null;
      }
    }
  }

  /**
   * Implement the PUT method that creates a Source resource.
   * 
   * @param entity The entity to be put.
   */
  @Override
  public void store(String entity) {
    // Cannot be anonymous to put a source
    if (isAnonymous()) {
      setStatusBadCredentials();
      return;
    }
    // Get the payload.
    String entityString = entity;
    Source source;
    String sourceName;
    // Try to make the XML payload into Source, return failure if this fails.
    if ((entityString == null) || ("".equals(entityString))) {
      setStatusMiscError("Entity body was empty");
      return;
    }
    try {
      source = makeSource(entityString);
      sourceName = source.getName();
    }
    catch (JAXBException e) {
      setStatusMiscError("Invalid Source representation: " + entityString);
      return;
    }
    // Return failure if the name of Source doesn't match the name given in URI
    if (!uriSource.equals(sourceName)) {
      setStatusMiscError("The source given in the URI (" + uriSource
          + ") does not match the source given in the payload (" + sourceName + ")");
      return;
    }
    if (overwrite) {
      Source existingSource = dbManager.getSource(sourceName);
      // If source already exists, must be owner to overwrite
      if ((existingSource != null) && (!validateSourceOwnerOrAdmin(existingSource))) {
        return;
      }
      if (dbManager.storeSource(source, overwrite)) {
        getResponse().setStatus(Status.SUCCESS_CREATED);
      }
      else {
        // all inputs have been validated by this point, so must be internal error
        setStatusInternalError(String.format("Unable to create Source named %s", uriSource));
        return;
      }
    }
    else {
      if (super.dbManager.getSource(uriSource) == null) {
        if (dbManager.storeSource(source)) {
          getResponse().setStatus(Status.SUCCESS_CREATED);
        }
        else {
          // all inputs have been validated by this point, so must be internal error
          setStatusInternalError(String.format("Unable to create Source named %s", uriSource));
          return;
        }
      }
      else {
        // if Source with given name already exists and not overwriting, then fail
        setStatusResourceOverwrite(uriSource);
        return;
      }
    }
  }

  /**
   * Implement the DELETE method that deletes an existing Source. Only the SourceOwner (or an admin)
   * can delete a Source resource.
   * 
   */
  @Override
  public void remove() {
    Source source = validateKnownSource();
    // First check if source in URI exists
    if (source == null) {
      return;
    }
    if (validateSourceOwnerOrAdmin(source)) {
      if (super.dbManager.deleteSource(uriSource)) {
        getResponse().setStatus(Status.SUCCESS_OK);
      }
      else {
        setStatusInternalError(String.format("Unable to delete Source %s", this.uriSource));
        return;
      }
    }
  }
}

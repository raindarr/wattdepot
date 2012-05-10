package org.wattdepot.resource.source.summary;

import javax.xml.bind.JAXBException;
import org.wattdepot.resource.ResourceInterface;
import org.wattdepot.resource.WattDepotResource;

/**
 * Represents a summary of a source.
 * 
 * @author Robert Brewer
 */

public class SourceSummaryResource extends WattDepotResource implements ResourceInterface {

  @Override
  public String getXml() {
    String xmlString;

    // If we make it here, we're all clear to send the XML: either source is public or source is
    // private but user is authorized to GET.
    try {
      xmlString = getSourceSummary();
    }
    catch (JAXBException e) {
      setStatusInternalError(e);
      return null;
    }
    return xmlString;
  }

  @Override
  public void store(String entity) {
    setStatusMethodNotAllowed();
  }

  @Override
  public void remove() {
    setStatusMethodNotAllowed();
  }
}

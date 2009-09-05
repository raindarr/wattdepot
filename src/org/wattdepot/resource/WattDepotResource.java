package org.wattdepot.resource;

import java.io.StringWriter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import org.hackystat.utilities.stacktrace.StackTrace;
import org.restlet.Context;
import org.restlet.data.CharacterSet;
import org.restlet.data.Language;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;
import org.restlet.resource.StringRepresentation;
import org.wattdepot.server.Server;
import org.wattdepot.server.db.DbManager;

/**
 * An abstract superclass for WattDepot resources, providing initialization and utility functions.
 * Portions of this code are adapted from http://hackystat-sensorbase-uh.googlecode.com/
 * 
 * @author Robert Brewer
 * @author Philip Johnson
 */
public class WattDepotResource extends Resource {

  /** Holds the class-wide JAXBContext, which is thread-safe. */
  private JAXBContext userJaxbContext;

  /** The server. */
  protected Server server;

  /** The DbManager associated with this server. */
  protected DbManager dbManager;

  /** Everyone generally wants to create one of these, so declare it here. */
  protected String responseMsg;

  /**
   * Provides the following representational variants: TEXT_XML.
   * 
   * @param context The context.
   * @param request The request object.
   * @param response The response object.
   */
  public WattDepotResource(Context context, Request request, Response response) {
    super(context, request, response);

    this.server = (Server) getContext().getAttributes().get("WattDepotServer");
    this.dbManager = (DbManager) getContext().getAttributes().get("DbManager");

    try {
      this.userJaxbContext = JAXBContext
          .newInstance(org.wattdepot.resource.user.jaxb.ObjectFactory.class);
      // loadDefaultUsers(); //NOPMD it's throwing a false warning.
      // initializeCache(); //NOPMD
      // initializeAdminUser(); //NOPMD
    }
    catch (Exception e) {
      String msg = "Exception during JAXB User initialization processing";
      this.server.getLogger().warning(msg + "\n" + StackTrace.toString(e));
      throw new RuntimeException(msg, e);
    }
  }

  /**
   * Creates and returns a new Restlet StringRepresentation built from xmlData. The xmlData will be
   * prefixed with a processing instruction indicating UTF-8 and version 1.0.
   * 
   * @param xmlData The XML data as a string.
   * @return A StringRepresentation of that xmlData.
   */
  public static StringRepresentation getStringRepresentation(String xmlData) {
    // StringBuilder builder = new StringBuilder(500);
    // builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    // builder.append(xmlData);
    // return new StringRepresentation(builder, MediaType.TEXT_XML, Language.ALL,
    // CharacterSet.UTF_8);
    return new StringRepresentation(xmlData, MediaType.TEXT_XML, Language.ALL, CharacterSet.UTF_8);
  }

  /**
   * Helper function that removes any newline characters from the supplied string and replaces them
   * with a blank line.
   * 
   * @param msg The msg whose newlines are to be removed.
   * @return The string without newlines.
   */
  private String removeNewLines(String msg) {
    return msg.replace(System.getProperty("line.separator"), " ");
  }

  /**
   * Returns the XML string containing the UserIndex with all defined Users.
   * 
   * @return The XML string providing an index to all current Users.
   * @throws Exception If there are problems mashalling the UserIndex
   */
  public String getUserIndex() throws Exception {
    Marshaller marshaller = this.userJaxbContext.createMarshaller();
    StringWriter writer = new StringWriter();

    marshaller.marshal(this.dbManager.getUsers(), writer);
    return writer.toString();
    // DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    // dbf.setNamespaceAware(true);
    // DocumentBuilder documentBuilder = dbf.newDocumentBuilder();
    // Document doc = documentBuilder.newDocument();
    // marshaller.marshal(this.dbManager.getUsers(), doc);
    // DOMSource domSource = new DOMSource(doc);
    // StringWriter writer = new StringWriter();
    // StreamResult result = new StreamResult(writer);
    // TransformerFactory tf = TransformerFactory.newInstance();
    // Transformer transformer = tf.newTransformer();
    // transformer.transform(domSource, result);
    // String xmlString = writer.toString();
    // // Now remove the processing instruction. This approach seems like a total hack.
    // xmlString = xmlString.substring(xmlString.indexOf('>') + 1);
    // return xmlString;
  }

  /**
   * Called when an exception is caught while processing a request. Just sets the response code.
   * 
   * @param e The exception that was caught.
   */
  protected void setStatusInternalError(Exception e) {
    this.responseMsg = ResponseMessage.internalError(this, this.getLogger(), e);
    getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, removeNewLines(this.responseMsg));
  }
}

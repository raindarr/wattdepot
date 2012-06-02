package org.wattdepot.client.property;

import static org.wattdepot.datainput.DataInputClientProperties.WATTDEPOT_PASSWORD_KEY;
import static org.wattdepot.datainput.DataInputClientProperties.WATTDEPOT_USERNAME_KEY;
import java.io.IOException;
import java.util.List;
import javax.xml.bind.JAXBException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.wattdepot.client.BadXmlException;
import org.wattdepot.client.MiscClientException;
import org.wattdepot.client.NotAuthorizedException;
import org.wattdepot.client.OverwriteAttemptedException;
import org.wattdepot.client.WattDepotClient;
import org.wattdepot.datainput.DataInputClientProperties;
import org.wattdepot.resource.property.jaxb.Properties;
import org.wattdepot.resource.property.jaxb.Property;
import org.wattdepot.resource.source.jaxb.Source;
import org.wattdepot.util.logger.WattDepotUserHome;

/**
 * Adds the arbitrary properties to Sources in batch mode.
 * 
 * @author Robert Brewer
 */
public class PropertyAdder {
  /** The URI of the server to be updated. */
  private String uri;

  /** Name of the property file containing essential preferences. */
  protected DataInputClientProperties properties;

  /** Name of this tool. */
  private static final String toolName = "PropertyAdder";

  /** Making PMD happy. */
  private static final String REQUIRED_PARAMETER_ERROR_MSG =
      "Required parameter %s not found in properties.%n";

  /** If true, just print out what would happen without making changes to Sources. */
  private boolean dryRun;

  /** The Property to be added to Sources. */
  private Property newProperty;

  /** If true, skip virtual Sources when adding properties. */
  private boolean skipVirtual;

  /** If true, overwrite any existing Property with given key by new value. */
  private boolean overwrite;

  /**
   * Creates the new PropertyAdder client.
   * 
   * @param propertyFilename Name of the file to read essential properties from. Defaults to
   * ~/.wattdepot/client/datainput.properties
   * @param uri The URI of the WattDepot server.
   * @param dryRun If true, just print out what would happen without making changes to Sources.
   * @param newProperty The new Property to be added to Sources.
   * @param skipVirtual If true, skip virtual Sources when adding properties.
   * @param overwrite If true, overwrite any existing Property with given key by new value.
   * @throws IOException If the property file cannot be found.
   */
  public PropertyAdder(String propertyFilename, String uri, boolean dryRun, Property newProperty,
      boolean skipVirtual, boolean overwrite) throws IOException {
    if (propertyFilename == null) {
      // Use default property file name
      this.properties =
          new DataInputClientProperties(WattDepotUserHome.getHomeString()
              + "/.wattdepot/client/datainput.properties");
    }
    else {
      this.properties = new DataInputClientProperties(propertyFilename);
    }

    this.uri = uri;
    this.dryRun = dryRun;
    this.newProperty = newProperty;
    this.skipVirtual = skipVirtual;
    this.overwrite = overwrite;
  }

  /**
   * Actually adds the property as appropriate.
   * 
   * @return True if all sources were processed without incident, false if any problems were
   * encountered.
   */
  public boolean process() {
    String wattDepotUsername = this.properties.get(WATTDEPOT_USERNAME_KEY);
    if (wattDepotUsername == null) {
      System.err.format(REQUIRED_PARAMETER_ERROR_MSG, WATTDEPOT_USERNAME_KEY);
      return false;
    }
    String wattDepotPassword = this.properties.get(WATTDEPOT_PASSWORD_KEY);
    if (wattDepotPassword == null) {
      System.err.format(REQUIRED_PARAMETER_ERROR_MSG, WATTDEPOT_PASSWORD_KEY);
      return false;
    }
    WattDepotClient client = new WattDepotClient(this.uri, wattDepotUsername, wattDepotPassword);
    List<Source> sources;
    if (client.isHealthy()) {
      try {
        sources = client.getSources();
      }
      catch (NotAuthorizedException e) {
        System.err.format("Bad credentials in properties file.%n");
        return false;
      }
      catch (BadXmlException e) {
        System.err.println("Received bad XML from server, which is weird. Aborting.");
        return false;
      }
      catch (MiscClientException e) {
        System.err.println("Had problems retrieving source from server, which is weird. Aborting.");
        return false;
      }

      // Iterate through all sources
      for (Source source : sources) {
        if (source.isVirtual() && this.skipVirtual) {
          // we only care about non-virtual sources, so skip
          System.out.format("Skipping virtual source %s%n", source.getName());
          continue;
        }
        String existingPropertyValue = source.getProperty(this.newProperty.getKey());

        if (dryRun) {
          if (existingPropertyValue == null) {
            System.out.format("dryRun: would have added new Property to source %s%n",
                source.getName());
          }
          else if (overwrite) {
            System.out.format("dryRun: would have overwritten new Property to source %s%n",
                source.getName());
          }
          else {
            System.out.format("dryRun: Source %s already has new Property set to \"%s\" and "
                + "overwrite not selected, skipping%n", source.getName(), existingPropertyValue);
          }
        }
        else {
          if (existingPropertyValue == null) {
            // new Property isn't already present, so let's add it
            source.addProperty(newProperty);
          }
          else if (overwrite) {
            // new Property is already present, but we are going to overwrite it
            Properties properties = source.getProperties();
            List<Property> propList = properties.getProperty();

            for (int i = 0; i < propList.size(); i++) {
              if (propList.get(i).getKey().equals(newProperty.getKey())) {
                propList.remove(i);
                break;
              }
            }
            source.addProperty(newProperty);
          }
          else {
            // Source already has new Property, and we are not overwriting so skip it
            System.out.format("Source %s already has new Property set to \"%s\", skipping%n",
                source.getName(), existingPropertyValue);
            continue;
          }

          try {
            if (!client.storeSource(source, true)) {
              System.err.format(
                  "Had problems updating Source %s on server, which is weird. Aborting.",
                  source.getName());
              return false;
            }
            if (existingPropertyValue == null) {
              System.out.format("Added new Property to source %s%n", source.getName());
            }
            else {
              System.out.format("Source %s already has new Property set to \"%s\", overwrote "
                  + "with new value%n", source.getName(), existingPropertyValue);
            }
          }
          catch (NotAuthorizedException e) {
            System.err.format(
                "Had problems updating Source %s on server, which is weird. Aborting.",
                source.getName());
          }
          catch (BadXmlException e) {
            System.err.println("Received bad XML from server, which is weird. Aborting.");
            return false;
          }
          catch (OverwriteAttemptedException e) {
            System.err
                .println("Attempted to overwrite source without overwrite flag (should never "
                    + "happen). Aborting.");
            return false;
          }
          catch (MiscClientException e) {
            System.err.println("Had problems storing Source on server, which is weird. Aborting.");
            return false;
          }
          catch (JAXBException e) {
            System.err.println("Had some problem marshalling XML , which is weird. Aborting.");
            return false;
          }
        }
      }
      return true;
    }
    else {
      System.err.println("Unable to connect to WattDepot server. Aborting.");
      return false;
    }
  }

  /**
   * Processes command line arguments, creates the MonitorSourceClient object and starts monitoring.
   * 
   * @param args command line arguments.
   */
  public static void main(String[] args) {
    Options options = new Options();
    options.addOption("h", "help", false, "Print this message");
    options.addOption("u", "uri", true,
        "URI of WattDepot server, ex. \"http://server.wattdepot.org:8182/wattdepot/\".");
    options.addOption("p", "propertyFilename", true, "Filename of property file");
    options
        .addOption("n", "dryRun", false, "Just print what would happen without changing Sources");
    options.addOption("a", "addProperty", true,
        "Property and value to add, ex. \"fooProperty:true\"");
    options.addOption("v", "skipVirtual", false, "Do not add property to virtual Sources");
    options.addOption("o", "overwrite", false, "Overwrite any existing property with new value");

    CommandLine cmd = null;
    String uri = null, propertyFilename = null, propertyString = null;
    Property newProperty = null;
    boolean dryRun, skipVirtual, overwrite;

    CommandLineParser parser = new PosixParser();
    HelpFormatter formatter = new HelpFormatter();
    try {
      cmd = parser.parse(options, args);
    }
    catch (ParseException e) {
      System.err.println("Command line parsing failed. Reason: " + e.getMessage() + ". Exiting.");
      System.exit(1);
    }

    if (cmd.hasOption("h")) {
      formatter.printHelp(toolName, options);
      System.exit(0);
    }

    if (cmd.hasOption("u")) {
      uri = cmd.getOptionValue("u");
    }
    else {
      System.err.println("Required URI parameter not provided, exiting.");
      formatter.printHelp(toolName, options);
      System.exit(1);
    }

    if (cmd.hasOption("p")) {
      propertyFilename = cmd.getOptionValue("p");
    }

    dryRun = cmd.hasOption("n");

    if (cmd.hasOption("a")) {
      propertyString = cmd.getOptionValue("a");
      String propertyName = null, propertyValue = null;
      String[] extractedStrings = propertyString.split(":");
      if (extractedStrings.length == 2) {
        propertyName = extractedStrings[0].trim();
        propertyValue = extractedStrings[1].trim();
        if ((propertyName.length() != 0) || (propertyValue.length() != 0)) {
          newProperty = new Property(propertyName, propertyValue);
        }
        else {
          System.err.println("Supplied Property value invalid, exiting.");
          formatter.printHelp(toolName, options);
          System.exit(1);
        }
      }
      else {
        System.err.println("Supplied Property value invalid, exiting.");
        formatter.printHelp(toolName, options);
        System.exit(1);
      }
    }
    else {
      System.err.println("Required property parameter not provided, exiting.");
      formatter.printHelp(toolName, options);
      System.exit(1);
    }

    skipVirtual = cmd.hasOption("v");

    overwrite = cmd.hasOption("o");

    // Results of command line processing, should probably be commented out
    System.out.println("uri: " + uri);
    System.out.println("propertyFilename: " + propertyFilename);
    System.out.println("dryRun: " + dryRun);
    System.out.println("newProperty: " + newProperty);
    System.out.println("skipVirtual: " + skipVirtual);
    System.out.println("overwrite: " + overwrite);
    System.out.println();

    // Actually create the input client
    PropertyAdder propertyAdder = null;
    try {
      propertyAdder =
          new PropertyAdder(propertyFilename, uri, dryRun, newProperty, skipVirtual, overwrite);
    }
    catch (IOException e) {
      System.err.println("Unable to read properties file, terminating.");
      System.exit(2);
    }
    // Just do it
    if ((propertyAdder != null) && (propertyAdder.process())) {
      System.out.println("Process ran without errors.");
      System.exit(0);
    }
    else {
      System.err.println("Error encountered while trying to add properties.");
      System.exit(2);
    }
  }
}
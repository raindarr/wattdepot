package org.wattdepot.server;

import static org.wattdepot.server.ServerProperties.CONTEXT_ROOT_KEY;
import static org.wattdepot.server.ServerProperties.GVIZ_CONTEXT_ROOT_KEY;
import static org.wattdepot.server.ServerProperties.GVIZ_PORT_KEY;
import static org.wattdepot.server.ServerProperties.LOGGING_LEVEL_KEY;
import static org.wattdepot.server.ServerProperties.PORT_KEY;
import static org.wattdepot.server.ServerProperties.TEST_INSTALL_KEY;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Guard;
import org.restlet.Restlet;
import org.restlet.Router;
import org.restlet.data.Protocol;
import org.wattdepot.resource.carbon.CarbonResource;
import org.wattdepot.resource.energy.EnergyResource;
import org.wattdepot.resource.gviz.GVisualizationServlet;
import org.wattdepot.resource.health.HealthResource;
import org.wattdepot.resource.power.PowerResource;
import org.wattdepot.resource.sensordata.SensorDataResource;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.resource.sensordata.jaxb.SensorDatas;
import org.wattdepot.resource.source.SourceResource;
import org.wattdepot.resource.source.jaxb.Source;
import org.wattdepot.resource.source.summary.SourceSummaryResource;
import org.wattdepot.resource.user.UserResource;
import org.wattdepot.resource.user.UsersResource;
import org.wattdepot.resource.user.jaxb.User;
import org.wattdepot.server.db.DbManager;
import org.wattdepot.util.logger.RestletLoggerUtil;
import org.wattdepot.util.logger.WattDepotLogger;

/**
 * Sets up HTTP server and routes requests to appropriate resources. Portions of this code are
 * adapted from http://hackystat-sensorbase-uh.googlecode.com/
 * 
 * @author Robert Brewer
 * @author Philip Johnson
 */
public class Server extends Application {

  /** Holds the Restlet Component associated with this Server. */
  private Component component;

  /** Holds the hostname associated with this Server. */
  private String hostName;

  /** Holds the hostname associated with the Google Visualization API service. */
  private String gvizHostName;

  /** Holds the WattDepotLogger for the Server. */
  private Logger logger = null;

  /** Holds the ServerProperties instance associated with this Server. */
  private ServerProperties serverProperties;

  /** The URI used for the health resource. */
  public static final String HEALTH_URI = "health";

  /** The URI used for the sources resource. */
  public static final String SOURCES_URI = "sources";

  /** The URI used for the sensordata resource. */
  public static final String SENSORDATA_URI = "sensordata";

  /** The URI used for the users resource. */
  public static final String USERS_URI = "users";

  /** The URI used for the users resource. */
  public static final String GVIZ_URI = "gviz";

  /** URI fragment for source summary. */
  public static final String SUMMARY_URI = "summary";

  /** URI fragment for source summary. */
  public static final String POWER_URI = "power";

  /** URI fragment for source summary. */
  public static final String ENERGY_URI = "energy";

  /** URI fragment for source summary. */
  public static final String CARBON_URI = "carbon";

  /** URI parameter for source name. */
  private static final String SOURCE_PARAM = "{source}";

  /** Users JAXBContext. */
  private static final JAXBContext userJAXB;
  /** SensorData JAXBContext. */
  private static final JAXBContext sensorDataJAXB;
  /** Source JAXBContext. */
  private static final JAXBContext sourceJAXB;
  // JAXBContexts are thread safe, so we can share them across all instances and threads.
  // https://jaxb.dev.java.net/guide/Performance_and_thread_safety.html
  static {
    try {
      userJAXB = JAXBContext.newInstance(org.wattdepot.resource.user.jaxb.ObjectFactory.class);
      sensorDataJAXB =
          JAXBContext.newInstance(org.wattdepot.resource.sensordata.jaxb.ObjectFactory.class);
      sourceJAXB = JAXBContext.newInstance(org.wattdepot.resource.source.jaxb.ObjectFactory.class);
    }
    catch (Exception e) {
      throw new RuntimeException("Couldn't create JAXB context instances.", e);
    }
  }

  /**
   * Creates a new instance of a WattDepot HTTP server, listening on the port defined either by the
   * properties file or a default. WattDepot properties are initialized from the user's
   * wattdepot-server.properties file.
   * 
   * @return The Server instance created.
   * @throws Exception If problems occur starting up this server.
   */
  public static Server newInstance() throws Exception {
    return newInstance(new ServerProperties());
  }

  /**
   * Creates a new instance of a WattDepot HTTP server suitable for unit testing. WattDepot
   * properties are initialized from the User's wattdepot-server.properties file, then set to their
   * "testing" versions.
   * 
   * @return The Server instance created.
   * @throws Exception If problems occur starting up this server.
   */
  public static Server newTestInstance() throws Exception {
    ServerProperties properties = new ServerProperties();
    properties.setTestProperties();
    return newInstance(properties);
  }

  /**
   * Creates a new instance of a WattDepot HTTP server, listening on the port defined either by the
   * properties file or a default.
   * 
   * @param serverProperties The ServerProperties used to initialize this server.
   * @return The Server instance created.
   * @throws Exception If problems occur starting up this server.
   */
  public static Server newInstance(ServerProperties serverProperties) throws Exception {
    Server server = new Server();
    server.logger = WattDepotLogger.getLogger("org.wattdepot.server", "server");
    server.serverProperties = serverProperties;
    server.hostName = server.serverProperties.getFullHost();
    int port = Integer.valueOf(server.serverProperties.get(PORT_KEY));
    server.component = new Component();
    server.component.getServers().add(Protocol.HTTP, port);
    server.component.getDefaultHost().attach("/" + server.serverProperties.get(CONTEXT_ROOT_KEY),
        server);

    // Set up logging.
    RestletLoggerUtil.useFileHandler("server");
    WattDepotLogger.setLoggingLevel(server.logger, server.serverProperties.get(LOGGING_LEVEL_KEY));
    server.logger.warning("Starting WattDepot server.");
    server.logger.warning("Host: " + server.hostName);
    server.logger.info(server.serverProperties.echoProperties());

    Map<String, Object> attributes = server.getContext().getAttributes();
    // Put server and serverProperties in first, because dbManager() will look at serverProperties
    attributes.put("WattDepotServer", server);
    attributes.put("ServerProperties", server.serverProperties);
    DbManager dbManager = new DbManager(server);
    if (server.serverProperties.get(TEST_INSTALL_KEY).equalsIgnoreCase("true")) {
      // In test mode, use programmatically created test data
      // Create default data to support short-term demo
      if (!dbManager.createDefaultData()) {
        server.logger.severe("Unable to create default data");
      }
    }
    else {
      try {
        server.loadDefaultResources(dbManager);
      }
      catch (Exception e) {
        server.logger.severe("Unable to load default resources: " + e.toString());
      }
    }
    attributes.put("DbManager", dbManager);

    // Set up the Google Visualization API servlet
    server.gvizHostName = server.serverProperties.getGvizFullHost();
    int gvizPort = Integer.valueOf(server.serverProperties.get(GVIZ_PORT_KEY));
    org.mortbay.jetty.Server jettyServer = new org.mortbay.jetty.Server(gvizPort);
    server.logger.warning("Google visualization URL: " + server.gvizHostName);
    Context jettyContext =
        new Context(jettyServer, "/" + server.serverProperties.get(GVIZ_CONTEXT_ROOT_KEY));

    ServletHolder servletHolder = new ServletHolder(new GVisualizationServlet(server));
    servletHolder.setInitParameter("applicationClassName",
        "org.wattdepot.resource.gviz.GVisualizationServlet");
    servletHolder.setInitOrder(1);
    jettyContext.addServlet(servletHolder, "/sources/*");

    // Now let's open for business.
    server.logger.info("Maximum Java heap size (MB): "
        + (Runtime.getRuntime().maxMemory() / 1000000.0));
    server.component.start();
    jettyServer.start();
    server.logger.warning("WattDepot server (Version " + getVersion() + ") now running.");
    return server;
  }

  /**
   * Loads the default resources from canonical directory into database. Intended to be called after
   * the database has been created, but before the server has started accepting network connections.
   * 
   * @param dbManager The database the default resources will be loaded into.
   * @throws JAXBException If there are problems unmarshalling XML from the files
   */
  protected void loadDefaultResources(DbManager dbManager) throws JAXBException {
    String userHome = System.getProperty("user.home");
    String wattDepotHome = userHome + "/.wattdepot";
    String serverHome = wattDepotHome + "/server";
    String defaultDir = serverHome + "/default-resources";
    File defaultDirFile = new File(defaultDir);
    Unmarshaller unmarshaller;

    if (defaultDirFile.isDirectory()) {
      File usersDir = new File(defaultDirFile, "users");
      if (usersDir.isDirectory()) {
        unmarshaller = userJAXB.createUnmarshaller();
        User user;
        for (File userFile : usersDir.listFiles()) {
          user = (User) unmarshaller.unmarshal(userFile);
          if (dbManager.storeUser(user)) {
            logger.info("Loaded user " + user.getEmail() + " from defaults.");
          }
          else {
            logger.warning("Default resource from file \"" + userFile.toString()
                + "\" could not be stored in DB.");
          }
        }
      }
      File sourcesDir = new File(defaultDirFile, "sources");
      if (sourcesDir.isDirectory()) {
        unmarshaller = sourceJAXB.createUnmarshaller();
        Source source;
        for (File sourceFile : sourcesDir.listFiles()) {
          source = (Source) unmarshaller.unmarshal(sourceFile);
          // Source read from the file might have an Owner field that points to a different
          // host URI. We want all defaults normalized to this server, so update it.
          source.setOwner(User.updateUri(source.getOwner(), this));
          // Source read from the file might have an Href elements under SubSources that points to
          // a different host URI. We want all defaults normalized to this server, so update it.
          if (source.isSetSubSources()) {
            List<String> hrefs = source.getSubSources().getHref();
            for (int i = 0; i < hrefs.size(); i++) {
              hrefs.set(i, Source.updateUri(hrefs.get(i), this));
            }
          }
          if (dbManager.storeSource(source)) {
            logger.info("Loaded source " + source.getName() + " from defaults.");
          }
          else {
            logger.warning("Default resource from file \"" + sourceFile.toString()
                + "\" could not be stored in DB.");
          }
        }
      }
      File sensorDataDir = new File(defaultDirFile, "sensordata");
      if (sensorDataDir.isDirectory()) {
        unmarshaller = sensorDataJAXB.createUnmarshaller();
        Object xmlObj;
        SensorData data;
        SensorDatas datas;
        int dataCount = 0;
        logger.info("Loading sensor data from defaults.");
        for (File sensorDataFile : sensorDataDir.listFiles()) {
          xmlObj = unmarshaller.unmarshal(sensorDataFile);
          if (xmlObj instanceof SensorData) {
            data = (SensorData) xmlObj;
            // SensorData read from the file might have an Owner field that points to a different
            // host URI. We want all defaults normalized to this server, so update it.
            data.setSource(Source.updateUri(data.getSource(), this));
            if (dbManager.storeSensorData(data)) {
              // Too voluminous to print every sensor data loaded
              // logger.info("Loaded sensor data for source " + data.getSource() + ", time "
              // + data.getTimestamp() + " from defaults.");
              dataCount++;
            }
            else {
              logger.warning("Default resource from file \"" + sensorDataFile.toString()
                  + "\" could not be stored in DB.");
            }
          }
          else if (xmlObj instanceof SensorDatas) {
            datas = (SensorDatas) xmlObj;
            for (SensorData theData : datas.getSensorData()) {
              // SensorData read from the file might have an Owner field that points to a different
              // host URI. We want all defaults normalized to this server, so update it.
              theData.setSource(Source.updateUri(theData.getSource(), this));
              if (dbManager.storeSensorData(theData)) {
                // Too voluminous to print every sensor data loaded
                // logger.info("Loaded sensor data for source " + data.getSource() + ", time "
                // + data.getTimestamp() + " from defaults.");
                dataCount++;
              }
              else {
                logger.warning("A default resource from file \"" + sensorDataFile.toString()
                    + "\" could not be stored in database.");
              }
            }
          }
          else {
            logger
                .warning("Found unknown XML type in sensordata file " + sensorDataFile.toString());
          }
        }
        logger.info("Loaded " + dataCount + " sensor data objects from defaults.");
      }
    }
    else {
      logger.warning("Default resource directory " + defaultDir
          + " not found, no default resources loaded.");
    }
  }

  /**
   * Starts up the WattDepot web service using the properties specified in
   * wattdepot-server.properties. On Unix-like systems, use Control-C to exit.
   * 
   * @param args String array of command line arguments that are ignored
   * @throws Exception if things go horribly awry during startup
   */
  public static void main(String[] args) throws Exception {
    Server.newInstance();
  }

  /**
   * Creates a root Restlet that will receive all incoming calls.
   * 
   * @return the newly created Restlet object
   */
  @Override
  public synchronized Restlet createRoot() {
    // This Router is used to control access to the User resource
    Router userRouter = new Router(getContext());
    userRouter.attach("/" + USERS_URI, UsersResource.class);
    userRouter.attach("/" + USERS_URI + "/{user}", UserResource.class);
    Guard userGuard = new AdminAuthenticator(getContext());
    userGuard.setNext(userRouter);

    Router router = new Router(getContext());

    // Health resource is public, so no Guard
    router.attach("/" + HEALTH_URI, HealthResource.class);
    // Source does its own authentication processing, so don't use Guard
    router.attach("/" + SOURCES_URI, SourceResource.class);
    router.attach("/" + SOURCES_URI + "/" + SOURCE_PARAM, SourceResource.class);
    router.attach("/" + SOURCES_URI + "/" + SOURCE_PARAM + "/" + SUMMARY_URI,
        SourceSummaryResource.class);
    // SensorData does its own authentication processing, so don't use Guard
    router.attach("/" + SOURCES_URI + "/" + SOURCE_PARAM + "/" + SENSORDATA_URI,
        SensorDataResource.class);
    router.attach("/" + SOURCES_URI + "/" + SOURCE_PARAM + "/" + SENSORDATA_URI
        + "/?startTime={startTime}&endTime={endTime}", SensorDataResource.class);
    router.attach("/" + SOURCES_URI + "/" + SOURCE_PARAM + "/" + SENSORDATA_URI + "/{timestamp}",
        SensorDataResource.class);
    // Power does its own authentication processing, so don't use Guard
    router.attach("/" + SOURCES_URI + "/" + SOURCE_PARAM + "/" + POWER_URI + "/{timestamp}",
        PowerResource.class);
    // Energy does its own authentication processing, so don't use Guard
    router.attach("/" + SOURCES_URI + "/" + SOURCE_PARAM + "/" + ENERGY_URI
        + "/?startTime={startTime}&endTime={endTime}&samplingInterval={interval}",
        EnergyResource.class);
    router.attach("/" + SOURCES_URI + "/" + SOURCE_PARAM + "/" + ENERGY_URI
        + "/?startTime={startTime}&endTime={endTime}", EnergyResource.class);
    // Carbon does its own authentication processing, so don't use Guard
    router.attach("/" + SOURCES_URI + "/" + SOURCE_PARAM + "/" + CARBON_URI
        + "/?startTime={startTime}&endTime={endTime}&samplingInterval={interval}",
        CarbonResource.class);
    router.attach("/" + SOURCES_URI + "/" + SOURCE_PARAM + "/" + CARBON_URI
        + "/?startTime={startTime}&endTime={endTime}", CarbonResource.class);

    // // Google Visualization API resource
    // Route route = router.attach("/" + SOURCES_URI + "/{source}" + "/" + GVIZ_URI,
    // GVisualizationResource.class);
    // route.getTemplate().setMatchingMode(Template.MODE_EQUALS);
    // router.attach("/" + SOURCES_URI + "/{source}" + "/" + GVIZ_URI + "?{parameters}",
    // GVisualizationResource.class);
    router.attachDefault(userGuard);

    return router;
  }

  /**
   * Returns the version associated with this Package, if available from the jar file manifest. If
   * not being run from a jar file, then returns "Development".
   * 
   * @return The version.
   */
  public static String getVersion() {
    String version = Package.getPackage("org.wattdepot.server").getImplementationVersion();
    return (version == null) ? "Development" : version;
  }

  /**
   * Returns the hostname associated with this server. Example: "http://localhost:8182/wattdepot/"
   * 
   * @return The hostname.
   */
  public String getHostName() {
    return this.hostName;
  }

  /**
   * Returns the ServerProperties instance associated with this server.
   * 
   * @return The server properties.
   */
  public ServerProperties getServerProperties() {
    return this.serverProperties;
  }

  /**
   * Shuts down the WattDepot server, in the hope that it will stop listening for connections. This
   * might not actually work, currently untested.
   * 
   * @throws Exception if something goes wrong during the shutdown.
   */
  public void shutdown() throws Exception {
    this.component.stop();
  }

  /**
   * Returns the logger for the WattDepot server.
   * 
   * @return The logger.
   */
  @Override
  public Logger getLogger() {
    return this.logger;
  }

}
package org.wattdepot.server;

import static org.wattdepot.server.ServerProperties.CONTEXT_ROOT_KEY;
import static org.wattdepot.server.ServerProperties.LOGGING_LEVEL_KEY;
import static org.wattdepot.server.ServerProperties.PORT_KEY;
import java.util.Map;
import java.util.logging.Logger;
import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Guard;
import org.restlet.Restlet;
import org.restlet.Router;
import org.restlet.data.Protocol;
import org.wattdepot.resource.health.HealthResource;
import org.wattdepot.resource.user.UserResource;
import org.wattdepot.resource.user.UsersResource;
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
     DbManager dbManager = new DbManager(server); // we need this later in this method.
     attributes.put("DbManager", dbManager);
    // attributes.put("SdtManager", new SdtManager(server));
    // attributes.put("UserManager", new UserManager(server));
    // attributes.put("ProjectManager", new ProjectManager(server));
    // attributes.put("SensorDataManager", new SensorDataManager(server));
    attributes.put("WattDepotServer", server);
    attributes.put("ServerProperties", server.serverProperties);

    // Now let's open for business.
    server.logger.info("Maximum Java heap size (MB): "
        + (Runtime.getRuntime().maxMemory() / 1000000.0));
    server.component.start();
    server.logger.warning("WattDepot server (Version " + getVersion() + ") now running.");
    return server;
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

    // Defines only one route
    router.attach("/" + HEALTH_URI, HealthResource.class);
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
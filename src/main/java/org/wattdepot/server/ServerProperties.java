package org.wattdepot.server;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * Provides access to the values stored in the wattdepot-server.properties file. Portions of this
 * code are adapted from http://hackystat-sensorbase-uh.googlecode.com/
 * 
 * @author Philip Johnson
 * @author Robert Brewer
 */
public class ServerProperties {

  /** The full path to the server's home directory. */
  public static final String SERVER_HOME_DIR = "wattdepot-server.homedir";
  /** The admin email key. */
  public static final String ADMIN_EMAIL_KEY = "wattdepot-server.admin.email";
  /** The admin password. */
  public static final String ADMIN_PASSWORD_KEY = "wattdepot-server.admin.password";
  /** The context root key. */
  public static final String CONTEXT_ROOT_KEY = "wattdepot-server.context.root";
  /** The database URL. */
  public static final String DATABASE_URL_KEY = "wattdepot-server.db.url";
  /** The database host name. */
  public static final String DB_HOSTNAME_KEY = "wattdepot-server.db.hostname";
  /** The database catalog name. */
  public static final String DB_DATABASE_NAME_KEY = "wattdepot-server.db.dbName";
  /** The database port number. */
  public static final String DB_PORT_KEY = "wattdepot-server.db.port";
  /** The database username. */
  public static final String DB_USERNAME_KEY = "wattdepot-server.db.username";
  /** The database password. */
  public static final String DB_PASSWORD_KEY = "wattdepot-server.db.password";
  /** The berkeleyDB database directory key. */
  public static final String BERKELEYDB_DIR_KEY = "wattdepot-server.db.berkeleydb.dir";
  /** The derby database directory key. */
  public static final String DERBY_DIR_KEY = "wattdepot-server.db.derby.dir";
  /** The derby database snapshot directory key. */
  public static final String DERBY_SNAPSHOT_KEY = "wattdepot-server.db.derby.snapshot";
  /** The Postgres database snapshot file key. */
  public static final String POSTGRES_SNAPSHOT_KEY = "wattdepot-server.db.postgres.snapshot";
  /** The Postgres schema (namespace prefix) key. */
  public static final String POSTGRES_SCHEMA_KEY = "wattdepot-server.db.postgres.schema";
  /** The maximum Postgres connection pool size key. */
  public static final String POSTGRES_MAX_ACTIVE_KEY = "wattdepot-server.db.postgres.maxActive";
  /** The initial Postgres connection pool size key. */
  public static final String POSTGRES_INITIAL_SIZE_KEY = "wattdepot-server.db.postgres.initialSize";
  /** The database implementation class. */
  public static final String DB_IMPL_KEY = "wattdepot-server.db.impl";
  /** The hostname key. */
  public static final String HOSTNAME_KEY = "wattdepot-server.hostname";
  /** The logging level key. */
  public static final String LOGGING_LEVEL_KEY = "wattdepot-server.logging.level";
  /** The Restlet Logging key. */
  public static final String RESTLET_LOGGING_KEY = "wattdepot-server.restlet.logging";
  /** The SMTP host key. */
  public static final String SMTP_HOST_KEY = "wattdepot-server.smtp.host";
  /** The wattdepot-server port key. */
  public static final String PORT_KEY = "wattdepot-server.port";
  /** The test installation key. */
  public static final String TEST_INSTALL_KEY = "wattdepot-server.test.install";
  /** The test domain key. */
  public static final String TEST_DOMAIN_KEY = "wattdepot-server.test.domain";
  /** The wattdepot-server port key during testing. */
  public static final String TEST_PORT_KEY = "wattdepot-server.test.port";
  /** The berkeleyDB database dir during testing. */
  public static final String TEST_BERKELEYDB_DIR_KEY = "wattdepot-server.test.db.berkeleydb.dir";
  /** The derby database dir during testing. */
  public static final String TEST_DERBY_DIR_KEY = "wattdepot-server.test.db.derby.dir";
  /** The derby database snapshot directory key during testing. */
  public static final String TEST_DERBY_SNAPSHOT_KEY = "wattdepot-server.test.db.derby.snapshot";
  /** The postgres database snapshot file key during testing. */
  public static final String TEST_POSTGRES_SNAPSHOT_KEY =
      "wattdepot-server.test.db.postgres.snapshot";
  /** The database host name during testing. */
  public static final String TEST_DB_HOSTNAME_KEY = "wattdepot-server.test.db.hostname";
  /** The database catalog name during testing. */
  public static final String TEST_DB_DATABASE_NAME_KEY = "wattdepot-server.test.db.dbName";
  /** The database port number during testing. */
  public static final String TEST_DB_PORT_KEY = "wattdepot-server.test.db.port";
  /** The test admin email key. */
  public static final String TEST_ADMIN_EMAIL_KEY = "wattdepot-server.test.admin.email";
  /** The test admin password. */
  public static final String TEST_ADMIN_PASSWORD_KEY = "wattdepot-server.test.admin.password";
  /** The test hostname. */
  public static final String TEST_HOSTNAME_KEY = "wattdepot-server.test.hostname";
  /** Heroku key. */
  public static final String USE_HEROKU_KEY = "wattdepot-server.heroku";
  /** Heroku test key. */
  public static final String TEST_HEROKU_KEY = "wattdepot-server.test.heroku";
  /** The hostname for Heroku. */
  public static final String HEROKU_HOSTNAME_KEY = "wattdepot-server.heroku.hostname";
  /** The heroku database URL. */
  public static final String HEROKU_DATABASE_URL_KEY = "wattdepot-server.heroku.db.url";
  /** The file to use for sensor configuration. */
  public static final String DATAINPUT_FILE_KEY = "wattdepot-server.datainput.file";
  /** Should sensors be started automatically? */
  public static final String DATAINPUT_START_KEY = "wattdepot-server.datainput.start";
  /**
   * The maximum number of threads to use for the WattDepot server. See this mailing list thread for
   * more info:
   * http://restlet.tigris.org/ds/viewMessage.do?dsForumId=4447&viewType=browseAll&dsMessageId
   * =2625612
   */
  public static final String MAX_THREADS = "wattdepot-server.maxthreads";

  /** Where we store the properties. */
  private Properties properties;

  private static String FALSE = "false";
  private static String TRUE = "true";

  /**
   * Creates a new ServerProperties instance using the default filename. Prints an error to the
   * console if problems occur on loading.
   */
  public ServerProperties() {
    this(null);
  }

  /**
   * Creates a new ServerProperties instance loaded from the given filename. Prints an error to the
   * console if problems occur on loading.
   * 
   * @param serverSubdir The name of the subdirectory used to store all files for this server.
   */
  public ServerProperties(String serverSubdir) {
    try {
      initializeProperties(serverSubdir);
    }
    catch (Exception e) {
      System.out.println("Error initializing server properties.");
    }
  }

  /**
   * Reads in the properties in ~/.wattdepot/server/wattdepot-server.properties if this file exists,
   * and provides default values for all properties not mentioned in this file. Will also add any
   * pre-existing System properties that start with "wattdepot-server.".
   * 
   * @param serverSubdir The name of the subdirectory used to store all files for this server.
   * @throws Exception if errors occur.
   */
  private void initializeProperties(String serverSubdir) throws Exception {
    String userHome = org.wattdepot.util.logger.WattDepotUserHome.getHomeString();
    String wattDepotHome = userHome + "/.wattdepot/";
    String serverHome;
    if (serverSubdir == null) {
      // Use default directory
      serverHome = wattDepotHome + "server";
    }
    else {
      serverHome = wattDepotHome + serverSubdir;
    }
    String propFile = serverHome + "/wattdepot-server.properties";
    String defaultAdmin = "admin@example.com";
    this.properties = new Properties();
    // Set defaults for 'standard' operation.
    properties.setProperty(SERVER_HOME_DIR, serverHome);
    properties.setProperty(ADMIN_EMAIL_KEY, defaultAdmin);
    properties.setProperty(ADMIN_PASSWORD_KEY, defaultAdmin);
    properties.setProperty(CONTEXT_ROOT_KEY, "wattdepot");
    properties.setProperty(DERBY_DIR_KEY, serverHome + "/Derby");
    properties.setProperty(DERBY_SNAPSHOT_KEY, serverHome + "/Derby-snapshot");
    properties.setProperty(BERKELEYDB_DIR_KEY, serverHome + "/BerkeleyDb");
    properties.setProperty(POSTGRES_SNAPSHOT_KEY, serverHome + "/Postgres-snapshot");
    properties.setProperty(POSTGRES_MAX_ACTIVE_KEY, "19");
    properties.setProperty(POSTGRES_INITIAL_SIZE_KEY, "10");
    properties.setProperty(DB_IMPL_KEY, "org.wattdepot.server.db.derby.DerbyStorageImplementation");
    properties.setProperty(DB_HOSTNAME_KEY, "localhost");
    properties.setProperty(DB_PORT_KEY, "5432");
    properties.setProperty(DB_DATABASE_NAME_KEY, "wattdepot");
    properties.setProperty(DATAINPUT_FILE_KEY, wattDepotHome + "client/datainput.properties");
    properties.setProperty(DATAINPUT_START_KEY, FALSE);

    properties.setProperty(HOSTNAME_KEY, "localhost");
    properties.setProperty(LOGGING_LEVEL_KEY, "INFO");
    properties.setProperty(RESTLET_LOGGING_KEY, FALSE);
    properties.setProperty(SMTP_HOST_KEY, "mail.hawaii.edu");
    properties.setProperty(PORT_KEY, "8182");
    properties.setProperty(MAX_THREADS, "255");
    properties.setProperty(TEST_DOMAIN_KEY, "example.com");
    properties.setProperty(TEST_INSTALL_KEY, FALSE);
    properties.setProperty(TEST_ADMIN_EMAIL_KEY, defaultAdmin);
    properties.setProperty(TEST_ADMIN_PASSWORD_KEY, defaultAdmin);
    properties.setProperty(TEST_DERBY_DIR_KEY, serverHome + "/testDerby");
    properties.setProperty(TEST_DERBY_SNAPSHOT_KEY, serverHome + "/testDerby-snapshot");
    properties.setProperty(TEST_BERKELEYDB_DIR_KEY, serverHome + "/testBerkeleyDb");
    properties.setProperty(TEST_POSTGRES_SNAPSHOT_KEY, serverHome + "/testPostgres-snapshot");
    properties.setProperty(TEST_DB_HOSTNAME_KEY, "localhost");
    properties.setProperty(TEST_PORT_KEY, "8183");
    properties.setProperty(TEST_HOSTNAME_KEY, "localhost");
    properties.setProperty(TEST_DB_DATABASE_NAME_KEY, "testwattdepot");
    properties.setProperty(TEST_DB_PORT_KEY, "5432");
    properties.setProperty(USE_HEROKU_KEY, FALSE);
    properties.setProperty(TEST_HEROKU_KEY, FALSE);

    // grab all of the properties in the environment
    Map<String, String> systemProps = System.getenv();
    for (Map.Entry<String, String> prop : systemProps.entrySet()) {
      if (prop.getKey().startsWith("wattdepot-server.")) {
        properties.setProperty(prop.getKey(), prop.getValue());
      }
    }

    // Use properties from file, if they exist.
    FileInputStream stream = null;
    try {
      stream = new FileInputStream(propFile);
      properties.load(stream);
      System.out.println("Loading Server properties from: " + propFile);
    }
    catch (IOException e) {
      System.out.println(propFile + " not found. Using default server properties.");
    }
    finally {
      if (stream != null) {
        stream.close();
      }
    }

    addServerSystemProperties(this.properties);

    // If this environment variable is set, get some extra config vars from Heroku.
    if (properties.get(USE_HEROKU_KEY).equals(TRUE)) {
      // Postgres is currently the only option on Heroku.
      properties.setProperty(DB_IMPL_KEY,
          "org.wattdepot.server.db.postgres.PostgresStorageImplementation");

      // These two properties are set by Heroku, so we access them specifically
      properties.setProperty(PORT_KEY, System.getenv("PORT"));
      properties.setProperty(DATABASE_URL_KEY, System.getenv("DATABASE_URL"));
    }
    // If we want to run unit tests on Heroku, update the Database URL and Hostname properties.
    else if (properties.get(TEST_HEROKU_KEY).equals(TRUE)) {
      properties.setProperty(DB_IMPL_KEY,
          "org.wattdepot.server.db.postgres.PostgresStorageImplementation");
      properties.setProperty(DATABASE_URL_KEY, properties.getProperty(HEROKU_DATABASE_URL_KEY));
      properties.setProperty(TEST_HOSTNAME_KEY, properties.getProperty(HEROKU_HOSTNAME_KEY));
    }

    trimProperties(properties);

    // Now add to System properties. Since the Mailer class expects to find this stuff on the
    // System Properties, we will add everything to it. In general, however, clients should not
    // use the System Properties to get at these values, since that precludes running several
    // SensorBases in a single JVM. And just is generally bogus.
    // Properties systemProperties = System.getProperties();
    // systemProperties.putAll(properties);
    // System.setProperties(systemProperties);
  }

  /**
   * Finds any System properties whose key begins with "wattdepot-server.", and adds those key-value
   * pairs to the passed Properties object.
   * 
   * @param properties The properties instance to be updated with the WattDepot system properties.
   */
  private void addServerSystemProperties(Properties properties) {
    Properties systemProperties = System.getProperties();
    for (Map.Entry<Object, Object> entry : systemProperties.entrySet()) {
      String sysPropName = (String) entry.getKey();
      if (sysPropName.startsWith("wattdepot-server.")) {
        String sysPropValue = (String) entry.getValue();
        properties.setProperty(sysPropName, sysPropValue);
      }
    }
  }

  /**
   * Sets the following properties to their "test" equivalents.
   * <ul>
   * <li>ADMIN_EMAIL_KEY
   * <li>ADMIN_PASSWORD_KEY
   * <li>HOSTNAME_KEY
   * <li>DERBY_DIR_KEY
   * <li>DERBY_SNAPSHOT_KEY
   * <li>POSTGRES_SNAPSHOT_KEY
   * <li>BERKELEYDB_DIR_KEY
   * <li>PORT_KEY
   * <li>DB_HOSTNAME_KEY
   * <li>DB_PORT_KEY
   * </ul>
   * Also sets TEST_INSTALL_KEY to true.
   */
  public void setTestProperties() {
    properties.setProperty(ADMIN_EMAIL_KEY, properties.getProperty(TEST_ADMIN_EMAIL_KEY));
    properties.setProperty(ADMIN_PASSWORD_KEY, properties.getProperty(TEST_ADMIN_PASSWORD_KEY));
    properties.setProperty(HOSTNAME_KEY, properties.getProperty(TEST_HOSTNAME_KEY));
    properties.setProperty(DERBY_DIR_KEY, properties.getProperty(TEST_DERBY_DIR_KEY));
    properties.setProperty(DERBY_SNAPSHOT_KEY, properties.getProperty(TEST_DERBY_SNAPSHOT_KEY));
    properties.setProperty(POSTGRES_SNAPSHOT_KEY,
        properties.getProperty(TEST_POSTGRES_SNAPSHOT_KEY));
    properties.setProperty(BERKELEYDB_DIR_KEY, properties.getProperty(TEST_BERKELEYDB_DIR_KEY));
    properties.setProperty(PORT_KEY, properties.getProperty(TEST_PORT_KEY));
    properties.setProperty(DB_HOSTNAME_KEY, properties.getProperty(TEST_DB_HOSTNAME_KEY));
    properties.setProperty(DB_DATABASE_NAME_KEY, properties.getProperty(TEST_DB_DATABASE_NAME_KEY));
    properties.setProperty(DB_PORT_KEY, properties.getProperty(TEST_DB_PORT_KEY));
    properties.setProperty(TEST_INSTALL_KEY, TRUE);
    // Change the db implementation class if DB_IMPL_KEY is in system properties.
    String dbImpl = System.getProperty(DB_IMPL_KEY);
    if (dbImpl != null) {
      properties.setProperty(DB_IMPL_KEY, dbImpl);
    }
    trimProperties(properties);
    // update the system properties object to reflect these new values.
    Properties systemProperties = System.getProperties();
    systemProperties.putAll(properties);
    System.setProperties(systemProperties);
  }

  /**
   * Returns a string containing all current properties in alphabetical order. Properties with the
   * word "password" in their key list "((hidden))" rather than their actual value for security
   * reasons. This is not super sophisticated - other properties such as database URLs might benefit
   * from being hidden too - but it should work for now.
   * 
   * @return A string with the properties.
   */
  public String echoProperties() {
    String cr = System.getProperty("line.separator");
    String eq = " = ";
    String pad = "                ";
    // Adding them to a treemap has the effect of alphabetizing them.
    TreeMap<String, String> alphaProps = new TreeMap<String, String>();
    for (Map.Entry<Object, Object> entry : this.properties.entrySet()) {
      String propName = (String) entry.getKey();
      String propValue = (String) entry.getValue();
      alphaProps.put(propName, propValue);
    }
    StringBuffer buff = new StringBuffer(25);
    buff.append("Server Properties:").append(cr);
    for (String key : alphaProps.keySet()) {
      if (key.contains("password")) {
        buff.append(pad).append(key).append(eq).append("((hidden))").append(cr);
      }
      else {
        buff.append(pad).append(key).append(eq).append(get(key)).append(cr);
      }
    }
    return buff.toString();
  }

  /**
   * Returns the value of the Server Property specified by the key.
   * 
   * @param key Should be one of the public static final strings in this class.
   * @return The value of the key, or null if not found.
   */
  public String get(String key) {
    return this.properties.getProperty(key);
  }

  /**
   * Ensures that the there is no leading or trailing whitespace in the property values. The fact
   * that we need to do this indicates a bug in Java's Properties implementation to me.
   * 
   * @param properties The properties.
   */
  private void trimProperties(Properties properties) {
    // Have to do this iteration in a Java 5 compatible manner. no stringPropertyNames().
    for (Map.Entry<Object, Object> entry : properties.entrySet()) {
      String propName = (String) entry.getKey();
      properties.setProperty(propName, properties.getProperty(propName).trim());
    }
  }

  /**
   * Returns the fully qualified host name, such as "http://localhost:9876/wattdepot/". Note, the
   * String will end with "/", so there is no need to append another if you are constructing a URI.
   * 
   * @return The fully qualified host name.
   */
  public String getFullHost() {
    // We have a special case for Heroku here, which leaves out the port number. This is needed
    // because Heroku apps listen on a private port on localhost, but remote connections into
    // the server always come on port 80. This causes problems because the port number is used
    // in at least 3 places: by the Server to decide what port number to bind to (on Heroku this
    // is the private port # given by the $PORT environment variable), the announced URL of the
    // server to the public (always 80 on Heroku, though usually left out of URI to default to
    // 80), and the URI of parent resources such as a Source in a SensorData resource (should be
    // the public port on Heroku).
    if (properties.getProperty(USE_HEROKU_KEY).equals(TRUE)
        || properties.getProperty(TEST_HEROKU_KEY).equals(TRUE)) {
      return "http://" + get(HOSTNAME_KEY) + ":80/" + get(CONTEXT_ROOT_KEY) + "/";
    }
    else {
      return "http://" + get(HOSTNAME_KEY) + ":" + get(PORT_KEY) + "/" + get(CONTEXT_ROOT_KEY)
          + "/";
    }
  }

}

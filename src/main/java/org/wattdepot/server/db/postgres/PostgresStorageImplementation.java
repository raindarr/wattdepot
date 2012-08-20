package org.wattdepot.server.db.postgres;

import static org.wattdepot.server.ServerProperties.DATABASE_URL_KEY;
import static org.wattdepot.server.ServerProperties.DB_DATABASE_NAME_KEY;
import static org.wattdepot.server.ServerProperties.DB_HOSTNAME_KEY;
import static org.wattdepot.server.ServerProperties.DB_PASSWORD_KEY;
import static org.wattdepot.server.ServerProperties.DB_PORT_KEY;
import static org.wattdepot.server.ServerProperties.DB_USERNAME_KEY;
import static org.wattdepot.server.ServerProperties.POSTGRES_SCHEMA_KEY;
import static org.wattdepot.server.ServerProperties.POSTGRES_MAX_ACTIVE_KEY;
import static org.wattdepot.server.ServerProperties.POSTGRES_INITIAL_SIZE_KEY;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.wattdepot.resource.property.jaxb.Property;
import org.wattdepot.resource.sensordata.SensorDataStraddle;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.resource.sensordata.jaxb.SensorDataIndex;
import org.wattdepot.resource.sensordata.jaxb.SensorDataRef;
import org.wattdepot.resource.sensordata.jaxb.SensorDatas;
import org.wattdepot.resource.source.jaxb.Source;
import org.wattdepot.resource.source.jaxb.SourceIndex;
import org.wattdepot.resource.source.jaxb.SourceRef;
import org.wattdepot.resource.source.jaxb.Sources;
import org.wattdepot.resource.source.jaxb.SubSources;
import org.wattdepot.resource.source.summary.jaxb.SourceSummary;
import org.wattdepot.resource.user.jaxb.User;
import org.wattdepot.resource.user.jaxb.UserIndex;
import org.wattdepot.resource.user.jaxb.UserRef;
import org.wattdepot.server.Server;
import org.wattdepot.server.ServerProperties;
import org.wattdepot.server.db.DbBadIntervalException;
import org.wattdepot.server.db.DbImplementation;
import org.wattdepot.server.db.DbManager;
import org.wattdepot.util.StackTrace;
import org.wattdepot.util.UriUtils;
import org.wattdepot.util.tstamp.Tstamp;

/**
 * Provides a implementation of DbImplementation using PostgreSQL.
 * 
 * @author Andrea Connell
 */
@SuppressWarnings("PMD.TooManyStaticImports")
public class PostgresStorageImplementation extends DbImplementation {

  /** The logger message when executing a query. */
  private static final String executeQueryMsg = "PostgreSQL: Executing query ";
  /** The logger message for connection closing errors. */
  private static final String errorClosingMsg = "PostgreSQL: Error while closing. \n";
  /** The logger message for database errors. */
  private static final String postgresError = "PostgreSQL: Error ";

  /** The JDBC driver. */
  private static final String driver = "org.postgresql.Driver";
  /** The PostgreSQL connection URL. */
  private String connectionUrl;
  /** Indicates whether this database was initialized or was pre-existing. */
  private boolean isFreshlyCreated = false;

  /**
   * The connection pool, which manages database connections so we don't have to open and close them
   * each time. This avoids any overhead of repeatedly opening and closing connections, and allows a
   * large number of clients to share a small number of database connections.
   */
  private DataSource connectionPool;

  /** The SQL state indicating that INSERT tried to add data to a table with a preexisting key. */
  private static final String DUPLICATE_KEY = "23505";
  /** The SQL state indication that INSERT tried to violate a foreign key constraint. */
  private static final String FOREIGN_KEY_VIOLATION = "23503";
  /** The SQL state indication that a connection was made to an invalid catalog name. */
  private static final String INVALID_CATALOG_NAME = "3D000";

  /**
   * Instantiates the PostgreSQL implementation. Throws a Runtime exception if the PostgeSQL jar
   * file cannot be found on the classpath.
   * 
   * @param server The server this DbImplementation is associated with.
   * @param dbManager The dbManager this DbImplementation is associated with.
   */
  public PostgresStorageImplementation(Server server, DbManager dbManager) {
    super(server, dbManager);

    ServerProperties props = server.getServerProperties();
    String propDbName = props.get(DB_DATABASE_NAME_KEY), propHostname = props.get(DB_HOSTNAME_KEY), propPort =
        props.get(DB_PORT_KEY), propUser = props.get(DB_USERNAME_KEY), propPassword =
        props.get(DB_PASSWORD_KEY), jdbcPrefix = "jdbc:postgresql://";

    // Connection URL lacking database name, which will cause Postgres to default to a database
    // named after the username
    String baseConnectionUrl =
        jdbcPrefix + propHostname + ":" + propPort + "?user=" + propUser + "&password="
            + propPassword;

    if (props.get(DATABASE_URL_KEY) == null || props.get(DATABASE_URL_KEY).length() == 0) {
      if (isIdentifierSafe(propDbName)) {
        this.connectionUrl =
            jdbcPrefix + propHostname + ":" + propPort + "/" + propDbName + "?user=" + propUser
                + "&password=" + propPassword;
      }
      else {
        String msg =
            "Postgres: Database catalog name \"" + propDbName
                + "\" is not a valid identifier [a-zA-Z_0-9]. Aborting.";
        this.logger.severe(msg);
        throw new RuntimeException(msg);
      }
    }

    // make sure URL is JDBC compliant (default from Heroku isn't)
    else if (!props.get(DATABASE_URL_KEY).contains("postgresql:")) {
      this.connectionUrl = props.get(DATABASE_URL_KEY);
      // assume connectionUrl is in the form postgres://username:password@host/dbName
      String userInfo =
          connectionUrl.substring(connectionUrl.indexOf("//") + 2, connectionUrl.indexOf("@"));
      String user = userInfo.split(":")[0];
      String password = userInfo.split(":")[1];
      String hostName =
          connectionUrl.substring(connectionUrl.indexOf("@") + 1, connectionUrl.lastIndexOf("/"));
      String dbName = connectionUrl.substring(connectionUrl.lastIndexOf("/") + 1);
      if (dbName.contains("?")) {
        dbName = dbName.substring(0, dbName.indexOf("?"));
      }

      String params = "";
      if (connectionUrl.contains("?")) {
        params = "&" + connectionUrl.substring(connectionUrl.indexOf("?") + 1);
      }
      this.connectionUrl =
          "jdbc:postgresql://" + hostName + "/" + dbName + "?user=" + user + "&password="
              + password + params;
      // baseConnectionUrl needs string without database name
      baseConnectionUrl =
          "jdbc:postgresql://" + hostName + "?user=" + user + "&password=" + password + params;
    }
    else {
      // assume we have a valid jdbc connectionUrl in the form
      // postgresql://hostName/dbName?user=user&password=password
      this.connectionUrl = "jdbc:" + props.get(DATABASE_URL_KEY);
      // baseConnectionUrl needs string without database name
      baseConnectionUrl =
          this.connectionUrl.substring(0, this.connectionUrl.lastIndexOf("/") - 1)
              + this.connectionUrl.substring(this.connectionUrl.lastIndexOf("?") + 1,
                  this.connectionUrl.length());
    }

    // Test if database catalog exists
    Connection conn = null;
    String schemaName = props.get(POSTGRES_SCHEMA_KEY);
    PoolProperties poolProps = new PoolProperties();

    try {
      // First try connection to configured database name
      conn = DriverManager.getConnection(this.connectionUrl);
    }
    catch (SQLException e) {
      String theError = e.getSQLState();
      if (INVALID_CATALOG_NAME.equals(theError)) {
        // the specified database catalog doesn't exist
        this.logger.warning("Postgres: configured database catalog " + propDbName
            + " does not exist, attempting to create.");
        // so try connecting without database name, which will default to a database equal to
        // the username.
        try {
          conn = DriverManager.getConnection(baseConnectionUrl);
          createDatabaseCatalog(conn, propDbName);
        }
        catch (SQLException exception) {
          String error = exception.getSQLState();
          String msg;
          if (INVALID_CATALOG_NAME.equals(error)) {
            // Can't connect to any database catalog, so give up.
            msg =
                "Postgres: unable to connect to default database catalog also. "
                    + "Please create database catalog \"" + propDbName
                    + "\" and try again. Aborting.";
          }
          else {
            // Must be a problem creating the database catalog
            msg =
                "Postgres: unable to create database catalog. "
                    + "Please create database catalog \"" + propDbName
                    + "\" and try again. Aborting.";
          }
          this.logger.severe(msg);
          throw new RuntimeException(msg, exception);
        }
      }
      else {
        String msg = "Postgres: failed to open connection to configured datbase catalog. Aborting.";
        this.logger.severe(msg + StackTrace.toString(e));
        throw new RuntimeException(msg, e);
      }
    }
    finally {
      try {
        if (conn != null) {
          conn.close();
        }
      }
      catch (SQLException e) {
        this.logger.warning(errorClosingMsg + StackTrace.toString(e));
      }
    }

    // Check if configured to use schemas and set that up if needed
    if ((schemaName != null) && (schemaName.length() != 0)) {
      if (isIdentifierSafe(schemaName)) {
        try {
          // Note we cannot use connection from connection pool, because we need to query the
          // database to find out what parameters to use in creating the pool.
          conn = DriverManager.getConnection(this.connectionUrl);
          configureSchema(conn, schemaName);
          // Now make every pooled connection set search path to the configured schema
          poolProps.setInitSQL("SET search_path TO " + schemaName + ";");
        }
        catch (SQLException e) {
          String msg = "Failure attempting to create schema " + schemaName + ". ";
          this.logger.warning(msg + StackTrace.toString(e));
          throw new RuntimeException(msg, e);
        }
        finally {
          try {
            if (conn != null) {
              conn.close();
            }
          }
          catch (SQLException e) {
            this.logger.warning(errorClosingMsg + StackTrace.toString(e));
          }
        }
      }
      else {
        String msg =
            "Postgres: Schema name \"" + schemaName
                + "\" is not a valid identifier [a-zA-Z_0-9]. Aborting.";
        this.logger.severe(msg);
        throw new RuntimeException(msg);
      }
    }

    // Set appropriate pool parameters
    poolProps.setUrl(this.connectionUrl);
    poolProps.setDriverClassName(driver);
    poolProps.setTestWhileIdle(false);
    // poolProps.setTestOnBorrow(true);
    // poolProps.setValidationQuery("SELECT 1");
    poolProps.setTestOnReturn(false);
    // poolProps.setValidationInterval(30000);
    // poolProps.setTimeBetweenEvictionRunsMillis(30000);
    poolProps.setMaxActive(Integer.parseInt(props.get(POSTGRES_MAX_ACTIVE_KEY)));
    poolProps.setInitialSize(Integer.parseInt(props.get(POSTGRES_INITIAL_SIZE_KEY)));
    poolProps.setMaxWait(10000);
    poolProps.setRemoveAbandoned(true);
    poolProps.setLogAbandoned(true);
    poolProps.setRemoveAbandonedTimeout(15);
    // poolProps.setMinEvictableIdleTimeMillis(30000);
    // poolProps.setMinIdle(10);
    try {
      this.connectionPool = new DataSource(poolProps);
      conn = this.connectionPool.getConnection();
    }
    catch (SQLException e) {
      String msg = "Postgres: failed to open connection. ";
      this.logger.severe(msg + StackTrace.toString(e));
      throw new RuntimeException(msg, e);
    }
    finally {
      try {
        if (conn != null) {
          conn.close();
        }
      }
      catch (SQLException e) {
        this.logger.warning("Postgres: failed to close connection. " + StackTrace.toString(e));
      }
    }

  }

  /**
   * Indicates whether a given string is safe as an SQL identifier (database, schema, or table
   * names). Currently using regex "[a-zA-Z_0-9]+", i.e. alphanumeric plus "_". This might be a
   * subset of all valid identifiers, but better safe than sorry.
   * 
   * @param id The possibly valid SQL identifier.
   * @return true if the identifier is safe, and false if it is not.
   */
  public boolean isIdentifierSafe(String id) {
    return id.matches("\\w+");
  }

  /**
   * Creates the given database catalog.
   * 
   * @param conn The connection to use to connect to the database. Closing the connection is the
   * responsibility of the caller.
   * @param databaseName The name of the database. This string should be checked by
   * isIdentifierSafe() before passing in.
   * @throws SQLException if there is a problem creating the database.
   * @see #isIdentifierSafe
   */
  private void createDatabaseCatalog(Connection conn, String databaseName) throws SQLException {
    // Using Statement rather than PreparedStatement because database catalog names are not
    // SQL Data statements. See
    // http://www.applettalk.com/limitations-of-preparedstatement-with-postgresql-vt96926.html

    Statement s = null;

    this.logger.info("Creating database catalog " + databaseName);

    String createDb = "CREATE DATABASE " + databaseName;

    try {
      s = conn.createStatement();
      s.executeUpdate(createDb);
    }
    finally {
      try {
        if (s != null) {
          s.close();
        }
      }
      catch (SQLException e) {
        this.logger.warning(errorClosingMsg + StackTrace.toString(e));
      }
    }
  }

  /**
   * Checks if the given schema exists, and if not, creates it.
   * 
   * @param conn The connection to use to connect to the database. Closing the connection is the
   * responsibility of the caller.
   * @param schemaName Name of the schema to create.
   * @throws SQLException if there is a problem creating the schema.
   * 
   */
  private void configureSchema(Connection conn, String schemaName) throws SQLException {
    PreparedStatement s = null;
    Statement execStatement = null;
    ResultSet rs = null;
    String schemaExistsP =
        "SELECT schema_name FROM information_schema.schemata WHERE schema_name =?";

    try {
      s = conn.prepareStatement(schemaExistsP);
      s.setString(1, schemaName);
      rs = s.executeQuery();
      if (!rs.next()) {
        // Schema does not exist, so we must create it
        this.logger.info("Creating schema " + schemaName);

        execStatement = conn.createStatement();
        String createSchema = "CREATE SCHEMA " + schemaName;
        execStatement.executeUpdate(createSchema);
      }
    }
    finally {
      try {
        if (rs != null) {
          rs.close();
        }
        if (s != null) {
          s.close();
        }
        if (execStatement != null) {
          execStatement.close();
        }
      }
      catch (SQLException e) {
        this.logger.warning(errorClosingMsg + StackTrace.toString(e));
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public void initialize(boolean wipe) {
    Runtime.getRuntime().addShutdownHook(new Thread() {
      /** Run the shutdown hook for shutting down Postgres connection pool. */
      @Override
      public void run() {
        try {
          connectionPool.close();
        }
        catch (Exception e) {
          System.out.println("Postgres shutdown hook results: " + e.getMessage());
        }
      }
    });
    String errorPrefix = "Error during initialization: ";

    try {
      try {
        this.isFreshlyCreated = !isPreExisting();
        String dbStatusMsg =
            (this.isFreshlyCreated) ? "Postgres: uninitialized."
                : "Postgres: previously initialized.";
        this.logger.info(dbStatusMsg);
      }
      catch (SQLException e) {
        if (wipe) {
          // An exception in isPreExisting most likely means the table schemas are incorrect.
          // If the user wants to wipe all data AND the current tables have the wrong schema, just
          // delete all tables and start from scratch.
          this.logger.warning("Recreating Postgres schema");
          dropTables();
          this.isFreshlyCreated = true;
        }
        else {
          throw e;
        }
      }
      if (this.isFreshlyCreated) {
        this.logger.warning("Postgres tables don't exist. Creating...");
        createTables();
      }
      // Only need to wipe tables if database has already been created and wiping was requested
      else if (wipe) {
        wipeTables();
      }
    }
    catch (SQLException e) {
      String msg = errorPrefix + StackTrace.toString(e);
      this.logger.warning(msg);
      throw new RuntimeException(msg, e);
    }
  }

  /**
   * Determine if the database has already been initialized with correct table definitions. Table
   * schemas are checked by seeing if a dummy insert on the table will work OK.
   * 
   * @return True if the database exists and tables are set up correctly.
   * @throws SQLException If problems occur accessing the database or the tables aren't set up
   * correctly.
   */
  private boolean isPreExisting() throws SQLException {
    Connection conn = null;
    Statement s = null;
    boolean ret = true;
    try {
      conn = connectionPool.getConnection();

      List<String> testStatements =
          Arrays.asList(testUserTableStatement, testUserPropertyTableStatement,
              testSourceTableStatement, testSourceHierarchyTableStatement,
              testSourcePropertyTableStatement, testSensorDataTableStatement,
              testSensorDataPropertyTableStatement);

      s = conn.createStatement();
      for (String test : testStatements) {
        try {
          s.execute(test);
        }
        catch (SQLException e) {
          String theError = (e).getSQLState();

          if ("42P01".equals(theError)) {
            // Table doesn't exist. Remember this and continue to check the rest of the tables.
            // We do this so that if an error occurs in another table, it still gets thrown.
            ret = false;
          }
          else if ("42703".equals(theError) || "42P10".equals(theError)) {
            // Incorrect table definition.
            throw e;
          }
          else {
            // Unknown SQLException
            throw e;
          }
        }
      }
    }
    finally {
      if (s != null) {
        s.close();
      }
      if (conn != null) {
        conn.close();
      }
    }
    // If table exists will get - WARNING 02000: No row was found
    return ret;
  }

  /**
   * Initialize the database by creating tables for each resource type.
   * 
   * @throws SQLException If table creation fails.
   */
  private void createTables() throws SQLException {
    Connection conn = null;
    Statement s = null;
    try {
      conn = connectionPool.getConnection();

      List<String> createStatements =
          Arrays.asList(createUserTableStatement, createUserPropertyTableStatement,
              createSourceTableStatement, createSourceHierarchyTableStatement,
              createSourcePropertyTableStatement, createSensorDataTableStatement,
              createSensorDataPropertyTableStatement, indexSensorDataSourceTstampDescStatement);

      s = conn.createStatement();
      for (String create : createStatements) {
        try {
          s.execute(create);
        }
        catch (SQLException e) {
          // If the error is that the table already exists, ignore it and move on.
          if (!"42P07".equals(e.getSQLState())) {
            this.logger.warning(e.getSQLState() + ": " + e.getMessage());
          }
        }
      }
    }
    finally {
      if (s != null) {
        s.close();
      }
      if (conn != null) {
        conn.close();
      }
    }
  }

  /**
   * Drop all database tables.
   * 
   * @throws SQLException If an error occurs with the database connection.
   * 
   */
  private void dropTables() throws SQLException {
    Connection conn = null;
    Statement s = null;
    try {
      conn = connectionPool.getConnection();
      List<String> dropStatements =
          Arrays.asList(dropSensorDataSourceTstampDescStatement,
              dropSensorDataPropertyTableStatement, dropSensorDataTableStatement,
              dropSourcePropertyTableStatement, dropSourceHierarchyTableStatement,
              dropSourceTableStatement, dropUserPropertyTableStatement, dropUserTableStatement);

      s = conn.createStatement();
      for (String drop : dropStatements) {
        try {
          s.execute(drop);
        }
        catch (SQLException e) {
          // If the error is that the table or index doesn't exist, ignore it and move on.
          if (!"42704".equals(e.getSQLState()) && !"42P01".equals(e.getSQLState())) {
            this.logger.warning(e.getSQLState() + ": " + e.getMessage());
          }
        }
      }
      s.close();

    }
    finally {
      if (s != null) {
        s.close();
      }
      if (conn != null) {
        conn.close();
      }
    }
  }

  /**
   * Wipe the database by deleting all records from each table.
   * 
   * @throws SQLException If table deletion fails.
   */
  private void wipeTables() throws SQLException {
    Connection conn = null;
    Statement s = null;
    try {
      conn = connectionPool.getConnection();
      s = conn.createStatement();
      s.execute("DELETE from SensorDataProperty");
      s.execute("DELETE from SensorData");
      s.execute("DELETE from SourceProperty");
      s.execute("DELETE from SourceHierarchy");
      s.execute("DELETE from Source");
      s.execute("DELETE from WattDepotUserProperty");
      s.execute("DELETE from WattDepotUser");
      s.close();
    }
    finally {
      if (s != null) {
        s.close();
      }
      if (conn != null) {
        conn.close();
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public boolean isFreshlyCreated() {
    // This value is initialized by initialize()
    return this.isFreshlyCreated;
  }

  /** The SQL string for creating the Source table. */
  private static final String createSourceTableStatement = "create table Source  " + "("
      + " Name VARCHAR(128) NOT NULL, " + " Owner VARCHAR(128) NOT NULL, "
      + " PublicP BOOLEAN NOT NULL, " + " Virtual BOOLEAN NOT NULL, "
      + " Coordinates VARCHAR(80), " + " Location VARCHAR(256), " + " Description VARCHAR(1024), "
      + " CarbonIntensity DOUBLE PRECISION, " + " FuelType VARCHAR(50), "
      + " UpdateInterval INTEGER, " + " EnergyDirection VARCHAR(50), "
      + " SupportsEnergyCounters BOOLEAN, " + " CacheWindowLength INTEGER, "
      + " CacheCheckpointInterval INTEGER, " + " LastMod TIMESTAMP NOT NULL, "
      + " PRIMARY KEY (Name), " + " FOREIGN KEY (Owner) REFERENCES WattDepotUser(Username)" + ")";

  /** An SQL string to test whether the Source table exists and has the correct schema. */
  private static final String testSourceTableStatement = " UPDATE Source SET "
      + " Name = 'db-test-source', "
      + " Owner = 'http://server.wattdepot.org/wattdepot/users/db-test-user', "
      + " PublicP = FALSE, " + " Virtual = TRUE, " + " Coordinates = '21.30078,-157.819129,41', "
      + " Location = 'Some place', " + " Description = 'A test source.', "
      + " CarbonIntensity = 2120, " + " FuelType = 'LSFO', " + " UpdateInterval = 60, "
      + " EnergyDirection = 'consumer+generator'," + " SupportsEnergyCounters = TRUE, "
      + " CacheWindowLength = 60, " + " CacheCheckpointInterval = 5, " + " LastMod = '"
      + new Timestamp(new Date().getTime()).toString() + "' " + " WHERE 1=3";

  /** An SQL string to drop the Source table. */
  private static final String dropSourceTableStatement = "DROP TABLE Source";

  /** The SQL string for creating the SourceHierarchy table. */
  private static final String createSourceHierarchyTableStatement = "create table SourceHierarchy"
      + "(" + "ParentSourceName VARCHAR(128) NOT NULL, " + " SubSourceName VARCHAR(128) NOT NULL, "
      + " PRIMARY KEY (ParentSourceName, SubSourceName), "
      + " FOREIGN KEY (ParentSourceName) REFERENCES Source(Name), "
      + " FOREIGN KEY (SubSourceName) REFERENCES Source(Name)" + ")";

  /** An SQL string to test whether the SourceHierarchy table exists and has the correct schema. */
  private static final String testSourceHierarchyTableStatement =
      " UPDATE SourceHierarchy SET "
          + " ParentSourceName = 'db-test-source', "
          + " SubSourceName = 'http://server.wattdepot.org:1234/wattdepot/sources/SIM_HONOLULU_8' WHERE 1=3";

  /** An SQL string to drop the SourceHierarchy table. */
  private static final String dropSourceHierarchyTableStatement = "DROP TABLE SourceHierarchy";

  /** The SQL string for creating SourceProperty table. */
  private static final String createSourcePropertyTableStatement = "create table SourceProperty"
      + "(" + "SourceName VARCHAR(128) NOT NULL, " + " PropertyKey VARCHAR(128) NOT NULL, "
      + " PropertyValue VARCHAR(128) NOT NULL, " + " PRIMARY KEY (SourceName, PropertyKey), "
      + " FOREIGN KEY (SourceName) REFERENCES Source(Name)" + ")";

  /** An SQL string to test whether the SourceProperty table exists and has the correct schema. */
  private static final String testSourcePropertyTableStatement = " UPDATE SourceProperty SET "
      + " SourceName= 'db-test-source', " + " PropertyKey = 'carbonIntensity', "
      + " PropertyValue = '2120'" + " WHERE 1=3";

  /** An SQL string to drop the SourceProperty table. */
  private static final String dropSourcePropertyTableStatement = "DROP TABLE SourceProperty";

  /** {@inheritDoc} */
  @Override
  public SourceIndex getSourceIndex() {
    SourceIndex index = new SourceIndex();
    String statement =
        "SELECT Name, Owner, PublicP, Virtual, Coordinates, Location, Description FROM Source ORDER BY Name";
    Connection conn = null;
    PreparedStatement s = null;
    ResultSet rs = null;
    SourceRef ref;
    try {
      conn = connectionPool.getConnection();
      server.getLogger().fine(executeQueryMsg + statement);
      s = conn.prepareStatement(statement);
      rs = s.executeQuery();
      while (rs.next()) {
        ref = new SourceRef();
        String name = rs.getString("Name");
        ref.setName(name);
        ref.setOwner(User.userToUri(rs.getString("Owner"), server));
        ref.setPublic(rs.getBoolean("PublicP"));
        ref.setVirtual(rs.getBoolean("Virtual"));
        ref.setCoordinates(rs.getString("Coordinates"));
        ref.setLocation(rs.getString("Location"));
        ref.setDescription(rs.getString("Description"));
        ref.setHref(name, this.server);
        index.getSourceRef().add(ref);
      }
    }
    catch (SQLException e) {
      this.logger.info("DB: Error in getSources()" + StackTrace.toString(e));
    }
    finally {
      try {
        if (rs != null) {
          rs.close();
        }
        if (s != null) {
          s.close();
        }
        if (conn != null) {
          conn.close();
        }
      }
      catch (SQLException e) {
        this.logger.warning(errorClosingMsg + StackTrace.toString(e));
      }
    }
    // Collections.sort(index.getSourceRef());
    return index;
  }

  /**
   * Converts a database row from source table to a Source object. The caller should have advanced
   * the cursor to the next row via rs.next() before calling this method.
   * 
   * @param rs The result set to be examined.
   * @return The new Source object.
   */
  private Source resultSetToSource(ResultSet rs) {
    Source source = new Source();

    try {
      source.setName(rs.getString("Name"));
      source.setOwner(User.userToUri(rs.getString("Owner"), server));
      source.setPublic(rs.getBoolean("PublicP"));
      source.setVirtual(rs.getBoolean("Virtual"));
      source.setCoordinates(rs.getString("Coordinates"));
      source.setLocation(rs.getString("Location"));
      source.setDescription(rs.getString("Description"));

      if (rs.getObject("CarbonIntensity") != null) {
        source.addProperty(new Property(Source.CARBON_INTENSITY, rs.getDouble("CarbonIntensity")));
      }
      if (rs.getObject("FuelType") != null) {
        source.addProperty(new Property(Source.FUEL_TYPE, rs.getString("FuelType")));
      }
      if (rs.getObject("UpdateInterval") != null) {
        source.addProperty(new Property(Source.UPDATE_INTERVAL, rs.getInt("UpdateInterval")));
      }
      if (rs.getObject("EnergyDirection") != null) {
        source.addProperty(new Property(Source.ENERGY_DIRECTION, rs.getString("EnergyDirection")));
      }
      if (rs.getObject("SupportsEnergyCounters") != null) {
        source.addProperty(new Property(Source.SUPPORTS_ENERGY_COUNTERS, String.valueOf(rs
            .getBoolean("SupportsEnergyCounters"))));
      }
      if (rs.getObject("CacheWindowLength") != null) {
        source
            .addProperty(new Property(Source.CACHE_WINDOW_LENGTH, rs.getInt("CacheWindowLength")));
      }
      if (rs.getObject("CacheCheckpointInterval") != null) {
        source.addProperty(new Property(Source.CACHE_CHECKPOINT_INTERVAL, rs
            .getInt("CacheCheckpointInterval")));
      }
    }
    catch (SQLException e) {
      this.logger.info("DB: Error in getSource()" + StackTrace.toString(e));
      return null;
    }
    return source;
  }

  /** {@inheritDoc} */
  @Override
  public Sources getSources() {
    Sources sources = new Sources();
    String statement = "SELECT * FROM Source ORDER BY Name";
    Connection conn = null;
    PreparedStatement s = null;
    ResultSet rs = null;
    try {
      conn = connectionPool.getConnection();
      server.getLogger().fine(executeQueryMsg + statement);
      s = conn.prepareStatement(statement);
      rs = s.executeQuery();
      while (rs.next()) {
        Source source = resultSetToSource(rs);
        if (source.isVirtual()) {
          source.setSubSources(getSubSources(source.getName()));
        }
        List<Property> props = getSourceProperties(source.getName());
        for (Property p : props) {
          source.addProperty(p);
        }

        sources.getSource().add(source);
      }
    }
    catch (SQLException e) {
      this.logger.info("DB: Error in getSources()" + StackTrace.toString(e));
    }
    finally {
      try {
        if (rs != null) {
          rs.close();
        }
        if (s != null) {
          s.close();
        }
        if (conn != null) {
          conn.close();
        }
      }
      catch (SQLException e) {
        this.logger.warning(errorClosingMsg + StackTrace.toString(e));
      }
    }
    return sources;
  }

  /**
   * Determines if a Source with the given name exists or not.
   * 
   * @param sourceName Name of the Source
   * @return True if the given source exists, false otherwise.
   */
  private boolean sourceExists(String sourceName) {
    if (sourceName == null) {
      return false;
    }
    else {
      String statement = "SELECT * FROM Source WHERE Name = ?";
      Connection conn = null;
      PreparedStatement s = null;
      ResultSet rs = null;
      try {
        conn = connectionPool.getConnection();
        server.getLogger().fine(executeQueryMsg + statement);
        s = conn.prepareStatement(statement);
        s.setString(1, sourceName);
        rs = s.executeQuery();
        if (rs.next()) {
          return true;
        }
      }
      catch (SQLException e) {
        this.logger.info("DB: Error in sourceExists()" + StackTrace.toString(e));
      }
      finally {
        try {
          if (rs != null) {
            rs.close();
          }
          if (s != null) {
            s.close();
          }
          if (conn != null) {
            conn.close();
          }
        }
        catch (SQLException e) {
          this.logger.warning(errorClosingMsg + StackTrace.toString(e));
        }
      }
      return false;
    }
  }

  /** {@inheritDoc} */
  @Override
  public Source getSource(String sourceName) {
    if (sourceName == null) {
      return null;
    }
    else {
      String statement = "SELECT * FROM Source WHERE Name = ?";
      Connection conn = null;
      PreparedStatement s = null;
      ResultSet rs = null;
      Source source = null;
      try {
        conn = connectionPool.getConnection();
        server.getLogger().fine(executeQueryMsg + statement);
        s = conn.prepareStatement(statement);
        s.setString(1, sourceName);
        rs = s.executeQuery();
        while (rs.next()) { // the select statement must guarantee only one row is returned.
          source = resultSetToSource(rs);
          if (source.isVirtual()) {
            source.setSubSources(getSubSources(source.getName()));
          }
          List<Property> props = getSourceProperties(source.getName());
          for (Property p : props) {
            source.addProperty(p);
          }
        }

      }
      catch (SQLException e) {
        this.logger.info("DB: Error in getSource()" + StackTrace.toString(e));
      }
      finally {
        try {
          if (rs != null) {
            rs.close();
          }
          if (s != null) {
            s.close();
          }
          if (conn != null) {
            conn.close();
          }
        }
        catch (SQLException e) {
          this.logger.warning(errorClosingMsg + StackTrace.toString(e));
        }
      }
      return source;
    }
  }

  /**
   * Get properties for a source.
   * 
   * @param sourceName The source to get properties for.
   * @return The list of properties for the source.
   */
  private List<Property> getSourceProperties(String sourceName) {
    List<Property> props = new ArrayList<Property>();

    Connection conn = null;
    PreparedStatement s = null;
    ResultSet rs = null;
    try {
      conn = connectionPool.getConnection();
      String statement = "SELECT * FROM SourceProperty WHERE SourceName = ? ORDER BY PropertyKey ";
      server.getLogger().fine(executeQueryMsg + statement);
      s = conn.prepareStatement(statement);
      s.setString(1, sourceName);
      rs = s.executeQuery();
      while (rs.next()) {
        props.add(new Property(rs.getString("PropertyKey"), rs.getString("PropertyValue")));
      }

    }
    catch (SQLException e) {
      this.logger.info("DB: Error in getSourceProperties()" + StackTrace.toString(e));
    }
    finally {
      try {
        if (rs != null) {
          rs.close();
        }
        if (s != null) {
          s.close();
        }
        if (conn != null) {
          conn.close();
        }
      }
      catch (SQLException e) {
        this.logger.warning(errorClosingMsg + StackTrace.toString(e));
      }
    }
    return props;

  }

  /**
   * Get SubSources of a source.
   * 
   * @param sourceName The source to get properties for.
   * @return The SubSources for the source.
   */
  public SubSources getSubSources(String sourceName) {
    SubSources subSources = new SubSources();

    Connection conn = null;
    PreparedStatement s = null;
    ResultSet rs = null;
    try {
      conn = connectionPool.getConnection();
      String statement =
          "SELECT * FROM SourceHierarchy WHERE ParentSourceName = ? ORDER BY SubSourceName ";
      server.getLogger().fine(executeQueryMsg + statement);
      s = conn.prepareStatement(statement);
      s.setString(1, sourceName);
      rs = s.executeQuery();
      while (rs.next()) {
        subSources.getHref().add(Source.sourceToUri(rs.getString("SubSourceName"), server));
      }

    }
    catch (SQLException e) {
      this.logger.info("DB: Error in getSubSources()" + StackTrace.toString(e));
    }
    finally {
      try {
        if (rs != null) {
          rs.close();
        }
        if (s != null) {
          s.close();
        }
        if (conn != null) {
          conn.close();
        }
      }
      catch (SQLException e) {
        this.logger.warning(errorClosingMsg + StackTrace.toString(e));
      }
    }
    return subSources;

  }

  /** {@inheritDoc} */
  @Override
  public SourceSummary getSourceSummary(String sourceName) {
    if (sourceName == null) {
      // null or non-existent source name
      return null;
    }
    Source baseSource = getSource(sourceName);
    if (baseSource == null) {
      return null;
    }
    SourceSummary summary = new SourceSummary();
    summary.setHref(Source.sourceToUri(sourceName, this.server));
    // Want to go through sensordata for base source, and all subsources recursively
    List<Source> sourceList = getAllNonVirtualSubSources(baseSource);
    if (sourceList.size() == 0) {
      summary.setTotalSensorDatas(0);
      return summary;
    }
    XMLGregorianCalendar firstTimestamp = null, lastTimestamp = null;
    Timestamp sqlDataTimestamp = null;
    long dataCount = 0;
    String statement;
    Connection conn = null;
    PreparedStatement s = null;
    ResultSet rs = null;

    try {
      conn = connectionPool.getConnection();
      // Get first timestamp, last timestamp, and count of all timestamps for this list of sources
      statement =
          String
              .format(
                  "SELECT Max(Tstamp) as maxTime, Min(Tstamp) as minTime, Count(1) as dataCount FROM SensorData WHERE Source IN (%s)",
                  preparePlaceHolders(sourceList.size()));

      server.getLogger().fine(executeQueryMsg + statement);
      s = conn.prepareStatement(statement);
      // Add the name of each source to the IN clause of the SQL statement
      for (int i = 0; i < sourceList.size(); i++) {
        s.setString(i + 1, sourceList.get(i).getName());
      }

      rs = s.executeQuery();
      if (rs.next()) {
        sqlDataTimestamp = rs.getTimestamp("minTime");
        if (sqlDataTimestamp != null) {
          firstTimestamp = Tstamp.makeTimestamp(sqlDataTimestamp);
        }
        sqlDataTimestamp = rs.getTimestamp("maxTime");
        if (sqlDataTimestamp != null) {
          lastTimestamp = Tstamp.makeTimestamp(sqlDataTimestamp);
        }
        dataCount = rs.getInt("dataCount");
      }
    }
    catch (SQLException e) {
      this.logger.info("DB: Error in getSourceSummary()" + StackTrace.toString(e));
    }
    finally {
      try {
        if (rs != null) {
          rs.close();
        }
        if (s != null) {
          s.close();
        }
        if (conn != null) {
          conn.close();
        }
      }
      catch (SQLException e) {
        this.logger.warning(errorClosingMsg + StackTrace.toString(e));
      }
    }

    summary.setFirstSensorData(firstTimestamp);
    summary.setLastSensorData(lastTimestamp);
    summary.setTotalSensorDatas(dataCount);
    return summary;
  }

  /**
   * Build a list of question marks separated by commas for the IN clause of a prepared statement.
   * We need to put this in a separate method so that FindBugs doesn't complain about generating a
   * prepared statement from a nonconstant string.
   * 
   * @param length The number of question marks that the string should include.
   * @return A string of length-number question marks separated by commas.
   */
  private String preparePlaceHolders(int length) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < length;) {
      builder.append("?");
      if (++i < length) {
        builder.append(",");
      }
    }
    return builder.toString();
  }

  /** {@inheritDoc} */
  @Override
  public boolean storeSource(Source source, boolean overwrite) {
    if (source == null) {
      return false;
    }
    else {
      Connection conn = null;
      PreparedStatement s = null;

      try {
        conn = connectionPool.getConnection();
        if (conn.getMetaData().supportsTransactionIsolationLevel(
            Connection.TRANSACTION_READ_COMMITTED)) {
          conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        }
        conn.setAutoCommit(false);
        // If source exists already, then do update rather than insert IF overwrite is true
        if (sourceExists(source.getName())) {
          if (overwrite) {
            s =
                conn.prepareStatement("UPDATE Source SET Name = ?, Owner = ?, PublicP = ?, "
                    + " Virtual = ?, Coordinates = ?, Location = ?, Description = ?, "
                    + " CarbonIntensity = ?, FuelType = ?, UpdateInterval = ?, "
                    + " EnergyDirection = ?, SupportsEnergyCounters = ?, CacheWindowLength = ?, "
                    + " CacheCheckpointInterval = ?, LastMod = ? " + " WHERE Name = ?");
            s.setString(16, source.getName());
          }
          else {
            this.logger.fine("PostgreSQL: Attempted to overwrite without overwrite=true Source "
                + source.getName());
            return false;
          }
        }
        else {
          s =
              conn.prepareStatement("INSERT INTO Source VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        }
        // Order: Name Owner PublicP Virtual Coordinates Location Description SubSources
        // CarbonIntensity FuelType UpdateInterval EnergyDirection SupportsEnergyCounters LastMod
        s.setString(1, source.getName());
        s.setString(2, UriUtils.getUriSuffix(source.getOwner()));
        s.setBoolean(3, source.isPublic());
        s.setBoolean(4, source.isVirtual());
        s.setString(5, source.getCoordinates());
        s.setString(6, source.getLocation());
        s.setString(7, source.getDescription());

        // initialize properties to null
        s.setNull(8, java.sql.Types.DOUBLE);
        s.setNull(9, java.sql.Types.VARCHAR);
        s.setNull(10, java.sql.Types.INTEGER);
        s.setNull(11, java.sql.Types.VARCHAR);
        s.setNull(12, java.sql.Types.BOOLEAN);
        s.setNull(13, java.sql.Types.INTEGER);
        s.setNull(14, java.sql.Types.INTEGER);

        if (source.isSetProperties()) {
          if (source.getProperties().getProperty(Source.CARBON_INTENSITY) != null) {
            s.setDouble(8, source.getProperties().getPropertyAsDouble(Source.CARBON_INTENSITY));
          }
          if (source.getProperties().getProperty(Source.FUEL_TYPE) != null) {
            s.setString(9, source.getProperties().getProperty(Source.FUEL_TYPE));
          }
          if (source.getProperties().getProperty(Source.UPDATE_INTERVAL) != null) {
            s.setInt(10, source.getProperties().getPropertyAsInt(Source.UPDATE_INTERVAL));
          }
          if (source.getProperties().getProperty(Source.ENERGY_DIRECTION) != null) {
            s.setString(11, source.getProperties().getProperty(Source.ENERGY_DIRECTION));
          }
          if (source.getProperties().getProperty(Source.SUPPORTS_ENERGY_COUNTERS) != null) {
            s.setBoolean(12, Boolean.valueOf(source.getProperties().getProperty(
                Source.SUPPORTS_ENERGY_COUNTERS)));
          }
          if (source.getProperties().getProperty(Source.CACHE_WINDOW_LENGTH) != null) {
            s.setInt(13, source.getProperties().getPropertyAsInt(Source.CACHE_WINDOW_LENGTH));
          }
          if (source.getProperties().getProperty(Source.CACHE_CHECKPOINT_INTERVAL) != null) {
            s.setInt(14, source.getProperties().getPropertyAsInt(Source.CACHE_CHECKPOINT_INTERVAL));
          }
        }

        s.setTimestamp(15, new Timestamp(new Date().getTime()));
        s.executeUpdate();

        if (overwrite) {
          deleteSubSources(source.getName(), conn);
          deleteSourceProperties(source.getName(), conn);
        }
        if (source.isSetSubSources()) {
          for (String subSource : source.getSubSources().getHref()) {
            s = conn.prepareStatement("INSERT INTO SourceHierarchy VALUES (?, ?) ");
            s.setString(1, source.getName());
            s.setString(2, UriUtils.getUriSuffix(subSource));
            s.executeUpdate();
          }
        }
        if (source.isSetProperties()) {
          for (Property p : source.getProperties().getProperty()) {
            if (!p.getKey().equals(Source.CARBON_INTENSITY) && !p.getKey().equals(Source.FUEL_TYPE)
                && !p.getKey().equals(Source.UPDATE_INTERVAL)
                && !p.getKey().equals(Source.ENERGY_DIRECTION)
                && !p.getKey().equals(Source.SUPPORTS_ENERGY_COUNTERS)
                && !p.getKey().equals(Source.CACHE_WINDOW_LENGTH)
                && !p.getKey().equals(Source.CACHE_CHECKPOINT_INTERVAL)) {
              s = conn.prepareStatement("INSERT INTO SourceProperty VALUES (?, ?, ?) ");
              s.setString(1, source.getName());
              s.setString(2, p.getKey());
              s.setString(3, p.getValue());
              s.executeUpdate();
            }
          }
        }
        conn.commit();

        this.logger.fine("PostgreSQL: Inserted Source" + source.getName());
        return true;
      }
      catch (SQLException e) {
        if (DUPLICATE_KEY.equals(e.getSQLState())) {
          // This should be extremely rare now that we check for existence before INSERTting
          this.logger.fine("PostgreSQL: Attempted to overwrite Source " + source.getName());
          return false;
        }
        else if (FOREIGN_KEY_VIOLATION.equals(e.getSQLState())) {
          this.logger.fine("Derby: Foreign key constraint violation on Source " + source.getName()
              + ". " + e.getMessage());
          return false;
        }
        else {
          this.logger.info(postgresError + StackTrace.toString(e));
          return false;
        }
      }
      finally {
        try {
          if (s != null) {
            s.close();
          }
          if (conn != null) {
            conn.setAutoCommit(true);
            conn.close();
          }
        }
        catch (SQLException e) {
          this.logger.warning(errorClosingMsg + StackTrace.toString(e));
        }
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public boolean deleteSource(String sourceName) {
    if (sourceName == null) {
      return false;
    }
    Connection conn = null;
    try {
      conn = connectionPool.getConnection();
      conn.setAutoCommit(false);

      deleteSensorData(sourceName, conn);
      deleteSubSources(sourceName, conn);
      deleteParentSources(sourceName, conn);
      deleteSourceProperties(sourceName, conn);
      String statement = "DELETE FROM Source WHERE Name='" + sourceName.replace("'", "''") + "'";
      return deleteResource(statement, conn);
    }
    catch (SQLException e) {
      return false;
    }
    finally {
      if (conn != null) {
        try {
          conn.setAutoCommit(true);
          conn.close();
        }
        catch (SQLException e) { // NOPMD

        }
      }
    }
  }

  /**
   * Ensures that the Source with the given name is no longer present in storage. All sensor data
   * associated with this Source will also be deleted.
   * 
   * @param sourceName The name of the Source.
   * @param conn The connection encapsulating this transaction.
   * @return True if the Source was deleted, or false if it was not deleted or the requested Source
   * does not exist.
   */
  private boolean deleteSource(String sourceName, Connection conn) {
    if (sourceName == null) {
      return false;
    }
    else {
      deleteSensorData(sourceName, conn);
      deleteSubSources(sourceName, conn);
      deleteParentSources(sourceName, conn);
      deleteSourceProperties(sourceName, conn);
      String statement = "DELETE FROM Source WHERE Name='" + sourceName.replace("'", "''") + "'";
      return deleteResource(statement, conn);
    }
  }

  /**
   * Find and delete all sources that are owned by the given user.
   * 
   * @param username The username of the source owner.
   * @param conn The connection encapsulating this transaction.
   * @return True if all sources were deleted.
   */
  private boolean deleteSourcesForOwner(String username, Connection conn) {
    if (username == null) {
      return false;
    }

    String statement = "SELECT Name FROM Source WHERE Owner = ?";
    PreparedStatement s = null;
    ResultSet rs = null;
    try {
      server.getLogger().fine(executeQueryMsg + statement);
      s = conn.prepareStatement(statement);
      s.setString(1, username);
      rs = s.executeQuery();
      while (rs.next()) {
        if (!deleteSource(rs.getString("Name"), conn)) {
          return false;
        }
      }
    }
    catch (SQLException e) {
      this.logger.info("DB: Error in deleteSourcesForOwner " + StackTrace.toString(e));
      return false;
    }
    finally {
      try {
        if (rs != null) {
          rs.close();
        }
        if (s != null) {
          s.close();
        }
      }
      catch (SQLException e) {
        this.logger.warning(errorClosingMsg + StackTrace.toString(e));
      }
    }
    return true;
  }

  /**
   * Remove the relationship between the given source and its subsources by deleting the records
   * where the given source is listed as the parent.
   * 
   * @param sourceName The source to delete subsources for.
   * @param conn The connection encapsulating this transaction.
   * @return True if the subsources were successfully deleted.
   */
  private boolean deleteSubSources(String sourceName, Connection conn) {
    return deleteResource(
        "DELETE FROM SourceHierarchy WHERE ParentSourceName = '" + sourceName.replace("'", "''")
            + "'", conn);
  }

  /**
   * Remove the relationship between the given source and its parent source(s) by deleting the
   * records where the given source is listed as the subsource.
   * 
   * @param sourceName The source to delete parent sources for.
   * @param conn The connection encapsulating this transaction.
   * @return True if the parent sources were successfully deleted.
   */
  private boolean deleteParentSources(String sourceName, Connection conn) {
    return deleteResource(
        "DELETE FROM SourceHierarchy WHERE SubSourceName='" + sourceName.replace("'", "''") + "'",
        conn);
  }

  /**
   * Delete a source's Property entries from SourceProperty table.
   * 
   * @param sourceName The source to delete properties for.
   * @param conn The connection encapsulating this transaction.
   * @return True if the properties were successfully deleted.
   */
  private boolean deleteSourceProperties(String sourceName, Connection conn) {
    return deleteResource(
        "DELETE FROM SourceProperty WHERE SourceName='" + sourceName.replace("'", "''") + "'", conn);
  }

  /** The SQL string for creating the SensorData table. */
  private static final String createSensorDataTableStatement = "create table SensorData  " + "("
      + " Tstamp TIMESTAMP NOT NULL, " + " Tool VARCHAR(128) NOT NULL, "
      + " Source VARCHAR(128) NOT NULL, " + " PowerConsumed DOUBLE PRECISION, "
      + " EnergyConsumedToDate DOUBLE PRECISION, " + " PowerGenerated DOUBLE PRECISION, "
      + " EnergyGeneratedToDate DOUBLE PRECISION, " + " LastMod TIMESTAMP NOT NULL, "
      + " PRIMARY KEY (Source, Tstamp), " + " FOREIGN KEY (Source) REFERENCES Source(Name)" + ")";

  /** An SQL string to test whether the SensorData table exists and has the correct schema. */
  private static final String testSensorDataTableStatement = " UPDATE SensorData SET "
      + " Tstamp = '" + new Timestamp(new Date().getTime()).toString() + "', "
      + " Tool = 'test-db-tool', " + " Source = 'test-db-source', " + " PowerConsumed='4.6E7', "
      + " EnergyConsumedToDate='1000', " + " PowerGenerated='100.0', "
      + " EnergyGeneratedToDate='5778.0', " + " LastMod = '"
      + new Timestamp(new Date().getTime()).toString() + "' " + " WHERE 1=3";

  /** An SQL string to drop the SensorData table. */
  private static final String dropSensorDataTableStatement = "DROP TABLE SensorData";

  /** The SQL string for creating the SensorDataProperty table. */
  private static final String createSensorDataPropertyTableStatement =
      "create table SensorDataProperty " + "(" + " Tstamp TIMESTAMP NOT NULL, "
          + " Source VARCHAR(128) NOT NULL, " + " PropertyKey VARCHAR(128) NOT NULL, "
          + " PropertyValue VARCHAR(128) NOT NULL, " + "PRIMARY KEY(Tstamp, Source, PropertyKey), "
          + " FOREIGN KEY (Source, Tstamp) REFERENCES SensorData" + ")";

  /** An SQL string to test whether the SensorDataProperty table exists and has the correct schema. */
  private static final String testSensorDataPropertyTableStatement =
      " UPDATE SensorDataProperty SET " + " Tstamp = '"
          + new Timestamp(new Date().getTime()).toString() + "', " + " Source = 'test-db-source', "
          + " PropertyKey = 'powerGenerated', " + " PropertyValue = '4.6E7' " + " WHERE 1=3";

  /** An SQL string to drop the SensorDataProperty table. */
  private static final String dropSensorDataPropertyTableStatement =
      "DROP TABLE SensorDataProperty";

  /**
   * Compound index used based on this mailing list reply.
   * http://mail-archives.apache.org/mod_mbox/db
   * -derby-user/201007.mbox/%3cx67hl3kmqf.fsf@oracle.com%3e
   */
  private static final String indexSensorDataSourceTstampDescStatement =
      "CREATE INDEX TstampSourceIndexDesc ON SensorData(Source, Tstamp DESC)";
  private static final String dropSensorDataSourceTstampDescStatement =
      "DROP INDEX TstampSourceIndexDesc";

  /**
   * Converts a database row from the SensorData table to a SensorData object. The caller should
   * have advanced the cursor to the next row via rs.next() before calling this method.
   * 
   * @param rs The result set to be examined.
   * @return The new SensorData object.
   */
  private SensorData resultSetToSensorData(ResultSet rs) {
    SensorData data = new SensorData();

    try {
      data.setTimestamp(Tstamp.makeTimestamp(rs.getTimestamp("Tstamp")));
      data.setTool(rs.getString("Tool"));
      data.setSource(Source.sourceToUri(rs.getString("Source"), server));

      if (rs.getObject("PowerConsumed") != null) {
        data.addProperty(new Property(SensorData.POWER_CONSUMED, rs.getDouble("PowerConsumed")));
      }
      if (rs.getObject("EnergyConsumedToDate") != null) {
        data.addProperty(new Property(SensorData.ENERGY_CONSUMED_TO_DATE, rs
            .getDouble("EnergyConsumedToDate")));
      }
      if (rs.getObject("PowerGenerated") != null) {
        data.addProperty(new Property(SensorData.POWER_GENERATED, rs.getDouble("PowerGenerated")));
      }
      if (rs.getObject("EnergyGeneratedToDate") != null) {
        data.addProperty(new Property(SensorData.ENERGY_GENERATED_TO_DATE, rs
            .getDouble("EnergyGeneratedToDate")));
      }

    }
    catch (SQLException e) {
      this.logger.info("DB: Error in getSource()" + StackTrace.toString(e));
      return null;
    }
    return data;
  }

  /** {@inheritDoc} */
  @Override
  public SensorDataIndex getSensorDataIndex(String sourceName) {
    if (sourceName == null) {
      return null;
    }
    else if (getSource(sourceName) == null) {
      // Unknown Source name, therefore no possibility of SensorData
      return null;
    }
    else {
      SensorDataIndex index = new SensorDataIndex();
      String statement =
          "SELECT Tstamp, Tool, Source FROM SensorData WHERE Source = ? ORDER BY Tstamp";
      Connection conn = null;
      PreparedStatement s = null;
      ResultSet rs = null;
      SensorDataRef ref;
      try {
        conn = connectionPool.getConnection();
        server.getLogger().fine(executeQueryMsg + statement);
        s = conn.prepareStatement(statement);
        s.setString(1, sourceName);
        rs = s.executeQuery();
        while (rs.next()) {
          Timestamp timestamp = rs.getTimestamp("Tstamp");
          String tool = rs.getString("Tool");
          String sourceUri = Source.sourceToUri(rs.getString("Source"), server);
          ref = new SensorDataRef(Tstamp.makeTimestamp(timestamp), tool, sourceUri);
          index.getSensorDataRef().add(ref);
        }
      }
      catch (SQLException e) {
        this.logger.info("DB: Error in getSensorDataIndex()" + StackTrace.toString(e));
      }
      finally {
        try {
          if (rs != null) {
            rs.close();
          }
          if (s != null) {
            s.close();
          }
          if (conn != null) {
            conn.close();
          }
        }
        catch (SQLException e) {
          this.logger.warning(errorClosingMsg + StackTrace.toString(e));
        }
      }
      return index;
    }
  }

  /** {@inheritDoc} */
  @Override
  public SensorDataIndex getSensorDataIndex(String sourceName, XMLGregorianCalendar startTime,
      XMLGregorianCalendar endTime) throws DbBadIntervalException {

    if ((sourceName == null) || (startTime == null)) {
      return null;
    }
    else if (getSource(sourceName) == null) {
      // Unknown Source name, therefore no possibility of SensorData
      return null;
    }
    else if (endTime != null && startTime.compare(endTime) == DatatypeConstants.GREATER) {
      // startTime > endTime, which is bogus
      throw new DbBadIntervalException(startTime, endTime);
    }
    else {
      SensorDataIndex index = new SensorDataIndex();
      String statement;
      if (endTime == null) {
        statement =
            "SELECT Tstamp, Tool, Source FROM SensorData WHERE Source = ? AND "
                + "Tstamp >= ? ORDER BY Tstamp";
      }
      else {
        statement =
            "SELECT Tstamp, Tool, Source FROM SensorData WHERE Source = ? AND "
                + " (Tstamp BETWEEN ? AND ?)" + " ORDER BY Tstamp";
      }
      Connection conn = null;
      PreparedStatement s = null;
      ResultSet rs = null;
      SensorDataRef ref;
      try {
        conn = connectionPool.getConnection();
        server.getLogger().fine(executeQueryMsg + statement);
        s = conn.prepareStatement(statement);
        s.setString(1, sourceName);
        s.setTimestamp(2, Tstamp.makeTimestamp(startTime));
        if (endTime != null) {
          s.setTimestamp(3, Tstamp.makeTimestamp(endTime));
        }
        rs = s.executeQuery();
        while (rs.next()) {
          Timestamp timestamp = rs.getTimestamp("Tstamp");
          String tool = rs.getString("Tool");
          String sourceUri = Source.sourceToUri(rs.getString("Source"), server);
          ref = new SensorDataRef(Tstamp.makeTimestamp(timestamp), tool, sourceUri);
          index.getSensorDataRef().add(ref);
        }
      }
      catch (SQLException e) {
        this.logger.info("DB: Error in getSensorDataIndex()" + StackTrace.toString(e));
      }
      finally {
        try {
          if (rs != null) {
            rs.close();
          }
          if (s != null) {
            s.close();
          }
          if (conn != null) {
            conn.close();
          }
        }
        catch (SQLException e) {
          this.logger.warning(errorClosingMsg + StackTrace.toString(e));
        }
      }
      return index;
    }
  }

  /** {@inheritDoc} */
  @Override
  public SensorDatas getSensorDatas(String sourceName, XMLGregorianCalendar startTime,
      XMLGregorianCalendar endTime) throws DbBadIntervalException {

    if ((sourceName == null) || (startTime == null)) {
      return null;
    }
    else if (getSource(sourceName) == null) {
      // Unknown Source name, therefore no possibility of SensorData
      return null;
    }
    else if (endTime != null && startTime.compare(endTime) == DatatypeConstants.GREATER) {
      // startTime > endTime, which is bogus
      throw new DbBadIntervalException(startTime, endTime);
    }
    else {
      SensorDatas datas = new SensorDatas();
      String statement;
      if (endTime == null) {
        statement = "SELECT * FROM SensorData WHERE Source = ? AND Tstamp >= ? ORDER BY Tstamp";
      }
      else {
        statement =
            "SELECT * FROM SensorData WHERE Source = ? AND (Tstamp BETWEEN ? AND ?)"
                + " ORDER BY Tstamp";
      }
      Connection conn = null;
      PreparedStatement s = null;
      ResultSet rs = null;
      try {
        conn = connectionPool.getConnection();
        server.getLogger().fine(executeQueryMsg + statement);
        s = conn.prepareStatement(statement);
        s.setString(1, sourceName);
        s.setTimestamp(2, Tstamp.makeTimestamp(startTime));
        if (endTime != null) {
          s.setTimestamp(3, Tstamp.makeTimestamp(endTime));
        }
        rs = s.executeQuery();
        while (rs.next()) {
          SensorData data = resultSetToSensorData(rs);
          List<Property> props = getSensorDataProperties(sourceName, data.getTimestamp());
          for (Property p : props) {
            data.addProperty(p);
          }
          datas.getSensorData().add(data);
        }
      }
      catch (SQLException e) {
        this.logger.info("DB: Error in getSensorDatas()" + StackTrace.toString(e));
      }
      finally {
        try {
          if (rs != null) {
            rs.close();
          }
          if (s != null) {
            s.close();
          }
          if (conn != null) {
            conn.close();
          }
        }
        catch (SQLException e) {
          this.logger.warning(errorClosingMsg + StackTrace.toString(e));
        }
      }
      return datas;
    }
  }

  /** {@inheritDoc} */
  @Override
  public SensorData getSensorData(String sourceName, XMLGregorianCalendar timestamp) {
    if ((sourceName == null) || (timestamp == null)) {
      return null;
    }
    else {
      String statement = "SELECT * FROM SensorData WHERE Source = ? AND Tstamp = ?";
      Connection conn = null;
      PreparedStatement s = null;
      ResultSet rs = null;
      boolean hasData = false;
      SensorData data = new SensorData();
      try {
        conn = connectionPool.getConnection();
        server.getLogger().fine(executeQueryMsg + statement);
        s = conn.prepareStatement(statement);
        s.setString(1, sourceName);
        s.setTimestamp(2, Tstamp.makeTimestamp(timestamp));
        rs = s.executeQuery();
        while (rs.next()) { // the select statement must guarantee only one row is returned.
          hasData = true;
          data = resultSetToSensorData(rs);
          List<Property> props = getSensorDataProperties(sourceName, data.getTimestamp());
          for (Property p : props) {
            data.addProperty(p);
          }
        }
      }
      catch (SQLException e) {
        this.logger.info("DB: Error in getSensorData()" + StackTrace.toString(e));
      }
      finally {
        try {
          if (rs != null) {
            rs.close();
          }
          if (s != null) {
            s.close();
          }
          if (conn != null) {
            conn.close();
          }
        }
        catch (SQLException e) {
          this.logger.warning(errorClosingMsg + StackTrace.toString(e));
        }
      }
      return (hasData) ? data : null;
    }
  }

  /** {@inheritDoc} */
  @Override
  protected SensorData getLatestNonVirtualSensorData(String sourceName) {
    if (sourceName == null) {
      return null;
    }
    Connection conn = null;
    PreparedStatement s = null;
    ResultSet rs = null;
    boolean hasData = false;
    SensorData data = new SensorData();
    try {
      conn = connectionPool.getConnection();
      String statement = "SELECT * FROM SensorData WHERE Source = ? ORDER BY Tstamp DESC LIMIT 1";
      server.getLogger().fine(executeQueryMsg + statement);
      s = conn.prepareStatement(statement);
      s.setString(1, sourceName);
      rs = s.executeQuery();
      if (rs.next()) {
        hasData = true;
        data = resultSetToSensorData(rs);
        List<Property> props = getSensorDataProperties(sourceName, data.getTimestamp());
        for (Property p : props) {
          data.addProperty(p);
        }
      }
    }
    catch (SQLException e) {
      this.logger.info("DB: Error in getSensorData()" + StackTrace.toString(e));
    }
    finally {
      try {
        if (rs != null) {
          rs.close();
        }
        if (s != null) {
          s.close();
        }
        if (conn != null) {
          conn.close();
        }
      }
      catch (SQLException e) {
        this.logger.warning(errorClosingMsg + StackTrace.toString(e));
      }
    }
    return (hasData) ? data : null;
  }

  /**
   * Get Properties for a SensorData.
   * 
   * @param sourceName The source to get properties for.
   * @param timestamp The timestamp to get properties for.
   * @return The list of properties for the Source and Timestamp.
   */
  private List<Property> getSensorDataProperties(String sourceName, XMLGregorianCalendar timestamp) {
    List<Property> props = new ArrayList<Property>();

    Connection conn = null;
    PreparedStatement s = null;
    ResultSet rs = null;
    try {
      conn = connectionPool.getConnection();
      String statement =
          "SELECT * FROM SensorDataProperty WHERE Source = ? AND Tstamp = ? ORDER BY PropertyKey ";
      server.getLogger().fine(executeQueryMsg + statement);
      s = conn.prepareStatement(statement);
      s.setString(1, sourceName);
      s.setTimestamp(2, Tstamp.makeTimestamp(timestamp));
      rs = s.executeQuery();
      while (rs.next()) {
        props.add(new Property(rs.getString("PropertyKey"), rs.getString("PropertyValue")));
      }

    }
    catch (SQLException e) {
      this.logger.info("DB: Error in getSensorDataProperties()" + StackTrace.toString(e));
    }
    finally {
      try {
        if (rs != null) {
          rs.close();
        }
        if (s != null) {
          s.close();
        }
        if (conn != null) {
          conn.close();
        }
      }
      catch (SQLException e) {
        this.logger.warning(errorClosingMsg + StackTrace.toString(e));
      }
    }
    return props;
  }

  /** {@inheritDoc} */
  @Override
  public boolean hasSensorData(String sourceName, XMLGregorianCalendar timestamp) {
    // Could be made a little faster by doing just the DB query ourselves and not reconstituting
    // the SensorData object, but that's probably an unneeded optimization.
    return (getSensorData(sourceName, timestamp) != null);
  }

  /** {@inheritDoc} */
  @Override
  public boolean storeSensorData(SensorData data) {
    if (data == null) {
      return false;
    }
    else {
      Connection conn = null;
      PreparedStatement s = null;
      try {
        conn = connectionPool.getConnection();
        if (conn.getMetaData().supportsTransactionIsolationLevel(
            Connection.TRANSACTION_READ_COMMITTED)) {
          conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        }
        conn.setAutoCommit(false);
        s = conn.prepareStatement("INSERT INTO SensorData VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
        // Order: Tstamp Tool Source PowerConsumed EnergyConsumedToDate PowerGenerated
        // EnergyGeneratedToDate LastMod
        s.setTimestamp(1, Tstamp.makeTimestamp(data.getTimestamp()));
        s.setString(2, data.getTool());
        s.setString(3, UriUtils.getUriSuffix(data.getSource()));

        // Initialize properties to NULL
        s.setNull(4, java.sql.Types.DOUBLE);
        s.setNull(5, java.sql.Types.DOUBLE);
        s.setNull(6, java.sql.Types.DOUBLE);
        s.setNull(7, java.sql.Types.DOUBLE);

        if (data.isSetProperties()) {
          if (data.getProperties().getProperty(SensorData.POWER_CONSUMED) != null) {
            s.setDouble(4, data.getProperties().getPropertyAsDouble(SensorData.POWER_CONSUMED));
          }
          if (data.getProperties().getProperty(SensorData.ENERGY_CONSUMED_TO_DATE) != null) {
            s.setDouble(5,
                data.getProperties().getPropertyAsDouble(SensorData.ENERGY_CONSUMED_TO_DATE));
          }
          if (data.getProperties().getProperty(SensorData.POWER_GENERATED) != null) {
            s.setDouble(6, data.getProperties().getPropertyAsDouble(SensorData.POWER_GENERATED));
          }
          if (data.getProperties().getProperty(SensorData.ENERGY_GENERATED_TO_DATE) != null) {
            s.setDouble(7,
                data.getProperties().getPropertyAsDouble(SensorData.ENERGY_GENERATED_TO_DATE));
          }
        }

        s.setTimestamp(8, new Timestamp(new Date().getTime()));
        s.executeUpdate();

        if (data.isSetProperties()) {
          for (Property p : data.getProperties().getProperty()) {
            if (!p.getKey().equals(SensorData.POWER_CONSUMED)
                && !p.getKey().equals(SensorData.ENERGY_CONSUMED_TO_DATE)
                && !p.getKey().equals(SensorData.POWER_GENERATED)
                && !p.getKey().equals(SensorData.ENERGY_GENERATED_TO_DATE)) {
              s = conn.prepareStatement("INSERT INTO SensorDataProperty VALUES (?, ?, ?, ?) ");
              s.setTimestamp(1, Tstamp.makeTimestamp(data.getTimestamp()));
              s.setString(2, UriUtils.getUriSuffix(data.getSource()));
              s.setString(3, p.getKey());
              s.setString(4, p.getValue());
              s.executeUpdate();
            }
          }
        }

        conn.commit();
        this.logger.fine("PostgreSQL: Inserted SensorData" + data.getTimestamp());
        return true;
      }
      catch (SQLException e) {
        if (DUPLICATE_KEY.equals(e.getSQLState())) {
          this.logger.fine("PostgreSQL: Attempted to overwrite SensorData " + data.getTimestamp());
          return false;
        }
        else if (FOREIGN_KEY_VIOLATION.equals(e.getSQLState())) {
          this.logger.fine("Derby: Foreign key constraint violation on SensorData "
              + data.getTimestamp() + ". " + e.getMessage());
          return false;
        }
        else {
          this.logger.info(postgresError + StackTrace.toString(e));
          return false;
        }
      }
      finally {
        try {
          if (s != null) {
            s.close();
          }
          if (conn != null) {
            conn.setAutoCommit(true);
            conn.close();
          }
        }
        catch (SQLException e) {
          this.logger.warning(errorClosingMsg + StackTrace.toString(e));
        }
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public boolean deleteSensorData(String sourceName, XMLGregorianCalendar timestamp) {
    boolean succeeded = false;
    if ((sourceName == null) || (timestamp == null)) {
      return false;
    }
    Connection conn = null;
    try {
      conn = connectionPool.getConnection();
      conn.setAutoCommit(false);

      deleteSensorDataProperties(sourceName, timestamp, conn);
      String statement =
          "DELETE FROM SensorData WHERE Source='" + sourceName.replace("'", "''")
              + "' AND Tstamp='" + Tstamp.makeTimestamp(timestamp) + "'";
      succeeded = deleteResource(statement, conn);
      return succeeded;
    }
    catch (SQLException e) {
      return false;
    }
    finally {
      if (conn != null) {
        try {
          conn.setAutoCommit(true);
          conn.close();
        }
        catch (SQLException e) { // NOPMD

        }
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public boolean deleteSensorData(String sourceName) {
    if (sourceName == null) {
      return false;
    }

    Connection conn = null;
    try {
      conn = connectionPool.getConnection();
      conn.setAutoCommit(false);
      deleteSensorDataProperties(sourceName, conn);
      String statement =
          "DELETE FROM SensorData WHERE Source='" + sourceName.replace("'", "''") + "'";
      return deleteResource(statement, conn);
    }
    catch (SQLException e) {
      return false;
    }
    finally {
      if (conn != null) {
        try {
          conn.setAutoCommit(true);
          conn.close();
        }
        catch (SQLException e) { // NOPMD

        }
      }
    }
  }

  /**
   * Ensures that all sensor data from the named Source is no longer present in storage.
   * 
   * @param sourceName The name of the Source whose sensor data is to be deleted.
   * @param conn The connection encapsulating this transaction.
   * @return True if all the sensor data was deleted, or false if it was not deleted or the
   * requested Source does not exist.
   */
  private boolean deleteSensorData(String sourceName, Connection conn) {
    boolean succeeded = false;

    if (sourceName == null) {
      return false;
    }
    else {
      deleteSensorDataProperties(sourceName, conn);
      String statement =
          "DELETE FROM SensorData WHERE Source='" + sourceName.replace("'", "''") + "'";
      succeeded = deleteResource(statement, conn);
    }
    return succeeded;
  }

  /**
   * Delete properties from SensorDataProperty table given a sourceURI and timestamp.
   * 
   * @param sourceName The URI of the source to delete properties for.
   * @param timestamp The timestamp to delete properties for.
   * @param conn The connection encapsulating this transaction.
   * @return True if the properties were successfully deleted.
   */
  private boolean deleteSensorDataProperties(String sourceName, XMLGregorianCalendar timestamp,
      Connection conn) {
    return deleteResource(
        "DELETE FROM SensorDataProperty WHERE Source='" + sourceName.replace("'", "''")
            + "' AND Tstamp='" + Tstamp.makeTimestamp(timestamp) + "'", conn);
  }

  /**
   * Delete properties from SensorDataProperty table given a sourceURI.
   * 
   * @param sourceName The URI of the source to delete properties for.
   * @param conn The connection encapsulating this transaction.
   * @return True if the properties were successfully deleted.
   */
  private boolean deleteSensorDataProperties(String sourceName, Connection conn) {
    return deleteResource(
        "DELETE FROM SensorDataProperty WHERE Source='" + sourceName.replace("'", "''") + "'", conn);
  }

  /**
   * Returns a SensorDataStraddle that straddles the given timestamp, using SensorData from the
   * given source. Note that a virtual source contains no SensorData directly, so this method will
   * always return null if the given sourceName is a virtual source. To obtain a list of
   * SensorDataStraddles for all the non-virtual subsources of a virtual source, see
   * getSensorDataStraddleList.
   * 
   * If the given timestamp corresponds to an actual SensorData, then return a degenerate
   * SensorDataStraddle with both ends of the straddle set to the actual SensorData.
   * 
   * @param source The source object to generate the straddle from.
   * @param timestamp The timestamp of interest in the straddle.
   * @return A SensorDataStraddle that straddles the given timestamp. Returns null if: parameters
   * are null, the source doesn't exist, source has no sensor data, or there is no sensor data that
   * straddles the timestamp.
   * @see org.wattdepot.server.db.memory#getSensorDataStraddleList
   */
  @Override
  public SensorDataStraddle getSensorDataStraddle(Source source, XMLGregorianCalendar timestamp) {

    SensorData beforeData = null, afterData = null;
    if ((source == null) || (timestamp == null)) {
      return null;
    }
    String sourceName = source.getName();

    XMLGregorianCalendar dataTimestamp;
    int dataTimestampCompare;

    Connection conn = null;
    PreparedStatement s = null;
    ResultSet rs = null;
    String statement;

    try {
      conn = connectionPool.getConnection();
      // Find data just before desired timestamp
      statement =
          "Select * FROM SensorData WHERE Source = ? AND Tstamp <= ? ORDER BY Tstamp DESC LIMIT 1";
      server.getLogger().fine(executeQueryMsg + statement);
      s = conn.prepareStatement(statement);
      s.setString(1, sourceName);
      s.setTimestamp(2, Tstamp.makeTimestamp(timestamp));
      rs = s.executeQuery();
      // Expecting only one row of data (fetch first row only)
      while (rs.next()) {
        beforeData = resultSetToSensorData(rs);
        dataTimestamp = beforeData.getTimestamp(); // Tstamp.makeTimestamp(rs.getTimestamp("Tstamp"));
        dataTimestampCompare = dataTimestamp.compare(timestamp);
        if (dataTimestampCompare == DatatypeConstants.EQUAL) {
          return new SensorDataStraddle(timestamp, beforeData, beforeData);
        }
      }
      // Close those statement and result set resources before we reuse the variables
      s.close();
      rs.close();

      // Find data just after desired timestamp
      statement =
          "SELECT * FROM SensorData WHERE Source = ? AND Tstamp >= ? "
              + "ORDER BY Tstamp ASC LIMIT 1";
      server.getLogger().fine(executeQueryMsg + statement);
      s = conn.prepareStatement(statement);
      s.setString(1, sourceName);
      s.setTimestamp(2, Tstamp.makeTimestamp(timestamp));
      rs = s.executeQuery();
      // Expecting only one row of data (fetch first row only)
      while (rs.next()) {
        afterData = resultSetToSensorData(rs);
        dataTimestamp = afterData.getTimestamp(); // Tstamp.makeTimestamp(rs.getTimestamp("Tstamp"));
        dataTimestampCompare = dataTimestamp.compare(timestamp);
        if (dataTimestampCompare == DatatypeConstants.EQUAL) {
          // There is SensorData for the requested timestamp, but we already checked for this.
          // Thus there is a logic error somewhere
          this.logger
              .warning("Found sensordata that matches timestamp, but after already checked!");
          return new SensorDataStraddle(timestamp, afterData, afterData);
        }
      }

      if ((beforeData == null) || (afterData == null)) {
        // one of the sentinels never got changed, so no straddle
        return null;
      }
      else {
        return new SensorDataStraddle(timestamp, beforeData, afterData);
      }
    }
    catch (SQLException e) {
      this.logger.info("DB: Error in getSensorDataStraddle()" + StackTrace.toString(e));
      return null;
    }
    finally {
      try {
        if (rs != null) {
          rs.close();
        }
        if (s != null) {
          s.close();
        }
        if (conn != null) {
          conn.close();
        }
      }
      catch (SQLException e) {
        this.logger.warning(errorClosingMsg + StackTrace.toString(e));
      }
    }
  }

  /** The SQL string for creating the WattDepotUser table. So named because 'User' is reserved. */
  private static final String createUserTableStatement = "create table WattDepotUser  " + "("
      + " Username VARCHAR(128) NOT NULL, " + " Password VARCHAR(128) NOT NULL, "
      + " Admin BOOLEAN NOT NULL, " + " LastMod TIMESTAMP NOT NULL, " + " PRIMARY KEY (Username) "
      + ")";

  /** An SQL string to test whether the User table exists and has the correct schema. */
  private static final String testUserTableStatement = " UPDATE WattDepotUser SET "
      + " Username = 'TestEmail@foo.com', " + " Password = 'changeme', " + " Admin = FALSE, "
      + " LastMod = '" + new Timestamp(new Date().getTime()).toString() + "' " + " WHERE 1=3";

  /** An SQL string to drop the User table. */
  private static final String dropUserTableStatement = "DROP TABLE WattDepotUser";

  /** The SQL string for creating the WattDepotUserProperty table. */
  private static final String createUserPropertyTableStatement =
      "create table WattDepotUserProperty " + "(" + " Username VARCHAR(128) NOT NULL, "
          + " PropertyKey VARCHAR(128) NOT NULL, " + " PropertyValue VARCHAR(128) NOT NULL, "
          + " PRIMARY KEY (Username, PropertyKey), "
          + " FOREIGN KEY (Username) REFERENCES WattDepotUser" + ")";

  /** An SQL string to test whether the UserProperty table exists and has the correct schema. */
  private static final String testUserPropertyTableStatement = " UPDATE WattDepotUserProperty SET "
      + " Username = 'TestEmail@foo.com', " + " PropertyKey = 'awesomeness', "
      + " PropertyValue = 'total'" + " WHERE 1=3";

  /** An SQL string to drop the UserProperty table. */
  private static final String dropUserPropertyTableStatement = "DROP TABLE WattDepotUserProperty";

  /** {@inheritDoc} */
  @Override
  public UserIndex getUsers() {
    UserIndex index = new UserIndex();
    String statement = "SELECT Username FROM WattDepotUser ORDER BY Username";
    Connection conn = null;
    PreparedStatement s = null;
    ResultSet rs = null;
    try {
      conn = connectionPool.getConnection();
      server.getLogger().fine(executeQueryMsg + statement);
      s = conn.prepareStatement(statement);
      rs = s.executeQuery();
      while (rs.next()) {
        index.getUserRef().add(new UserRef(rs.getString("Username"), this.server));
      }
    }
    catch (SQLException e) {
      this.logger.info("DB: Error in getUsers()" + StackTrace.toString(e));
    }
    finally {
      try {
        if (rs != null) {
          rs.close();
        }
        if (s != null) {
          s.close();
        }
        if (conn != null) {
          conn.close();
        }
      }
      catch (SQLException e) {
        this.logger.warning(errorClosingMsg + StackTrace.toString(e));
      }
    }
    return index;
  }

  /** {@inheritDoc} */
  @Override
  public User getUser(String username) {
    if (username == null) {
      return null;
    }
    else {
      String statement = "SELECT * FROM WattDepotUser WHERE Username = ?";
      Connection conn = null;
      PreparedStatement s = null;
      ResultSet rs = null;
      boolean hasData = false;
      User user = new User();
      try {
        conn = connectionPool.getConnection();
        server.getLogger().fine(executeQueryMsg + statement);
        s = conn.prepareStatement(statement);
        s.setString(1, username);
        rs = s.executeQuery();
        while (rs.next()) { // the select statement must guarantee only one row is returned.
          hasData = true;
          user.setEmail(rs.getString("Username"));
          user.setPassword(rs.getString("Password"));
          user.setAdmin(rs.getBoolean("Admin"));

          List<Property> props = getUserProperties(username);
          for (Property p : props) {
            user.addProperty(p);
          }
        }
      }
      catch (SQLException e) {
        this.logger.info("DB: Error in getUser()" + StackTrace.toString(e));
      }
      finally {
        try {
          if (rs != null) {
            rs.close();
          }
          if (s != null) {
            s.close();
          }
          if (conn != null) {
            conn.close();
          }
        }
        catch (SQLException e) {
          this.logger.warning(errorClosingMsg + StackTrace.toString(e));
        }
      }
      return (hasData) ? user : null;
    }
  }

  /**
   * Get properties for a particular WattDepotUser.
   * 
   * @param username The user to get properties for.
   * @return A list of the properties for the user.
   */
  private List<Property> getUserProperties(String username) {
    List<Property> props = new ArrayList<Property>();
    String statement =
        "SELECT * FROM WattDepotUserProperty WHERE Username = ? ORDER BY PropertyKey";
    Connection conn = null;
    PreparedStatement s = null;
    ResultSet rs = null;
    try {
      conn = connectionPool.getConnection();
      server.getLogger().fine(executeQueryMsg + statement);
      s = conn.prepareStatement(statement);
      s.setString(1, username);
      rs = s.executeQuery();
      while (rs.next()) {
        props.add(new Property(rs.getString("PropertyKey"), rs.getString("PropertyValue")));
      }
    }
    catch (SQLException e) {
      this.logger.info("DB: Error in getUserProperties()" + StackTrace.toString(e));
    }
    finally {
      try {
        if (rs != null) {
          rs.close();
        }
        if (s != null) {
          s.close();
        }
        if (conn != null) {
          conn.close();
        }
      }
      catch (SQLException e) {
        this.logger.warning(errorClosingMsg + StackTrace.toString(e));
      }
    }
    return props;

  }

  /** {@inheritDoc} */
  @Override
  public boolean storeUser(User user) {
    if (user == null) {
      return false;
    }
    else {
      Connection conn = null;
      PreparedStatement s = null;

      try {
        conn = connectionPool.getConnection();
        if (conn.getMetaData().supportsTransactionIsolationLevel(
            Connection.TRANSACTION_READ_COMMITTED)) {
          conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        }
        conn.setAutoCommit(false);
        s = conn.prepareStatement("INSERT INTO WattDepotUser VALUES (?, ?, ?, ?)");
        // Order: Username Password Admin LastMod
        s.setString(1, user.getEmail());
        s.setString(2, user.getPassword());
        s.setBoolean(3, user.isAdmin());

        s.setTimestamp(4, new Timestamp(new Date().getTime()));
        s.executeUpdate();
        s.close();

        s = conn.prepareStatement("INSERT INTO WattDepotUserProperty VALUES (?, ?, ?)");
        s.setString(1, user.getEmail());
        if (user.isSetProperties()) {
          for (Property p : user.getProperties().getProperty()) {
            s.setString(2, p.getKey());
            s.setString(3, p.getValue());
            s.executeUpdate();
          }
        }
        conn.commit();

        this.logger.fine("PostgreSQL: Inserted User" + user.getEmail());
        return true;
      }
      catch (SQLException e) {
        if (DUPLICATE_KEY.equals(e.getSQLState())) {
          this.logger.fine("PostgreSQL: Attempted to overwrite User " + user.getEmail());
          return false;
        }
        else if (FOREIGN_KEY_VIOLATION.equals(e.getSQLState())) {
          this.logger.fine("Derby: Foreign key constraint violation on User " + user.getEmail()
              + ". " + e.getMessage());
          return false;
        }
        else {
          this.logger.info(postgresError + StackTrace.toString(e));
          return false;
        }
      }
      finally {
        try {
          if (s != null) {
            s.close();
          }
          if (conn != null) {
            conn.setAutoCommit(true);
            conn.close();
          }
        }
        catch (SQLException e) {
          this.logger.warning(errorClosingMsg + StackTrace.toString(e));
        }
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public boolean deleteUser(String username) {
    if (username == null) {
      return false;
    }

    Connection conn = null;
    try {
      conn = connectionPool.getConnection();
      conn.setAutoCommit(false);
      deleteSourcesForOwner(username, conn);
      deleteUserProperties(username, conn);
      String statement =
          "DELETE FROM WattDepotUser WHERE Username='" + username.replace("'", "''") + "'";
      return deleteResource(statement, conn);
    }
    catch (SQLException e) {
      return false;
    }
    finally {
      if (conn != null) {
        try {
          conn.setAutoCommit(true);
          conn.close();
        }
        catch (SQLException e) { // NOPMD

        }
      }
    }
  }

  /**
   * Delete properties from WattDepotUserProperty table given a username.
   * 
   * @param username The name of the user to delete properties for.
   * @param conn The connection encapsulating this transaction.
   * @return True if the properties were successfully deleted.
   */
  private boolean deleteUserProperties(String username, Connection conn) {
    return deleteResource(
        "DELETE FROM WattDepotUserProperty WHERE Username='" + username.replace("'", "''") + "'",
        conn);
  }

  /**
   * Deletes the resource, given the SQL statement to perform the delete.
   * 
   * @param statement The SQL delete statement.
   * @param conn The connection encapsulating this transaction.
   * @return True if resource was successfully deleted, false otherwise.
   */
  private boolean deleteResource(String statement, Connection conn) {
    PreparedStatement s = null;
    boolean succeeded = false;

    try {
      server.getLogger().fine("PostgreSQL: " + statement);
      s = conn.prepareStatement(statement);
      int rowCount = s.executeUpdate();
      // actually deleted something
      if (rowCount >= 1) {
        succeeded = true;
      }
      else {
        // didn't delete anything (maybe resource didn't exist?)
        return false;
      }
    }
    catch (SQLException e) {
      this.logger.info("PostgreSQL: Error in deleteResource()" + StackTrace.toString(e));
      succeeded = false;
    }
    finally {
      try {
        if (s != null) {
          s.close();
        }
      }
      catch (SQLException e) {
        this.logger.warning(errorClosingMsg + StackTrace.toString(e));
      }
    }
    return succeeded;
  }

  /**
   * Reclaim storage space from deleted or updated tuples. PostgreSQL doesn't do this automatically
   * (as of version 8.3) so this should be done somewhat regularly on tables that are frequently
   * updated.
   * 
   * @return True if the maintenance succeeded
   */
  @Override
  public boolean performMaintenance() {
    boolean success = false;
    this.logger.fine("Starting garbage collection.");
    Connection conn = null;
    CallableStatement s = null;
    try {
      conn = connectionPool.getConnection();
      s = conn.prepareCall("VACUUM");
      s.execute();
      s.close();
      success = true;
    }
    catch (SQLException e) {
      this.logger.info("PostgreSQL: Error in performMaintenance()" + StackTrace.toString(e));
      success = false;
    }
    finally {
      try {
        if (s != null) {
          s.close();
        }
        if (conn != null) {
          conn.close();
        }
      }
      catch (SQLException e) {
        this.logger.warning(errorClosingMsg + StackTrace.toString(e));
        success = false;
      }
    }
    this.logger.fine("Finished garbage collection.");
    return success;

  }

  /** {@inheritDoc} */
  @Override
  public boolean indexTables() {
    this.logger.fine("Starting to index tables.");
    boolean success = false;
    Connection conn = null;
    Statement s = null;
    try {
      conn = connectionPool.getConnection();
      s = conn.createStatement();

      // Note: If the db is being set up for the first time, it is not an error for the drop index
      // statement to fail. Thus, we simply log the occurrence.

      try {
        s.execute(dropSensorDataSourceTstampDescStatement);
      }
      catch (Exception e) {
        this.logger.info("Failed to drop SensorData(Source, Tstamp DESC) index.");
      }
      s.execute(indexSensorDataSourceTstampDescStatement);

      s.close();
      success = true;
    }
    catch (SQLException e) {
      this.logger.info("PostgreSQL: Error in indexTables()" + StackTrace.toString(e));
      success = false;
    }
    finally {
      try {
        if (s != null) {
          s.close();
        }
        if (conn != null) {
          conn.close();
        }
      }
      catch (SQLException e) {
        this.logger.warning(errorClosingMsg + StackTrace.toString(e));
        success = false;
      }
    }
    this.logger.fine("Finished indexing tables.");
    return success;
  }

  @Override
  public boolean wipeData() {
    try {
      wipeTables();
      return true;
    }
    catch (SQLException e) {
      return false;
    }
  }

  /**
   * {@inheritDoc} Multiple backup or snapshot options are available in PostgreSQL. This method
   * assumes no prior knowledge of the Postgres installation, but it only works when the database
   * and the code are running on the same machine. Other options could allow for a remote host, but
   * require more knowledge about the Postgres installation. This method requires the archive_mode
   * to be on, and works on Postgres 8.3 and above. When combined with write ahead log (WAL)
   * archiving, this enables Point-In-Time recovery. For more information, see:
   * http://www.postgresql.org/docs/8.3/interactive/continuous-archiving.html
   * 
   * This doesn't work on all systems because of permissions. Backups can also be done fairly easily
   * outside of the WattDepot system.
   */
  @Override
  public boolean makeSnapshot() {
    this.logger.fine("Creating snapshot of database.");
    boolean success = false;

    Connection conn = null;
    PreparedStatement s = null;
    ResultSet rs = null;
    try {
      conn = connectionPool.getConnection();
      s = conn.prepareCall("select pg_start_backup('" + new Date().toString() + "');");
      s.execute();
      s.close();

      // figure out where postgres stores its data
      s = conn.prepareCall("select setting from pg_settings where name='data_directory'");
      rs = s.executeQuery();
      String dataPath = "";
      if (rs.next()) {
        dataPath = rs.getString("setting");

        s.close();
        rs.close();

        // now zip the whole data directory up as a backup
        this.logger.info("PostgreSQL: Creating zip backup from " + dataPath);

        FileOutputStream fout =
            new FileOutputStream(server.getServerProperties().get(
                ServerProperties.POSTGRES_SNAPSHOT_KEY)
                + ".zip");
        ZipOutputStream zout = new ZipOutputStream(fout);
        File fileSource = new File(dataPath);
        addDirectory(zout, fileSource, "");
        zout.close();

        success = true;
      }
      else {
        success = false;
      }
    }
    catch (SQLException e) {
      this.logger.warning("PostgreSQL: Error in makeSnapshot():" + StackTrace.toString(e));
      success = false;
    }
    catch (FileNotFoundException e) {
      this.logger.warning("PostgreSQL: Error in makeSnapshot():" + StackTrace.toString(e));
      success = false;
    }
    catch (IOException e) {
      this.logger.warning("PostgreSQL: IO Error in makeSnapshot():" + StackTrace.toString(e));
      success = false;
    }
    finally {
      try {
        // always stop backup at the end
        if (conn != null) {
          s = conn.prepareCall("select pg_stop_backup();");
          s.execute();
          s.close();
        }

        // don't set success = true here, because we might have had an error copying the backup.
      }
      catch (SQLException e) {
        this.logger.warning("PostgreSQL: Error stopping backup in makeSnapshot():"
            + StackTrace.toString(e));
        success = false;
      }
      finally {
        try {
          if (s != null) {
            s.close();
          }
          if (conn != null) {
            conn.close();
          }
          if (rs != null) {
            rs.close();
          }
        }
        catch (SQLException e) {
          this.logger.warning(errorClosingMsg + StackTrace.toString(e));
          success = false;
        }
      }
    }
    this.logger.fine("PostgreSQL: Created snapshot of database.");

    return success;
  }

  /**
   * Recursively add everything in fileSource to zout zip file. Adapted from
   * www.java-examples.com/create-zip-file-directory-recursively-using-zipoutputstream-example
   * 
   * @param zout The zip file to make
   * @param fileSource The directory to zip
   * @param parent The name of the parent directory
   */
  private void addDirectory(ZipOutputStream zout, File fileSource, String parent) {

    // get sub-folder/files list
    File[] files = fileSource.listFiles();
    if (files != null) {

      for (int i = 0; i < files.length; i++) {
        // if the file is directory, call the function recursively
        if (files[i].isDirectory()) {
          if (!files[i].getName().equals("pg_xlog")) {
            addDirectory(zout, files[i], parent + files[i].getName() + "/");
          }
          continue;
        }

        // it is a file and not directory, so add it to the zip file
        try {
          byte[] buffer = new byte[1024];
          FileInputStream fin = new FileInputStream(files[i]);
          zout.putNextEntry(new ZipEntry(parent + files[i].getName()));

          // After creating entry in the zip file, actually write the file.
          int length;

          while ((length = fin.read(buffer)) > 0) {
            zout.write(buffer, 0, length);
          }

          zout.closeEntry();
          fin.close();

        }
        catch (IOException ioe) {
          System.out.println("IOException :" + ioe);
        }
      }
    }
    else {
      this.logger.info("PostgreSQL: makeSnapshot() tried to zip null file " + fileSource.getName());
    }

  }

  /**
   * Close the connection object pool.
   */
  @Override
  public void stop() {
    try {
      this.connectionPool.close();
    }
    catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
}

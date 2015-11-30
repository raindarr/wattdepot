# Introduction #

WattDepot is designed to support multiple back-end storage implementations for maximal flexibility. WattDepot currently supports four backend storage implementations:

  * [Apache Derby](http://db.apache.org/derby/), an embedded, open source Java SQL database
  * [PostgreSQL](http://www.postgresql.org/), a widely-used, open source database
  * [Oracle Berkeley DB Java Edition](http://www.oracle.com/technetwork/products/berkeleydb/overview/index-093405.html), another open source, embedded database
  * An ephemeral in-memory storage implementation designed for test purposes

Derby is the backend that has be exercised the most to date, though PostgreSQL support is now under heavy development. Berkeley DB has not been used in production to date, and the memory storage system is not appropriate for production since it does not write any data to disk.

The storage implementation is picked using the `wattdepot-server.db.impl` server property. The default server properties file has examples on selecting each of the storage implementations.

This page documents the different storage options available.

# Derby #

Derby provides an embedded SQL database that runs inside the WattDepot server. Since it is embedded, it does not require an additional process to run, as the database will be started up and shut down along with the server process itself.

By default, the DerbyStorageImplementation stores all its data in a `Derby` subdirectory under the server home directory. This path can be customized with the `wattdepot-server.db.derby.dir` server property. Unit tests will store data in a subdirectory specified by `wattdepot-server.test.db.derby.dir`, which defaults to `testDerby` under the server home directory.

## Snapshots ##

The RestApi provides a way to make a [snapshot of the database](http://code.google.com/p/wattdepot/wiki/RestApi#PUT_{host}/db/snapshot) to another directory specified by the `wattdepot-server.db.derby.snapshot` property. By default, this is set to the Derby-snapshot directory  under the server home. This snapshot is performed online, so the server will continue to respond to new sensor data while the snapshot is being made. Backups of the Derby db directory made while the server is active are likely to be corrupt, so backups should be made of the snapshot directory instead.

## Wiping schema and data ##

When you run unit tests, Derby will automatically update an incorrect schema. However, if you wish to manually wipe the schema or need to deploy schema changes to a production server with no data you can simply delete the Derby directory at `wattdepot-server.db.derby.dir` or `wattdepot-server.test.db.derby.dir`.

# PostgreSQL #

PostgreSQL is an external database, so it must be installed separately if it does not already exist on your system. You can find out more about [PostgreSQL at their website](http://www.postgresql.org/), and you can get it from their [download page](http://www.postgresql.org/download/).

There are four ways to setup a connection to your Postgres database. The first option is to use the server properties listed in the following table.If you have a Postgres instance set up on your local machine using the default port this is generally the easiest choice. **Setting your Postgres username and password is required**.

| **Property** | **Default Value** |
|:-------------|:------------------|
|`wattdepot-server.db.hostname` | localhost         |
|`wattdepot-server.test.db.hostname` | localhost         |
|`wattdepot-server.db.dbName` | wattdepot         |
|`wattdepot-server.test.db.dbName` | testwattdepot     |
|`wattdepot-server.db.port` | 5432              |
|`wattdepot-server.test.db.port` | 5432              |
|`wattdepot-server.db.username` |                   |
|`wattdepot-server.db.password` |                   |



The second way to specify a Postgres connection is through the `wattdepot-server.db.url` property. The property must be set to a valid JDBC connection URL, following the form `postgresql://hostName/dbName?user=user&password=password`. For example, "postgresql://localhost:5432/wattdepot?user=myUser&password=myPassword"

These two options will both create the wattdepot database catalog and set up the appropriate tables.

The other two ways to use Postgres assume that WattDepot is running on Heroku, and are covered in the InstallingWattDepotServerOnHeroku guide. As a recap: when deploying to Heroku, you don't need to do anything. WattDepot automatically knows to use Postgres and how to connect to it, as long as you have set the Heroku properties correctly. When testing on Heroku, use the `wattdepot-server.heroku.db.url` server property (in addition to the `wattdepot-server.test.heroku` and `wattdepot-server.heroku.hostname` properties).

## Snapshots ##

It is possible to create a Snapshot of your Postgres database through the [RestApi](http://code.google.com/p/wattdepot/wiki/RestApi#PUT_{host}/db/snapshot) under specific conditions. The `archive_mode` feature of Postgres must be turned on, and the system user that WattDepot is running under must have access to the data directory of Postgres. If these conditions are met and the snapshot is created, it is placed in the location specified in the `wattdepot-server.db.postgres.snapshot` property as a .zip file. By default, it is Postgres-snapshot.zip under the server home directory.

Postgres has other backup and snapshot capabilities built in, so it is recommended to use those rather than the limited functionality provided through WattDepot.

## Wiping schema and data ##

When you run unit tests, Postgres will automatically update an incorrect schema. However, if you wish to manually wipe the schema or need to deploy schema changes to a production server with no data you can use the following instructions. Note that these instructions will **DELETE ALL DATA** in the database catalog that you wipe, so this should not be done frequently or without careful consideration.

First you need to start the `psql` command line tool. Depending on your Postgres configuration, this might be as easy as typing `psql`, or you might have to do something like `sudo -u postgres psql` and enter in your sudo password and your Postgres database password.

The `\l` command will list all of your database catalogs. By default, you will have `testwattdepot` or `wattdepot`. Let's assume we are wiping the test schema, so we want to connect to the testwattdepot database using `\c testwattdepot`. This will look something like this:

```
psql (9.1.3)
Type "help" for help.


postgres=# \c testwattdepot
You are now connected to database "testwattdepot" as user "postgres".
testwattdepot=#
```

Use the `\d` command to list all of the tables in this catalog, and drop each table in turn. Note that in WattDepot 2.0 and above, the order that you delete the tables may matter.

```
testwattdepot=# \d
             List of relations
 Schema |     Name      | Type  |  Owner   
--------+---------------+-------+----------
 public | sensordata    | table | postgres
 public | source        | table | postgres
 public | wattdepotuser | table | postgres
(3 rows)

testwattdepot=# drop table sensordata;
DROP TABLE
testwattdepot=# drop table source;
DROP TABLE
testwattdepot=# drop table wattdepotuser;
DROP TABLE
testwattdepot=# \q
```

The database should be clean now. Note that the unit tests run in the "testwattdepot" catalog, so this should not affect any production data (though you probably want your production data separate from your development postgres instance anyway).

# BerkeleyDb #

The BerkeleyDb implementation was written as a proof-of-concept and has not been tested extensively. By default, the BerkeleyDbImplementation stores all its data in a `BerkeleyDb` subdirectory under the server home directory. This path can be customized with the `wattdepot-server.db.berkeleydb.dir` server property. Unit tests will store data in a subdirectory specified by `wattdepot-server.test.db.berkeleydb.dir`, which defaults to `testBerkeleyDb` under the server home directory.

## Snapshots ##
The [RestApi](http://code.google.com/p/wattdepot/wiki/RestApi#PUT_{host}/db/snapshot) functionality for a snapshot in BerkeleyDb creates a `backup` directory under the BerkeleyDb or testBerkeleyDb directory.

## Wiping schema and data ##

When you run unit tests, BerkeleyDb will automatically update an incorrect schema. However, if you wish to manually wipe the schema or need to deploy schema changes to a production server with no data you can simply delete the BerkeleyDb directory at `wattdepot-server.db.berkeleydb.dir` or `wattdepot-server.test.db.berkeleydb.dir`.
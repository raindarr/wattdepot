# Introduction #

This document explains how to install the WattDepot server on a local computer server.

# Requirements #

To run the WattDepot server, you just need a computer running Java 1.6 or later. If you do not already have Java installed, you can [download it](http://www.java.com/). Java is not included on Mac OS X 10.7 (Lion), so you will be prompted to download it after trying to run a Java program for the first time. You can initiate this by typing `java -version` in a Terminal window.

You can run the WattDepot server from your own account, but you may want to create a separate `wattdepot` account and run it from that, to keep things separate.

# Download WattDepot #

Download the latest release of WattDepot from the [downloads page](http://code.google.com/p/wattdepot/downloads/list). You can read the VersionHistory for each release if you wish. Unzip the archive you downloaded.

# Set up WattDepot server home directory #

WattDepot stores all its files in a server home directory. By default, this will be a `.wattdepot` directory in your home directory. If you want to create the `.wattdepot` directory elsewhere, set the `wattdepot.user.home` Java property to the desired parent directory. The name `.wattdepot` directory name itself is not configurable.

Within the server home directory are subdirectories that store all the data for a particular server instance. The default subdirectory is `server`, but this can be changed on the command line with the `-d` argument. This allows multiple WattDepot server instances to run in parallel, each writing to a different subdirectory. Create this directory:

```
mkdir ~/.wattdepot
mkdir ~/.wattdepot/server
```

# Customize the server properties file #

The server reads properties on launch from a file named `wattdepot-server.properties` in the server subdirectory. A default properties file can be found in the wattdepot files you downloaded as `src/main/resources/etc/wattdepot-server.properties.default`. Copy that file to your new server subdirectory (`~/.wattdepot/server` if you are using the defaults), and rename it to `wattdepot-server.properties`.

After the file is copied into place, open it in your favorite text editor and change or uncomment any lines you want. At a minimum, you will want to change the default administrator username and password.

WattDepot currently supports multiple [backend storage implementations](StorageImplementations.md). You can choose which storage backend you wish to use in the server properties file. If you don't make a selection, it will default to Derby.

# Run the server #

Change to the top level of the wattdepot distribution you downloaded. Run the server:

```
java -jar wattdepot-server.jar
```

You should see output something like this:

```
Loading Server properties from: /Users/foo/.wattdepot/server/wattdepot-server.properties
logDirString: /Users/foo/.wattdepot/server/logs/
2012/03/29 15:51:36  Starting WattDepot server.
2012/03/29 15:51:36  Host: http://localhost:8182/wattdepot/
2012/03/29 15:51:36  Server Properties:
                wattdepot-server.admin.email = foo@example.com
                wattdepot-server.admin.password = SERIOUSLY-CHANGE-ME
                wattdepot-server.context.root = wattdepot
                wattdepot-server.db.dir = /Users/foo/.wattdepot/server/db
                wattdepot-server.db.impl = org.wattdepot.server.db.derby.DerbyStorageImplementation
                wattdepot-server.db.snapshot = /Users/foo/.wattdepot/server/db-snapshot
                wattdepot-server.gviz.context.root = gviz
                wattdepot-server.gviz.port = 8184
                wattdepot-server.homedir = /Users/foo/.wattdepot/server
                wattdepot-server.hostname = localhost
                wattdepot-server.logging.level = INFO
                wattdepot-server.port = 8182
                wattdepot-server.restlet.logging = false
                wattdepot-server.smtp.host = mail.hawaii.edu
                wattdepot-server.test.admin.email = admin@example.com
                wattdepot-server.test.admin.password = admin@example.com
                wattdepot-server.test.db.dir = /Users/foo/.wattdepot/server/testdb
                wattdepot-server.test.db.snapshot = /Users/foo/.wattdepot/server/testdb-snapshot
                wattdepot-server.test.domain = example.com
                wattdepot-server.test.gviz.port = 8185
                wattdepot-server.test.hostname = localhost
                wattdepot-server.test.install = false
                wattdepot-server.test.port = 8183

2012/03/29 15:51:38  Derby: uninitialized.
2012/03/29 15:51:38  Derby: creating DB in: /Users/foo/.wattdepot/server/db
2012/03/29 15:51:38  Default resource directory /Users/foo/.wattdepot/server/default-resources not found, no default resources loaded.
2012-03-29 15:51:38.606::INFO:  Logging to STDERR via org.mortbay.log.StdErrLog
2012/03/29 15:51:38  Google visualization URL: http://localhost:8184/gviz/
2012/03/29 15:51:38  Maximum Java heap size (MB): 129.957888
2012-03-29 15:51:38.739::INFO:  jetty-1.6.130
2012-03-29 15:51:38.766::INFO:  Started SocketConnector@0.0.0.0:8184
2012/03/29 15:51:38  WattDepot server (Version 1.6.130) now running.
```

You have successfully started the server! You can stop the server by typing Control-C

# Creating users and sources #

Now you will probably want to create some user accounts and Sources to store data in. See CreatingResources for information on how to do that.

# Running the server in production #

Once you are done configuring the server, you will probably want to make sure it runs every time your server boots. Provided with the distribution are example property files for [launchd](http://en.wikipedia.org/wiki/Launchd), the native utility for running daemons on Mac OS X. They can be found in the distribution directory under `src/main/resources/etc/startup-scripts`. Users of other operating systems will need to write their own startup scripts, but can base them on the launchd property files. If you write a startup script for another operating system, please let us know so we can incorporate it!
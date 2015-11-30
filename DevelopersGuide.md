# Introduction #

This page contains instructions on how to check out, build, and test the WattDepot code base.

# Code Check Out #

The WattDepot code is managed with [Git](http://help.github.com/set-up-git-redirect). To check out a local copy, use
```
git clone https://code.google.com/p/wattdepot/
```
or follow the instructions on the [Source](http://code.google.com/p/wattdepot/source/checkout) page.

Add the WattDepot project as a `remote` so you can push and pull from it.
```
git remote add googlecode https://code.google.com/p/wattdepot
```

# Build System #

WattDepot uses [Maven 3](http://maven.apache.org/index.html) as a build system. Maven is configured with the pom.xml file found at the root of the WattDepot source directory. Maven has a number of lifecycle phases - the most useful in this project are clean, compile, test, package, verify, and site.

  * `mvn clean` will delete the target directory.
  * `mvn compile` will compile the project.
  * `mvn test` will compile the project, then run JUnit and QA tests.
    * According to the POM, JUnit tests are run in multiple sections. First all non-database tests are executed with the main storage implementation (currently Derby). After those pass the database tests are executed separately for each storage implementation. Note: the postgres storage implementation requires you to setup the wattdepot-server.properties file in order to supply the wattdepot-server.db.username property.
    * FindBugs
    * Checkstyle
    * PMD
    * JavaDocs are created
  * `mvn package` will compile the project, run the tests, and then create .jar and .zip files for a distribution
    * The .jar files are created from anttasks.build.xml (this functionality hasn't been ported from Ant to Maven)
    * The POM zips the source
    * The POM zips the javadocs
    * The POM zips everything into a distribution
  * `mvn verify` runs the compile, test and package phases and ensures the result is valid
  * `mvn site` executes FindBugs, Checkstyle, PMD, and JavaDocs, then presents the results in an easy to read website (located in target/site)

Rather than executing a whole phase, you can also run an individual goal. For example, to run FindBugs without doing anything else, use
```
mvn findbugs:check
```
The same works for `mvn pmd:check` and `mvn checkstyle:check`. However, note that this does not recompile the project. If you make changes to the source code, you should run `mvn compile` before running any other checks.

If new dependencies are added to the project, they should be added into the POM and possibly into the .jar creation step in anttasks.build.xml. They also need to be added to the Eclipse classpath, which can be accomplished with
```
mvn eclipse:eclipse
```
This ensures that all dependencies listed in the POM are listed in the Eclipse classpath, with relative paths to the Maven repository. This is the best way to add any new dependency to the project because it ensures that it will work for any developer on any system.

Another useful tip is adding properties to Maven calls with the `-D` argument. For example, to run the package phase without running JUnit tests, use
```
mvn package -DskipTests
```
This can be a great time saver if you've just run the test phase and now want to package. These properties can also affect the running of WattDepot. Say you want to test multiple configurations without changing the wattdepot-server.properties file between each test. Try
```
mvn test -Dwattdepot-server.db.impl=org.wattdepot.server.db.postgres.PostgresStorageImplementation
```
compared to
```
mvn test -Dwattdepot-server.db.impl=org.wattdepot.server.db.derby.DerbyStorageImplementation
```
Obviously using the wattdepot-server.properties file is best for long term configuration changes, but when doing individual tests it can be faster to just use Maven.


# Code Check In #
Before pushing a change to the GoogleCode repository, run `mvn verify` to ensure that everything is valid. If that passes, create a commit with a detailed message of the change using
```
git commit -m MESSAGE
```
and then
```
git push googlecode master
```

# Creating a Distribution #
To create a distribution, run `mvn package` and then upload the resulting .zip file to the [Downloads](http://code.google.com/p/wattdepot/downloads) page. Include a summary of the changes in the title and more detailed information in the description. Optionally make the download featured, and remove the Featured tag on the previous download.

Tag the most recent code revision that is included in the distribution. For example, if revision 8fef12173063 finalizes version 2.0.0504, use
```
git tag v2.0.0504 8fef12173063
```
Push the tag to GoogleCode with
```
git push googlecode --tags
```

Add to the VersionHistory page describing the new version and what its changes mean for developers and clients. Include the tag name and a link to the download.

Post to the [Discussion Group](https://groups.google.com/forum/?fromgroups#!forum/wattdepot-users) with a summary of the new revision and a link to the VersionHistory page.
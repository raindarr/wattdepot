# Note #
These instructions no longer work properly due to changes at Heroku and in WattDepot. For now, they are provided as a starting point for technically-knowledgable individuals that want to get WattDepot working on Heroku.

# Introduction #

This document explains how to install the WattDepot server as a Heroku cloud application. Following these instructions will actually result in _two_ Heroku apps: one testing app and one production app. The testing app is optional, but highly recommended. Many of the commands below are duplicated in order to set up both apps.

# Requirements #
  * Create a Heroku account
  * Install Heroku toolbelt
  * Install JDK 1.5 or above (for testing)
  * Install Maven 3 (for testing)

# Setup Heroku #

### Sign up for an account and install Heroku toolbelt ###
Follow Steps 1-3 of the [Heroku Cheat Sheet](https://devcenter.heroku.com/articles/quickstart)
This involves:
  * Signing up with the Heroku service
  * Install the Heroku Toolbelt (provides the "git" and "heroku" commands).
  * Logging in to Heroku.
### Verify your Heroku account by adding a credit card number ###
Note that nothing in these steps actually creates charges, but does require the account to be verified.
  * Sign into [Heroku](http://www.heroku.com) and go to "My Account"
  * Press the "Verify" button in the Billing Info section
  * Enter the billing information to associate with this account

### Create your Heroku app ###

Open the command line and execute the following command to create your testing application:
```sh

heroku create --stack cedar```
A name is given to the app, which will be part of the URL. The app name will be used in the following commands. We'll refer to it here as _TESTAPPNAME_, and your test app URL will be _TESTAPPNAME_.herokuapp.com by default. If you would like to change your _TESTAPPNAME_ you can do so through your online Heroku account, but keep in mind that no two apps can share the same name so pick something unique (for example, "test-wattdepot-hpu").

To create the production app, run the same command:
```sh

heroku create --stack cedar```
You will be given another name, which we will call _APPNAME_. Again, this can be changed through your online Heroku account (for example, "wattdepot-hpu").

### Set up Heroku database ###
Add an add-on shared database:
```sh

heroku addons:add heroku-shared-postgresql --app TESTAPPNAME
heroku addons:add heroku-shared-postgresql --app APPNAME ```
This will tell you the names of your shared databases. For example, HEROKU\_SHARED\_POSTGRESQL\_BLACK. You need these in the next step. Let's refer to them as _TESTDBNAME_ and _DBNAME_.
```sh

heroku pg:promote TESTDBNAME --app TESTAPPNAME
heroku pg:promote DBNAME --app APPNAME```

### Setup config vars ###

A few configuration variables are required to run WattDepot on Heroku.
  * **wattdepot-server.hostname** should be set to the URL of your Heroku application. By default, this is _TESTAPPNAME_.herokuapp.com or _APPNAME_.herokuapp.com
  * **wattdepot-server.heroku** must be set to true
  * **wattdepot-server.admin.email** will be the username for your WattDepot administrator
  * **wattdepot-server.admin.password** will be the password for your WattDepot administrator.
  * **wattdepot-server.context.root** will be the root of your app. For example, if set to "wattdepot" you will access your application via _appname_.herokuapp.com/wattdepot/. This value can be the same for both the test and production apps.

Determine the correct value for each variable and set them via command line:
```sh

heroku config:add wattdepot-server.hostname=TESTAPPNAME.herokuapp.com  wattdepot-server.admin.email=user@example.com wattdepot-server.admin.password=myPassw0rd wattdepot-server.context.root=wattdepot wattdepot-server.heroku=true --app TESTAPPNAME
heroku config:add wattdepot-server.hostname=APPNAME.herokuapp.com  wattdepot-server.admin.email=user@example.com wattdepot-server.admin.password=myPassw0rd wattdepot-server.context.root=wattdepot wattdepot-server.heroku=true --app APPNAME```

This step can be redone if you want to change the variables later, but it requires restarting the Heroku app.

The test app requires a timezone change so that the test cases will pass.

```sh

heroku config:add TZ=Pacific/Honolulu --app TESTAPPNAME```

This config var can be set on the production app if desired, but it is not necessary.

# Push the WattDepot code to your Heroku App #

### Clone the WattDepot repository ###
  * From the command line, change into the directory you want the code in.
```sh

cd WattDepot```
  * Get the code from the repository.
```sh

git clone https://code.google.com/p/wattdepot/```
  * Add a git remote to each of your Heroku apps
```sh

git remote add testheroku git@heroku.com:TESTAPPNAME.git
git remote add heroku git@heroku.com:APPNAME.git```

### Verify that the code is working locally ###
  * Download and Install Maven 3 following the instructions [here](http://maven.apache.org/download.html)
  * Execute the verify task to test the code
```sh

mvn verify```
  * The tests should take about ten minutes to complete. If there are any errors, they need to be fixed before continuing. Check with the WattDepot developers via the Google Group to determine the problem.

### Push code to test app ###
  * Go to VersionHistory to find the tag of the release you want to install (for example, v1.6.130). Push the release to the test app using the tag name. Remember the tag name that you select, as you will need it again for the production app.
```sh

git push testheroku v1.6.130```
  * Enter authentication if requested
  * Heroku will check the files to make sure it can run them. If this does not pass, Heroku won't accept the files. Check with the WattDepot developers via the Google Group to determine the problem.

### Verify that the code is working on Heroku ###
  * In order to test on Heroku, we need to get the Database URL of the test app.
```sh

heroku config --app TESTAPPNAME```
  * Find and copy the value listed after DATABASE\_URL. Paste it into another application (like Notepad or Word) and append `?ssl=true&sslfactory=org.postgresql.ssl.NonValidatingFactory` to the end of it. We'll call this the _DBURL_ It should look something like this:
`postgres://zqgwamfnsv:9qkZEYwp1A7gm_dEYlT@pg36.sharedpg.heroku.com/evening_rain_158?ssl=true&sslfactory=org.postgresql.ssl.NonValidatingFactory`
  * Run the test, passing in the parameters as shown below (including the quotation marks).
```sh

mvn test "-Dwattdepot-server.test.heroku=true" "-Dwattdepot-server.heroku.hostname=TESTAPPNAME.herokuapp.com" "-Dwattdepot-server.heroku.db.url=DBURL"```
  * The test may take around half an hour to run, since it is doing a great deal of work over the network.
  * At the end, make sure it says SUCCESSFUL. If not, there is something wrong with either the code or the Heroku configuration. Only proceed to the next step after the tests pass.
  * You can visit the app at _TESTAPNAME_.herokuapp.com
    * If it says "Not Found" that means the server is running.
    * If it says "Application Error", check the log files to see what went wrong
> > ```sh
heroku logs --app TESTAPPNAME```

### Push code to production app ###
  * Push the code to Heroku, using the same tag name you used for the test app above.
```sh

git push heroku v1.6.130```
  * Enter authentication if requested
  * Heroku will check the files again. At this point everything should be working since we already checked it.

### Visit your WattDepot production server ###
  * Visit _APPNAME_.herokuapp.com
  * It should say "Not Found", meaning that the server is running.
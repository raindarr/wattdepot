# Introduction #

As discussed in the RestApi, there is a hierarchy of resources in WattDepot: Users own Sources, and Sources contain SensorData. This page explains how to create Users, Sources, and SensorData.

Unfortunately, there is currently no user-friendly tool for creating the resources. They must either be created by hand-edited XML, or programmatically through the RestApi.

# Hand-editing XML #

If you are creating a small number of relatively homogenous resources, it is straightforward to write the XML for them. In the WattDepot distribution, there is a directory `src/main/resources/xml/example-resources` that contains example XML for a User and several Sources. You can use these as templates and create your own User(s) and Source(s). The RestApi also has example XML for each resource, and the PropertyDictionary describes the canonical properties used for resources.

Once you have created the XML for your resources, you need to load it into the server. The server will look for default resources in a directory named `default-resources` in the server subdirectory. Each type of resource should be in a separate subdirectory, `users` for Users, `sources` for Sources, and `sensordata` for SensorData. So for example, Sources should be placed in `~/.wattdepot/server/default-resources/sources` (assuming the default server subdirectory name is used). The WattDepot server looks for this directory when it starts up, so either start the server, or if it is already running, shut it down and restart it.

Note that the server will examine the `default-resources` directory each time it starts and try to load any resources it finds. Since the resources placed here are generally intended to be loaded once, the next time the server is restarted it will attempt to recreate the resources and generate error messages. For this reason, you should move the `default-resources` directory aside after you have successfully loaded the resources once, for example by renaming the directory to `default-resources-stored`.

# Loading by the RestApi #

Sources can be created through the RestApi, so depending on your application, you might want to create the resources you need programmatically. This might be appropriate if you always create Sources with a particular name. If you end up writing a general purpose tool for creating resources, we'd be happy to add it to WattDepot.
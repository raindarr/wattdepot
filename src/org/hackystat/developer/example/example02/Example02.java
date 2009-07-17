package org.hackystat.developer.example.example02;

import org.hackystat.sensorbase.client.SensorBaseClient;
import org.hackystat.sensorshell.SensorShellProperties;

/**
 * Illustrates the use of the SensorShellProperties class to retrieve the users local credentials.
 * Requires sensorshell.jar and sensorbaseclient.jar on the classpath.
 * See: http://csdl.ics.hawaii.edu/~johnson/screencasts/Hackystat.Programming.01.mov. 
 * @author Philip Johnson
 *
 */
public class Example02 {  //NOPMD Ignore recommendation to make this class a singleton

  /**
   * Illustrate the use of hte SensorShellProperties class to get user credentials. 
   * @param args Ignored. 
   * @throws Exception If problems occur accessing the public server. 
   */
  
  public static void main(String[] args) throws Exception {
    // Get the contents of the ~/.hackystat/sensorshell/sensorshell.properties file. 
    SensorShellProperties properties = new SensorShellProperties();
    String user = properties.getSensorBaseUser();
    String password = properties.getSensorBasePassword();
    String host = "http://dasha.ics.hawaii.edu:9876/sensorbase";
    // See if the stored credentials are valid with the public sensorbase.
    boolean credentialsOK = SensorBaseClient.isRegistered(host, user, password);
    // Print out the results. 
    System.out.println("Example02: credentials are: " + (credentialsOK ? "OK." : "not OK."));
  }
  
}

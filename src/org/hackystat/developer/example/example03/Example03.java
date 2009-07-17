package org.hackystat.developer.example.example03;

import javax.xml.datatype.XMLGregorianCalendar;

import org.hackystat.dailyprojectdata.client.DailyProjectDataClient;
import org.hackystat.dailyprojectdata.resource.devtime.jaxb.DevTimeDailyProjectData;
import org.hackystat.utilities.tstamp.Tstamp;

/**
 * Illustrate simple use of the DailyProjectDataClient.
 * Requires dailyprojectdata.jar on the classpath.
 * Uses credentials from http://code.google.com/p/hackystat/wiki/HackystatDemo.  
 * Similar to: http://csdl.ics.hawaii.edu/~johnson/screencasts/Hackystat.Programming.02.mov.  
 * @author Philip Johnson
 *
 */
public class Example03 {
  /** The public DPD host used for this example. */
  private static final String host = "http://dasha.ics.hawaii.edu:9877/dailyprojectdata";
  /** The user (from HackystatDemo). */
  private static final String user = "joe.simpleportfolio@hackystat.org";
  /** For password (same as the user for test users-those with a hackystat.org extension). */
  private static final String password = user;
  
  /**
   * Returns the total DevTime associated with the example user on July 1, 2008.
   * @return The total DevTime. 
   * @throws Exception If problems occur retrieving the data. 
   */
  public int getDevTime() throws Exception {
    DailyProjectDataClient dpdClient = new DailyProjectDataClient(host, user, password);
    dpdClient.authenticate();
    XMLGregorianCalendar startTime = Tstamp.makeTimestamp("2008-07-01");
    DevTimeDailyProjectData devtime = dpdClient.getDevTime(user, "Default", startTime);
    return devtime.getTotalDevTime().intValue();
  }
  
  /**
   * An example main method to be invoked by the example03.jar file. 
   * @param args Ignored. 
   * @throws Exception If problems occur accessing the server. 
   */
  public static void main(String[] args) throws Exception {
    Example03 example = new Example03();
    System.out.println("Example03: DevTime is: " + example.getDevTime());
  }

}

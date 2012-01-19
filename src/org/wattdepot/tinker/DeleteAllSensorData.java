package org.wattdepot.tinker;

import java.util.Scanner;
import org.wattdepot.client.WattDepotClient;
import org.wattdepot.resource.source.summary.jaxb.SourceSummary;

public class DeleteAllSensorData {

  public static String hostUri;
  public static String username;
  public static String password;
  public static String source;

  /**
   * Quick and dirty hack to delete all SensorData for a source.
   * 
   * @param args
   * @throws Exception If things go wrong
   */
  public static void main(String[] args) throws Exception {
    hostUri = args[0];
    username = args[1];
    password = args[2];
    source = args[3];
    WattDepotClient client = new WattDepotClient(hostUri, username, password);

    SourceSummary summary = client.getSourceSummary(source);
    System.out.println(summary);

    System.out.println("Warning, this will delete all SensorData for Source \"" + source + "\"");
    System.out.print("Are you sure you want to do this? (yes/no): ");
    Scanner scanner = new Scanner(System.in);
    if (scanner.nextLine().equals("yes")) {
      client.deleteAllSensorData(source);
      summary = client.getSourceSummary(source);
      System.out.println(summary);
    }
  }
}

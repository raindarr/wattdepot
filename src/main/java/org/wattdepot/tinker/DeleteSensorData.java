package org.wattdepot.tinker;

import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;
import org.wattdepot.client.WattDepotClient;
import org.wattdepot.resource.sensordata.jaxb.SensorDataIndex;
import org.wattdepot.resource.sensordata.jaxb.SensorDataRef;
import org.wattdepot.resource.source.summary.jaxb.SourceSummary;
import org.wattdepot.util.tstamp.Tstamp;

public class DeleteSensorData {

  public static String hostUri;
  public static String username;
  public static String password;
  public static String source;

  /**
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

    XMLGregorianCalendar startDate = Tstamp.makeTimestamp("2011-10-12T16:08:45.543-10:00");
    XMLGregorianCalendar endDate = Tstamp.makeTimestamp("2011-10-14T14:19:14.040-10:00");

    SensorDataIndex index = client.getSensorDataIndex(source, startDate, endDate);
    List<SensorDataRef> refs = index.getSensorDataRef();
    System.out.println("SensorData resources from " + startDate + " to " + endDate + ": "
        + refs.size());

    for (SensorDataRef ref : refs) {
      client.deleteSensorData(source, ref.getTimestamp());
    }
    summary = client.getSourceSummary(source);
    System.out.println(summary);
  }
}

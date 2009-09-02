package org.wattdepot.server.db.memory;

import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.resource.sensordata.jaxb.SensorDataRef;
import org.wattdepot.server.Server;

/**
 * Some static utility methods useful for the MemoryStorageImplementation.
 * 
 * @author Robert Brewer
 */
public class MemoryStorageUtil {

  /**
   * Creates a SensorDataRef object from a SensorData object. The Server argument is required to
   * build the URI in the SensorDataRef pointing to the full SensorData resource.
   * 
   * @param data The SensorData to build the SensorDataRef from.
   * @param server The Server where the SensorData is located.
   * @return The new SensorDataRef object.
   */
  public static SensorDataRef makeSensorDataRef(SensorData data, Server server) {
    SensorDataRef ref = new SensorDataRef();
    ref.setTimestamp(data.getTimestamp());
    ref.setTool(data.getTool());
    ref.setSource(data.getSource());
    ref.setHref(server.getHostName() + Server.SOURCES_URI + "/" + Server.SENSORDATA_URI + "/"
        + data.getTimestamp().toString());
    return ref;
  }
}

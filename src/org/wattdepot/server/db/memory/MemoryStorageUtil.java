package org.wattdepot.server.db.memory;

import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.resource.sensordata.jaxb.SensorDataRef;
import org.wattdepot.resource.source.jaxb.Source;
import org.wattdepot.resource.source.jaxb.SourceRef;
import org.wattdepot.server.Server;

/**
 * Some static utility methods useful for the MemoryStorageImplementation.
 * 
 * @author Robert Brewer
 */
public class MemoryStorageUtil {

  /**
   * Creates a SourceRef object from a Source object. The Server argument is required to build the
   * URI in the SourceRef pointing to the full Source resource.
   * 
   * @param source The Source to build the SourceRef from.
   * @param server The Server where the Source is located.
   * @return The new SourceRef object.
   */
  public static SourceRef makeSourceRef(Source source, Server server) {
    SourceRef ref = new SourceRef();
    ref.setName(source.getName());
    ref.setOwner(source.getOwner());
    ref.setPublic(source.isPublic());
    ref.setVirtual(source.isVirtual());
    ref.setCoordinates(source.getCoordinates());
    ref.setLocation(source.getLocation());
    ref.setDescription(source.getDescription());
    ref.setHref(server.getHostName() + Server.SOURCES_URI + "/" + source.getName());
    return ref;
  }

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

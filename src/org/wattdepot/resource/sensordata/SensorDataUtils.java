package org.wattdepot.resource.sensordata;

import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;
import org.hackystat.utilities.tstamp.Tstamp;
import org.wattdepot.resource.sensordata.jaxb.Properties;
import org.wattdepot.resource.sensordata.jaxb.Property;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.resource.sensordata.jaxb.SensorDataRef;
import org.wattdepot.resource.source.jaxb.Source;
import org.wattdepot.server.Server;

/**
 * Some convenience methods for creating SensorData and associated resources programmatically.
 * 
 * @author Robert Brewer
 */
/**
 * 
 * @author Robert Brewer
 */
public class SensorDataUtils {

  /**
   * Returns a new SensorData object with the provided parameters. Needs to be kept up to date with
   * any changes to the schema, which is bogus.
   * 
   * @param tstamp The timestamp for the SensorData.
   * @param tool The tool used to create the SensorData.
   * @param source The URI of the Source this SensorData belongs to.
   * @param props The properties for the SensorData.
   * @return The freshly created SensorData object.
   */
  public static SensorData makeSensorData(XMLGregorianCalendar tstamp, String tool, String source,
      Properties props) {
    SensorData data = new SensorData();
    data.setTimestamp(tstamp);
    data.setTool(tool);
    data.setSource(source);
    data.setProperties(props);
    return data;
  }

  /**
   * Returns a new SensorData Property object with the provided key and value.
   * 
   * @param key The key.
   * @param value The value.
   * @return The freshly created Property object.
   */
  public static Property makeSensorDataProperty(String key, String value) {
    Property prop = new Property();
    prop.setKey(key);
    prop.setValue(value);
    return prop;
  }

  /**
   * Creates a SensorDataRef object from a SensorData object. The Server argument is required to
   * build the URI in the SensorDataRef pointing to the full SensorData resource. Needs to be kept
   * up to date with any changes to the schema, which is bogus.
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
    ref.setHref(data.getSource() + "/" + Server.SENSORDATA_URI + "/"
        + data.getTimestamp().toString());
    return ref;
  }

  /**
   * Creates a SensorDataRef object from the given parameters. Note that no parameter is needed for
   * the Href field, as it can be constructed from the source and timestamp.
   *
   * @param timestamp The timestamp for the new object.
   * @param tool The tool for the new object.
   * @param source The URI of the source for the new object.
   * @return The new SensorDataRef object.
   */
  public static SensorDataRef makeSensorDataRef(XMLGregorianCalendar timestamp, String tool,
      String source) {
    SensorDataRef ref = new SensorDataRef();
    ref.setTimestamp(timestamp);
    ref.setTool(tool);
    ref.setSource(source);
    ref.setHref(source + "/" + Server.SENSORDATA_URI + "/" + timestamp.toXMLFormat());
    return ref;
  }

  /**
   * Determines if the subset of information in a SensorDataRef is equal to particular SensorData
   * object. Note that only the final segment of the href field of the SensorDataRef is compared to
   * the Source object, as the SensorData object does not contain its own URI. Thus if the
   * SensorDataRef was from a different server than the SensorData object, this test would return
   * true even though the SensorDataRef points to a different copy of this SensorData object.
   * 
   * @param ref The SensorDataRef to be compared.
   * @param data The SensorData to be compared.
   * @return True if all the fields in the SensorDataRef correspond to the same fields in the
   * SensorData.
   */
  public static boolean sensorDataRefEqualsSensorData(SensorDataRef ref, SensorData data) {
    XMLGregorianCalendar hrefTimestamp;
    try {
      hrefTimestamp =
          Tstamp.makeTimestamp(ref.getHref().substring(ref.getHref().lastIndexOf('/') + 1));
    }
    catch (Exception e) {
      // If the SourceRef has a bad timestamp, must be hosed so return false
      return false;
    }

    return (ref.getTimestamp().equals(data.getTimestamp())
        && (ref.getTool().equals(data.getTool())) && (ref.getSource().equals(data.getSource())) && (data
        .getTimestamp().equals(hrefTimestamp)));
  }

  /**
   * Compares a List of SensorDataRef to a List of SensorData.
   * 
   * @param retrievedRefs The List of SensorDataRef.
   * @param origData The List of SensorData.
   * @return true if every SensorData object has a matching SensorDataRef.
   */
  public static boolean compareSensorDataRefsToSensorDatas(List<SensorDataRef> retrievedRefs,
      List<SensorData> origData) {
    if (retrievedRefs.size() != origData.size()) {
      return false;
    }
    for (SensorDataRef ref : retrievedRefs) {
      int found = 0;
      for (SensorData data : origData) {
        if (sensorDataRefEqualsSensorData(ref, data)) {
          found++;
        }
      }
      if (found != 1) {
        return false;
      }
    }
    return true;
  }

  /**
   * Given a SensorData object and the Server it belongs to, returns the URI to that SensorData
   * resource.
   * 
   * @param data The SensorData object under consideration.
   * @param source The Source the SensorData belongs to.
   * @param server The Server user belongs to.
   * @return The URI to the SensorData resource corresponding to the given SensorData object.
   */
  public static String sensorDataToUri(SensorData data, Source source, Server server) {
    return server.getHostName() + Server.SOURCES_URI + "/" + source.getName() + "/"
        + Server.SENSORDATA_URI + data.getTimestamp().toString();
  }
}

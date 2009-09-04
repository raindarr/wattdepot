package org.wattdepot.resource.source;

import org.wattdepot.resource.source.jaxb.Properties;
import org.wattdepot.resource.source.jaxb.Property;
import org.wattdepot.resource.source.jaxb.Source;
import org.wattdepot.resource.source.jaxb.SourceRef;
import org.wattdepot.server.Server;

/**
 * Some convenience methods for creating Source and associated resources programmatically.
 * 
 * @author Robert Brewer
 */
public class SourceUtils {

  /**
   * Returns a new Source object with the provided parameters. Needs to be kept up to date with any
   * changes to the schema, which is bogus.
   * 
   * @param name The name for the Source.
   * @param owner The owner URI for the Source.
   * @param publicp Whether the Source is public.
   * @param virtualp Whether the Source is virtual.
   * @param coordinates The coordinates for the Source.
   * @param location The location for the Source.
   * @param description The description of the Source.
   * @param props The properties for the Source.
   * @return The freshly created Source object.
   */
  public static Source makeSource(String name, String owner, boolean publicp, boolean virtualp,
      String coordinates, String location, String description, Properties props) {
    Source source = new Source();
    source.setName(name);
    source.setOwner(owner);
    source.setPublic(publicp);
    source.setVirtual(virtualp);
    source.setCoordinates(coordinates);
    source.setLocation(location);
    source.setDescription(description);
    source.setProperties(props);
    return source;
  }

  /**
   * Returns a new Source Property object with the provided key and value.
   * 
   * @param key The key.
   * @param value The value.
   * @return The freshly created Property object.
   */
  public static Property makeSourceProperty(String key, String value) {
    Property prop = new Property();
    prop.setKey(key);
    prop.setValue(value);
    return prop;
  }

  /**
   * Creates a SourceRef object from a Source object. The Server argument is required to build the
   * URI in the SourceRef pointing to the full Source resource.
   * 
   * @param source The Source to build the SourceRef from.
   * @param server The Server where the Source is located.
   * @return The new SourceRef object.
   */
  public static SourceRef makeSourceRef(Source source, Server server) {
    return makeSourceRef(source, server.getHostName() + Server.SOURCES_URI + "/"
        + source.getName());
  }

  /**
   * Creates a SourceRef object from a Source object using the provided URI for the SourceRef
   * pointing to the full Source resource. Needs to be kept up to date with any changes to the
   * schema, which is bogus.
   * 
   * @param source The Source to build the SourceRef from.
   * @param uri The URI where the Source is located.
   * @return The new SourceRef object.
   */
  public static SourceRef makeSourceRef(Source source, String uri) {
    SourceRef ref = new SourceRef();
    ref.setName(source.getName());
    ref.setOwner(source.getOwner());
    ref.setPublic(source.isPublic());
    ref.setVirtual(source.isVirtual());
    ref.setCoordinates(source.getCoordinates());
    ref.setLocation(source.getLocation());
    ref.setDescription(source.getDescription());
    ref.setHref(uri);
    return ref;
  }

  /**
   * Determines if the subset of information in a SourceRef is equal to particular Source object.
   * Note that only the final segment of the href field of the SourceRef is compared to the Source
   * object, as the Source object does not contain its own URI. Thus if the SourceRef was from a
   * different server than the Source object, this test would return true even though the SourceRef
   * points to a different copy of this Source object.
   * 
   * @param ref The SourceRef to be compared.
   * @param source The Source to be compared.
   * @return True if all the fields in the SourceRef correspond to the same fields in the Source
   */
  public static boolean sourceRefEqualsSource(SourceRef ref, Source source) {
    String hrefSourceName = ref.getHref().substring(ref.getHref().lastIndexOf('/') + 1);

    return (ref.getName().equals(source.getName()) && (ref.getOwner().equals(source.getOwner()))
        && (ref.isPublic() == source.isPublic()) && (ref.isVirtual() == source.isVirtual())
        && (ref.getCoordinates().equals(source.getCoordinates()))
        && (ref.getLocation().equals(source.getLocation()))
        && (ref.getDescription().equals(source.getDescription())) && (source.getName()
        .equals(hrefSourceName)));
  }

  /**
   * Given a Source object and the Server it belongs to, returns the URI to that Source resource.
   * 
   * @param source The Source object under consideration.
   * @param server The Server user belongs to.
   * @return The URI to the Source resource corresponding to the given Source.
   */
  public static String sourceToUri(Source source, Server server) {
    return server.getHostName() + Server.SOURCES_URI + "/" + source.getName();
  }
}

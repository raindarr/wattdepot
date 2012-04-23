package org.wattdepot.tinker;

import static org.wattdepot.server.ServerProperties.DB_IMPL_KEY;
import java.util.Date;
import javax.xml.datatype.XMLGregorianCalendar;
import org.wattdepot.resource.property.jaxb.Properties;
import org.wattdepot.resource.property.jaxb.Property;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.resource.source.jaxb.Source;
import org.wattdepot.resource.user.jaxb.User;
import org.wattdepot.server.Server;
import org.wattdepot.server.ServerProperties;
import org.wattdepot.server.db.DbManager;
import org.wattdepot.util.tstamp.Tstamp;

public class CacheSpeedTest {

  public CacheSpeedTest() throws Exception {
    Server server = Server.newTestInstance();
    ServerProperties props = server.getServerProperties();
    final DbManager manager = new DbManager(server, props.get(DB_IMPL_KEY), true);
    String owner = props.get(ServerProperties.ADMIN_EMAIL_KEY);

    Source source1 = new Source("source1", User.userToUri(owner, server), true);
    source1.addProperty(new Property(Source.CACHE_CHECKPOINT_INTERVAL, "15"));
    source1.addProperty(new Property(Source.CACHE_WINDOW_LENGTH, "30"));
    manager.storeSource(source1);
    Source source2 = new Source("source2", User.userToUri(owner, server), true);
    manager.storeSource(source2);

    XMLGregorianCalendar t1 = Tstamp.incrementMinutes(Tstamp.makeTimestamp(), -30);
    XMLGregorianCalendar t2 = Tstamp.incrementMinutes(Tstamp.makeTimestamp(), -25);
    XMLGregorianCalendar t3 = Tstamp.incrementMinutes(Tstamp.makeTimestamp(), -20);
    XMLGregorianCalendar t4 = Tstamp.incrementMinutes(Tstamp.makeTimestamp(), -15);
    XMLGregorianCalendar t5 = Tstamp.incrementMinutes(Tstamp.makeTimestamp(), -10);
    XMLGregorianCalendar t6 = Tstamp.incrementMinutes(Tstamp.makeTimestamp(), -5);
    XMLGregorianCalendar t7 = Tstamp.incrementMinutes(Tstamp.makeTimestamp(), 0);

    // Make some totally bogus data
    Properties p = new Properties();
    p.getProperty().add(new Property(SensorData.ENERGY_GENERATED_TO_DATE, "100"));
    p.getProperty().add(new Property(SensorData.POWER_GENERATED, "100"));

    // Insert data for cached source
    // Skip cache for data that is more than 15 minutes old to be realistic
    Long before = new Date().getTime();
    manager.storeSensorDataNoCache(new SensorData(t1, "CacheSpeedTest", Source.sourceToUri(
        source1.getName(), server), p));
    manager.storeSensorDataNoCache(new SensorData(t2, "CacheSpeedTest", Source.sourceToUri(
        source1.getName(), server), p));
    manager.storeSensorDataNoCache(new SensorData(t3, "CacheSpeedTest", Source.sourceToUri(
        source1.getName(), server), p));
    manager.storeSensorData(
        new SensorData(t4, "CacheSpeedTest", Source.sourceToUri(source1.getName(), server), p),
        source1);
    manager.storeSensorData(
        new SensorData(t5, "CacheSpeedTest", Source.sourceToUri(source1.getName(), server), p),
        source1);
    manager.storeSensorData(
        new SensorData(t6, "CacheSpeedTest", Source.sourceToUri(source1.getName(), server), p),
        source1);
    manager.storeSensorData(
        new SensorData(t7, "CacheSpeedTest", Source.sourceToUri(source1.getName(), server), p),
        source1);
    Long after = new Date().getTime();
    System.out.println("Insert took " + (after - before) + "milliseconds with cache");

    // Insert data for non-cached source
    before = new Date().getTime();
    manager.storeSensorDataNoCache(new SensorData(t1, "CacheSpeedTest", Source.sourceToUri(
        source2.getName(), server), p));
    manager.storeSensorDataNoCache(new SensorData(t2, "CacheSpeedTest", Source.sourceToUri(
        source2.getName(), server), p));
    manager.storeSensorDataNoCache(new SensorData(t3, "CacheSpeedTest", Source.sourceToUri(
        source2.getName(), server), p));
    manager.storeSensorDataNoCache(new SensorData(t4, "CacheSpeedTest", Source.sourceToUri(
        source2.getName(), server), p));
    manager.storeSensorDataNoCache(new SensorData(t5, "CacheSpeedTest", Source.sourceToUri(
        source2.getName(), server), p));
    manager.storeSensorDataNoCache(new SensorData(t6, "CacheSpeedTest", Source.sourceToUri(
        source2.getName(), server), p));
    manager.storeSensorDataNoCache(new SensorData(t7, "CacheSpeedTest", Source.sourceToUri(
        source1.getName(), server), p));
    after = new Date().getTime();
    System.out.println("Insert took " + (after - before) + "milliseconds without cache");

    // Get the interpolated data back
    before = new Date().getTime();
    manager.getCarbon(source1, Tstamp.incrementMinutes(t2, 1), Tstamp.incrementMinutes(t6, 1), 3);
    manager.getEnergy(source1, Tstamp.incrementMinutes(t2, 1), Tstamp.incrementMinutes(t6, 1), 3);
    manager.getPower(source1, Tstamp.incrementMinutes(t6, 2));
    after = new Date().getTime();
    System.out.println("Gets took " + (after - before) + "milliseconds with cache");

    before = new Date().getTime();
    manager.getCarbon(source2, Tstamp.incrementMinutes(t2, 1), Tstamp.incrementMinutes(t6, 1), 3);
    manager.getEnergy(source2, Tstamp.incrementMinutes(t2, 1), Tstamp.incrementMinutes(t6, 1), 3);
    manager.getPower(source2, Tstamp.incrementMinutes(t6, 2));
    after = new Date().getTime();
    System.out.println("Gets took " + (after - before) + "milliseconds without cache");

    server.shutdown();
  }

  public static void main(String[] args) throws Exception {
    new CacheSpeedTest();
  }
}

package org.wattdepot.test.functional;

import static org.junit.Assert.assertEquals;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.XMLGregorianCalendar;
import org.hackystat.utilities.tstamp.Tstamp;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wattdepot.client.WattDepotClient;
import org.wattdepot.datainput.OscarRowParser;
import org.wattdepot.datainput.RowParseException;
import org.wattdepot.resource.sensordata.jaxb.Property;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.resource.source.jaxb.Source;
import org.wattdepot.server.db.DbException;
import org.wattdepot.server.db.DbManager;
import org.wattdepot.test.ServerTestHelper;
import org.wattdepot.util.UriUtils;

/**
 * Tests WattDepot server loaded with Oscar data for functionality to be used in a programming
 * assignment. Thus it tests functionality desired by a user of the system, so I'm calling it a
 * functional test.
 * 
 * @author Robert Brewer
 */
public class TestOscarFunctionality extends ServerTestHelper {

  /**
   * Programmatically loads some Oscar data needed by the rest of the tests.
   * 
   * @throws RowParseException If there is a problem parsing the static data (should never happen).
   * @throws JAXBException If there are problems marshalling the XML dat
   * @throws DbException If there is a problem storing the test data in the database
   * 
   */
  @SuppressWarnings("PMD.AvoidDuplicateLiterals")
  // PMD going wild on the test data
  @BeforeClass
  public static void loadOscarData() throws RowParseException, JAXBException, DbException {
    // Some sim data from Oscar to load into the system before tests
    String[] oscarRows =
        { "2009-10-12T00:00:00-1000,SIM_HPOWER,46,5,BASELOAD",
            "2009-10-12T00:00:00-1000,SIM_KAHE_6,135,8,BASELOAD",
            "2009-10-12T00:00:00-1000,SIM_KAHE_5,88,4,BASELOAD",
            "2009-10-12T00:00:00-1000,SIM_KAHE_4,88,4,BASELOAD",
            "2009-10-12T00:00:00-1000,SIM_KAHE_3,88,4,BASELOAD",
            "2009-10-12T00:00:00-1000,SIM_KAHE_2,55,4,BASELOAD",
            "2009-10-12T00:00:00-1000,SIM_KAHE_1,0,4,BASELOAD",
            "2009-10-12T00:00:00-1000,SIM_KALAELOA,0,5,BASELOAD",
            "2009-10-12T00:00:00-1000,SIM_WAIAU_5,0,0,CYCLING",
            "2009-10-12T00:00:00-1000,SIM_WAIAU_6,0,0,CYCLING",
            "2009-10-12T00:00:00-1000,SIM_HONOLULU_8,0,0,CYCLING",
            "2009-10-12T00:00:00-1000,SIM_HONOLULU_9,0,0,CYCLING",
            "2009-10-12T00:00:00-1000,SIM_WAIAU_9,0,0,PEAKING",
            "2009-10-12T00:00:00-1000,SIM_WAIAU_10,0,0,PEAKING",
            "2009-10-12T00:15:00-1000,SIM_HPOWER,46,5,BASELOAD",
            "2009-10-12T00:15:00-1000,SIM_KAHE_6,135,8,BASELOAD",
            "2009-10-12T00:15:00-1000,SIM_KAHE_5,88,4,BASELOAD",
            "2009-10-12T00:15:00-1000,SIM_KAHE_4,88,4,BASELOAD",
            "2009-10-12T00:15:00-1000,SIM_KAHE_3,88,4,BASELOAD",
            "2009-10-12T00:15:00-1000,SIM_KAHE_2,64,4,BASELOAD",
            "2009-10-12T00:15:00-1000,SIM_KAHE_1,0,4,BASELOAD",
            "2009-10-12T00:15:00-1000,SIM_KALAELOA,0,5,BASELOAD",
            "2009-10-12T00:15:00-1000,SIM_WAIAU_5,0,0,CYCLING",
            "2009-10-12T00:15:00-1000,SIM_WAIAU_6,0,0,CYCLING",
            "2009-10-12T00:15:00-1000,SIM_HONOLULU_8,0,0,CYCLING",
            "2009-10-12T00:15:00-1000,SIM_HONOLULU_9,0,0,CYCLING",
            "2009-10-12T00:15:00-1000,SIM_WAIAU_9,0,0,PEAKING",
            "2009-10-12T00:15:00-1000,SIM_WAIAU_10,0,0,PEAKING",
            "2009-10-12T00:30:00-1000,SIM_HPOWER,46,5,BASELOAD",
            "2009-10-12T00:30:00-1000,SIM_KAHE_6,135,8,BASELOAD",
            "2009-10-12T00:30:00-1000,SIM_KAHE_5,88,4,BASELOAD",
            "2009-10-12T00:30:00-1000,SIM_KAHE_4,88,4,BASELOAD",
            "2009-10-12T00:30:00-1000,SIM_KAHE_3,88,4,BASELOAD",
            "2009-10-12T00:30:00-1000,SIM_KAHE_2,50,4,BASELOAD",
            "2009-10-12T00:30:00-1000,SIM_KAHE_1,0,4,BASELOAD",
            "2009-10-12T00:30:00-1000,SIM_KALAELOA,0,5,BASELOAD",
            "2009-10-12T00:30:00-1000,SIM_WAIAU_5,0,0,CYCLING",
            "2009-10-12T00:30:00-1000,SIM_WAIAU_6,0,0,CYCLING",
            "2009-10-12T00:30:00-1000,SIM_HONOLULU_8,0,0,CYCLING",
            "2009-10-12T00:30:00-1000,SIM_HONOLULU_9,0,0,CYCLING",
            "2009-10-12T00:30:00-1000,SIM_WAIAU_9,0,0,PEAKING",
            "2009-10-12T00:30:00-1000,SIM_WAIAU_10,0,0,PEAKING" };
    String[] sourceXmlStrings =
        {
            "<?xml version=\"1.0\"?>"
                + "<Source>"
                + "<Name>SIM_HPOWER</Name>"
                + "<Owner>http://server.wattdepot.org:1234/wattdepot/users/oscar@wattdepot.org</Owner>"
                + "<Public>true</Public>"
                + "<Virtual>false</Virtual>"
                + "<Coordinates>0,0,0</Coordinates>"
                + "<Location>To be looked up later</Location>"
                + "<Description>HPOWER is an independent power producer on Oahu's grid that uses municipal waste as its fuel.</Description>"
                + "<Properties>" + " <Property>" + "  <Key>carbonIntensity</Key>"
                + "  <Value>150</Value>" + " </Property>" + "</Properties>" + "</Source>",
            "<?xml version=\"1.0\"?>"
                + "<Source>"
                + "<Name>SIM_KAHE_1</Name>"
                + "<Owner>http://server.wattdepot.org:1234/wattdepot/users/oscar@wattdepot.org</Owner>"
                + "<Public>true</Public>"
                + "<Virtual>false</Virtual>"
                + "<Coordinates>0,0,0</Coordinates>"
                + "<Location>To be looked up later</Location>"
                + "<Description>Kahe 1 is a HECO plant on Oahu's grid that uses LSFO as its fuel.</Description>"
                + "<Properties>" + " <Property>" + "  <Key>carbonIntensity</Key>"
                + "  <Value>1744</Value>" + " </Property>" + "</Properties>" + "</Source>",
            "<?xml version=\"1.0\"?>"
                + "<Source>"
                + "<Name>SIM_KAHE_2</Name>"
                + "<Owner>http://server.wattdepot.org:1234/wattdepot/users/oscar@wattdepot.org</Owner>"
                + "<Public>true</Public>"
                + "<Virtual>false</Virtual>"
                + "<Coordinates>0,0,0</Coordinates>"
                + "<Location>To be looked up later</Location>"
                + "<Description>Kahe 2 is a HECO plant on Oahu's grid that uses LSFO as its fuel.</Description>"
                + "<Properties>" + " <Property>" + "  <Key>carbonIntensity</Key>"
                + "  <Value>1744</Value>" + " </Property>" + "</Properties>" + "</Source>",
            "<?xml version=\"1.0\"?>"
                + "<Source>"
                + "<Name>SIM_KAHE_3</Name>"
                + "<Owner>http://server.wattdepot.org:1234/wattdepot/users/oscar@wattdepot.org</Owner>"
                + "<Public>true</Public>"
                + "<Virtual>false</Virtual>"
                + "<Coordinates>0,0,0</Coordinates>"
                + "<Location>To be looked up later</Location>"
                + "<Description>Kahe 3 is a HECO plant on Oahu's grid that uses LSFO as its fuel.</Description>"
                + "<Properties>" + " <Property>" + "  <Key>carbonIntensity</Key>"
                + "  <Value>1744</Value>" + " </Property>" + "</Properties>" + "</Source>",
            "<?xml version=\"1.0\"?>"
                + "<Source>"
                + "<Name>SIM_KAHE_4</Name>"
                + "<Owner>http://server.wattdepot.org:1234/wattdepot/users/oscar@wattdepot.org</Owner>"
                + "<Public>true</Public>"
                + "<Virtual>false</Virtual>"
                + "<Coordinates>0,0,0</Coordinates>"
                + "<Location>To be looked up later</Location>"
                + "<Description>Kahe 4 is a HECO plant on Oahu's grid that uses LSFO as its fuel.</Description>"
                + "<Properties>" + " <Property>" + "  <Key>carbonIntensity</Key>"
                + "  <Value>1744</Value>" + " </Property>" + "</Properties>" + "</Source>",
            "<?xml version=\"1.0\"?>"
                + "<Source>"
                + "<Name>SIM_KAHE_5</Name>"
                + "<Owner>http://server.wattdepot.org:1234/wattdepot/users/oscar@wattdepot.org</Owner>"
                + "<Public>true</Public>"
                + "<Virtual>false</Virtual>"
                + "<Coordinates>0,0,0</Coordinates>"
                + "<Location>To be looked up later</Location>"
                + "<Description>Kahe 5 is a HECO plant on Oahu's grid that uses LSFO as its fuel.</Description>"
                + "<Properties>" + " <Property>" + "  <Key>carbonIntensity</Key>"
                + "  <Value>1744</Value>" + " </Property>" + "</Properties>" + "</Source>",
            "<?xml version=\"1.0\"?>"
                + "<Source>"
                + "<Name>SIM_KAHE_6</Name>"
                + "<Owner>http://server.wattdepot.org:1234/wattdepot/users/oscar@wattdepot.org</Owner>"
                + "<Public>true</Public>"
                + "<Virtual>false</Virtual>"
                + "<Coordinates>0,0,0</Coordinates>"
                + "<Location>To be looked up later</Location>"
                + "<Description>Kahe 6 is a HECO plant on Oahu's grid that uses LSFO as its fuel.</Description>"
                + "<Properties>" + " <Property>" + "  <Key>carbonIntensity</Key>"
                + "  <Value>1744</Value>" + " </Property>" + "</Properties>" + "</Source>",
            "<?xml version=\"1.0\"?>"
                + "<Source>"
                + "<Name>SIM_KALAELOA</Name>"
                + "<Owner>http://server.wattdepot.org:1234/wattdepot/users/oscar@wattdepot.org</Owner>"
                + "<Public>true</Public>"
                + "<Virtual>false</Virtual>"
                + "<Coordinates>0,0,0</Coordinates>"
                + "<Location>To be looked up later</Location>"
                + "<Description>Kalaeloa is an independent power producer on Oahu's grid that uses LSFO as its fuel.</Description>"
                + "<Properties>" + " <Property>" + "  <Key>carbonIntensity</Key>"
                + "  <Value>2050</Value>" + " </Property>" + "</Properties>" + "</Source>",
            "<?xml version=\"1.0\"?>"
                + "<Source>"
                + "<Name>SIM_WAIAU_5</Name>"
                + "<Owner>http://server.wattdepot.org:1234/wattdepot/users/oscar@wattdepot.org</Owner>"
                + "<Public>true</Public>"
                + "<Virtual>false</Virtual>"
                + "<Coordinates>0,0,0</Coordinates>"
                + "<Location>To be looked up later</Location>"
                + "<Description>Waiau 5 is a HECO plant on Oahu's grid that uses LSFO as its fuel.</Description>"
                + "<Properties>" + " <Property>" + "  <Key>carbonIntensity</Key>"
                + "  <Value>1800</Value>" + " </Property>" + "</Properties>" + "</Source>",
            "<?xml version=\"1.0\"?>"
                + "<Source>"
                + "<Name>SIM_WAIAU_6</Name>"
                + "<Owner>http://server.wattdepot.org:1234/wattdepot/users/oscar@wattdepot.org</Owner>"
                + "<Public>true</Public>"
                + "<Virtual>false</Virtual>"
                + "<Coordinates>0,0,0</Coordinates>"
                + "<Location>To be looked up later</Location>"
                + "<Description>Waiau 6 is a HECO plant on Oahu's grid that uses LSFO as its fuel.</Description>"
                + "<Properties>" + " <Property>" + "  <Key>carbonIntensity</Key>"
                + "  <Value>1800</Value>" + " </Property>" + "</Properties>" + "</Source>",
            "<?xml version=\"1.0\"?>"
                + "<Source>"
                + "<Name>SIM_HONOLULU_8</Name>"
                + "<Owner>http://server.wattdepot.org:1234/wattdepot/users/oscar@wattdepot.org</Owner>"
                + "<Public>true</Public>"
                + "<Virtual>false</Virtual>"
                + "<Coordinates>0,0,0</Coordinates>"
                + "<Location>To be looked up later</Location>"
                + "<Description>Honolulu 8 is a HECO plant on Oahu's grid that uses LSFO as its fuel.</Description>"
                + "<Properties>" + " <Property>" + "  <Key>carbonIntensity</Key>"
                + "  <Value>2240</Value>" + " </Property>" + "</Properties>" + "</Source>",
            "<?xml version=\"1.0\"?>"
                + "<Source>"
                + "<Name>SIM_HONOLULU_9</Name>"
                + "<Owner>http://server.wattdepot.org:1234/wattdepot/users/oscar@wattdepot.org</Owner>"
                + "<Public>true</Public>"
                + "<Virtual>false</Virtual>"
                + "<Coordinates>0,0,0</Coordinates>"
                + "<Location>To be looked up later</Location>"
                + "<Description>Honolulu 9 is a HECO plant on Oahu's grid that uses LSFO as its fuel.</Description>"
                + "<Properties>" + " <Property>" + "  <Key>carbonIntensity</Key>"
                + "  <Value>2240</Value>" + " </Property>" + "</Properties>" + "</Source>",
            "<?xml version=\"1.0\"?>"
                + "<Source>"
                + "<Name>SIM_WAIAU_9</Name>"
                + "<Owner>http://server.wattdepot.org:1234/wattdepot/users/oscar@wattdepot.org</Owner>"
                + "<Public>true</Public>"
                + "<Virtual>false</Virtual>"
                + "<Coordinates>0,0,0</Coordinates>"
                + "<Location>To be looked up later</Location>"
                + "<Description>Waiau 9 is a HECO plant on Oahu's grid that uses diesel as its fuel.</Description>"
                + "<Properties>" + " <Property>" + "  <Key>carbonIntensity</Key>"
                + "  <Value>2400</Value>" + " </Property>" + "</Properties>" + "</Source>",
            "<?xml version=\"1.0\"?>"
                + "<Source>"
                + "<Name>SIM_WAIAU_10</Name>"
                + "<Owner>http://server.wattdepot.org:1234/wattdepot/users/oscar@wattdepot.org</Owner>"
                + "<Public>true</Public>"
                + "<Virtual>false</Virtual>"
                + "<Coordinates>0,0,0</Coordinates>"
                + "<Location>To be looked up later</Location>"
                + "<Description>Waiau 10 is a HECO plant on Oahu's grid that uses diesel as its fuel.</Description>"
                + "<Properties>" + " <Property>" + "  <Key>carbonIntensity</Key>"
                + "  <Value>2400</Value>" + " </Property>" + "</Properties>" + "</Source>" };
    // Whacking directly on the database here
    DbManager dbManager = (DbManager) server.getContext().getAttributes().get("DbManager");
    JAXBContext sourceJAXB;
    Unmarshaller unmarshaller;
    sourceJAXB = JAXBContext.newInstance(org.wattdepot.resource.source.jaxb.ObjectFactory.class);
    unmarshaller = sourceJAXB.createUnmarshaller();
    Source source;
    // Go through each Source String in XML format, turn it into a Source, and store in DB
    for (String xmlInput : sourceXmlStrings) {
      source = (Source) unmarshaller.unmarshal(new StringReader(xmlInput));
      if (!dbManager.storeSource(source)) {
        throw new DbException("Unable to store source from static XML data");
      }
    }
    OscarRowParser parser = new OscarRowParser("OscarDataConverter", getHostName());
    for (String row : oscarRows) {
      dbManager.storeSensorData(parser.parseRow(row.split(",")));
    }
  }

  // @Test
  // public void listSources() {
  // WattDepotClient client = new WattDepotClient(getHostName(), adminEmail, adminPassword);
  //
  // }

  /**
   * Fetches one SensorData from WattDepot server, and formats it as a nice string, and compares
   * that to an expected value. Solves this assignment problem:
   * http://code.google.com/p/wattdepot/wiki
   * /WattDepotCLI#2.5_list_sensordata_{source}_timestamp_{timestamp}
   * 
   * @throws Exception If there is a problem with anything.
   */
  @Test
  public void displayOneSensorData() throws Exception {
    // Anonymous access
    WattDepotClient client = new WattDepotClient(getHostName(), null, null);
    // These are the parameters that would be coming from the command line
    String sourceName = "SIM_KAHE_2", timestampString = "2009-10-12T00:15:00.000-10:00";
    SensorData data = null;
    String expectedOutput =
        String
            .format("Tool: OscarDataConverter%nSource: SIM_KAHE_2%nProperties: (powerGenerated : 6.4E7)%n");
    data = client.getSensorData(sourceName, Tstamp.makeTimestamp(timestampString));
    StringBuffer buff = new StringBuffer(200);
    buff.append(String.format("Tool: %s%n", data.getTool()));
    buff.append(String.format("Source: %s%n", UriUtils.getUriSuffix(data.getSource())));
    buff.append("Properties: ");
    List<Property> props = data.getProperties().getProperty();
    Property prop;
    for (int i = 0; i < props.size(); i++) {
      prop = props.get(i);
      if (i != 0) {
        // Before each property but the first, prefix with comma
        buff.append(", ");
      }
      buff.append(String.format("(%s : %s)", prop.getKey(), prop.getValue()));
    }
    buff.append(String.format("%n"));
    assertEquals("Generated string doesn't match expected", expectedOutput, buff.toString());
  }

  /**
   * Fetches one day of SensorData from WattDepot server, and formats it as series of nice lines,
   * and compares that to an expected value. Solves this assignment problem:
   * http://code.google.com/p/wattdepot/wiki/WattDepotCLI#2.6_list_sensordata_{source}_day_{day}
   * 
   * @throws Exception If there is a problem with anything.
   */
  @Test
  public void listOneDaySensorData() throws Exception {
    // Anonymous access
    WattDepotClient client = new WattDepotClient(getHostName(), null, null);
    // These are the parameters that would be coming from the command line
    String sourceName = "SIM_KAHE_2", day = "2009-10-12";
    String lineSep = System.getProperty("line.separator");
    String expectedOutput =
        "2009-10-12T00:00:00.000-10:00 Tool: OscarDataConverter Properties: [Property [key=powerGenerated, value=5.5E7]]"
            + lineSep
            + "2009-10-12T00:15:00.000-10:00 Tool: OscarDataConverter Properties: [Property [key=powerGenerated, value=6.4E7]]"
            + lineSep
            + "2009-10-12T00:30:00.000-10:00 Tool: OscarDataConverter Properties: [Property [key=powerGenerated, value=5.0E7]]"
            + lineSep;
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    Date startDate;
    XMLGregorianCalendar startTime, endTime;
    startDate = format.parse(day);
    startTime = Tstamp.makeTimestamp(startDate.getTime());
    endTime = Tstamp.incrementDays(startTime, 1);
    // // DEBUG
    // DateFormat formatOutput = DateFormat.getDateTimeInstance();
    // System.out.format("Start time: %s, end time: %s%n", startTime.toString(),
    // endTime.toString());
    List<SensorData> dataList = client.getSensorDatas(sourceName, startTime, endTime);
    StringBuffer buff = new StringBuffer(1000);
    for (SensorData data : dataList) {
      buff.append(data.getTimestamp() + " Tool: " + data.getTool() + " Properties: ");
      buff.append(data.getProperties().toString());
      buff.append(String.format("%n"));
    }
    assertEquals("Generated string doesn't match expected", expectedOutput, buff.toString());
  }
}

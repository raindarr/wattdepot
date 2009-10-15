package org.wattdepot.test.functional;

import static org.junit.Assert.assertEquals;
import java.io.StringReader;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
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
  @SuppressWarnings("PMD.AvoidDuplicateLiterals") // PMD going wild on the test data
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
   * http://code.google.com/p/wattdepot/wiki/WattDepotCLI#2.5_list_sensordata_{source}_timestamp_{timestamp}
   * 
   * @throws Exception If there is a problem with anything.
   */
  @Test
  public void displayOneSensorData() throws Exception {
    // Anonymous access
    WattDepotClient client = new WattDepotClient(getHostName(), null, null);
    SensorData data = null;
    String expectedOutput =
        String
            .format("Tool: OscarDataConverter%nSource: SIM_KAHE_2%nProperties: (powerGenerated : 6.4E7)%n");
    data =
        client.getSensorData("SIM_KAHE_2", Tstamp.makeTimestamp("2009-10-12T00:15:00.000-10:00"));
    StringBuffer buff = new StringBuffer();
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
}

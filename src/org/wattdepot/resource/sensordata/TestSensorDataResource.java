package org.wattdepot.resource.sensordata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.XMLGregorianCalendar;
import org.junit.Test;
import org.restlet.data.CharacterSet;
import org.restlet.data.Language;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Preference;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.StringRepresentation;
import org.wattdepot.client.BadXmlException;
import org.wattdepot.client.NotAuthorizedException;
import org.wattdepot.client.OverwriteAttemptedException;
import org.wattdepot.client.ResourceNotFoundException;
import org.wattdepot.client.WattDepotClient;
import org.wattdepot.client.WattDepotClientException;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.resource.sensordata.jaxb.SensorDataIndex;
import org.wattdepot.resource.sensordata.jaxb.SensorDataRef;
import org.wattdepot.resource.source.jaxb.Source;
import org.wattdepot.server.Server;
import org.wattdepot.test.ServerTestHelper;
import org.wattdepot.util.UriUtils;
import org.wattdepot.util.tstamp.Tstamp;

/**
 * Tests the SensorData resource API at the HTTP level using WattDepotClient.
 * 
 * @author Robert Brewer
 */
public class TestSensorDataResource extends ServerTestHelper {

  /** Making PMD happy. */
  private static final String REFS_DONT_MATCH_SENSORDATA =
      "SensorDataRefs from getSensorDataIndex do not match input SensorData";

  /** Making PMD happy. */
  private static final String DATA_STORE_FAILED = "SensorData store failed";

  /** Making PMD happy. */
  private static final String JUNIT_TOOL = "JUnit";

  /** Making PMD happy. */
  private static final String RETRIEVED_DATA_DOESNT_MATCH =
      "Retrieved SensorData does not match stored SensorData";

  /** Making PMD happy. */
  private static final String MISSING_SENSORDATAREFS =
      "SensorDataIndex did not contain list of SensorDataRefs";

  // Tests for GET {host}/sources/{source}/sensordata

  /**
   * Tests retrieval of all SensorData from a Source. Type: public Source with no credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test
  public void testFullIndexPublicWithNoCredentials() throws WattDepotClientException {
    WattDepotClient client = new WattDepotClient(getHostName());
    assertNotNull(MISSING_SENSORDATAREFS, client.getSensorDataIndex(defaultPublicSource)
        .getSensorDataRef());
  }

  /**
   * Tests retrieval of all SensorData from a Source. Type: public Source with invalid credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test(expected = NotAuthorizedException.class)
  public void testFullIndexPublicBadAuth() throws WattDepotClientException {
    // Shouldn't authenticate with invalid credentials.
    WattDepotClient client = new WattDepotClient(getHostName(), adminEmail, "foo");
    client.getSensorDataIndex(defaultPublicSource);
  }

  /**
   * Tests retrieval of all SensorData from a Source. Type: public Source with valid admin
   * credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test
  public void testFullIndexPublicWithAdminCredentials() throws WattDepotClientException {
    WattDepotClient client = new WattDepotClient(getHostName(), adminEmail, adminPassword);
    assertNotNull(MISSING_SENSORDATAREFS, client.getSensorDataIndex(defaultPublicSource)
        .getSensorDataRef());
  }

  /**
   * Tests retrieval of all SensorData from a Source. Type: public Source with valid owner
   * credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test
  public void testFullIndexPublicWithOwnerCredentials() throws WattDepotClientException {
    WattDepotClient client =
        new WattDepotClient(getHostName(), defaultOwnerUsername, defaultOwnerPassword);
    assertNotNull(MISSING_SENSORDATAREFS, client.getSensorDataIndex(defaultPublicSource)
        .getSensorDataRef());
  }

  /**
   * Tests retrieval of all SensorData from a Source. Type: public Source with valid non-owner
   * credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test
  public void testFullIndexPublicWithNonOwnerCredentials() throws WattDepotClientException {
    WattDepotClient client =
        new WattDepotClient(getHostName(), defaultNonOwnerUsername, defaultNonOwnerPassword);
    assertNotNull(MISSING_SENSORDATAREFS, client.getSensorDataIndex(defaultPublicSource)
        .getSensorDataRef());
  }

  /**
   * Tests retrieval of all SensorData from a Source. Type: private Source with no credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test(expected = NotAuthorizedException.class)
  public void testFullIndexPrivateWithNoCredentials() throws WattDepotClientException {
    WattDepotClient client = new WattDepotClient(getHostName());
    client.getSensorDataIndex(defaultPrivateSource);
  }

  /**
   * Tests retrieval of all SensorData from a Source. Type: private Source with invalid credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test(expected = NotAuthorizedException.class)
  public void testFullIndexPrivateBadAuth() throws WattDepotClientException {
    // Shouldn't authenticate with no username or password
    WattDepotClient client = new WattDepotClient(getHostName(), adminEmail, "wrong-password");
    client.getSensorDataIndex(defaultPrivateSource);
  }

  /**
   * Tests retrieval of all SensorData from a Source. Type: private Source with admin credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test
  public void testFullIndexPrivateAdminAuth() throws WattDepotClientException {
    // Shouldn't authenticate with no username or password
    WattDepotClient client = new WattDepotClient(getHostName(), adminEmail, adminPassword);
    assertNotNull(MISSING_SENSORDATAREFS, client.getSensorDataIndex(defaultPrivateSource));
  }

  /**
   * Tests retrieval of all SensorData from a Source. Type: private Source with owner credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test
  public void testFullIndexPrivateOwnerAuth() throws WattDepotClientException {
    // Shouldn't authenticate with no username or password
    WattDepotClient client =
        new WattDepotClient(getHostName(), defaultOwnerUsername, defaultOwnerPassword);
    assertNotNull(MISSING_SENSORDATAREFS, client.getSensorDataIndex(defaultPrivateSource));
  }

  /**
   * Tests retrieval of all SensorData from a Source. Type: private Source with non-owner
   * credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test(expected = NotAuthorizedException.class)
  public void testFullIndexPrivateNonOwnerAuth() throws WattDepotClientException {
    // Shouldn't authenticate with no username or password
    WattDepotClient client =
        new WattDepotClient(getHostName(), defaultNonOwnerUsername, defaultNonOwnerPassword);
    client.getSensorDataIndex(defaultPrivateSource);
  }

  /**
   * Tests retrieval of all SensorData from a Source. Type: unknown Source name with no credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test(expected = ResourceNotFoundException.class)
  public void testFullIndexBadSourceNameAnon() throws WattDepotClientException {
    // Shouldn't authenticate with no username or password
    WattDepotClient client = new WattDepotClient(getHostName());
    client.getSensorDataIndex("bogus-source-name");
  }

  /**
   * Tests retrieval of all SensorData from a Source. Type: unknown Source name with valid
   * credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test(expected = ResourceNotFoundException.class)
  public void testFullIndexBadSourceNameAuth() throws WattDepotClientException {
    // Shouldn't authenticate with no username or password
    WattDepotClient client = new WattDepotClient(getHostName(), adminEmail, adminPassword);
    client.getSensorDataIndex("bogus-source-name");
  }

  /**
   * Tests that a Source starts with no SensorData. Type: public Source with valid owner
   * credentials.
   * 
   * @throws WattDepotClientException If problems are encountered
   */
  @Test
  public void testFullIndexStartsEmpty() throws WattDepotClientException {
    WattDepotClient client =
        new WattDepotClient(getHostName(), defaultOwnerUsername, defaultOwnerPassword);
    assertTrue("Fresh DB contains SensorData", client.getSensorDataIndex(defaultPublicSource)
        .getSensorDataRef().isEmpty());
  }

  /**
   * Tests that after storing SensorData to a Source, the SensorDataIndex corresponds to the data
   * that has been stored. Type: public Source with valid owner credentials.
   * 
   * @throws Exception If stuff goes wrong.
   */
  @Test
  public void testFullIndexAfterStores() throws Exception {
    WattDepotClient client =
        new WattDepotClient(getHostName(), defaultOwnerUsername, defaultOwnerPassword);
    SensorData data1 = makeTestSensorData1(), data2 = makeTestSensorData2(), data3 =
        makeTestSensorData3();
    assertTrue(DATA_STORE_FAILED, client.storeSensorData(data1));
    List<SensorDataRef> index = client.getSensorDataIndex(defaultPublicSource).getSensorDataRef();
    assertEquals("Wrong number of SensorDataRefs after store", 1, index.size());
    assertTrue("getSensorDataIndex didn't return expected SensorDataRef", index.get(0)
        .equalsSensorData(data1));
    assertTrue(DATA_STORE_FAILED, client.storeSensorData(data2));
    index = client.getSensorDataIndex(defaultPublicSource).getSensorDataRef();
    assertEquals("Wrong number of SensorDataRefs after store", 2, index.size());
    List<SensorData> origData = new ArrayList<SensorData>();
    origData.add(data1);
    origData.add(data2);
    assertTrue("getSensorDataIndex didn't return expected SensorDataRefs", SensorDataRef
        .compareSensorDataRefsToSensorDatas(index, origData));
    assertTrue(DATA_STORE_FAILED, client.storeSensorData(data3));
    index = client.getSensorDataIndex(defaultPublicSource).getSensorDataRef();
    assertEquals("Wrong number of SensorDataRefs after store", 3, index.size());
    origData.add(data3);
    assertTrue("getSensorDataIndex didn't return expected SensorDataRefs", SensorDataRef
        .compareSensorDataRefsToSensorDatas(index, origData));
  }

  // Tests for GET {host}/sources/{source}/sensordata/{timestamp}
  // Cheating: by looking inside the black box, we know that all GET methods share the same access
  // control code, so not repeating all of that for this type of GET.

  /**
   * Tests that after storing SensorData to a Source, we can get the SensorData back out again.
   * Type: public Source with no credentials.
   * 
   * @throws Exception If stuff goes wrong.
   */
  @Test
  public void testGetAfterStores() throws Exception {
    WattDepotClient storeClient =
        new WattDepotClient(getHostName(), defaultOwnerUsername, defaultOwnerPassword);
    WattDepotClient getClient = new WattDepotClient(getHostName());
    SensorData data1 = makeTestSensorData1(), data2 = makeTestSensorData2(), data3 =
        makeTestSensorData3();

    assertTrue(DATA_STORE_FAILED, storeClient.storeSensorData(data1));
    assertEquals(RETRIEVED_DATA_DOESNT_MATCH, data1, getClient.getSensorData(defaultPublicSource,
        data1.getTimestamp()));
    assertTrue(DATA_STORE_FAILED, storeClient.storeSensorData(data2));
    assertEquals(RETRIEVED_DATA_DOESNT_MATCH, data2, getClient.getSensorData(defaultPublicSource,
        data2.getTimestamp()));
    assertTrue(DATA_STORE_FAILED, storeClient.storeSensorData(data3));
    assertEquals(RETRIEVED_DATA_DOESNT_MATCH, data3, getClient.getSensorData(defaultPublicSource,
        data3.getTimestamp()));
  }

  /**
   * Tests that SensorData cannot be retrieved from an empty database. Type: public Source with no
   * credentials.
   * 
   * @throws Exception If stuff goes wrong.
   */
  @Test(expected = ResourceNotFoundException.class)
  public void testGetWithFreshDB() throws Exception {
    WattDepotClient client = new WattDepotClient(getHostName());
    assertNull("Able to retrieve SensorData from fresh DB", client.getSensorData(
        defaultPublicSource, Tstamp.makeTimestamp("2009-07-28T09:00:00.000-10:00")));
  }

  /**
   * Tests that SensorData cannot be retrieved using a bogus timestamp. Type: public Source with no
   * credentials.
   * 
   * @throws Exception If stuff goes wrong.
   */
  @Test
  public void testGetWithBadTimestamp() throws Exception {
    WattDepotClient client = new WattDepotClient(getHostName());

    Response response =
        client.makeRequest(Method.GET, Server.SOURCES_URI + "/" + defaultPublicSource + "/"
            + Server.SENSORDATA_URI + "/" + "bogus-timestamp", new Preference<MediaType>(
            MediaType.TEXT_XML), null);
    Status status = response.getStatus();
    assertEquals("Able to get SensorData with bad timestamp", Status.CLIENT_ERROR_BAD_REQUEST,
        status);
  }

  // Tests for GET {host}/sources/{source}/sensordata/?startTime={timestamp}&endTime={timestamp}
  // Again: cheating by looking inside the black box, we know that all GET methods share the same
  // access control code, so not repeating all of that for this type of GET.

  /**
   * Tests that after storing SensorData to a Source, the fetching various ranges of SensorData
   * corresponds to the data that has been stored. Type: public Source with owner credentials.
   * 
   * @throws Exception If stuff goes wrong.
   */
  @Test
  public void testRangeIndexAfterStores() throws Exception {
    WattDepotClient client =
        new WattDepotClient(getHostName(), defaultOwnerUsername, defaultOwnerPassword);
    SensorData data1 = makeTestSensorData1(), data2 = makeTestSensorData2(), data3 =
        makeTestSensorData3();
    // before all three of the test data items
    XMLGregorianCalendar before1 = Tstamp.makeTimestamp("2009-07-28T08:00:00.000-10:00"),
    // At the time of data1
    at1 = Tstamp.makeTimestamp("2009-07-28T09:00:00.000-10:00"),
    // between data1 and data2
    between1And2 = Tstamp.makeTimestamp("2009-07-28T09:07:00.000-10:00"),
    // between data2 and data3
    between2And3 = Tstamp.makeTimestamp("2009-07-28T09:23:00.000-10:00"),
    // At the time of data3
    at3 = Tstamp.makeTimestamp("2009-07-28T09:30:00.000-10:00"),
    // after all three test data items
    after3 = Tstamp.makeTimestamp("2009-07-28T10:00:00.000-10:00"),
    // Ever later than after3
    moreAfter3 = Tstamp.makeTimestamp("2009-07-29T10:00:00.000-10:00");

    assertTrue(DATA_STORE_FAILED, client.storeSensorData(data1));
    assertTrue(DATA_STORE_FAILED, client.storeSensorData(data2));
    assertTrue(DATA_STORE_FAILED, client.storeSensorData(data3));

    // valid range covering no data
    assertEquals("SensorDataIndex generated for period containing no SensorData",
        new SensorDataIndex().getSensorDataRef(), client.getSensorDataIndex(defaultPublicSource,
            after3, moreAfter3).getSensorDataRef());

    // range covering all three data items
    List<SensorDataRef> retrievedRefs =
        client.getSensorDataIndex(defaultPublicSource, before1, after3).getSensorDataRef();
    List<SensorData> origData = new ArrayList<SensorData>();
    origData.add(data1);
    origData.add(data2);
    origData.add(data3);
    assertTrue(REFS_DONT_MATCH_SENSORDATA, SensorDataRef.compareSensorDataRefsToSensorDatas(
        retrievedRefs, origData));

    // range starting exactly at data1 and ending exactly at data3
    retrievedRefs = client.getSensorDataIndex(defaultPublicSource, at1, at3).getSensorDataRef();
    origData.clear();
    origData.add(data1);
    origData.add(data2);
    origData.add(data3);
    assertTrue(REFS_DONT_MATCH_SENSORDATA, SensorDataRef.compareSensorDataRefsToSensorDatas(
        retrievedRefs, origData));

    // range covering only data1
    assertSame("getSensorDataIndex didn't contain all expected SensorData", client
        .getSensorDataIndex(defaultPublicSource, before1, between1And2).getSensorDataRef().size(),
        1);
    assertTrue("getSensorDataIndex entry didn't match input data", client.getSensorDataIndex(
        defaultPublicSource, before1, between1And2).getSensorDataRef().get(0).equalsSensorData(
        data1));

    // range covering only data2
    assertSame("getSensorDataIndex didn't contain all expected SensorData", client
        .getSensorDataIndex(defaultPublicSource, between1And2, between2And3).getSensorDataRef()
        .size(), 1);
    assertTrue("getSensorDataIndex didn't return expected ", client.getSensorDataIndex(
        defaultPublicSource, between1And2, between2And3).getSensorDataRef().get(0)
        .equalsSensorData(data2));

    // range covering data1 & data2
    retrievedRefs =
        client.getSensorDataIndex(defaultPublicSource, before1, between2And3).getSensorDataRef();
    origData.clear();
    origData.add(data1);
    origData.add(data2);
    assertTrue(REFS_DONT_MATCH_SENSORDATA, SensorDataRef.compareSensorDataRefsToSensorDatas(
        retrievedRefs, origData));

    // range covering data2 & data3
    retrievedRefs =
        client.getSensorDataIndex(defaultPublicSource, between1And2, after3).getSensorDataRef();
    origData.clear();
    origData.add(data2);
    origData.add(data3);
    assertTrue(REFS_DONT_MATCH_SENSORDATA, SensorDataRef.compareSensorDataRefsToSensorDatas(
        retrievedRefs, origData));
  }

  /**
   * Tests that a Source starts with no SensorData. Type: public Source with valid owner
   * credentials.
   * 
   * @throws Exception If problems are encountered
   */
  @Test
  public void testRangeIndexStartsEmpty() throws Exception {
    WattDepotClient client =
        new WattDepotClient(getHostName(), defaultOwnerUsername, defaultOwnerPassword);
    // before all three of the test data items
    XMLGregorianCalendar before1 = Tstamp.makeTimestamp("2009-07-28T08:00:00.000-10:00"),
    // after all three test data items
    after3 = Tstamp.makeTimestamp("2009-07-28T10:00:00.000-10:00");

    assertTrue("Fresh DB contains SensorData", client.getSensorDataIndex(defaultPublicSource,
        before1, after3).getSensorDataRef().isEmpty());
  }

  /**
   * Tests that after storing SensorData to a Source, the end time parameter must be later than the
   * end time Type: public Source with owner credentials.
   * 
   * @throws Exception If stuff goes wrong.
   */
  @Test(expected = BadXmlException.class)
  public void testRangeIndexBadInterval() throws Exception {
    WattDepotClient client =
        new WattDepotClient(getHostName(), defaultOwnerUsername, defaultOwnerPassword);
    SensorData data1 = makeTestSensorData1(), data2 = makeTestSensorData2(), data3 =
        makeTestSensorData3();
    // before all three of the test data items
    XMLGregorianCalendar before1 = Tstamp.makeTimestamp("2009-07-28T08:00:00.000-10:00"),
    // after all three test data items
    after3 = Tstamp.makeTimestamp("2009-07-28T10:00:00.000-10:00");

    assertTrue(DATA_STORE_FAILED, client.storeSensorData(data1));
    assertTrue(DATA_STORE_FAILED, client.storeSensorData(data2));
    assertTrue(DATA_STORE_FAILED, client.storeSensorData(data3));

    // This should fail since start time is after end time
    client.getSensorDataIndex(defaultPublicSource, after3, before1);
    fail("Fetching SensorDataIndex with bad range succeeded");
  }

  // Tests for PUT {host}/sources/{source}/sensordata/{timestamp}

  /**
   * Tests storing SensorData to a Source. Type: public Source with no credentials.
   * 
   * @throws Exception If things go wrong during data setup.
   */
  @Test(expected = NotAuthorizedException.class)
  public void testStorePublicWithNoCredentials() throws Exception {
    WattDepotClient client = new WattDepotClient(getHostName());
    SensorData data = makeTestSensorData1();
    assertFalse("Able to store SensorData without credentials", client.storeSensorData(data));
  }

  /**
   * Tests storing SensorData to a Source. Type: public Source with invalid credentials.
   * 
   * @throws Exception If things go wrong during data setup.
   */
  @Test(expected = NotAuthorizedException.class)
  public void testStorePublicWithBadAuth() throws Exception {
    // Shouldn't authenticate with no username or password
    WattDepotClient client = new WattDepotClient(getHostName(), adminEmail, "foo");
    SensorData data = makeTestSensorData1();
    assertFalse("Able to store SensorData with bad credentials", client.storeSensorData(data));
  }

  /**
   * Tests storing SensorData to a Source. Type: public Source with valid admin credentials.
   * 
   * @throws Exception If things go wrong during data setup.
   */
  @Test
  public void testStorePublicWithAdminCredentials() throws Exception {
    WattDepotClient client = new WattDepotClient(getHostName(), adminEmail, adminPassword);
    SensorData data = makeTestSensorData1();
    assertTrue("Unable to store SensorData with admin credentials", client.storeSensorData(data));
    assertEquals(RETRIEVED_DATA_DOESNT_MATCH, data, client.getSensorData(defaultPublicSource, data
        .getTimestamp()));
  }

  /**
   * Tests storing SensorData to a Source. Type: public Source with valid owner credentials.
   * 
   * @throws Exception If problems are encountered.
   */
  @Test
  public void testStorePublicWithOwnerCredentials() throws Exception {
    WattDepotClient client =
        new WattDepotClient(getHostName(), defaultOwnerUsername, defaultOwnerPassword);
    SensorData data = makeTestSensorData1();
    assertTrue("Unable to store SensorData with valid owner credentials", client
        .storeSensorData(data));
    assertEquals(RETRIEVED_DATA_DOESNT_MATCH, data, client.getSensorData(defaultPublicSource, data
        .getTimestamp()));
  }

  /**
   * Tests storing SensorData to a Source. Type: public Source with valid non-owner credentials.
   * 
   * @throws Exception If problems are encountered.
   */
  @Test(expected = NotAuthorizedException.class)
  public void testStorePublicWithNonOwnerCredentials() throws Exception {
    WattDepotClient client =
        new WattDepotClient(getHostName(), defaultNonOwnerUsername, defaultNonOwnerPassword);
    SensorData data = makeTestSensorData1();
    assertFalse("Able to store SensorData with valid non-owner credentials", client
        .storeSensorData(data));
  }

  /**
   * Tests storing SensorData to a Source. Type: private Source with no credentials.
   * 
   * @throws Exception If problems are encountered.
   */
  @Test(expected = NotAuthorizedException.class)
  public void testStorePrivateWithNoCredentials() throws Exception {
    WattDepotClient client = new WattDepotClient(getHostName());
    SensorData data = makeTestSensorDataPrivateSource();
    assertFalse("Able to store SensorData with no credentials", client.storeSensorData(data));
  }

  /**
   * Tests storing SensorData to a Source. Type: private Source with invalid credentials.
   * 
   * @throws Exception If problems are encountered.
   */
  @Test(expected = NotAuthorizedException.class)
  public void testStorePrivateBadAuth() throws Exception {
    WattDepotClient client = new WattDepotClient(getHostName(), adminEmail, "wrong-password");
    SensorData data = makeTestSensorDataPrivateSource();
    assertFalse("Able to store SensorData with invalid credentials", client.storeSensorData(data));
  }

  /**
   * Tests storing SensorData to a Source. Type: private Source with admin credentials.
   * 
   * @throws Exception If problems are encountered.
   */
  @Test
  public void testStorePrivateAdminAuth() throws Exception {
    WattDepotClient client = new WattDepotClient(getHostName(), adminEmail, adminPassword);
    SensorData data = makeTestSensorDataPrivateSource();
    assertTrue("Unable to store SensorData with admin credentials", client.storeSensorData(data));
    assertEquals(RETRIEVED_DATA_DOESNT_MATCH, data, client.getSensorData(defaultPrivateSource, data
        .getTimestamp()));
  }

  /**
   * Tests storing SensorData to a Source. Type: private Source with owner credentials.
   * 
   * @throws Exception If problems are encountered.
   */
  @Test
  public void testStorePrivateOwnerAuth() throws Exception {
    WattDepotClient client =
        new WattDepotClient(getHostName(), defaultOwnerUsername, defaultOwnerPassword);
    SensorData data = makeTestSensorDataPrivateSource();
    assertTrue("Unable to store SensorData with owner credentials", client.storeSensorData(data));
    assertEquals(RETRIEVED_DATA_DOESNT_MATCH, data, client.getSensorData(defaultPrivateSource, data
        .getTimestamp()));
  }

  /**
   * Tests storing SensorData to a Source. Type: private Source with non-owner credentials.
   * 
   * @throws Exception If problems are encountered.
   */
  @Test(expected = NotAuthorizedException.class)
  public void testStorePrivateNonOwnerAuth() throws Exception {
    WattDepotClient client =
        new WattDepotClient(getHostName(), defaultNonOwnerUsername, defaultNonOwnerPassword);
    SensorData data = makeTestSensorDataPrivateSource();
    assertFalse("Able to store SensorData with non-owner credentials", client.storeSensorData(data));
    assertEquals(RETRIEVED_DATA_DOESNT_MATCH, data, client.getSensorData(defaultPrivateSource, data
        .getTimestamp()));
  }

  /**
   * Tests storing SensorData to a Source. Type: unknown Source name in URI with owner credentials.
   * 
   * @throws Exception If problems are encountered.
   */
  @Test(expected = ResourceNotFoundException.class)
  public void testStoreBadSourceName() throws Exception {
    WattDepotClient client =
        new WattDepotClient(getHostName(), defaultOwnerUsername, defaultOwnerPassword);
    SensorData data =
        new SensorData(Tstamp.makeTimestamp(), JUNIT_TOOL, getHostName() + Server.SOURCES_URI
            + "/bogus-source-name");
    assertFalse("Able to store SensorData with bogus Source name", client.storeSensorData(data));
  }

  /**
   * Tests storing SensorData to a Source. Type: bad URI timestamp with owner credentials.
   * 
   * @throws JAXBException If there are problems making XML.
   */
  @Test
  public void testStoreBadTimestamp() throws JAXBException {
    WattDepotClient client = new WattDepotClient(getHostName(), adminEmail, adminPassword);
    SensorData data =
        new SensorData(Tstamp.makeTimestamp(), JUNIT_TOOL, Source.sourceToUri(defaultPublicSource,
            server));
    // Can't use WattDepotClient to test this, as storeSensorData() is unable to send a bad
    // timestamp. Have to do things manually.
    JAXBContext sensorDataJAXB =
        JAXBContext.newInstance(org.wattdepot.resource.sensordata.jaxb.ObjectFactory.class);
    Marshaller marshaller = sensorDataJAXB.createMarshaller();
    StringWriter writer = new StringWriter();
    marshaller.marshal(data, writer);
    Representation rep =
        new StringRepresentation(writer.toString(), MediaType.TEXT_XML, Language.ALL,
            CharacterSet.UTF_8);
    Response response =
        client.makeRequest(Method.PUT, Server.SOURCES_URI + "/"
            + UriUtils.getUriSuffix(data.getSource()) + "/" + Server.SENSORDATA_URI + "/"
            + "bogus-timestamp", new Preference<MediaType>(MediaType.TEXT_XML), rep);
    Status status = response.getStatus();
    assertEquals("Able to store SensorData with bad timestamp", Status.CLIENT_ERROR_BAD_REQUEST,
        status);
  }

  /**
   * Tests storing SensorData to a Source. Type: no entity body with owner credentials.
   */
  @Test
  public void testStoreNullEntity() {
    WattDepotClient client = new WattDepotClient(getHostName(), adminEmail, adminPassword);
    SensorData data =
        new SensorData(Tstamp.makeTimestamp(), JUNIT_TOOL, Source.sourceToUri(defaultPublicSource,
            server));
    // Can't use WattDepotClient.storeSensorData() to test this, as it is unable to send empty
    // body. Have to do things manually.
    Response response =
        client.makeRequest(Method.PUT, Server.SOURCES_URI + "/"
            + UriUtils.getUriSuffix(data.getSource()) + "/" + Server.SENSORDATA_URI + "/"
            + data.getTimestamp().toString(), new Preference<MediaType>(MediaType.TEXT_XML), null);
    Status status = response.getStatus();
    assertEquals("Able to store SensorData with no entity body", Status.CLIENT_ERROR_BAD_REQUEST,
        status);
  }

  /**
   * Tests storing SensorData to a Source. Type: empty entity body with owner credentials.
   */
  @Test
  public void testStoreEmptyEntity() {
    WattDepotClient client = new WattDepotClient(getHostName(), adminEmail, adminPassword);
    SensorData data =
        new SensorData(Tstamp.makeTimestamp(), JUNIT_TOOL, Source.sourceToUri(defaultPublicSource,
            server));
    // Can't use WattDepotClient.storeSensorData() to test this, as it is unable to send empty
    // body. Have to do things manually.
    Representation rep =
        new StringRepresentation("", MediaType.TEXT_XML, Language.ALL, CharacterSet.UTF_8);
    Response response =
        client.makeRequest(Method.PUT, Server.SOURCES_URI + "/"
            + UriUtils.getUriSuffix(data.getSource()) + "/" + Server.SENSORDATA_URI + "/"
            + data.getTimestamp().toString(), new Preference<MediaType>(MediaType.TEXT_XML), rep);
    Status status = response.getStatus();
    assertEquals("Able to store SensorData with empty entity body",
        Status.CLIENT_ERROR_BAD_REQUEST, status);
  }

  /**
   * Tests storing SensorData to a Source. Type: bogus XML in entity body with owner credentials.
   */
  @Test
  public void testStoreBogusXML() {
    WattDepotClient client = new WattDepotClient(getHostName(), adminEmail, adminPassword);
    SensorData data =
        new SensorData(Tstamp.makeTimestamp(), JUNIT_TOOL, Source.sourceToUri(defaultPublicSource,
            server));
    // Can't use WattDepotClient.storeSensorData() to test this, as it is unable to send bogus XML.
    // Have to do things manually.
    Representation rep =
        new StringRepresentation("bogus-non-XML", MediaType.TEXT_XML, Language.ALL,
            CharacterSet.UTF_8);
    Response response =
        client.makeRequest(Method.PUT, Server.SOURCES_URI + "/"
            + UriUtils.getUriSuffix(data.getSource()) + "/" + Server.SENSORDATA_URI + "/"
            + data.getTimestamp().toString(), new Preference<MediaType>(MediaType.TEXT_XML), rep);
    Status status = response.getStatus();
    assertEquals("Able to store SensorData with bogus XML in entity body",
        Status.CLIENT_ERROR_BAD_REQUEST, status);
  }

  /**
   * Tests storing SensorData to a Source. Type: timestamp in URI doesn't match timestamp in entity
   * body with owner credentials.
   * 
   * @throws JAXBException If there are problems making XML.
   */
  @Test
  public void testStoreMismatchedTimestamps() throws JAXBException {
    WattDepotClient client = new WattDepotClient(getHostName(), adminEmail, adminPassword);
    XMLGregorianCalendar timestamp = Tstamp.makeTimestamp();
    SensorData data =
        new SensorData(timestamp, JUNIT_TOOL, Source.sourceToUri(defaultPublicSource, server));
    // Can't use WattDepotClient.storeSensorData() to test this, as it is unable to send
    // mismatching timestamps. Have to do things manually.
    JAXBContext sensorDataJAXB =
        JAXBContext.newInstance(org.wattdepot.resource.sensordata.jaxb.ObjectFactory.class);
    Marshaller marshaller = sensorDataJAXB.createMarshaller();
    StringWriter writer = new StringWriter();
    marshaller.marshal(data, writer);
    Representation rep =
        new StringRepresentation(writer.toString(), MediaType.TEXT_XML, Language.ALL,
            CharacterSet.UTF_8);
    // make timestamp 1 hour later in URI
    XMLGregorianCalendar uriTimestamp = Tstamp.incrementHours(timestamp, 1);
    Response response =
        client.makeRequest(Method.PUT, Server.SOURCES_URI + "/"
            + UriUtils.getUriSuffix(data.getSource()) + "/" + Server.SENSORDATA_URI + "/"
            + uriTimestamp.toString(), new Preference<MediaType>(MediaType.TEXT_XML), rep);
    Status status = response.getStatus();
    assertEquals("Able to store SensorData with mismatching timestamps between URI and body",
        Status.CLIENT_ERROR_BAD_REQUEST, status);
  }

  /**
   * Tests storing SensorData to a Source. Type: source in URI doesn't match source in entity body
   * with owner credentials.
   * 
   * @throws JAXBException If there are problems making XML.
   */
  @Test
  public void testStoreMismatchedSources() throws JAXBException {
    WattDepotClient client = new WattDepotClient(getHostName(), adminEmail, adminPassword);
    SensorData data =
        new SensorData(Tstamp.makeTimestamp(), JUNIT_TOOL, Source.sourceToUri("bogus-source-name",
            server));
    // Can't use WattDepotClient.storeSensorData() to test this, as it is unable to send
    // mismatching timestamps. Have to do things manually.
    JAXBContext sensorDataJAXB =
        JAXBContext.newInstance(org.wattdepot.resource.sensordata.jaxb.ObjectFactory.class);
    Marshaller marshaller = sensorDataJAXB.createMarshaller();
    StringWriter writer = new StringWriter();
    marshaller.marshal(data, writer);
    Representation rep =
        new StringRepresentation(writer.toString(), MediaType.TEXT_XML, Language.ALL,
            CharacterSet.UTF_8);
    // make timestamp 1 hour later in URI
    Response response =
        client.makeRequest(Method.PUT, Server.SOURCES_URI + "/" + defaultPublicSource + "/"
            + Server.SENSORDATA_URI + "/" + data.getTimestamp().toString(),
            new Preference<MediaType>(MediaType.TEXT_XML), rep);
    Status status = response.getStatus();
    assertEquals("Able to store SensorData with mismatching source name between URI and body",
        Status.CLIENT_ERROR_BAD_REQUEST, status);
  }

  /**
   * Tests overwriting a SensorData resource. Type: public Source with owner credentials.
   * 
   * @throws Exception If problems are encountered.
   */
  @Test(expected = OverwriteAttemptedException.class)
  public void testStoreOverwrite() throws Exception {
    WattDepotClient client =
        new WattDepotClient(getHostName(), defaultOwnerUsername, defaultOwnerPassword);
    SensorData data = makeTestSensorData1();
    assertTrue("Unable to store SensorData with owner credentials", client.storeSensorData(data));
    assertFalse("Able to overwrite existing SensorData resource", client.storeSensorData(data));
  }

//  @Test
//  public void bogusTest() {
//    assertTrue("Expected failure", false);
//  }
  
  // Tests for DELETE {host}/sources/{source}/sensordata/{timestamp}
  // TODO

}

package org.wattdepot.resource.gviz;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wattdepot.datainput.OscarRowParser;
import org.wattdepot.datainput.RowParseException;
import org.wattdepot.resource.source.jaxb.Source;
import org.wattdepot.resource.user.jaxb.User;
import org.wattdepot.server.Server;
import org.wattdepot.server.db.DbException;
import org.wattdepot.test.ServerTestHelper;
import com.meterware.httpunit.HttpException;
import com.meterware.httpunit.WebConversation;

/**
 * Tests the GVisualizationResource's JSONP responses using HTTPUnit.
 * 
 * The <a href="code.google.com/apis/ajax/playground/?type=visualization#using_the_query_language">
 * Google Code Playground </a> can also be helpful in debugging.
 * 
 * @author Andrea Connell
 * 
 */

public class TestGVisualizationResource extends ServerTestHelper {

  /** First timestamp. */
  private static String timestamp1 = "2009-10-12T00:00:00-10:00";
  /** Second timestamp. */
  private static String timestamp2 = "2009-10-12T00:15:00-10:00";
  /** Third timestamp. */
  private static String timestamp3 = "2009-10-12T00:30:00-10:00";

  /** Prefix to query string parameter for startTime. */
  private static String startTime = "startTime=";
  /** Prefix to query string parameter for endTime. */
  private static String endTime = "endTime=";
  /** Prefix to query string parameter for samplingInterval. */
  private static String samplingInterval = "samplingInterval=";
  /** Prefix to query string parameter for the query. */
  private static String query = "tq=";
  /** Prefix to query string parameter for query arguments. */
  private static String tqx = "tqx=";
  /** Query string for invalid parameter, which shouldn't break the request. */
  private static String bogusParam = "bogus=bogus";
  /** The prefix that should be found at the beginning of every GViz response. */
  private static String expectedJsonpPrefix = "google.visualization.Query.setResponse(";

  /** URL of the gviz resource for a source. */
  private static String gvizUrl = server.getHostName() + Server.SOURCES_URI
      + "/SIM_HONOLULU_8/gviz/";
  /** URL of the gviz resource for a virtual source. */
  private static String gvizVirtualUrl = server.getHostName() + Server.SOURCES_URI
      + "/SIM_HONOLULU/gviz/";
  /** URL of the gviz resource for a private source. */
  private static String gvizPrivateUrl = server.getHostName() + Server.SOURCES_URI
      + "/SIM_HPOWER/gviz/";
  /** URL of the sensor data gviz resource. */
  private static String sensorDataUrl = gvizUrl + "sensordata";
  /** URL of the calculated gviz resource. */
  private static String calculatedUrl = gvizUrl + "calculated?" + startTime + timestamp1 + "&"
      + endTime + timestamp2;

  /** The WebConversation to use for the tests. */
  private static WebConversation wc;

  /**
   * Initialize variables for the tests.
   */
  @BeforeClass
  public static void initialize() {
    wc = new WebConversation();
    try {
      query += URLEncoder.encode("Select timePoint, powerConsumed", "UTF-8");
      tqx += URLEncoder.encode("reqId:2", "UTF-8");
    }
    catch (UnsupportedEncodingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  /** Override setupDB, since we don't want the default test data this time. */
  @Override
  public void setupDB() {
  };

  /**
   * Test the overall gviz response for sensordata.
   * 
   * @throws Exception if URL cannot be found, if URL returns an error, or if JSON cannot be parsed.
   */
  @Test
  public void testSensorData() throws Exception {
    String expectedResponse =
        "google.visualization.Query.setResponse({status:'ok',table:{cols:[{id:'timePoint',label:'Date & Time',type:'datetime',pattern:''},{id:'powerConsumed',label:'Power Cons. (W)',type:'number',pattern:''},{id:'powerGenerated',label:'Power Gen. (W)',type:'number',pattern:''},{id:'energyConsumedToDate',label:'Energy Consumed To Date (Wh)',type:'number',pattern:''},{id:'energyGeneratedToDate',label:'Energy Generated To Date (Wh)',type:'number',pattern:''}],rows:[{c:[{v:new Date(2009,9,12,0,0,0)},{v:0.0},{v:0.0},{v:0.0},{v:0.0}]},{c:[{v:new Date(2009,9,12,0,15,0)},{v:0.0},{v:0.0},{v:0.0},{v:0.0}]},{c:[{v:new Date(2009,9,12,0,30,0)},{v:0.0},{v:0.0},{v:0.0},{v:0.0}]}]}});";

    // Test fetch all sensor data
    String response = wc.getResponse(sensorDataUrl).getText();

    // strip out dates before compare to ignore time zone issues
    // expectedResponse = expectedResponse.replaceAll("new Date\\(([0-9,]*)\\)", "date");
    // response = response.replaceAll("new Date\\(([0-9,]*)\\)", "date");
    assertEquals("GViz sensordata response is incorrect", expectedResponse, response);

    // Test all parameters together
    String responseAllParams =
        wc.getResponse(
            sensorDataUrl + "?" + startTime + timestamp1 + "&" + endTime + timestamp2 + "&"
                + query + "&" + tqx).getText();
    assertTrue("GViz sensor data with all params is not proper JSONP",
        validateJson(responseAllParams));
  }

  /**
   * Test that a private source cannot be accessed by a non-owner.
   * 
   * @throws Exception HttpException expected.
   */
  @Test(expected = HttpException.class)
  public void testPrivateSource() throws Exception {
    wc.getResponse(gvizPrivateUrl + "sensordata").getText();
    fail("GViz private source does not throw expected error.");
  }

  /**
   * Test that a private subsource cannot be viewed by using the displaySubsources parameter of a
   * calculated GViz query.
   * 
   * @throws Exception if URL cannot be found, if URL returns an error, or if JSON cannot be parsed.
   */
  @Test
  public void testPrivateSubsource() throws Exception {
    String response =
        wc.getResponse(
            server.getHostName() + Server.SOURCES_URI + "/SIM_IPP/gviz/calculated?" + startTime
                + timestamp1 + "&" + endTime + timestamp2 + "&displaySubsources=true").getText();
    JSONObject json = getJsonObject(response);
    assertEquals("GViz calculated response with private subsource has incorrect columns", 11,
        this.getNumColumns(json));
    assertTrue("GViz calculated response with private subsource mentions the subsource",
        !response.contains("HPOWER"));
  }

  /**
   * Test the overall gviz response for calculated data.
   * 
   * @throws Exception if URL cannot be found, if URL returns an error, or if JSON cannot be parsed.
   */
  @Test
  public void testCalculated() throws Exception {

    String expectedResponse =
        "google.visualization.Query.setResponse({status:'ok',table:{cols:[{id:'timePoint',label:'Date & Time',type:'datetime',pattern:''},{id:'powerConsumed',label:'Power Cons. (W)',type:'number',pattern:''},{id:'powerGenerated',label:'Power Gen. (W)',type:'number',pattern:''},{id:'energyConsumed',label:'Energy Cons. (Wh)',type:'number',pattern:''},{id:'energyGenerated',label:'Energy Gen. (Wh)',type:'number',pattern:''},{id:'carbonEmitted',label:'Carbon (lbs CO2)',type:'number',pattern:''}],rows:[{c:[{v:new Date(2009,9,12,0,0,0)},{v:0.0},{v:0.0},{v:0.0},{v:0.0},{v:0.0}]},{c:[{v:new Date(2009,9,12,0,1,30)},{v:0.0},{v:0.0},{v:0.0},{v:0.0},{v:0.0}]},{c:[{v:new Date(2009,9,12,0,3,0)},{v:0.0},{v:0.0},{v:0.0},{v:0.0},{v:0.0}]},{c:[{v:new Date(2009,9,12,0,4,30)},{v:0.0},{v:0.0},{v:0.0},{v:0.0},{v:0.0}]},{c:[{v:new Date(2009,9,12,0,6,0)},{v:0.0},{v:0.0},{v:0.0},{v:0.0},{v:0.0}]},{c:[{v:new Date(2009,9,12,0,7,30)},{v:0.0},{v:0.0},{v:0.0},{v:0.0},{v:0.0}]},{c:[{v:new Date(2009,9,12,0,9,0)},{v:0.0},{v:0.0},{v:0.0},{v:0.0},{v:0.0}]},{c:[{v:new Date(2009,9,12,0,10,30)},{v:0.0},{v:0.0},{v:0.0},{v:0.0},{v:0.0}]},{c:[{v:new Date(2009,9,12,0,12,0)},{v:0.0},{v:0.0},{v:0.0},{v:0.0},{v:0.0}]},{c:[{v:new Date(2009,9,12,0,13,30)},{v:0.0},{v:0.0},{v:0.0},{v:0.0},{v:0.0}]},{c:[{v:new Date(2009,9,12,0,15,0)},{v:0.0},{v:0.0},{v:0.0},{v:0.0},{v:0.0}]}]}});";

    String response = wc.getResponse(calculatedUrl).getText();

    // strip out dates before compare to ignore time zone issues
    // expectedResponse = expectedResponse.replaceAll("new Date\\(([0-9,]*)\\)", "date");
    // response = response.replaceAll("new Date\\(([0-9,]*)\\)", "date");
    assertEquals("GViz calculated response is incorrect", expectedResponse, response);

    // Test all parameters
    String responseAllParams =
        wc.getResponse(
            calculatedUrl + "&" + samplingInterval + "15&displaySubsources=true&" + query + "&"
                + tqx).getText();
    assertTrue("GViz calculated with all params is not proper JSONP",
        validateJson(responseAllParams));
  }

  /**
   * Test that sensor data returns the correct number of rows, whether or not start and end dates
   * are provided.
   * 
   * @throws Exception if URL cannot be found, if URL returns an error, or if JSON cannot be parsed.
   */
  @Test
  public void testSensorDataDates() throws Exception {
    String responseWithoutDate = wc.getResponse(sensorDataUrl).getText();
    JSONObject withoutDate = getJsonObject(responseWithoutDate);
    assertTrue("GViz sensor data is not proper JSONP", withoutDate != null);
    assertTrue("GViz sensor data without date returns wrong number of rows",
        getNumRows(withoutDate) == 3);

    String responseByDate1 =
        wc.getResponse(sensorDataUrl + "?" + startTime + timestamp1 + "&" + endTime + timestamp2)
            .getText();
    JSONObject json1 = getJsonObject(responseByDate1);
    assertTrue("GViz sensor data by date is not proper JSONP", json1 != null);
    assertTrue("GViz sensor data by date returns wrong number of rows", getNumRows(json1) == 2);

    String responseByDate2 =
        wc.getResponse(sensorDataUrl + "?" + startTime + timestamp1 + "&" + endTime + timestamp3)
            .getText();
    JSONObject json2 = getJsonObject(responseByDate2);
    assertTrue("GViz sensor data by date is not proper JSONP", json2 != null);
    assertTrue("GViz sensor data by date returns wrong number of rows", getNumRows(json2) == 3);
  }

  /**
   * Test that requesting calculated data without start and end dates returns an error.
   * 
   * @throws Exception if URL cannot be found or if URL returns an error.
   */
  @Test
  // (expected = HttpException.class)
  public void testCalculatedDates() throws Exception {
    String responseNoDates = wc.getResponse(gvizUrl + "calculated").getText();
    assertEquals("GViz calculated without dates does not throw expected error.",
        expectedJsonpPrefix + "{status:'error',errors:[{reason:'INVALID_REQUEST',"
            + "message:'Valid startTime and endTime parameters are required.'}]});",
        responseNoDates);

    String responseBadInterval =
        wc.getResponse(
            gvizUrl + "calculated?" + startTime + timestamp2 + "&" + endTime + timestamp1)
            .getText();
    assertEquals("GViz calculated with bad interval does not throw expected error.",
        expectedJsonpPrefix + "{status:'error',errors:[{reason:'INVALID_REQUEST',"
            + "message:'startTime parameter later than endTime parameter'}]});",
        responseBadInterval);

    String responseInvalidDates =
        wc.getResponse(gvizUrl + "calculated?" + startTime + "bogus&" + endTime + timestamp1)
            .getText();
    assertEquals("GViz calculated with invalid dates does not throw expected error.",
        expectedJsonpPrefix + "{status:'error',errors:[{reason:'INVALID_REQUEST',"
            + "message:'Invalid Start Time'}]});", responseInvalidDates);

    // fail("GViz calculated without dates does not throw error.");
  }

  /**
   * Test that unexpected parameters does not break GViz (as required by Google).
   * 
   * @throws Exception if URL cannot be found, if URL returns an error, or if JSON cannot be parsed.
   */
  @Test
  public void testUnexpectedParameter() throws Exception {

    String sensorData = wc.getResponse(sensorDataUrl + "?" + bogusParam).getText();
    assertTrue("GViz sensor data with bogus param is not proper JSONP", validateJson(sensorData));

    String latestSensorData = wc.getResponse(sensorDataUrl + "/latest?" + bogusParam).getText();
    assertTrue("GViz latest sensor data with unexpected param is not proper JSONP",
        validateJson(latestSensorData));

    String calculated = wc.getResponse(calculatedUrl + "&" + bogusParam).getText();
    assertTrue("GViz calculated with unexpected param is not proper JSONP",
        validateJson(calculated));
  }

  /**
   * Test that the latest sensor data returns a single table row, whether or not other parameters
   * are present.
   * 
   * @throws Exception if URL cannot be found, if URL returns an error, or if JSON cannot be parsed.
   */
  @Test
  public void testLatestSensorData() throws Exception {
    // Test correct number of rows
    String response = wc.getResponse(sensorDataUrl + "/latest").getText();
    JSONObject json = getJsonObject(response);
    assertTrue("GViz latest sensor data response is not proper JSONP", json != null);
    assertTrue("GViz latest sensor data returns wrong number of rows", getNumRows(json) == 1);

    // Test with all parameters
    String responseParameters =
        wc.getResponse(sensorDataUrl + "/latest?" + query + "&" + tqx).getText();
    JSONObject jsonParameters = getJsonObject(responseParameters);
    assertTrue("GViz latest sensor data with all params is not proper JSONP",
        jsonParameters != null);
    assertTrue("GViz latest sensor data with all params returns wrong number of rows",
        getNumRows(jsonParameters) == 1);
  }

  /**
   * Test that a virtual source returns the correct number of rows and columns, with and without
   * subsources turned on.
   * 
   * @throws Exception if URL cannot be found, if URL returns an error, or if JSON cannot be parsed.
   */
  @Test
  public void testSubsources() throws Exception {
    String calculatedVirtualUrl =
        gvizVirtualUrl + "calculated?" + startTime + timestamp1 + "&" + endTime + timestamp2;

    String responseWithSubsources =
        wc.getResponse(calculatedVirtualUrl + "&displaySubsources=true").getText();
    JSONObject withSubsources = getJsonObject(responseWithSubsources);
    assertTrue("GViz calculated with subsources is not proper JSONP", withSubsources != null);
    assertTrue("GViz calculated with subsources has wrong number of columns",
        getNumColumns(withSubsources) == 16);
    assertTrue("GViz calculated with subsources has wrong number of rows",
        getNumRows(withSubsources) == 11);

    // Test virtual source without subsources
    String responseWithoutSubsources =
        wc.getResponse(calculatedVirtualUrl + "&displaySubsources=false").getText();
    JSONObject withoutSubsources = getJsonObject(responseWithoutSubsources);
    assertTrue("GViz calculated without subsources is not proper JSONP", withoutSubsources != null);
    assertTrue("GViz calculated without subsources has wrong number of columns",
        getNumColumns(withoutSubsources) == 6);
    assertTrue("GViz calculated without subsources has wrong number of rows",
        getNumRows(withoutSubsources) == 11);
  }

  /**
   * Test that a calculated query returns the correct number of rows, with or without the sampling
   * interval.
   * 
   * @throws Exception if URL cannot be found, if URL returns an error, or if JSON cannot be parsed.
   */
  @Test
  public void testSamplingInterval() throws Exception {
    String response = wc.getResponse(calculatedUrl).getText();
    JSONObject json = getJsonObject(response);
    assertTrue("GViz response is not proper JSONP", json != null);
    assertTrue("GViz sampling interval returns wrong number of results", getNumRows(json) == 11);

    String response15 = wc.getResponse(calculatedUrl + "&" + samplingInterval + "15").getText();
    JSONObject json15 = getJsonObject(response15);
    assertTrue("GViz response is not proper JSONP", json15 != null);
    assertTrue("GViz sampling interval returns wrong number of results", getNumRows(json15) == 2);

    String response12 = wc.getResponse(calculatedUrl + "&" + samplingInterval + "12").getText();
    JSONObject json12 = getJsonObject(response12);
    assertTrue("GViz response is not proper JSONP", json12 != null);
    assertTrue("GViz sampling interval returns wrong number of results", getNumRows(json12) == 3);

    String responseTooHigh =
        wc.getResponse(calculatedUrl + "&" + samplingInterval + "60").getText();
    assertEquals("GViz setting samplingInterval too high does not throw expected error.",
        expectedJsonpPrefix + "{status:'error',errors:[{reason:'INVALID_REQUEST',"
            + "message:'samplingInterval parameter was larger than time range.'}]});",
        responseTooHigh);

    String responseTooLow =
        wc.getResponse(calculatedUrl + "&" + samplingInterval + "-60").getText();
    assertEquals("GViz setting samplingInterval below zero does not throw expected error.",
        expectedJsonpPrefix + "{status:'error',errors:[{reason:'INVALID_REQUEST',"
            + "message:'samplingInterval parameter was less than 0.'}]});", responseTooLow);
  }

  /**
   * Test that a response has the correct number of table columns with and without the query
   * parameter.
   * 
   * @throws Exception if URL cannot be found, if URL returns an error, or if JSON cannot be parsed.
   */
  @Test
  public void testQuery() throws Exception {
    String sensordataResponseWithoutQuery = wc.getResponse(sensorDataUrl).getText();
    JSONObject sensordataWithoutQuery = getJsonObject(sensordataResponseWithoutQuery);
    assertTrue("GViz sensor data without query is not proper JSONP",
        sensordataWithoutQuery != null);
    assertTrue("GViz sensor data without query has wrong number of columns",
        getNumColumns(sensordataWithoutQuery) == 5);

    String sensordataResponseWithQuery = wc.getResponse(sensorDataUrl + "?" + query).getText();
    JSONObject sensordataWithQuery = getJsonObject(sensordataResponseWithQuery);
    assertTrue("GViz sensor data with query is not proper JSONP", sensordataWithQuery != null);
    assertTrue("GViz sensor data without query has wrong number of columns",
        getNumColumns(sensordataWithQuery) == 2);

    String responseCalculatedWithoutQuery = wc.getResponse(calculatedUrl).getText();
    JSONObject calculatedWithoutQuery = getJsonObject(responseCalculatedWithoutQuery);
    assertTrue("GViz calculated without query is not proper JSONP", calculatedWithoutQuery != null);
    assertTrue("GViz calculated without query has wrong number of columns",
        getNumColumns(calculatedWithoutQuery) == 6);

    String calculatedResponseWithQuery = wc.getResponse(calculatedUrl + "&" + query).getText();
    JSONObject calculatedwithQuery = getJsonObject(calculatedResponseWithQuery);
    assertTrue("GViz calculated with query is not proper JSONP", calculatedwithQuery != null);
    assertTrue("GViz calculated with query has wrong number of columns",
        getNumColumns(calculatedwithQuery) == 2);
  }

  /**
   * Test that a request with a tqx parameter containing a reqId field returns the reqId in the
   * response.
   * 
   * @throws Exception if URL cannot be found, if URL returns an error, or if JSON cannot be parsed.
   */
  @Test
  public void testReqId() throws Exception {
    String calculatedResponse = wc.getResponse(calculatedUrl + "&" + tqx).getText();
    JSONObject calculated = getJsonObject(calculatedResponse);
    assertTrue("GViz calculated doesn't return reqId", calculated.getString("reqId").equals("2"));

    String sensorDataResponse = wc.getResponse(sensorDataUrl + "?" + tqx).getText();
    JSONObject sensorData = getJsonObject(sensorDataResponse);
    assertTrue("GViz sensor data doesn't return reqId", sensorData.getString("reqId").equals("2"));
  }

  /**
   * Ensures that a GViz response strings contains valid JSON.
   * 
   * @param jsonpString the GViz response string.
   * @return true if the jsonpString contains valid JSON.
   * @throws Exception if the JSON cannot be parsed.
   */
  private boolean validateJson(String jsonpString) throws Exception {
    return getJsonObject(jsonpString) != null;
  }

  /**
   * Turns a GViz response string into a JSON object. Checks for and strips off JSONP prefix, then
   * replaces non-JSON date formats before parsing. Checks that the status, table, cols, and rows
   * elements all exist.
   * 
   * @param jsonpString the GViz response string.
   * @return The JSONObject representation of the response.
   * @throws Exception if the JSON cannot be parsed.
   */
  private JSONObject getJsonObject(String jsonpString) throws Exception {
    // check for the prefix
    if (!jsonpString.contains(expectedJsonpPrefix)) {
      return null;
    }
    // trim off prefix and trailing ");"
    String jsonString =
        jsonpString.substring(expectedJsonpPrefix.length(), jsonpString.length() - 2);

    // strip out the dates, which GViz lets us use but aren't actually JSON compliant
    jsonString = jsonString.replaceAll("new Date\\(([0-9,]*)\\)", "date");

    // parse remaining jsonString and check for required elements
    JSONObject jsonResponse = new JSONObject(jsonString);

    String status = jsonResponse.getString("status");
    if (status == null) {
      return null;
    }
    JSONObject jsonTable = jsonResponse.getJSONObject("table");
    if (jsonTable == null) {
      return null;
    }
    JSONArray colArray = jsonTable.getJSONArray("cols");
    if (colArray == null) {
      return null;
    }
    JSONArray rowArray = jsonTable.getJSONArray("rows");
    if (rowArray == null) {
      return null;
    }

    return jsonResponse;
  }

  /**
   * Find the number of table rows in a JSONObject that represents a GViz response.
   * 
   * @param json the JSONObject, obtained from parsing a GViz response.
   * @return The number of rows in the table
   * @throws Exception if the JSONObject does not have a table with rows.
   */
  private int getNumRows(JSONObject json) throws Exception {
    return json.getJSONObject("table").getJSONArray("rows").length();
  }

  /**
   * Find the number of table columns in a jSONObject that represents a GViz response.
   * 
   * @param json The JSONObject, obtained from parsing a GViz response.
   * @return The number of columns in the table.
   * @throws Exception if the JSONObject does nto have a table with columns.
   */
  private int getNumColumns(JSONObject json) throws Exception {
    return json.getJSONObject("table").getJSONArray("cols").length();
  }

  /**
   * Programmatically loads some Oscar data needed by the rest of the tests.
   * 
   * @throws RowParseException If there is a problem parsing the static data (should never happen).
   * @throws JAXBException If there are problems marshalling the XML dat
   * @throws DbException If there is a problem storing the test data in the database
   * 
   */
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
                + "<Public>false</Public>"
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
                + "  <Value>2400</Value>" + " </Property>" + "</Properties>" + "</Source>",
            "<?xml version=\"1.0\"?>"
                + "<Source>"
                + "<Name>SIM_KAHE</Name>"
                + "<Owner>http://server.wattdepot.org:1234/wattdepot/users/oscar@wattdepot.org</Owner>"
                + "<Public>true</Public>" + "<Virtual>true</Virtual>"
                + "<Coordinates>0,0,0</Coordinates>"
                + "<Location>To be looked up later</Location>"
                + "<Description>Virtual resource for all Kahe power plants.</Description>"
                + "<SubSources>"
                + " <Href>http://server.wattdepot.org:1234/wattdepot/sources/SIM_KAHE_1</Href>"
                + " <Href>http://server.wattdepot.org:1234/wattdepot/sources/SIM_KAHE_2</Href>"
                + " <Href>http://server.wattdepot.org:1234/wattdepot/sources/SIM_KAHE_3</Href>"
                + " <Href>http://server.wattdepot.org:1234/wattdepot/sources/SIM_KAHE_4</Href>"
                + " <Href>http://server.wattdepot.org:1234/wattdepot/sources/SIM_KAHE_5</Href>"
                + " <Href>http://server.wattdepot.org:1234/wattdepot/sources/SIM_KAHE_6</Href>"
                // + " <Href>http://server.wattdepot.org:1234/wattdepot/sources/SIM_KAHE_7</Href>"
                + "</SubSources>" + "</Source>",
            "<?xml version=\"1.0\"?>"
                + "<Source>"
                + "<Name>SIM_WAIAU</Name>"
                + "<Owner>http://server.wattdepot.org:1234/wattdepot/users/oscar@wattdepot.org</Owner>"
                + "<Public>true</Public>"
                + "<Virtual>true</Virtual>"
                + "<Coordinates>0,0,0</Coordinates>"
                + "<Location>To be looked up later</Location>"
                + "<Description>Virtual resource for all Waiau power plants.</Description>"
                + "<SubSources>"
                + " <Href>http://server.wattdepot.org:1234/wattdepot/sources/SIM_WAIAU_5</Href>"
                + " <Href>http://server.wattdepot.org:1234/wattdepot/sources/SIM_WAIAU_6</Href>"
                // + " <Href>http://server.wattdepot.org:1234/wattdepot/sources/SIM_WAIAU_7</Href>"
                // + " <Href>http://server.wattdepot.org:1234/wattdepot/sources/SIM_WAIAU_8</Href>"
                + " <Href>http://server.wattdepot.org:1234/wattdepot/sources/SIM_WAIAU_9</Href>"
                + " <Href>http://server.wattdepot.org:1234/wattdepot/sources/SIM_WAIAU_10</Href>"
                + "</SubSources>" + "</Source>",
            "<?xml version=\"1.0\"?>"
                + "<Source>"
                + "<Name>SIM_HONOLULU</Name>"
                + "<Owner>http://server.wattdepot.org:1234/wattdepot/users/oscar@wattdepot.org</Owner>"
                + "<Public>true</Public>"
                + "<Virtual>true</Virtual>"
                + "<Coordinates>21.306278,-157.863997,0</Coordinates>"
                + "<Location>To be looked up later</Location>"
                + "<Description>Virtual resource for all Honolulu power plants.</Description>"
                + "<SubSources>"
                + " <Href>http://server.wattdepot.org:1234/wattdepot/sources/SIM_HONOLULU_8</Href>"
                + " <Href>http://server.wattdepot.org:1234/wattdepot/sources/SIM_HONOLULU_9</Href>"
                + "</SubSources>" + "</Source>",
            "<?xml version=\"1.0\"?>"
                + "<Source>"
                + "<Name>SIM_IPP</Name>"
                + "<Owner>http://server.wattdepot.org:1234/wattdepot/users/oscar@wattdepot.org</Owner>"
                + "<Public>true</Public>"
                + "<Virtual>true</Virtual>"
                + "<Coordinates>0,0,0</Coordinates>"
                + "<Location>To be looked up later</Location>"
                + "<Description>Virtual resource for all independent power producers (non-HECO).</Description>"
                + "<SubSources>"
                // + " <Href>http://server.wattdepot.org:1234/wattdepot/sources/SIM_AES</Href>"
                + " <Href>http://server.wattdepot.org:1234/wattdepot/sources/SIM_HPOWER</Href>"
                + " <Href>http://server.wattdepot.org:1234/wattdepot/sources/SIM_KALAELOA</Href>"
                + "</SubSources>" + "</Source>",
            "<?xml version=\"1.0\"?>"
                + "<Source>"
                + "<Name>SIM_OAHU_GRID</Name>"
                + "<Owner>http://server.wattdepot.org:1234/wattdepot/users/oscar@wattdepot.org</Owner>"
                + "<Public>true</Public>" + "<Virtual>true</Virtual>"
                + "<Coordinates>0,0,0</Coordinates>"
                + "<Location>To be looked up later</Location>"
                + "<Description>Virtual resource for all Oahu power plants.</Description>"
                + "<SubSources>"
                + " <Href>http://server.wattdepot.org:1234/wattdepot/sources/SIM_KAHE</Href>"
                + " <Href>http://server.wattdepot.org:1234/wattdepot/sources/SIM_WAIAU</Href>"
                + " <Href>http://server.wattdepot.org:1234/wattdepot/sources/SIM_HONOLULU</Href>"
                + " <Href>http://server.wattdepot.org:1234/wattdepot/sources/SIM_IPP</Href>"
                + "</SubSources>" + "</Source>" };

    JAXBContext sourceJAXB;
    Unmarshaller unmarshaller;
    sourceJAXB = JAXBContext.newInstance(org.wattdepot.resource.source.jaxb.ObjectFactory.class);
    unmarshaller = sourceJAXB.createUnmarshaller();
    Source source;

    if (!manager.storeUser(new User("oscar@wattdepot.org", "password", false, null))) {
      throw new DbException("Unable to store oscar user");
    }

    // Go through each Source String in XML format, turn it into a Source, and store in DB
    for (String xmlInput : sourceXmlStrings) {
      source = (Source) unmarshaller.unmarshal(new StringReader(xmlInput));
      // Source read from the file might have an Href elements under SubSources that points to
      // a different host URI. We want all defaults normalized to this server, so update it.
      if (source.isSetSubSources()) {
        List<String> hrefs = source.getSubSources().getHref();
        for (int i = 0; i < hrefs.size(); i++) {
          hrefs.set(i, Source.updateUri(hrefs.get(i), server));
        }
      }
      if (!manager.storeSource(source)) {
        throw new DbException("Unable to store source from static XML data");
      }
    }
    OscarRowParser parser = new OscarRowParser("OscarDataConverter", getHostName());
    for (String row : oscarRows) {
      manager.storeSensorDataNoCache(parser.parseRow(row.split(",")));
    }
    server.getContext().getAttributes().put("DbManager", manager);
  }
}

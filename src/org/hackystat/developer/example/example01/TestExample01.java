package org.hackystat.developer.example.example01;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;


/**
 * Tests the code in Example01.
 * @author Philip Johnson
 */
public class TestExample01 {

  /**
   * Tests the checkHost method. 
   */
  @Test
  public void testCheckHost () {
    Example01 example = new Example01();
    assertTrue("Checking that the public sensorbase is available", example.checkHost());
  }

  /**
   * Tests the checkCredentials() method. 
   */
  @Test
  public void testCheckCredentials () {
    Example01 example = new Example01();
    assertTrue("Checking that the example credentials are valid", example.checkCredentials());
  }
  
  /**
   * Tests the getSensorDataCount() method. 
   * @throws Exception If problems occur getting the data. 
   */
  @Test 
  public void testGetSensorDataCount() throws Exception {
    Example01 example = new Example01();
    assertEquals("Checking sensor data count", 2797, example.getSensorDataCount());
  }
  
  /**
   * Tests the getDevEventCount() method. 
   * @throws Exception If problems occur getting the data. 
   */
  @Test 
  public void testGetDevEventDataCount() throws Exception {
    Example01 example = new Example01();
    assertEquals("Checking DevEvent count", 2216, example.getDevEventCount());
  }

  

}

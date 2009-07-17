package org.hackystat.developer.example.example03;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * A simple test of the Example03 class which exercises the DailyProjectDataClient. 
 * @author Philip Johnson
 *
 */
public class TestExample03 {

  /**
   * Checks to see that the getDevTime() method returns the right value from the public server.
   * @throws Exception If problems occur retrieving the data. 
   */
  @Test
  public void testGetDevTime() throws Exception {
    Example03 example = new Example03();
    assertEquals("Checking total devtime", 430, example.getDevTime());
  }
}

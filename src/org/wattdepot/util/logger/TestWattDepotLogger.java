package org.wattdepot.util.logger;

import static org.junit.Assert.assertEquals;
import java.util.logging.Logger;
import org.junit.Test;

/**
 * Tests the WattDepotLogger class. Portions of this code are adapted from
 * http://hackystat-utilities.googlecode.com/
 * 
 * @author Philip Johnson
 * @author Robert Brewer
 */

public class TestWattDepotLogger {

  /**
   * Tests the logger. Instantiates the logger and writes a test message.
   * 
   */
  @Test
  public void testLogging() {
    Logger logger = WattDepotLogger.getLogger("org.wattdepot.util.testlogger");
    logger.fine("(Test message)");
    logger = WattDepotLogger.getLogger("org.wattdepot.nestedlogger.test", "testlogging");
    WattDepotLogger.setLoggingLevel(logger, "FINE");
    logger.fine("(Test message2)");
    assertEquals("Checking identity", "org.wattdepot.nestedlogger.test", logger.getName());
  }
}

package org.wattdepot.server.db;

import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Instantiates a DbManager and tests the database snapshot method.
 * 
 * @author Robert Brewer
 */
public class TestDbManagerSnapshot extends DbManagerTestHelper {

  /**
   * Tests the makeSnapshot method. Note that at the DbManager level we cannot test any deeper than
   * this, since this could be a stub method for implementations that do not have snapshot support.
   * For a more in-depth test of snapshot creationg, see
   * org.wattdepot.server.db.derby.TestDerbyStorageImplementation().
   * 
   * @see org.wattdepot.server.db.derby.TestDerbyStorageImplementation
   */
  @Test
  public void testMakeSnapshot() {
    assertTrue("Unable to create snapshot", manager.makeSnapshot());
  }
}

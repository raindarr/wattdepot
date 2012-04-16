package org.wattdepot.server.db.derby;

import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.wattdepot.server.db.DbManagerTestHelper;

/**
 * Tests functionality that is specific to the DerbyStorageImplementation, that cannot be assumed at
 * the DbManager level.
 * 
 * @author Robert Brewer
 */
public class TestDerbyStorageImplementation extends DbManagerTestHelper {
  /**
   * Tests the makeSnapshot method. The check to see if the modification date of the backup log is
   * updated by the method was removed because a race condition caused it to fail frequently.
   * 
   */
  @Test
  public void testMakeSnapshot() {
    // File snapshotDir =
    // new File(server.getServerProperties().get(ServerProperties.DERBY_SNAPSHOT_KEY));
    // File backupLog =
    // new File(server.getServerProperties().get(ServerProperties.DERBY_DIR_KEY)
    // + "/wattdepot/BACKUP.HISTORY");
    // Date before = null;
    //
    // if (backupLog.exists()) {
    // assertTrue("Backup logfile isn't a file!", backupLog.isFile());
    // before = new Date(backupLog.lastModified());
    // // System.out.format("before: %1$ta %1$tb %1$td %1$tT.%1$tL %n", before); // DEBUG
    // }
    // else {
    // System.out.println("before: [doesn't exist]");
    // }
    assertTrue("Unable to create snapshot", manager.makeSnapshot());

    // // There is a potential race condition since we might test for the snapshot before it is made
    // // as the snapshot is made asynchronously. Hopefully this kludge is good enough for the test.
    // Thread.sleep(10000);
    // assertTrue("No snapshot directory created", snapshotDir.isDirectory());
    // assertTrue("No backup log created", backupLog.isFile());
    //
    // if (before != null) {
    // // Only need to check if modification time changed if there was a snapshot directory before
    // Date after = new Date(backupLog.lastModified());
    // // System.out.format("after: %1$ta %1$tb %1$td %1$tT.%1$tL%n", after); // DEBUG
    // assertTrue("Backup log not modified", after.after(before));
    // }
  }
}

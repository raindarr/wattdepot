package org.wattdepot.server.db.postgres;

import static org.junit.Assert.assertTrue;
import java.io.File;
import java.util.Date;
import org.junit.Ignore;
import org.junit.Test;
import org.wattdepot.server.ServerProperties;
import org.wattdepot.server.db.DbManagerTestHelper;

/**
 * Tests functionality that is specific to the PostgresStorageImplementation, that cannot be assumed
 * at the DbManager level.
 * 
 * @author Andrea Connell
 */
public class TestPostgresStorageImplementation extends DbManagerTestHelper {
  /**
   * Tests the makeSnapshot method. Checks to see if the modification date of the backup zip is
   * updated by the method as a quick check that something happened.
   * 
   */
  @Test
  @Ignore("This doesn't work on all systems because of permissions")
  public void testMakeSnapshot() {
    File backupZip =
        new File(server.getServerProperties().get(ServerProperties.POSTGRES_SNAPSHOT_KEY) + ".zip");
    Date before = null;

    if (backupZip.exists()) {
      assertTrue("Backup zip isn't a file!", backupZip.isFile());
      before = new Date(backupZip.lastModified());
    }
    else {
      System.out.println("before: [doesn't exist]");
    }
    assertTrue("Unable to create snapshot", manager.makeSnapshot());
    assertTrue("No backup zip created", backupZip.isFile());
    assertTrue("Backup zip is empty", backupZip.length() > 0);

    if (before != null) {
      // Only need to check if modification time changed if there was a snapshot directory before
      Date after = new Date(backupZip.lastModified());
      assertTrue("Backup zip not modified", after.after(before));
    }
  }
}
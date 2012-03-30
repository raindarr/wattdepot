package org.wattdepot.server.db.berkeleydb;

import javax.xml.datatype.XMLGregorianCalendar;
import org.wattdepot.resource.user.jaxb.User;
import org.wattdepot.util.tstamp.Tstamp;
import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

/**
 * Represents a WattDepot user in BerkeleyDB.
 * 
 * @author George Lee
 * 
 */
@Entity
public class BerkeleyDbUser {
  @PrimaryKey
  private String username;
  private String password;
  private boolean isAdmin;
  private long lastMod;

  /**
   * Default constructor required by BerkeleyDb.
   */
  public BerkeleyDbUser() {
    // Required by BerkeleyDb.
  }

  /**
   * Construct a BerkeleyDbUser given the properties from a WattDepot user.
   * 
   * @param email The email of the user (their username).
   * @param password The user's password.
   * @param isAdmin True if the user is an admin, false otherwise.
   */
  public BerkeleyDbUser(String email, String password, boolean isAdmin) {
    this.username = email;
    this.password = password;
    this.isAdmin = isAdmin;
    this.lastMod = Tstamp.makeTimestamp().toGregorianCalendar().getTimeInMillis();
  }

  /**
   * Get the username of this user.
   * 
   * @return The user's username (their email address).
   */
  public String getUsername() {
    return this.username;
  }

  /**
   * Get a WattDepot representation of this BerkeleyDbUser.
   * 
   * @return WattDepot representation of this user.
   */
  public User asUser() {
    User user = new User();
    user.setEmail(this.username);
    user.setPassword(this.password);
    user.setAdmin(this.isAdmin);

    return user;
  }

  /**
   * Get the last modification date of this sensor data.
   * 
   * @return The last modification date.
   */
  public XMLGregorianCalendar getLastMod() {
    return Tstamp.makeTimestamp(this.lastMod);
  }
}

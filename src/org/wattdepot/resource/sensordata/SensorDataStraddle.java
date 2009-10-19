package org.wattdepot.resource.sensordata;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;
import org.wattdepot.resource.sensordata.jaxb.SensorData;

/**
 * Provides a representation for a pair of SensorData objects that "straddle" a particular
 * timestamp. One SensorData comes before the timestamp, one comes after the timestamp. In most
 * cases, the straddling SensorData should be the closest possible SensorData to the given
 * timestamp, providing the shortest interval that contains the timestamp.
 * 
 * There is a degenerate case, where there is SensorData that corresponds exactly to the timestamp.
 * In this case, both the straddling SensorData are equal to the SensorData for the timestamp.
 * 
 * It also provides methods for computing various interpolated values based on the properties
 * specified in the straddling SensorData objects.
 * 
 * @author Robert Brewer
 */
public class SensorDataStraddle {

  /** The timestamp of interest. */
  private XMLGregorianCalendar timestamp;

  /** The SensorData that comes before the timestamp. */
  private SensorData beforeData;

  /** The SensorData that comes after the timestamp. */
  private SensorData afterData;

  /**
   * Creates the new SensorDataStraddle with the given parameters.
   * 
   * @param timestamp The timestamp of interest in the straddle.
   * @param beforeData The SensorData that comes before the timestamp.
   * @param afterData The SensorData that comes after the timestamp.
   * @throws IllegalArgumentException If beforeData or afterData are null, if beforeData is after
   * afterData (order swapped), or if the timestamp is not between beforeData and afterData.
   */
  public SensorDataStraddle(XMLGregorianCalendar timestamp, SensorData beforeData,
      SensorData afterData) {
    if ((beforeData == null) || (afterData == null)) {
      throw new IllegalArgumentException(
          "Attempt to create SensorDataStraddle with null SensorData");
    }
    else if (beforeData.getTimestamp().compare(afterData.getTimestamp()) == DatatypeConstants.GREATER) {
      // before > after, which is bogus
      throw new IllegalArgumentException(
          "Attempt to create SensorDataStraddle with null SensorData");
    }
    else {
      this.beforeData = beforeData;
      this.afterData = afterData;
    }
    if (validateTimestamp(timestamp)) {
      this.timestamp = timestamp;
    }
    else {
      throw new IllegalArgumentException("Attempt to set timestamp outside of straddle range");
    }
  }

  /**
   * Checks whether the given timestamp is inside the straddle range.
   * 
   * @param timestamp The timestamp to be checked.
   * @return True if the timestamp is after (or equal to) the start of the straddle and before (or
   * equal to) the end of the straddle, false otherwise.
   */
  private boolean validateTimestamp(XMLGregorianCalendar timestamp) {
    XMLGregorianCalendar before = this.beforeData.getTimestamp(), after =
        this.afterData.getTimestamp();
    int compareBefore = timestamp.compare(before);
    int compareAfter = timestamp.compare(after);
    // valid if timestamp equal to beforeData or afterData timestamp
    if ((compareBefore == DatatypeConstants.EQUAL) || (compareAfter == DatatypeConstants.EQUAL)) {
      return true;
    }
    // valid if timestamp between beforeData and afterData timestamps
    else {
      return (compareBefore == DatatypeConstants.GREATER) && (compareAfter == DatatypeConstants.LESSER);
    }
  }

  /**
   * @return the timestamp
   */
  public XMLGregorianCalendar getTimestamp() {
    return timestamp;
  }

  /**
   * @param timestamp the timestamp to set
   * @throws IllegalArgumentException If the timestamp is not between beforeData and afterData.
   */
  public void setTimestamp(XMLGregorianCalendar timestamp) {
    if (validateTimestamp(timestamp)) {
      this.timestamp = timestamp;
    }
    else {
      throw new IllegalArgumentException("Attempt to set timestamp outside of straddle range");
    }
  }

  /**
   * @return the beforeData
   */
  public SensorData getBeforeData() {
    return beforeData;
  }

  /**
   * @return the afterData
   */
  public SensorData getAfterData() {
    return afterData;
  }
}

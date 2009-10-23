package org.wattdepot.resource.sensordata;

import java.util.List;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;
import org.wattdepot.resource.sensordata.jaxb.Properties;
import org.wattdepot.resource.sensordata.jaxb.Property;
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
  private final SensorData beforeData;

  /** The SensorData that comes after the timestamp. */
  private final SensorData afterData;

  /** Whether this straddle is degenerate or not. */
  private final boolean degenerate;

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
      this.degenerate = beforeData.equals(afterData);
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
      return (compareBefore == DatatypeConstants.GREATER)
          && (compareAfter == DatatypeConstants.LESSER);
    }
  }

  /**
   * Returns the timestamp for this object.
   * 
   * @return the timestamp
   */
  public XMLGregorianCalendar getTimestamp() {
    return timestamp;
  }

  /**
   * Sets the timestamp for this object.
   * 
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
   * Gets the SensorData that is before the timestamp.
   * 
   * @return the beforeData
   */
  public SensorData getBeforeData() {
    return beforeData;
  }

  /**
   * Gets the SensorData that is after the timestamp.
   * 
   * @return the afterData
   */
  public SensorData getAfterData() {
    return afterData;
  }

  /**
   * Indicates whether this straddle is the degenerate case where the timestamp was equal to a
   * sensor data in the source.
   * 
   * @return True if timestamp matched sensor data, false otherwise.
   */
  public boolean isDegenerate() {
    return this.degenerate;
  }

  /**
   * Takes the name of a property, and then returns the output value for the timestamp via linear
   * interpolation between the beforeData and afterData. Assumes that the property key exists in
   * beforeData and afterData, and that the property value can be converted to a double.
   * 
   * @param propertyKey the key for the property we are interested in.
   * @return the linearly interpolated output given the timestamp.
   */
  private double linearlyInterpolate(String propertyKey) {
    if (isDegenerate()) {
      // degenerate case: timestamp matched actual sensor data, so just return property from
      // sensor data
      return beforeData.getProperties().getPropertyAsDouble(propertyKey);
    }
    else {
      String beforeValue = SensorDataUtils.getPropertyValue(this.beforeData, propertyKey);
      String afterValue = SensorDataUtils.getPropertyValue(this.afterData, propertyKey);
      // If the property is missing from either SensorData, return 0
      if ((beforeValue == null) || (afterValue == null)) {
        return 0;
      }
      else {
        double beforePower = Double.valueOf(beforeValue);
        double afterPower = Double.valueOf(afterValue);
        // convert from milliseconds to seconds
        double beforeTime =
            this.beforeData.getTimestamp().toGregorianCalendar().getTimeInMillis() / 1000.0;
        double afterTime =
            this.afterData.getTimestamp().toGregorianCalendar().getTimeInMillis() / 1000.0;
        double timestampTime = this.timestamp.toGregorianCalendar().getTimeInMillis() / 1000.0;

        // linear interpolation time!
        return ((afterPower - beforePower) / (afterTime - beforeTime))
            * (timestampTime - beforeTime) + beforePower;
      }
    }
  }

  /**
   * Returns the linearly interpolated value for powerGenerated at the timestamp.
   * 
   * @return the power generated at the timestamp.
   */
  public double getPowerGenerated() {
    return linearlyInterpolate("powerGenerated");
  }

  /**
   * Returns the linearly interpolated value for powerConsumed at the timestamp.
   * 
   * @return the power consumed at the timestamp.
   */
  public double getPowerConsumed() {
    return linearlyInterpolate("powerConsumed");
  }

  /**
   * Returns a SensorData object representing the power at the timestamp. If the timestamp
   * corresponded to sensor data from the source, then this is just that sensor data. If the
   * timestamp did not correspond to a sensor data object but did fall between two sensor data
   * objects, then the resulting SensorData object will return the interpolated values for
   * powerGenerated and/or powerConsumed (if they are available).
   * 
   * @return The SensorData object representing power.
   */
  public SensorData getPower() {
    if (isDegenerate()) {
      // degenerate case: timestamp matched actual sensor data, so just return that sensor data
      return beforeData;
    }
    else {
      double powerGeneratedValue = getPowerGenerated();
      double powerConsumedValue = getPowerConsumed();

      return makePowerSensorData(this.timestamp, this.beforeData.getSource(), powerGeneratedValue,
          powerConsumedValue, true);
    }
  }

  /**
   * Creates an SensorData object that contains the given powerGenerated and powerConsumed values.
   * 
   * @param timestamp The timestamp of the SensorData to be created.
   * @param source The source URI of the SensorData to be created.
   * @param powerGeneratedValue The amount of power generated.
   * @param powerConsumedValue The amount of power consumed.
   * @param interpolated True if the values were determined by interpolation, false otherwise.
   * @return The new SensorData object.
   */
  public static SensorData makePowerSensorData(XMLGregorianCalendar timestamp, String source,
      double powerGeneratedValue, double powerConsumedValue, boolean interpolated) {
    Properties props = new Properties();
    Property generatedProp, consumedProp, interpolatedProp;
    generatedProp =
        SensorDataUtils.makeSensorDataProperty("powerGenerated", Double
            .toString(powerGeneratedValue));
    consumedProp =
        SensorDataUtils
            .makeSensorDataProperty("powerConsumed", Double.toString(powerConsumedValue));
    props.getProperty().add(generatedProp);
    props.getProperty().add(consumedProp);
    if (interpolated) {
      interpolatedProp = SensorDataUtils.makeSensorDataProperty("interpolated", "true");
      props.getProperty().add(interpolatedProp);
    }
    return SensorDataUtils.makeSensorData(timestamp, "WattDepot Server", source, props);
  }

  /**
   * Takes a List of SensorDataStraddles, sums up the powerGenerated and powerConsumed properties of
   * each straddle, and returns a new SensorData object with those sums. Generally used on the
   * output of DbImplementation.getSensorDataStraddleList().
   * 
   * @param straddleList The list of straddles to process.
   * @param source The URI of the Source (needed to create the SensorData).
   * @return The newly created SensorData object.
   */
  public static SensorData getPowerFromList(List<SensorDataStraddle> straddleList, String source) {
    if (straddleList == null) {
      return null;
    }
    double powerGenerated = 0, powerConsumed = 0;
    boolean wasInterpolated = false;
    XMLGregorianCalendar timestamp;
    if (straddleList.isEmpty()) {
      return null;
    }
    else {
      // All timestamps are the same, so we just need one
      timestamp = straddleList.get(0).getTimestamp();
      for (SensorDataStraddle straddle : straddleList) {
        if (straddle.isDegenerate()) {
          powerGenerated +=
              straddle.getBeforeData().getProperties().getPropertyAsDouble("powerGenerated");
          powerConsumed +=
              straddle.getBeforeData().getProperties().getPropertyAsDouble("powerConsumed");
        }
        else {
          // If any of the straddles were non-degenerate, then set the whole thing to interpolated
          wasInterpolated = true;
          powerGenerated += straddle.getPowerGenerated();
          powerConsumed += straddle.getPowerConsumed();
        }
      }
      return makePowerSensorData(timestamp, source, powerGenerated, powerConsumed, wasInterpolated);
    }
  }
}
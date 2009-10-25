package org.wattdepot.resource.carbon;

import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;
import org.wattdepot.resource.energy.Energy;
import org.wattdepot.resource.sensordata.SensorDataStraddle;
import org.wattdepot.resource.sensordata.SensorDataUtils;
import org.wattdepot.resource.sensordata.StraddleList;
import org.wattdepot.resource.sensordata.jaxb.Properties;
import org.wattdepot.resource.sensordata.jaxb.Property;
import org.wattdepot.resource.sensordata.jaxb.SensorData;
import org.wattdepot.resource.source.jaxb.Source;

/**
 * Represents the carbon emitted between two straddles. Currently the carbon emitted is calculated
 * by computing the energy, and then multiplying it by the carbonIntensity property of the source.
 * 
 * @author Robert Brewer
 */
public class Carbon extends Energy {

  /** The carbon intensity of the Source this Carbon belongs to. */
  private double carbonIntensity;

  private static final double MEGA = 1E6;

  /**
   * Creates a new Carbon object given a start and end straddle.
   * 
   * @param startStraddle The start straddle.
   * @param endStraddle The end straddle.
   * @param carbonIntensity The carbon intensity for this source.
   * @throws IllegalArgumentException If either straddle given is null.
   */
  public Carbon(SensorDataStraddle startStraddle, SensorDataStraddle endStraddle,
      double carbonIntensity) {
    super(startStraddle, endStraddle);
    this.carbonIntensity = carbonIntensity;
  }

  /**
   * Computes the amount of carbon emitted between the two straddles by computing energy between the
   * straddles, and multiplying by the carbon intensity.
   * 
   * @return The carbon emitted between the straddles in lbs CO2 equivalent.
   */
  public double getCarbonEmitted() {
    double energyGenerated = this.getEnergyGenerated();

    // carbonIntensity is in lbs per MWh, so convert to MWh and then multiply
    return (energyGenerated / MEGA) * this.carbonIntensity;
  }

  /**
   * Returns a SensorData object representing the carbon emitted between the straddles.
   * 
   * @return The SensorData object representing carbon emitted.
   */
  public SensorData getCarbon() {
    double carbonEmittedValue = getCarbonEmitted();

    return makeCarbonSensorData(this.startStraddle.getTimestamp(), this.startStraddle
        .getBeforeData().getSource(), carbonEmittedValue, true);
  }

  /**
   * Creates an SensorData object that contains the given carbonEmitted value.
   * 
   * @param timestamp The timestamp of the SensorData to be created.
   * @param source The source URI of the SensorData to be created.
   * @param carbonEmittedValue The amount of carbon emitted.
   * @param interpolated True if the values were determined by interpolation, false otherwise.
   * @return The new SensorData object.
   */
  public static SensorData makeCarbonSensorData(XMLGregorianCalendar timestamp, String source,
      double carbonEmittedValue, boolean interpolated) {
    Properties props = new Properties();
    Property emittedProp, interpolatedProp;
    emittedProp =
        SensorDataUtils
            .makeSensorDataProperty("carbonEmitted", Double.toString(carbonEmittedValue));
    props.getProperty().add(emittedProp);
    if (interpolated) {
      interpolatedProp = SensorDataUtils.makeSensorDataProperty("interpolated", "true");
      props.getProperty().add(interpolatedProp);
    }
    return SensorDataUtils.makeSensorData(timestamp, "WattDepot Server", source, props);
  }

  /**
   * Takes a List of SensorDataStraddles, computes and sums up the carbon emitted between each
   * straddle, and returns the sum as a double.
   * 
   * @param straddleList The list of straddles to process.
   * @param carbonIntensity The carbon intensity of the source these straddles come from.
   * @return The newly created SensorData object.
   */
  public static double getCarbonFromList(List<SensorDataStraddle> straddleList,
      double carbonIntensity) {
    if (straddleList == null) {
      throw new IllegalArgumentException("Attempt to compute carbon from null list");
    }
    double carbonEmitted = 0;
    Carbon carbon;
    if (straddleList.isEmpty()) {
      throw new IllegalArgumentException("Attempt to compute carbon from empty list");
    }
    else {
      // iterate over list of straddles (note that i never reaches the max index)
      for (int i = 0; i < (straddleList.size() - 1); i++) {
        // making Carbon objects of each pair of straddle
        carbon = new Carbon(straddleList.get(i), straddleList.get(i + 1), carbonIntensity);
        carbonEmitted += carbon.getCarbonEmitted();
      }
      return carbonEmitted;
    }
  }

  /**
   * Takes a List of StraddleLists, computes and sums up the carbon emitted for each list of
   * straddles, and returns a new SensorData object with those sums.
   * 
   * @param masterList The list of StraddleLists to process.
   * @param sourceUri The URI of the Source (needed to create the SensorData).
   * @return The newly created SensorData object.
   */
  public static SensorData getCarbonFromStraddleList(List<StraddleList> masterList, String sourceUri) {
    if (masterList == null) {
      return null;
    }
    double carbonEmitted = 0;
    boolean wasInterpolated = true;
    XMLGregorianCalendar timestamp;
    Source source;
    if (masterList.isEmpty()) {
      return null;
    }
    else {
      timestamp = masterList.get(0).getStraddleList().get(0).getTimestamp();
      // iterate over the list of StraddleLists (each one corresponding to a different source)
      for (StraddleList list : masterList) {
        source = list.getSource();
        carbonEmitted +=
            getCarbonFromList(list.getStraddleList(), source.getProperties().getPropertyAsDouble(
                "carbonIntensity"));
      }
      return makeCarbonSensorData(timestamp, sourceUri, carbonEmitted, wasInterpolated);
    }
  }
}

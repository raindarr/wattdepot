package org.wattdepot.datainput;

/**
 * Holds all configuration properties for one meter to one source.
 * 
 * @author Andrea Connell
 * 
 */
public class SensorSource {

  private String key;
  private String name;
  private int updateRate;
  private String meterHostname;

  /**
   * Creates a SensorSource with the given key.
   * 
   * @param key The key for this SensorSource.
   */
  public SensorSource(String key) {
    this.key = key;
  }

  /**
   * Returns the key.
   * 
   * @return The value of the key.
   */
  public String getKey() {
    return key;
  }

  /**
   * Sets the key.
   * 
   * @param key The value to set the key to.
   */
  public void setKey(String key) {
    this.key = key;
  }

  /**
   * Returns the source name.
   * 
   * @return The name of the source.
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the source name.
   * 
   * @param name The value to set the source name to.
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Returns the update rate.
   * 
   * @return The update rate.
   */
  public int getUpdateRate() {
    return updateRate;
  }

  /**
   * Sets the udpate rate.
   * 
   * @param updateRate The value to set the update rate to.
   */
  public void setUpdateRate(int updateRate) {
    this.updateRate = updateRate;
  }

  /**
   * Returns the meter host name.
   * 
   * @return The host name of the meter.
   */
  public String getMeterHostname() {
    return meterHostname;
  }

  /**
   * Sets the meter host name.
   * 
   * @param meterHostname The value to set the meter host name to.
   */
  public void setMeterHostname(String meterHostname) {
    this.meterHostname = meterHostname;
  }
}

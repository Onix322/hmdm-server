/*
 *
 * Headwind MDM: Open Source Android MDM Software
 * https://h-mdm.com
 *
 * Copyright (C) 2019 Headwind Solutions LLC (http://h-sms.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.hmdm.plugins.deviceinfo.persistence.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hmdm.persistence.domain.CustomerData;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

/**
 * A domain object representing a single collection of <code>Device Info</code> plugin settings per
 * customer account.
 *
 * @author isv
 */
@JsonIgnoreProperties(
    ignoreUnknown = true,
    value = {"customerId"})
public class DeviceInfoPluginSettings implements CustomerData, Serializable {

  private static final long serialVersionUID = 7558374403579325390L;

  /** An ID of a setting record. */
  @Schema(description = "An ID of a setting record.")
  private Integer id;

  /** An ID of a customer account which the record belongs to. */
  @Schema(hidden = true)
  private int customerId;

  /** A period for preserving the data records in persistent data store (in days). */
  @Schema(
      description = "A period for preserving the data records in persistent data store (in days)",
      required = true)
  private int dataPreservePeriod = 30;

  /** An interval for transmitting data by device (in minutes) */
  @Schema(description = "An interval for transmitting data by device (in minutes)", required = true)
  private int intervalMins = 15;

  /** A flag indicating if device must send dynamic data or not. */
  @Schema(
      description = "A flag indicating if device must send dynamic data or not",
      required = true)
  private boolean sendData = false;

  /**
   * Constructs new <code>DeviceInfoPluginSettings</code> instance. This implementation does
   * nothing.
   */
  public DeviceInfoPluginSettings() {}

  @Override
  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  @Override
  public int getCustomerId() {
    return customerId;
  }

  @Override
  public void setCustomerId(int customerId) {
    this.customerId = customerId;
  }

  public int getDataPreservePeriod() {
    return dataPreservePeriod;
  }

  public void setDataPreservePeriod(int dataPreservePeriod) {
    this.dataPreservePeriod = dataPreservePeriod;
  }

  public int getIntervalMins() {
    return intervalMins;
  }

  public void setIntervalMins(int intervalMins) {
    this.intervalMins = intervalMins;
  }

  public boolean isSendData() {
    return sendData;
  }

  public void setSendData(boolean sendData) {
    this.sendData = sendData;
  }

  @Override
  public String toString() {
    return "DeviceInfoPluginSettings{"
        + "id="
        + id
        + ", customerId="
        + customerId
        + ", dataPreservePeriod="
        + dataPreservePeriod
        + ", intervalMins="
        + intervalMins
        + ", sendData="
        + sendData
        + '}';
  }
}

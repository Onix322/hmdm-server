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

package com.hmdm.rest.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.hmdm.persistence.domain.ConfigurationFile;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

@Schema(
    description =
        "A single configuration file to be used on mobile device and used in data "
            + "synchronization between mobile device and server application")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SyncConfigurationFile implements Serializable, SyncConfigurationFileInt {

  @JsonIgnore private final ConfigurationFile wrapped;

  /**
   * Constructs new <code>SyncConfigurationFile</code> instance. This implementation does nothing.
   */
  public SyncConfigurationFile(ConfigurationFile file) {
    this.wrapped = file;
  }

  /** A description of the file. */
  @Override
  @Schema(description = "A description of the file")
  public String getDescription() {
    // return wrapped.getDescription();
    // Not required in the mobile app
    return null;
  }

  /** A checksum for the file content. */
  @Override
  @Schema(description = "A checksum for the file content")
  public String getChecksum() {
    return wrapped.getChecksum();
  }

  /** A flag indicating if file is to be removed from the device or not. */
  @Override
  @Schema(description = "A flag indicating if file is to be removed from the device or not")
  public Boolean getRemove() {
    return wrapped.isRemove() ? true : null;
  }

  /** A timestamp of file uploading to server (in milliseconds since epoch time). */
  @Override
  @Schema(
      description = "A timestamp of file uploading to server (in milliseconds since epoch time)")
  public Long getLastUpdate() {
    return wrapped.getLastUpdate();
  }

  /** A path to a file on device (including the name of the file). */
  @Schema(description = "A path to a file on device")
  public String getPath() {
    return wrapped.getDevicePath();
  }

  /** An URL referencing the content of the file. */
  @Schema(description = "An URL referencing the content of the file")
  public String getUrl() {
    return wrapped.getUrl();
  }

  /** A flag indicating if file is to be removed from the device or not. */
  @Override
  @Schema(
      description =
          "A flag indicating whether the file content must be updated by device-specific values")
  public Boolean getVarContent() {
    return wrapped.isReplaceVariables() ? true : null;
  }
}

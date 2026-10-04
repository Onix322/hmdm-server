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

package com.hmdm.persistence.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hmdm.rest.json.FileConfigurationLink;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import lombok.Data;

/** A configuration file to be sent to mobile device for usage */
@Schema(description = "A configuration file to be sent to mobile device for usage")
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class ConfigurationFile implements Serializable {

  /** An ID of configuration file. */
  @Schema(description = "A configuration file ID")
  //    @JsonIgnore
  private Integer id;

  /** An ID of a configuration record associated with this file. */
  @Schema(hidden = true)
  @JsonIgnore
  private int configurationId;

  /** A description of the file. Since v5.36.1, determined in UploadedFile linked via fileId */
  @Schema(description = "A description of the file")
  private String description;

  /**
   * A path to a file on device (including the name of the file). Since v5.36.1, determined in
   * UploadedFile
   */
  @Schema(description = "A path to a file on device (including the file name)")
  @JsonProperty("path")
  private String devicePath;

  /** A flag specifying whether the file's device path should be overridden in this configuration */
  @Schema(description = "A path to a file on device (including the file name)")
  @JsonProperty("overridePath")
  private boolean overrideDevicePath;

  /**
   * An URL referencing the content of the file available on external resource. This property is
   * mutually exclusive with {
   *
   * @link #filePath} property. Since v5.36.1, determined in UploadedFile
   */
  @Schema(hidden = true)
  private String externalUrl;

  /**
   * A path to a file relative to base directory for stored files. This property is mutually
   * exclusive with {
   *
   * @link #externalUrl} property. Since v5.36.1, determined in UploadedFile
   */
  @Schema(hidden = true)
  private String filePath;

  /**
   * A checksum for the file content. DEPRECATED since v5.36.1 - checksum isn't used due to possible
   * variable content, use lastUpdate instead
   */
  @Schema(description = "A checksum for the file content")
  @Deprecated
  private String checksum;

  /** A flag indicating if file is to be removed from the device or not. */
  @Schema(description = "A flag indicating if file is to be removed from the device or not")
  private boolean remove;

  /**
   * A timestamp of file uploading to server (in milliseconds since epoch time). Since v5.36.1,
   * determined in UploadedFile
   */
  @Schema(
      description = "A timestamp of file uploading to server (in milliseconds since epoch time)")
  private Long lastUpdate;

  /** An ID of an uploaded file storing the content of the file. */
  @Schema(hidden = true)
  private Integer fileId;

  /** An URL referencing the content of the file. Since v5.36.1, determined in UploadedFile */
  @Schema(description = "An URL referencing the content of the file")
  private String url;

  /**
   * A flag indicating whether the file content must be updated by device-specific values. Since
   * v5.36.1, determined in UploadedFile
   */
  @Schema(
      description =
          "A flag indicating whether the file content must be updated by device-specific values")
  private boolean replaceVariables;

  /** Default constructor */
  public ConfigurationFile() {}

  /** Constructor from Link (could be from File and Link in the future) */
  public ConfigurationFile(FileConfigurationLink link) {
    configurationId = link.getConfigurationId();
    fileId = link.getFileId();
    remove = link.isRemove();
  }
}

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

package com.hmdm.guice;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.Resource;
import liquibase.resource.URIResource;

/**
 * A loader for Liquibase resources which are contained within the JAR-files.
 *
 * @author isv
 */
public class LiquibaseJARResourceAccessor extends ClassLoaderResourceAccessor {

  /**
   * Constructs new <code>LiquibaseJARResourceAccessor</code> instance. This implementation does
   * nothing.
   */
  public LiquibaseJARResourceAccessor() {}

  @Override
  public List<Resource> search(String path, boolean recursive) throws IOException {
    try {
      // În Java 21, `new URL(path)` este deprecated; se folosește `URI.create().toURL()`
      URL url = URI.create(path).toURL();

      // Cream un Resource pe baza URL-ului
      Resource resource = new URIResource(path, url.toURI());

      return List.of(resource);
    } catch (Exception e) {
      return Collections.emptyList();
    }
  }

  @Override
  public List<Resource> getAll(String path) throws IOException {
    return search(path, false);
  }

  /** Not deleted after porting to java 21 to keep compatibility */
  public Set<InputStream> getResourcesAsStream(String path) throws IOException {
    return getAll(path).stream()
        .map(
            resource -> {
              try {
                return resource.openInputStream();
              } catch (IOException e) {
                throw new RuntimeException(
                    "Could not open stream for resource: " + resource.getPath(), e);
              }
            })
        .collect(Collectors.toSet());
  }
}

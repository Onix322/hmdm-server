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

package com.hmdm.plugins.audit.rest.filter;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A wrapper around the servlet response stream used for capturing the content of the response.
 *
 * @author isv
 */
public class ServletOutputStreamWrapper extends ServletOutputStream {

  /** An original servlet response stream wrapped by this wrapper. */
  private OutputStream outputStream;

  /** A copy of the response content collected while client code writes to response stream. */
  private ByteArrayOutputStream copy;

  public ServletOutputStreamWrapper(OutputStream outputStream) {
    this.outputStream = outputStream;
    this.copy = new ByteArrayOutputStream(1024);
  }

  @Override
  public void write(int b) throws IOException {
    outputStream.write(b);
    copy.write(b);
  }

  /**
   * Gets the content of the response.
   *
   * @return a content of the response.
   */
  public byte[] getContent() {
    return copy.toByteArray();
  }

  @Override
  public boolean isReady() {
    return false;
  }

  @Override
  public void setWriteListener(WriteListener arg0) {
    // stub
  }
}

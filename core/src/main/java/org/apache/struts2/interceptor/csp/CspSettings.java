/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.struts2.interceptor.csp;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * CspSettings interface used by the {@link CspInterceptor} to add the CSP header to the response.
 * The default implementation can be found in {@link DefaultCspSettings}.
 *
 * @see DefaultCspSettings
 */
public interface CspSettings {

    int NONCE_RANDOM_LENGTH = 18;

    String CSP_ENFORCE_HEADER = "Content-Security-Policy";
    String CSP_REPORT_HEADER = "Content-Security-Policy-Report-Only";
    String OBJECT_SRC = "object-src";
    String SCRIPT_SRC = "script-src";
    String BASE_URI = "base-uri";
    String REPORT_URI = "report-uri";
    String REPORT_TO = "report-to";
    String NONE = "none";
    String STRICT_DYNAMIC = "strict-dynamic";
    String HTTP = "http:";
    String HTTPS = "https:";
    String CSP_REPORT_TYPE = "application/csp-report";

    /**
     * @deprecated use {@link #addCspHeaders(HttpServletRequest, HttpServletResponse)} instead
     */
    @Deprecated
    void addCspHeaders(HttpServletResponse response);

    void addCspHeaders(HttpServletRequest request, HttpServletResponse response);

    /**
     * Sets the uri where csp violation reports will be sent
     */
    void setReportUri(String uri);

    /**
     * Sets the report group where csp violation reports will be sent
     *
     * @since Struts 6.5.0
     */
    void setReportTo(String group);

    /**
     * Sets CSP headers in enforcing mode when true, and report-only when false
     */
    void setEnforcingMode(boolean value);
}

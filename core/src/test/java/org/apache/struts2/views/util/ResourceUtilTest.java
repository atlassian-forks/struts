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
package org.apache.struts2.views.util;
/**
 * <code>ResourceUtilTest</code>
 *
 */
import jakarta.servlet.http.HttpServletRequest;

import junit.framework.TestCase;

import static org.easymock.EasyMock.*;

public class ResourceUtilTest extends TestCase {

    private HttpServletRequest requestMock;

    public void testGetResourceBase() throws Exception {
        expect(requestMock.getServletPath()).andReturn("/mycontext/");
        expect(requestMock.getRequestURI()).andReturn("/mycontext/");
        replay(requestMock);
        assertEquals("/mycontext", ResourceUtil.getResourceBase(requestMock));
        verify(requestMock);

        reset(requestMock);

        expect(requestMock.getServletPath()).andReturn("/mycontext/test.jsp");
        expect(requestMock.getRequestURI()).andReturn("/mycontext/test.jsp");
        replay(requestMock);
        
        assertEquals("/mycontext", ResourceUtil.getResourceBase(requestMock));
        verify(requestMock);

    }


    protected void setUp() {
        requestMock = (HttpServletRequest) createMock(HttpServletRequest.class);
    }
}

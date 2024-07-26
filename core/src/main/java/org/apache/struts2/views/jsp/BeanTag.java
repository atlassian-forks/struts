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
package org.apache.struts2.views.jsp;

import com.opensymphony.xwork2.util.ValueStack;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.struts2.components.Bean;
import org.apache.struts2.components.Component;

import java.io.Serial;

/**
 * @see Bean
 */
public class BeanTag extends ContextBeanTag {

    @Serial
    private static final long serialVersionUID = -3863152522071209267L;

    protected static Logger LOG = LogManager.getLogger(BeanTag.class);

    protected String name;

    @Override
    public Component getBean(ValueStack stack, HttpServletRequest req, HttpServletResponse res) {
        return new Bean(stack);
    }

    @Override
    protected void populateParams() {
        super.populateParams();

        ((Bean) component).setName(name);
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Must declare the setter at the descendant Tag class level in order for the tag handler to locate the method.
     */
    @Override
    public void setPerformClearTagStateForTagPoolingServers(boolean performClearTagStateForTagPoolingServers) {
        super.setPerformClearTagStateForTagPoolingServers(performClearTagStateForTagPoolingServers);
    }

    @Override
    protected void clearTagStateForTagPoolingServers() {
       if (!getPerformClearTagStateForTagPoolingServers()) {
            return;  // If flag is false (default setting), do not perform any state clearing.
        }
        super.clearTagStateForTagPoolingServers();
        this.name = null;
    }

}

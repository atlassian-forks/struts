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
package com.opensymphony.xwork2.ognl;

import com.opensymphony.xwork2.inject.Inject;
import ognl.Node;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.struts2.StrutsConstants;

import java.util.HashSet;
import java.util.Set;

import static com.opensymphony.xwork2.util.TextParseUtil.commaDelimitedStringToSet;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;

/**
 * The default implementation of {@link OgnlGuard}.
 *
 * @since 6.4.0
 */
public class DefaultOgnlGuard implements OgnlGuard {

    private static final Logger LOG = LogManager.getLogger(DefaultOgnlGuard.class);

    private Set<String> excludedNodeTypes = emptySet();

    @Inject(value = StrutsConstants.STRUTS_OGNL_EXCLUDED_NODE_TYPES, required = false)
    public void useExcludedNodeTypes(String excludedNodeTypes) {
        Set<String> incomingExcludedNodeTypes = commaDelimitedStringToSet(excludedNodeTypes);
        Set<String> newExcludeNodeTypes = new HashSet<>(this.excludedNodeTypes);
        newExcludeNodeTypes.addAll(incomingExcludedNodeTypes);
        this.excludedNodeTypes = unmodifiableSet(newExcludeNodeTypes);
    }

    @Override
    public boolean isBlocked(String expr, Object tree) {
        return containsExcludedNodeType(tree);
    }

    protected boolean containsExcludedNodeType(Object tree) {
        if (!(tree instanceof Node) || excludedNodeTypes.isEmpty()) {
            return false;
        }
        return recurseExcludedNodeType((Node) tree);
    }

    protected boolean recurseExcludedNodeType(Node node) {
        String nodeClassName = node.getClass().getName();
        if (excludedNodeTypes.contains(nodeClassName)) {
            LOG.warn("Expression contains blocked node type [{}]", nodeClassName);
            return true;
        } else {
            for (int i = 0; i < node.jjtGetNumChildren(); i++) {
                if (containsExcludedNodeType(node.jjtGetChild(i))) {
                    return true;
                }
            }
            return false;
        }
    }
}

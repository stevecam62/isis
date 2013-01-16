/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.isis.viewer.scimpi.dispatcher.view.simple;

import org.apache.isis.viewer.scimpi.dispatcher.context.Request;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request.Scope;
import org.apache.isis.viewer.scimpi.dispatcher.processor.TagProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.view.AbstractElementProcessor;

public class CookieValue extends AbstractElementProcessor {

    @Override
    public void process(final TagProcessor tagProcessor) {
        String name = tagProcessor.getRequiredProperty(NAME);
        String resultName = tagProcessor.getOptionalProperty(RESULT_NAME, name);
        final String scopeName = tagProcessor.getOptionalProperty(SCOPE);
        final Scope scope = Request.scope(scopeName, Scope.REQUEST);
        
        String cookieString = tagProcessor.getContext().getCookie(name);
        
        tagProcessor.appendDebug("    " + resultName + " (" + scope + ") set to " + cookieString + " from cookie " + name);
        tagProcessor.getContext().addVariable(resultName, cookieString, scope);
    }

    @Override
    public String getName() {
        return "cookie-value";
    }
}

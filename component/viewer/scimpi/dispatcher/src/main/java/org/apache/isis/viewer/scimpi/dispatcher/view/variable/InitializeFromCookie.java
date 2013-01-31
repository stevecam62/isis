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

package org.apache.isis.viewer.scimpi.dispatcher.view.variable;

import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.runtime.persistence.ObjectNotFoundException;
import org.apache.isis.viewer.scimpi.Names;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request;
import org.apache.isis.viewer.scimpi.dispatcher.context.RequestState;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request.Scope;
import org.apache.isis.viewer.scimpi.dispatcher.processor.TemplateProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.view.AbstractElementProcessor;

public class InitializeFromCookie extends AbstractElementProcessor {
    private static final String SEVEN_DAYS = Integer.toString(60 * 24 * 7);

    @Override
    public void process(final TemplateProcessor templateProcessor, RequestState state) {
        final String name = templateProcessor.getRequiredProperty(NAME);

        final Request context = templateProcessor.getContext();
        if (context.getVariable(name) != null) {
            templateProcessor.skipUntilClose();
        } else {
            final String scopeName = templateProcessor.getOptionalProperty(SCOPE);
            final Scope scope = Request.scope(scopeName, Scope.SESSION);

            final String cookieName = templateProcessor.getOptionalProperty("cookie", name);
            final String cookieValue = context.getCookie(cookieName);
            boolean hasObject;
            if (cookieValue != null) {
                try {
                    context.getMappedObject(cookieValue);
                    hasObject = true;
                } catch (final ObjectNotFoundException e) {
                    hasObject = false;
                }
            } else {
                hasObject = false;
            }

            if (hasObject) {
                templateProcessor.skipUntilClose();
                context.addVariable(name, cookieValue, scope);
            } else {
                final String expiresString = templateProcessor.getOptionalProperty("expires", SEVEN_DAYS);
                templateProcessor.pushNewBuffer();
                templateProcessor.processUtilCloseTag();
                templateProcessor.popBuffer();
                final String id = (String) context.getVariable(Names.RESULT);
                final ObjectAdapter variable = context.getMappedObject(id);
                if (variable != null) {
                    context.addCookie(cookieName, id, Integer.valueOf(expiresString));
                    context.addVariable(name, id, scope);
                }
            }
        }
    }

    @Override
    public String getName() {
        return "initialize-from-cookie";
    }

}

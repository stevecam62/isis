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
import org.apache.isis.viewer.scimpi.Names;
import org.apache.isis.viewer.scimpi.ScimpiException;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request;
import org.apache.isis.viewer.scimpi.dispatcher.context.RequestState;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request.Scope;
import org.apache.isis.viewer.scimpi.dispatcher.processor.TemplateProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.view.AbstractElementProcessor;

public class InitializeFromResult extends AbstractElementProcessor {

    @Override
    public void process(final TemplateProcessor templateProcessor, RequestState state) {
        disallowSourceAndDefault(templateProcessor);
        final String sourceObjectId = objectOrResult(templateProcessor);
        final Class<?> cls = forClass(templateProcessor);
        final String variableName = templateProcessor.getRequiredProperty(NAME);
        final String defaultObjectId = templateProcessor.getOptionalProperty(DEFAULT);
        final String scopeName = templateProcessor.getOptionalProperty(SCOPE);
        final Scope scope = Request.scope(scopeName, Scope.REQUEST);

        final Request context = templateProcessor.getContext();
        final ObjectAdapter sourceObject = context.getMappedObject(sourceObjectId);
        final boolean isSourceSet = sourceObject != null;
        final boolean isSourceAssignable = isSourceSet && (cls == null || cls.isAssignableFrom(sourceObject.getObject().getClass()));
        if (isSourceAssignable) {
            templateProcessor.appendDebug("     " + variableName + " set to " + sourceObjectId + " (" + scope + ")");
            context.addVariable(variableName, sourceObjectId, scope);
        } else {
            templateProcessor.appendDebug("     " + variableName + " set to " + sourceObjectId + " (" + scope + ")");
            if (defaultObjectId != null) {
                context.addVariable(variableName, defaultObjectId, scope);
            }
            context.changeScope(variableName, scope);
        }
    }

    private String objectOrResult(final TemplateProcessor templateProcessor) {
        final String sourceObjectId = templateProcessor.getOptionalProperty(OBJECT);
        if (sourceObjectId == null) {
            return (String) templateProcessor.getContext().getVariable(Names.RESULT);
        } else {
            return sourceObjectId;
        }
    }

    private void disallowSourceAndDefault(final TemplateProcessor templateProcessor) {
        if (templateProcessor.getOptionalProperty(DEFAULT) != null && templateProcessor.getOptionalProperty(OBJECT) != null) {
            throw new ScimpiException("Cannot specify both " + OBJECT + " and " + DEFAULT + " for the " + getName() + " element");
        }
    }

    @Override
    public String getName() {
        return "initialize";
    }

}

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

package org.apache.isis.viewer.scimpi.dispatcher.view.action;

import java.util.List;

import org.apache.isis.applib.annotation.Where;
import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.spec.feature.ObjectAction;
import org.apache.isis.core.metamodel.spec.feature.ObjectActionParameter;
import org.apache.isis.viewer.scimpi.ForbiddenException;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request.Scope;
import org.apache.isis.viewer.scimpi.dispatcher.processor.TagProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.util.MethodsUtils;
import org.apache.isis.viewer.scimpi.dispatcher.view.AbstractElementProcessor;

public class RunAction extends AbstractElementProcessor {

    // REVIEW: should provide this rendering context, rather than hardcoding.
    // the net effect currently is that class members annotated with 
    // @Hidden(where=Where.ANYWHERE) or @Disabled(where=Where.ANYWHERE) will indeed
    // be hidden/disabled, but will be visible/enabled (perhaps incorrectly) 
    // for any other value for Where
    private final Where where = Where.ANYWHERE;

    @Override
    public void process(final TagProcessor tagProcessor) {
        final Request context = tagProcessor.getContext();

        final String objectId = tagProcessor.getOptionalProperty(OBJECT);
        final ObjectAdapter object = MethodsUtils.findObject(context, objectId);

        final String methodName = tagProcessor.getRequiredProperty(METHOD);
        final ObjectAction action = MethodsUtils.findAction(object, methodName);

        final String variableName = tagProcessor.getOptionalProperty(RESULT_NAME);
        final String scopeName = tagProcessor.getOptionalProperty(SCOPE);

        final ActionContent parameterBlock = new ActionContent(action);
        tagProcessor.setBlockContent(parameterBlock);
        tagProcessor.processUtilCloseTag();
        final ObjectAdapter[] parameters = parameterBlock.getParameters(tagProcessor);

        if (!MethodsUtils.isVisibleAndUsable(object, action, where)) {
            throw new ForbiddenException(action, ForbiddenException.VISIBLE_AND_USABLE);
        }

        // swap null parameter of the object's type to run a contributed method
        if (action.isContributed()) {
            final List<ObjectActionParameter> parameterSpecs = action.getParameters();
            for (int i = 0; i < parameters.length; i++) {
                if (parameters[i] == null && object.getSpecification().isOfType(parameterSpecs.get(i).getSpecification())) {
                    parameters[i] = object;
                    break;
                }
            }
        }

        final Scope scope = Request.scope(scopeName, Scope.REQUEST);
        MethodsUtils.runMethod(context, action, object, parameters, variableName, scope);
        tagProcessor.popBlockContent();
    }

    @Override
    public String getName() {
        return "run-action";
    }

}

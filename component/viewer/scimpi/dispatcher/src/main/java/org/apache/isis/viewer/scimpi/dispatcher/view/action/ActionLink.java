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

import java.net.URLEncoder;

import org.apache.isis.applib.annotation.Where;
import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.spec.feature.ObjectAction;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request.Scope;
import org.apache.isis.viewer.scimpi.dispatcher.processor.TagProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.util.MethodsUtils;
import org.apache.isis.viewer.scimpi.dispatcher.view.AbstractElementProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.view.other.HelpLink;

public class ActionLink extends AbstractElementProcessor {

    // REVIEW: confirm this rendering context
    private final Where where = Where.OBJECT_FORMS;

    @Override
    public void process(final TagProcessor tagProcessor) {
        String objectId = tagProcessor.getOptionalProperty(OBJECT);
        final String method = tagProcessor.getOptionalProperty(METHOD);
        final String forwardResultTo = tagProcessor.getOptionalProperty(VIEW);
        final String forwardVoidTo = tagProcessor.getOptionalProperty(VOID);
        String resultOverride = tagProcessor.getOptionalProperty(RESULT_OVERRIDE);
        
        final String resultName = tagProcessor.getOptionalProperty(RESULT_NAME);
        final String resultNameSegment = resultName == null ? "" : "&amp;" + RESULT_NAME + "=" + resultName;
        final String scope = tagProcessor.getOptionalProperty(SCOPE);
        final String scopeSegment = scope == null ? "" : "&amp;" + SCOPE + "=" + scope;
        final String confirm = tagProcessor.getOptionalProperty(CONFIRM);
        final String completionMessage = tagProcessor.getOptionalProperty(MESSAGE);

        // TODO need a mechanism for globally dealing with encoding; then use
        // the new encode method
        final String confirmSegment = confirm == null ? "" : "&amp;" + "_" + CONFIRM + "=" + URLEncoder.encode(confirm);
        final String messageSegment = completionMessage == null ? "" : "&amp;" + "_" + MESSAGE + "=" + URLEncoder.encode(completionMessage);

        final Request context = tagProcessor.getContext();
        final ObjectAdapter object = MethodsUtils.findObject(context, objectId);
        final String version = context.mapVersion(object);
        final ObjectAction action = MethodsUtils.findAction(object, method);
        objectId = tagProcessor.getContext().mapObject(object, Scope.REQUEST);

        final ActionContent parameterBlock = new ActionContent(action);
        tagProcessor.setBlockContent(parameterBlock);
        tagProcessor.pushNewBuffer();
        tagProcessor.processUtilCloseTag();
        final String text = tagProcessor.popBuffer();
        
        final String[] parameters = parameterBlock.getParameters();
        final String target;
        if (action.isContributed()) {
            System.arraycopy(parameters, 0, parameters, 1, parameters.length - 1);
            parameters[0] = tagProcessor.getContext().mapObject(object, Scope.REQUEST);
            target =  tagProcessor.getContext().mapObject(action.realTarget(object), Scope.REQUEST);
            if (!action.hasReturn() && resultOverride == null) {
                resultOverride = parameters[0];
            }
        } else {
            target = objectId;
        }

        if (MethodsUtils.isVisibleAndUsable(object, action, where)) {
            writeLink(tagProcessor, target, version, method, forwardResultTo, forwardVoidTo, resultNameSegment, resultOverride, scopeSegment,
                    confirmSegment, messageSegment, context, action, parameters, text);
        }
        tagProcessor.popBlockContent();
    }

    public static void writeLink(
            final TagProcessor tagProcessor,
            final String objectId,
            final String version,
            final String method,
            final String forwardResultTo,
            final String forwardVoidTo,
            final String resultNameSegment,
            final String resultOverride,
            final String scopeSegment,
            final String confirmSegment,
            final String messageSegment,
            final Request context,
            final ObjectAction action,
            final String[] parameters,
            String text) {
        text = text == null || text.trim().equals("") ? action.getName() : text;

        String parameterSegment = "";
        for (int i = 0; i < parameters.length; i++) {
            parameterSegment += "&param" + (i + 1) + "=" + parameters[i];
        }

        final String interactionParamters = context.encodedInteractionParameters();
        final String forwardResultSegment = forwardResultTo == null ? "" : "&amp;" + "_" + VIEW + "=" + context.fullFilePath(forwardResultTo);
        final String resultOverrideSegment = resultOverride == null ? "" : "&amp;" + "_" + RESULT_OVERRIDE + "=" + resultOverride;
        final String voidView = context.fullFilePath(forwardVoidTo == null ? context.getResourceFile() : forwardVoidTo);
        final String forwardVoidSegment = "&amp;" + "_" + VOID + "=" + voidView;
        tagProcessor.appendHtml("<a href=\"action.app?" + "_" + OBJECT + "=" + objectId + "&amp;" + "_" + VERSION + "=" + version
                + "&amp;" + "_" + METHOD + "=" + method + resultOverrideSegment + forwardResultSegment + forwardVoidSegment + resultNameSegment
                + parameterSegment + scopeSegment + confirmSegment + messageSegment + interactionParamters + "\">");
        tagProcessor.appendHtml(text);
        tagProcessor.appendHtml("</a>");
        HelpLink.append(tagProcessor, action.getDescription(), action.getHelp());
    }

    @Override
    public String getName() {
        return "action-link";
    }

}

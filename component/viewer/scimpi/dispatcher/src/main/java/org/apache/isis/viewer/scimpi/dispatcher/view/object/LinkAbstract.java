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

package org.apache.isis.viewer.scimpi.dispatcher.view.object;

import org.apache.isis.applib.annotation.Where;
import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.spec.feature.ObjectAssociation;
import org.apache.isis.core.runtime.system.context.IsisContext;
import org.apache.isis.viewer.scimpi.ForbiddenException;
import org.apache.isis.viewer.scimpi.Names;
import org.apache.isis.viewer.scimpi.ScimpiException;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request;
import org.apache.isis.viewer.scimpi.dispatcher.context.RequestState;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request.Scope;
import org.apache.isis.viewer.scimpi.dispatcher.processor.TemplateProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.util.MethodsUtils;
import org.apache.isis.viewer.scimpi.dispatcher.view.AbstractElementProcessor;

public abstract class LinkAbstract extends AbstractElementProcessor {

    @Override
    public void process(final TemplateProcessor templateProcessor, RequestState state) {
        final String title = templateProcessor.getOptionalProperty(FORM_TITLE);
        final String name = templateProcessor.getOptionalProperty(NAME);
        final boolean showAsButton = templateProcessor.isRequested("show-as-button", false);
        final String linkClass = templateProcessor.getOptionalProperty(CLASS, showAsButton ? "button" : defaultLinkClass());
        final String containerClass = templateProcessor.getOptionalProperty(CONTAINER_CLASS, "action");
        final String object = templateProcessor.getOptionalProperty(OBJECT);
        final Request context = templateProcessor.getContext();
        String objectId = object != null ? object : (String) context.getVariable(Names.RESULT);
        ObjectAdapter adapter = MethodsUtils.findObject(context, objectId);

        // REVIEW this is common used code
        final String fieldName = templateProcessor.getOptionalProperty(FIELD);
        if (fieldName != null) {
            final ObjectAssociation field = adapter.getSpecification().getAssociation(fieldName);
            if (field == null) {
                throw new ScimpiException("No field " + fieldName + " in " + adapter.getSpecification().getFullIdentifier());
            }
            
            // REVIEW: should provide this rendering context, rather than hardcoding.
            // the net effect currently is that class members annotated with 
            // @Hidden(where=Where.ANYWHERE) or @Disabled(where=Where.ANYWHERE) will indeed
            // be hidden/disabled, but will be visible/enabled (perhaps incorrectly) 
            // for any other value for Where
            final Where where = Where.ANYWHERE;
            
            if (field.isVisible(IsisContext.getAuthenticationSession(), adapter, where).isVetoed()) {
                throw new ForbiddenException(field, ForbiddenException.VISIBLE);
            }
            IsisContext.getPersistenceSession().resolveField(adapter, field);
            adapter = field.get(adapter);
            if (adapter != null) {
                objectId = context.mapObject(adapter, Scope.INTERACTION);
            }
        }

        if (adapter != null && valid(templateProcessor, adapter)) {
            final String variable = templateProcessor.getOptionalProperty("param-name", Names.RESULT);
            final String variableSegment = variable + "=" + objectId;

            String view = templateProcessor.getOptionalProperty(VIEW);
            if (view == null) {
                view = defaultView();
            }
            view = context.fullUriPath(view);
            final String classSegment = " class=\"" + linkClass + "\"";
            final String titleSegment = title == null ? "" : (" title=\"" + title + "\"");
            String additionalSegment = additionalParameters(templateProcessor);
            additionalSegment = additionalSegment == null ? "" : "&amp;" + additionalSegment;
            if (showAsButton) {
                templateProcessor.appendHtml("<div class=\"" + containerClass + "\">");
            }
            templateProcessor.appendHtml("<a" + classSegment + titleSegment + " href=\"" + view + "?" + variableSegment + context.encodedInteractionParameters() + additionalSegment + "\">");
            templateProcessor.pushNewBuffer();
            templateProcessor.processUtilCloseTag();
            final String buffer = templateProcessor.popBuffer();
            if (buffer.trim().length() > 0) {
                templateProcessor.appendHtml(buffer);
            } else {
                templateProcessor.appendAsHtmlEncoded(linkLabel(name, adapter));
            }
            templateProcessor.appendHtml("</a>");
            if (showAsButton) {
                templateProcessor.appendHtml("</div>");
            }
        } else {
            templateProcessor.skipUntilClose();
        }
    }

    private String defaultLinkClass() {
        return "action";
    }

    protected abstract String linkLabel(String name, ObjectAdapter object);

    protected String additionalParameters(final TemplateProcessor templateProcessor) {
        return null;
    }

    protected abstract boolean valid(TemplateProcessor templateProcessor, ObjectAdapter adapter);

    protected abstract String defaultView();
}

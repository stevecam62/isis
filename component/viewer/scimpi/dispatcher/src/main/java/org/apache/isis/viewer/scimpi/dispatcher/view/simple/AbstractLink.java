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

import org.apache.isis.applib.annotation.Where;
import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.spec.feature.ObjectAssociation;
import org.apache.isis.core.runtime.system.context.IsisContext;
import org.apache.isis.viewer.scimpi.ForbiddenException;
import org.apache.isis.viewer.scimpi.Names;
import org.apache.isis.viewer.scimpi.ScimpiException;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request.Scope;
import org.apache.isis.viewer.scimpi.dispatcher.processor.TagProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.util.MethodsUtils;
import org.apache.isis.viewer.scimpi.dispatcher.view.AbstractElementProcessor;

public abstract class AbstractLink extends AbstractElementProcessor {

    @Override
    public void process(final TagProcessor tagProcessor) {
        final String title = tagProcessor.getOptionalProperty(FORM_TITLE);
        final String name = tagProcessor.getOptionalProperty(NAME);
        final boolean showAsButton = tagProcessor.isRequested("show-as-button", false);
        final String linkClass = tagProcessor.getOptionalProperty(CLASS, showAsButton ? "button" : defaultLinkClass());
        final String containerClass = tagProcessor.getOptionalProperty(CONTAINER_CLASS, "action");
        final String object = tagProcessor.getOptionalProperty(OBJECT);
        final Request context = tagProcessor.getContext();
        String objectId = object != null ? object : (String) context.getVariable(Names.RESULT);
        ObjectAdapter adapter = MethodsUtils.findObject(context, objectId);

        // REVIEW this is common used code
        final String fieldName = tagProcessor.getOptionalProperty(FIELD);
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

        if (adapter != null && valid(tagProcessor, adapter)) {
            final String variable = tagProcessor.getOptionalProperty("param-name", Names.RESULT);
            final String variableSegment = variable + "=" + objectId;

            String view = tagProcessor.getOptionalProperty(VIEW);
            if (view == null) {
                view = defaultView();
            }
            view = context.fullUriPath(view);
            final String classSegment = " class=\"" + linkClass + "\"";
            final String titleSegment = title == null ? "" : (" title=\"" + title + "\"");
            String additionalSegment = additionalParameters(tagProcessor);
            additionalSegment = additionalSegment == null ? "" : "&amp;" + additionalSegment;
            if (showAsButton) {
                tagProcessor.appendHtml("<div class=\"" + containerClass + "\">");
            }
            tagProcessor.appendHtml("<a" + classSegment + titleSegment + " href=\"" + view + "?" + variableSegment + context.encodedInteractionParameters() + additionalSegment + "\">");
            tagProcessor.pushNewBuffer();
            tagProcessor.processUtilCloseTag();
            final String buffer = tagProcessor.popBuffer();
            if (buffer.trim().length() > 0) {
                tagProcessor.appendHtml(buffer);
            } else {
                tagProcessor.appendAsHtmlEncoded(linkLabel(name, adapter));
            }
            tagProcessor.appendHtml("</a>");
            if (showAsButton) {
                tagProcessor.appendHtml("</div>");
            }
        } else {
            tagProcessor.skipUntilClose();
        }
    }

    private String defaultLinkClass() {
        return "action";
    }

    protected abstract String linkLabel(String name, ObjectAdapter object);

    protected String additionalParameters(final TagProcessor tagProcessor) {
        return null;
    }

    protected abstract boolean valid(TagProcessor tagProcessor, ObjectAdapter adapter);

    protected abstract String defaultView();
}

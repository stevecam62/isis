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

import java.util.List;

import org.apache.isis.applib.annotation.Where;
import org.apache.isis.applib.filter.Filter;
import org.apache.isis.core.commons.authentication.AuthenticationSession;
import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.consent.Consent;
import org.apache.isis.core.metamodel.spec.feature.ObjectAssociation;
import org.apache.isis.core.metamodel.spec.feature.ObjectAssociationFilters;
import org.apache.isis.core.metamodel.spec.feature.OneToManyAssociation;
import org.apache.isis.core.runtime.system.context.IsisContext;
import org.apache.isis.viewer.scimpi.ForbiddenException;
import org.apache.isis.viewer.scimpi.Names;
import org.apache.isis.viewer.scimpi.ScimpiException;
import org.apache.isis.viewer.scimpi.dispatcher.action.RemoveAction;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request.Scope;
import org.apache.isis.viewer.scimpi.dispatcher.processor.TagProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.util.MethodsUtils;
import org.apache.isis.viewer.scimpi.dispatcher.view.AbstractElementProcessor;

public class RemoveElement extends AbstractElementProcessor {

    // REVIEW: should provide this rendering context, rather than hardcoding.
    // the net effect currently is that class members annotated with 
    // @Hidden(where=Where.ANYWHERE) or @Disabled(where=Where.ANYWHERE) will indeed
    // be hidden/disabled, but will be visible/enabled (perhaps incorrectly) 
    // for any other value for Where
    private final static Where where = Where.ANYWHERE;

    @Override
    public void process(final TagProcessor tagProcessor) {
        final String title = tagProcessor.getOptionalProperty(BUTTON_TITLE, "Remove From List");
        final String cls = tagProcessor.getOptionalProperty(CLASS, "action in-line delete confirm");
        final String object = tagProcessor.getOptionalProperty(OBJECT);
        final String resultOverride = tagProcessor.getOptionalProperty(RESULT_OVERRIDE);
        final Request context = tagProcessor.getContext();
        final String objectId = object != null ? object : (String) context.getVariable(Names.RESULT);
        final ObjectAdapter adapter = MethodsUtils.findObject(context, objectId);

        final String element = tagProcessor.getOptionalProperty(ELEMENT, (String) context.getVariable(ELEMENT));
        final ObjectAdapter elementId = MethodsUtils.findObject(context, element);

        final String fieldName = tagProcessor.getRequiredProperty(FIELD);

        String view = tagProcessor.getOptionalProperty(VIEW);
        view = context.fullFilePath(view == null ? context.getResourceFile() : view);
        String error = tagProcessor.getOptionalProperty(ERROR);
        error = context.fullFilePath(error == null ? context.getResourceFile() : error);

        tagProcessor.processUtilCloseTag();

        write(tagProcessor, adapter, fieldName, elementId, resultOverride, view, error, title, cls);
    }

    @Override
    public String getName() {
        return "remove-element";
    }

    public static void write(final TagProcessor tagProcessor, final ObjectAdapter adapter, final String fieldName, final ObjectAdapter element, final String resultOverride, final String view, final String error, final String title, final String cssClass) {
        final ObjectAssociation field = adapter.getSpecification().getAssociation(fieldName);
        if (field == null) {
            throw new ScimpiException("No field " + fieldName + " in " + adapter.getSpecification().getFullIdentifier());
        }
        if (!field.isOneToManyAssociation()) {
            throw new ScimpiException("Field " + fieldName + " not a collection, in " + adapter.getSpecification().getFullIdentifier());
        }
        if (field.isVisible(IsisContext.getAuthenticationSession(), adapter, where).isVetoed()) {
            throw new ForbiddenException(field, ForbiddenException.VISIBLE);
        }
        IsisContext.getPersistenceSession().resolveField(adapter, field);

        Consent usable = field.isUsable(IsisContext.getAuthenticationSession(), adapter, where);
        if (usable.isAllowed()) {
            usable = ((OneToManyAssociation) field).isValidToRemove(adapter, element);
        }

        if (usable.isVetoed()) {
            tagProcessor.appendHtml("<div class=\"" + cssClass + " disabled-form\">"); 
            tagProcessor.appendHtml("<div class=\"button disabled\" title=\""); 
            tagProcessor.appendAsHtmlEncoded(usable.getReason());
            tagProcessor.appendHtml("\" >" + title);
            tagProcessor.appendHtml("</div>");
            tagProcessor.appendHtml("</div>");
        } else {
            if (valid(tagProcessor, adapter)) {
                final String classSegment = " class=\"" + cssClass + "\"";

                final String objectId = tagProcessor.getContext().mapObject(adapter, Scope.INTERACTION);
                final String elementId = tagProcessor.getContext().mapObject(element, Scope.INTERACTION);
                final String action = RemoveAction.ACTION + Names.COMMAND_ROOT;
                tagProcessor.appendHtml("<form" + classSegment + " method=\"post\" action=\"" + action + "\" >");
                tagProcessor.appendHtml("<input type=\"hidden\" name=\"" + OBJECT + "\" value=\"" + objectId + "\" />");
                tagProcessor.appendHtml("<input type=\"hidden\" name=\"" + FIELD + "\" value=\"" + fieldName + "\" />");
                tagProcessor.appendHtml("<input type=\"hidden\" name=\"" + ELEMENT + "\" value=\"" + elementId + "\" />");
                if (resultOverride != null) {
                    tagProcessor.appendHtml("<input type=\"hidden\" name=\"" + RESULT_OVERRIDE + "\" value=\"" + resultOverride + "\" />");
                }
                tagProcessor.appendHtml("<input type=\"hidden\" name=\"" + VIEW + "\" value=\"" + view + "\" />");
                tagProcessor.appendHtml("<input type=\"hidden\" name=\"" + ERROR + "\" value=\"" + error + "\" />");
                tagProcessor.appendHtml(tagProcessor.getContext().interactionFields());
                tagProcessor.appendHtml("<input class=\"button\" type=\"submit\" value=\"" + title + "\" />");
                tagProcessor.appendHtml("</form>");
            }
        }
    }

    private static boolean valid(final TagProcessor tagProcessor, final ObjectAdapter adapter) {
        // TODO is this check valid/necessary?

        // TODO check is valid to remove element
        final AuthenticationSession session = IsisContext.getAuthenticationSession();
        final Filter<ObjectAssociation> filter = ObjectAssociationFilters.dynamicallyVisible(session, adapter, where);
        final List<ObjectAssociation> visibleFields = adapter.getSpecification().getAssociations(filter);
        if (visibleFields.size() == 0) {
            return false;
        }

        return true;
    }
}

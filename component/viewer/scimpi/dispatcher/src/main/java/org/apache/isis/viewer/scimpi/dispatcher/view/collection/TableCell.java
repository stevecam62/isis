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

package org.apache.isis.viewer.scimpi.dispatcher.view.collection;

import org.apache.isis.applib.annotation.Where;
import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.spec.feature.ObjectAssociation;
import org.apache.isis.core.runtime.system.context.IsisContext;
import org.apache.isis.viewer.scimpi.Names;
import org.apache.isis.viewer.scimpi.ScimpiException;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request;
import org.apache.isis.viewer.scimpi.dispatcher.context.RequestState;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request.Scope;
import org.apache.isis.viewer.scimpi.dispatcher.processor.TemplateProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.view.AbstractElementProcessor;

public class TableCell extends AbstractElementProcessor {

    // REVIEW: should provide this rendering context, rather than hardcoding.
    // the net effect currently is that class members annotated with
    // @Hidden(where=Where.ALL_TABLES) or @Disabled(where=Where.ALL_TABLES) will indeed
    // be hidden from all tables but will be visible/enabled (perhaps incorrectly) 
    // if annotated with Where.PARENTED_TABLE or Where.STANDALONE_TABLE
    private final Where where = Where.ALL_TABLES;

    @Override
    public void process(final TemplateProcessor templateProcessor, RequestState state) {
        final TableBlock tableBlock = (TableBlock) templateProcessor.peekBlock();
        final String id = templateProcessor.getOptionalProperty(OBJECT);
        final String fieldName = templateProcessor.getRequiredProperty(FIELD);
        final String linkView = templateProcessor.getOptionalProperty(LINK_VIEW);
        String className = templateProcessor.getOptionalProperty(CLASS);
        className = className == null ? "" : " class=\"" + className + "\"";
        Request context = templateProcessor.getContext();
        final ObjectAdapter object = context.getMappedObjectOrVariable(id, tableBlock.getElementName());
        final ObjectAssociation field = object.getSpecification().getAssociation(fieldName);
        if (field == null) {
            throw new ScimpiException("No field " + fieldName + " in " + object.getSpecification().getFullIdentifier());
        }
        templateProcessor.appendHtml("<td" + className + ">");
        if (field.isVisible(IsisContext.getAuthenticationSession(), object, where).isAllowed()) {
            final ObjectAdapter fieldReference = field.get(object);
            final String source = fieldReference == null ? "" : context.mapObject(fieldReference, Scope.REQUEST);
            final String name = templateProcessor.getOptionalProperty(RESULT_NAME, fieldName);
            context.addVariable(name, templateProcessor.encodeHtml(source), Scope.REQUEST);

            if (linkView != null) {
                final String linkId = context.mapObject(object, Scope.REQUEST);
                final String linkName = templateProcessor.getOptionalProperty(LINK_NAME, Names.RESULT);
                final String linkObject = templateProcessor.getOptionalProperty(LINK_OBJECT, linkId);
                templateProcessor.appendHtml("<a href=\"" + linkView + "?" + linkName + "=" + linkObject + context.encodedInteractionParameters() + "\">");
            } else if(tableBlock.getlinkView() != null) {
                String linkObjectInVariable = tableBlock.getElementName();
                final String linkId = (String) context.getVariable(linkObjectInVariable);
                templateProcessor.appendHtml("<a href=\"" + tableBlock.getlinkView() + "?" + tableBlock.getlinkName() + "=" + linkId + context.encodedInteractionParameters() + "\">");                
            }
            templateProcessor.pushNewBuffer();
            templateProcessor.processUtilCloseTag();
            final String buffer = templateProcessor.popBuffer();
            if (buffer.trim().length() == 0) {
                templateProcessor.appendAsHtmlEncoded(fieldReference == null ? "" : fieldReference.titleString());
            } else {
                templateProcessor.appendHtml(buffer);
            }
            if (linkView != null) {
                templateProcessor.appendHtml("</a>");
            }
        } else {
            templateProcessor.skipUntilClose();
        }
        templateProcessor.appendHtml("</td>");
    }

    @Override
    public String getName() {
        return "table-cell";
    }

}

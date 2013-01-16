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

package org.apache.isis.viewer.scimpi.dispatcher.view.display;

import java.text.DecimalFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.isis.applib.annotation.Where;
import org.apache.isis.core.commons.authentication.AuthenticationSession;
import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.spec.feature.ObjectAssociation;
import org.apache.isis.core.progmodel.facets.value.date.DateValueFacet;
import org.apache.isis.core.runtime.system.context.IsisContext;
import org.apache.isis.viewer.scimpi.ForbiddenException;
import org.apache.isis.viewer.scimpi.ScimpiException;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request.Scope;
import org.apache.isis.viewer.scimpi.dispatcher.processor.TagProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.view.AbstractElementProcessor;

public class GetField extends AbstractElementProcessor {

    // REVIEW: should provide this rendering context, rather than hardcoding.
    // the net effect currently is that class members annotated with 
    // @Hidden(where=Where.ANYWHERE) or @Disabled(where=Where.ANYWHERE) will indeed
    // be hidden/disabled, but will be visible/enabled (perhaps incorrectly) 
    // for any other value for Where
    private final Where where = Where.ANYWHERE;

    @Override
    public void process(final TagProcessor tagProcessor) {
        final String id = tagProcessor.getOptionalProperty(OBJECT);
        final String fieldName = tagProcessor.getRequiredProperty(FIELD);
        final ObjectAdapter object = tagProcessor.getContext().getMappedObjectOrResult(id);
        if (object == null) {
            throw new ScimpiException("No object to get field for: " + fieldName + " - " + id);
        }
        final ObjectAssociation field = object.getSpecification().getAssociation(fieldName);
        if (field == null) {
            throw new ScimpiException("No field " + fieldName + " in " + object.getSpecification().getFullIdentifier());
        }
        final AuthenticationSession session = IsisContext.getAuthenticationSession();
        if (field.isVisible(session, object, where).isVetoed()) {
            throw new ForbiddenException(field, ForbiddenException.VISIBLE);
        }

        String pattern = tagProcessor.getOptionalProperty("decimal-format");
        Format format = null;
        if (pattern != null) {
            format = new DecimalFormat(pattern);
        }
        pattern = tagProcessor.getOptionalProperty("date-format");
        if (pattern != null) {
            format = new SimpleDateFormat(pattern);
        }

        final String name = tagProcessor.getOptionalProperty(RESULT_NAME, fieldName);
        final String scopeName = tagProcessor.getOptionalProperty(SCOPE);
        final Scope scope = Request.scope(scopeName, Scope.REQUEST);

        process(tagProcessor, object, field, format, name, scope);
    }

    protected void process(final TagProcessor tagProcessor, final ObjectAdapter object, final ObjectAssociation field, final Format format, final String name, final Scope scope) {
        final ObjectAdapter fieldReference = field.get(object);
        if (format != null && fieldReference.isValue()) {
            final DateValueFacet facet = fieldReference.getSpecification().getFacet(DateValueFacet.class);
            final Date date = facet.dateValue(fieldReference);
            final String value = format.format(date);
            tagProcessor.appendDebug("    " + object + " -> " + value);
            tagProcessor.getContext().addVariable(name, TagProcessor.getEncoder().encoder(value), scope);
        } else {
            final String source = fieldReference == null ? "" : tagProcessor.getContext().mapObject(fieldReference, scope);
            tagProcessor.appendDebug("    " + object + " -> " + source);
            tagProcessor.getContext().addVariable(name, source, scope);
        }
    }

    @Override
    public String getName() {
        return "get-field";
    }

}

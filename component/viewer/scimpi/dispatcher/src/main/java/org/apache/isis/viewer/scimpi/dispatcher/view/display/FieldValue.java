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

import org.apache.isis.applib.annotation.Where;
import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.facets.object.parseable.ParseableFacet;
import org.apache.isis.core.metamodel.spec.ObjectSpecification;
import org.apache.isis.core.metamodel.spec.feature.ObjectAssociation;
import org.apache.isis.core.progmodel.facets.value.booleans.BooleanValueFacet;
import org.apache.isis.core.runtime.persistence.ObjectNotFoundException;
import org.apache.isis.core.runtime.system.context.IsisContext;
import org.apache.isis.viewer.scimpi.ForbiddenException;
import org.apache.isis.viewer.scimpi.ScimpiException;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request.Scope;
import org.apache.isis.viewer.scimpi.dispatcher.processor.TagProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.view.AbstractElementProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.view.field.LinkedObject;

public class FieldValue extends AbstractElementProcessor {

    // REVIEW: should provide this rendering context, rather than hardcoding.
    // the net effect currently is that class members annotated with 
    // @Hidden(where=Where.ANYWHERE) or @Disabled(where=Where.ANYWHERE) will indeed
    // be hidden/disabled, but will be visible/enabled (perhaps incorrectly) 
    // for any other value for Where
    private final Where where = Where.ANYWHERE;

    @Override
    public void process(final TagProcessor tagProcessor) {
        final String className = tagProcessor.getOptionalProperty(CLASS);
        final String id = tagProcessor.getOptionalProperty(OBJECT);
        final String fieldName = tagProcessor.getRequiredProperty(FIELD);
        final ObjectAdapter object = tagProcessor.getContext().getMappedObjectOrResult(id);
        final ObjectAssociation field = object.getSpecification().getAssociation(fieldName);
        if (field == null) {
            throw new ScimpiException("No field " + fieldName + " in " + object.getSpecification().getFullIdentifier());
        }
        if (field.isVisible(IsisContext.getAuthenticationSession(), object, where).isVetoed()) {
            throw new ForbiddenException(field, ForbiddenException.VISIBLE);
        }
        final boolean isIconShowing = tagProcessor.isRequested(SHOW_ICON, showIconByDefault());
        final int truncateTo = Integer.valueOf(tagProcessor.getOptionalProperty(TRUNCATE, "0")).intValue();

        write(tagProcessor, object, field, null, className, isIconShowing, truncateTo);
    }

    @Override
    public String getName() {
        return "field";
    }

    public static void write(final TagProcessor tagProcessor, final ObjectAdapter object, final ObjectAssociation field, final LinkedObject linkedField, final String className, final boolean showIcon, final int truncateTo) {

        final ObjectAdapter fieldReference = field.get(object);

        if (fieldReference != null) {
            final String classSection = "class=\"" + (className == null ? "value" : className) + "\"";
            tagProcessor.appendHtml("<span " + classSection + ">");
            if (field.isOneToOneAssociation()) {
                try {
                    IsisContext.getPersistenceSession().resolveImmediately(fieldReference);
                } catch (final ObjectNotFoundException e) {
                    tagProcessor.appendHtml(e.getMessage() + "</span>");
                }
            }

            if (!field.getSpecification().containsFacet(ParseableFacet.class) && showIcon) {
                tagProcessor.appendHtml("<img class=\"small-icon\" src=\"" + tagProcessor.getContext().imagePath(fieldReference) + "\" alt=\"" + field.getSpecification().getShortIdentifier() + "\"/>");
            }

            if (linkedField != null) {
                final String id = tagProcessor.getContext().mapObject(fieldReference, linkedField.getScope(), Scope.INTERACTION);
                tagProcessor.appendHtml("<a href=\"" + linkedField.getForwardView() + "?" + linkedField.getVariable() + "=" + id + tagProcessor.getContext().encodedInteractionParameters() + "\">");
            }
            String value = fieldReference == null ? "" : fieldReference.titleString();
            if (truncateTo > 0 && value.length() > truncateTo) {
                value = value.substring(0, truncateTo) + "...";
            }

            // TODO figure out a better way to determine if boolean or a
            // password
            final ObjectSpecification spec = field.getSpecification();
            final BooleanValueFacet facet = spec.getFacet(BooleanValueFacet.class);
            if (facet != null) {
                final boolean flag = facet.isSet(fieldReference);
                final String valueSegment = flag ? " checked=\"checked\"" : "";
                final String disabled = " disabled=\"disabled\"";
                tagProcessor.appendHtml("<input type=\"checkbox\"" + valueSegment + disabled + " />");
            } else {
                tagProcessor.appendAsHtmlEncoded(value);
            }

            if (linkedField != null) {
                tagProcessor.appendHtml("</a>");
            }
            tagProcessor.appendHtml("</span>");
        }
    }

}

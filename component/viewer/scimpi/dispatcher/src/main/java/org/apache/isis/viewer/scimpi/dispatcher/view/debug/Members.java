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

package org.apache.isis.viewer.scimpi.dispatcher.view.debug;

import java.util.List;

import org.apache.isis.applib.annotation.Where;
import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.spec.ActionType;
import org.apache.isis.core.metamodel.spec.ObjectSpecification;
import org.apache.isis.core.metamodel.spec.feature.ObjectAction;
import org.apache.isis.core.metamodel.spec.feature.ObjectActionContainer.Contributed;
import org.apache.isis.core.metamodel.spec.feature.ObjectActionParameter;
import org.apache.isis.core.metamodel.spec.feature.ObjectAssociation;
import org.apache.isis.core.runtime.system.context.IsisContext;
import org.apache.isis.viewer.scimpi.ForbiddenException;
import org.apache.isis.viewer.scimpi.ScimpiException;
import org.apache.isis.viewer.scimpi.dispatcher.processor.TagProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.view.AbstractElementProcessor;

public class Members extends AbstractElementProcessor {

    // REVIEW: should provide this rendering context, rather than hardcoding.
    // the net effect currently is that class members annotated with 
    // @Hidden(where=Where.ANYWHERE) or @Disabled(where=Where.ANYWHERE) will indeed
    // be hidden/disabled, but will be visible/enabled (perhaps incorrectly) 
    // for any other value for Where
    private final Where where = Where.ANYWHERE;

    @Override
    public String getName() {
        return "members";
    }

    @Override
    public void process(final TagProcessor tagProcessor) {
        if (tagProcessor.getContext().isDebugDisabled()) {
            return;
        }

        final String id = tagProcessor.getOptionalProperty(OBJECT);
        final String fieldName = tagProcessor.getOptionalProperty(FIELD);
        tagProcessor.appendHtml("<pre class=\"debug\">");
        try {
            ObjectAdapter object = tagProcessor.getContext().getMappedObjectOrResult(id);
            ObjectAssociation field = null;
            if (fieldName != null) {
                field = object.getSpecification().getAssociation(fieldName);
                if (field.isVisible(IsisContext.getAuthenticationSession(), object, where).isVetoed()) {
                    throw new ForbiddenException(field, ForbiddenException.VISIBLE);
                }
                object = field.get(object);
            }
            tagProcessor.processUtilCloseTag();

            final ObjectSpecification specification = field == null ? object.getSpecification() : field.getSpecification();

            tagProcessor.appendHtml(specification.getSingularName() + " (" + specification.getFullIdentifier() + ") \n");
            final List<ObjectAssociation> fields = specification.getAssociations();
            for (final ObjectAssociation fld : fields) {
                if (!fld.isAlwaysHidden()) {
                    tagProcessor.appendHtml("   " + fld.getId() + " - '" + fld.getName() + "' -> " + fld.getSpecification().getSingularName() + (fld.isOneToManyAssociation() ? " (collection of)" : "") + "\n");
                }
            }
            tagProcessor.appendHtml("   --------------\n");
            final List<ObjectAction> actions = specification.getObjectActions(ActionType.USER, Contributed.INCLUDED);
            ;
            for (final ObjectAction action : actions) {
                tagProcessor.appendHtml("   " + action.getId() + " (");
                boolean first = true;
                for (final ObjectActionParameter parameter : action.getParameters()) {
                    if (!first) {
                        tagProcessor.appendHtml(", ");
                    }
                    tagProcessor.appendHtml(parameter.getSpecification().getSingularName());
                    first = false;
                }
                tagProcessor.appendHtml(")" + " - '" + action.getName() + "'");
                if (action.getSpecification() != null) {
                    tagProcessor.appendHtml(" -> " + action.getSpecification().getSingularName() + ")");
                }
                tagProcessor.appendHtml("\n");
            }
        } catch (final ScimpiException e) {
            tagProcessor.appendHtml("Debug failed: " + e.getMessage());
        }
        tagProcessor.appendHtml("</pre>");
    }

}

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

import java.util.Collection;
import java.util.List;

import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.adapter.version.Version;
import org.apache.isis.core.metamodel.facets.object.parseable.ParseableFacet;
import org.apache.isis.core.metamodel.spec.ObjectSpecification;
import org.apache.isis.core.metamodel.spec.feature.ObjectAssociation;
import org.apache.isis.core.metamodel.spec.feature.ObjectAssociationFilters;
import org.apache.isis.core.runtime.system.context.IsisContext;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request.Scope;
import org.apache.isis.viewer.scimpi.dispatcher.processor.TagProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.view.AbstractObjectProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.view.display.FieldValue;
import org.apache.isis.viewer.scimpi.dispatcher.view.field.LinkedObject;


public class DebugObjectView extends AbstractObjectProcessor {

    @Override
    public void process(final TagProcessor tagProcessor, final ObjectAdapter object) {
        final String classString = " class=\"" + tagProcessor.getOptionalProperty(CLASS, "form debug") + "\"";
        final String objectLink = tagProcessor.getOptionalProperty(OBJECT + "-" + LINK_VIEW, tagProcessor.getViewPath());
        // final String collectionLink = request.getOptionalProperty(COLLECTION + "-" + LINK_VIEW, request.getViewPath());
        final String oddRowClass = tagProcessor.getOptionalProperty(ODD_ROW_CLASS);
        final String evenRowClass = tagProcessor.getOptionalProperty(EVEN_ROW_CLASS);
        final boolean showIcons = tagProcessor.isRequested(SHOW_ICON, true);

        ObjectSpecification specification = object.getSpecification();

        tagProcessor.appendHtml("<div" + classString + ">");
        tagProcessor.appendHtml("<div class=\"title\">");
        tagProcessor.appendAsHtmlEncoded(specification.getSingularName() + " - " + specification.getFullIdentifier());
        tagProcessor.appendHtml("</div>");

        Version version = object.getVersion();
        tagProcessor.appendHtml("<div class=\"version\">");
        tagProcessor.appendAsHtmlEncoded("#" + version.sequence() + " - " + version.getUser() + " (" + version.getTime() + ")" );
        tagProcessor.appendHtml("</div>");

        final List<ObjectAssociation> fields = specification.getAssociations(ObjectAssociationFilters.ALL);

        int row = 1;
        for (int i = 0; i < fields.size(); i++) {
            final ObjectAssociation field = fields.get(i);
            /*
             * if (field.isVisible(IsisContext.getAuthenticationSession(), object).isVetoed()) { continue; }
             */
            String cls;
            if (row++ % 2 == 1) {
                cls = " class=\"field " + (oddRowClass == null ? ODD_ROW_CLASS : oddRowClass) + "\"";
            } else {
                cls = " class=\"field " + (evenRowClass == null ? EVEN_ROW_CLASS : evenRowClass) + "\"";
            }
            tagProcessor.appendHtml("<div " + cls + "><span class=\"label\">");
            tagProcessor.appendAsHtmlEncoded(field.getName());
            tagProcessor.appendHtml(":</span>");
            
            final boolean isNotParseable =
                !fields.get(i).getSpecification().containsFacet(ParseableFacet.class);
            LinkedObject linkedObject = null;
            if (isNotParseable) {
     //           linkedObject = new LinkedObject(field.isOneToManyAssociation() ? collectionLink : objectLink);
                linkedObject = new LinkedObject(objectLink);
            }
            addField(tagProcessor, object, field, linkedObject, showIcons);
            
            if (field.isOneToManyAssociation()) {
                Collection collection = (Collection) field.get(object).getObject();
                if (collection.size() == 0) {
                    tagProcessor.appendHtml("[empty]");
                } else {
                    // request.appendHtml(collection.size() + " elements");
                   
                    tagProcessor.appendHtml("<ol>");
                    
                   for (Object element : collection) {
                       ObjectAdapter adapterFor = IsisContext.getPersistenceSession().getAdapterManager().getAdapterFor(element);
                       IsisContext.getPersistenceSession().resolveImmediately(adapterFor);

                       String id = tagProcessor.getContext().mapObject(adapterFor, linkedObject.getScope(), Scope.INTERACTION);

                       tagProcessor.appendHtml("<li class=\"element\">");
                       tagProcessor.appendHtml("<a href=\"" + linkedObject.getForwardView() + "?" + linkedObject.getVariable() + "="
                               + id + tagProcessor.getContext().encodedInteractionParameters() + "\">");
                       tagProcessor.appendHtml(element.toString());
                       tagProcessor.appendHtml("</a></li>");
                   }
                   tagProcessor.appendHtml("</ol>");
                }
            } else {
                FieldValue.write(tagProcessor, object, field, linkedObject, "value", showIcons, 0);
            }

            
            
            tagProcessor.appendHtml("</div>");
        }
        tagProcessor.appendHtml("</div>");
    }

    protected void addField(
            final TagProcessor tagProcessor,
            final ObjectAdapter object,
            final ObjectAssociation field,
            final LinkedObject linkedObject,
            final boolean showIcons) {
     }

    @Override
    public String getName() {
        return "debug-object";
    }

}

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
import org.apache.isis.viewer.scimpi.dispatcher.processor.TemplateProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.view.AbstractObjectProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.view.determine.LinkedObject;
import org.apache.isis.viewer.scimpi.dispatcher.view.object.FieldValue;


public class DebugObjectView extends AbstractObjectProcessor {

    @Override
    public void process(final TemplateProcessor templateProcessor, final ObjectAdapter object) {
        final String classString = " class=\"" + templateProcessor.getOptionalProperty(CLASS, "form debug") + "\"";
        final String objectLink = templateProcessor.getOptionalProperty(OBJECT + "-" + LINK_VIEW, templateProcessor.getViewPath());
        // final String collectionLink = request.getOptionalProperty(COLLECTION + "-" + LINK_VIEW, request.getViewPath());
        final String oddRowClass = templateProcessor.getOptionalProperty(ODD_ROW_CLASS);
        final String evenRowClass = templateProcessor.getOptionalProperty(EVEN_ROW_CLASS);
        final boolean showIcons = templateProcessor.isRequested(SHOW_ICON, true);

        ObjectSpecification specification = object.getSpecification();

        templateProcessor.appendHtml("<div" + classString + ">");
        templateProcessor.appendHtml("<div class=\"title\">");
        templateProcessor.appendAsHtmlEncoded(specification.getSingularName() + " - " + specification.getFullIdentifier());
        templateProcessor.appendHtml("</div>");

        Version version = object.getVersion();
        templateProcessor.appendHtml("<div class=\"version\">");
        templateProcessor.appendAsHtmlEncoded("#" + version.sequence() + " - " + version.getUser() + " (" + version.getTime() + ")" );
        templateProcessor.appendHtml("</div>");

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
            templateProcessor.appendHtml("<div " + cls + "><span class=\"label\">");
            templateProcessor.appendAsHtmlEncoded(field.getName());
            templateProcessor.appendHtml(":</span>");
            
            final boolean isNotParseable =
                !fields.get(i).getSpecification().containsFacet(ParseableFacet.class);
            LinkedObject linkedObject = null;
            if (isNotParseable) {
     //           linkedObject = new LinkedObject(field.isOneToManyAssociation() ? collectionLink : objectLink);
                linkedObject = new LinkedObject(objectLink);
            }
            addField(templateProcessor, object, field, linkedObject, showIcons);
            
            if (field.isOneToManyAssociation()) {
                Collection collection = (Collection) field.get(object).getObject();
                if (collection.size() == 0) {
                    templateProcessor.appendHtml("[empty]");
                } else {
                    // request.appendHtml(collection.size() + " elements");
                   
                    templateProcessor.appendHtml("<ol>");
                    
                   for (Object element : collection) {
                       ObjectAdapter adapterFor = IsisContext.getPersistenceSession().getAdapterManager().getAdapterFor(element);
                       IsisContext.getPersistenceSession().resolveImmediately(adapterFor);

                       String id = templateProcessor.getContext().mapObject(adapterFor, linkedObject.getScope(), Scope.INTERACTION);

                       templateProcessor.appendHtml("<li class=\"element\">");
                       templateProcessor.appendHtml("<a href=\"" + linkedObject.getForwardView() + "?" + linkedObject.getVariable() + "="
                               + id + templateProcessor.getContext().encodedInteractionParameters() + "\">");
                       templateProcessor.appendHtml(element.toString());
                       templateProcessor.appendHtml("</a></li>");
                   }
                   templateProcessor.appendHtml("</ol>");
                }
            } else {
                FieldValue.write(templateProcessor, object, field, linkedObject, "value", showIcons, 0);
            }

            
            
            templateProcessor.appendHtml("</div>");
        }
        templateProcessor.appendHtml("</div>");
    }

    protected void addField(
            final TemplateProcessor templateProcessor,
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

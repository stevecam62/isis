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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.spec.ObjectSpecification;
import org.apache.isis.core.metamodel.spec.feature.ObjectAssociation;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request;
import org.apache.isis.viewer.scimpi.dispatcher.context.RequestState;
import org.apache.isis.viewer.scimpi.dispatcher.processor.TemplateProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.view.AbstractElementProcessor;


public class Specification extends AbstractElementProcessor {

    @Override
    public void process(final TemplateProcessor templateProcessor, RequestState state) {
        final Request context = templateProcessor.getContext();
        if (context.isDebugDisabled()) {
            return;
        }

        if (templateProcessor.isRequested("always") || context.getDebug() == Request.Debug.ON) {
            templateProcessor.appendHtml("<div class=\"debug\">");
            templateProcessor.appendHtml("<pre>");

            final String id = templateProcessor.getOptionalProperty("object");
            final ObjectAdapter object = context.getMappedObjectOrResult(id);
            final ObjectSpecification specification = object.getSpecification();
            String type = templateProcessor.getOptionalProperty(TYPE, "details");

            if (type.equals("graph")) {
                specificationGraph(templateProcessor, specification, null, new ArrayList<ObjectSpecification>(), 0);
            } else if (type.equals("details")) {
                specificationDetails(templateProcessor, specification);
            } else {
                templateProcessor.appendHtml("invalid type: " + type);
            }

            templateProcessor.appendHtml("</pre>");
            templateProcessor.appendHtml("</div>");
        }
    }

    private void specificationDetails(final TemplateProcessor templateProcessor, final ObjectSpecification specification) {
        renderName(templateProcessor, specification);
        final List<ObjectAssociation> fields = specification.getAssociations();
        for (int i = 0; i < fields.size(); i++) {
            templateProcessor.appendHtml("    " + fields.get(i).getName() + " (" + fields.get(i).getSpecification().getSingularName()
                    + ") \n");
        }
    }

    private void specificationGraph(
            TemplateProcessor templateProcessor,
            ObjectSpecification specification,
            String fieldName,
            List<ObjectSpecification> processed,
            int level) {
        if (processed.contains(specification)) {
            return;
        }

        templateProcessor.appendHtml(StringUtils.repeat("    ", level));
        if (processed.contains(specification)) {
            templateProcessor.appendHtml("* ");
        }
        templateProcessor.appendHtml(specification.getFullIdentifier());
        if (fieldName != null) {
            templateProcessor.appendHtml(" (" + fieldName + ")");
        }
        templateProcessor.appendHtml("\n");

        if (processed.contains(specification)) {
            return;
        }
        processed.add(specification);

        final List<ObjectAssociation> fields = specification.getAssociations();
        for (int i = 0; i < fields.size(); i++) {
            ObjectSpecification fieldSpecification = fields.get(i).getSpecification();
            if (fieldSpecification.isValue()) {
                continue;
            }
            specificationGraph(templateProcessor, fieldSpecification, fields.get(i).getName(), processed, level + 1);
        }
    }

    private void renderName(final TemplateProcessor templateProcessor, final ObjectSpecification specification) {
        templateProcessor.appendHtml(specification.getSingularName() + " (" + specification.getFullIdentifier() + ") \n");
    }

    @Override
    public String getName() {
        return "specification";
    }

}

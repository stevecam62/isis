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
import org.apache.isis.viewer.scimpi.dispatcher.processor.TagProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.view.AbstractElementProcessor;


public class Specification extends AbstractElementProcessor {

    @Override
    public void process(final TagProcessor tagProcessor) {
        final Request context = tagProcessor.getContext();
        if (context.isDebugDisabled()) {
            return;
        }

        if (tagProcessor.isRequested("always") || context.getDebug() == Request.Debug.ON) {
            tagProcessor.appendHtml("<div class=\"debug\">");
            tagProcessor.appendHtml("<pre>");

            final String id = tagProcessor.getOptionalProperty("object");
            final ObjectAdapter object = context.getMappedObjectOrResult(id);
            final ObjectSpecification specification = object.getSpecification();
            String type = tagProcessor.getOptionalProperty(TYPE, "details");

            if (type.equals("graph")) {
                specificationGraph(tagProcessor, specification, null, new ArrayList<ObjectSpecification>(), 0);
            } else if (type.equals("details")) {
                specificationDetails(tagProcessor, specification);
            } else {
                tagProcessor.appendHtml("invalid type: " + type);
            }

            tagProcessor.appendHtml("</pre>");
            tagProcessor.appendHtml("</div>");
        }
    }

    private void specificationDetails(final TagProcessor tagProcessor, final ObjectSpecification specification) {
        renderName(tagProcessor, specification);
        final List<ObjectAssociation> fields = specification.getAssociations();
        for (int i = 0; i < fields.size(); i++) {
            tagProcessor.appendHtml("    " + fields.get(i).getName() + " (" + fields.get(i).getSpecification().getSingularName()
                    + ") \n");
        }
    }

    private void specificationGraph(
            TagProcessor tagProcessor,
            ObjectSpecification specification,
            String fieldName,
            List<ObjectSpecification> processed,
            int level) {
        if (processed.contains(specification)) {
            return;
        }

        tagProcessor.appendHtml(StringUtils.repeat("    ", level));
        if (processed.contains(specification)) {
            tagProcessor.appendHtml("* ");
        }
        tagProcessor.appendHtml(specification.getFullIdentifier());
        if (fieldName != null) {
            tagProcessor.appendHtml(" (" + fieldName + ")");
        }
        tagProcessor.appendHtml("\n");

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
            specificationGraph(tagProcessor, fieldSpecification, fields.get(i).getName(), processed, level + 1);
        }
    }

    private void renderName(final TagProcessor tagProcessor, final ObjectSpecification specification) {
        tagProcessor.appendHtml(specification.getSingularName() + " (" + specification.getFullIdentifier() + ") \n");
    }

    @Override
    public String getName() {
        return "specification";
    }

}

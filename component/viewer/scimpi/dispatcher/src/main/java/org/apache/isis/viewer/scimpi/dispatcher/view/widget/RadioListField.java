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

package org.apache.isis.viewer.scimpi.dispatcher.view.widget;

import java.util.Iterator;

import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.facets.collections.modify.CollectionFacet;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request.Scope;
import org.apache.isis.viewer.scimpi.dispatcher.processor.TagProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.view.AbstractElementProcessor;

public class RadioListField extends AbstractElementProcessor {

    @Override
    public void process(final TagProcessor tagProcessor) {
        final FormFieldBlock block = (FormFieldBlock) tagProcessor.getBlockContent();
        final String field = tagProcessor.getRequiredProperty(FIELD);
        if (block.isVisible(field)) {
            final String id = tagProcessor.getRequiredProperty(COLLECTION);
            final String exclude = tagProcessor.getOptionalProperty("exclude");

            final ObjectAdapter collection = tagProcessor.getContext().getMappedObjectOrResult(id);

            final Request context = tagProcessor.getContext();
            final CollectionFacet facet = collection.getSpecification().getFacet(CollectionFacet.class);
            final Iterator<ObjectAdapter> iterator = facet.iterator(collection);

            final StringBuffer buffer = new StringBuffer();

            while (iterator.hasNext()) {
                final ObjectAdapter element = iterator.next();
                final Scope scope = Scope.INTERACTION;
                final String elementId = context.mapObject(element, scope);
                if (exclude != null && context.getMappedObject(exclude) == element) {
                    continue;
                }
                final String title = element.titleString();
                final String checked = "";
                buffer.append("<input type=\"radio\" name=\"" + field + "\" value=\"" + elementId + "\"" + checked + " />" + title + "</input><br/>\n");
            }

            block.replaceContent(field, buffer.toString());
        }
    }

    @Override
    public String getName() {
        return "radio-list";
    }

}

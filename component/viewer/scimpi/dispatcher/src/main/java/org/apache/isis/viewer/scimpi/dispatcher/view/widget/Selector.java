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

import org.apache.isis.core.commons.exceptions.UnknownTypeException;
import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.facets.collections.modify.CollectionFacet;
import org.apache.isis.core.metamodel.spec.feature.ObjectAction;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request.Scope;
import org.apache.isis.viewer.scimpi.dispatcher.processor.TagProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.util.MethodsUtils;
import org.apache.isis.viewer.scimpi.dispatcher.view.AbstractElementProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.view.action.ActionForm;
import org.apache.isis.viewer.scimpi.dispatcher.view.action.CreateFormParameter;

public class Selector extends AbstractElementProcessor {

    @Override
    public void process(final TagProcessor tagProcessor) {
        final FormFieldBlock block = (FormFieldBlock) tagProcessor.getBlockContent();
        final String field = tagProcessor.getRequiredProperty(FIELD);
        if (block.isVisible(field)) {
            processElement(tagProcessor, block, field);
        }
        tagProcessor.skipUntilClose();
    }

    private void processElement(final TagProcessor tagProcessor, final FormFieldBlock block, final String field) {
        final String type = tagProcessor.getOptionalProperty(TYPE, "dropdown");
        if (!tagProcessor.isPropertySpecified(METHOD) && tagProcessor.isPropertySpecified(COLLECTION)) {
            final String id = tagProcessor.getRequiredProperty(COLLECTION, TagProcessor.NO_VARIABLE_CHECKING);
            final String selector = showSelectionList(tagProcessor, id, block.getCurrent(field), block.isNullable(field), type);
            block.replaceContent(field, selector);
        } else {
            final String objectId = tagProcessor.getOptionalProperty(OBJECT);
            final String methodName = tagProcessor.getRequiredProperty(METHOD);
            final ObjectAdapter object = MethodsUtils.findObject(tagProcessor.getContext(), objectId);
            final ObjectAction action = MethodsUtils.findAction(object, methodName);
            if (action.getParameterCount() == 0) {
                final ObjectAdapter collection = action.execute(object, new ObjectAdapter[0]);
                final String selector = showSelectionList(tagProcessor, collection, block.getCurrent(field), block.isNullable(field), type);
                block.replaceContent(field, selector);
            } else {
                final String id = "selector_options";
                final String id2 = (String) tagProcessor.getContext().getVariable(id);
                final String selector = showSelectionList(tagProcessor, id2, block.getCurrent(field), block.isNullable(field), type);

                final CreateFormParameter parameters = new CreateFormParameter();
                parameters.objectId = objectId;
                parameters.methodName = methodName;
                parameters.buttonTitle = tagProcessor.getOptionalProperty(BUTTON_TITLE, "Search");
                parameters.formTitle = tagProcessor.getOptionalProperty(FORM_TITLE);
                parameters.className = tagProcessor.getOptionalProperty(CLASS, "selector");
                parameters.id = tagProcessor.getOptionalProperty(ID);

                parameters.resultName = id;
                parameters.forwardResultTo = tagProcessor.getContext().getResourceFile();
                parameters.forwardVoidTo = "error";
                parameters.forwardErrorTo = parameters.forwardResultTo;
                parameters.scope = Scope.REQUEST.name();
                tagProcessor.pushNewBuffer();
                ActionForm.createForm(tagProcessor, parameters);
                block.replaceContent(field, selector);

                tagProcessor.appendHtml(tagProcessor.popBuffer());
            }
        }
    }

    private String showSelectionList(final TagProcessor tagProcessor, final String collectionId, final ObjectAdapter selectedItem, final boolean allowNotSet, final String type) {
        if (collectionId != null && !collectionId.equals("")) {
            final ObjectAdapter collection = tagProcessor.getContext().getMappedObjectOrResult(collectionId);
            return showSelectionList(tagProcessor, collection, selectedItem, allowNotSet, type);
        } else {
            return null;
        }
    }

    private String showSelectionList(final TagProcessor tagProcessor, final ObjectAdapter collection, final ObjectAdapter selectedItem, final boolean allowNotSet, final String type) {
        final String field = tagProcessor.getRequiredProperty(FIELD);
        final CollectionFacet facet = collection.getSpecification().getFacet(CollectionFacet.class);

        if (type.equals("radio")) {
            return radioButtonList(tagProcessor, field, allowNotSet, collection, selectedItem, facet);
        } else if (type.equals("list")) {
            final String size = tagProcessor.getOptionalProperty("size", "5");
            return dropdownList(tagProcessor, field, allowNotSet, collection, selectedItem, size, facet);
        } else if (type.equals("dropdown")) {
            return dropdownList(tagProcessor, field, allowNotSet, collection, selectedItem, null, facet);
        } else {
            throw new UnknownTypeException(type);
        }
    }

    private String radioButtonList(final TagProcessor tagProcessor, final String field, final boolean allowNotSet, final ObjectAdapter collection, final ObjectAdapter selectedItem, final CollectionFacet facet) {
        final Request context = tagProcessor.getContext();
        final Iterator<ObjectAdapter> iterator = facet.iterator(collection);
        final StringBuffer buffer = new StringBuffer();
        if (allowNotSet) {
            buffer.append("<input type=\"radio\" name=\"" + field + "\" value=\"null\"></input><br/>\n");
        }
        while (iterator.hasNext()) {
            final ObjectAdapter element = iterator.next();
            final String elementId = context.mapObject(element, Scope.INTERACTION);
            final String title = element.titleString();
            final String checked = element == selectedItem ? "checked=\"checked\"" : "";
            buffer.append("<input type=\"radio\" name=\"" + field + "\" value=\"" + elementId + "\"" + checked + ">" + title + "</input><br/>\n");
        }

        return buffer.toString();
    }

    private String dropdownList(final TagProcessor tagProcessor, final String field, final boolean allowNotSet, final ObjectAdapter collection, final ObjectAdapter selectedItem, String size, final CollectionFacet facet) {
        final Request context = tagProcessor.getContext();
        final Iterator<ObjectAdapter> iterator = facet.iterator(collection);
        final StringBuffer buffer = new StringBuffer();
        size = size == null ? "" : " size =\"" + size + "\"";
        buffer.append("<select name=\"" + field + "\"" + size + " >\n");
        if (allowNotSet) {
            buffer.append("  <option value=\"null\"></option>\n");
        }
        while (iterator.hasNext()) {
            final ObjectAdapter element = iterator.next();
            final String elementId = context.mapObject(element, Scope.INTERACTION);
            final String title = element.titleString();
            final String checked = element == selectedItem ? "selected=\"selected\"" : "";
            buffer.append("  <option value=\"" + elementId + "\"" + checked + ">" + title + "</option>\n");
        }
        buffer.append("</select>\n");
        return buffer.toString();
    }

    @Override
    public String getName() {
        return "selector";
    }

}

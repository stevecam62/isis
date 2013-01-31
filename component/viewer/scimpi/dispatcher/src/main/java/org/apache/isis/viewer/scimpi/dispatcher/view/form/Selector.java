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

package org.apache.isis.viewer.scimpi.dispatcher.view.form;

import java.util.Iterator;

import org.apache.isis.core.commons.exceptions.UnknownTypeException;
import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.facets.collections.modify.CollectionFacet;
import org.apache.isis.core.metamodel.spec.feature.ObjectAction;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request;
import org.apache.isis.viewer.scimpi.dispatcher.context.RequestState;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request.Scope;
import org.apache.isis.viewer.scimpi.dispatcher.processor.TemplateProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.util.MethodsUtils;
import org.apache.isis.viewer.scimpi.dispatcher.view.AbstractElementProcessor;

public class Selector extends AbstractElementProcessor {

    @Override
    public void process(final TemplateProcessor templateProcessor, RequestState state) {
        final FormFieldBlock block = (FormFieldBlock) templateProcessor.peekBlock();
        final String field = templateProcessor.getRequiredProperty(FIELD);
        if (block.isVisible(field)) {
            processElement(templateProcessor, block, field);
        }
        templateProcessor.skipUntilClose();
    }

    private void processElement(final TemplateProcessor templateProcessor, final FormFieldBlock block, final String field) {
        final String type = templateProcessor.getOptionalProperty(TYPE, "dropdown");
        if (!templateProcessor.isPropertySpecified(METHOD) && templateProcessor.isPropertySpecified(COLLECTION)) {
            final String id = templateProcessor.getRequiredProperty(COLLECTION, TemplateProcessor.NO_VARIABLE_CHECKING);
            final String selector = showSelectionList(templateProcessor, id, block.getCurrent(field), block.isNullable(field), type);
            block.replaceContent(field, selector);
        } else {
            final String objectId = templateProcessor.getOptionalProperty(OBJECT);
            final String methodName = templateProcessor.getRequiredProperty(METHOD);
            final ObjectAdapter object = MethodsUtils.findObject(templateProcessor.getContext(), objectId);
            final ObjectAction action = MethodsUtils.findAction(object, methodName);
            if (action.getParameterCount() == 0) {
                final ObjectAdapter collection = action.execute(object, new ObjectAdapter[0]);
                final String selector = showSelectionList(templateProcessor, collection, block.getCurrent(field), block.isNullable(field), type);
                block.replaceContent(field, selector);
            } else {
                final String id = "selector_options";
                final String id2 = (String) templateProcessor.getContext().getVariable(id);
                final String selector = showSelectionList(templateProcessor, id2, block.getCurrent(field), block.isNullable(field), type);

                final CreateFormParameter parameters = new CreateFormParameter();
                parameters.objectId = objectId;
                parameters.methodName = methodName;
                parameters.buttonTitle = templateProcessor.getOptionalProperty(BUTTON_TITLE, "Search");
                parameters.formTitle = templateProcessor.getOptionalProperty(FORM_TITLE);
                parameters.className = templateProcessor.getOptionalProperty(CLASS, "selector");
                parameters.id = templateProcessor.getOptionalProperty(ID);

                parameters.resultName = id;
                parameters.forwardResultTo = templateProcessor.getContext().getResourceFile();
                parameters.forwardVoidTo = "error";
                parameters.forwardErrorTo = parameters.forwardResultTo;
                parameters.scope = Scope.REQUEST.name();
                templateProcessor.pushNewBuffer();
                ActionFormAbstract.createForm(templateProcessor, parameters);
                block.replaceContent(field, selector);

                templateProcessor.appendHtml(templateProcessor.popBuffer());
            }
        }
    }

    private String showSelectionList(final TemplateProcessor templateProcessor, final String collectionId, final ObjectAdapter selectedItem, final boolean allowNotSet, final String type) {
        if (collectionId != null && !collectionId.equals("")) {
            final ObjectAdapter collection = templateProcessor.getContext().getMappedObjectOrResult(collectionId);
            return showSelectionList(templateProcessor, collection, selectedItem, allowNotSet, type);
        } else {
            return null;
        }
    }

    private String showSelectionList(final TemplateProcessor templateProcessor, final ObjectAdapter collection, final ObjectAdapter selectedItem, final boolean allowNotSet, final String type) {
        final String field = templateProcessor.getRequiredProperty(FIELD);
        final CollectionFacet facet = collection.getSpecification().getFacet(CollectionFacet.class);

        if (type.equals("radio")) {
            return radioButtonList(templateProcessor, field, allowNotSet, collection, selectedItem, facet);
        } else if (type.equals("list")) {
            final String size = templateProcessor.getOptionalProperty("size", "5");
            return dropdownList(templateProcessor, field, allowNotSet, collection, selectedItem, size, facet);
        } else if (type.equals("dropdown")) {
            return dropdownList(templateProcessor, field, allowNotSet, collection, selectedItem, null, facet);
        } else {
            throw new UnknownTypeException(type);
        }
    }

    private String radioButtonList(final TemplateProcessor templateProcessor, final String field, final boolean allowNotSet, final ObjectAdapter collection, final ObjectAdapter selectedItem, final CollectionFacet facet) {
        final Request context = templateProcessor.getContext();
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

    private String dropdownList(final TemplateProcessor templateProcessor, final String field, final boolean allowNotSet, final ObjectAdapter collection, final ObjectAdapter selectedItem, String size, final CollectionFacet facet) {
        final Request context = templateProcessor.getContext();
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

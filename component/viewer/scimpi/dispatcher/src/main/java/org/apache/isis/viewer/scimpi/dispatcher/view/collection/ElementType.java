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

package org.apache.isis.viewer.scimpi.dispatcher.view.collection;

import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.spec.ObjectSpecification;
import org.apache.isis.core.metamodel.spec.feature.ObjectAssociation;
import org.apache.isis.viewer.scimpi.ScimpiException;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request;
import org.apache.isis.viewer.scimpi.dispatcher.context.RequestState;
import org.apache.isis.viewer.scimpi.dispatcher.processor.TemplateProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.view.AbstractElementProcessor;

public class ElementType extends AbstractElementProcessor {

    @Override
    public void process(final TemplateProcessor templateProcessor, RequestState state) {
        ObjectAdapter collection;
        final String field = templateProcessor.getOptionalProperty(FIELD);
        final Request context = templateProcessor.getContext();
        if (field != null) {
            final String id = templateProcessor.getRequiredProperty(OBJECT);
            final ObjectAdapter object = context.getMappedObjectOrResult(id);
            final ObjectAssociation objectField = object.getSpecification().getAssociation(field);
            if (!objectField.isOneToManyAssociation()) {
                throw new ScimpiException("Field " + objectField.getId() + " is not a collection");
            }
            collection = objectField.get(object);
        } else {
            final String id = templateProcessor.getOptionalProperty(COLLECTION);
            collection = context.getMappedObjectOrResult(id);
        }

        final ObjectSpecification elementSpecification = collection.getElementSpecification();
        final String name = elementSpecification.getSingularName();

        templateProcessor.appendAsHtmlEncoded(name);
    }

    @Override
    public String getName() {
        return "element-type";
    }

}

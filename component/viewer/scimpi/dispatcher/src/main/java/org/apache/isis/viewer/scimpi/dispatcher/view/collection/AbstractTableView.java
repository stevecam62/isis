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

import java.util.Iterator;
import java.util.List;

import org.apache.isis.applib.annotation.Where;
import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.facets.collections.modify.CollectionFacet;
import org.apache.isis.core.metamodel.facets.typeof.TypeOfFacet;
import org.apache.isis.core.metamodel.spec.ObjectSpecification;
import org.apache.isis.core.metamodel.spec.feature.ObjectAssociation;
import org.apache.isis.core.metamodel.spec.feature.ObjectAssociationFilters;
import org.apache.isis.core.runtime.system.context.IsisContext;
import org.apache.isis.core.runtime.system.persistence.Persistor;
import org.apache.isis.viewer.scimpi.ScimpiException;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request;
import org.apache.isis.viewer.scimpi.dispatcher.context.RequestState;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request.Scope;
import org.apache.isis.viewer.scimpi.dispatcher.processor.TemplateProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.view.AbstractElementProcessor;

public abstract class AbstractTableView extends AbstractElementProcessor {

    // REVIEW: should provide this rendering context, rather than hardcoding.
    // the net effect currently is that class members annotated with
    // @Hidden(where=Where.ALL_TABLES) or @Disabled(where=Where.ALL_TABLES) will indeed
    // be hidden from all tables but will be visible/enabled (perhaps incorrectly) 
    // if annotated with Where.PARENTED_TABLE or Where.STANDALONE_TABLE
    private final Where where = Where.ALL_TABLES;

    @Override
    public void process(final TemplateProcessor templateProcessor, RequestState state) {
        final Request context = templateProcessor.getContext();

        ObjectAdapter collection;
        String parentObjectId = null;
        boolean isFieldEditable = false;
        final String field = templateProcessor.getOptionalProperty(FIELD);
        final String tableClass = templateProcessor.getOptionalProperty(CLASS);
        ObjectSpecification elementSpec;
        String tableId;
        if (field != null) {
            final String objectId = templateProcessor.getOptionalProperty(OBJECT);
            final ObjectAdapter object = context.getMappedObjectOrResult(objectId);
            if (object == null) {
                throw new ScimpiException("No object for result or " + objectId);
            }
            final ObjectAssociation objectField = object.getSpecification().getAssociation(field);
            if (!objectField.isOneToManyAssociation()) {
                throw new ScimpiException("Field " + objectField.getId() + " is not a collection");
            }
            isFieldEditable = objectField.isUsable(IsisContext.getAuthenticationSession(), object, where).isAllowed();
            getPersistenceSession().resolveField(object, objectField);
            collection = objectField.get(object);
            final TypeOfFacet facet = objectField.getFacet(TypeOfFacet.class);
            elementSpec = facet.valueSpec();
            parentObjectId = objectId == null ? context.mapObject(object, Scope.REQUEST) : objectId;
            tableId = templateProcessor.getOptionalProperty(ID, field);
        } else {
            final String id = templateProcessor.getOptionalProperty(COLLECTION);
            collection = context.getMappedObjectOrResult(id);
            elementSpec = collection.getElementSpecification();
            tableId = templateProcessor.getOptionalProperty(ID, collection.getElementSpecification().getShortIdentifier());
        }

        final String summary = templateProcessor.getOptionalProperty("summary");
        final String rowClassesList = templateProcessor.getOptionalProperty(ROW_CLASSES, ODD_ROW_CLASS + "|" + EVEN_ROW_CLASS);
        String[] rowClasses = null;
        if (rowClassesList.length() > 0) {
            rowClasses = rowClassesList.split("[,|/]");
        }

        final List<ObjectAssociation> allFields = elementSpec.getAssociations(ObjectAssociationFilters.WHEN_VISIBLE_IRRESPECTIVE_OF_WHERE);
        final TableContentWriter rowBuilder = createRowBuilder(templateProcessor, context, isFieldEditable ? parentObjectId : null, allFields, collection);
        write(templateProcessor, collection, summary, rowBuilder, tableId, tableClass, rowClasses);

    }

    protected Persistor getPersistenceSession() {
        return IsisContext.getPersistenceSession();
    }

    protected abstract TableContentWriter createRowBuilder(final TemplateProcessor templateProcessor, Request context, final String parent, final List<ObjectAssociation> allFields, ObjectAdapter collection);

    public static void write(
            final TemplateProcessor templateProcessor,
            final ObjectAdapter collection,
            final String summary,
            final TableContentWriter rowBuilder,
            final String tableId,
            final String tableClass,
            final String[] rowClasses) {
        final Request context = templateProcessor.getContext();

        final String summarySegment = summary == null ? "" : (" summary=\"" + summary + "\"");
        final String idSegment = tableId == null ? "" : (" id=\"" + tableId + "\""); 
        final String classSegment = tableClass == null ? "" : (" class=\"" + tableClass + "\"");
        templateProcessor.appendHtml("<table" + idSegment + classSegment + summarySegment + ">");
        rowBuilder.writeCaption(templateProcessor);
        rowBuilder.writeHeaders(templateProcessor);
        rowBuilder.writeFooters(templateProcessor);

        templateProcessor.appendHtml("<tbody>");
        final CollectionFacet facet = collection.getSpecification().getFacet(CollectionFacet.class);
        final Iterator<ObjectAdapter> iterator = facet.iterator(collection);
        int row = 1;
        while (iterator.hasNext()) {
            final ObjectAdapter element = iterator.next();

            context.addVariable("row", "" + (row), Scope.REQUEST);
            String cls = "";
            if (rowClasses != null) {
                cls = " class=\"" + rowClasses[row % rowClasses.length] + "\"";
            }
            templateProcessor.appendHtml("<tr" + cls + ">");
            rowBuilder.writeElement(templateProcessor, context, element);
            templateProcessor.appendHtml("</tr>");
            row++;
        }
        templateProcessor.appendHtml("</tbody>");
        templateProcessor.appendHtml("</table>");
        
        rowBuilder.tidyUp();
    }

    @Override
    public String getName() {
        return "table";
    }

}

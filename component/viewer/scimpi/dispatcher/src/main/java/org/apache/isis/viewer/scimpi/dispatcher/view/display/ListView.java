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

package org.apache.isis.viewer.scimpi.dispatcher.view.display;

import java.util.Iterator;

import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.facets.collections.modify.CollectionFacet;
import org.apache.isis.core.metamodel.spec.feature.ObjectAssociation;
import org.apache.isis.viewer.scimpi.Names;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request.Scope;
import org.apache.isis.viewer.scimpi.dispatcher.processor.TagProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.view.AbstractObjectProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.view.field.LinkedObject;

public class ListView extends AbstractObjectProcessor {

    @Override
    public String checkFieldType(final ObjectAssociation objectField) {
        return objectField.isOneToManyAssociation() ? null : "is not a collection";
    }

    @Override
    public void process(final TagProcessor tagProcessor, final ObjectAdapter object) {
        final String linkRowView = tagProcessor.getOptionalProperty(LINK_VIEW);
        final String linkObjectName = tagProcessor.getOptionalProperty(ELEMENT_NAME, Names.RESULT);
        final String linkObjectScope = tagProcessor.getOptionalProperty(SCOPE, Scope.INTERACTION.toString());
        LinkedObject linkedRow = null;
        if (linkRowView != null) {
            linkedRow = new LinkedObject(linkObjectName, linkObjectScope, tagProcessor.getContext().fullUriPath(linkRowView));
        }
        final String bulletType = tagProcessor.getOptionalProperty("type");
        write(tagProcessor, object, linkedRow, bulletType);
    }

    public static void write(final TagProcessor tagProcessor, final ObjectAdapter collection, final LinkedObject linkRow, final String bulletType) {

        if (bulletType == null) {
            tagProcessor.appendHtml("<ol>");
        } else {
            tagProcessor.appendHtml("<ul type=\"" + bulletType + "\">");
        }

        final CollectionFacet facet = collection.getSpecification().getFacet(CollectionFacet.class);
        final Iterator<ObjectAdapter> iterator = facet.iterator(collection);
        while (iterator.hasNext()) {
            final ObjectAdapter element = iterator.next();

            tagProcessor.appendHtml("<li>");
            if (linkRow != null) {
                final Scope scope = linkRow == null ? Scope.INTERACTION : Request.scope(linkRow.getScope());
                final String rowId = tagProcessor.getContext().mapObject(element, scope);
                tagProcessor.appendHtml("<a class=\"item-select\" href=\"" + linkRow.getForwardView() + "?" + linkRow.getVariable() + "=" + rowId + "\">");
            }
            tagProcessor.appendAsHtmlEncoded(element.titleString());
            if (linkRow != null) {
                tagProcessor.appendHtml("</a>");
            }

            tagProcessor.appendHtml("</li>\n");
        }
        if (bulletType == null) {
            tagProcessor.appendHtml("</ol>");
        } else {
            tagProcessor.appendHtml("</ul>");
        }

    }

    @Override
    public String getName() {
        return "list";
    }

}

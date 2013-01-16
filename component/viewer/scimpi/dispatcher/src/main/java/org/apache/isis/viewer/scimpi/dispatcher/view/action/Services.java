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

package org.apache.isis.viewer.scimpi.dispatcher.view.action;

import java.util.List;

import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.runtime.system.context.IsisContext;
import org.apache.isis.core.runtime.system.persistence.PersistenceSession;
import org.apache.isis.viewer.scimpi.Names;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request.Scope;
import org.apache.isis.viewer.scimpi.dispatcher.processor.TagProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.view.AbstractElementProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.view.field.InclusionList;

public class Services extends AbstractElementProcessor {

    @Override
    public void process(final TagProcessor tagProcessor) {
        final boolean showForms = tagProcessor.isRequested(FORMS, false);
        final String view = tagProcessor.getOptionalProperty(VIEW, "_generic_action." + Names.EXTENSION);
        final String cancelTo = tagProcessor.getOptionalProperty(CANCEL_TO);

        final InclusionList inclusionList = new InclusionList();
        tagProcessor.setBlockContent(inclusionList);
        tagProcessor.processUtilCloseTag();

        final List<ObjectAdapter> serviceAdapters = getPersistenceSession().getServices();
        for (final ObjectAdapter adapter : serviceAdapters) {
            final String serviceId = tagProcessor.getContext().mapObject(adapter, Scope.REQUEST);
            tagProcessor.appendHtml("<div class=\"actions\">");
            tagProcessor.appendHtml("<h3>");
            tagProcessor.appendAsHtmlEncoded(adapter.titleString());
            tagProcessor.appendHtml("</h3>");
            Methods.writeMethods(tagProcessor, serviceId, adapter, showForms, inclusionList, view, cancelTo);
            tagProcessor.appendHtml("</div>");
        }
        tagProcessor.popBlockContent();
    }

    @Override
    public String getName() {
        return "services";
    }

    private static PersistenceSession getPersistenceSession() {
        return IsisContext.getPersistenceSession();
    }

}

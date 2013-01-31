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

package org.apache.isis.viewer.scimpi.dispatcher.view.global;

import java.util.List;

import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.runtime.system.context.IsisContext;
import org.apache.isis.core.runtime.system.persistence.PersistenceSession;
import org.apache.isis.viewer.scimpi.Names;
import org.apache.isis.viewer.scimpi.dispatcher.context.RequestState;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request.Scope;
import org.apache.isis.viewer.scimpi.dispatcher.processor.TemplateProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.view.AbstractElementProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.view.determine.InclusionList;

public class Services extends AbstractElementProcessor {

    @Override
    public void process(final TemplateProcessor templateProcessor, RequestState state) {
        final boolean showForms = templateProcessor.isRequested(FORMS, false);
        final String view = templateProcessor.getOptionalProperty(VIEW, "_generic_action." + Names.EXTENSION);
        final String cancelTo = templateProcessor.getOptionalProperty(CANCEL_TO);

        final InclusionList inclusionList = new InclusionList();
        templateProcessor.pushBlock(inclusionList);
        templateProcessor.processUtilCloseTag();

        final List<ObjectAdapter> serviceAdapters = getPersistenceSession().getServices();
        for (final ObjectAdapter adapter : serviceAdapters) {
            final String serviceId = templateProcessor.getContext().mapObject(adapter, Scope.REQUEST);
            templateProcessor.appendHtml("<div class=\"actions\">");
            templateProcessor.appendHtml("<h3>");
            templateProcessor.appendAsHtmlEncoded(adapter.titleString());
            templateProcessor.appendHtml("</h3>");
            Methods.writeMethods(templateProcessor, serviceId, adapter, showForms, inclusionList, view, cancelTo);
            templateProcessor.appendHtml("</div>");
        }
        templateProcessor.popBlock();
    }

    @Override
    public String getName() {
        return "services";
    }

    private static PersistenceSession getPersistenceSession() {
        return IsisContext.getPersistenceSession();
    }

}

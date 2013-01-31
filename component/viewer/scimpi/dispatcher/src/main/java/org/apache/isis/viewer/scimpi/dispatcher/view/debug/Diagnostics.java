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

import org.apache.isis.core.commons.authentication.AuthenticationSession;
import org.apache.isis.core.commons.debug.DebugHtmlString;
import org.apache.isis.core.runtime.system.context.IsisContext;
import org.apache.isis.viewer.scimpi.Names;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request;
import org.apache.isis.viewer.scimpi.dispatcher.context.RequestState;
import org.apache.isis.viewer.scimpi.dispatcher.processor.TemplateProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.view.AbstractElementProcessor;

public class Diagnostics extends AbstractElementProcessor {

    @Override
    public void process(final TemplateProcessor templateProcessor, RequestState state) {
        if (templateProcessor.getContext().isDebugDisabled()) {
            return;
        }

        final String type = templateProcessor.getOptionalProperty(TYPE, "page");
        final boolean isForced = templateProcessor.isRequested("force");
        if (isForced || templateProcessor.getContext().showDebugData()) {
            templateProcessor.appendHtml("<div class=\"debug\">");
            if ("page".equals(type)) {
                templateProcessor.appendHtml("<pre>");
                final Request context = templateProcessor.getContext();
                templateProcessor.appendHtml("URI:  " + context.getUri());
                templateProcessor.appendHtml("\n");
                templateProcessor.appendHtml("File: " + context.fullFilePath(context.getResourceFile()));
                final String result = (String) templateProcessor.getContext().getVariable(Names.RESULT);
                if (result != null) {
                    templateProcessor.appendHtml("\n");
                    templateProcessor.appendHtml("Object: " + result);
                }
                templateProcessor.appendHtml("</pre>");
            } else if ("session".equals(type)) {
                templateProcessor.appendHtml("<pre>");
                final AuthenticationSession session = IsisContext.getAuthenticationSession();
                templateProcessor.appendHtml("Session:  " + session.getUserName() + " " + session.getRoles());
                templateProcessor.appendHtml("</pre>");
            } else if ("variables".equals(type)) {
                final Request context = templateProcessor.getContext();
                final DebugHtmlString debug = new DebugHtmlString();
                debug.appendln("", "");
                context.append(debug, "variables");
                debug.close();
                templateProcessor.appendHtml(debug.toString());
            } else if ("processing".equals(type)) {
                templateProcessor.appendHtml("<pre>");
                templateProcessor.appendHtml(templateProcessor.getContext().getDebugTrace());
                templateProcessor.appendHtml("</pre>");
            } else {
                templateProcessor.appendHtml("<i>No such type " + type + "</i>");
            }
            templateProcessor.appendHtml("</div>");
        }
    }

    @Override
    public String getName() {
        return "diagnostics";
    }

}

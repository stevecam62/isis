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
import org.apache.isis.viewer.scimpi.dispatcher.processor.TagProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.view.AbstractElementProcessor;

public class Diagnostics extends AbstractElementProcessor {

    @Override
    public void process(final TagProcessor tagProcessor) {
        if (tagProcessor.getContext().isDebugDisabled()) {
            return;
        }

        final String type = tagProcessor.getOptionalProperty(TYPE, "page");
        final boolean isForced = tagProcessor.isRequested("force");
        if (isForced || tagProcessor.getContext().showDebugData()) {
            tagProcessor.appendHtml("<div class=\"debug\">");
            if ("page".equals(type)) {
                tagProcessor.appendHtml("<pre>");
                final Request context = tagProcessor.getContext();
                tagProcessor.appendHtml("URI:  " + context.getUri());
                tagProcessor.appendHtml("\n");
                tagProcessor.appendHtml("File: " + context.fullFilePath(context.getResourceFile()));
                final String result = (String) tagProcessor.getContext().getVariable(Names.RESULT);
                if (result != null) {
                    tagProcessor.appendHtml("\n");
                    tagProcessor.appendHtml("Object: " + result);
                }
                tagProcessor.appendHtml("</pre>");
            } else if ("session".equals(type)) {
                tagProcessor.appendHtml("<pre>");
                final AuthenticationSession session = IsisContext.getAuthenticationSession();
                tagProcessor.appendHtml("Session:  " + session.getUserName() + " " + session.getRoles());
                tagProcessor.appendHtml("</pre>");
            } else if ("variables".equals(type)) {
                final Request context = tagProcessor.getContext();
                final DebugHtmlString debug = new DebugHtmlString();
                debug.appendln("", "");
                context.append(debug, "variables");
                debug.close();
                tagProcessor.appendHtml(debug.toString());
            } else if ("processing".equals(type)) {
                tagProcessor.appendHtml("<pre>");
                tagProcessor.appendHtml(tagProcessor.getContext().getDebugTrace());
                tagProcessor.appendHtml("</pre>");
            } else {
                tagProcessor.appendHtml("<i>No such type " + type + "</i>");
            }
            tagProcessor.appendHtml("</div>");
        }
    }

    @Override
    public String getName() {
        return "diagnostics";
    }

}

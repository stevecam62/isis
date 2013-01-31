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

import org.apache.isis.viewer.scimpi.Names;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request;
import org.apache.isis.viewer.scimpi.dispatcher.context.RequestState;
import org.apache.isis.viewer.scimpi.dispatcher.processor.TemplateProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.view.AbstractElementProcessor;

public class DebuggerLink extends AbstractElementProcessor {

    @Override
    public void process(final TemplateProcessor templateProcessor, RequestState state) {
        if (templateProcessor.getContext().isDebugDisabled()) {
            templateProcessor.skipUntilClose();
            return;
        }

        final Request context = templateProcessor.getContext();
        final Object result = context.getVariable(Names.RESULT);
        templateProcessor.appendHtml("<div class=\"debug\">");
        templateProcessor.appendHtml("<a class=\"debug-link\" href=\"/debug/debug.shtml\" target=\"debug\" title=\"debug\" >...</a>");
        if (result != null) {
            templateProcessor.appendHtml(" <a href=\"/debug/object.shtml?_result=" + result + "\" target=\"debug\"  title=\"debug instance\">...</a>");
        }
        templateProcessor.appendHtml(" <span class=\"debug-link\" onclick=\"$('#page-debug').toggle()\" alt=\"show/hide debug details\">...</span>");
        templateProcessor.appendHtml("</div>");

        templateProcessor.processUtilCloseTag();
    }

    @Override
    public String getName() {
        return "debugger";
    }

}

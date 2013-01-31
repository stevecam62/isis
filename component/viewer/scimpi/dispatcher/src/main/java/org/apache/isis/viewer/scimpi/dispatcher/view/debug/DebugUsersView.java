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

import org.apache.isis.viewer.scimpi.dispatcher.context.RequestState;
import org.apache.isis.viewer.scimpi.dispatcher.processor.TemplateProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.view.AbstractElementProcessor;

public class DebugUsersView extends AbstractElementProcessor {

    @Override
    public String getName() {
        return "debug-users";
    }

    @Override
    public void process(final TemplateProcessor templateProcessor, RequestState state) {
        final String view = templateProcessor.getContext().getContextPath() + templateProcessor.getContext().getResourceParentPath() + templateProcessor.getContext().getResourceFile();

        templateProcessor.appendHtml("<form class=\"generic action\" action=\"debug-user.app\" method=\"post\" accept-charset=\"ISO-8859-1\">\n");
        templateProcessor.appendHtml("<div class=\"title\">Add Debug User</div>\n");
        templateProcessor.appendHtml("<div class=\"field\"><label>User Name:</label><input type=\"text\" name=\"name\" size=\"30\" /></div>\n");
        templateProcessor.appendHtml("<input type=\"hidden\" name=\"method\" value=\"add\" />\n");
        templateProcessor.appendHtml("<input type=\"hidden\" name=\"view\" value=\"" + view + "\" />\n");
        templateProcessor.appendHtml("<input class=\"button\" type=\"submit\" value=\"Add User\" />\n");
        templateProcessor.appendHtml("</form>\n");

        templateProcessor.appendHtml("<table class=\"debug\">\n<tr><th class=\"title\">Name</th><th class=\"title\"></th></tr>\n");
        for (final String name : templateProcessor.getContext().getDebugUsers()) {
            templateProcessor.appendHtml("<tr><th>" + name + "</th><th><a href=\"debug-user.app?method=remove&name=" + name + "&view=" + view + " \">remove</a></th></tr>\n");
        }
        templateProcessor.appendHtml("</table>\n");
    }
}

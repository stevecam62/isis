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

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;

import org.apache.isis.viewer.scimpi.dispatcher.context.RequestState;
import org.apache.isis.viewer.scimpi.dispatcher.processor.TemplateProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.view.AbstractElementProcessor;

public class LogLevel extends AbstractElementProcessor {

    @Override
    public void process(final TemplateProcessor templateProcessor, RequestState state) {

        String view = templateProcessor.getOptionalProperty(VIEW, templateProcessor.getViewPath());
        view = templateProcessor.getContext().fullFilePath(view);
        final Level level = LogManager.getRootLogger().getLevel();
        final boolean showSelector = templateProcessor.isRequested(SHOW_SELECT, true);
        if (showSelector) {
            templateProcessor.appendHtml("<form action=\"log.app\" type=\"post\" >");
            templateProcessor.appendHtml("<input type=\"hidden\" name=\"view\" value=\"" + view + "\" />");
            templateProcessor.appendHtml("<select name=\"level\">");
            for (final Level l : new Level[] { Level.OFF, Level.FATAL, Level.ERROR, Level.WARN, Level.INFO, Level.DEBUG, Level.TRACE }) {
                final String settings = level + "\"" + (level == l ? " selected=\"selected\" " : "");
                templateProcessor.appendHtml("<option " + settings + ">" + l + "</option>");
            }
            templateProcessor.appendHtml("<input type=\"submit\" value=\"Change Level\" />");
            templateProcessor.appendHtml("</select>");
            templateProcessor.appendHtml("</form>");
        } else {
            templateProcessor.appendHtml(level.toString());
        }
    }

    @Override
    public String getName() {
        return "log-level";
    }

}

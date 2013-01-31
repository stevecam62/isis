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

package org.apache.isis.viewer.scimpi.dispatcher.view.variable;

import org.apache.isis.viewer.scimpi.dispatcher.context.RequestState;
import org.apache.isis.viewer.scimpi.dispatcher.processor.RequiredPropertyException;
import org.apache.isis.viewer.scimpi.dispatcher.processor.TemplateProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.view.AbstractElementProcessor;

public class SetCookie extends AbstractElementProcessor {

    @Override
    public void process(final TemplateProcessor templateProcessor, RequestState state) {
        final String name = templateProcessor.getRequiredProperty("name");
        final String value = templateProcessor.getOptionalProperty("value");
        final boolean isClear = templateProcessor.getOptionalProperty("action", "set").equals("clear");
        final String expiresString = templateProcessor.getOptionalProperty("expires", "-1");

        if (!isClear && value == null) {
            throw new RequiredPropertyException("Property not set: " + value);
        }
        if (isClear) {
            templateProcessor.appendDebug("cookie: " + name + " (cleared)");
            templateProcessor.getContext().addCookie(name, null, 0);
        } else {
            if (value.length() > 0) {
                templateProcessor.appendDebug("cookie: " + name + " set to"+ value);
                templateProcessor.getContext().addCookie(name, value, Integer.valueOf(expiresString));
            }
        }
    }

    @Override
    public String getName() {
        return "set-cookie";
    }
}

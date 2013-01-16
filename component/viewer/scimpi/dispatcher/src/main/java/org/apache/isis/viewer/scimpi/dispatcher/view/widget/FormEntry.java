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

package org.apache.isis.viewer.scimpi.dispatcher.view.widget;

import org.apache.isis.viewer.scimpi.dispatcher.processor.TagProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.view.AbstractElementProcessor;

public class FormEntry extends AbstractElementProcessor {

    @Override
    public void process(final TagProcessor tagProcessor) {
        final FormFieldBlock block = (FormFieldBlock) tagProcessor.getBlockContent();
        final String field = tagProcessor.getRequiredProperty(FIELD);
        final String value = tagProcessor.getRequiredProperty(VALUE);
        final boolean isHidden = tagProcessor.isRequested(HIDDEN, true);
        block.exclude(field);
        // TODO this is replaced because the field is marked as hidden!
        final String content = "reference <input type=\"" + (isHidden ? "hidden" : "text") + "\" disabled=\"disabled\" name=\"" + field + "\" value=\"" + value + "\" />";
        block.replaceContent(field, content);
    }

    @Override
    public String getName() {
        return "form-entry";
    }

}

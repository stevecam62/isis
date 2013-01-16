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

import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request.Scope;
import org.apache.isis.viewer.scimpi.dispatcher.processor.TagProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.view.AbstractElementProcessor;

/**
 * <swf:selected name="selected" object="${action}" equals="${subaction}" />
 */
public class SelectedObject extends AbstractElementProcessor {

    @Override
    public void process(final TagProcessor tagProcessor) {
        final String name = tagProcessor.getOptionalProperty(NAME, "selected");
        final String objectId = tagProcessor.getRequiredProperty(OBJECT);
        final String equalsId = tagProcessor.getOptionalProperty("equals");
        final String title = tagProcessor.getOptionalProperty(BUTTON_TITLE);

        final ObjectAdapter object = tagProcessor.getContext().getMappedObjectOrResult(objectId);
        final ObjectAdapter other = tagProcessor.getContext().getMappedObjectOrResult(equalsId);
        if (object == other || object.equals(title)) {
            // TODO title is not being used!
            tagProcessor.getContext().addVariable(ID, " id=\"" + name + "\" ", Scope.INTERACTION);
        } else {
            tagProcessor.getContext().addVariable(ID, "", Scope.INTERACTION);
        }
        tagProcessor.closeEmpty();
    }

    @Override
    public String getName() {
        return "selected";
    }

}

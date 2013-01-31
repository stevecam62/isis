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


import org.apache.isis.viewer.scimpi.dispatcher.context.RequestState;
import org.apache.isis.viewer.scimpi.dispatcher.processor.TemplateProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.view.form.ActionFormAbstract;
import org.apache.isis.viewer.scimpi.dispatcher.view.form.CreateFormParameter;

public class ActionForm extends ActionFormAbstract {

    @Override
    public void process(final TemplateProcessor templateProcessor, RequestState state) {
        final CreateFormParameter parameters = new CreateFormParameter();
        parameters.objectId = templateProcessor.getOptionalProperty(OBJECT);
        parameters.methodName = templateProcessor.getRequiredProperty(METHOD);
        parameters.forwardResultTo = templateProcessor.getOptionalProperty(VIEW);
        parameters.forwardVoidTo = templateProcessor.getOptionalProperty(VOID);
        parameters.forwardErrorTo = templateProcessor.getOptionalProperty(ERROR);
        parameters.cancelTo = templateProcessor.getOptionalProperty(CANCEL_TO); 
        parameters.showIcon = templateProcessor.isRequested(SHOW_ICON, showIconByDefault());
        parameters.buttonTitle = templateProcessor.getOptionalProperty(BUTTON_TITLE);
        parameters.formTitle = templateProcessor.getOptionalProperty(FORM_TITLE);
        parameters.labelDelimiter = templateProcessor.getOptionalProperty(LABEL_DELIMITER, ":");
        parameters.formId = templateProcessor.getOptionalProperty(FORM_ID, templateProcessor.nextFormId());
        parameters.resultName = templateProcessor.getOptionalProperty(RESULT_NAME);
        parameters.resultOverride = templateProcessor.getOptionalProperty(RESULT_OVERRIDE);
        parameters.scope = templateProcessor.getOptionalProperty(SCOPE);
        parameters.className = templateProcessor.getOptionalProperty(CLASS, "action full");
        parameters.showMessage = templateProcessor.isRequested(SHOW_MESSAGE, false);
        parameters.completionMessage = templateProcessor.getOptionalProperty(MESSAGE);
        parameters.id = templateProcessor.getOptionalProperty(ID, parameters.methodName);
        createForm(templateProcessor, parameters);
    }

    @Override
    public String getName() {
        return "action-form";
    }

}

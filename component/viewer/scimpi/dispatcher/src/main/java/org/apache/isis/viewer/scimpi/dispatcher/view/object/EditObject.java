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

package org.apache.isis.viewer.scimpi.dispatcher.view.object;

import java.util.ArrayList;
import java.util.List;

import org.apache.isis.core.commons.authentication.AuthenticationSession;
import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.spec.ObjectSpecification;
import org.apache.isis.core.metamodel.spec.feature.ObjectAssociation;
import org.apache.isis.core.metamodel.spec.feature.ObjectAssociationFilters;
import org.apache.isis.core.progmodel.facets.value.booleans.BooleanValueFacet;
import org.apache.isis.core.runtime.system.context.IsisContext;
import org.apache.isis.viewer.scimpi.dispatcher.action.EditAction;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request;
import org.apache.isis.viewer.scimpi.dispatcher.context.RequestState;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request.Scope;
import org.apache.isis.viewer.scimpi.dispatcher.form.FieldEditState;
import org.apache.isis.viewer.scimpi.dispatcher.form.FormState;
import org.apache.isis.viewer.scimpi.dispatcher.processor.TemplateProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.view.form.EditFormAbstract;
import org.apache.isis.viewer.scimpi.dispatcher.view.form.FormFieldBlock;
import org.apache.isis.viewer.scimpi.dispatcher.view.form.HiddenInputField;
import org.apache.isis.viewer.scimpi.dispatcher.view.form.HtmlFormBuilder;
import org.apache.isis.viewer.scimpi.dispatcher.view.form.InputField;

public class EditObject extends EditFormAbstract {

    @Override
    public void process(final TemplateProcessor templateProcessor, RequestState state) {
        final Request context = templateProcessor.getContext();

        final String objectId = templateProcessor.getOptionalProperty(OBJECT);
        final String forwardEditedTo = templateProcessor.getOptionalProperty(VIEW);
        final String forwardErrorTo = templateProcessor.getOptionalProperty(ERROR);
        final String cancelTo = templateProcessor.getOptionalProperty(CANCEL_TO); 
        final boolean hideNonEditableFields = templateProcessor.isRequested(HIDE_UNEDITABLE, false);
        final boolean showIcon = templateProcessor.isRequested(SHOW_ICON, showIconByDefault());
        final String labelDelimiter = templateProcessor.getOptionalProperty(LABEL_DELIMITER, ":");
        String buttonTitle = templateProcessor.getOptionalProperty(BUTTON_TITLE);
        String formTitle = templateProcessor.getOptionalProperty(FORM_TITLE);
        final String formId = templateProcessor.getOptionalProperty(FORM_ID, templateProcessor.nextFormId());
        final String variable = templateProcessor.getOptionalProperty(RESULT_NAME);
        final String resultOverride = templateProcessor.getOptionalProperty(RESULT_OVERRIDE);
        final String scope = templateProcessor.getOptionalProperty(SCOPE);
        final String className = templateProcessor.getOptionalProperty(CLASS, "edit full");
        final String completionMessage = templateProcessor.getOptionalProperty(MESSAGE);

        final ObjectAdapter object = context.getMappedObjectOrResult(objectId);
        final String actualObjectId = context.mapObject(object, Scope.INTERACTION);
        final String version = context.mapVersion(object);

        final String id = templateProcessor.getOptionalProperty(ID, object.getSpecification().getShortIdentifier());

        final FormState entryState = (FormState) context.getVariable(ENTRY_FIELDS);

        final ObjectSpecification specification = object.getSpecification();
        final FormFieldBlock containedBlock = new FormFieldBlock() {
            @Override
            public boolean isVisible(final String name) {
                final ObjectAssociation fld = specification.getAssociation(name);
                final boolean isVisible = fld.isVisible(IsisContext.getAuthenticationSession(), object, where).isAllowed();
                final boolean isUseable = fld.isUsable(IsisContext.getAuthenticationSession(), object, where).isAllowed();
                return isVisible && isUseable;
            }

            @Override
            public ObjectAdapter getCurrent(final String name) {
                ObjectAdapter value = null;
                if (entryState != null) {
                    final FieldEditState field2 = entryState.getField(name);
                    value = field2.getValue();
                }
                if (value == null) {
                    final ObjectAssociation fld = specification.getAssociation(name);
                    value = fld.get(object);
                }
                return value;
            }

            @Override
            public boolean isNullable(final String name) {
                final ObjectAssociation fld = specification.getAssociation(name);
                return !fld.isMandatory();
            }
        };

        templateProcessor.pushBlock(containedBlock);
        templateProcessor.processUtilCloseTag();

        final AuthenticationSession session = IsisContext.getAuthenticationSession();
        List<ObjectAssociation> viewFields = specification.getAssociations(ObjectAssociationFilters.dynamicallyVisible(session, object, where));
        viewFields = containedBlock.includedFields(viewFields);
        final InputField[] formFields = createFields(viewFields);

        initializeFields(context, object, formFields, entryState, !hideNonEditableFields);
        setDefaults(context, object, formFields, entryState, showIcon);

        copyFieldContent(context, object, formFields, showIcon);
        overrideWithHtml(context, containedBlock, formFields);
        String errors = null;
        if (entryState != null && entryState.isForForm(formId)) {
            copyEntryState(context, object, formFields, entryState);
            errors = entryState.getError();
        }

        final String errorView = context.fullFilePath(forwardErrorTo == null ? context.getResourceFile() : forwardErrorTo);
        final List<HiddenInputField> hiddenFields = new ArrayList<HiddenInputField>();
        hiddenFields.add(new HiddenInputField("_" + OBJECT, actualObjectId));
        hiddenFields.add(new HiddenInputField("_" + VERSION, version));
        hiddenFields.add(new HiddenInputField("_" + FORM_ID, formId));
        hiddenFields.add(completionMessage == null ? null : new HiddenInputField("_" + MESSAGE, completionMessage));
        hiddenFields.add(forwardEditedTo == null ? null : new HiddenInputField("_" + VIEW, context.fullFilePath(forwardEditedTo)));
        hiddenFields.add(new HiddenInputField("_" + ERROR, errorView));
        hiddenFields.add(variable == null ? null : new HiddenInputField("_" + RESULT_NAME, variable));
        hiddenFields.add(resultOverride == null ? null : new HiddenInputField("_" + RESULT_OVERRIDE, resultOverride));
        hiddenFields.add(scope == null ? null : new HiddenInputField("_" + SCOPE, scope));

        if (!object.isTransient()) {
            // ensure all booleans are included so the pass back TRUE if set.
            final List<ObjectAssociation> fields2 = object.getSpecification().getAssociations();
            for (int i = 0; i < fields2.size(); i++) {
                final ObjectAssociation field = fields2.get(i);
                if (!viewFields.contains(field) && field.getSpecification().containsFacet(BooleanValueFacet.class)) {
                    final String fieldId = field.getId();
                    final String value = getValue(context, field.get(object));
                    hiddenFields.add(new HiddenInputField(fieldId, value));
                }
            }
        }

        if (formTitle == null) {
            formTitle = specification.getSingularName();
        }

        if (buttonTitle == null) {
            buttonTitle = "Save " + specification.getSingularName();
        } else if (buttonTitle.equals("")) {
            buttonTitle = "Save";
        }

        final HiddenInputField[] hiddenFieldArray = hiddenFields.toArray(new HiddenInputField[hiddenFields.size()]);
        HtmlFormBuilder.createForm(templateProcessor, EditAction.ACTION + ".app", hiddenFieldArray, formFields, className, id, formTitle,
                labelDelimiter, null, null, buttonTitle, errors, cancelTo);
     templateProcessor.popBlock();
    }

    @Override
    public String getName() {
        return "edit";
    }

}

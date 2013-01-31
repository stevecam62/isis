package org.apache.isis.viewer.scimpi.dispatcher.view.form;

import java.util.ArrayList;
import java.util.List;

import org.apache.isis.viewer.scimpi.dispatcher.context.Request;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request.Scope;
import org.apache.isis.viewer.scimpi.dispatcher.form.FieldEditState;
import org.apache.isis.viewer.scimpi.dispatcher.form.FormState;
import org.apache.isis.viewer.scimpi.dispatcher.processor.TemplateProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.view.AbstractElementProcessor;

//TODO combine the common stuff that is found here and in ActionFormAbstract and EditFormAbstract
public abstract class LogonFormAbstract extends AbstractElementProcessor {

    public static void loginForm(final TemplateProcessor templateProcessor, final String view) {
        final String object = templateProcessor.getOptionalProperty(OBJECT);
        final String method = templateProcessor.getOptionalProperty(METHOD, "logon");
        final String result = templateProcessor.getOptionalProperty(RESULT_NAME, "_user");
        final String resultScope = templateProcessor.getOptionalProperty(SCOPE, Scope.SESSION.name());
        final String isisUser = templateProcessor.getOptionalProperty("isis-user", "_web_default");
        final String formId = templateProcessor.getOptionalProperty(FORM_ID, templateProcessor.nextFormId());
        final String labelDelimiter = templateProcessor.getOptionalProperty(LABEL_DELIMITER, ":");
    
        // TODO error if all values are not set (not if use type is not set and all others are still defaults);
    
        if (object != null) {
            Request context = templateProcessor.getContext();
            context.addVariable(LOGON_OBJECT, object, Scope.SESSION);
            context.addVariable(LOGON_METHOD, method, Scope.SESSION);
            context.addVariable(LOGON_RESULT_NAME, result, Scope.SESSION);
            context.addVariable(LOGON_SCOPE, resultScope, Scope.SESSION);
            context.addVariable(PREFIX + "isis-user", isisUser, Scope.SESSION);
            context.addVariable(LOGON_FORM_ID, formId, Scope.SESSION);
        }
        
        final String error = templateProcessor.getOptionalProperty(ERROR, templateProcessor.getContext().getRequestedFile());
        final List<HiddenInputField> hiddenFields = new ArrayList<HiddenInputField>();
        hiddenFields.add(new HiddenInputField(ERROR, error));
        if (view != null) {
            hiddenFields.add(new HiddenInputField(VIEW, view));
        }
        hiddenFields.add(new HiddenInputField("_" + FORM_ID, formId));
    
        final FormState entryState = (FormState) templateProcessor.getContext().getVariable(ENTRY_FIELDS);
        boolean isforThisForm = entryState != null && entryState.isForForm(formId);
        if (entryState != null && entryState.isForForm(formId)) {
        }
        final InputField nameField = createdField("username", "User Name", InputField.TEXT, isforThisForm ? entryState : null);
        final String width = templateProcessor.getOptionalProperty("width");
        if (width != null) {
            final int w = Integer.valueOf(width).intValue();
            nameField.setWidth(w);
        }
        final InputField passwordField = createdField("password", "Password", InputField.PASSWORD, isforThisForm ? entryState : null);
        final InputField[] fields = new InputField[] { nameField, passwordField, };
    
        final String formTitle = templateProcessor.getOptionalProperty(FORM_TITLE);
        final String loginButtonTitle = templateProcessor.getOptionalProperty(BUTTON_TITLE, "Log in");
        final String className = templateProcessor.getOptionalProperty(CLASS, "login");
        final String id = templateProcessor.getOptionalProperty(ID, "logon");
    
        HtmlFormBuilder.createForm(templateProcessor, "logon.app", hiddenFields.toArray(new HiddenInputField[hiddenFields.size()]), fields,
                className, id, formTitle, labelDelimiter, null, null, loginButtonTitle,
                isforThisForm && entryState != null ? entryState.getError() : null , null);        
    }

    protected static InputField createdField(final String fieldName, final String fieldLabel, final int type, final FormState entryState) {
        final InputField nameField = new InputField(fieldName);
        nameField.setType(type);
        nameField.setLabel(fieldLabel);
        if (entryState != null) {
            final FieldEditState fieldState = entryState.getField(fieldName);
            final String entry = fieldState == null ? "" : fieldState.getEntry();
            nameField.setValue(entry);
            final String error = fieldState == null ? "" : fieldState.getError();
            nameField.setErrorText(error);
        }
        return nameField;
    }

    public LogonFormAbstract() {
        super();
    }

}

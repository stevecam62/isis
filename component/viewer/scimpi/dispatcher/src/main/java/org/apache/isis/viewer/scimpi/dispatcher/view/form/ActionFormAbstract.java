package org.apache.isis.viewer.scimpi.dispatcher.view.form;

import java.util.List;

import org.apache.isis.applib.annotation.Where;
import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.spec.ObjectSpecification;
import org.apache.isis.core.metamodel.spec.feature.ObjectAction;
import org.apache.isis.core.metamodel.spec.feature.ObjectActionParameter;
import org.apache.isis.viewer.scimpi.Names;
import org.apache.isis.viewer.scimpi.ScimpiException;
import org.apache.isis.viewer.scimpi.dispatcher.action.ActionAction;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request.Scope;
import org.apache.isis.viewer.scimpi.dispatcher.form.FieldEditState;
import org.apache.isis.viewer.scimpi.dispatcher.form.FormState;
import org.apache.isis.viewer.scimpi.dispatcher.processor.TemplateProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.util.MethodsUtils;
import org.apache.isis.viewer.scimpi.dispatcher.view.AbstractElementProcessor;

// TODO combine the common stuff that is found here and in EditFormAbstract and LoginFormAbstract
public abstract class ActionFormAbstract extends AbstractElementProcessor {

    private static final Where where = Where.OBJECT_FORMS;

    public static void createForm(final TemplateProcessor templateProcessor, final CreateFormParameter parameterObject) {
        createForm(templateProcessor, parameterObject, false);
    }

    public static void createForm(final TemplateProcessor templateProcessor, final CreateFormParameter parameterObject, final boolean withoutProcessing) {
        final Request context = templateProcessor.getContext();
        final ObjectAdapter object = MethodsUtils.findObject(context, parameterObject.objectId);
        final String version = templateProcessor.getContext().mapVersion(object);
        final ObjectAction action = MethodsUtils.findAction(object, parameterObject.methodName);
        // TODO how do we distinguish between overloaded methods?
    
        // REVIEW Is this useful?
        if (action.getParameterCount() == 0) {
            throw new ScimpiException("Action form can only be used for actions with parameters");
        }
        if (parameterObject.showMessage && MethodsUtils.isVisible(object, action, where)) {
            final String notUsable = MethodsUtils.isUsable(object, action, where);
            if (notUsable != null) {
                if (!withoutProcessing) {
                    templateProcessor.skipUntilClose();
                }
                templateProcessor.appendHtml("<div class=\"" + parameterObject.className + "-message\" >");
                templateProcessor.appendAsHtmlEncoded(notUsable);
                templateProcessor.appendHtml("</div>");
                return;
            }
        }
        if (!MethodsUtils.isVisibleAndUsable(object, action, where)) {
            if (!withoutProcessing) {
                templateProcessor.skipUntilClose();
            }
            return;
        }
        final String objectId = context.mapObject(object, Scope.INTERACTION);
        final String errorView = context.fullFilePath(parameterObject.forwardErrorTo == null ? context.getResourceFile() : parameterObject.forwardErrorTo);
        final String voidView = context.fullFilePath(parameterObject.forwardVoidTo == null ? context.getResourceFile() : parameterObject.forwardVoidTo);
        if (action.isContributed() && !action.hasReturn() && parameterObject.resultOverride == null) {
            parameterObject.resultOverride = objectId;
        }
        final HiddenInputField[] hiddenFields = new HiddenInputField[] { new HiddenInputField("_" + OBJECT, objectId), new HiddenInputField("_" + VERSION, version), new HiddenInputField("_" + FORM_ID, parameterObject.formId), new HiddenInputField("_" + METHOD, parameterObject.methodName),
                parameterObject.forwardResultTo == null ? null : new HiddenInputField("_" + VIEW, context.fullFilePath(parameterObject.forwardResultTo)), new HiddenInputField("_" + VOID, voidView), new HiddenInputField("_" + ERROR, errorView),
                parameterObject.completionMessage == null ? null : new HiddenInputField("_" + MESSAGE, parameterObject.completionMessage), parameterObject.scope == null ? null : new HiddenInputField("_" + SCOPE, parameterObject.scope),
                parameterObject.resultOverride == null ? null : new HiddenInputField("_" + RESULT_OVERRIDE, parameterObject.resultOverride), parameterObject.resultName == null ? null : new HiddenInputField("_" + RESULT_NAME, parameterObject.resultName),
                parameterObject.resultName == null ? null : new HiddenInputField(Names.RESULT, (String) templateProcessor.getContext().getVariable(Names.RESULT)) };
    
        // TODO when the block contains a selector tag it doesn't disable it if
        // the field cannot be edited!!!
        final FormFieldBlock containedBlock = new FormFieldBlock() {
            @Override
            public boolean isNullable(final String name) {
                final int index = Integer.parseInt(name.substring(5)) - 1;
                final ObjectActionParameter param = action.getParameters().get(index);
                return param.isOptional();
            }
        };
        templateProcessor.pushBlock(containedBlock);
        if (!withoutProcessing) {
            templateProcessor.processUtilCloseTag();
        }
    
        final FormState entryState = (FormState) context.getVariable(ENTRY_FIELDS);
    
        // TODO the list of included fields should be considered in the next
        // method (see EditObject)
        final InputField[] formFields = createFields(action, object);
        hideExcludedParameters(containedBlock, formFields);
        containedBlock.setUpValues(formFields);
        initializeFields(context, object, action, formFields);
        setDefaults(context, object, action, formFields, entryState, parameterObject.showIcon);
        String errors = null;
        if (entryState != null && entryState.isForForm(parameterObject.formId)) {
            copyEntryState(context, object, action, formFields, entryState);
            errors = entryState.getError();
        }
        overrideWithHtml(context, containedBlock, formFields);
    
        String formTitle;
        if (parameterObject.formTitle == null) {
            formTitle = action.getName();
        } else {
            formTitle = parameterObject.formTitle;
        }
    
        String buttonTitle = parameterObject.buttonTitle;
        if (buttonTitle == null) {
            buttonTitle = action.getName();
        } else if (buttonTitle.equals("")) {
            buttonTitle = "Ok";
        }
    
        HtmlFormBuilder.createForm(templateProcessor, ActionAction.ACTION + ".app", hiddenFields, formFields, parameterObject.className,
                parameterObject.id, formTitle, parameterObject.labelDelimiter, action.getDescription(), action.getHelp(), buttonTitle, errors, parameterObject.cancelTo);
    
        templateProcessor.popBlock();
    }

    private static void hideExcludedParameters(final FormFieldBlock containedBlock, final InputField[] formFields) {
        for (final InputField inputField : formFields) {
            final String id2 = inputField.getName();
            if (!containedBlock.includes(id2)) {
                inputField.setHidden(true);
            }
        }
    }

    private static InputField[] createFields(final ObjectAction action, final ObjectAdapter object) {
        final int parameterCount = action.getParameterCount();
        final InputField[] fields = new InputField[parameterCount];
        for (int i = 0; i < fields.length; i++) {
            fields[i] = new InputField(ActionAction.parameterName(i));
        }
        return fields;
    }

    private static void initializeFields(final Request context, final ObjectAdapter object, final ObjectAction action, final InputField[] fields) {
        final List<ObjectActionParameter> parameters = action.getParameters();
        for (int i = 0; i < fields.length; i++) {
            final InputField field = fields[i];
            final ObjectActionParameter param = parameters.get(i);
            if (action.isContributed() && i == 0) {
                // fields[i].setValue(context.mapObject(object,
                // Scope.INTERACTION));
                fields[i].setType(InputField.REFERENCE);
                fields[i].setHidden(true);
            } else {
    
                fields[i].setHelpReference("xxxhelp");
                final ObjectAdapter[] optionsForParameter = action.getChoices(object)[i];
                FieldFactory.initializeField(context, object, param, optionsForParameter, !param.isOptional(), field);
            }
        }
    }

    /**
     * Sets up the fields with their initial values
     * 
     * @param showIcon
     */
    private static void setDefaults(final Request context, final ObjectAdapter object, final ObjectAction action, final InputField[] fields, final FormState entryState, final boolean showIcon) {
        final ObjectAdapter[] defaultValues = action.getDefaults(object);
        if (defaultValues == null) {
            return;
        }
    
        for (int i = 0; i < fields.length; i++) {
            final InputField field = fields[i];
            final ObjectAdapter defaultValue = defaultValues[i];
    
            final String title = defaultValue == null ? "" : defaultValue.titleString();
            if (field.getType() == InputField.REFERENCE) {
                final ObjectSpecification objectSpecification = action.getParameters().get(i).getSpecification();
                if (defaultValue != null) {
                    final String imageSegment = showIcon ? "<img class=\"small-icon\" src=\"" + context.imagePath(objectSpecification) + "\" alt=\"" + objectSpecification.getShortIdentifier() + "\"/>" : "";
                    final String html = imageSegment + title;
                    final String value = context.mapObject(defaultValue, Scope.INTERACTION);
                    field.setValue(value);
                    field.setHtml(html);
                }
            } else {
                field.setValue(title);
            }
        }
    }

    private static void copyEntryState(final Request context, final ObjectAdapter object, final ObjectAction action, final InputField[] fields, final FormState entryState) {
    
        for (final InputField field : fields) {
            final FieldEditState fieldState = entryState.getField(field.getName());
            if (fieldState != null) {
                if (field.isEditable()) {
                    String entry;
                    entry = fieldState.getEntry();
                    field.setValue(entry);
                }
    
                field.setErrorText(fieldState.getError());
            }
        }
    }

    private static void overrideWithHtml(final Request context, final FormFieldBlock containedBlock, final InputField[] formFields) {
        for (int i = 0; i < formFields.length; i++) {
            final String id = ActionAction.parameterName(i);
            if (containedBlock.hasContent(id)) {
                final String content = containedBlock.getContent(id);
                if (content != null) {
                    formFields[i].setHtml(content);
                    formFields[i].setType(InputField.HTML);
                }
            }
        }
    }

    public ActionFormAbstract() {
        super();
    }

}

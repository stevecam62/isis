package org.apache.isis.viewer.scimpi.dispatcher.view.form;

import java.util.List;

import org.apache.isis.applib.annotation.Where;
import org.apache.isis.core.commons.authentication.AuthenticationSession;
import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.consent.Consent;
import org.apache.isis.core.metamodel.facets.object.parseable.ParseableFacet;
import org.apache.isis.core.metamodel.spec.ObjectSpecification;
import org.apache.isis.core.metamodel.spec.feature.ObjectAssociation;
import org.apache.isis.core.progmodel.facets.object.choices.enums.EnumFacet;
import org.apache.isis.core.runtime.system.context.IsisContext;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request.Scope;
import org.apache.isis.viewer.scimpi.dispatcher.form.FieldEditState;
import org.apache.isis.viewer.scimpi.dispatcher.form.FormState;
import org.apache.isis.viewer.scimpi.dispatcher.view.AbstractElementProcessor;


//TODO combine the common stuff that is found here and in ActionFormAbstract and LoginFormAbstract
public abstract class EditFormAbstract extends AbstractElementProcessor {

    protected final Where where = Where.OBJECT_FORMS;

    public EditFormAbstract() {
        super();
    }

    protected InputField[] createFields(final List<ObjectAssociation> fields) {
        final InputField[] formFields = new InputField[fields.size()];
        int length = 0;
        for (int i = 0; i < fields.size(); i++) {
            if (!fields.get(i).isOneToManyAssociation()) {
                formFields[i] = new InputField(fields.get(i).getId());
                length++;
            }
        }
        final InputField[] array = new InputField[length];
        for (int i = 0, j = 0; i < formFields.length; i++) {
            if (formFields[i] != null) {
                array[j++] = formFields[i];
            }
        }
        return array;
    }

    protected void initializeFields(final Request context, final ObjectAdapter object, final InputField[] formFields, final FormState entryState, final boolean includeUnusableFields) {
        for (final InputField formField : formFields) {
            final String fieldId = formField.getName();
            final ObjectAssociation field = object.getSpecification().getAssociation(fieldId);
            final AuthenticationSession session = IsisContext.getAuthenticationSession();
            final Consent usable = field.isUsable(session, object, where);
            final ObjectAdapter[] options = field.getChoices(object);
            FieldFactory.initializeField(context, object, field, options, field.isMandatory(), formField);
    
            final boolean isEditable = usable.isAllowed();
            if (!isEditable) {
                formField.setDescription(usable.getReason());
            }
            formField.setEditable(isEditable);
            final boolean hiddenField = field.isVisible(session, object, where).isVetoed();
            final boolean unusable = usable.isVetoed();
            final boolean hideAsUnusable = unusable && !includeUnusableFields;
            if (hiddenField || hideAsUnusable) {
                formField.setHidden(true);
            }
        }
    }

    protected void copyFieldContent(final Request context, final ObjectAdapter object, final InputField[] formFields, final boolean showIcon) {
        for (final InputField inputField : formFields) {
            final String fieldName = inputField.getName();
            final ObjectAssociation field = object.getSpecification().getAssociation(fieldName);
            if (field.isVisible(IsisContext.getAuthenticationSession(), object, where).isAllowed()) {
                IsisContext.getPersistenceSession().resolveField(object, field);
                final ObjectAdapter fieldValue = field.get(object);
                if (inputField.isEditable()) {
                    final String value = getValue(context, fieldValue);
                    if (!value.equals("") || inputField.getValue() == null) {
                        inputField.setValue(value);
                    }
                } else {
                    final String entry = getValue(context, fieldValue);
                    inputField.setHtml(entry);
                    inputField.setType(InputField.HTML);
    
                }
    
                if (field.getSpecification().getFacet(ParseableFacet.class) == null) {
                    if (fieldValue != null) {
                        final String iconSegment = showIcon ? "<img class=\"small-icon\" src=\"" + context.imagePath(field.getSpecification()) + "\" alt=\"" + field.getSpecification().getShortIdentifier() + "\"/>" : "";
                        final String entry = iconSegment + fieldValue.titleString();
                        inputField.setHtml(entry);
                    } else {
                        final String entry = "<em>none specified</em>";
                        inputField.setHtml(entry);
                    }
                }
            }
        }
    }

    protected void setDefaults(final Request context, final ObjectAdapter object, final InputField[] formFields, final FormState entryState, final boolean showIcon) {
        for (final InputField formField : formFields) {
            final String fieldId = formField.getName();
            final ObjectAssociation field = object.getSpecification().getAssociation(fieldId);
            final ObjectAdapter defaultValue = field.getDefault(object);
            if (defaultValue == null) {
                continue;
            }
    
            final String title = defaultValue.titleString();
            if (field.getSpecification().containsFacet(ParseableFacet.class)) {
                formField.setValue(title);
            } else if (field.isOneToOneAssociation()) {
                final ObjectSpecification objectSpecification = field.getSpecification();
                if (defaultValue != null) {
                    final String iconSegment = showIcon ? "<img class=\"small-icon\" src=\"" + context.imagePath(objectSpecification) + "\" alt=\"" + objectSpecification.getShortIdentifier() + "\"/>" : "";
                    final String html = iconSegment + title;
                    formField.setHtml(html);
                    final String value = defaultValue == null ? null : context.mapObject(defaultValue, Scope.INTERACTION);
                    formField.setValue(value);
                }
            }
        }
    }

    protected void overrideWithHtml(final Request context, final FormFieldBlock containedBlock, final InputField[] formFields) {
        for (final InputField formField : formFields) {
            final String fieldId = formField.getName();
            if (containedBlock.hasContent(fieldId)) {
                final String content = containedBlock.getContent(fieldId);
                if (content != null) {
                    formField.setHtml(content);
                    formField.setValue(null);
                    formField.setType(InputField.HTML);
                }
            }
        }
    }

    protected void copyEntryState(final Request context, final ObjectAdapter object, final InputField[] formFields, final FormState entryState) {
        for (final InputField formField : formFields) {
            final String fieldId = formField.getName();
            final ObjectAssociation field = object.getSpecification().getAssociation(fieldId);
            if (field.isVisible(IsisContext.getAuthenticationSession(), object, where).isAllowed() && formField.isEditable()) {
                final FieldEditState fieldState = entryState.getField(field.getId());
                final String entry = fieldState == null ? "" : fieldState.getEntry();
                formField.setValue(entry);
                final String error = fieldState == null ? "" : fieldState.getError();
                formField.setErrorText(error);
            }
        }
    }

    protected String getValue(final Request context, final ObjectAdapter field) {
        if (field == null || field.isTransient()) {
            return "";
        }
        final ObjectSpecification specification = field.getSpecification();
        if (specification.containsFacet(EnumFacet.class)) {
            return String.valueOf(field.getObject());
        } else if (specification.getFacet(ParseableFacet.class) == null) {
            return context.mapObject(field, Scope.INTERACTION);
        } else {
            return field.titleString();
        }
    }

}

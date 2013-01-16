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

package org.apache.isis.viewer.scimpi.dispatcher.view.form;

import org.apache.isis.core.commons.exceptions.UnknownTypeException;
import org.apache.isis.viewer.scimpi.dispatcher.processor.TagProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.view.other.HelpLink;

public class HtmlFormBuilder {

    public static void createForm(
            final TagProcessor tagProcessor,
            final String action,
            final HiddenInputField[] hiddenFields,
            final InputField[] fields,
            final String className,
            final String id,
            final String formTitle,
            final String labelDelimiter,
            final String description,
            final String helpReference,
            final String buttonTitle,
            final String errors,
            final String cancelTo) {

        String classSegment = " class=\"" + className + (id == null ? "\"" : "\" id=\"" + id + "\"");
        tagProcessor.appendHtml("<form " + classSegment + " action=\"" + action + "\" method=\"post\" accept-charset=\"UTF-8\">\n");
        if (formTitle != null && formTitle.trim().length() > 0) {
            classSegment = " class=\"title\"";
            tagProcessor.appendHtml("<div" + classSegment + ">");
            tagProcessor.appendAsHtmlEncoded(formTitle);
            tagProcessor.appendHtml("</div>\n");
        }

        // TODO reinstate fieldsets when we can specify them
        // request.appendHtml("<fieldset>\n");

        final String cls = "errors";
        if (errors != null) {
            tagProcessor.appendHtml("<div class=\"" + cls + "\">");
            tagProcessor.appendAsHtmlEncoded(errors);
            tagProcessor.appendHtml("</div>");
        }
        for (final HiddenInputField hiddenField : hiddenFields) {
            if (hiddenField == null) {
                continue;
            }
            tagProcessor.appendHtml("  <input type=\"hidden\" name=\"" + hiddenField.getName() + "\" value=\"");
            tagProcessor.appendAsHtmlEncoded(hiddenField.getValue());
            tagProcessor.appendHtml("\" />\n");
        }
        tagProcessor.appendHtml(tagProcessor.getContext().interactionFields());
        for (final InputField fld : fields) {
            if (fld.isHidden()) {
                tagProcessor.appendHtml("  <input type=\"hidden\" name=\"" + fld.getName() + "\" value=\"");
                tagProcessor.appendAsHtmlEncoded(fld.getValue());
                tagProcessor.appendHtml("\" />\n");
            } else {
                final String errorSegment = fld.getErrorText() == null ? "" : "<span class=\"error\">" + fld.getErrorText() + "</span>";
                final String fieldSegment = createField(fld);
                final String helpSegment = HelpLink.createHelpSegment(fld.getDescription(), fld.getHelpReference());
                final String title = fld.getDescription().equals("") ? "" : " title=\"" + fld.getDescription() + "\"";
                tagProcessor.appendHtml("  <div class=\"field " + fld.getName() + "\"><label class=\"label\" " + title + ">");
                tagProcessor.appendAsHtmlEncoded(fld.getLabel());
                tagProcessor.appendHtml(labelDelimiter + "</label>" + fieldSegment + errorSegment + helpSegment + "</div>\n");
            }
        }

        tagProcessor.appendHtml("  <input class=\"button\" type=\"submit\" value=\"");
        tagProcessor.appendAsHtmlEncoded(buttonTitle);
        tagProcessor.appendHtml("\" name=\"execute\" />\n");
        HelpLink.append(tagProcessor, description, helpReference);
        // TODO alllow forms to be cancelled, returning to previous page.
        // request.appendHtml("  <div class=\"action\"><a class=\"button\" href=\"reset\">Cancel</a></div>");

        if (cancelTo != null) {
            tagProcessor.appendHtml("  <input class=\"button\" type=\"button\" value=\"");
            tagProcessor.appendAsHtmlEncoded("Cancel");
            tagProcessor.appendHtml("\" onclick=\"window.location = '" + cancelTo + "'\" name=\"cancel\" />\n");
        }

        // TODO reinstate fieldsets when we can specify them
        // request.appendHtml("</fieldset>\n");
        tagProcessor.appendHtml("</form>\n");
    }

    private static String createField(final InputField field) {
        if (field.isHidden()) {
            if (field.getType() == InputField.REFERENCE) {
                return createObjectField(field, "hidden");
            } else {
                return "";
            }
        } else {
            if (field.getType() == InputField.HTML) {
                return "<span class=\"value\">" + field.getHtml() + "</span>";
            } else if (field.getOptionsText() != null) {
                return createOptions(field);
            } else if (field.getType() == InputField.REFERENCE) {
                return createObjectField(field, "text");
            } else if (field.getType() == InputField.CHECKBOX) {
                return createCheckbox(field);
            } else if (field.getType() == InputField.PASSWORD) {
                return createPasswordField(field);
            } else if (field.getType() == InputField.TEXT) {
                if (field.getHeight() > 1) {
                    return createTextArea(field);
                } else {
                    return createTextField(field);
                }
            } else {
                throw new UnknownTypeException(field.toString());
            }
        }
    }

    private static String createObjectField(final InputField field, final String type) {
        return field.getHtml();
    }

    private static String createTextArea(final InputField field) {
        final String columnsSegment = field.getWidth() == 0 ? "" : " cols=\"" + field.getWidth() / field.getHeight() + "\"";
        final String rowsSegment = field.getHeight() == 0 ? "" : " rows=\"" + field.getHeight() + "\"";
        final String wrapSegment = !field.isWrapped() ? "" : " wrap=\"off\"";
        final String requiredSegment = !field.isRequired() ? "" : " class=\"required\"";
        final String disabled = field.isEditable() ? "" : " disabled=\"disabled\"";
        final String maxLength = field.getMaxLength() == 0 ? "" : " maxlength=\"" + field.getMaxLength() + "\"";
        return "<textarea" + requiredSegment + " name=\"" + field.getName() + "\"" + columnsSegment + rowsSegment + wrapSegment
                + maxLength + disabled + ">" + TagProcessor.getEncoder().encoder(field.getValue()) + "</textarea>";
    }

    private static String createPasswordField(final InputField field) {
        final String extra = " autocomplete=\"off\"";
        return createTextField(field, "password", extra);
    }

    private static String createTextField(final InputField field) {
        return createTextField(field, "text", "");
    }

    private static String createTextField(final InputField field, final String type, final String additionalAttributes) {
        final String value = field.getValue();
        final String valueSegment = value == null ? "" : " value=\"" + TagProcessor.getEncoder().encoder(value) + "\"";
        final String lengthSegment = field.getWidth() == 0 ? "" : " size=\"" + field.getWidth() + "\"";
        final String maxLengthSegment = field.getMaxLength() == 0 ? "" : " maxlength=\"" + field.getMaxLength() + "\"";
        final String requiredSegment = !field.isRequired() ? "" : " required";
        final String disabled = field.isEditable() ? "" : " disabled=\"disabled\"";
        return "<input class=\"" + field.getDataType() + requiredSegment + "\" type=\"" + type + "\" name=\"" + field.getName() + "\"" + 
                valueSegment + lengthSegment + maxLengthSegment + disabled + additionalAttributes + " />";
    }

    private static String createCheckbox(final InputField field) {
        final String entryText = field.getValue();
        final String valueSegment = entryText != null && entryText.toLowerCase().equals("true") ? " checked=\"checked\"" : "";
        final String disabled = field.isEditable() ? "" : " disabled=\"disabled\"";
        return "<input type=\"checkbox\" name=\"" + field.getName() + "\" value=\"true\" " + valueSegment + disabled + " />";
    }

    private static String createOptions(final InputField field) {
        final String[] options = field.getOptionsText();
        final String[] ids = field.getOptionValues();
        final int length = options.length;
        final String classSegment = field.isRequired() && length == 0 ? " class=\"required\"" : "";
        final String disabled = field.isEditable() ? "" : " disabled=\"disabled\"";
        final StringBuffer str = new StringBuffer();
        str.append("\n  <select name=\"" + field.getName() + "\"" + disabled  + classSegment + ">\n");
        boolean offerOther = false;
        for (int i = 0; i < length; i++) {
            final String selectedSegment = field.getValue() == null || ids[i].equals(field.getValue()) ? " selected=\"selected\"" : "";
            if (field.getType() == InputField.TEXT && options[i].equals("__other")) {
                offerOther = true;
            } else {
                str.append("    <option value=\"" + TagProcessor.getEncoder().encoder(ids[i]) + "\"" + selectedSegment + ">" + TagProcessor.getEncoder().encoder(options[i]) + "</option>\n");
            }
        }
        if (!field.isRequired() || length == 0) {
            final String selectedSegment = field.getValue() == null || field.getValue().equals("") ? " selected=\"selected\"" : "";
            str.append("    <option value=\"null\"" + selectedSegment + "></option>\n");
        }
        if (offerOther) {
            str.append("    <option value=\"-OTHER-\">Other:</option>\n");
        }
        str.append("  </select>");
        if (field.getType() == InputField.TEXT) {
            final String lengthSegment = field.getWidth() == 0 ? "" : " size=\"" + field.getWidth() + "\"";
            final String hideSegment = " style=\"display: none;\" "; // TODO
                                                                     // only
                                                                     // hide
                                                                     // when JS
                                                                     // enabled
            str.append("  <input type=\"text\" name=\"" + field.getName() + "-other\"" + hideSegment + lengthSegment + disabled + " />");
        }
        str.append("\n");
        return str.toString();
    }

}

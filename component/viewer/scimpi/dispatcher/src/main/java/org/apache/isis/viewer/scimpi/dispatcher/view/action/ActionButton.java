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

import org.apache.log4j.Logger;

import org.apache.isis.applib.annotation.Where;
import org.apache.isis.applib.profiles.Localization;
import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.consent.Consent;
import org.apache.isis.core.metamodel.facets.object.parseable.ParseableFacet;
import org.apache.isis.core.metamodel.spec.ObjectSpecification;
import org.apache.isis.core.metamodel.spec.feature.ObjectAction;
import org.apache.isis.core.metamodel.spec.feature.ObjectActionParameter;
import org.apache.isis.core.runtime.system.context.IsisContext;
import org.apache.isis.viewer.scimpi.Names;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request;
import org.apache.isis.viewer.scimpi.dispatcher.context.RequestState;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request.Scope;
import org.apache.isis.viewer.scimpi.dispatcher.processor.TemplateProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.util.MethodsUtils;
import org.apache.isis.viewer.scimpi.dispatcher.view.AbstractElementProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.view.other.HelpLink;

public class ActionButton extends AbstractElementProcessor {
    private static final Logger LOG = Logger.getLogger(ActionButton.class);

    // REVIEW: should provide this rendering context, rather than hardcoding.
    // the net effect currently is that class members annotated with 
    // @Hidden(where=Where.ANYWHERE) or @Disabled(where=Where.ANYWHERE) will indeed
    // be hidden/disabled, but will be visible/enabled (perhaps incorrectly) 
    // for any other value for Where
    private final static Where where = Where.ANYWHERE;

    @Override
    public void process(final TemplateProcessor templateProcessor, RequestState state) {
        final String objectId = templateProcessor.getOptionalProperty(OBJECT);
        final String methodName = templateProcessor.getRequiredProperty(METHOD);
        final String forwardResultTo = templateProcessor.getOptionalProperty(VIEW);
        final String forwardVoidTo = templateProcessor.getOptionalProperty(VOID);
        final String forwardErrorTo = templateProcessor.getOptionalProperty(ERROR);
        final String variable = templateProcessor.getOptionalProperty(RESULT_NAME);
        final String scope = templateProcessor.getOptionalProperty(SCOPE);
        final String buttonTitle = templateProcessor.getOptionalProperty(BUTTON_TITLE);
        String resultOverride = templateProcessor.getOptionalProperty(RESULT_OVERRIDE);
        final String idName = templateProcessor.getOptionalProperty(ID, methodName);
        final String className = templateProcessor.getOptionalProperty(CLASS);
        final boolean showMessage = templateProcessor.isRequested(SHOW_MESSAGE, false);
        final String completionMessage = templateProcessor.getOptionalProperty(MESSAGE);

        final ObjectAdapter object = MethodsUtils.findObject(templateProcessor.getContext(), objectId);
        final String version = templateProcessor.getContext().mapVersion(object);
        final ObjectAction action = MethodsUtils.findAction(object, methodName);

        final ActionContent parameterBlock = new ActionContent(action);
        templateProcessor.pushBlock(parameterBlock);
        templateProcessor.processUtilCloseTag();
        final String[] parameters = parameterBlock.getParameters();
        final ObjectAdapter[] objectParameters;
        
        final ObjectAdapter target;
        if (action.isContributed()) {
            objectParameters= null;
            System.arraycopy(parameters, 0, parameters, 1, parameters.length - 1);
            parameters[0] = templateProcessor.getContext().mapObject(object, Scope.REQUEST);
            target =  action.realTarget(object);
            if (!action.hasReturn() && resultOverride == null) {
                resultOverride = parameters[0];
            }
        } else {
            objectParameters = new ObjectAdapter[parameters.length];
            target = object;
            int i = 0;
            for (final ObjectActionParameter spec : action.getParameters()) {
                final ObjectSpecification type = spec.getSpecification();
                if (parameters[i] == null) {
                    objectParameters[i] = null;
                } else if (type.getFacet(ParseableFacet.class) != null) {
                    final ParseableFacet facet = type.getFacet(ParseableFacet.class);
                    Localization localization = IsisContext.getLocalization(); 
                    objectParameters[i] = facet.parseTextEntry(null, parameters[i], localization); 
                } else {
                    objectParameters[i] = MethodsUtils.findObject(templateProcessor.getContext(), parameters[i]);
                }
                i++;
            }
        }

        if (MethodsUtils.isVisibleAndUsable(object, action, where) && MethodsUtils.canRunMethod(object, action, objectParameters).isAllowed()) {
            // TODO use the form creation mechanism as used in ActionForm
            write(templateProcessor, target, action, parameters, objectId, version, forwardResultTo, forwardVoidTo, forwardErrorTo, variable, scope, buttonTitle, completionMessage, resultOverride, idName, className);
        }

        if (showMessage) {
            final Consent usable = action.isUsable(IsisContext.getAuthenticationSession(), object, where);
            if (usable.isVetoed()) {
                final String notUsable = usable.getReason();
                if (notUsable != null) {
                    String title = buttonTitle == null ? action.getName() : buttonTitle;
                    disabledButton(templateProcessor, title, notUsable, idName, className);
                }
            } else {
                final Consent valid = action.isProposedArgumentSetValid(object, objectParameters);
                final String notValid = valid.getReason();
                if (notValid != null) {
                    String title = buttonTitle == null ? action.getName() : buttonTitle;
                    disabledButton(templateProcessor, title, notValid, idName, className);
                }
            }
        }

        templateProcessor.popBlock();
    }

    private void disabledButton(final TemplateProcessor templateProcessor, final String buttonTitle, String message, String id, String className) {
        if (className == null) {
            className = "access";
        }
        templateProcessor.appendHtml("<div id=\"" + id + "\" class=\"" + className + " disabled-form\">");
        templateProcessor.appendHtml("<div class=\"button disabled\" title=\"");
        templateProcessor.appendAsHtmlEncoded(message);
        templateProcessor.appendHtml("\" >" + buttonTitle);
        templateProcessor.appendHtml("</div>");
        templateProcessor.appendHtml("</div>");
    }

    public static void write(
            final TemplateProcessor templateProcessor,
            final ObjectAdapter object,
            final ObjectAction action,
            final String[] parameters,
            final String objectId,
            final String version,
            String forwardResultTo,
            String forwardVoidTo,
            String forwardErrorTo,
            final String variable,
            final String scope,
            String buttonTitle,
            final String completionMessage,
            final String resultOverride,
            final String idName,
            final String className) {
        final Request context = templateProcessor.getContext();

        buttonTitle = buttonTitle != null ? buttonTitle : action.getName();

        if (action.isVisible(IsisContext.getAuthenticationSession(), object, where).isVetoed()) {
            LOG.info("action not visible " + action.getName());
            return;
        }
        final Consent usable = action.isUsable(IsisContext.getAuthenticationSession(), object, where);
        if (usable.isVetoed()) {
            LOG.info("action not available: " + usable.getReason());
            return;
        }

        /*
         * 
         * TODO this mechanism fails as it tries to process tags - which we dont
         * need! Also it calls action 'edit' (not 'action'). Field[] fields =
         * new Field[0]; HiddenField[] hiddenFields = new HiddenField[] { new
         * HiddenField("service", serviceId), new HiddenField("method",
         * methodName), new HiddenField("view", forwardToView), variable == null
         * ? null : new HiddenField("variable", variable), };
         * Form.createForm(request, buttonTitle, fields, hiddenFields, false);
         */

        final String idSegment = idName == null ? "" : ("id=\"" + idName + "\" ");
        final String classSegment = "class=\"" + (className == null ? "action in-line" : className) + "\"";
        templateProcessor.appendHtml("\n<form " + idSegment + classSegment + " action=\"action.app\" method=\"post\">\n");
        if (objectId == null) {
            templateProcessor.appendHtml("  <input type=\"hidden\" name=\"" + "_" + OBJECT + "\" value=\"" + context.getVariable(Names.RESULT) + "\" />\n");
        } else {
            templateProcessor.appendHtml("  <input type=\"hidden\" name=\"" + "_" + OBJECT + "\" value=\"" + objectId + "\" />\n");
        }
        templateProcessor.appendHtml("  <input type=\"hidden\" name=\"" + "_" + VERSION + "\" value=\"" + version + "\" />\n");
        if (scope != null) {
            templateProcessor.appendHtml("  <input type=\"hidden\" name=\"" + "_" + SCOPE + "\" value=\"" + scope + "\" />\n");
        }
        templateProcessor.appendHtml("  <input type=\"hidden\" name=\"" + "_" + METHOD + "\" value=\"" + action.getId() + "\" />\n");
        if (forwardResultTo != null) {
            forwardResultTo = context.fullFilePath(forwardResultTo);
            templateProcessor.appendHtml("  <input type=\"hidden\" name=\"" + "_" + VIEW + "\" value=\"" + forwardResultTo + "\" />\n");
        }
        if (forwardErrorTo == null) {
            forwardErrorTo = templateProcessor.getContext().getResourceFile();
        }
        forwardErrorTo = context.fullFilePath(forwardErrorTo);
        templateProcessor.appendHtml("  <input type=\"hidden\" name=\"" + "_" + ERROR + "\" value=\"" + forwardErrorTo + "\" />\n");
        if (forwardVoidTo == null) {
            forwardVoidTo = templateProcessor.getContext().getResourceFile();
        }
        forwardVoidTo = context.fullFilePath(forwardVoidTo);
        templateProcessor.appendHtml("  <input type=\"hidden\" name=\"" + "_" + VOID + "\" value=\"" + forwardVoidTo + "\" />\n");
        if (variable != null) {
            templateProcessor.appendHtml("  <input type=\"hidden\" name=\"" + "_" + RESULT_NAME + "\" value=\"" + variable + "\" />\n");
        }
        if (resultOverride != null) {
            templateProcessor.appendHtml("  <input type=\"hidden\" name=\"" + "_" + RESULT_OVERRIDE + "\" value=\"" + resultOverride + "\" />\n");
        }
        if (completionMessage != null) {
            templateProcessor.appendHtml("  <input type=\"hidden\" name=\"" + "_" + MESSAGE + "\" value=\"" + completionMessage + "\" />\n");
        }

        for (int i = 0; i < parameters.length; i++) {
            templateProcessor.appendHtml("  <input type=\"hidden\" name=\"param" + (i + 1) + "\" value=\"" + parameters[i] + "\" />\n");
        }
        templateProcessor.appendHtml(templateProcessor.getContext().interactionFields());
        templateProcessor.appendHtml("  <input class=\"button\" type=\"submit\" value=\"" + buttonTitle + "\" name=\"execute\" title=\"" + action.getDescription() + "\" />");
        HelpLink.append(templateProcessor, action.getDescription(), action.getHelp());
        templateProcessor.appendHtml("\n</form>\n");
    }

    @Override
    public String getName() {
        return "action-button";
    }

}

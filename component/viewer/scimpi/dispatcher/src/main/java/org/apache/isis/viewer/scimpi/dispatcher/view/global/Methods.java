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

package org.apache.isis.viewer.scimpi.dispatcher.view.global;

import java.util.List;

import org.apache.isis.applib.annotation.Where;
import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.spec.ActionType;
import org.apache.isis.core.metamodel.spec.ObjectActionSet;
import org.apache.isis.core.metamodel.spec.feature.ObjectAction;
import org.apache.isis.core.metamodel.spec.feature.ObjectActionContainer.Contributed;
import org.apache.isis.core.runtime.system.context.IsisContext;
import org.apache.isis.viewer.scimpi.Names;
import org.apache.isis.viewer.scimpi.dispatcher.context.RequestState;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request.Scope;
import org.apache.isis.viewer.scimpi.dispatcher.processor.TemplateProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.util.MethodsUtils;
import org.apache.isis.viewer.scimpi.dispatcher.view.AbstractElementProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.view.action.ActionButton;
import org.apache.isis.viewer.scimpi.dispatcher.view.action.ActionForm;
import org.apache.isis.viewer.scimpi.dispatcher.view.determine.InclusionList;
import org.apache.isis.viewer.scimpi.dispatcher.view.form.CreateFormParameter;

public class Methods extends AbstractElementProcessor {

    // REVIEW: should provide this rendering context, rather than hardcoding.
    // the net effect currently is that class members annotated with 
    // @Hidden(where=Where.ANYWHERE) or @Disabled(where=Where.ANYWHERE) will indeed
    // be hidden/disabled, but will be visible/enabled (perhaps incorrectly) 
    // for any other value for Where
    private final static Where where = Where.ANYWHERE;

    @Override
    public void process(final TemplateProcessor templateProcessor, RequestState state) {
        String objectId = templateProcessor.getOptionalProperty(OBJECT);
        final String view = templateProcessor.getOptionalProperty(VIEW, "_generic_action." + Names.EXTENSION);
        // final String cancelTo = tagProcessor.getOptionalProperty(CANCEL_TO);
        final boolean showForms = templateProcessor.isRequested(FORMS, false);
        final ObjectAdapter object = MethodsUtils.findObject(templateProcessor.getContext(), objectId);
        if (objectId == null) {
            objectId = templateProcessor.getContext().mapObject(object, null);
        }

        final InclusionList inclusionList = new InclusionList();
        templateProcessor.pushBlock(inclusionList);
        templateProcessor.processUtilCloseTag();

        templateProcessor.appendHtml("<div class=\"actions\">");
        if (inclusionList.includes("edit") && !object.getSpecification().isService()) {
            templateProcessor.appendHtml("<div class=\"action\">");
            templateProcessor.appendHtml("<a class=\"button\" href=\"_generic_edit." + Names.EXTENSION + "?_result=" + objectId + "\">Edit...</a>");
            templateProcessor.appendHtml("</div>");
        }
        writeMethods(templateProcessor, objectId, object, showForms, inclusionList, view, "_generic.shtml?_result=" + objectId);
        templateProcessor.popBlock();
        templateProcessor.appendHtml("</div>");
    }

    public static void writeMethods(
            final TemplateProcessor templateProcessor,
            final String objectId,
            final ObjectAdapter adapter,
            final boolean showForms,
            final InclusionList inclusionList,
            final String view,
            final String cancelTo) {
        List<ObjectAction> actions = adapter.getSpecification().getObjectActions(ActionType.USER, Contributed.INCLUDED);
        writeMethods(templateProcessor, adapter, actions, objectId, showForms, inclusionList, view, cancelTo);
        // TODO determine if system is set up to display exploration methods
        if (true) {
            actions = adapter.getSpecification().getObjectActions(ActionType.EXPLORATION, Contributed.INCLUDED);
            writeMethods(templateProcessor, adapter, actions, objectId, showForms, inclusionList, view, cancelTo);
        }
        // TODO determine if system is set up to display debug methods
        if (true) {
            actions = adapter.getSpecification().getObjectActions(ActionType.DEBUG, Contributed.INCLUDED);
            writeMethods(templateProcessor, adapter, actions, objectId, showForms, inclusionList, view, cancelTo);
        }
    }

    private static void writeMethods(
            final TemplateProcessor templateProcessor,
            final ObjectAdapter adapter,
            List<ObjectAction> actions,
            final String objectId,
            final boolean showForms,
            final InclusionList inclusionList,
            final String view,
            final String cancelTo) {
        actions = inclusionList.includedActions(actions);
        for (int j = 0; j < actions.size(); j++) {
            final ObjectAction action = actions.get(j);
            if (action instanceof ObjectActionSet) {
                templateProcessor.appendHtml("<div class=\"actions\">");
                writeMethods(templateProcessor, adapter, action.getActions(), objectId, showForms, inclusionList, view, cancelTo);
                templateProcessor.appendHtml("</div>");
            } else if (action.isContributed()) {
                if (action.getParameterCount() == 1 && adapter.getSpecification().isOfType(action.getParameters().get(0).getSpecification())) {
                    if (objectId != null) {
                        final ObjectAdapter target = templateProcessor.getContext().getMappedObject(objectId);
                        final ObjectAdapter realTarget = action.realTarget(target);
                        final String realTargetId = templateProcessor.getContext().mapObject(realTarget, Scope.INTERACTION);
                        writeMethod(templateProcessor, adapter, new String[] { objectId }, action, realTargetId, showForms, view, cancelTo);
                    } else {
                        templateProcessor.appendHtml("<div class=\"action\">");
                        templateProcessor.appendAsHtmlEncoded(action.getName());
                        templateProcessor.appendHtml("???</div>");
                    }
                } else if (!adapter.getSpecification().isService()) {
                    writeMethod(templateProcessor, adapter, new String[0], action, objectId, showForms, view, cancelTo);
                }
            } else {
                writeMethod(templateProcessor, adapter, new String[0], action, objectId, showForms, view, cancelTo);
            }
        }
    }

    private static void writeMethod(
            final TemplateProcessor templateProcessor,
            final ObjectAdapter adapter,
            final String[] parameters,
            final ObjectAction action,
            final String objectId,
            final boolean showForms,
            final String view,
            final String cancelTo) {
        // if (action.isVisible(IsisContext.getSession(), null) &&
        // action.isVisible(IsisContext.getSession(), adapter))
        // {
        if (action.isVisible(IsisContext.getAuthenticationSession(), adapter, where).isAllowed()) {
            templateProcessor.appendHtml("<div class=\"action\">");
            if (IsisContext.getSession() == null) {
                templateProcessor.appendHtml("<span class=\"disabled\" title=\"no user logged in\">");
                templateProcessor.appendAsHtmlEncoded(action.getName());
                templateProcessor.appendHtml("</span>");
                /*
                 * } else if (action.isUsable(IsisContext.getSession(),
                 * null).isVetoed()) {
                 * request.appendHtml("<span class=\"disabled\" title=\"" +
                 * action.isUsable(IsisContext.getSession(), null).getReason() +
                 * "\">"); request.appendHtml(action.getName());
                 * request.appendHtml("</span>");
                 */} else if (action.isUsable(IsisContext.getAuthenticationSession(), adapter, where).isVetoed()) {
                templateProcessor.appendHtml("<span class=\"disabled\" title=\"" + action.isUsable(IsisContext.getAuthenticationSession(), adapter, where).getReason() + "\">");
                templateProcessor.appendAsHtmlEncoded(action.getName());
                templateProcessor.appendHtml("</span>");
            } else {
                final String version = templateProcessor.getContext().mapVersion(adapter);
                if (action.getParameterCount() == 0 || (action.isContributed() && action.getParameterCount() == 1)) {
                    ActionButton.write(templateProcessor, adapter, action, parameters, objectId, version, "_generic." + Names.EXTENSION, null, null, null, null, null, null, null, null, null);
                } else if (showForms) {
                    final CreateFormParameter params = new CreateFormParameter();
                    params.objectId = objectId;
                    params.methodName = action.getId();
                    params.forwardResultTo = "_generic." + Names.EXTENSION;
                    params.buttonTitle = "OK";
                    params.formTitle = action.getName();
                    ActionForm.createForm(templateProcessor, params, true);
                } else {
                    templateProcessor.appendHtml("<a class=\"button\" href=\"" + view + "?_result=" + objectId + "&amp;_" + VERSION + "=" + version + "&amp;_" + METHOD + "=" + action.getId());
                    if (cancelTo != null) {
                        templateProcessor.appendHtml("&amp;_cancel-to=");
                        templateProcessor.appendAsHtmlEncoded("cancel-to=\"" + cancelTo + "\"");
                    }
                    templateProcessor.appendHtml("\" title=\"" + action.getDescription() + "\">");
                    templateProcessor.appendAsHtmlEncoded(action.getName() + "...");
                    templateProcessor.appendHtml("</a>");
                }
            }
            templateProcessor.appendHtml("</div>");
        }
    }

    @Override
    public String getName() {
        return "methods";
    }

}

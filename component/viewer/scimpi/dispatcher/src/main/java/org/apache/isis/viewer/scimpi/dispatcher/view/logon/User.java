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

package org.apache.isis.viewer.scimpi.dispatcher.view.logon;

import org.apache.isis.core.runtime.system.context.IsisContext;
import org.apache.isis.viewer.scimpi.Names;
import org.apache.isis.viewer.scimpi.dispatcher.processor.TagProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.view.AbstractElementProcessor;

public class User extends AbstractElementProcessor {
    private static final String LOGIN_VIEW = "login-view";
    private static final String DEFAULT_LOGIN_VIEW = "login." + Names.EXTENSION;
    private static final String LOGOUT_VIEW = "logout-view";
    private static final String DEFAULT_LOGOUT_VIEW = "logout." + Names.EXTENSION;

    @Override
    public void process(final TagProcessor tagProcessor) {
        final boolean isAuthenticatedn = tagProcessor.getContext().isUserAuthenticated();
        tagProcessor.appendHtml("<div class=\"user\">");
        if (isAuthenticatedn) {
            displayUserAndLogoutLink(tagProcessor);
        } else {
            displayLoginForm(tagProcessor);
        }
        tagProcessor.appendHtml("</div>");
    }

    public void displayLoginForm(final TagProcessor tagProcessor) {
        String loginView = tagProcessor.getOptionalProperty(LOGIN_VIEW);
        if (loginView == null) {
            Logon.loginForm(tagProcessor, ".");
        } else {
            if (loginView.trim().length() == 0) {
                loginView = DEFAULT_LOGIN_VIEW;
            }
            tagProcessor.appendHtml("<a div class=\"link\" href=\"" + loginView + "\">Log in</a>");
        }
    }

    public void displayUserAndLogoutLink(final TagProcessor tagProcessor) {
        String user = tagProcessor.getOptionalProperty(NAME);
        if (user == null) {
            user = (String) tagProcessor.getContext().getVariable("_username");
        }
        if (user == null) {
            user = IsisContext.getAuthenticationSession().getUserName();
        }
        tagProcessor.appendHtml("Welcome <span class=\"name\">");
        tagProcessor.appendAsHtmlEncoded(user);
        tagProcessor.appendHtml("</span>, ");
        final String logoutView = tagProcessor.getOptionalProperty(LOGOUT_VIEW, DEFAULT_LOGOUT_VIEW);
        tagProcessor.appendHtml("<a class=\"link\" href=\"logout.app?view=" + logoutView + "\">Log out</a>");
    }

    @Override
    public String getName() {
        return "user";
    }

}

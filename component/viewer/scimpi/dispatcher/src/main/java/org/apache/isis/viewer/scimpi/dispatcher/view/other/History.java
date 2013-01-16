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
package org.apache.isis.viewer.scimpi.dispatcher.view.other;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request.Scope;
import org.apache.isis.viewer.scimpi.dispatcher.processor.TagProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.util.MethodsUtils;
import org.apache.isis.viewer.scimpi.dispatcher.view.AbstractElementProcessor;

public class History extends AbstractElementProcessor {

    private static final String _HISTORY = "_history";

    static class Crumb {
        String name;
        String link;
    }

    static class Crumbs implements Serializable {
        private static final long serialVersionUID = 1L;
        private static final int MAXIMUM_SIZE = 10;
        private final List<Crumb> crumbs = new ArrayList<Crumb>();

        public void add(final String name, final String link) {
            for (final Crumb crumb : crumbs) {
                if (crumb.link.equals(link)) {
                    crumbs.remove(crumb);
                    crumbs.add(crumb);
                    return;
                }
            }

            final Crumb crumb = new Crumb();
            crumb.name = name;
            crumb.link = link;
            crumbs.add(crumb);

            if (crumbs.size() > MAXIMUM_SIZE) {
                crumbs.remove(0);
            }
        }

        public void clear() {
            crumbs.clear();
        }

        public boolean isEmpty() {
            return crumbs.size() == 0;
        }

        public int size() {
            return crumbs.size();
        }

        public Iterable<Crumb> iterator() {
            return crumbs;
        }

    }

    @Override
    public String getName() {
        return "history";
    }

    @Override
    public void process(final TagProcessor tagProcessor) {
        final String action = tagProcessor.getOptionalProperty("action", "display");
        final Crumbs crumbs = getCrumbs(tagProcessor);
        if (action.equals("display") && crumbs != null) {
            write(crumbs, tagProcessor);
        } else if (action.equals("link")) {
            final String name = tagProcessor.getRequiredProperty(NAME);
            final String link = tagProcessor.getRequiredProperty(LINK_VIEW);
            crumbs.add(name, link);
        } else if (action.equals("object")) {
            final String id = tagProcessor.getOptionalProperty(OBJECT);
            final ObjectAdapter object = MethodsUtils.findObject(tagProcessor.getContext(), id);
            final String name = object.titleString();
            String link = tagProcessor.getRequiredProperty(LINK_VIEW);
            link += "?_result=" + id;
            crumbs.add(name, link);
        } else if (action.equals("return")) {

        } else if (action.equals("clear")) {
            crumbs.clear();
        }

    }

    public void write(final Crumbs crumbs, final TagProcessor tagProcessor) {
        if (crumbs.isEmpty()) {
            return;
        }

        tagProcessor.appendHtml("<div id=\"history\">");
        int i = 0;
        final int length = crumbs.size();
        for (final Crumb crumb : crumbs.iterator()) {
            final String link = crumb.link;
            if (i > 0) {
                tagProcessor.appendHtml("<span class=\"separator\"> | </span>");
            }
            if (i == length - 1 || link == null) {
                tagProcessor.appendHtml("<span class=\"disabled\">");
                tagProcessor.appendHtml(crumb.name);
                tagProcessor.appendHtml("</span>");
            } else {
                tagProcessor.appendHtml("<a class=\"linked\" href=\"" + link + "\">");
                tagProcessor.appendHtml(crumb.name);
                tagProcessor.appendHtml("</a>");
            }
            i++;
        }
        tagProcessor.appendHtml("</div>");
    }

    private Crumbs getCrumbs(final TagProcessor tagProcessor) {
        final Request context = tagProcessor.getContext();
        Crumbs crumbs = (Crumbs) context.getVariable(_HISTORY);
        if (crumbs == null) {
            crumbs = new Crumbs();
            context.addVariable(_HISTORY, crumbs, Scope.SESSION);
        }
        return crumbs;
    }

}

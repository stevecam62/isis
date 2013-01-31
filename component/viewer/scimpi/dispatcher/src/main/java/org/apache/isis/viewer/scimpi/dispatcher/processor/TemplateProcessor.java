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

package org.apache.isis.viewer.scimpi.dispatcher.processor;

import java.util.Stack;

import org.apache.log4j.Logger;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.isis.viewer.scimpi.ScimpiException;
import org.apache.isis.viewer.scimpi.dispatcher.context.Request;
import org.apache.isis.viewer.scimpi.dispatcher.context.RequestState;

public class TemplateProcessor implements PageWriter, HtmlEncoder {

    public class RepeatMarker {
        private final int index;

        private RepeatMarker(final int index) {
            this.index = index;
        }

        public void repeat() {
            TemplateProcessor.this.index = index;
        }
    }

    private static Logger LOG = Logger.getLogger(TemplateProcessor.class);
    public static final boolean ENSURE_VARIABLES_EXIST = true;
    public static final boolean NO_VARIABLE_CHECKING = false;

    private final RequestState state;
    private final Request context;
    private final Stack<Snippet> snippets;
    private final Stack<StringBuffer> buffers;
    private final Stack<BlockContent> blocks;
    private final ElementProcessorLookup processors;
    private int nextFormId;
    private int index = 0;
    private final String path;

    public TemplateProcessor(final String path, final RequestState state, final Request context, final Stack<Snippet> snippets, final ElementProcessorLookup processors) {
        this.path = path;
        this.state = state;
        this.context = context;
        this.snippets = snippets;
        this.processors = processors;

        buffers = new Stack<StringBuffer>();
        blocks = new Stack<BlockContent>();
        pushNewBuffer();
    }

    public void processNextTag() {
        while (index < snippets.size() - 1) {
            index++;
            final Snippet snippet = snippets.get(index);
            if (snippet instanceof HtmlSnippet) {
                appendSnippet((HtmlSnippet) snippet);
            } else {
                final SwfTag tag = (SwfTag) snippet;
                final String name = tag.getName();
                final ElementProcessor processor = processors.getFor(name);
                process(tag, processor);
                if (context.isAborted()) {
                    return;
                }
            }
        }
    }

    private void appendSnippet(final HtmlSnippet snippet) {
        String html = snippet.getHtml();
        try {
            if (snippet.isContainsVariable()) {
                html = context.replaceVariables(html);
            }
            appendHtml(html);
        } catch (final TemplateProcessingException e) {
            throw e;
        } catch (final RuntimeException e) {
            final String replace = "<";
            final String withReplacement = "&lt;";
            html = html.replaceAll(replace, withReplacement);

            throw new TemplateProcessingException("Error while processing html block at " + snippet.errorAt() + " - " + e.getMessage(), html, e);
        }
    }

    @Override
    public void appendAsHtmlEncoded(final String string) {
        appendHtml(encodeHtml(string));
        // appendHtml(string);
    }

    @Override
    public void appendHtml(final String html) {
        final StringBuffer buffer = buffers.peek();
        buffer.append(html);
    }

    public void appendDebug(final String line) {
        context.appendDebugTrace(encodeHtml(line));
    }

    public String encodeHtml(final String text) {
        return StringEscapeUtils.escapeHtml(text);
    }

    public void appendTruncated(String text, final int truncateTo) {
        if (truncateTo > 0 && text.length() > truncateTo) {
            text = text.substring(0, truncateTo) + "...";
        }
        appendAsHtmlEncoded(text);
    }

    private void process(final SwfTag tag, final ElementProcessor processor) {
        try {
            LOG.debug("processing " + processor.getName() + " " + tag);
            appendDebug("\n" + tag.debug());
            if (tag.getType() == SwfTag.END) {
                throw new TemplateProcessingException(tag.errorAt() + " - end tag mistaken for a start tag", tag.toString());
            }
            processor.process(this, state);
        } catch (final TemplateProcessingException e) {
            throw e;
        } catch (final RuntimeException e) {
            throw new TemplateProcessingException("Error while processing " + tag.getName().toLowerCase() + " element at " + tag.errorAt() + " - " + e.getMessage(), tag.toString(), e);
        }
    }

    public void processUtilCloseTag() {
        final SwfTag tag = getTag();
        if (tag.getType() == SwfTag.EMPTY) {
            return;
        }
        while (index < snippets.size() - 1) {
            index++;
            final Snippet snippet = snippets.get(index);
            if (snippet instanceof HtmlSnippet) {
                appendSnippet((HtmlSnippet) snippet);
            } else {
                final SwfTag nextTag = (SwfTag) snippet;
                if (tag.getName().equals(nextTag.getName())) {
                    if (nextTag.getType() == SwfTag.START) {
                    } else {
                        return;
                    }
                }
                final String name = nextTag.getName();
                if (nextTag.getType() == SwfTag.END && !tag.getName().equals(name)) {
                    throw new TemplateProcessingException("Expected " + nextTag.getName().toLowerCase() + " tag but found " + tag.getName().toLowerCase() + " tag at " + nextTag.errorAt(), tag.toString());
                }
                final ElementProcessor processor = processors.getFor(name);
                process(nextTag, processor);
            }
        }
    }

    public void skipUntilClose() {
        final SwfTag tag = getTag();
        if (tag.getType() == SwfTag.EMPTY) {
            if (context.isDebug()) {
                appendHtml("<!-- " + "skipped " + tag + " -->");
            }
            return;
        }
        int depth = 1;
        while (index < snippets.size() - 1) {
            index++;
            final Snippet snippet = snippets.get(index);
            if (snippet instanceof SwfTag) {
                final SwfTag nextTag = (SwfTag) snippet;
                if (tag.getName().equals(nextTag.getName())) {
                    if (nextTag.getType() == SwfTag.START) {
                        depth++;
                    } else {
                        depth--;
                        if (depth == 0) {
                            return;
                        }
                    }
                }
            }
        }
    }

    public void closeEmpty() {
        final SwfTag tag = getTag();
        if (tag.getType() == SwfTag.EMPTY) {
            return;
        }
        if (index < snippets.size()) {
            final Snippet snippet = snippets.get(index);
            if (snippet instanceof SwfTag) {
                final SwfTag nextTag = (SwfTag) snippet;
                if (nextTag.getType() == SwfTag.EMPTY) {
                    return;
                }
            }
        }
        throw new ScimpiException("Empty tag not closed");
    }

    public void pushNewBuffer() {
        final StringBuffer buffer = new StringBuffer();
        buffers.push(buffer);
    }

    public String popBuffer() {
        final String content = buffers.pop().toString();
        return content;
    }

    public SwfTag getTag() {
        return (SwfTag) snippets.get(index);
    }

    public Request getContext() {
        return context;
    }

    public void pushBlock(final BlockContent content) {
        blocks.add(content);
    }

    public BlockContent popBlock() {
        return blocks.pop();
    }

    public BlockContent peekBlock() {
        return blocks.peek();
    }

    public String getViewPath() {
        return path;
    }

    public String nextFormId() {
        return String.valueOf(nextFormId++);
    }

    public String getOptionalProperty(final String name, final String defaultValue) {
        return getOptionalProperty(name, defaultValue, true);
    }

    public String getOptionalProperty(final String name, final String defaultValue, final boolean ensureVariablesExists) {
        final TagAttributes tagAttributes = getTag().getAttributes();
//        return tagAttributes.getOptionalProperty(name, defaultValue, ensureVariablesExists);
        String attribute = tagAttributes.getOptionalProperty(name, defaultValue, ensureVariablesExists);
        return attribute == null ? null : state.replaceVariables(attribute);
    }

    public String getOptionalProperty(final String name) {
        return getOptionalProperty(name, true);
    }

    public String getOptionalProperty(final String name, final boolean ensureVariablesExists) {
        final TagAttributes tagAttributes = getTag().getAttributes();
        // return tagAttributes.getOptionalProperty(name, ensureVariablesExists);
        String attribute = tagAttributes.getOptionalProperty(name, ensureVariablesExists);
        return attribute == null ? null : state.replaceVariables(attribute);
    }

    public TagAttributes getAttributes() {
        return getTag().getAttributes();
    }

    public String getRequiredProperty(final String name) {
        return getRequiredProperty(name, true);
    }

    public String getRequiredProperty(final String name, final boolean ensureVariablesExists) {
        final TagAttributes tagAttributes = getTag().getAttributes();
        return tagAttributes.getRequiredProperty(state, name, ensureVariablesExists);
    }

    public boolean isRequested(final String name) {
        final TagAttributes tagAttributes = getTag().getAttributes();
        return tagAttributes.isRequested(name);
    }

    public boolean isRequested(final String name, final boolean defaultValue) {
        final TagAttributes tagAttributes = getTag().getAttributes();
        return tagAttributes.isRequested(name, defaultValue);
    }

    public boolean isPropertySet(final String name) {
        final TagAttributes tagAttributes = getTag().getAttributes();
        return tagAttributes.isPropertySet(context, name);
    }

    public boolean isPropertySpecified(final String name) {
        final TagAttributes tagAttributes = getTag().getAttributes();
        return tagAttributes.isPropertySpecified(name);
    }

    public RepeatMarker createMarker() {
        return new RepeatMarker(index);
    }
}

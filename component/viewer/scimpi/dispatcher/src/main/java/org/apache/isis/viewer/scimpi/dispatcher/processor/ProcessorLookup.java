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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import org.apache.isis.core.commons.debug.DebugBuilder;

public class ProcessorLookup {
    private final Map<String, ElementProcessor> swfElementProcessors = new HashMap<String, ElementProcessor>();

    public void addElementProcessor(final ElementProcessor action) {
        swfElementProcessors.put("SWF:" + action.getName().toUpperCase(), action);
    }

    public void debug(final DebugBuilder debug) {
        debug.startSection("Recognised tags");
        final Iterator<String> it2 = new TreeSet<String>(swfElementProcessors.keySet()).iterator();
        while (it2.hasNext()) {
            final String name = it2.next();
            debug.appendln(name.toLowerCase(), swfElementProcessors.get(name));
        }
        debug.endSection();
    }

    public ElementProcessor getFor(final String name) {
        return swfElementProcessors.get(name);
    }

}

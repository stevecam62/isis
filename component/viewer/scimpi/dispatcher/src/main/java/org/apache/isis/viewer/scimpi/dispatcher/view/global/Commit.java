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

import org.apache.isis.core.runtime.system.context.IsisContext;
import org.apache.isis.core.runtime.system.transaction.IsisTransactionManager;
import org.apache.isis.viewer.scimpi.dispatcher.context.RequestState;
import org.apache.isis.viewer.scimpi.dispatcher.processor.TemplateProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.view.AbstractElementProcessor;

public class Commit extends AbstractElementProcessor {

    @Override
    public String getName() {
        return "commit";
    }

    @Override
    public void process(final TemplateProcessor templateProcessor, RequestState state) {
        // Note - the session will have changed since the earlier call if a user
        // has logged in or out in the action
        // processing above.
        final IsisTransactionManager transactionManager = IsisContext.getPersistenceSession().getTransactionManager();
        if (transactionManager.getTransaction().getState().canCommit()) {
            transactionManager.endTransaction();
            transactionManager.startTransaction();
        }
    }

}

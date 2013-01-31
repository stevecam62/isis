package org.apache.isis.viewer.scimpi.dispatcher.view;

import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.viewer.scimpi.dispatcher.context.RequestState;

public class TestRequestState implements RequestState {

    private ObjectAdapter adapter;

    public String replaceVariables(String value) {
        return value;
    }

    public String getErrorMessage() {
        return "an error" ;
    }

    public String getStringVariable(String result) {
        return "string-var";
    }

    public ObjectAdapter getMappedObject(String objectId) {
        return adapter;
    }

    public void setObject(ObjectAdapter adapter) {
        this.adapter = adapter;}
    
    
    
}


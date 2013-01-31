package org.apache.isis.viewer.scimpi.dispatcher;

import org.apache.isis.core.commons.config.IsisConfiguration;
import org.apache.isis.core.runtime.system.context.IsisContext;
import org.apache.isis.viewer.scimpi.ScimpiContext;

public class RuntimeScimpiContext implements ScimpiContext {

    public String configurationName(String name) {
        return BASE + name;
    }

    public IsisConfiguration getConfiguration() {
        return IsisContext.getConfiguration();
    }

}

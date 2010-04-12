/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.myfaces.extensions.scripting.core.lifecycle;

import org.apache.myfaces.extensions.scripting.api.ScriptingConst;
import org.apache.myfaces.extensions.scripting.core.support.ContextUtils;
import org.apache.myfaces.extensions.scripting.core.support.MockServletContext;
import org.apache.myfaces.extensions.scripting.core.util.WeavingContext;
import org.apache.myfaces.extensions.scripting.core.util.WeavingContextInitializer;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletContext;

import static org.junit.Assert.*;

/**
 * Unit tests which should secure the startup cycle
 *
 * @author Werner Punz (latest modification by $Author$)
 * @version $Revision$ $Date$
 */

public class StartupTestCase {
    ServletContext context;

    @Before
    public void init() {
        context = ContextUtils.startupSystem();
    }

    @Test
    public void testStartup() {
        assertTrue("Scripting must be enabled", WeavingContext.isScriptingEnabled());
        assertTrue("Configuration must be reachable", WeavingContext.getConfiguration() != null);
        assertTrue("RefreshContext must be set", WeavingContext.getRefreshContext() != null);
        assertTrue("Daemon must be running", WeavingContext.getRefreshContext().getDaemon().isRunning());
        assertTrue("Daemon must be running", WeavingContext.getFileChangedDaemon().isRunning());
        assertTrue("External context must be reachable", WeavingContext.getExternalContext() == context);
        assertTrue("Weaver must be set", WeavingContext.getWeaver() != null);
    }

    @Test
    public void testConfiguration() {
        assertTrue("Compile target dir must be set", WeavingContext.getConfiguration().getCompileTarget() != null);
        assertTrue("Initial compile flag must be set to allow the initial compile", WeavingContext.getConfiguration().isInitialCompile());

        assertTrue("Source dirs per registered scripting engine must be one", WeavingContext.getConfiguration().getSourceDirs(ScriptingConst.ENGINE_TYPE_JSF_JAVA).size() == 1);
        assertTrue("Source dirs per registered scripting engine must be one", WeavingContext.getConfiguration().getSourceDirs(ScriptingConst.ENGINE_TYPE_JSF_GROOVY).size() == 1);
    }

    @Test
    public void testNotFullyStarted() {
        assertTrue("Startup not done yet", context.getAttribute(ScriptingConst.CTX_ATTR_STARTUP) == null);
    }
}
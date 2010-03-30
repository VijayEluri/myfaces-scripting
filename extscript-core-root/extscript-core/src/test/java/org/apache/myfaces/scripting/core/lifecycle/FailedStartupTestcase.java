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

package org.apache.myfaces.scripting.core.lifecycle;

import org.apache.myfaces.scripting.api.ScriptingConst;
import org.apache.myfaces.scripting.core.support.LoggingHandler;
import org.apache.myfaces.scripting.core.support.MockServletContext;
import org.apache.myfaces.scripting.core.util.WeavingContext;
import org.apache.myfaces.scripting.core.util.WeavingContextInitializer;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletContext;

import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.*;

/**
 * A Test case simulating failed startup conditions
 * (first a missing servlet filter)
 *
 * @author Werner Punz (latest modification by $Author$)
 * @version $Revision$ $Date$
 */

public class FailedStartupTestCase {
    ServletContext context;
    LoggingHandler handler;
    Logger logger;
    private static final String MSG_DISABLED = "Scripting must be disabled";
    private static final String VALID_PATH = "../../src/test/resources/brokenwebapp";
    private static final String INVALID_PATH = "../../src/test/resources/nonexisting";

    @Before
    public void init() {
        logger = Logger.getLogger(WeavingContextInitializer.class.getName());
        handler = new LoggingHandler();
        /*
        * we suppress the original handlers because we do not
        * want unwanted messages in our console
        */
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);
        logger.setLevel(Level.INFO);
    }

    @Test
    public void testStartup() {
        context = new MockServletContext(VALID_PATH);
        WeavingContextInitializer.initWeavingContext(context);
        assertFalse(MSG_DISABLED, WeavingContext.isScriptingEnabled());
        assertTrue(handler.getOutput().toString().contains(ScriptingConst.ERR_SERVLET_FILTER));
    }

    @Test
    public void testWebxmlMissing() {
        context = new MockServletContext(INVALID_PATH);
        WeavingContextInitializer.initWeavingContext(context);
        assertFalse(MSG_DISABLED, WeavingContext.isScriptingEnabled());
        assertTrue(handler.getOutput().toString().contains(ScriptingConst.ERR_SERVLET_FILTER));
    }
}

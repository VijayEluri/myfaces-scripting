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

package org.apache.myfaces.scripting.core;

import org.apache.myfaces.scripting.api.BaseWeaver;
import org.apache.myfaces.scripting.api.DynamicCompiler;
import org.apache.myfaces.scripting.api.ScriptingConst;
import org.apache.myfaces.scripting.core.probes.MethodReloadingProbe;
import org.apache.myfaces.scripting.core.probes.Probe;
import org.apache.myfaces.scripting.core.util.ReflectUtil;
import org.apache.myfaces.scripting.core.util.WeavingContext;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertTrue;

/**
 * TestCase for our method level reloading handler
 *
 * @author Werner Punz (latest modification by $Author$)
 * @version $Revision$ $Date$
 */

public class MethodLevelReloadingHandlerTest {

    MethodReloadingProbe _probe;

    /**
     * This weaver does nothing except instantiating
     * the object anew at every reload instance request
     * <p/>
     * we need it to simulate the object level reloading
     * at every method call
     */
    class ObjectReladingWeaver extends BaseWeaver {

        Class _clazz;

        public ObjectReladingWeaver(Class clazz) {
            super();
            _clazz = clazz;
        }

        @Override
        public boolean isDynamic(Class clazz) {
            return true;
        }

        public void scanForAddedClasses() {
        }

        @Override
        protected DynamicCompiler instantiateCompiler() {
            return null;
        }

        @Override
        protected String getLoadingInfo(String file) {
            return null;
        }

        @Override
        public Class reloadScriptingClass(Class aclass) {
            return aclass;
        }

        @Override
        public Object reloadScriptingInstance(Object scriptingInstance, int artifactType) {
            return ReflectUtil.instantiate(_clazz);
        }
    }

    /**
     * Before
     */
    @Before
    public void init() {
        WeavingContext.setWeaver(new ObjectReladingWeaver(Probe.class));
        _probe = new Probe();
    }

    @Test
    public void testInvoke() throws Exception {
        MethodLevelReloadingHandler handler = new MethodLevelReloadingHandler(_probe, ScriptingConst.ARTIFACT_TYPE_PHASELISTENER);

        ReflectUtil.executeMethod(handler, "testMethod1");
        assertTrue("objects must be replaced", handler.getDelegate() != _probe && handler.getDelegate() != null);
        Object secondObject = handler.getDelegate();
        Boolean retVal = (Boolean) ReflectUtil.executeMethod(handler, "testMethod3", "true");
        assertTrue(retVal); 
        assertTrue("objects must be replaced", handler.getDelegate() != secondObject && handler.getDelegate() != null);

    }
}

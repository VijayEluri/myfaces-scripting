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
package org.apache.myfaces.scripting.core.reloading;

import org.apache.myfaces.scripting.api.ReloadingStrategy;
import org.apache.myfaces.scripting.api.ScriptingWeaver;

/**
 * @author Werner Punz (latest modification by $Author$)
 * @version $Revision$ $Date$
 *          <p/>
 *          The managed beans have a different reloading
 *          strategy. We follow the route of dropping
 *          all dynamic beans for now which seems to be a middle ground
 *          between simple (do nothing at all except simple bean reloading)
 *          and graph dependency check (drop only the dependend objects and the
 *          referencing objects)
 */

public class ManagedBeanReloadingStrategy implements ReloadingStrategy {

    ScriptingWeaver _weaver;

    public ManagedBeanReloadingStrategy(ScriptingWeaver weaver) {
        _weaver = weaver;
    }

    public ManagedBeanReloadingStrategy() {
    }

    /**
     * In our case the dropping already has happened at request time
     * no need for another reloading here
     *
     * @param scriptingInstance the instance which has to be reloaded
     * @param artifactType      the type of artifact
     * @return does nothing in this case and returns only the original instance, the reloading is handled
     *         for managed beans on another level
     */
    public Object reload(Object scriptingInstance, int artifactType) {
        return scriptingInstance;
    }

    public ScriptingWeaver getWeaver() {
        return _weaver;
    }

    public void setWeaver(ScriptingWeaver weaver) {
        _weaver = weaver;
    }

}
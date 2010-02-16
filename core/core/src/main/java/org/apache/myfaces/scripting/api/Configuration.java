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
package org.apache.myfaces.scripting.api;

import org.apache.myfaces.scripting.core.util.FileUtils;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Configuration class keeping all the core elements of the configuration
 * this class has to be thread save in its internal data structures!
 * <p/>
 * since the end goal is that a single thread has to preinit the config
 * we don�t have to synchronize its access!
 * <p/>
 */
public class Configuration {

    /**
     * the source dirs per scripting engine
     */
    volatile Map<Integer, CopyOnWriteArrayList<String>> _sourceDirs = new ConcurrentHashMap<Integer, CopyOnWriteArrayList<String>>();

    volatile File _compileTarget = FileUtils.getTempDir();

    boolean _initialCompile = true;

    List<String> _packageWhiteList = new LinkedList<String>();

    List<String> _additionalClassPath = new LinkedList<String>();

    /**
     * we keep track of separate resource dirs
     * for systems which can use resource loaders
     * <p/>
     * so that we can load various resources as well
     * from separate source directories instead
     */
    volatile List<String> _resourceDirs = new CopyOnWriteArrayList<String>();

    public Collection<String> getSourceDirs(int scriptingEngine) {
        return _sourceDirs.get(scriptingEngine);
    }

    public void addSourceDir(int scriptingEngine, String sourceDir) {
        CopyOnWriteArrayList<String> dirs = _sourceDirs.get(scriptingEngine);
        if (dirs == null) {
            dirs = new CopyOnWriteArrayList<String>();
            _sourceDirs.put(scriptingEngine, dirs);
        }
        dirs.add(sourceDir);
    }

    public File getCompileTarget() {
        return _compileTarget;
    }

    public void addResourceDir(String dir) {
        _resourceDirs.add(dir);
    }

    public List<String> getResourceDirs() {
        return _resourceDirs;
    }

    public boolean isInitialCompile() {
        return _initialCompile;
    }

    public void setInitialCompile(boolean initialCompile) {
        this._initialCompile = initialCompile;
    }

    public void addWhitelistPackage(String pkg) {
        _packageWhiteList.add(pkg);
    }

    public List<String> getPackageWhiteList() {
        return _packageWhiteList;
    }

    public void setPackageWhiteList(List<String> packageWhiteList) {
        this._packageWhiteList = packageWhiteList;
    }

    public void addAdditionalClassPath(String path) {
        _additionalClassPath.add(path);
    }

    public List<String> getAdditionalClassPath() {
        return _additionalClassPath;
    }

    public void setAdditionalClassPath(List<String> additionalClassPath) {
        this._additionalClassPath = additionalClassPath;
    }
}

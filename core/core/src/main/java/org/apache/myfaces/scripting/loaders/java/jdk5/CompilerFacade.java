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
package org.apache.myfaces.scripting.loaders.java.jdk5;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.myfaces.scripting.api.DynamicCompiler;
import org.apache.myfaces.scripting.core.util.ClassUtils;
import org.apache.myfaces.scripting.loaders.java.RecompiledClassLoader;

import java.io.File;

/**
 * @author Werner Punz (latest modification by $Author$)
 * @version $Revision$ $Date$
 *          <p/>
 *          Custom compiler call for jdk5
 *          we can call javac directly
 */

public class CompilerFacade implements DynamicCompiler {
    JavacCompiler compiler = null;

    Log log = LogFactory.getLog(this.getClass());
    ContainerFileManager fileManager = null;


    public CompilerFacade() {
        super();

        compiler = new JavacCompiler();
        fileManager = new ContainerFileManager();
    }


    public Class compileFile(String sourceRoot, String classPath, String filePath) throws ClassNotFoundException {

        String className = filePath.replaceAll(File.separator, ".");
        className = ClassUtils.relativeFileToClassName(className);

        try {
            CompilationResult result = compiler.compile(new File(sourceRoot), fileManager.getTempDir(), filePath, fileManager.getClassPath());

            displayMessages(result);

            if (result.getErrors().size() == 0) {
                ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
                if (!(oldClassLoader instanceof RecompiledClassLoader)) {
                    try {
                        RecompiledClassLoader classLoader = (RecompiledClassLoader) fileManager.getClassLoader();
                        Thread.currentThread().setContextClassLoader(classLoader);

                        ClassUtils.markAsDynamicJava(fileManager.getTempDir().getAbsolutePath(), className);

                        return classLoader.loadClass(className);
                    } finally {
                        Thread.currentThread().setContextClassLoader(oldClassLoader);
                    }
                }
            } else {
                log.error("Compiler output:"+result.getCompilerOutput());
            }

        } catch (CompilationException e) {
            log.error(e);
        }
        return null;
    }

    public void compileAllFiles(String sourceRoot, String classPath) throws ClassNotFoundException {
        //TODO implement this
    }

    private void displayMessages(CompilationResult result) {
        for (CompilationResult.CompilationMessage error : result.getErrors()) {
            log.error(error.getLineNumber()+"-"+error.getMessage());

        }
        for (CompilationResult.CompilationMessage error : result.getWarnings()) {
            log.error(error.getMessage());
        }
    }
}
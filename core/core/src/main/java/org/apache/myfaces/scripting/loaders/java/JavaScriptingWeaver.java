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
package org.apache.myfaces.scripting.loaders.java;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.myfaces.scripting.api.*;
import org.apache.myfaces.scripting.core.util.ReflectUtil;
import org.apache.myfaces.scripting.core.util.ClassUtils;
import org.apache.myfaces.scripting.core.util.WeavingContext;
import org.apache.myfaces.scripting.refresh.FileChangedDaemon;
import org.apache.myfaces.scripting.refresh.ReloadingMetadata;
import org.apache.myfaces.config.element.ManagedBean;
import org.apache.myfaces.config.RuntimeConfig;
//import org.apache.myfaces.scripting.loaders.java.jsr199.ReflectCompilerFacade;

import javax.servlet.ServletContext;
import javax.faces.context.FacesContext;
import java.io.File;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * @author werpu
 *         <p/>
 *         The Scripting Weaver for the java core which reloads the java scripts
 *         dynamically upon change
 */
public class JavaScriptingWeaver extends BaseWeaver implements ScriptingWeaver, Serializable {

    Log log = LogFactory.getLog(JavaScriptingWeaver.class);
    String classPath = "";
    DynamicClassIdentifier identifier = new DynamicClassIdentifier();

    private static final String JAVA_FILE_ENDING = ".java";
    private static final String JSR199_COMPILER = "org.apache.myfaces.scripting.loaders.java.jsr199.JSR199Compiler";
    private static final String JAVA5_COMPILER = "org.apache.myfaces.scripting.loaders.java.jdk5.CompilerFacade";

    AnnotationScanner _scanner = null;


    /**
     * helper to allow initial compiler classpath scanning
     *
     * @param servletContext
     */
    public JavaScriptingWeaver(ServletContext servletContext) {
        super(JAVA_FILE_ENDING, ScriptingConst.ENGINE_TYPE_JAVA);
        //init classpath removed we can resolve that over the
        //url classloader at the time myfaces is initialized
        try {
            Class scanner = ClassUtils.getContextClassLoader().loadClass("org.apache.myfaces.scripting.jsf2.annotation.JavaAnnotationScanner");
            this._scanner = (AnnotationScanner) scanner.newInstance();

            FileChangedDaemon.getInstance().getSystemRecompileMap().put(ScriptingConst.ENGINE_TYPE_JAVA, Boolean.TRUE);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            //we do nothing here
        }

    }

    @Override
    public void appendCustomScriptPath(String scriptPath) {
        super.appendCustomScriptPath(scriptPath);
        if (_scanner != null) {
            _scanner.addScanPath(scriptPath);
        }
    }

    public JavaScriptingWeaver() {
        super(JAVA_FILE_ENDING, ScriptingConst.ENGINE_TYPE_JAVA);
    }


    /**
     * helper to map the properties wherever possible
     *
     * @param target
     * @param src
     */
    protected void mapProperties(Object target, Object src) {
        try {
            //TODO add dynamic property reloading hook here
            BeanUtils.copyProperties(target, src);
        } catch (IllegalAccessException e) {
            log.debug(e);
            //this is wanted
        } catch (InvocationTargetException e) {
            log.debug(e);
            //this is wanted
        }
    }


    /**
     * loads a class from a given sourceroot and filename
     * note this method does not have to be thread safe
     * it is called in a thread safe manner by the base class
     *
     * @param sourceRoot the source search lookup path
     * @param file       the filename to be compiled and loaded
     * @return a valid class if it could be found, null if none was found
     */
    @Override
    protected Class loadScriptingClassFromFile(String sourceRoot, String file) {
        //we load the scripting class from the given className
        File currentClassFile = new File(sourceRoot + File.separator + file);
        if (!currentClassFile.exists()) {
            return null;
        }

        if (log.isInfoEnabled()) {
            log.info("Loading Java file:" + file);
        }

        Iterator<String> it = scriptPaths.iterator();
        Class retVal = null;

        try {
            //we initialize the compiler lazy
            //because the facade itself is lazy
            DynamicCompiler compiler = (DynamicCompiler) ReflectUtil.instantiate(getScriptingFacadeClass());//new ReflectCompilerFacade();
            retVal = compiler.compileFile(sourceRoot, classPath, file);
        } catch (ClassNotFoundException e) {
            //can be safely ignored
        }

        if (retVal != null) {
            refreshReloadingMetaData(sourceRoot, file, currentClassFile, retVal, ScriptingConst.ENGINE_TYPE_JAVA);
        }

        /**
         * we now scan the return value and update its configuration parameters if needed
         * this can help to deal with method level changes of class files like managed properties
         * or scope changes from shorter running scopes to longer running ones
         * if the annotation has been moved the class will be deregistered but still delivered for now
         *
         * at the next refresh the second step of the registration cycle should pick the new class up
         * //TODO we have to mark the artefacting class as deregistered and then enforce
         * //a reload this is however not the scope of the commit of this subtask
         * //we only deal with class level reloading here
         * //the deregistration notification should happen on artefact level (which will be the next subtask)
         */
        if (_scanner != null) {
            _scanner.scanClass(retVal);
        }

        return retVal;
    }

    private String getScriptingFacadeClass() {
        String javaVer = System.getProperty("java.version");
        String[] versionArr = javaVer.split("\\.");

        int major = Integer.parseInt(versionArr[Math.min(versionArr.length, 1)]);

        if (major > 5) {
            //jsr199 compliant jdk
            return JSR199_COMPILER;
        }
        //otherwise 
        return JAVA5_COMPILER;
    }

    public boolean isDynamic(Class clazz) {
        return identifier.isDynamic(clazz);  //To change body of implemented methods use File | Settings | File Templates.
    }


    /**
     * full scan, scans for all artefacts in all files
     */
    public void fullAnnotationScan() {
        if (_scanner == null) {
            return;
        }
        _scanner.scanPaths();
    }

    public void fullRecompile() {
        if (isFullyRecompiled()) {
            return;
        }

        DynamicCompiler compiler = (DynamicCompiler) ReflectUtil.instantiate(getScriptingFacadeClass());//new ReflectCompilerFacade();
        for (String scriptPath : getScriptPaths()) {
            //compile via javac dynamically, also after this block dynamic compilation
            //for the entire length of the request,
            try {
                compiler.compileAllFiles(scriptPath, classPath);
            } catch (ClassNotFoundException e) {
                log.error(e);
            }

        }

        markAsFullyRecompiled();
    }

    public void requestRefresh() {
        if (
              FileChangedDaemon.getInstance().getSystemRecompileMap().get(ScriptingConst.ENGINE_TYPE_JAVA) == null ||
              FileChangedDaemon.getInstance().getSystemRecompileMap().get(ScriptingConst.ENGINE_TYPE_JAVA)
           ) {
            fullRecompile();
            //TODO if managed beans are tainted we have to do a full drop
            refreshManagedBeans();
        }
    }

    private void refreshManagedBeans() {
        if(FacesContext.getCurrentInstance() == null) {
            return;//no npe allowed    
        }
        Set<String> tainted = new HashSet<String>();
        for (Map.Entry<String, ReloadingMetadata> it : FileChangedDaemon.getInstance().getClassMap().entrySet()) {
            if (it.getValue().getScriptingEngine() == ScriptingConst.ENGINE_TYPE_JAVA && it.getValue().isTainted()) {
                tainted.add(it.getValue().getAClass().getName());
            }
        }
        if (tainted.size() > 0) {
            boolean managedBeanTainted = false;
            //We now have to check if the tainted classes belong to the managed beans
            Set<String> managedBeanClasses = new HashSet<String>();
            Map<String, ManagedBean> mbeans = RuntimeConfig.getCurrentInstance(FacesContext.getCurrentInstance().getExternalContext()).getManagedBeans();
            for (Map.Entry<String, ManagedBean> entry : mbeans.entrySet()) {
                managedBeanClasses.add(entry.getValue().getManagedBeanClassName());
            }
            for (String taintedClass : tainted) {
                if (managedBeanClasses.contains(taintedClass)) {
                    managedBeanTainted = true;
                    break;
                }
            }
            if (managedBeanTainted) {
                for (Map.Entry<String, ManagedBean> entry : mbeans.entrySet()) {
                    Class managedBeanClass = entry.getValue().getManagedBeanClass();
                    if (WeavingContext.isDynamic(managedBeanClass)) {
                        //managed bean class found we drop the class from our session
                        removeBeanReferences(entry.getValue());
                    }
                }

            }
        }
    }

    /**
     * removes the references from out static scope
     * for jsf2 we probably have some kind of notification mechanism
     * which notifies custom scopes
     *
     * @param bean
     */
    private void removeBeanReferences(ManagedBean bean) {
        getLog().info("JavaScriptingWeaver.removeBeanReferences(" + bean.getManagedBeanName() + ")");

        String scope = bean.getManagedBeanScope();

        if (scope != null && scope.equalsIgnoreCase("session")) {
            FacesContext.getCurrentInstance().getExternalContext().getSessionMap().remove(bean.getManagedBeanName());
        } else if (scope != null && scope.equalsIgnoreCase("application")) {
            FacesContext.getCurrentInstance().getExternalContext().getApplicationMap().remove(bean.getManagedBeanName());
        } else if (scope != null) {
            Object scopeImpl = FacesContext.getCurrentInstance().getExternalContext().getApplicationMap().get(scope);
            if (scopeImpl == null) return; //scope not implemented
            //we now have to revert to introspection here because scopes are a pure jsf2 construct
            //so we use a messaging pattern here to cope with it
            ReflectUtil.executeMethod(scopeImpl, "remove", bean.getManagedBeanName());
        }
    }
    

    private void markAsFullyRecompiled() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context != null) {
            //mark the request as tainted with recompile
            if (context != null) {
                Map<String, Object> requestMap = context.getExternalContext().getRequestMap();
                requestMap.put(JavaScriptingWeaver.class.getName() + "_recompiled", Boolean.TRUE);
            }
        }
        FileChangedDaemon.getInstance().getSystemRecompileMap().put(ScriptingConst.ENGINE_TYPE_JAVA,Boolean.FALSE);
    }

    private boolean isFullyRecompiled() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context != null) {
            return context.getExternalContext().getRequestMap().containsKey(JavaScriptingWeaver.class.getName() + "_recompiled");
        }
        return false;
    }

}

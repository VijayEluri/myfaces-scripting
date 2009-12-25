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
package org.apache.myfaces.scripting.servlet;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.myfaces.scripting.api.Configuration;
import org.apache.myfaces.scripting.api.ScriptingConst;
import org.apache.myfaces.scripting.api.ScriptingWeaver;
import org.apache.myfaces.scripting.core.util.ClassUtils;
import org.apache.myfaces.scripting.core.util.WeavingContext;
import org.apache.myfaces.scripting.refresh.RefreshContext;
import org.apache.myfaces.webapp.StartupListener;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author werpu
 * @date: 14.08.2009
 * <p/>
 * Startup context plugin chainloader
 * for myfaces 1.2.x,
 * we hook ourselfs into the startup event
 * system we have for myfaces 1.2.x+ to do the initial
 * configuration before the myfaces init itself starts!
 */
public class StartupServletContextPluginChainLoader implements StartupListener {
    final Log log = LogFactory.getLog(this.getClass());


    public void preInit(ServletContextEvent servletContextEvent) {

        log.info("[EXT-SCRIPTING] Instantiating StartupServletContextPluginChainLoader");

        ServletContext servletContext = servletContextEvent.getServletContext();
        if (servletContext == null) return;

        servletContext.setAttribute(ScriptingConst.CTX_REQUEST_CNT, new AtomicInteger(0));

        initConfig(servletContext);
        CustomChainLoader loader = initChainLoader(servletContext);
        ScriptingWeaver weaver = initScriptingWeaver(servletContext, loader);
        initRefreshContext(servletContext);

        initInitialCompileAndScan(weaver);
    }


    /**
     * initiates the first compile and scan in the subsystem
     *
     * @param weaver our weaver which reveices the trigger calls
     */
    private final void initInitialCompileAndScan(ScriptingWeaver weaver) {
        //log.info("[EXT-SCRIPTING] Compiling all sources for the first time");
        weaver.requestRefresh();
        //weaver.fullClassScan();
    }

    /**
     * initialisation of the refresh context object
     * the refresh context, is a context object which keeps
     * the refresh information (refresh time, needs refresh) etc...
     *
     * @param servletContext the servlet context singleton which keeps
     *                       the context for distribution
     */
    private final void initRefreshContext(ServletContext servletContext) {
        RefreshContext rContext = new RefreshContext();
        servletContext.setAttribute("RefreshContext", rContext);
        WeavingContext.setRefreshContext(rContext);
    }

    /**
     * The initialisation of our global weaver chain
     * which triggers the various subweavers depending
     * on the scripting engine plugged in.
     *
     * @param servletContext the application scoped holder for our weaver
     * @param loader         the chain loader which serves the weavers
     * @return the weaver instance wich is generated and stored
     */
    private final ScriptingWeaver initScriptingWeaver(ServletContext servletContext, CustomChainLoader loader) {
        ScriptingWeaver weaver = loader.getScriptingWeaver();
        servletContext.setAttribute("ScriptingWeaver", weaver);
        return weaver;
    }

    /**
     * initializes our custom chain loader which gets plugged into
     * the myfaces loading part for classes!
     *
     * @param servletContext
     * @return
     */
    private final CustomChainLoader initChainLoader(ServletContext servletContext) {
        CustomChainLoader loader = new CustomChainLoader(servletContext);
        ClassUtils.addClassLoadingExtension(loader, true);
        return loader;
    }

    /**
     * initializes the central config storage!
     *
     * @param servletContext
     */
    private final void initConfig(ServletContext servletContext) {
        Configuration conf = new Configuration();
        servletContext.setAttribute(ScriptingConst.CTX_CONFIGURATION, conf);
        WeavingContext.setConfiguration(conf);
    }

    public void postInit(ServletContextEvent evt) {

    }

    public void preDestroy(ServletContextEvent evt) {
    }

    public void postDestroy(ServletContextEvent evt) {
        //context is destroyed we have to shut down our daemon as well, by giving it
        //a hint to shutdown
        RefreshContext rContext = (RefreshContext) evt.getServletContext().getAttribute("RefreshContext");
        rContext.getDaemon().setRunning(false);
    }

}
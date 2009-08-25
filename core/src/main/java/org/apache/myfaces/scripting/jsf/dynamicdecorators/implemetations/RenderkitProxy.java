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
package org.apache.myfaces.scripting.jsf.dynamicdecorators.implemetations;

import org.apache.myfaces.groovyloader.core.DynamicClassIdentifier;
import org.apache.myfaces.scripting.api.Decorated;
import org.apache.myfaces.scripting.api.ScriptingConst;
import org.apache.myfaces.scripting.core.util.ProxyUtils;

import javax.faces.context.FacesContext;
import javax.faces.render.RenderKit;
import javax.faces.render.Renderer;
import javax.faces.render.ResponseStateManager;
import javax.faces.context.ResponseWriter;
import javax.faces.context.ResponseStream;
import java.io.Writer;
import java.io.OutputStream;
import javax.servlet.ServletRequest;

/**
 * Weaving renderkit which
 * acts as a proxy factory for
 * our internal reloading renerers
 *
 * @author Werner Punz
 */
public class RenderkitProxy extends RenderKit implements Decorated {



    private void weaveDelegate() {
        _delegate = (RenderKit) ProxyUtils.getWeaver().reloadScriptingInstance(_delegate);
    }

    private boolean alreadyWovenInRequest(String clazz) {
        //todo also enable portlets here
       ServletRequest req = (ServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        if(req.getAttribute(ScriptingConst.SCRIPTING_REQUSINGLETON+clazz) == null) {
            req.setAttribute(ScriptingConst.SCRIPTING_REQUSINGLETON+clazz,"");
            return false;
        }
        return true;
    }

    RenderKit _delegate = null;


    public RenderkitProxy(RenderKit delegate) {
        _delegate = delegate;
    }

    public void addRenderer(String s, String s1, Renderer renderer) {
        weaveDelegate();
        if(ProxyUtils.isDynamic(renderer.getClass()) && !alreadyWovenInRequest(renderer.toString())) {
            renderer = (Renderer) ProxyUtils.getWeaver().reloadScriptingInstance(renderer);
            alreadyWovenInRequest(renderer.toString());
        }

        _delegate.addRenderer(s, s1, renderer);
    }

    public Renderer getRenderer(String s, String s1) {
        weaveDelegate();
        Renderer retVal = _delegate.getRenderer(s, s1);

        if (retVal != null && ProxyUtils.isDynamic(retVal.getClass())&& !alreadyWovenInRequest(retVal.toString())) {
            retVal = (Renderer) ProxyUtils.getWeaver().reloadScriptingInstance(retVal);
            alreadyWovenInRequest(retVal.toString());
            _delegate.addRenderer(s,s1,retVal);
        }
        return retVal;
    }

    public ResponseStateManager getResponseStateManager() {
        weaveDelegate();
        return _delegate.getResponseStateManager();
    }

    public ResponseWriter createResponseWriter(Writer writer, String s, String s1) {
        weaveDelegate();
        return _delegate.createResponseWriter(writer, s, s1);
    }

    public ResponseStream createResponseStream(OutputStream outputStream) {
        weaveDelegate();
        return _delegate.createResponseStream(outputStream);
    }

    public Object getDelegate() {
        return _delegate;  //To change body of implemented methods use File | Settings | File Templates.
    }
}

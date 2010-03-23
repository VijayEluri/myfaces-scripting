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

import org.apache.myfaces.config.RuntimeConfig;
import org.apache.myfaces.scripting.api.Decorated;
import org.apache.myfaces.scripting.api.ScriptingConst;
import org.apache.myfaces.scripting.core.util.WeavingContext;

import javax.el.*;
import javax.faces.context.FacesContext;
import java.beans.FeatureDescriptor;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * EL Resolver which is scripting enabled
 *
 * @author Werner Punz
 */
public class ELResolverProxy extends ELResolver implements Decorated {

    Logger log = Logger.getLogger(ELResolverProxy.class.getName());
    ELResolver _delegate = null;

    static ThreadLocal<Boolean> _getValue = new ThreadLocal<Boolean>();

    public Object getValue(ELContext elContext, final Object base, final Object property) throws NullPointerException, PropertyNotFoundException, ELException {

        Object retVal = _delegate.getValue(elContext, base, property);

        Object newRetVal = null;
        //TODO simplify this, we do not need it anymore since we do our base scan at the beginning

        if (retVal != null && WeavingContext.isDynamic(retVal.getClass())) {
            //special case late el expression resolution, not catachable on class level

            if (retVal.getClass().getName().equals("BlogEntry")) {
                System.out.println(Thread.currentThread().getContextClassLoader().getClass().toString());
            }
            newRetVal = WeavingContext.getWeaver().reloadScriptingInstance(retVal, ScriptingConst.ARTIFACT_TYPE_MANAGEDBEAN);

            if (newRetVal != retVal) {
                setValue(elContext, base, property, newRetVal);
            }

            //in case we have an annotation change we have to deal with it differently
            //newRetVal = reloadAnnotatedBean(elContext, base, property, newRetVal);

            return newRetVal;

        } else if (retVal == null) {
            retVal = reloadAnnotatedBean(elContext, base, property, null);
        }

        return retVal;
    }

    private Object reloadAnnotatedBean(ELContext elContext, Object base, Object property, Object newRetVal) {
        //Avoid recursive calls into ourselfs here

        try {
            if (_getValue.get() != null && _getValue.get().equals(Boolean.TRUE)) {
                return newRetVal;
            }
            _getValue.set(Boolean.TRUE);
            //base == null means bean el
            if (base == null) {
                final FacesContext facesContext = FacesContext.getCurrentInstance();
                RuntimeConfig config = RuntimeConfig.getCurrentInstance(facesContext.getExternalContext());
                Map<String, org.apache.myfaces.config.element.ManagedBean> mbeans = config.getManagedBeans();
                if (!mbeans.containsKey(property.toString())) {
                    if (log.isLoggable(Level.FINE)) {
                        log.log(Level.FINE, "[EXT-SCRIPTING] ElResolverProxy.getValue old bean not existing we have to perform a full annotation scan");
                    }
                    setValue(elContext, base, property, null);

                    //we only trigger this if the bean was deregistered, we now can reregister it again
                    WeavingContext.getWeaver().fullClassScan();
                    newRetVal = _delegate.getValue(elContext, base, property);
                }
            }
        } finally {
            _getValue.set(Boolean.FALSE);
        }
        return newRetVal;
    }

    public Class<?> getType(ELContext elContext, Object o, Object o1) throws NullPointerException, PropertyNotFoundException, ELException {
        Class<?> retVal = _delegate.getType(elContext, o, o1);
        if (retVal != null && WeavingContext.isDynamic((Class) retVal)) {
            return WeavingContext.getWeaver().reloadScriptingClass((Class) retVal);
        }
        return retVal;
    }

    public void setValue(ELContext elContext, Object base, Object property, Object value) throws NullPointerException, PropertyNotFoundException, PropertyNotWritableException, ELException {
        //now to more complex relations...
        if (base != null) {
            WeavingContext.getRefreshContext().getDependencyRegistry().addDependency(ScriptingConst.ENGINE_TYPE_JSF_ALL, base.getClass().getName(), base.getClass().getName(), value.getClass().getName());
        }
        _delegate.setValue(elContext, base, property, value);
    }

    public boolean isReadOnly(ELContext elContext, Object o, Object o1) throws NullPointerException, PropertyNotFoundException, ELException {
        return _delegate.isReadOnly(elContext, o, o1);
    }

    public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext elContext, Object o) {
        return _delegate.getFeatureDescriptors(elContext, o);
    }

    public Class<?> getCommonPropertyType(ELContext elContext, Object o) {
        return _delegate.getCommonPropertyType(elContext, o);
    }

    public ELResolverProxy(ELResolver delegate) {
        _delegate = delegate;
    }

    public Object getDelegate() {
        return _delegate;  //To change body of implemented methods use File | Settings | File Templates.
    }

}
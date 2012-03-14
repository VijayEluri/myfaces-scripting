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
package org.apache.myfaces.extensions.scripting.api;

import org.apache.myfaces.config.RuntimeConfig;
import org.apache.myfaces.config.annotation.LifecycleProvider;
import org.apache.myfaces.config.annotation.LifecycleProviderFactory;
import org.apache.myfaces.extensions.scripting.core.util.ReflectUtil;
import org.apache.myfaces.extensions.scripting.core.util.WeavingContext;
import org.apache.myfaces.extensions.scripting.monitor.ClassResource;
import org.apache.myfaces.extensions.scripting.monitor.RefreshContext;
import org.apache.myfaces.extensions.scripting.monitor.RefreshAttribute;
import org.apache.myfaces.util.ContainerUtils;

import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.faces.context.FacesContext;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bean handler implementation
 * which encapsulates the myfaces specific parts
 * of the bean processing
 */
public class MyFacesBeanHandler implements BeanHandler {

    final Logger _logger = Logger.getLogger(MyFacesBeanHandler.class.getName());

    /**
     * scripting engine for this bean handler
     */
    int _scriptingEngine;

    /**
     * constructor
     *
     * @param scriptingEngine the scripting engine the bean handler
     *                        currently has to attach to for its
     *                        operations
     */
    public MyFacesBeanHandler(int scriptingEngine) {
        this._scriptingEngine = scriptingEngine;
    }

    /**
     * scans all bean dependencies according to
     * their IoC information stored by the runtime
     * (in our case the MyFaces runtime confing)
     * and adds those into our backward referencing dependency map
     * to add further dependency information on IoC level
     * (we can have IoC dependencies which are bound by object
     * types, this is a corner case but it still can happen)
     */
    public void scanDependencies() {

    }

    /**
     * scans the dependencies on el level
     * Not working out for now
     */
  /*  public void scanElDependencies() {
        Map<String, ManagedBean> mbeans = RuntimeConfig.getCurrentInstance(FacesContext.getCurrentInstance().getExternalContext()).getManagedBeans();
        for(Map.Entry<String, ManagedBean> entry: mbeans.entrySet()) {
            Object bean = entry.getValue();
            if(bean instanceof org.apache.myfaces.config.impl.digester.elements.ManagedBean) {
                org.apache.myfaces.config.impl.digester.elements.ManagedBean workBean = (org.apache.myfaces.config.impl.digester.elements.ManagedBean) bean;

                Object props = ReflectUtil.executeMethod(workBean,"getManagedProperties");
                if(props instanceof Iterator) {
                    //myfaces 1.2
                } else {
                    for(ManagedProperty prop: ((Collection<ManagedProperty>) props)) {
                          ExpressionFactory expFactory = FacesContext.getCurrentInstance().getApplication().getExpressionFactory();
                          ELContext elContext = FacesContext.getCurrentInstance().getELContext();
                           expFactory.coerceToType("#{myFactory['booga']}");   
                          //if(ContainerUtils.isValueReference((String) prop.getV))
                          elContext.getELResolver().getType(elContext,"myFactory","booga");
                    }
                }


            }
            //Iterator<ManagedProperty> it = bean.getManagedProperties();
            //we rescan all managed props to cover pure object
            //references as well as class references
            //while(it.hasNext()) {
            //    ManagedProperty prop = it.next();
            //}
        }
    } */

    /**
     * Refreshes all managed beans
     * session, and personal scoped ones
     * <p/>
     * personal scoped beans are beans which
     * have either
     * <li> session scope </li>
     * <li> page scope </li>
     * <li> custom scope </li>
     */
    public void refreshAllManagedBeans() {
        if (FacesContext.getCurrentInstance() == null) {
            return;//no npe allowed
        }

        Set<String> tainted = getTaintedClasses();

        //scanElDependencies();

        if (tainted.size() > 0) {
            //We now have to check if the tainted classes belong to the managed beans
            Set<String> managedBeanClasses = new HashSet<String>();

            Map mbeans = RuntimeConfig.getCurrentInstance(FacesContext.getCurrentInstance().getExternalContext()).getManagedBeans();
            Map mbeansSnapshotView;

            synchronized (RefreshContext.BEAN_SYNC_MONITOR) {
                mbeansSnapshotView = makeSnapshot(mbeans);
            }

            for (Object entry :  mbeansSnapshotView.entrySet()) {
                Object bean = (Object)  ((Map.Entry)entry).getValue();

                managedBeanClasses.add((String)ReflectUtil.executeMethod(bean, "getManagedBeanClassName"));//bean.getManagedBeanClassName());
            }

            boolean managedBeanTainted = isAnyManagedBeanTainted(tainted, managedBeanClasses);
            markPersonalScopeRefreshRecommended();
            getLog().info("[EXT-SCRIPTING] Tainting all beans to avoid classcast exceptions");
            if (managedBeanTainted) {
                globalManagedBeanRefresh(mbeansSnapshotView);
                //personalScopeRefresh();
            }
        }
    }

    /**
     * Exposed personal scope refresh
     */
    public void personalScopeRefresh() {
        //shortcut to avoid heavier operations in the beginning
        long globalBeanRefreshTimeout = WeavingContext.getRefreshContext().getPersonalScopedBeanRefresh();
        if (globalBeanRefreshTimeout == -1L) return;

        Map sessionMap = FacesContext.getCurrentInstance().getExternalContext().getSessionMap();
        Long timeOut = (Long) sessionMap.get(ScriptingConst.SESS_BEAN_REFRESH_TIMER);
        if (timeOut == null || timeOut <= globalBeanRefreshTimeout) {
            refreshPersonalScopedBeans();
        }
    }

    /**
     * removes all bean references which have been tainted
     * (note for now we remove all dynamic references until we
     * get a more sophisticated handling of managed beans)
     *
     * @param workCopy the managed beam snapshot view
     */
    private void globalManagedBeanRefresh(Map workCopy) {
        Set<String> tainted = getTaintedClasses();

        for (Object entry : workCopy.entrySet()) {
            Object bean = ((Map.Entry) entry).getValue();
            Class managedBeanClass =  (Class) ReflectUtil.executeMethod(bean, "getManagedBeanClass");
            if (hasToBeRefreshed(tainted, managedBeanClass)) {
                //managed bean class found we drop the class from our session
                removeBeanReferences(bean);
            }
            //one bean tainted we have to taint all dynamic beans otherwise we will get classcast
            //exceptions
            /*getLog().info("[EXT-SCRIPTING] Tainting ");
            RefreshAttribute metaData = WeavingContext.getFileChangedDaemon().getClassMap().get(managedBeanClass.getName());
            if (metaData != null) {
                metaData.requestRefresh();
            }*/
        }
    }

    /**
     * determines whether any bean in our managed bean list
     * is tainted or not
     *
     * @param tainted            a list of classes which are tainted in this iteration
     * @param managedBeanClasses a ist of classes which are our managed beans
     * @return true if one of the beans is tainted
     */
    private boolean isAnyManagedBeanTainted(Set<String> tainted, Set<String> managedBeanClasses) {
        boolean managedBeanTainted = false;
        for (String taintedClass : tainted) {
            if (managedBeanClasses.contains(taintedClass)) {
                managedBeanTainted = true;
                break;
            }
        }
        return managedBeanTainted;
    }

    /**
     * helper which returns all tainted classes
     *
     * @return the tainted classes
     */
    private Set<String> getTaintedClasses() {
        Set<String> tainted = new HashSet<String>();

        for (Map.Entry<String, ClassResource> it : WeavingContext.getFileChangedDaemon().getClassMap().entrySet()) {
            if (it.getValue().getScriptingEngine() == getScriptingEngine() && it.getValue().getRefreshAttribute().requiresRefresh()) {
                tainted.add(it.getKey());
            }
        }
        return tainted;
    }

    /**
     * refreshes all personal scoped beans (aka beans which
     * have an assumed lifecycle <= session)
     * <p/>
     * This is needed for multiuser purposes because if one user alters some beans
     * other users have to drop their non application scoped beans as well!
     */
    private void refreshPersonalScopedBeans() {
        //the refreshing is only allowed if no compile is in progress
        //and vice versa
        Map sessionMap = FacesContext.getCurrentInstance().getExternalContext().getSessionMap();
        Long taintingPeriod = (Long) sessionMap.get(ScriptingConst.SESS_BEAN_REFRESH_TIMER);
        if (taintingPeriod == null) {
            taintingPeriod = -1L;
        }
        Set<String> taintedInTime = WeavingContext.getRefreshContext().getTaintHistoryClasses(taintingPeriod);

        synchronized (RefreshContext.BEAN_SYNC_MONITOR) {
            Map mbeans = RuntimeConfig.getCurrentInstance(FacesContext.getCurrentInstance().getExternalContext()).getManagedBeans();
            //the map is immutable but in between scanning might change it so we make a full copy of the map

            //We can synchronized the refresh, but if someone alters
            //the bean map from outside we still get race conditions
            //But for most cases this mutex should be enough
            Map mbeansSnapshotView = makeSnapshot(mbeans);

            for (Object entry : mbeansSnapshotView.entrySet()) {
                Object value = ((Map.Entry) entry).getValue();
                Class managedBeanClass = (Class) ReflectUtil.executeMethod(value,"getManagedBeanClass");
                if (hasToBeRefreshed(taintedInTime, managedBeanClass)) {
                    FacesContext.getCurrentInstance().getExternalContext().getSessionMap().remove(ReflectUtil.executeMethod(value,"getManagedBeanName"));
                    removeCustomScopedBean( value );
                }
            }
            updateBeanRefreshTime();
        }
    }

    /**
     * removes the references from out static scope
     * for jsf2 we probably have some kind of notification mechanism
     * which notifies custom scopes
     *
     * @param bean the managed bean which all references have to be removed from
     */

    private void removeBeanReferences(Object bean) {
        String managedBeanName = (String) ReflectUtil.executeMethod(bean, "getManagedBeanName");

        if (getLog().isLoggable(Level.FINE)) {
            getLog().log(Level.FINE, "[EXT-SCRIPTING] JavaScriptingWeaver.removeBeanReferences({0})", managedBeanName);
        }

        FacesContext.getCurrentInstance().getExternalContext().getSessionMap().remove(managedBeanName);
        FacesContext.getCurrentInstance().getExternalContext().getApplicationMap().remove(managedBeanName);
        removeCustomScopedBean(bean);
    }

    /**
     * @return the log for this class
     */
    protected Logger getLog() {
        return Logger.getLogger(this.getClass().getName());
    }



    /**
     * jsf2 helper to remove custom scoped beans
     *
     * @param bean the managed bean which has to be removed from the custom scope from
     */
    private void removeCustomScopedBean(Object bean) {
        Object scopeImpl = FacesContext.getCurrentInstance().getExternalContext().getApplicationMap().get(ReflectUtil.executeMethod(bean,"getManagedBeanScope"));
        if (scopeImpl == null) return; //scope not implemented
        //we now have to revert to introspection here because scopes are a pure jsf2 construct
        //so we use a messaging pattern here to cope with it

        Object beanInstance = ReflectUtil.executeMethod(scopeImpl, "get", ReflectUtil.executeMethod(bean, "getManagedBeanName"));
        LifecycleProvider lifecycleProvider =
                LifecycleProviderFactory.getLifecycleProviderFactory().getLifecycleProvider(FacesContext.getCurrentInstance().getExternalContext());
        try {
            lifecycleProvider.destroyInstance(beanInstance);
        } catch (IllegalAccessException e) {
            _logger.log(Level.WARNING, "removeCustomScopedBean():", e);
        } catch (InvocationTargetException e) {
            _logger.log(Level.WARNING, "removeCustomScopedBean():", e);
        }
    }

    /**
     * MyFaces 2.0 keeps an immutable map over the session
     * and request scoped beans
     * if we alter that during our loop we get a concurrent modification exception
     * taking a snapshot in time fixes that
     *
     * @param mbeans the internal managed bean map which has to be investigated
     * @return a map with the class name as key and the managed bean info
     *         as value of the current state of the internal runtime config bean map
     */
    private Map makeSnapshot(Map mbeans) {
        Map workCopy;

        workCopy = new HashMap(mbeans.size());
        for (Object elem: mbeans.entrySet()) {
            Map.Entry entry = (Map.Entry) elem;
            workCopy.put(entry.getKey(), entry.getValue());
        }

        return workCopy;
    }

    /**
     * updates the internal timer
     * for our personal scoped beans so that
     * we dont get updates on beans we have refreshed already
     */
    private void updateBeanRefreshTime() {
        long sessionRefreshTime = System.currentTimeMillis();
        FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put(ScriptingConst.SESS_BEAN_REFRESH_TIMER, sessionRefreshTime);
    }

    /**
     * sets the internal timer for other processes
     * to update their beans as well
     */
    private void markPersonalScopeRefreshRecommended() {
        long sessionRefreshTime = System.currentTimeMillis();
        WeavingContext.getRefreshContext().setPersonalScopedBeanRefresh(sessionRefreshTime);
        FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put(ScriptingConst.SESS_BEAN_REFRESH_TIMER, sessionRefreshTime);
    }

    /**
     * important, this method determines whether a managed bean class
     * has to be refreshed or not
     *
     * @param tainted          set of tainted classes
     * @param managedBeanClass the class to be checked for refresh criteria
     * @return true if the current bean class fulfills our refresh criteria
     */
    protected boolean hasToBeRefreshed(Set<String> tainted, Class managedBeanClass) {

        return WeavingContext.isDynamic(managedBeanClass) && tainted.contains(managedBeanClass.getName());
    }

    /**
     * returns the scripting engine which is attached
     * to this bean handler
     *
     * @return the current scripting engine
     */
    private int getScriptingEngine() {
        return _scriptingEngine;
    }
}

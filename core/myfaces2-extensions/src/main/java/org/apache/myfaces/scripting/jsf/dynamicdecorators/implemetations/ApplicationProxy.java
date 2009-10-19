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

import org.apache.myfaces.scripting.api.Decorated;
import org.apache.myfaces.scripting.api.ScriptingConst;
import org.apache.myfaces.scripting.core.util.ProxyUtils;
import org.apache.myfaces.scripting.jsf2.annotation.purged.PurgedResourceHandler;
import org.apache.myfaces.scripting.jsf2.annotation.purged.PurgedComponent;

import javax.el.*;
import javax.faces.FacesException;
import javax.faces.application.*;
import javax.faces.component.UIComponent;
import javax.faces.component.behavior.Behavior;
import javax.faces.component.behavior.BehaviorBase;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.el.*;
import javax.faces.event.ActionListener;
import javax.faces.event.SystemEvent;
import javax.faces.event.SystemEventListener;
import javax.faces.validator.Validator;
import java.lang.reflect.Proxy;
import java.util.*;

/**
 * our decorating applicstion
 * which should resolve our bean issues within a central
 * bean processing interceptor
 * <p/>
 *
 * TODO at component reload via annotations the component family is lost
 * locate where it is and then add the family handling here
 * so that it is set again!
 * (Line 490 it is lost)
 *
 * @author Werner Punz
 */
public class ApplicationProxy extends Application implements Decorated {

    Application _delegate = null;

    public ApplicationProxy(Application delegate) {
        _delegate = delegate;
    }


    public void addELResolver(ELResolver elResolver) {
        weaveDelegate();
        //TODO this can be problematic if several libraries add their own proxies
        // that way then might get get a cyclic stack
        //under normal circumstances this should not happen
        //because addElResolver is called once and getElResolver
        //does not change the stack afterwards in the worst case
        //we might get 2 of our proxies in the delegate stack

        //the same goes for the rest of the factory stuff
        if (!(elResolver instanceof ELResolverProxy))
            elResolver = new ELResolverProxy(elResolver);
        _delegate.addELResolver(elResolver);
    }

    private void weaveDelegate() {
        if (_delegate != null) {
            _delegate = (Application) ProxyUtils.getWeaver().reloadScriptingInstance(_delegate);
        }
    }

    public ELResolver getELResolver() {
        weaveDelegate();
        ELResolver retVal = _delegate.getELResolver();
        if (!(retVal instanceof ELResolverProxy)) {
            retVal = new ELResolverProxy(retVal);
        }
        return retVal;

    }

    //TOD add a weaving for resource bundles
    public ResourceBundle getResourceBundle(FacesContext facesContext, String s) throws FacesException, NullPointerException {
        weaveDelegate();
        return _delegate.getResourceBundle(facesContext, s);
    }

    public UIComponent createComponent(ValueExpression valueExpression, FacesContext facesContext, String s) throws FacesException, NullPointerException {
        weaveDelegate();
        UIComponent component = _delegate.createComponent(valueExpression, facesContext, s);
        UIComponent oldComponent = component;
        //We can replace annotated components on the fly via
        //ApplicationImpl.addComponent(final String componentType, final String componentClassName)

        /*we are reweaving on the fly because we cannot be sure if
        * the class is not recycled all the time in the creation
        * code, in the renderer we do it on method base
        * due to the fact that our renderers are recycled via
        * a flyweight pattern
        *
        *
        * Also we cannot proxy here because there is no UIComponent interface
        * maybe in the long run we can make a decorator here instead
        * but for now lets try it this way
        */
        component = (UIComponent) reloadInstance(component);

        //we now have to check for an annotation change, but only in case a reload has happened
        if (component.getClass().hashCode() != oldComponent.getClass().hashCode()) {
            return handeAnnotationChange(component, valueExpression, facesContext, s);
        }

        return component;

    }


    public ExpressionFactory getExpressionFactory() {
        weaveDelegate();
        return _delegate.getExpressionFactory();
    }

    public void addELContextListener(ELContextListener elContextListener) {
        weaveDelegate();
        if (ProxyUtils.isDynamic(elContextListener.getClass()))
            elContextListener = (ELContextListener) ProxyUtils.createMethodReloadingProxyFromObject(elContextListener, ELContextListener.class);
        _delegate.addELContextListener(elContextListener);
    }

    public void removeELContextListener(ELContextListener elContextListener) {
        //TODO which el context listener is coming in our normal one
        //or the reloaded one?
        weaveDelegate();
        _delegate.removeELContextListener(elContextListener);
    }

    public ELContextListener[] getELContextListeners() {
        weaveDelegate();
        return _delegate.getELContextListeners();
    }


    public ActionListener getActionListener() {
        weaveDelegate();
        ActionListener retVal = _delegate.getActionListener();
        if (ProxyUtils.isDynamic(retVal.getClass()))
            retVal = (ActionListener) ProxyUtils.createMethodReloadingProxyFromObject(retVal, ActionListener.class);
        return retVal;
    }

    public void setActionListener(ActionListener actionListener) {
        weaveDelegate();
        if (ProxyUtils.isDynamic(actionListener.getClass()))
            actionListener = (ActionListener) ProxyUtils.createMethodReloadingProxyFromObject(actionListener, ActionListener.class);
        _delegate.setActionListener(actionListener);
    }

    public Locale getDefaultLocale() {
        weaveDelegate();
        return _delegate.getDefaultLocale();
    }

    public void setDefaultLocale(Locale locale) {
        weaveDelegate();
        _delegate.setDefaultLocale(locale);
    }

    public String getDefaultRenderKitId() {
        weaveDelegate();
        return _delegate.getDefaultRenderKitId();
    }

    public void setDefaultRenderKitId(String s) {
        weaveDelegate();
        _delegate.setDefaultRenderKitId(s);
    }

    public String getMessageBundle() {
        weaveDelegate();
        return _delegate.getMessageBundle();
    }

    public void setMessageBundle(String s) {
        weaveDelegate();
        _delegate.setMessageBundle(s);
    }

    public NavigationHandler getNavigationHandler() {
        weaveDelegate();
        //defined in the setter to speed things up a little
        NavigationHandler retVal = _delegate.getNavigationHandler();
        if (retVal != null && ProxyUtils.isDynamic(retVal.getClass()))
            retVal = new NavigationHandlerProxy(retVal);
        return retVal;
    }

    public void setNavigationHandler(NavigationHandler navigationHandler) {
        weaveDelegate();
        if (navigationHandler != null && ProxyUtils.isDynamic(navigationHandler.getClass()))
            navigationHandler = new NavigationHandlerProxy(navigationHandler);
        _delegate.setNavigationHandler(navigationHandler);
    }

    public PropertyResolver getPropertyResolver() {
        weaveDelegate();
        return _delegate.getPropertyResolver();
    }

    public void setPropertyResolver(PropertyResolver propertyResolver) {
        weaveDelegate();
        _delegate.setPropertyResolver(propertyResolver);
    }

    public VariableResolver getVariableResolver() {
        weaveDelegate();
        VariableResolver variableResolver = _delegate.getVariableResolver();
        if (!(variableResolver instanceof VariableResolverProxy))
            variableResolver = new VariableResolverProxy(variableResolver);
        return variableResolver;
    }

    public void setVariableResolver(VariableResolver variableResolver) {
        weaveDelegate();
        if (!(variableResolver instanceof VariableResolverProxy))
            variableResolver = new VariableResolverProxy(variableResolver);

        _delegate.setVariableResolver(variableResolver);
    }

    public ViewHandler getViewHandler() {
        weaveDelegate();
        ViewHandler handler = _delegate.getViewHandler();

        /*
        We proxy here to emable dynamic reloading for
        methods in the long run, as soon as we hit
        java all our groovy reloading code is lost
        hence we have to work with proxies here
        */
        if (ProxyUtils.isDynamic(handler.getClass()))
            handler = (ViewHandlerProxy) new ViewHandlerProxy(handler);
        return handler;
    }

    public void setViewHandler(ViewHandler viewHandler) {
        weaveDelegate();
        /*make sure you have the delegates as well in properties*/
        if (ProxyUtils.isDynamic(viewHandler.getClass()))
            viewHandler = (ViewHandlerProxy) new ViewHandlerProxy(viewHandler);

        _delegate.setViewHandler(viewHandler);
    }

    public StateManager getStateManager() {
        weaveDelegate();
        return _delegate.getStateManager();
    }

    public void setStateManager(StateManager stateManager) {
        weaveDelegate();
        _delegate.setStateManager(stateManager);
    }

    public void addComponent(String componentType, String componentClass) {
        weaveDelegate();
        _delegate.addComponent(componentType, componentClass);
    }

    public UIComponent createComponent(String componentType) throws FacesException {
        weaveDelegate();
        //the components are generated anew very often
        //we cannot do an on object weaving here
        UIComponent oldComponent = _delegate.createComponent(componentType);

        /*we are reweaving on the fly because we cannot be sure if
        * the class is not recycled all the time in the creation
        * code, in the renderer we do it on method base
        * due to the fact that our renderers are recycled via
        * a flyweight pattern*/
        UIComponent component = (UIComponent) reloadInstance(oldComponent);

        //we now have to check for an annotation change, but only in case a reload has happened
        if (component.getClass().hashCode() != oldComponent.getClass().hashCode()) {
            return handeAnnotationChange(component, componentType);
        }

        return component;

    }

    public UIComponent createComponent(ValueBinding valueBinding, FacesContext facesContext, String componentType) throws FacesException {
        weaveDelegate();
        UIComponent oldComponent = _delegate.createComponent(valueBinding, facesContext, componentType);

        /*we are reweaving on the fly because we cannot be sure if
         * the class is not recycled all the time in the creation
         * code, in the renderer we do it on method base
         * due to the fact that our renderers are recycled via
         * a flyweight pattern*/
        UIComponent component = (UIComponent) reloadInstance(oldComponent);

        //we now have to check for an annotation change, but only in case a reload has happened
        if (component.getClass().hashCode() != oldComponent.getClass().hashCode()) {
            return handeAnnotationChange(component, valueBinding, facesContext, componentType);
        }

        return component;
    }

    public Iterator<String> getComponentTypes() {
        weaveDelegate();
        return _delegate.getComponentTypes();
    }

    public void addConverter(String converterId, String converterClass) {
        weaveDelegate();
        _delegate.addConverter(converterId, converterClass);
    }

    public void addConverter(Class targetClass, String converterClass) {
        weaveDelegate();
        _delegate.addConverter(targetClass, converterClass);
    }

    public Converter createConverter(String converterId) {
        weaveDelegate();
        Converter retVal = _delegate.createConverter(converterId);
        /**
         * since createConverter is called only once
         * we have to work with method reloading proxies
         * we cannot use this technique extensively for speed reasons
         * most of the time it is fine just to work with
         *
         * reloading objects at their interception points
         */
        if (ProxyUtils.isDynamic(retVal.getClass())) {
            retVal = (Converter) ProxyUtils.createMethodReloadingProxyFromObject(retVal, Converter.class);

        }

        return retVal;
    }

    public Converter createConverter(Class aClass) {
        weaveDelegate();
        Converter retVal = _delegate.createConverter(aClass);
        if (retVal != null && ProxyUtils.isDynamic(retVal.getClass())) {
            retVal = (Converter) ProxyUtils.createMethodReloadingProxyFromObject(retVal, Converter.class);
        }

        return retVal;
    }

    public Iterator<String> getConverterIds() {
        weaveDelegate();
        return _delegate.getConverterIds();
    }


    public Iterator<Class<?>> getConverterTypes() {
        weaveDelegate();
        return _delegate.getConverterTypes();
    }

    public MethodBinding createMethodBinding(String s, Class[] classes) throws ReferenceSyntaxException {
        weaveDelegate();
        return _delegate.createMethodBinding(s, classes);
    }

    public Iterator<Locale> getSupportedLocales() {
        weaveDelegate();
        return _delegate.getSupportedLocales();
    }

    public void setSupportedLocales(Collection<Locale> locales) {
        weaveDelegate();
        _delegate.setSupportedLocales(locales);
    }

    public void addValidator(String s, String s1) {
        weaveDelegate();
        _delegate.addValidator(s, s1);
    }

    public Validator createValidator(String s) throws FacesException {
        weaveDelegate();

        Validator retVal = _delegate.createValidator(s);
        if (ProxyUtils.isDynamic(retVal.getClass()) && !Proxy.isProxyClass(retVal.getClass())) {
            retVal = (Validator) ProxyUtils.createMethodReloadingProxyFromObject(retVal, Validator.class);
        }
        return retVal;
    }

    public Iterator<String> getValidatorIds() {
        weaveDelegate();
        return _delegate.getValidatorIds();
    }

    public ValueBinding createValueBinding(String s) throws ReferenceSyntaxException {
        weaveDelegate();
        return _delegate.createValueBinding(s);
    }


    @Override
    public void addBehavior(String behaviorId, String behaviorClass) {
        weaveDelegate();
        _delegate.addBehavior(behaviorId, behaviorClass);
    }

    @Override
    public void addDefaultValidatorId(String behaviorId) {
        weaveDelegate();
        _delegate.addDefaultValidatorId(behaviorId);
    }

    @Override
    public Behavior createBehavior(String behaviorId) throws FacesException {
        weaveDelegate();
        Behavior retVal = _delegate.createBehavior(behaviorId);
        //in case of a descendend of BehaviorBase we probably
        //can count on additional functionality within the codebase we avoid
        //therefore the interface reloading and work directly on the object
        //still direct casts are forbidden, but parent casts are ok, which should
        //be enough for behavior replacements on the user side, which this mechanism should
        //cover for now
        boolean isDynamic = ProxyUtils.isDynamic(retVal.getClass());
        //TODO Check how often the behavior is really created if only once
        //we have to operate over reloading proxies nevertheless
        //the add and remove behavior listeners being protected might
        //be a showstopper because we cannot provide the same in our proxies
        if (!isDynamic) {
            return retVal;
        } else if (retVal instanceof BehaviorBase) {
            //we might have casts here against one of the parents
            //of this object
            return (Behavior) ProxyUtils.getWeaver().reloadScriptingInstance(retVal);
        } else {
            return (Behavior) ProxyUtils.createMethodReloadingProxyFromObject(retVal, Behavior.class);
        }
    }


    //TODO implement those
    @Override
    public UIComponent createComponent(FacesContext facesContext, Resource resource) {
        weaveDelegate();

        UIComponent oldComponent = _delegate.createComponent(facesContext, resource);

        /*we are reweaving on the fly because we cannot be sure if
         * the class is not recycled all the time in the creation
         * code, in the renderer we do it on method base
         * due to the fact that our renderers are recycled via
         * a flyweight pattern*/
        UIComponent component = (UIComponent) reloadInstance(oldComponent);

        //we now have to check for an annotation change, but only in case a reload has happened
        if (component.getClass().hashCode() != oldComponent.getClass().hashCode()) {
            return handeAnnotationChange(component, facesContext, resource);
        }

        return component;

    }

    @Override
    public UIComponent createComponent(FacesContext facesContext, String componentType, String rendererType) {
        weaveDelegate();
        UIComponent oldComponent = _delegate.createComponent(facesContext, componentType, rendererType);

        /*we are reweaving on the fly because we cannot be sure if
         * the class is not recycled all the time in the creation
         * code, in the renderer we do it on method base
         * due to the fact that our renderers are recycled via
         * a flyweight pattern*/
        UIComponent component = (UIComponent) reloadInstance(oldComponent);

        //we now have to check for an annotation change, but only in case a reload has happened
        if (component.getClass().hashCode() != oldComponent.getClass().hashCode()) {
            return handeAnnotationChange(component, facesContext, componentType, rendererType);
        }

        return component;
    }

    @Override
    public UIComponent createComponent(ValueExpression valueExpression, FacesContext facesContext, String s, String s1) {
        weaveDelegate();
        UIComponent oldComponent = _delegate.createComponent(valueExpression, facesContext, s, s1);

        /*we are reweaving on the fly because we cannot be sure if
     * the class is not recycled all the time in the creation
     * code, in the renderer we do it on method base
     * due to the fact that our renderers are recycled via
     * a flyweight pattern*/
        UIComponent component = (UIComponent) reloadInstance(oldComponent);

        //we now have to check for an annotation change, but only in case a reload has happened
        if (component.getClass().hashCode() != oldComponent.getClass().hashCode()) {
            return handeAnnotationChange(component, valueExpression, facesContext, s, s1);
        }

        return component;
    }

    @Override
    public <T> T evaluateExpressionGet(FacesContext facesContext, String s, Class<? extends T> aClass) throws ELException {
        weaveDelegate();
        //good place for a dynamic reloading check as well
        T retVal = _delegate.evaluateExpressionGet(facesContext, s, aClass);
        if (ProxyUtils.isDynamic(retVal.getClass()))
            retVal = (T) ProxyUtils.getWeaver().reloadScriptingInstance(retVal);
        return retVal;
    }

    @Override
    public Iterator<String> getBehaviorIds() {
        weaveDelegate();
        return _delegate.getBehaviorIds();
    }

    @Override
    public Map<String, String> getDefaultValidatorInfo() {
        weaveDelegate();
        return _delegate.getDefaultValidatorInfo();
    }

    @Override
    public ProjectStage getProjectStage() {
        weaveDelegate();
        return _delegate.getProjectStage();
    }

    @Override
    public ResourceHandler getResourceHandler() {
        weaveDelegate();
        ResourceHandler retVal = _delegate.getResourceHandler();
        if (retVal instanceof PurgedResourceHandler) {
            //TODO if resource handler purged then do a recompile
            retVal = _delegate.getResourceHandler();
            return retVal;
        }

        return (ResourceHandler) reloadInstance(retVal);
    }

    @Override
    public void publishEvent(FacesContext facesContext, Class<? extends SystemEvent> eventClass, Class<?> aClass, Object o) {
        weaveDelegate();
        _delegate.publishEvent(facesContext, eventClass, aClass, o);
    }

    @Override
    public void publishEvent(FacesContext facesContext, Class<? extends SystemEvent> eventClass, Object o) {
        weaveDelegate();
        _delegate.publishEvent(facesContext, eventClass, o);
    }

    @Override
    public void setResourceHandler(ResourceHandler resourceHandler) {
        weaveDelegate();
        _delegate.setResourceHandler(resourceHandler);
    }

    @Override
    public void subscribeToEvent(Class<? extends SystemEvent> eventClass, Class<?> aClass, SystemEventListener systemEventListener) {
        weaveDelegate();
        _delegate.subscribeToEvent(eventClass, aClass, systemEventListener);
    }

    @Override
    public void subscribeToEvent(Class<? extends SystemEvent> aClass, SystemEventListener systemEventListener) {
        weaveDelegate();
        _delegate.subscribeToEvent(aClass, systemEventListener);
    }

    @Override
    public void unsubscribeFromEvent(Class<? extends SystemEvent> eventClass, Class<?> aClass, SystemEventListener systemEventListener) {
        weaveDelegate();
        _delegate.unsubscribeFromEvent(eventClass, aClass, systemEventListener);
    }

    @Override
    public void unsubscribeFromEvent(Class<? extends SystemEvent> aClass, SystemEventListener systemEventListener) {
        weaveDelegate();
        _delegate.unsubscribeFromEvent(aClass, systemEventListener);
    }


    public Object getDelegate() {
        return _delegate;  //To change body of implemented methods use File | Settings | File Templates.
    }

    private final Object reloadInstance(Object instance) {
        if (instance == null) {
            return null;
        }
        if (ProxyUtils.isDynamic(instance.getClass()) && !alreadyWovenInRequest(instance.toString())) {
            instance = ProxyUtils.getWeaver().reloadScriptingInstance(instance);
            alreadyWovenInRequest(instance.toString());
        }
        return instance;
    }


    private final boolean alreadyWovenInRequest(String clazz) {
        //portlets now can be enabled thanks to the jsf2 indirections regarding the external context
        Map<String, Object> req = FacesContext.getCurrentInstance().getExternalContext().getRequestMap();
        if (req.get(ScriptingConst.SCRIPTING_REQUSINGLETON + clazz) == null) {
            req.put(ScriptingConst.SCRIPTING_REQUSINGLETON + clazz, "");
            return false;
        }
        return true;
    }

    private UIComponent handeAnnotationChange(UIComponent oldComponent, ValueExpression valueExpression, FacesContext facesContext, String s) {
        UIComponent componentToChange = _delegate.createComponent(valueExpression, facesContext, s);
        if (componentToChange instanceof PurgedComponent) {
            ProxyUtils.getWeaver().fullAnnotationScan();
            //via an additional create component we can check whether a purged component
            //was registered after the reload because the annotation has been removed
            componentToChange = _delegate.createComponent(valueExpression, facesContext, s);
            if (componentToChange instanceof PurgedComponent) {
                throw new FacesException("Annotation on component removed but no replacement found");
            }
            return componentToChange;
        }
        return oldComponent;
    }


    private UIComponent handeAnnotationChange(UIComponent oldComponent, String componentType) {
        UIComponent componentToChange = _delegate.createComponent(componentType);
        if (componentToChange instanceof PurgedComponent) {
            ProxyUtils.getWeaver().fullAnnotationScan();
            //via an additional create component we can check whether a purged component
            //was registered after the reload because the annotation has been removed
            componentToChange = _delegate.createComponent(componentType);
            if (componentToChange instanceof PurgedComponent) {
                throw new FacesException("Annotation on component removed but no replacement found");
            }
            return componentToChange;
        }
        return oldComponent;
    }

    private UIComponent handeAnnotationChange(UIComponent oldComponent, ValueBinding valueBinding, FacesContext context, String componentType) {
        UIComponent componentToChange = _delegate.createComponent(valueBinding, context, componentType);
        if (componentToChange instanceof PurgedComponent) {
            ProxyUtils.getWeaver().fullAnnotationScan();
            //via an additional create component we can check whether a purged component
            //was registered after the reload because the annotation has been removed
            componentToChange = _delegate.createComponent(valueBinding, context, componentType);
            if (componentToChange instanceof PurgedComponent) {
                throw new FacesException("Annotation on component removed but no replacement found");
            }
            return componentToChange;
        }
        return oldComponent;
    }

    private UIComponent handeAnnotationChange(UIComponent oldComponent, FacesContext context, Resource resource) {
        UIComponent componentToChange = _delegate.createComponent(context, resource);
        if (componentToChange instanceof PurgedComponent) {
            ProxyUtils.getWeaver().fullAnnotationScan();
            //via an additional create component we can check whether a purged component
            //was registered after the reload because the annotation has been removed
            componentToChange = _delegate.createComponent(context, resource);
            if (componentToChange instanceof PurgedComponent) {
                throw new FacesException("Annotation on component removed but no replacement found");
            }
            return componentToChange;
        }
        return oldComponent;
    }

    private UIComponent handeAnnotationChange(UIComponent oldComponent, FacesContext context, String componentType, String rendererType) {
        UIComponent componentToChange = _delegate.createComponent(context, componentType, rendererType);
        if (componentToChange instanceof PurgedComponent) {
            ProxyUtils.getWeaver().fullAnnotationScan();
            //via an additional create component we can check whether a purged component
            //was registered after the reload because the annotation has been removed
            componentToChange = _delegate.createComponent(context, componentType, rendererType);
            if (componentToChange instanceof PurgedComponent) {
                throw new FacesException("Annotation on component removed but no replacement found");
            }
            return componentToChange;
        }
        return oldComponent;
    }

    private UIComponent handeAnnotationChange(UIComponent oldComponent, ValueExpression valueExpression, FacesContext facesContext, String s, String s1) {
        UIComponent componentToChange = _delegate.createComponent(valueExpression, facesContext, s, s1);
        String family = oldComponent.getFamily();
        if (componentToChange instanceof PurgedComponent) {
            ProxyUtils.getWeaver().fullAnnotationScan();

            //via an additional create component we can check whether a purged component
            //was registered after the reload because the annotation has been removed

            componentToChange = _delegate.createComponent(valueExpression, facesContext, s, s1);
            if (componentToChange instanceof PurgedComponent) {
                throw new FacesException("Annotation on component removed but no replacement found");
            }
            return componentToChange;
        }
        return oldComponent;
    }

}

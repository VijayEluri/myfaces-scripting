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
package org.apache.myfaces.javaloader.elResolverTest;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.component.UIComponent;

/**
 * Testcase for intra bean dependency detection
 * The theory is that if you change something in the referenced bean
 * secondaryBean also this bean myBean should be reloaded
 * this mechanism only works if you have some intra code dependencies
 * if you only have object references than the bean cannot be refreshed for now
 * due to limitations within the runtime system.
 *
 * This limitation will be lifted in the long run
 * 
 */
@ManagedBean(name = "myBean")
@RequestScoped
public class MyBean {
    String test;
    UIComponent bindingMyTest;
    String hello = "This is a teststring from the managed bean myBean";

    @ManagedProperty(value = "#{secondaryBean}")
    Object secondaryBean;
    
    public String getTest() {
        return test;
    }

    public void setTest(String test) {
        this.test = test;
    }

    public UIComponent getBindingMyTest() {
        return bindingMyTest;
    }

    public void setBindingMyTest(UIComponent bindingMyTest) {
        this.bindingMyTest = bindingMyTest;
    }

    public String getHello() {
        return hello;
    }

    public void setHello(String hello) {
        this.hello = hello;
    }


    
    public Object getSecondaryBean() {
        return secondaryBean;
    }

    public void setSecondaryBean(Object secondaryBean) {
        //note this pointless cast establises a code level
        //dependency which can be detected
        this.secondaryBean = (SecondaryBean) secondaryBean;
    }
}
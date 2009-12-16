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
package org.apache.myfaces.javaloader.renderer;


import org.apache.myfaces.shared_impl.renderkit.html.HtmlTextareaRendererBase;
import org.apache.myfaces.scripting.core.util.ReflectUtil;

import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.component.UIComponent;
import javax.faces.render.FacesRenderer;
import java.io.IOException;

/**
 * @author Werner Punz (latest modification by $Author$)
 * @version $Revision$ $Date$
 */

 @FacesRenderer(componentFamily = "javax.faces.Input", rendererType = "at.irian.JavaTestRenderer")
 
public class JavaTestRenderer2 extends HtmlTextareaRendererBase {

    private static final String MSG = "Hello world from Renderer 2  ";

    @Override
    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
        super.encodeBegin(context, component);
        ResponseWriter writer = context.getResponseWriter();
        writer.write(MSG);
        writer.write((String)ReflectUtil.executeMethod(component, "getMarker"));
    }

}

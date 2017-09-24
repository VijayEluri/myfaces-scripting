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

package org.apache.myfaces.extension.scripting.weld.core;

import org.apache.myfaces.extensions.scripting.core.api.WeavingContext;
import org.apache.myfaces.extensions.scripting.core.engine.ThrowAwayClassloader;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;

/**
 * @author Werner Punz (latest modification by $Author$)
 * @version $Revision$ $Date$
 *          <p>&nbsp;</p>
 *          We use
 */

public class CDIThrowAwayClassloader extends ClassLoader
{
    ThrowAwayClassloader _delegate;

    public CDIThrowAwayClassloader(ClassLoader classLoader)
    {
        super(classLoader);
        _delegate = new ThrowAwayClassloader(classLoader);
    }

    public CDIThrowAwayClassloader()
    {
        super();
        _delegate = new ThrowAwayClassloader();
    }

    @Override
    public Class<?> loadClass(String className) throws ClassNotFoundException
    {
        return _delegate.loadClass(className);
    }

    @Override
    public InputStream getResourceAsStream(String name)
    {
        return _delegate.getResourceAsStream(name);
    }

    @Override
    public URL getResource(String s)
    {
        if (s.contains("META-INF/beans.xml"))
        {
            //return target dir
            try
            {
                return WeavingContext.getInstance().getConfiguration().getCompileTarget().toURI().toURL();
            }
            catch (MalformedURLException e)
            {
                //cannot really happen
                e.printStackTrace();
            }
            return null;
        }
        return _delegate.getResource(s);
    }

    @Override
    public Enumeration<URL> getResources(String s) throws IOException
    {
        Enumeration<URL> urls = _delegate.getResources(s);
        if (s.contains("META-INF/beans.xml"))
        {
            ArrayList<URL> tmpList = Collections.list(urls);
            //WeavingContext.getInstance().getConfiguration().getCompileTarget("/META-INF/beans.xml").mkdirs();
            tmpList.add(WeavingContext.getInstance().getConfiguration().getCompileTarget("/META-INF/beans.xml").toURI().toURL());
            return Collections.enumeration(tmpList);
        }
        return urls;
    }

    public static URL getSystemResource(String s)
    {
        return ClassLoader.getSystemResource(s);
    }

    public static Enumeration<URL> getSystemResources(String s) throws IOException
    {
        return ClassLoader.getSystemResources(s);
    }

    public static InputStream getSystemResourceAsStream(String s)
    {
        return ClassLoader.getSystemResourceAsStream(s);
    }

    public static ClassLoader getSystemClassLoader()
    {
        return ClassLoader.getSystemClassLoader();
    }


    @Override
    public void setDefaultAssertionStatus(boolean b)
    {
        _delegate.setDefaultAssertionStatus(b);
    }

    @Override
    public void setPackageAssertionStatus(String s, boolean b)
    {
        _delegate.setPackageAssertionStatus(s, b);
    }

    @Override
    public void setClassAssertionStatus(String s, boolean b)
    {
        _delegate.setClassAssertionStatus(s, b);
    }

    @Override
    public void clearAssertionStatus()
    {
        _delegate.clearAssertionStatus();
    }
}

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
package org.apache.myfaces.extensions.scripting.cdi.monitor.resources;

import java.io.File;

/**
 * 
 *
 */
public interface Resource {

    /**
     * <p>Returns a reference to this resource on the file system,
     * i.e it returns a reference to a java.io.File object.</p>
     *
     * @return a reference to this resource on the file system
     */
    public File getFile();

    /**
     * <p>Returns the time that the resource denoted by this reference was last modified.</p>
     *
     * @return  A <code>long</code> value representing the time the file was
     *          last modified or <code>0L</code> if the file does not exist
     *          or if an I/O error occurs
     */
    public long lastModified();

}
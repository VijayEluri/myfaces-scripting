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
package org.apache.myfaces.scripting.core.dependencyScan;

import org.objectweb.asm.*;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Werner Punz (latest modification by $Author$)
 * @version $Revision$ $Date$
 */

class ClassScanVisitor implements ClassVisitor {

    Set<String> dependencies;
    //static final Logger log = Logger.getLogger("ClassScanVisitor");

    public ClassScanVisitor() {

    }

    public ClassScanVisitor(Set<String> dependencies) {
        super();
        this.dependencies = dependencies;
    }


    public void setDependencyTarget(Set<String> dependencyTarget) {
        dependencies = dependencyTarget;
    }

    public void visit(int version, int access, String name,
                      String signature, String superName, String[] interfaces) {
        //log.log(Level.INFO, "{0} extends {1} ", new String[]{name, superName});

        ClassLogUtils.logParmList(dependencies, superName);
    }

    public void visitSource(String source, String debug) {
        //log.log(Level.INFO, "source: {0}", source);
    }

    public void visitOuterClass(String owner, String name, String desc) {
        //nothing has to be done here I guess because
        //we only try to fetch the dependencies
    }

    public AnnotationVisitor visitAnnotation(String desc,
                                             boolean visible) {
        return null;
    }

    public void visitAttribute(Attribute attr) {
        //log.log(Level.INFO, "Attribute: {0}", attr.type);

    }

    public void visitInnerClass(String name, String outerName,
                                String innerName, int access) {
        //same as outer class
    }

    public FieldVisitor visitField(int access, String name, String desc,
                                   String signature, Object value) {
        //log.log(Level.INFO, "Field:{0} {1} ", new Object[]{desc, name});
        ClassLogUtils.logParmList(dependencies, desc);

        return null;
    }

    public MethodVisitor visitMethod(int access, String name,
                                     String desc, String signature, String[] exceptions) {
        //log.log(Level.INFO, "Method {0} {1} ", new Object[]{name, desc});
        //String subDesc = desc.substring(desc.indexOf('(') + 1, desc.lastIndexOf(")"));
        //String[] parms = subDesc.split(";");

        //ClassLogUtils.logParmList(dependencies, parms);

        //we now have to dig into the method to cover more, the parms are covered by our method scanner
        return new MethodScanVisitor(dependencies);
    }

    public void visitEnd() {
        //log.info("}");
    }

}

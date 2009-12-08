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
package org.apache.myfaces.scripting.core.util;

import org.apache.myfaces.scripting.loaders.java.util.DirStrategy;
import org.apache.myfaces.scripting.loaders.java.util.FileStrategy;

import java.io.File;
import java.util.List;

/**
 * @author Werner Punz (latest modification by $Author$)
 * @version $Revision$ $Date$
 */

public class FileUtils {
    static double _tempMarker = Math.random();


    

    public static File getTempDir() {
        File tempDir = null;

        String baseTempPath = System.getProperty("java.io.tmpdir");
        String tempDirName = "myfaces_compilation_" + _tempMarker;

        tempDir = new File(baseTempPath + File.separator + tempDirName);
        while (tempDir.exists()) {
            tempDirName = "myfaces_compilation_" + System.currentTimeMillis() + Math.random();
            tempDir = new File(baseTempPath + File.separator + tempDirName);
        }

        synchronized (FileUtils.class) {
            if (tempDir.exists()) {
                return tempDir;
            }
            tempDir.mkdirs();
            tempDir.deleteOnExit();
        }
        return tempDir;
    }


    /**
     * we roll our own treewalker here
     * to avoid a dependency into commons fileutils
     * and to apply an easier pattern than
     * commons fileutils uses
     *
     * @param rootDir
     * @param strategy
     */
    public static void listFiles(File rootDir, Strategy strategy) {
        if(!rootDir.isDirectory()) {
            strategy.apply(rootDir);
            return;
        }

        //TODO apply a filter here instead of doing the check directly
        File [] files = rootDir.listFiles();
        for(File file: files) {
            boolean isDirectory = file.isDirectory();
            if(isDirectory && !file.getName().endsWith(".")) {
                listFiles(file, strategy);
                strategy.apply(file);
            } else if (!isDirectory) {
                strategy.apply(file);
            }
        }
    }

    /**
     * <p>
     * target path check to check if the targetPath is valid or can be created
     * </p>
     *
     * @param path
     */
    public static void assertPath(File path) {
        // The destination directory must already exist as javac will not create the destination directory.
        if (!path.exists()) {
            if (!path.mkdirs()) {
                throw new IllegalStateException("It wasn't possible to create the target " +
                                                "directory for the compiler ['" + path.getAbsolutePath() + "'].");
            }

            // If we've created the destination directory, we'll delete it as well once the application exits
            path.deleteOnExit();
        }
    }


    /**
     * fetches recursively the files under the current root
     *
     * @param sourcePath the source path from which the walker should start from
     * @param fileType the pattern upon which the file has to be matched to aka *.java etc...
     * @return
     */
    public static List<File> fetchSourceFiles(File sourcePath, String fileType) {
        FileStrategy strategy = new FileStrategy(fileType);
        listFiles(sourcePath, strategy);

        return strategy.getFoundFiles();
    }


    /**
     * fetches the source paths from a given root directory in the format
     * <path>/<appendix>;...
     *
     * @param sourcePath the sourcePath from which the directory traversal should happen from
     * @param appendix the appendix which has to be appended to every path found
     * @return a string builder of the paths found
     */
    public static StringBuilder fetchSourcePaths(File sourcePath,  String appendix) {
        DirStrategy dirStrategy = new DirStrategy();
        listFiles(sourcePath, dirStrategy);

        StringBuilder sourcesList = new StringBuilder(512);

        String root = sourcePath.getAbsolutePath();
        int rootLen = root.length() + 1;
        for (File foundDir : dirStrategy.getFoundFiles()) {
            String dirName = foundDir.getAbsolutePath();
            sourcesList.append(dirName);
            sourcesList.append( File.separator);
            sourcesList.append(appendix);
        }
        return sourcesList;
    }
}

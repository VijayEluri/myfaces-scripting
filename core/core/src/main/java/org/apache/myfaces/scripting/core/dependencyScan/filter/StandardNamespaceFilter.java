package org.apache.myfaces.scripting.core.dependencyScan.filter;

import org.apache.myfaces.scripting.core.dependencyScan.core.ClassScanUtils;
import org.apache.myfaces.scripting.core.dependencyScan.api.ClassFilter;

/**
 * Filter facade for our standard namespace check
 */
public class StandardNamespaceFilter implements ClassFilter {

    public boolean isAllowed(Integer engineType, String clazz) {
        return !ClassScanUtils.isStandardNamespace(clazz);
    }
}
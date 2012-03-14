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
package org.apache.myfaces.extensions.scripting.jsf;

import org.apache.myfaces.extensions.scripting.core.util.WeavingContext;

import javax.faces.event.PhaseListener;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.context.FacesContext;
import java.util.Map;

/**
 * We use a phase listener here for all parts of the refresh
 * which have to rely on jsf access
 * <p/>
 * Which means all parts which need some kind of FacesConfig have
 * to rely on this one, we trigger before the first phase
 *
 * @author Werner Punz (latest modification by $Author$)
 * @version $Revision$ $Date$
 *          
 */

public class RefreshPhaseListener implements PhaseListener {

    public static final String EQ_KEY = "RefreshPhaseListenerDone";

    /**
     * this eases testing, because in our
     * absence of intelligence we have
     * not done yet the WeavingContext as singleton
     * like it should be
     */
    static Runnable _action = new Runnable() {
        public void run() {
              WeavingContext.jsfRequestRefresh();
        }
    };

    public void afterPhase(PhaseEvent event) {
    }

    @SuppressWarnings("unchecked")
    public void beforePhase(PhaseEvent event) {
        //we fetch the earliest phase possible, in case of a normal get it is the render phase
        //in every other case it is the restore view phase
        Map requestMap = FacesContext.getCurrentInstance().getExternalContext().getRequestMap();
        if (requestMap.containsKey(EQ_KEY)) return;
        requestMap.put(EQ_KEY, Boolean.TRUE);

       RefreshPhaseListener._action.run();
    }

    public PhaseId getPhaseId() {
        return PhaseId.ANY_PHASE;
    }

    @SuppressWarnings("unused")
    public static void applyAction(Runnable newAction) {
        _action = newAction;
    }

}
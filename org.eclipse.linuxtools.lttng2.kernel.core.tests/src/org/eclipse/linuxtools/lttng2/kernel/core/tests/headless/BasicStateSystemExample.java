/*******************************************************************************
 * Copyright (c) 2012 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Mathieu Denis <mathieu.denis@polymtl.ca> - Initial API and implementation
 *******************************************************************************/

package org.eclipse.linuxtools.lttng2.kernel.core.tests.headless;

import java.io.File;
import java.util.List;

import org.eclipse.linuxtools.internal.lttng2.kernel.core.Attributes;
import org.eclipse.linuxtools.internal.lttng2.kernel.core.stateprovider.CtfKernelStateInput;
import org.eclipse.linuxtools.lttng2.kernel.core.tests.stateprovider.CtfTestFiles;
import org.eclipse.linuxtools.tmf.core.exceptions.AttributeNotFoundException;
import org.eclipse.linuxtools.tmf.core.exceptions.StateValueTypeException;
import org.eclipse.linuxtools.tmf.core.exceptions.TimeRangeException;
import org.eclipse.linuxtools.tmf.core.exceptions.TmfTraceException;
import org.eclipse.linuxtools.tmf.core.interval.ITmfStateInterval;
import org.eclipse.linuxtools.tmf.core.statesystem.IStateChangeInput;
import org.eclipse.linuxtools.tmf.core.statesystem.IStateSystemQuerier;
import org.eclipse.linuxtools.tmf.core.statesystem.StateSystemManager;

/**
 * Simple example of how to use the state system using a CTF kernel trace.
 *
 * @author Mathieu Denis
 */
public class BasicStateSystemExample {

    public static void main(String[] args) {
        /* Read a trace and build the state system */
        try {
            File newStateFile = new File("/tmp/helloworldctf.ht"); //$NON-NLS-1$
            IStateChangeInput input = new CtfKernelStateInput(CtfTestFiles.getTestTrace());
            IStateSystemQuerier ss = StateSystemManager.loadStateHistory(newStateFile, input, true);

            requestExample(ss);
        } catch (TmfTraceException e) {
            e.printStackTrace();
        }
    }

    /**
     * From a state system tree previously built with a CTF kernel trace, print
     * to the console the interval of each state and the ID of the current
     * thread running on each CPU.
     *
     * @param ssb
     *            the State System Builder through which make request
     */
    private static void requestExample(final IStateSystemQuerier ssb) {
        try {
            /* Request the current thread executing on each CPU */
            List<Integer> currentThreadByCPUS;
            List<ITmfStateInterval> stateIntervals;
            StringBuilder output = new StringBuilder();

            currentThreadByCPUS = ssb.getQuarks(Attributes.CPUS, "*", Attributes.CURRENT_THREAD); //$NON-NLS-1$

            for (Integer currentThread : currentThreadByCPUS) {
                stateIntervals = ssb.queryHistoryRange(currentThread.intValue(), ssb.getStartTime(),
                        ssb.getCurrentEndTime());

                /* Output formatting */
                output.append("Value of attribute : "); //$NON-NLS-1$
                output.append(ssb.getFullAttributePath(currentThread.intValue()));
                output.append("\n------------------------------------------------\n"); //$NON-NLS-1$
                for (ITmfStateInterval stateInterval : stateIntervals) {
                    /* Print the interval */
                    output.append('[');
                    output.append(String.valueOf(stateInterval.getStartTime()));
                    output.append(", "); //$NON-NLS-1$
                    output.append(String.valueOf(stateInterval.getEndTime()));
                    output.append(']');
                    /* Print the attribute value */
                    output.append(" = "); //$NON-NLS-1$
                    output.append(stateInterval.getStateValue().unboxInt());
                    output.append('\n');
                }
            }
            System.out.println(output.toString());
        } catch (TimeRangeException e) {
            e.printStackTrace();
        } catch (AttributeNotFoundException e) {
            e.printStackTrace();
        } catch (StateValueTypeException e) {
            e.printStackTrace();
        }
    }
}
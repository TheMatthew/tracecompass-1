/*******************************************************************************
 * Copyright (c) 2015 EfficiOS Inc., Alexandre Montplaisir
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Alexandre Montplaisir - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.os.linux.core.latency;

import java.io.Serializable;
import java.util.Map;

import org.eclipse.tracecompass.analysis.os.linux.core.timerangestore.ITimeRange;

import com.google.common.collect.ImmutableMap;

/**
 * @author efficios
 * @since 1.0
 */
public class SystemCall implements ITimeRange {

    private static final long serialVersionUID = 1554494342105208730L;

    public static class InitialInfo implements Serializable {

        private static final long serialVersionUID = -5009710718804983721L;

        private final long fStartTime;
        private final String fName;
        private final Map<String, String> fArgs;

        public InitialInfo(
                long startTime,
                String name,
                Map<String, String> arguments) {
            fStartTime = startTime;
            fName = name;
            fArgs = ImmutableMap.copyOf(arguments);
        }
    }

    private final InitialInfo fInfo;
    private final long fEndTime;
    private final int fRet;
    private final transient long fDuration;

    public SystemCall(
            InitialInfo info,
            long endTime,
            int ret) {
        fInfo = info;
        fEndTime = endTime;
        fRet = ret;
        fDuration = fEndTime - fInfo.fStartTime;
    }

    @Override
    public long getStartTime() {
        return fInfo.fStartTime;
    }

    @Override
    public long getEndTime() {
        return fEndTime;
    }

    @Override
    public long getDuration() {
        return fDuration;
    }

    public String getName() {
        return fInfo.fName;
    }

    public Map<String, String> getArguments() {
        return fInfo.fArgs;
    }

    public int getReturnValue() {
        return fRet;
    }

    @Override
    public String toString() {
        return "Start Time = " + getStartTime() +
                "; End Time = " + getEndTime() +
                "; Duration = " + getDuration() +
                "; Name = " + getName() +
                "; Args = " + getArguments().toString() +
                "; Return = " + getReturnValue();
    }
}

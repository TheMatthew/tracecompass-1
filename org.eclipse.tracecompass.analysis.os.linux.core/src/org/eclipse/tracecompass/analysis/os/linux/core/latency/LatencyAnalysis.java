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

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.kernelanalysis.KernelTidAspect;
import org.eclipse.tracecompass.analysis.os.linux.core.timerangestore.ITimeRangeStore;
import org.eclipse.tracecompass.analysis.os.linux.core.timerangestore.JDBMTimeRangeStore;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelTrace;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAbstractAnalysisModule;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.request.ITmfEventRequest;
import org.eclipse.tracecompass.tmf.core.request.TmfEventRequest;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;

/**
 * @author efficios
 * @since 1.0
 */
public class LatencyAnalysis extends TmfAbstractAnalysisModule {

    public static final String ID = "org.eclipse.tracecompass.analysis.os.linux.latency"; //$NON-NLS-1$

    private static final String DATA_FILENAME = "latency-analysis.dat"; //$NON-NLS-1$

    private @Nullable ITimeRangeStore<SystemCall> fSystemCalls;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    protected boolean executeAnalysis(IProgressMonitor monitor) throws TmfAnalysisException {
        IKernelTrace trace = checkNotNull((IKernelTrace) getTrace());
        IKernelAnalysisEventLayout layout = trace.getKernelEventLayout();

        /* See if the data file already exists on disk */
        String dir = TmfTraceManager.getSupplementaryFileDir(trace);
        final Path file = Paths.get(dir, DATA_FILENAME);

        if (Files.exists(file)) {
            System.out.println("re-read existing file!");
            /* Attempt to read the existing file */
            try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(file))) {
                @SuppressWarnings("unchecked")
                ITimeRangeStore<SystemCall> syscalls = (ITimeRangeStore<SystemCall>) ois.readObject();
                fSystemCalls = syscalls;

                System.out.println(syscalls.getNbElements() + " syscalls");

                return true;

            } catch (IOException | ClassNotFoundException | ClassCastException e) {
                /*
                 * We did not manage to read the file successfully, we will just
                 * fall-through to rebuild a new one.
                 */
                try {
                    Files.delete(file);
                } catch (IOException e1) {
                }
            }
        }

        ITimeRangeStore<SystemCall> syscalls = new JDBMTimeRangeStore<>();
        ITmfEventRequest req = new LatencyAnalysisRequest(layout, syscalls);
        trace.sendRequest(req);
        try {
            req.waitForCompletion();
        } catch (InterruptedException e) {
        }
        /* The request will fill 'syscalls' */

        fSystemCalls = syscalls;
        System.out.println(syscalls.getNbElements() + " syscalls");

        /* Serialize the collections to disk for future usage */
        try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(file))) {
            System.out.println("writing new file!");
            oos.writeObject(syscalls);
        } catch (IOException e) {
            /* Didn't work, oh well. We will just re-read the trace next time */
        }

        return true;
    }

    @Override
    protected void canceling() {

    }

    public @Nullable ITimeRangeStore<SystemCall> getResults() {
        return fSystemCalls;
    }

    private static class LatencyAnalysisRequest extends TmfEventRequest {

        private final IKernelAnalysisEventLayout fLayout;
        private final ITimeRangeStore<SystemCall> fFullSyscalls;
        private final Map<Integer, SystemCall.InitialInfo> fOngoingSystemCalls = new HashMap<>();

        public LatencyAnalysisRequest(IKernelAnalysisEventLayout layout, ITimeRangeStore<SystemCall> syscalls) {
            super(ITmfEvent.class, 0, ITmfEventRequest.ALL_DATA, ExecutionType.BACKGROUND);
            fLayout = layout;
            /*
             * We do NOT make a copy here! We want to modify the list that was
             * passed in parameter.
             */
            fFullSyscalls = syscalls;
        }

        @Override
        public void handleData(final ITmfEvent event) {
            super.handleData(event);
            final String eventName = event.getType().getName();

            if (eventName.startsWith(fLayout.eventSyscallEntryPrefix()) ||
                    eventName.startsWith(fLayout.eventCompatSyscallEntryPrefix())) {
                /* This is a system call entry event */

                Integer tid = KernelTidAspect.INSTANCE.resolve(event);
                if (tid == null) {
                    // no information on this event/trace ?
                    return;
                }

                /* Record the event's data into the intial system call info */
                // String syscallName = fLayout.getSyscallNameFromEvent(event);
                long startTime = event.getTimestamp().getValue();
                String syscallName = eventName.substring(fLayout.eventSyscallEntryPrefix().length());
                FluentIterable<String> argNames = FluentIterable.from(event.getContent().getFieldNames());
                Map<String, String> args = argNames.toMap(new Function<String, String>() {
                    @Override
                    public String apply(@Nullable String input) {
                        return checkNotNull(event.getContent().getField(input).getValue().toString());
                    }
                });
                SystemCall.InitialInfo newSysCall = new SystemCall.InitialInfo(startTime, syscallName, args);
                fOngoingSystemCalls.put(tid, newSysCall);

            } else if (eventName.startsWith(fLayout.eventSyscallExitPrefix())) {
                /* This is a system call exit event */

                Integer tid = KernelTidAspect.INSTANCE.resolve(event);
                if (tid == null) {
                    return;
                }

                SystemCall.InitialInfo info = fOngoingSystemCalls.remove(tid);
                if (info == null) {
                    /*
                     * We have not seen the entry event corresponding to this
                     * exit (lost event, or before start of trace).
                     */
                    return;
                }

                long endTime = event.getTimestamp().getValue();
                int ret = ((Long) event.getContent().getField("ret").getValue()).intValue();
                SystemCall syscall = new SystemCall(info, endTime, ret);
                fFullSyscalls.addValue(syscall);
            }
        }

        @Override
        public void handleCompleted() {
            fOngoingSystemCalls.clear();
        }
    }


}

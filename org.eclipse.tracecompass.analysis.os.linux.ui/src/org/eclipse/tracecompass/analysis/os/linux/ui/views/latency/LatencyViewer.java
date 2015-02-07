/*******************************************************************************
 * Copyright (c) 2015 EfficiOS Inc., Alexandre Montplaisir
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Alexandre Montplaisir - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.os.linux.ui.views.latency;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ILazyContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.tracecompass.analysis.os.linux.core.latency.LatencyAnalysis;
import org.eclipse.tracecompass.analysis.os.linux.core.latency.SystemCall;
import org.eclipse.tracecompass.analysis.os.linux.core.timerangestore.ITimeRangeStore;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceClosedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceSelectedSignal;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestampFormat;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.ui.TmfUiRefreshHandler;
import org.eclipse.tracecompass.tmf.ui.viewers.TmfViewer;

/**
 * Displays the content of the state systems at the current time
 *
 * @author Alexandre Montplaisir
 * @since 1.0
 */
public class LatencyViewer extends TmfViewer {

    private final TableViewer fTableViewer;

    public LatencyViewer(Composite parent) {
        super(parent);
        fTableViewer = new TableViewer(parent, SWT.VIRTUAL);

        createColumns(fTableViewer);

        final Table table = fTableViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);

        fTableViewer.setContentProvider(new LatencyContentProvider(fTableViewer));
        fTableViewer.setUseHashlookup(true);
        reloadContents();
    }

    private static class LatencyContentProvider implements ILazyContentProvider {

        private final TableViewer fViewer;
        private @Nullable ITimeRangeStore<SystemCall> fInput;

        public LatencyContentProvider(TableViewer viewer) {
            this.fViewer = viewer;
        }

        @Override
        public void dispose() {
            if (fInput != null) {
                fInput.dispose();
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public void inputChanged(@Nullable Viewer viewer, @Nullable Object oldInput, @Nullable Object newInput) {
            /* Do not copy, we want to see potential changes */
            fInput = (ITimeRangeStore<SystemCall>) newInput;
        }

        @Override
        public void updateElement(int index) {
            if (fInput != null) {
                fViewer.replace(fInput.getElementAtIndex(index), index);
            }
        }
    }

    private void reloadContents() {
        ITmfTrace activeTrace = TmfTraceManager.getInstance().getActiveTrace();
        if (activeTrace == null) {
            /* No active trace, clear the view */
            clearAll();
            return;
        }
        reloadContents(activeTrace);
    }

    private void reloadContents(ITmfTrace trace) {
        final LatencyAnalysis analysis = TmfTraceUtils.getAnalysisModuleOfClass(trace, LatencyAnalysis.class, LatencyAnalysis.ID);
        if (analysis == null) {
            /* The active trace does not have anything to populate the view */
            clearAll();
            return;
        }

        // FIXME We should make sure there is only one instance of this thread running
        Thread updateThread = new Thread(new Runnable() {
            @Override
            public void run() {
                analysis.schedule();
                analysis.waitForCompletion();
                final ITimeRangeStore<SystemCall> data = analysis.getResults();
                if (data == null) {
                    /*
                     * Could be empty, but should not be null after
                     * waitForCompletion()
                     */
                    throw new IllegalStateException();
                }
                TmfUiRefreshHandler.getInstance().queueUpdate(this, new Runnable() {
                    @Override
                    public void run() {
                        fTableViewer.setInput(data);
                        // FIXME The viewer can only display Integer.MAX_VALUE
                        // events, but the backend may have more than that...
                        fTableViewer.setItemCount((int) data.getNbElements());
                        refresh();
                    }
                });
            }
        });
        updateThread.start();
    }

    private void clearAll() {
        fTableViewer.setInput(null);
        fTableViewer.setItemCount(0);
        refresh();
    }

    private static abstract class SyscallColumnLabelProvider extends ColumnLabelProvider {

        @Override
        public String getText(@Nullable Object input) {
            if (!(input instanceof SystemCall)) {
                /* Doubles as a null check */
                return ""; //$NON-NLS-1$
            }
            return getTextForSyscall((SystemCall) input);
        }

        public abstract String getTextForSyscall(SystemCall syscall);
    }

    private static void createColumns(TableViewer viewer) {
        /* m-muh lambdas... :'( */
        createColumn(viewer, "Start Time", 200, new SyscallColumnLabelProvider() {
            @Override
            public String getTextForSyscall(SystemCall syscall) {
                long startTime = syscall.getStartTime();
                return TmfTimestampFormat.getDefaulTimeFormat().format(startTime);
            }
        });
        createColumn(viewer, "End Time", 200, new SyscallColumnLabelProvider() {
            @Override
            public String getTextForSyscall(SystemCall syscall) {
                long endTime = syscall.getEndTime();
                return TmfTimestampFormat.getDefaulTimeFormat().format(endTime);
            }
        });
        createColumn(viewer, "Duration", 75, new SyscallColumnLabelProvider() {
            @Override
            public String getTextForSyscall(SystemCall syscall) {
                return Long.toString(syscall.getDuration());
            }
        });
        createColumn(viewer, "Name", 100, new SyscallColumnLabelProvider() {
            @Override
            public String getTextForSyscall(SystemCall syscall) {
                return syscall.getName();
            }
        });
        createColumn(viewer, "Arguments", 400, new SyscallColumnLabelProvider() {
            @Override
            public String getTextForSyscall(SystemCall syscall) {
                return syscall.getArguments().toString();
            }
        });
        createColumn(viewer, "Return", 75, new SyscallColumnLabelProvider() {
            @Override
            public String getTextForSyscall(SystemCall syscall) {
                return Integer.toString(syscall.getReturnValue());
            }
        });
    }

    private static void createColumn(TableViewer viewer, String name, int width, ColumnLabelProvider provider) {
        TableViewerColumn col = new TableViewerColumn(viewer, SWT.NONE);
        col.getColumn().setWidth(width);
        col.getColumn().setText(name);
        col.setLabelProvider(provider);
    }

    @Override
    public Control getControl() {
        return fTableViewer.getControl();
    }

    @Override
    public void refresh() {
        fTableViewer.refresh();
    }

    // ------------------------------------------------------------------------
    // Signal handlers
    // ------------------------------------------------------------------------

    @TmfSignalHandler
    public void traceSelected(TmfTraceSelectedSignal signal) {
        reloadContents(signal.getTrace());
    }

    /**
     * @param signal
     */
    @TmfSignalHandler
    public void traceClosed(TmfTraceClosedSignal signal) {
        /* The last opened trace was closed */
        clearAll();
    }

}

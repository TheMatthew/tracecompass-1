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

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.tmf.ui.views.TmfView;

/**
 * Displays the State System at a current time.
 *
 * @author Alexandre Montplaisir
 * @since 1.0
 */
public class LatencyView extends TmfView {

    /** The view's ID */
    public static final String ID = "org.eclipse.tracecompass.analysis.os.linux.views.latency"; //$NON-NLS-1$

    private @Nullable LatencyViewer fViewer;

    /**
     * Constructor
     */
    public LatencyView() {
        super(ID);
    }

    // ------------------------------------------------------------------------
    // ViewPart
    // ------------------------------------------------------------------------

    @Override
    public void createPartControl(@Nullable Composite parent) {
        fViewer = new LatencyViewer(checkNotNull(parent));
    }

    @Override
    public void setFocus() {
    }

    @Override
    public void dispose() {
        super.dispose();
        if (fViewer != null) {
            fViewer.dispose();
        }
    }
}

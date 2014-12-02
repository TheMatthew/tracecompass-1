/**********************************************************************
 * Copyright (c) 2014 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Alexandre Montplaisir - Initial API and implementation
 **********************************************************************/

package org.eclipse.tracecompass.tmf.ui.viewers.events;

import java.util.Map;
import java.util.TreeMap;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfAnalysisModuleWithStateSystems;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.ui.dialogs.SelectionDialog;

/**
 * A dialog from which to choose a trace's state system, and an attribute
 * path in that state system.
 *
 * @author Alexandre Montplaisir
 */
public class StateSystemAttributeSelectionDialog extends SelectionDialog {

    private final Map<String, ITmfStateSystem> fTraceStateSystems = new TreeMap<>();

    private Combo fStateSystemSelection;
    private Text fAttributeSelection;

    /**
     * Constructor
     *
     * @param parentShell
     *            The parent shell of this widget
     * @param trace
     *            The trace from which to select the state system
     */
    public StateSystemAttributeSelectionDialog(Shell parentShell, ITmfTrace trace) {
        super(parentShell);
        setTitle(Messages.StateSystemAttributeSelectionDialog_WindowTitle);

        Iterable<ITmfAnalysisModuleWithStateSystems> modules =
                TmfTraceUtils.getAnalysisModulesOfClass(trace, ITmfAnalysisModuleWithStateSystems.class);
        for (ITmfAnalysisModuleWithStateSystems module : modules) {
            if (module == null) {
                continue;
            }
            for (ITmfStateSystem ss : module.getStateSystems()) {
                if (ss == null) {
                    continue;
                }
                fTraceStateSystems.put(ss.getSSID(), ss);
            }
        }
    }

    // ------------------------------------------------------------------------
    // Dialog
    // ------------------------------------------------------------------------

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        composite.setLayout(new GridLayout());
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        Font font = parent.getFont();
        Composite folderGroup = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        folderGroup.setLayout(layout);
        folderGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        // State system selection label
        Label stateSystemLabel = new Label(folderGroup, SWT.NONE);
        stateSystemLabel.setFont(font);
        stateSystemLabel.setText(Messages.StateSystemAttributeSelectionDialog_LabelStateSystem);

        // State system selection drop-down
        fStateSystemSelection = new Combo(folderGroup, SWT.BORDER);
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
        fStateSystemSelection.setLayoutData(data);
        fStateSystemSelection.setFont(font);
        for (String ids : fTraceStateSystems.keySet()) {
            fStateSystemSelection.add(ids);
        }

        // Attribute selection label
        Label attributeLabel = new Label(folderGroup, SWT.NONE);
        attributeLabel.setFont(font);
        attributeLabel.setText(Messages.StateSystemAttributeSelectionDialog_LabelAttributePath);

        // Attribute selection text field
        fAttributeSelection = new Text(folderGroup, SWT.BORDER);
        data = new GridData(GridData.FILL_HORIZONTAL);
        data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
        fAttributeSelection.setLayoutData(data);
        fAttributeSelection.setFont(font);

        return composite;
    }

    @Override
    public void create() {
        super.create();
    }

    @Override
    protected void setShellStyle(int newShellStyle) {
        /* Use a mode-less window */
        super.setShellStyle(SWT.CLOSE | SWT.MODELESS| SWT.BORDER | SWT.TITLE);
        setBlockOnOpen(false);
    }

    @Override
    protected void okPressed() {
        ITmfStateSystem ss = fTraceStateSystems.get(fStateSystemSelection.getText());
        setSelectionResult(new Object[] { ss, fAttributeSelection.getText() });
        super.okPressed();
    }
}

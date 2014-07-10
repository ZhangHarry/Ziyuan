/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package tzuyu.plugin.tester.command.gentestwizard;

import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import tzuyu.plugin.commons.dto.WorkObject;
import tzuyu.plugin.tester.command.gentest.GenTestHandler;
import tzuyu.plugin.tester.command.gentest.GenTestPreferences;

/**
 * @author LLT
 * 
 */
public class GenTestWizardHandler extends GenTestHandler {

	@Override
	protected void run(WorkObject workObject, GenTestPreferences config) {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getShell();
		GenTestWizard wizard = new GenTestWizard(workObject, config);
		WizardDialog dialog = new WizardDialog(shell, wizard);
		dialog.create();
		if (dialog.open() == WizardDialog.OK) {
			super.run(workObject, config);
		}
	}

}

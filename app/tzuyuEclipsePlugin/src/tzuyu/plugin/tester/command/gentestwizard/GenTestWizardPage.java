/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package tzuyu.plugin.tester.command.gentestwizard;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.ui.wizards.NewElementWizardPage;

import tzuyu.plugin.TzuyuPlugin;
import tzuyu.plugin.commons.constants.Messages;
import tzuyu.plugin.tester.command.gentest.GenTestPreferences;
import tzuyu.plugin.tester.ui.AppEventManager;
import tzuyu.plugin.tester.ui.ValueChangedEvent;
import tzuyu.plugin.tester.ui.ValueChangedListener;

/**
 * @author LLT
 *
 */
public abstract class GenTestWizardPage extends NewElementWizardPage {
	protected GenTestPreferences prefs;
	protected AppEventManager eventManager;
	
	protected Messages msg = TzuyuPlugin.getMessages();
	
	protected GenTestWizardPage(String pageName, GenTestPreferences prefs, AppEventManager eventManager) {
		super(pageName);
		this.prefs = prefs;
		this.eventManager = eventManager;
	}
	
	protected void registerStatusChangeListener(Object source) {
		eventManager.register(ValueChangedEvent.TYPE, new ValueChangedListener<IStatus[]>(source) {

			@Override
			public void onValueChanged(ValueChangedEvent<IStatus[]> event) {
				updateStatus(event.getNewVal());
			}
		});
	}
	
	public abstract void preformFinish();
	
	
}

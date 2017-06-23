/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package tzuyu.plugin.tester.reporter;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.ui.statushandlers.StatusManager;

import sav.common.core.Logger;
import sav.common.core.utils.StringUtils;
import tzuyu.plugin.TzuyuPlugin;
import tzuyu.plugin.commons.constants.Messages;
import tzuyu.plugin.commons.utils.IStatusUtils;

/**
 * @author LLT
 * for logging in plugin.
 */
public class PluginLogger extends Logger<PluginLogger> {
	private static boolean debug = TzuyuPlugin.debug();
	private static final Messages msg = TzuyuPlugin.getMessages();
	private static final PluginLogger instance = new PluginLogger();
	
	public static PluginLogger getLogger() {
		return instance;
	}
	
	@Override
	public PluginLogger info(Object... msgs) {
		log(StringUtils.spaceJoin(msgs), IStatus.INFO);
		return this;
	}
	
	@Override
	public PluginLogger warn(Object... msgs) {
		log(StringUtils.spaceJoin(msgs), IStatus.WARNING);
		return this;
	}

	@Override
	public PluginLogger error(Object... msgs) {
		log(StringUtils.spaceJoin(msgs), IStatus.ERROR);
		return this;
	}
	
	@Override
	public PluginLogger debug(String msg) {
		if (debug) {
			info(msg);
		}
		return this;
	}
	
	/**
	 * type = 
	 * {@link org.eclipse.core.runtime.IStatus#INFO},
	 * {@link org.eclipse.core.runtime.IStatus#WARNING},
	 * {@link org.eclipse.core.runtime.IStatus#ERROR}
	 */
	public void log(String str, int type) {
		StatusManager.getManager().handle(IStatusUtils.status(type, str));
	}
	
	public void logEx(Exception e, String msg) {
		StatusManager.getManager().handle(IStatusUtils.exception(e, msg));
	}
	
	public void logEx(Exception e) {
		logEx(e, "");
	}

	@Override
	public void logEx(Exception ex, Enum<?> type) {
		logEx(ex, type == null ? "" : msg.getMessage(type));
	}

	public void debugGetImplForType(Class<?> clazz, String subType) {
		if (debug) {
			debug("Randomness. selected for interface: ", clazz.getName(), 
					"\n Result: ", StringUtils.nullToEmpty(subType));
		}
	}
}

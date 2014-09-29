/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package sav.common.core;

import sav.common.core.utils.StringUtils;

/**
 * @author LLT
 * 
 */
public abstract class Logger<T extends Logger<T>> {
	/* TODO - nice to have: better manage logger if having time */
	private static Logger<?> defaultLogger;
	
	public static Logger<?> getDefaultLogger() {
		return defaultLogger;
	}
	
	public static void setDefaultLogger(Logger<?> defaultLogger) {
		Logger.defaultLogger = defaultLogger;
	}
	
	public abstract T info(Object... msgs);

	public abstract T error(Object... msgs);

	public abstract void logEx(Exception ex, String msg);

	public abstract void close();

	public abstract void debug(String msg);

	public void logEx(SavException ex) {
		logEx(ex, ex.getType());
	}

	public void logEx(SavRtException ex) {
		logEx(ex, ex.getType());
	}

	protected abstract void logEx(Exception ex, Enum<?> type);

	public void debug(Object... msgs) {
		if (isDebug()) {
			info(StringUtils.spaceJoin(msgs));
		}
	}

	protected abstract boolean isDebug();
}

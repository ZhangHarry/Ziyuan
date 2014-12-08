/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package sav.common.core.utils;

import sav.common.core.Logger;


/**
 * @author LLT
 * If the exception because of Assertion error, means it needs to be fixed.
 */
public class Assert {
	private static Logger<?> log = Logger.getDefaultLogger();

	public static <T> void notNull(T value, String... msgs) {
		assertTrue(value != null, msgs);
	}

	public static <T> void assertTrue(boolean condition, String... msgs) {
		if (!condition) {
			String msg = StringUtils.EMPTY;
			if (msgs != null) {
				msg = StringUtils.spaceJoin((Object[]) msgs);
			}
			log.error((Object[])msgs);
			throw new IllegalArgumentException(msg);
		}
	}

	public static void fail(String msg) {
		throw new IllegalArgumentException(msg);
	}

}

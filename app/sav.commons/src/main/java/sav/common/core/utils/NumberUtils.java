/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package sav.common.core.utils;

/**
 * @author LLT
 *
 */
public class NumberUtils {
	private NumberUtils() {
	}
	
	public static boolean isNumber(String str)  {
		try {
			Integer.parseInt(str);
			return true;
		} catch(Exception ex) {
			return false;
		}
	}
}

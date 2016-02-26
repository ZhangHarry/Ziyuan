/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package microbat.util;

import sav.common.core.utils.CollectionUtils;


/**
 * @author LLT
 *
 */
public class PrimitiveUtils {
	private PrimitiveUtils(){}
	
	public static String[] PRIMITIVE_TYPES = new String[]{
		Integer.class.getName(),
		Boolean.class.getName(),
		Float.class.getName(),
		Character.class.getName(),
		Double.class.getName(),
		Long.class.getName(),
		Short.class.getName(),
		Byte.class.getName(),
	};
	private static String STRING_TYPE = String.class.getName();
	
	public static boolean isPrimitiveType(String clazzName) {
		return CollectionUtils.existIn(clazzName, PRIMITIVE_TYPES);
	}
	
	public static boolean isPrimitiveTypeOrString(String clazzName) {
		return isPrimitiveType(clazzName) || isString(clazzName) || clazzName.equals("String");
	}

	public static boolean isString(String clazzName) {
		return STRING_TYPE.equals(clazzName);
	}
	
	
}

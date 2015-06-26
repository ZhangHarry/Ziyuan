/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package icsetlv.common.dto;


/**
 * @author LLT
 *
 */
public class PrimitiveValue extends ExecValue {
	private String strVal;

	public PrimitiveValue(String id, String strVal) {
		super(id);
		this.strVal = strVal;
	}

	public String getStrVal() {
		return strVal;
	}
	
	@Override
	public double getDoubleVal() {
		try {
			return Double.parseDouble(strVal);
		} catch (NumberFormatException e) {
			return super.getDoubleVal();
		}
	}
	
	@Override
	public String toString() {
		return String.format("(%s:%s)", varId, strVal);
	}
	
	public static PrimitiveValue of(String id, boolean value) {
		return new BooleanValue(id, value);
	}
}

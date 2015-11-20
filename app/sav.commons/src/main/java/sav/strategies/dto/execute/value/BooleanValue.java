/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package sav.strategies.dto.execute.value;

/**
 * @author LLT
 *
 */
public class BooleanValue extends PrimitiveValue {
	private boolean value;

	public BooleanValue(String id, boolean value) {
		super(id, String.valueOf(value), "boolean");
		this.value = value;
	}

	@Override
	public double getDoubleVal() {
		if (value) {
			return 1;
		} else {
			return 0;
		}
	}
	
	public static BooleanValue of(String id, boolean value) {
		return new BooleanValue(id, value);
	}
	
	@Override
	public ExecVarType getType() {
		return ExecVarType.BOOLEAN;
	}
}

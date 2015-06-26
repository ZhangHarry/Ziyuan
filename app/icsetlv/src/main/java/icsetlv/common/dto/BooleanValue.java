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
public class BooleanValue extends PrimitiveValue {
	private boolean value;

	public BooleanValue(String id, boolean value) {
		super(id, String.valueOf(value));
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
}

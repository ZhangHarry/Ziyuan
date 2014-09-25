/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package faultLocalization.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * @author LLT
 * 
 */
public class ClassCoverageInSingleTestcase extends AbstractClassCoverage {
	private Map<Integer, Boolean> lineCorverageMap = new HashMap<Integer, Boolean>();

	public ClassCoverageInSingleTestcase(final String classResourcePath) {
		super(classResourcePath);
	}

	public void addInfo(int line, boolean isCovered) {
		lineCorverageMap.put(line, isCovered);
	}
}

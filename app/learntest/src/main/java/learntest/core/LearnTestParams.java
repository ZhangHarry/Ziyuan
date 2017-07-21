/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package learntest.core;

import java.util.Collections;
import java.util.List;

import learntest.core.LearntestParamsUtils.GenTestPackage;
import learntest.core.commons.data.LearnTestApproach;
import learntest.core.commons.data.classinfo.JunitTestsInfo;
import learntest.core.commons.data.classinfo.TargetMethod;
import sav.common.core.ISystemVariable;
import sav.strategies.dto.SystemPreferences;

/**
 * @author LLT
 *
 */
public class LearnTestParams {
	private LearnTestApproach approach;
	private TargetMethod targetMethod;
	private JunitTestsInfo initialTests;
	private SystemPreferences systemConfig;
	private int maxTcs;
	
	public LearnTestParams() {
		systemConfig = new SystemPreferences();
	}
	
	public LearnTestParams(TargetMethod targetMethod) {
		this();
		this.targetMethod = targetMethod;
	}

	public TargetMethod getTargetMethod() {
		return targetMethod;
	}

	public void setTargetMethod(TargetMethod targetMethod) {
		this.targetMethod = targetMethod;
	}
	
	public boolean isLearnByPrecond() {
		return approach == LearnTestApproach.L2T;
	}
	
	public LearnTestApproach getApproach() {
		return approach;
	}

	@SuppressWarnings("unchecked")
	public List<String> getInitialTestcases() {
		if (initialTests == null) {
			return Collections.EMPTY_LIST;
		}
		return initialTests.getJunitTestcases();
	}
	
	public void setInitialTests(JunitTestsInfo initialTests) {
		this.initialTests = initialTests;
	}
	
	public JunitTestsInfo getInitialTests() {
		if (initialTests == null) {
			initialTests = new JunitTestsInfo();
		}
		return initialTests;
	}

	public void setApproach(LearnTestApproach approach) {
		this.approach = approach;
	}

	public int getMaxTcs() {
		return maxTcs;
	}

	public void setMaxTcs(int maxTcs) {
		this.maxTcs = maxTcs;
	}

	public String getTestPackage(GenTestPackage phase) {
		return LearntestParamsUtils.getTestPackage(this, phase);
	}
	
	public LearnTestParams createNew() {
		LearnTestParams params = new LearnTestParams();
		params.targetMethod = targetMethod;
		params.systemConfig = systemConfig;
		return params;
	}
	
	public SystemPreferences getSystemConfig() {
		return systemConfig;
	}
	
	public static enum LearntestSystemVariable implements ISystemVariable {
		JDART_APP_PROPRETIES,
		JDART_SITE_PROPRETIES;

		public String getName() {
			return name();
		}
	}
}

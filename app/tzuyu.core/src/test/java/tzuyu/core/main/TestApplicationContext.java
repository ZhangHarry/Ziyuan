/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package tzuyu.core.main;


import java.util.List;

import faultLocalization.SuspiciousnessCalculator.SuspiciousnessCalculationAlgorithm;

import sav.common.core.utils.CollectionUtils;
import sav.commons.TestConfiguration;
import sav.commons.utils.ConfigUtils;
import tzuyu.core.main.context.AbstractApplicationContext;


/**
 * @author LLT
 *
 */
public class TestApplicationContext extends AbstractApplicationContext {
	private TestConfiguration testConfig;
	private SuspiciousnessCalculationAlgorithm suspiciousnessCalcul;
	private List<String> projectClasspath;

	public TestApplicationContext() {
		testConfig = TestConfiguration.getInstance();
		projectClasspath = CollectionUtils.listOf(testConfig.testTarget);
	}

	@Override
	public List<String> getProjectClasspath() {
		return projectClasspath; 
	}
	
	public void setProjectClasspath(List<String> projectClasspath) {
		this.projectClasspath = projectClasspath;
	}

	@Override
	protected String getTracerJarPath() {
		return ConfigUtils.getTracerLibPath();
	}

	@Override
	protected String getJavahome() {
		return ConfigUtils.getJavaHome();
	}

	@Override
	public SuspiciousnessCalculationAlgorithm getSuspiciousnessCalculationAlgorithm() {
		return suspiciousnessCalcul;
	}
	
	protected void setSuspiciousnessCalculationAlgorithm(
			SuspiciousnessCalculationAlgorithm algorithm) {
		this.suspiciousnessCalcul = algorithm;
	}
}

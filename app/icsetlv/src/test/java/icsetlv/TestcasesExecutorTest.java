/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package icsetlv;

import icsetlv.common.dto.BreakpointData;
import icsetlv.common.dto.BreakpointValue;
import icsetlv.variable.TestcasesExecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import sav.common.core.utils.CollectionUtils;
import sav.common.core.utils.JunitUtils;
import sav.commons.AbstractTest;
import sav.commons.TestConfiguration;
import sav.commons.testdata.calculator.Sum;
import sav.commons.testdata.calculator.SumTest;
import sav.strategies.dto.AppJavaClassPath;
import sav.strategies.dto.BreakPoint;
import sav.strategies.dto.BreakPoint.Variable;
import sav.strategies.dto.BreakPoint.Variable.VarScope;
import test.Sum1;
import testdata.testcasesexecutor.test1.TcExSum;
import testdata.testcasesexecutor.test1.TcExSumTest;

/**
 * @author LLT
 * 
 */
public class TestcasesExecutorTest extends AbstractTest {
	private TestcasesExecutor tcExecutor;
	private AppJavaClassPath appClasspath;
	
	@Before
	public void setup() {
		appClasspath = initAppClasspath();
		appClasspath.addClasspath(TestConfiguration.getTestTarget(ICSETLV));
		tcExecutor = new TestcasesExecutor(6);
	}

	@Test
	public void testExecute() throws Exception {
		// breakpoints
		List<BreakPoint> breakpoints = new ArrayList<BreakPoint>();
		String clazz = TcExSum.class.getName();
		BreakPoint bkp1 = new BreakPoint(clazz, null, 32);
		bkp1.addVars(new Variable("a"));
		bkp1.addVars(new Variable("a", "a", VarScope.THIS));
		bkp1.addVars(new Variable("innerClass", "innerClass.b"));
		bkp1.addVars(new Variable("innerClass", "innerClass.a"));
		bkp1.addVars(new Variable("innerClass", "innerClass.inner.b"));
		breakpoints.add(bkp1);
		List<String> tests = JunitUtils.extractTestMethods(CollectionUtils
				.listOf(TcExSumTest.class.getName()));
		tcExecutor.setup(appClasspath, tests);
		tcExecutor.run(breakpoints);
		List<BreakpointData> result = tcExecutor.getResult();
		System.out.println(result);
	}
	
	@Test
	public void getPropertyValueOfNullObj() throws Exception {
		// breakpoints
		List<BreakPoint> breakpoints = new ArrayList<BreakPoint>();
		String clazz = TcExSum.class.getName();
		BreakPoint bkp1 = new BreakPoint(clazz, null, 32);
		bkp1.addVars(new Variable("innerClass", "innerClass.inner.inner.b"));
		breakpoints.add(bkp1);
		List<String> tests = JunitUtils.extractTestMethods(CollectionUtils
				.listOf(TcExSumTest.class.getName()));
		tcExecutor.setup(appClasspath, tests);
		tcExecutor.run(breakpoints);
		List<BreakpointData> result = tcExecutor.getResult();
		System.out.println(result);
	}
	
	@Test
	public void getValueOfNonExistedProperty() throws Exception {
		// breakpoints
		List<BreakPoint> breakpoints = new ArrayList<BreakPoint>();
		String clazz = TcExSum.class.getName();
		BreakPoint bkp1 = new BreakPoint(clazz, null, 32);
		bkp1.addVars(new Variable("innerClass", "innerClass.aaa"));
		breakpoints.add(bkp1);
		List<String> tests = JunitUtils.extractTestMethods(CollectionUtils
				.listOf(TcExSumTest.class.getName()));
		tcExecutor.setup(appClasspath, tests);
		tcExecutor.run(breakpoints);
		List<BreakpointData> result = tcExecutor.getResult();
		Assert.assertEquals(1, result.size());
		BreakpointData bkpData = result.get(0);
		List<BreakpointValue> bkpVal = new ArrayList<BreakpointValue>(bkpData.getPassValues());
		bkpVal.addAll(bkpData.getFailValues());
		Assert.assertTrue(CollectionUtils.isEmpty(bkpVal.get(0).getChildren()));
	}
	
	@Test
	public void debugWithManyTestcases() throws Exception {
		List<String> tests = JunitUtils.extractTestMethods(Arrays.asList(
				SumTest.class.getName(), Sum1.class.getName()));
		tcExecutor.setup(appClasspath, tests);
		List<BreakPoint> breakpoints = new ArrayList<BreakPoint>();
		String clazz = Sum.class.getName();
		breakpoints.add(new BreakPoint(clazz, 23, new Variable("a")));
		breakpoints.add(new BreakPoint(clazz, 24, new Variable("a")));
		tcExecutor.run(breakpoints);
	}
}

/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package learntest.core;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cfgcoverage.jacoco.analysis.data.CfgCoverage;
import gentest.junit.TestsPrinter.PrintOption;
import icsetlv.common.dto.BreakpointValue;
import icsetlv.common.utils.BreakpointDataUtils;
import jdart.model.TestInput;
import learntest.core.LearntestParamsUtils.GenTestPackage;
import learntest.core.commons.utils.VarSolutionUtils;
import learntest.core.gentest.GentestParams;
import learntest.core.gentest.GentestResult;
import learntest.core.jdart.JDartRunner;
import learntest.core.jdart.JdartTestInputUtils;
import sav.common.core.SavException;
import sav.common.core.utils.CollectionUtils;
import sav.common.core.utils.TextFormatUtils;
import sav.settings.SAVTimer;
import sav.strategies.dto.AppJavaClassPath;
import sav.strategies.dto.execute.value.ExecVar;

/**
 * @author LLT
 *
 */
public class JDartLearntest extends LearnTest {
	private static Logger log = LoggerFactory.getLogger(JDartLearntest.class);
	
	public JDartLearntest(AppJavaClassPath appClasspath) {
		super(appClasspath);
	}
	
	@Override
	protected void prepareInitTestcase(LearnTestParams params) throws SavException {
		List<TestInput> inputs = generateTestAndRunJDart(params);
		if (CollectionUtils.isEmpty(inputs)) {
			log.info("jdart result: {}", TextFormatUtils.printListSeparateWithNewLine(inputs));
			return;
		} else{
			log.info("jdart result (print 100 result at most):");
			for (int i = 0; i < inputs.size() && i < 100; i++) {
				log.info("input: {}", inputs.get(i).toString());
			}
		}
		init(params);
		List<BreakpointValue> bkpVals = JdartTestInputUtils.toBreakpointValue(inputs,
				params.getTargetMethod().getMethodFullName());
		List<ExecVar> vars = BreakpointDataUtils.collectAllVars(bkpVals);
		List<double[]> solutions = VarSolutionUtils.buildSolutions(bkpVals, vars);
		GentestResult testResult = mediator.genTestAndCompile(solutions, vars, PrintOption.APPEND);
		params.getInitialTests().addJunitClass(testResult, appClasspath.getClassLoader());
	}
	
	public RunTimeInfo jdart(LearnTestParams params) {
		SAVTimer.startCount();
		try {
			init(params);
			prepareInitTestcase(params);
			CfgCoverage cfgCoverage = runCfgCoverage(params.getTargetMethod(), params.getInitialTests().getJunitClasses());
			return getRuntimeInfo(cfgCoverage, params.isLearnByPrecond());
		} catch (Exception e) {
			log.debug(e.getMessage());
			return null;
		}
	}

	public List<TestInput> generateTestAndRunJDart(LearnTestParams params) throws SavException {
		GentestParams gentestParams = LearntestParamsUtils.createGentestParams(appClasspath, params, GenTestPackage.INIT);
		/* generate testcase and jdart entry */
		gentestParams.setGenerateMainClass(true);
		randomGenerateInitTestWithBestEffort(params, gentestParams);
		JDartRunner jdartRunner = new JDartRunner(appClasspath);
		return jdartRunner.runJDart(params);
	}
}

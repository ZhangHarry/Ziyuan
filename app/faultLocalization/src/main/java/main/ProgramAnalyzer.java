/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package main;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import sav.common.core.Logger;
import sav.common.core.utils.BreakpointUtils;
import sav.common.core.utils.CollectionUtils;
import sav.common.core.utils.JunitUtils;
import sav.common.core.utils.StringUtils;
import sav.strategies.IApplicationContext;
import sav.strategies.codecoverage.ICodeCoverage;
import sav.strategies.dto.BreakPoint;
import sav.strategies.junit.JunitResult;
import sav.strategies.junit.JunitRunner;
import sav.strategies.junit.JunitRunnerParameters;
import sav.strategies.slicing.ISlicer;
import faultLocalization.CoverageReport;
import faultLocalization.FaultLocalizationReport;
import faultLocalization.SuspiciousnessCalculator.SuspiciousnessCalculationAlgorithm;

/**
 * @author LLT
 * 
 */
public class ProgramAnalyzer {
	private Logger<?> log = Logger.getDefaultLogger();
	private ISlicer slicer;
	private ICodeCoverage codeCoverageTool;
	private boolean useSlicer = true; // Use slicer by default

	public ProgramAnalyzer(IApplicationContext appContext) {
		slicer = appContext.getSlicer();
		codeCoverageTool = appContext.getCodeCoverageTool();
	}

	public FaultLocalizationReport analyse(List<String> testingClasseNames,
			List<String> junitClassNames) throws Exception {
		return analyse(testingClasseNames, junitClassNames,
				SuspiciousnessCalculationAlgorithm.TARANTULA);
	}
	
	public FaultLocalizationReport analyseSlicingFirst(
			List<String> analyzedClasses, List<String> analyzedPackages,
			List<String> junitClassNames,
			SuspiciousnessCalculationAlgorithm algorithm) throws Exception {
		List<String> testMethods = JunitUtils.extractTestMethods(junitClassNames);
		/* do slicing first, but we must run testcases first, and only slice the fail testcases */
		JunitRunnerParameters params = new JunitRunnerParameters();
		params.setClassMethods(testMethods);
		params.setTestingPkgs(analyzedPackages);
		JunitResult jresult = JunitRunner.runTestcases(params);
		// slice
		Set<BreakPoint> traces = jresult.getFailureTraces();
		/* do slicing */
		if (log.isDebug()) {
			log.debug("failureTraces=", BreakpointUtils.getPrintStr(traces));
		}
		slicer.setFiltering(analyzedClasses, analyzedPackages);
		List<BreakPoint> causeTraces = slicer.slice(
				new ArrayList<BreakPoint>(jresult.getFailureTraces()),
				JunitUtils.toClassMethodStrs(jresult.getFailTests()));
		if (log.isDebug()) {
			log.debug("causeTraces=", BreakpointUtils.getPrintStr(causeTraces));
		}
		traces.addAll(causeTraces);
		// coverage
		FaultLocalizationReport report = new FaultLocalizationReport();
		CoverageReport result = new CoverageReport();
		List<String> testingClasses = BreakpointUtils.extractClasses(traces);
		if (log.isDebug()) {
			log.debug("Analyzing classes: ");
			log.debug(StringUtils.join(testingClasses, "\n"));
		}
		codeCoverageTool.run(result, testingClasses, junitClassNames);
		report.setCoverageReport(result);
		if (useSlicer) {
			report.setLineCoverageInfos(result.tarantula(new ArrayList<BreakPoint>(traces), algorithm));
		} else {
			report.setLineCoverageInfos(result.tarantula(new ArrayList<BreakPoint>(), algorithm));
		}
		return report;
	}

	public FaultLocalizationReport analyse(List<String> testingClasses,
			List<String> junitClassNames, SuspiciousnessCalculationAlgorithm algorithm)
			throws Exception {
		log.info("Start analyzing..")
			.info("testingClasses=", testingClasses)
			.info("junitClassNames=", junitClassNames)
			.info("algorithm=", algorithm)
			.info("useSlicer=", useSlicer);
		FaultLocalizationReport report = new FaultLocalizationReport();
		CoverageReport result = new CoverageReport();
		codeCoverageTool.run(result, testingClasses, junitClassNames);
		report.setCoverageReport(result);
		if (useSlicer) {
			/* do slicing */
			List<BreakPoint> traces = result.getFailureTraces();
			if (log.isDebug()) {
				log.debug("failureTraces=", BreakpointUtils.getPrintStr(traces));
			}
			slicer.setFiltering(testingClasses, null);
			List<BreakPoint> causeTraces = slicer.slice(result.getFailureTraces(),
					JunitUtils.toClassMethodStrs(result.getFailTests()));
			if (log.isDebug()) {
				log.debug("causeTraces=", BreakpointUtils.getPrintStr(causeTraces));
			}
			for (BreakPoint bkp : causeTraces) {
				CollectionUtils.addIfNotNullNotExist(traces, bkp);
			}
			report.setLineCoverageInfos(result.tarantula(traces, algorithm));
		} else {
			report.setLineCoverageInfos(result.tarantula(new ArrayList<BreakPoint>(), algorithm));
		}
		return report;
	}

	public void setUseSlicer(boolean useSlicer) {
		this.useSlicer = useSlicer;
	}

}

/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package tzuyu.core.main;

import icsetlv.Engine.Result;
import icsetlv.common.exception.IcsetlvException;
import icsetlv.variable.VariableNameCollector;
import japa.parser.ast.CompilationUnit;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import main.FaultLocalization;

import org.apache.commons.collections.CollectionUtils;

import sav.common.core.Logger;
import sav.common.core.Pair;
import sav.common.core.SavException;
import sav.common.core.utils.BreakpointUtils;
import sav.common.core.utils.ClassUtils;
import sav.strategies.IApplicationContext;
import sav.strategies.dto.BreakPoint;
import sav.strategies.dto.ClassLocation;
import sav.strategies.mutanbug.DebugLineInsertionResult;
import tzuyu.core.inject.ApplicationData;
import tzuyu.core.machinelearning.LearnInvariants;
import tzuyu.core.mutantbug.MutanBug;
import tzuyu.core.mutantbug.Recompiler;
import faultLocalization.FaultLocalizationReport;
import gentest.builder.FixTraceGentestBuilder;
import gentest.core.data.Sequence;
import gentest.junit.FileCompilationUnitPrinter;
import gentest.junit.ICompilationUnitPrinter;
import gentest.junit.TestsPrinter;


/**
 * @author LLT
 * TODO LLT: to refactor TzuyuCore or something?
 * this class is temporary created for another faultLocate without testcase generation part,
 * used to run {@link FaultLocatePackageTest}
 * 
 */
public class TzuyuCore2 {
	private static final Logger<?> LOGGER = Logger.getDefaultLogger();
	private IApplicationContext appContext;
	private ApplicationData appData;
	private int numberOfTestCases = 100;
	private boolean enableGentest = true;
	private int rankToExamine = Integer.MAX_VALUE;
	
	public TzuyuCore2(IApplicationContext appContext, ApplicationData appData) {
		this.appContext = appContext;
		this.appData = appData;
	}

	public void faultLocate(List<String> testingClassNames,
			List<String> testingPackages, List<String> junitClassNames,
			boolean useSlicer)
			throws Exception {
		FaultLocalizationReport report = computeSuspiciousness(
				testingClassNames, testingPackages, junitClassNames, useSlicer);
//		mutation(report, junitClassNames);
		machineLearning(report, junitClassNames);
	}

	private FaultLocalizationReport computeSuspiciousness(List<String> testingClassName,
			List<String> testingPackages, List<String> junitClassNames,
			boolean useSlicer) throws Exception {
		LOGGER.info("Running " + appData.getSuspiciousCalculAlgo());
		
		final FaultLocalization analyzer = new FaultLocalization(appContext);
		analyzer.setUseSlicer(useSlicer);

		FaultLocalizationReport report;
		if (CollectionUtils.isEmpty(testingPackages)) {
			report = analyzer.analyse(testingClassName, junitClassNames,
					appData.getSuspiciousCalculAlgo());
		} else {
			report = analyzer.analyseSlicingFirst(testingClassName, testingPackages,
					junitClassNames, appData.getSuspiciousCalculAlgo());
		}
		LOGGER.info(report);
		return report;
	}

	private void mutation(FaultLocalizationReport report,
			List<String> junitClassNames) throws Exception {
		LOGGER.info("Running Mutation");
		MutanBug mutanbug = new MutanBug();
		mutanbug.setAppData(appData);
		mutanbug.setMutator(appContext.getMutator());
		mutanbug.mutateAndRunTests(report, rankToExamine, junitClassNames);
		LOGGER.info(report);
	}
	
	private void machineLearning(
			FaultLocalizationReport report,
			List<String> junitClassNames) throws ClassNotFoundException,
			SavException, IcsetlvException, Exception {
		
		LOGGER.info("Running Machine Learning");
//		if (enableGentest && methodName != null && verificationMethod != null) {
//			while (true) {
//				try {
//					List<String> randomTests = generateNewTests(testingClassName, methodName, verificationMethod);
//					junitClassNames.addAll(randomTests);
//					break;
//				} catch (Throwable exception) {
//
//				}
//			}
//		}
		
		List<ClassLocation> suspectLocations = report.getFirstRanksLocation(rankToExamine);
		List<BreakPoint> breakpoints = BreakpointUtils.toBreakpoints(suspectLocations);
		
		//compute variables appearing in each breakpoint
		VariableNameCollector nameCollector = new VariableNameCollector(appData.getAppSrc());
		nameCollector.updateVariables(breakpoints);
		MutanBug mutanbug = new MutanBug();
		List<BreakPoint> newBreakpoints = getNextLineToAddBreakpoint(mutanbug, breakpoints);
		Map<BreakPoint, BreakPoint> newLineToOldLine = buildNewToOriginalMap(
				breakpoints, newBreakpoints);
		
		if (CollectionUtils.isEmpty(suspectLocations)) {
			LOGGER.warn("Did not find any place to add break point. SVM will not run.");
		} else {
			LearnInvariants learnInvariant = new LearnInvariants(appData.getVmConfig());
			List<Result> invariants = learnInvariant.learn(newBreakpoints, junitClassNames, appData.getAppSrc());
			
			LOGGER.info("-----------------------------------------------------------------");
			LOGGER.info("SUMMARY INVARIANTS LEARNED:");
			LOGGER.info("-----------------------------------------------------------------");
			for(int i = 0; i < invariants.size(); i++){
				BreakPoint newBreakpoint = invariants.get(i).getBreakPoint();
				BreakPoint originalBreakpoint = newLineToOldLine.get(newBreakpoint);
				
				LOGGER.info(originalBreakpoint);
				LOGGER.info(invariants.get(i));
			}
		}
		mutanbug.restoreFiles();
	}
	
	private List<String> generateNewTests(String testingClassName, String methodName, String verificationMethod)
			throws ClassNotFoundException, SavException {
		Class<?> targetClass = Class.forName(testingClassName);
		
		FixTraceGentestBuilder builder = new FixTraceGentestBuilder(numberOfTestCases );
		
		String methodAlias = "methodName";
		builder.forClass(targetClass).method(methodName, methodAlias)
					.evaluationMethod(Class.forName(testingClassName), verificationMethod,
							methodAlias).paramAutofill();
		Pair<List<Sequence>, List<Sequence>> testcases = builder.generate();
		final FileCompilationUnitPrinter cuPrinter = new FileCompilationUnitPrinter(
				appData.getAppSrc());
		final List<String> junitClassNames = new ArrayList<String>();
		TestsPrinter printer = new TestsPrinter("test", null, "test",
				targetClass.getSimpleName(), new ICompilationUnitPrinter() {
					
					@Override
					public void print(List<CompilationUnit> compilationUnits) {
						for (CompilationUnit cu : compilationUnits) {
							junitClassNames.add(ClassUtils.getCanonicalName(cu
									.getPackage().getName().getName(), cu
									.getTypes().get(0).getName()));
						}
						cuPrinter.print(compilationUnits);
					}
				});
		printer.printTests(testcases);
		List<File> generatedFiles = cuPrinter.getGeneratedFiles();
		Recompiler recompiler = new Recompiler(appData.getVmConfig());
		recompiler.recompileJFile(appData.getAppTestTarget(), generatedFiles);
		
		return junitClassNames;
	}

	private List<BreakPoint> getNextLineToAddBreakpoint(MutanBug mutanbug, 
			List<BreakPoint> suspectLocations) throws SavException {
		mutanbug.setAppData(appData);
		mutanbug.setMutator(appContext.getMutator());
		
		Map<String, DebugLineInsertionResult> mutationInfo = mutanbug.mutateForMachineLearning(suspectLocations);
		suspectLocations = getNewLocationAfterMutation(suspectLocations, mutationInfo);
		return suspectLocations;
	}
	
	private List<BreakPoint> getNewLocationAfterMutation(
			List<BreakPoint> suspectLocations,
			Map<String, DebugLineInsertionResult> mutationInfo) {
		List<BreakPoint> result = new ArrayList<BreakPoint>(suspectLocations.size());
		DebugLineInsertionResult lineInfo = mutationInfo.get(suspectLocations.get(0).getClassCanonicalName());
		Map<Integer, Integer> lineToNextLine = lineInfo.getOldNewLocMap();
		
		for(BreakPoint location: suspectLocations){
			Integer newLineNo = lineToNextLine.get(location.getLineNo());
			if (newLineNo != null) {
				BreakPoint newBkp = new BreakPoint(location.getClassCanonicalName(), newLineNo);
				newBkp.setVars(location.getVars());
				result.add(newBkp);
				
			} else {
				result.add(location);
			}
		}
		
		return result;
	}
	

	private Map<BreakPoint, BreakPoint> buildNewToOriginalMap(
			List<BreakPoint> breakpoints, List<BreakPoint> newBreakpoints) {
		Map<BreakPoint, BreakPoint> newLineToOldLine = new HashMap<BreakPoint, BreakPoint>();
		for(int i = 0; i < newBreakpoints.size(); i++){
			newLineToOldLine.put(newBreakpoints.get(i), breakpoints.get(i));
		}
		return newLineToOldLine;
	}
	
	public void setEnableGentest(boolean enableGentest) {
		this.enableGentest = enableGentest;
	}
	
	public void setRankToExamine(int rankToExamine) {
		this.rankToExamine = rankToExamine;
	}
}

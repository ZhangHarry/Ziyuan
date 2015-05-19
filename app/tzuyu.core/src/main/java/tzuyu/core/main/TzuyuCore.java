/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package tzuyu.core.main;

import icsetlv.variable.VariableNameCollector;
import japa.parser.ast.CompilationUnit;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import main.FaultLocalization;
import mutation.mutator.insertdebugline.DebugLineInsertionResult;

import org.apache.commons.collections.CollectionUtils;

import sav.common.core.Logger;
import sav.common.core.Pair;
import sav.common.core.SavException;
import sav.common.core.utils.BreakpointUtils;
import sav.common.core.utils.ClassUtils;
import sav.strategies.dto.BreakPoint;
import sav.strategies.dto.ClassLocation;
import tzuyu.core.inject.ApplicationData;
import tzuyu.core.machinelearning.LearnInvariants;
import tzuyu.core.main.context.AbstractApplicationContext;
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
 *
 */
public class TzuyuCore {
	private static final Logger<?> LOGGER = Logger.getDefaultLogger();
	private AbstractApplicationContext appContext;
	private ApplicationData appData;
	private int numberOfTestCases = 100;
	
	public TzuyuCore(AbstractApplicationContext appContext) {
		this.appContext = appContext;
		this.appData = appContext.getAppData();
	}

	public FaultLocalizationReport faultLocalization(List<String> testingClassNames,
			List<String> junitClassNames) throws Exception {
		return faultLocalization(testingClassNames, junitClassNames, true);
	}
	
	public FaultLocalizationReport faultLocalization(List<String> testingClassNames,
			List<String> junitClassNames, boolean useSlicer) throws Exception {
		FaultLocalization analyzer = new FaultLocalization(appContext);
		analyzer.setUseSlicer(useSlicer);
		FaultLocalizationReport report = analyzer.analyse(testingClassNames, junitClassNames,
				appData.getSuspiciousCalculAlgo());
		MutanBug mutanbug = new MutanBug();
		mutanbug.setAppData(appData);
		mutanbug.setMutator(appContext.getMutator());
		mutanbug.mutateAndRunTests(report, junitClassNames);
		return report;
	}
	
	public FaultLocalizationReport faultLocalization2(
			List<String> testingClassNames, List<String> testingPackages,
			List<String> junitClassNames, boolean useSlicer) throws Exception {
		FaultLocalization analyzer = new FaultLocalization(appContext);
		analyzer.setUseSlicer(useSlicer);
		FaultLocalizationReport report = analyzer.analyseSlicingFirst(
				testingClassNames, testingPackages, junitClassNames,
				appData.getSuspiciousCalculAlgo());
		MutanBug mutanbug = new MutanBug();
		mutanbug.setAppData(appData);
		mutanbug.setMutator(appContext.getMutator());
		mutanbug.mutateAndRunTests(report, junitClassNames);
		return report;
	}

	public FaultLocalizationReport doSpectrumAndMachineLearning(String testingClassName, String methodName, String verificationMethod,
			List<String> testingPackages, List<String> junitClassNames, boolean useSlicer)
			throws Exception {
		final FaultLocalization analyzer = new FaultLocalization(appContext);
		analyzer.setUseSlicer(useSlicer);

		FaultLocalizationReport report;
		if (CollectionUtils.isEmpty(testingPackages)) {
			report = analyzer.analyse(Arrays.asList(testingClassName), junitClassNames,
					appData.getSuspiciousCalculAlgo());
		} else {
			report = analyzer.analyseSlicingFirst(Arrays.asList(testingClassName), testingPackages,
					junitClassNames, appData.getSuspiciousCalculAlgo());
		}

		List<String> randomTests = generateNewTests(testingClassName, methodName, verificationMethod);
		junitClassNames.addAll(randomTests);
		
		List<ClassLocation> suspectLocations = report.getFirstRanksLocation(Integer.MAX_VALUE);
		List<BreakPoint> breakpoints = BreakpointUtils.toBreakpoints(suspectLocations);
		
		//compute variables appearing in each breakpoint
		VariableNameCollector nameCollector = new VariableNameCollector(Arrays.asList(appData.getAppSrc()));
		nameCollector.updateVariables(breakpoints);
		
		breakpoints = getNextLineToAddBreakpoint(breakpoints);
		
		if (CollectionUtils.isEmpty(suspectLocations)) {
			LOGGER.warn("Did not find any place to add break point. SVM will not run.");
		} else {
			LearnInvariants learnInvariant = new LearnInvariants(appData.getVmConfig());
			learnInvariant.learn(breakpoints, junitClassNames, appData.getAppSrc());
		}

		return report;
	}

	private List<BreakPoint> getNextLineToAddBreakpoint( 
			List<BreakPoint> suspectLocations) throws SavException {
		MutanBug mutanbug = new MutanBug();
		mutanbug.setAppData(appData);
		mutanbug.setMutator(appContext.getMutator());
		Map<String, DebugLineInsertionResult> mutationInfo = mutanbug.mutateForMachineLearning(suspectLocations);
		suspectLocations = getNewLocationAfterMutation(suspectLocations, mutationInfo);
		return suspectLocations;
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

	
}

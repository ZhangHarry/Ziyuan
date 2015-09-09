/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package codecoverage.jacoco.agent;

import java.io.File;
import java.io.IOException;
import java.util.List;

import sav.common.core.Logger;
import sav.common.core.ModuleEnum;
import sav.common.core.SavException;
import sav.common.core.utils.CollectionUtils;
import sav.common.core.utils.JunitUtils;
import sav.strategies.codecoverage.ICodeCoverage;
import sav.strategies.codecoverage.ICoverageReport;
import sav.strategies.dto.AppJavaClassPath;
import sav.strategies.junit.JunitRunner;
import sav.strategies.junit.JunitRunner.JunitRunnerProgramArgBuilder;
import sav.strategies.junit.SavJunitRunner;
import sav.strategies.vm.VMConfiguration;

/**
 * @author LLT
 *
 */
public class JaCoCoAgent implements ICodeCoverage {
	private Logger<?> log = Logger.getDefaultLogger();
	private ICoverageReport report;
	private ExecutionDataReporter reporter;
	private AppJavaClassPath appClasspath;
	
	public JaCoCoAgent(AppJavaClassPath appClasspath) {
		reporter = new ExecutionDataReporter(new String[] {
				appClasspath.getTarget(), appClasspath.getTestTarget() });
		this.appClasspath = appClasspath;
		report = null;
	}
	
	@Override
	public void run(ICoverageReport reporter, List<String> testingClassNames,
			List<String> junitClassNames) throws Exception {
		try { 
			this.report = reporter;
			run(testingClassNames, junitClassNames);
		} catch (IOException e) {
			throw new SavException(ModuleEnum.JVM, e);
		}
	}

	private void run(List<String> testingClassNames,
			List<String> junitClassNames) throws SavException, IOException,
			ClassNotFoundException {
		log.debug("RUNNING JACOCO..");
		if (CollectionUtils.isEmpty(testingClassNames)) {
			log.warn("TestingClassNames is empty!!");
		}
		testingClassNames = CollectionUtils.nullToEmpty(testingClassNames);
		String destfile = File.createTempFile("tzJacoco", ".exec").getAbsolutePath();
		String junitResultFile = File.createTempFile("tzJunitRes", ".txt")
				.getAbsolutePath();
		JaCoCoVmRunner vmRunner = new JaCoCoVmRunner()
					.setDestfile(destfile)
					.setAppend(true);
		vmRunner.setAnalyzedClassNames(testingClassNames);
		VMConfiguration vmConfig = SavJunitRunner.createVmConfig(appClasspath);
		vmConfig.setLaunchClass(JunitRunner.class.getName());
		reporter.setReport(report);
		List<String> testMethods = JunitUtils.extractTestMethods(junitClassNames);
		@SuppressWarnings("unchecked")
		List<String> allClassNames = CollectionUtils.join(testingClassNames,
				junitClassNames);
		if (log.isDebug()) {
			log.debug("Start vmRunner..")
				.debug("destfile=", destfile)
				.debug("junitResultFile=", junitResultFile)
				.debug("append=true")
				.debug("testMethods=", testMethods)
				.debug("allClassNames=", allClassNames)
				.debug("junitClassNames=", junitClassNames)
				.debug("testingClassNames=", testingClassNames);
		}
		for (String testMethod : testMethods) {
			/* define arguments for JunitRunner */
			vmRunner.getProgramArgs().clear();
		
			List<String> arguments = new JunitRunnerProgramArgBuilder()
					.method(testMethod).destinationFile(junitResultFile)
					.testClassNames(allClassNames).build();
			vmRunner.setProgramArgs(arguments);
			vmRunner.startAndWaitUntilStop(vmConfig);
		}
		
		reporter.report(destfile, junitResultFile, testingClassNames);
	}
	
	public ExecutionDataReporter getReporter() {
		return reporter;
	}
	
	public void setReporter(ExecutionDataReporter reporter) {
		this.reporter = reporter;
	}
}

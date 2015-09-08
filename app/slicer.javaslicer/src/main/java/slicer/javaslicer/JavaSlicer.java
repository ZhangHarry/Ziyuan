/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package slicer.javaslicer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import sav.common.core.Logger;
import sav.common.core.ModuleEnum;
import sav.common.core.SavException;
import sav.common.core.utils.BreakpointUtils;
import sav.common.core.utils.ClassUtils;
import sav.common.core.utils.CollectionUtils;
import sav.common.core.utils.StopTimer;
import sav.strategies.dto.BreakPoint;
import sav.strategies.junit.JunitRunner;
import sav.strategies.junit.JunitRunner.JunitRunnerProgramArgBuilder;
import sav.strategies.slicing.ISlicer;
import sav.strategies.vm.VMConfiguration;
import de.unisb.cs.st.javaslicer.slicing.Slicer;
import de.unisb.cs.st.javaslicer.slicing.SlicingCriterion;
import de.unisb.cs.st.javaslicer.traceResult.ThreadId;
import de.unisb.cs.st.javaslicer.traceResult.TraceResult;

/**
 * @author LLT
 * 
 */
public class JavaSlicer implements ISlicer {
	private Logger<?> log = Logger.getDefaultLogger();
	private JavaSlicerVmRunner vmRunner;
	private VMConfiguration vmConfig;
	private SliceBreakpointCollector sliceCollector;
	
	public JavaSlicer() {
		vmRunner = new JavaSlicerVmRunner();
	}

	public void setVmConfig(VMConfiguration vmConfig) {
		this.vmConfig = vmConfig;
	}
	
	/**
	 * @param vmConfig:
	 * requires: javaHome, classpaths
	 */
	public List<BreakPoint> slice(List<BreakPoint> bkps,
			List<String> junitClassMethods) throws SavException, IOException,
			InterruptedException, ClassNotFoundException {
		ensureData();
		if (CollectionUtils.isEmpty(bkps)) {
			log.warn("List of breakpoints to slice is empty");
			return new ArrayList<BreakPoint>();
		}
		StopTimer timer = new StopTimer("Slicing");
		
		timer.newPoint("create Trace file");
		String tempFileName = createTraceFile(junitClassMethods);
		
		/* do slicing */
		timer.newPoint("slice");
		List<BreakPoint> result = slice(tempFileName, new HashSet<BreakPoint>(bkps), timer, junitClassMethods);
		
		timer.logResults(log);
		return result;
	}

	private void ensureData() {
		if (sliceCollector == null) {
			sliceCollector = new SliceBreakpointCollector();
		}
	}

	/**
	 * we start jvm to execute this cmd:
	 * 
	 * java -javaagent:[/tracer.jar]=tracefile:[temfile.trace] 
	 * 		-cp [project classpath + path of tzuyuSlicer.jar] sav.strategies.junit.JunitRunner
	 * 		-methods [classMethods]
	 */
	public String createTraceFile(List<String> junitClassMethods)
			throws IOException, SavException, InterruptedException,
			ClassNotFoundException {
		log.info("Slicing-creating trace file...");
		File tempFile = File.createTempFile(getTraceFileName(), ".trace");
		String tempFileName = tempFile.getAbsolutePath();
		/* run program and create trace file */
		vmConfig.setLaunchClass(JunitRunner.class.getName());
		List<String> arguments = new JunitRunnerProgramArgBuilder().methods(
				junitClassMethods).build();
		vmRunner.setProgramArgs(arguments);
		/**/
		vmRunner.setTraceFilePath(tempFileName);
		vmRunner.startAndWaitUntilStop(vmConfig);

		return tempFileName;
	}
	
	public List<BreakPoint> slice(String traceFilePath, Collection<BreakPoint> bkps,
			StopTimer timer, List<String> junitClassMethods)
			throws InterruptedException, SavException {
		log.info("Slicing-slicing...");
		log.debug("traceFilePath=", traceFilePath);
		log.info("entry points=", BreakpointUtils.getPrintStr(bkps));
		File traceFile = new File(traceFilePath);
		TraceResult trace;
		try {
			timer.newPoint("read trace file");
			trace = TraceResult.readFrom(traceFile);
		} catch (IOException e) {
			e.printStackTrace();
			return new ArrayList<BreakPoint>();
		}

		List<SlicingCriterion> criteria = new ArrayList<SlicingCriterion>(bkps.size());
		for (BreakPoint bkp : bkps) {
			try {
				SlicingCriterion criterion = SlicingCriterion.parse(
						buildSlicingCriterionStr(bkp), trace.getReadClasses());
				criteria.add(criterion);
			} catch (IllegalArgumentException e) {
				String classMethodStr = ClassUtils.toClassMethodStr(bkp.getClassCanonicalName(), bkp.getMethodName());
				if (!junitClassMethods.contains(classMethodStr)) {
					throw e;
				} 
				// ignore
			}
		}

		List<ThreadId> threads = trace.getThreads();
		if (threads.size() == 0) {
			throw new SavException(ModuleEnum.SLICING, "trace.threads.size=0");
		}

		ThreadId tracing = null;
		for (ThreadId t : threads) {
			if ("main".equals(t.getThreadName())
					&& (tracing == null || t.getJavaThreadId() < tracing
							.getJavaThreadId()))
				tracing = t;
		}

		if (tracing == null) {
			log.error("Couldn't find the main thread.");
			return new ArrayList<BreakPoint>();
		}
		Slicer slicer = new Slicer(trace);
		slicer.addSliceVisitor(sliceCollector);
		slicer.process(tracing, criteria, true);
		log.debug("Read Slicing Result:");
		List<BreakPoint> dynamicSlice = sliceCollector.getDynamicSlice();
		if (log.isDebug()) {
			log.debug("slicing-result:");
			for (BreakPoint bkp : dynamicSlice) {
				log.debug(bkp.getId());
			}
		}
		return dynamicSlice;
	}

	private String buildSlicingCriterionStr(BreakPoint bkp) {
		return String.format("%s.%s:%s:*", bkp.getClassCanonicalName(),
				bkp.getMethodName(), bkp.getLineNo());
	}

	private String getTraceFileName() {
		return "javaSlicer";
	}

	@Override
	public void setFiltering(List<String> analyzedClasses,
			List<String> analyzedPackages) {
		if (CollectionUtils.isNotEmpty(analyzedPackages)) {
			sliceCollector = new ClassPkgFilterSliceCollector(analyzedClasses, analyzedPackages); 
		} else {
			sliceCollector = new ClassFilterSliceCollector(analyzedClasses);
		}
	}
}

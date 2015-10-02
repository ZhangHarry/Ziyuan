/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package slicer.javaslicer;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;

import sav.common.core.SavException;
import sav.common.core.utils.JunitUtils;
import sav.strategies.dto.AppJavaClassPath;
import sav.strategies.dto.BreakPoint;
import slicer.javaslicer.testdata.SliceCollectorTestdata;

/**
 * @author LLT
 * 
 */
public class SliceCollectorTest extends AbstractJavaSlicerTest {
	
	@Override
	public void setup() throws Exception {
		slicer = new JavaSlicer() {
			@Override
			public List<BreakPoint> slice(AppJavaClassPath appClassPath,
					List<BreakPoint> bkps, List<String> junitClassMethods)
					throws SavException, IOException, InterruptedException,
					ClassNotFoundException {
				String traceFilePath = "C:/Users/DELL50~1/AppData/Local/Temp/javaSlicer446553082999454376.trace";
				List<BreakPoint> result = sliceFromTraceFile(traceFilePath ,
						new HashSet<BreakPoint>(bkps), junitClassMethods);
				return result;
			}
		};
		appClasspath = initAppClasspath();
	}
	
	@Test
	public void testSampleProgram() throws Exception {
		String targetClass = SliceCollectorTestdata.class.getName();
		String testClass = SliceCollectorTestdata.class.getName();
//		BreakPoint bkp2 = new BreakPoint(testClass, "testSum", 58);
		BreakPoint bkp2 = new BreakPoint(testClass, "getSum", 44);
		List<BreakPoint> breakpoints = Arrays.asList(bkp2);
		analyzedClasses = Arrays.asList(targetClass);
		testClassMethods = JunitUtils.extractTestMethods(Arrays
				.asList(testClass));
		run(breakpoints);
	}
}

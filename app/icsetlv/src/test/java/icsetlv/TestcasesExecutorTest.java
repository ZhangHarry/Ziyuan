/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package icsetlv;

import icsetlv.common.dto.TcExecResult;
import icsetlv.common.exception.IcsetlvException;
import icsetlv.variable.TestcasesExecutor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import sav.common.core.SavException;
import sav.common.core.utils.CollectionUtils;
import sav.commons.AbstractTest;
import sav.strategies.dto.BreakPoint;
import sav.strategies.dto.BreakPoint.Variable;
import sav.strategies.vm.VMConfiguration;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.IncompatibleThreadStateException;

/**
 * @author LLT
 * 
 */
public class TestcasesExecutorTest extends AbstractTest {

	@Test
	public void testExecute() throws IOException, InterruptedException,
			IncompatibleThreadStateException, AbsentInformationException,
			IcsetlvException, SavException {
		VMConfiguration vmConfig = initVmConfig();

		// breakpoints
		List<BreakPoint> breakpoints = new ArrayList<BreakPoint>();
		BreakPoint bkp1 = new BreakPoint("testdata.slice.FindMax", "findMax",
				11);
		bkp1.addVars(new Variable("max"));
		bkp1.addVars(new Variable("arr"));
		breakpoints.add(bkp1);
		BreakPoint bkp2 = new BreakPoint("testdata.slice.FindMax", "findMax",
				14);
		bkp2.addVars(new Variable("max"));
		bkp2.addVars(new Variable("arr"));
		breakpoints.add(bkp2);

		TestcasesExecutor varExtr = new TestcasesExecutor(vmConfig, 6);
		TcExecResult extractedResult = varExtr
				.execute(
						CollectionUtils
								.listOf("testdata.slice.FindMaxCallerPassTest1"),
						CollectionUtils
								.listOf("testdata.slice.FindMaxCallerFailTest1"),
						breakpoints);
		print(extractedResult);
	}
}

/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package icsetlv;

import java.util.List;

import org.junit.Test;

import sav.common.core.utils.CollectionUtils;
import icsetlv.common.dto.BreakPoint;
import icsetlv.common.exception.IcsetlvException;
import icsetlv.slicer.SlicerInput;
import icsetlv.slicer.WalaSlicer;

/**
 * @author LLT
 *
 */
public class TryWala extends AbstractTest {
	
	@Test
	public void runTest() throws IcsetlvException {
		SlicerInput input = initSlicerInput();
//		input.setClassEntryPoints(CollectionUtils.listOf(
//				new String[]{"Ltestdata/SamplePrograms", "callMax()V"}));
		BreakPoint bkp = new BreakPoint("testdata.SamplePrograms", "max(I;I;I)I", 25);
		
		WalaSlicer slicer = new WalaSlicer(input);
		List<BreakPoint> result = slicer.slice(CollectionUtils.listOf(bkp));
		System.out.println(result);
	}
}

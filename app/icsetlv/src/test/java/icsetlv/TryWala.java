/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package icsetlv;

import icsetlv.common.dto.BreakPoint;
import icsetlv.slicer.SlicerInput;
import icsetlv.slicer.WalaSlicer;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import sav.common.core.utils.CollectionUtils;

/**
 * @author LLT
 *
 */
public class TryWala extends AbstractTest {
	
	@Test
	@Category(sg.edu.sutd.test.core.TzuyuTestCase.class)
	public void runTest() throws Exception {
		SlicerInput input = initSlicerInput();
//		input.setClassEntryPoints(CollectionUtils.listOf(
//				new String[]{"Ltestdata/SamplePrograms", "callMax()V"}));
		BreakPoint bkp = new BreakPoint("testdata.SamplePrograms", "max(I;I;I)I", 25);
		
		WalaSlicer slicer = new WalaSlicer(input);
		List<BreakPoint> result = slicer.slice(CollectionUtils.listOf(bkp), new ArrayList<String>());
		System.out.println(result);
	}
}

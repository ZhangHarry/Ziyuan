/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package tzuyu.core.main;

import org.junit.Before;
import org.junit.Test;

import icsetlv.variable.VarNameVisitor.VarNameCollectionMode;
import sav.commons.testdata.opensource.TestPackage;

/**
 * @author LLT
 *
 */
public class FaultLocatePackageTest extends AbstractTzPackageTest {
	protected TzuyuCore tzCore;
	protected FaultLocateParams params;
	
	@Before
	public void setup() {
		super.setup();
		tzCore = new TzuyuCore(context, appData);
		params = new FaultLocateParams();
		params.setMachineLearningEnable(true);
		params.setRankToExamine(3);
		params.setRunMutation(false);
		params.setUseSlicer(true);
		params.setValueRetrieveLevel(3);
		params.setVarNameCollectionMode(VarNameCollectionMode.FULL_NAME);
	}
	
	public void runFaultLocate(TestPackage testPkg) throws Exception {
		prepare(testPkg);
		params.setTestingClassNames(testingClassNames);
		params.setTestingPkgs(testingPackages);
		params.setJunitClassNames(junitClassNames);
		tzCore.faultLocate(params);
	}
	
	/**
	 * test part
	 */
	
	/**
	 * https://code.google.com/p/javaparser/issues/detail?id=46&colspec=ID%20Type%20Status%20Stars%20Summary
	 * ASTParserTokenManager: 2220
	 * & ASTParserTokenManager: 69
	 */
	@Test
	public void testjavaparser46() throws Exception {
		TestPackage testPkg = TestPackage.getPackage("javaparser", "46");
		params.setRankToExamine(3);
		params.setValueRetrieveLevel(2);
		params.setVarNameCollectionMode(VarNameCollectionMode.HIGHEST_LEVEL_VAR);
		runFaultLocate(testPkg);
	}
	
	/**
	 * https://code.google.com/p/javaparser/issues/detail?id=57&colspec=ID%20Type%20Status%20Stars%20Summary
	 * ASTParser.ClassOrInterfaceType:1810
	 * 
	 */
	@Test
	public void testjavaparser57() throws Exception {
		TestPackage testPkg = TestPackage.getPackage("javaparser", "57");
		params.setRankToExamine(2);
		params.setRunMutation(false);
		params.setMachineLearningEnable(true);
		runFaultLocate(testPkg);
	}
	
	@Test
	public void countLocJavaParser57() throws Exception {
		TestPackage testPkg = TestPackage.getPackage("javaparser", "57");
		params.setRankToExamine(2);
		params.setRunMutation(false);
		params.setMachineLearningEnable(true);
		runFaultLocate(testPkg);
	}
	
	@Test
	public void testjavaparser69() throws Exception {
		TestPackage testPkg = TestPackage.getPackage("javaparser", "69");
		runFaultLocate(testPkg);
	}
	
	@Test
	public void testjodatime90() throws Exception {
		TestPackage testPkg = TestPackage.getPackage("joda-time", "90");
		params.setRankToExamine(3);
		runFaultLocate(testPkg);
	}
	
	@Test
	public void testjodatime194() throws Exception {
		TestPackage testPkg = TestPackage.getPackage("joda-time", "194");
		params.setRankToExamine(7);
		runFaultLocate(testPkg);
	}
	
	/**
	 * https://github.com/JodaOrg/joda-time/issues/233
	 * fix: 
	 * https://github.com/JodaOrg/joda-time/commit/48b6ae85b02f41bec0fac7110ee47239c53eee9d
	 */
	@Test
	public void testjodatime233() throws Exception {
		TestPackage testPkg = TestPackage.getPackage("joda-time", "233");
		params.setRankToExamine(3);
		runFaultLocate(testPkg);
	}
	
	/**
	 * https://github.com/JodaOrg/joda-time/issues/227
	 * fix:
	 * https://github.com/JodaOrg/joda-time/commit/b95ebe240aa65d2d28deb84b76d8a7edacf922f8
	 * bug at BasicMonthOfYearDateTimeField.add:212
	 * int curMonth0 = partial.getValue(0) - 1;
	 * => int curMonth0 = values[0] - 1;
	 */
	@Test
	public void testjodatime227() throws Exception {
		TestPackage testPkg = TestPackage.getPackage("joda-time", "227");
		params.setRankToExamine(3);
		params.setValueRetrieveLevel(4);
		params.setUseSlicer(false);
		params.setRunMutation(false);
		params.setMachineLearningEnable(true);
		runFaultLocate(testPkg);
	}
	
	@Test
	public void testjodatime21() throws Exception {
		TestPackage testPkg = TestPackage.getPackage("joda-time", "21");
		params.setRankToExamine(3);
		params.setValueRetrieveLevel(3);
		params.setUseSlicer(false);
		params.setRunMutation(false);
		params.setMachineLearningEnable(true);
		runFaultLocate(testPkg);
	}
	
	@Test
	public void testjodatime77() throws Exception {
		TestPackage testPkg = TestPackage.getPackage("joda-time", "77");
		params.setRankToExamine(3);
		params.setValueRetrieveLevel(3);
		params.setUseSlicer(false);
		params.setRunMutation(false);
		params.setMachineLearningEnable(true);
		runFaultLocate(testPkg);
	}
	
	/**
	 * bug at MyersDiff.buildPath:137
	 * final int middle = (size + 1) / 2;
	 * => final int middle = size / 2;
	 */
	@Test
	public void testDiffUtils8() throws Exception {
		TestPackage testPkg = TestPackage.getPackage("java-diff-utils", "8");
		params.setRankToExamine(3);
		params.setRunMutation(false);
		params.setValueRetrieveLevel(2);
//		params.setGenTest(true);
		runFaultLocate(testPkg);
	}
	
	/**
	 * bug at DiffUtils.parseUnifiedDiff:137 (rev.25)
	 * add else block: rawChunk.add(new Object[] {" ", ""});
	 */
	@Test
	public void testDiffUtils10() throws Exception {
		TestPackage testPkg = TestPackage.getPackage("java-diff-utils", "10");
		params.setRankToExamine(3);
		params.setValueRetrieveLevel(2);
		params.setGroupLines(true);
//		params.setRunMutation(true);
		runFaultLocate(testPkg);
	}
	
	/**
	 * bug in DiffUtils.generateUnifiedDiff:192 (rev.25) 
	 * add condition !patch.getDeltas().isEmpty()
	 */
	@Test
	public void testDiffUtils12() throws Exception {
		TestPackage testPkg = TestPackage.getPackage("java-diff-utils", "12");
		params.setRankToExamine(3);
		runFaultLocate(testPkg);
	}
	
	/**
	 * not really a bug, this result is acceptable because of the idea of myers algorithm
	 */
	@Test
	public void testDiffUtils18() throws Exception {
		TestPackage testPkg = TestPackage.getPackage("java-diff-utils", "18");
		params.setRankToExamine(3);
		runFaultLocate(testPkg);
	}
	
	/**
	 * Parsing add-only parts of unified diffs generated by "diff -U 0 ..." fails
	 * llt: not sure if this is really a bug, it depends on the requirement. 
	 * */
	@Test
	public void testDiffUtils20() throws Exception {
		TestPackage testPkg = TestPackage.getPackage("java-diff-utils", "20");
		params.setRankToExamine(3);
		runFaultLocate(testPkg);
	}
	
	@Test
	public void testCommonsMath1196() throws Exception {
		TestPackage testPkg = TestPackage.getPackage("apache-commons-math", "1196");
		params.setRankToExamine(3);
		runFaultLocate(testPkg);
	}
	
	@Test
	public void testCommonsMath835() throws Exception {
		TestPackage testPkg = TestPackage.getPackage("apache-commons-math", "835");
		params.setRankToExamine(3);
		runFaultLocate(testPkg);
	}
	
	@Test
	public void testCommonsMath1127() throws Exception {
		TestPackage testPkg = TestPackage.getPackage("apache-commons-math", "1127");
		params.setRankToExamine(3);
		runFaultLocate(testPkg);
	}
	
	@Test
	public void testCommonsMath1005() throws Exception {
		TestPackage testPkg = TestPackage.getPackage("apache-commons-math", "1005");
		params.setRankToExamine(3);
		runFaultLocate(testPkg);
	}
	
	@Test
	public void testCommonsMath1141() throws Exception {
		TestPackage testPkg = TestPackage.getPackage("apache-commons-math", "1141");
		params.setRankToExamine(3);
		runFaultLocate(testPkg);
	}
	
	@Test
	public void testCommonsCli233() throws Exception {
		TestPackage testPkg = TestPackage.getPackage("commons-cli", "233-v1.2");
		params.setRankToExamine(3);
		runFaultLocate(testPkg);
	}
	
	/**
	 * bug at CSVParser.parseLine:265-267
	 * missing else:  inField = false;  
	 */
	@Test
	public void testOpenCsv102() throws Exception {
		TestPackage testPkg = TestPackage.getPackage("opencsv", "102");
		params.setRankToExamine(4);
		runFaultLocate(testPkg);
	}
	
	@Test
	public void testOpenCsv108() throws Exception {
		TestPackage testPkg = TestPackage.getPackage("opencsv", "108");
		params.setRankToExamine(5);
		params.setValueRetrieveLevel(2);
		runFaultLocate(testPkg);
	}
	
	@Test
	public void testOpenCsv106() throws Exception {
		TestPackage testPkg = TestPackage.getPackage("opencsv", "106");
		params.setRankToExamine(4);
		params.setVarNameCollectionMode(VarNameCollectionMode.HIGHEST_LEVEL_VAR);
		runFaultLocate(testPkg);
	}
}

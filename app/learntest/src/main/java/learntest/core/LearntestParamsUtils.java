/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package learntest.core;

import learntest.core.commons.LearntestConstants;
import learntest.core.commons.data.LearnTestApproach;
import learntest.core.commons.data.classinfo.TargetMethod;
import learntest.core.gentest.GentestParams;
import sav.common.core.utils.CollectionUtils;
import sav.common.core.utils.SignatureUtils;
import sav.common.core.utils.StringUtils;
import sav.strategies.dto.AppJavaClassPath;

/**
 * @author LLT
 *
 */
public class LearntestParamsUtils {
	private LearntestParamsUtils() {
	}
	
	public static String getTestPackage(LearnTestParams params, GenTestPackage phaseType) {
		String methodLc = params.getTargetMethod().getMethodName().toLowerCase();
		/* handle keyword cases */
		if (CollectionUtils.existIn(methodLc, "instanceof")) {
			methodLc = methodLc + "ziy";
		}
		return String.format(phaseType.format, params.getApproach().getName(),
				params.getTargetMethod().getTargetClazz().getClassSimpleName().toLowerCase(),
				methodLc);
	}

	public static GentestParams createGentestParams(AppJavaClassPath appClasspath, LearnTestParams learntestParams,
			GenTestPackage gentestPackage) {
		GentestParams params = new GentestParams();
		params.setMethodExecTimeout(LearntestConstants.GENTEST_METHOD_EXEC_TIMEOUT);
		TargetMethod targetMethod = learntestParams.getTargetMethod();
		params.setMethodSignature(
				SignatureUtils.createMethodNameSign(targetMethod.getMethodName(), targetMethod.getMethodSignature()));
		params.setTargetClassName(targetMethod.getClassName());
		params.setNumberOfTcs(learntestParams.getInitialTcTotal());
		params.setTestPerQuery(1);
		params.setTestSrcFolder(appClasspath.getTestSrc());
		params.setTestPkg(getTestPackage(learntestParams, gentestPackage));
		params.setTestClassPrefix(targetMethod.getTargetClazz().getClassSimpleName());
		params.setTestMethodPrefix("test");
		params.setExtractTestcaseSequenceMap(true);
		return params;
	}
	
	/* testdata.[approachName].{init/result}.[classSimpleName].[methodName]*/
	public static enum GenTestPackage {
//		INIT("testdata.%s.init.%s.%s"), 
//		RESULT("testdata.%s.result.%s.%s"),
//		MAIN("testdata.%s.main.%s.%s"),
		JDART("testdata.%s.jdart.%s.%s"),
		INIT("testdata.%s.%s.%s"), //("testdata.%s.init.%s.%s"), 
		RESULT("testdata.%s.%s.%s");//("testdata.%s.result.%s.%s");

		private String format;
		private GenTestPackage(String format) {
			this.format = format;
		}
		
		public static boolean isTestResultPackage(String packageName) {
			String[] frags = StringUtils.dotSplit(packageName);
			if (frags.length < 4) {
				return false;
			}
			if (!"testdata".equals(frags[0])) {
				return false;
			}
			try {
				LearnTestApproach.valueOf(frags[1]);
				return true;
			} catch (Exception ex) {
				return false;
			}
			
		}
	}
}

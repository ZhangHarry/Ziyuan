/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package sav.common.core;

import static sav.common.core.SystemVariables.SYS_SAV_JUNIT_JAR;
import sav.strategies.dto.AppJavaClassPath;
import sav.strategies.junit.SavJunitRunner;


/**
 * @author LLT
 *
 */
public class SystemVariablesUtils {
	private SystemVariablesUtils(){}
	
	public static String updateSavJunitJarPath(AppJavaClassPath appClasspath) {
		String jarPath = appClasspath.getPreferences().getString(SYS_SAV_JUNIT_JAR);
		if (jarPath == null) {
			jarPath = SavJunitRunner.extractToTemp().getAbsolutePath();
			appClasspath.getPreferences().set(SYS_SAV_JUNIT_JAR, jarPath);
		}
		return jarPath;
	}
	
	public static ClassLoader getClassLoadder(AppJavaClassPath appClasspath) {
		return appClasspath.getPreferences().get(SystemVariables.PROJECT_CLASSLOADER);
	}
}

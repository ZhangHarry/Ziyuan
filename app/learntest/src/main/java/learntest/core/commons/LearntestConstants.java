/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package learntest.core.commons;

/**
 * @author LLT
 *
 */
public class LearntestConstants {
	private LearntestConstants(){}

	public static final String EXCLUSIVE_METHOD_FILE_NAME = "exclusive_methods.txt"; // successful
	public static final String SKIP_METHOD_FILE_NAME = "skip.txt";
	public static final String CHECK_METHOD_FILE_NAME = "check.txt";
	public static final long GENTEST_METHOD_EXEC_TIMEOUT = 200l; //ms
	
	/* project settings */
	// folder which contains output files: log, report
	public static final String REPORT_FOLDER = "learntest";
	public static final String LOG_FILE_NAME = "learntest-eclipse.log";
	public static final String LOG4J_PROPERTIES = "product_log4j"; 
//	public static final String LOG4J_PROPERTIES = "learntest_log4j";
	
}

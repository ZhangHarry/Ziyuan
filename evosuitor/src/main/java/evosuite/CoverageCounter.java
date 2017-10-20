/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package evosuite;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import cfgcoverage.jacoco.CfgJaCoCo;
import cfgcoverage.jacoco.analysis.data.CfgCoverage;
import evosuite.EvosuiteRunner.EvosuiteResult;
import sav.common.core.SavException;
import sav.common.core.SavRtException;
import sav.common.core.utils.ClassUtils;
import sav.common.core.utils.CollectionUtils;
import sav.common.core.utils.FileUtils;
import sav.common.core.utils.JavaFileUtils;
import sav.common.core.utils.TextFormatUtils;
import sav.strategies.dto.AppJavaClassPath;
import sav.strategies.vm.JavaCompiler;
import sav.strategies.vm.VMConfiguration;

/**
 * @author LLT
 *
 */
public class CoverageCounter {
	private JavaCompiler jCompiler;
	private AppJavaClassPath appClasspath;
	
	public CoverageCounter(AppJavaClassPath appClassPath) {
		
		jCompiler = new JavaCompiler(new VMConfiguration(appClassPath));
		this.appClasspath = appClassPath;
	}
	
	public CfgCoverage calculateCoverage(Configuration config, String newPkg, EvosuiteResult result) {
		try {
			System.out.println();
			CfgJaCoCo cfgJacoco = new CfgJaCoCo(appClasspath);
			FilesInfo info = lookupJunitClasses(config, newPkg);
			Map<String, CfgCoverage> cfgCoverage = cfgJacoco.runBySimpleRunner(extractTargetMethod(result),
					extractTargetClass(result), info.junitClasses);
			System.out.println(TextFormatUtils.printMap(cfgCoverage));
			return cfgCoverage.values().iterator().next();
		} catch (Exception e) {
			e.printStackTrace();
			throw new SavRtException(e);
		}
	}
	
	private List<String> extractTargetClass(EvosuiteResult result) {
		return Arrays.asList(result.targetClass);
	}

	private List<String> extractTargetMethod(EvosuiteResult result) {
		return Arrays.asList(result.targetMethod);
	}

	public FilesInfo lookupJunitClasses(Configuration config, String newPkg)
			throws FileNotFoundException, IOException, SavException {
		String folder = config.getEvoBaseDir() + "/evosuite-tests";
		Collection<?> files = getAllJavaFiles(folder);
		FilesInfo info = new FilesInfo();
		for (Object obj : files) {
			File file = (File) obj;
			if (!file.getName().contains("ESTest_scaffolding")) {
				info.junitClasses.add(ClassUtils.getCanonicalName(newPkg, file.getName().replace(".java", "")));
			}
			info.allFiles.add(file);
		}
		/* copy all files to source folder */
		String pkgDecl = new StringBuilder("package ").append(newPkg).append(";").toString();
		String junitNewFolder = JavaFileUtils.getClassFolder(config.getEvosuitSrcFolder(), newPkg);
		FileUtils.deleteAllFiles(junitNewFolder);
		FileUtils.copyFiles(info.allFiles, junitNewFolder);
		/* update new files */
		info.allFiles = CollectionUtils.toArrayList((File[])(new File(junitNewFolder)).listFiles());
		/* modify package */
		for (File file : info.allFiles) {
			modifyPackage(file, pkgDecl);
		}
		/* compile */
		jCompiler.compile(appClasspath.getTestTarget(), getAllJavaFiles(junitNewFolder));
		return info;
	}

	private Collection<File> getAllJavaFiles(String folder) {
		return org.apache.commons.io.FileUtils.listFiles(new File(folder), new String[] { "java" }, true);
	}
	
	private void modifyPackage(File file, String newPkgDecl) {
		try {
			List<String> lines = org.apache.commons.io.FileUtils.readLines(file);
			int i = 0;
			String newPkgLine = null;
			for (; i < lines.size(); i++) {
				String line = lines.get(i);
				if (line.startsWith("package ")) {
					int endDeclIdx = line.indexOf(";");
					if (endDeclIdx == (line.length() - 1)) {
						newPkgLine = newPkgDecl;
					} else {
						newPkgLine = new StringBuilder(newPkgDecl).append(line.substring(endDeclIdx + 1, line.length() - 1)).toString();
					}
					break;
				}
			}
			lines.set(i, newPkgLine);
			org.apache.commons.io.FileUtils.writeLines(file, lines);
		} catch (IOException e) {
			e.printStackTrace();
			throw new SavRtException(e);
		}
	}

	private static class FilesInfo {
		List<File> allFiles = new ArrayList<File>();
		List<String> junitClasses = new ArrayList<String>();
	}
}

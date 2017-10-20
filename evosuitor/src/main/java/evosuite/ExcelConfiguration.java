/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package evosuite;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import evosuite.EvosuiteRunner.EvosuiteResult;
import sav.common.core.utils.StringUtils;
import sav.strategies.dto.AppJavaClassPath;

/**
 * @author LLT
 *
 */
public class ExcelConfiguration extends Configuration {
	private List<ExportData> exportData;
	private Map<String, ExportData> methodDataMap;
	private EvosuiteExcelHandler excelHandler;
	private boolean rerun; // whether or not to rerun if evosuite's coverage is already existed.

	public ExcelConfiguration(AppJavaClassPath appClasspath, String excelFilePath) throws Exception {
		super(appClasspath);
		excelHandler = new EvosuiteExcelHandler(excelFilePath);
		exportData = excelHandler.readData();
	}
	
	@Override
	public List<String> loadValidMethods() {
		List<String> methods = new ArrayList<String>(exportData.size());
		methodDataMap = new HashMap<>();
		for (ExportData data : exportData) {
			if (!rerun && data.isEvoCvgExisted()) {
				continue;
			}
			String methodId = StringUtils.dotJoin(data.getMethodName(), data.getStartLine());
			methods.add(methodId);
			methodDataMap.put(methodId, data);
		}
		return methods;
	}

	@Override
	public void updateResult(String classMethod, int line, EvosuiteResult result) {
		ExportData data = methodDataMap.get(StringUtils.dotJoin(classMethod, line));
		data.setEvoResult(result);
		try {
			excelHandler.updateData(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean isRerun() {
		return rerun;
	}

	public void setRerun(boolean rerun) {
		this.rerun = rerun;
	}
	
}

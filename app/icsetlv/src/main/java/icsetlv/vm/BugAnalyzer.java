/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package icsetlv.vm;

import icsetlv.common.dto.BreakPoint;
import icsetlv.common.dto.VariablesExtractorResult;
import icsetlv.common.dto.VariablesExtractorResult.BreakpointResult;
import icsetlv.common.exception.IcsetlvException;
import icsetlv.iface.IBugAnalyzer;
import icsetlv.iface.IManager;
import icsetlv.svm.DatasetBuilder;
import icsetlv.svm.LibSVM;

import java.util.ArrayList;
import java.util.List;

import sav.common.core.utils.CollectionUtils;

/**
 * @author Jingyi
 * 
 */
public class BugAnalyzer implements IBugAnalyzer {
	private IManager manager;
	private List<String> passTestcases;
	private List<String> failTestcases;

	public BugAnalyzer(IManager manager, List<String> passTestcases,
			List<String> failTestcases) {
		this.manager = manager;
		this.passTestcases = passTestcases;
		this.failTestcases = failTestcases;
	}

	@Override
	public List<BreakPoint> analyze(List<BreakPoint> initBkps)
			throws IcsetlvException {
		List<BreakPoint> rootCause = new ArrayList<BreakPoint>();
		List<BreakPoint> allBkps = initBkps;
		boolean firstRound = true;
		while (!allBkps.isEmpty()) {
			List<BreakPoint> executeBkps = next(allBkps);
			VariablesExtractorResult result = manager.getVariableExtractor()
					.execute(passTestcases, failTestcases, executeBkps);
			for (BreakpointResult bkpRes : result.getResult()) {
				if (isTheRootCause(bkpRes)) {
					rootCause.add(bkpRes.getBreakpoint());
				} else if (firstRound){
					List<BreakPoint> sliceResult = manager.getSlicer().slice(executeBkps);
					allBkps.addAll(sliceResult);
					firstRound = false;
				}
			}
		}
		return rootCause;
	}
	
	private List<BreakPoint> next(List<BreakPoint> allBkps) {
		return CollectionUtils.listOf(allBkps.remove(0));
	}

	@Override
	public boolean isTheRootCause(BreakpointResult bkp) {
		if (bkp.getFailValues().isEmpty() || bkp.getPassValues().isEmpty()) {
			return false;
		}
		DatasetBuilder db = new DatasetBuilder(bkp);
		LibSVM svmer = new LibSVM();
		svmer.buildClassifier(db.buildDataset());
		Metric metric = new Metric(svmer.modelAccuracy());
		return bugFoundOrNot(metric);
	}

	/*
	 * Metric for assertion generation using svm For now, we use classification
	 * accuracy and set a hard threshold
	 */
	private class Metric {
		double modelAccuracy;

		public Metric(double macc) {
			this.modelAccuracy = macc;
		}
	}

	/*
	 * Check if we can still generate assertions. Set the threshold for
	 * classification accuracy
	 */
	private boolean bugFoundOrNot(Metric metric) {
		if (metric.modelAccuracy > 0.7) {
			return false;
		}
		return true;
	}
}

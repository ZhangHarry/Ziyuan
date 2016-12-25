/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package tzuyu.core.main;

import icsetlv.common.dto.BkpInvariantResult;
import sav.common.core.formula.Formula;
import sav.common.core.utils.ObjectUtils;
import sav.strategies.dto.BreakPoint;

/**
 * @author khanh
 *
 */
public class BugLocalizationLine {
	private int orgLine;
	private BreakPoint breakpoint;
	private double suspiciousness;
	private BkpInvariantResult learningResult;
	
	public BugLocalizationLine(int orgLine, BreakPoint breakPoint,
			double suspiciousness, BkpInvariantResult invariant) {
		this.orgLine = orgLine;
		this.breakpoint = breakPoint;
		this.suspiciousness = suspiciousness;
		this.learningResult = invariant;
	}

	public BreakPoint getBreakpoint() {
		return breakpoint;
	}

	public double getSuspiciousness() {
		return suspiciousness;
	}

	public int compare(BugLocalizationLine o) {
		int suspiciousnessComp = Double.compare(this.getSuspiciousness(), o.getSuspiciousness());
		if (this.getLearnedLogic() == null) {
			if (o.getLearnedLogic() != null) {
				return -1;
			}
		} else {
			if (o.getLearnedLogic() == null) {
				return 1;
			}
		}
		if (suspiciousnessComp == 0
				&& breakpoint.getClassCanonicalName().equals(
						o.breakpoint.getClassCanonicalName())) {
			return ObjectUtils.compare(o.orgLine, this.orgLine);
		}
		return suspiciousnessComp;
	}
	
	public Formula getLearnedLogic() {
		return learningResult.getLearnedLogic();
	}
	
	@Override
	public String toString() {
		final StringBuilder str = new StringBuilder();
		str.append(breakpoint.getClassCanonicalName()).append(":")
				.append(orgLine)
				.append(" (debugLine: ")
				.append(breakpoint.getLineNo()).append(")\n");
		str.append("suspiciousness: " + String.format("%.2f", suspiciousness) + "\n");
		Formula learnedLogic = learningResult.getLearnedLogic();
		if (learnedLogic == null) {
			str.append("Could not learn anything.");
		} else if (Formula.FALSE.equals(learnedLogic)) {
			str.append("This line is likely a bug!");
		} else {
			str.append("Logic: ").append(learnedLogic).append("\n");
			str.append("Accuracy: ").append(1).append("\n");
		}
		
		return str.toString();
	}
}

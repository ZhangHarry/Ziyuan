/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package learntest.core.commons.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cfgcoverage.jacoco.analysis.data.BranchRelationship;
import cfgcoverage.jacoco.analysis.data.CfgCoverage;
import cfgcoverage.jacoco.analysis.data.CfgNode;
import cfgcoverage.jacoco.analysis.data.DecisionBranchType;
import cfgcoverage.jacoco.analysis.data.NodeCoverage;
import sav.common.core.utils.CollectionUtils;
import sav.common.core.utils.StringUtils;

/**
 * @author LLT
 *
 */
public class CoverageUtils {
	private CoverageUtils() {}

	/**
	 * we only need to check the first node of cfg to know if the cfg is covered.
	 */
	public static boolean notCoverAtAll(CfgCoverage cfgcoverage) {
		NodeCoverage nodeCvg = cfgcoverage.getCoverage(cfgcoverage.getCfg().getStartNode());
		return !nodeCvg.isCovered();
	}

	public static boolean noDecisionNodeIsCovered(CfgCoverage cfgcoverage) {
		for (CfgNode node : cfgcoverage.getCfg().getDecisionNodes()) {
			if (cfgcoverage.getCoverage(node).isCovered()) {
				return false;
			}
		}
		return true;
	}
	
	public static double calculateCoverageByBranch(CfgCoverage cfgCoverage) {
		int totalBranches = 0;
		int coveredBranches = 0;
		List<CfgNode> decisionNodes = cfgCoverage.getCfg().getDecisionNodes();
		if (decisionNodes.isEmpty()) {
			if (!cfgCoverage.getCoverage(cfgCoverage.getCfg().getStartNode()).isCovered()) {
				return 0.0;
			}
			return 1.0;
		}
		for (CfgNode node : decisionNodes) {
			totalBranches += CollectionUtils.getSize(node.getBranches());
			coveredBranches += cfgCoverage.getCoverage(node).getCoveredBranches().size();
		}
		return coveredBranches / (double) totalBranches;
	}
	
	public static double calculateCoverage(CfgCoverage cfgCoverage) {
		int covered = 0;
		for (NodeCoverage nodeCvg : cfgCoverage.getNodeCoverages()) {
			if (nodeCvg.isCovered()) {
				covered++;
			}
		}
		double coverage = covered / (double) cfgCoverage.getCfg().getNodeList().size();
		return Math.round (coverage * 100.0) / 100.0;
	}
	
	/**
	 * build a coverage map from one single cfg coverage.
	 */
	public static Map<String, CfgCoverage> getCfgCoverageMap(CfgCoverage cfgCoverage) {
		Map<String, CfgCoverage> map = new HashMap<String, CfgCoverage>();
		map.put(cfgCoverage.getCfg().getId(), cfgCoverage);
		return map;
	}
	
	public static String getBranchCoverageDisplayText(CfgCoverage cfgCoverage) {
		return getBranchCoverageDisplayText(cfgCoverage, -1);
	}
	
	public static String getBranchCoverageDisplayText(CfgCoverage cfgCoverage, int testIdx) {
		return StringUtils.newLineJoin(getBranchCoverageDisplayTexts(cfgCoverage, testIdx));
	}
	
	public static List<String> getBranchCoverageDisplayTexts(CfgCoverage cfgCoverage, int testIdx) {
		List<String> lines = new ArrayList<String>();
		for (CfgNode node : cfgCoverage.getCfg().getDecisionNodes()) {
			StringBuilder sb = new StringBuilder();
			NodeCoverage nodeCvg = cfgCoverage.getCoverage(node);
			Set<DecisionBranchType> coveredBranches = getBranchCoverage(testIdx, node, nodeCvg);
			sb.append("NodeCoverage [").append(node).append(", covered=").append(isCovered(testIdx, nodeCvg))
						.append(", coveredBranches=").append(coveredBranches.size()).append(", ")
						.append(coveredBranches).append("]");
			lines.add(sb.toString());
		}
		return lines;
	}

	private static Set<DecisionBranchType> getBranchCoverage(int testIdx, CfgNode node, NodeCoverage nodeCvg) {
		Set<DecisionBranchType> coveredBranches = new HashSet<DecisionBranchType>(2);
		for (int branchIdx : getCoveredBranches(testIdx, nodeCvg)) {
			DecisionBranchType branchType = node.getDecisionBranchType(branchIdx);
			CollectionUtils.addIfNotNull(coveredBranches, branchType);
		}
		return coveredBranches;
	}
	
	public static HashMap<String, Set<DecisionBranchType>> getBranchCoverage(CfgCoverage cfgCoverage) {
		HashMap<String , Set<DecisionBranchType>> relationships = new HashMap<>();
		int testIdx = -1;
		for (CfgNode node : cfgCoverage.getCfg().getDecisionNodes()) {
			NodeCoverage nodeCvg = cfgCoverage.getCoverage(node);
			Set<DecisionBranchType> coveredBranches = getBranchCoverage(testIdx, node, nodeCvg);
			relationships.put(node.toString(), coveredBranches);
		}
		return relationships;
	}

	private static boolean isCovered(int testIdx, NodeCoverage nodeCvg) {
		if (testIdx < 0) {
			return nodeCvg.isCovered();
		}
		return nodeCvg.isCovered(testIdx);
	}

	private static Collection<Integer> getCoveredBranches(int testIdx, NodeCoverage nodeCvg) {
		if (testIdx < 0) {
			return nodeCvg.getCoveredBranches();
		}
		return nodeCvg.getCoveredBranches(testIdx);
	}
	
}

/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package cfgcoverage.jacoco.analysis.data;

import java.util.List;
import java.util.Set;

import org.jacoco.core.internal.flow.Instruction;

import cfgcoverage.jacoco.utils.OpcodeUtils;
import sav.common.core.utils.CollectionUtils;

/**
 * @author LLT
 *
 */
public class ExtInstruction extends Instruction {
	private CfgNode cfgNode;
	private NodeCoverage nodeCoverage;
	private boolean newCfg;
	private int testIdx;
	private ExtInstruction predecessor; // jacocoPredecessor
	private ExtInstruction nextNode;
	
	public ExtInstruction(CfgNode cfgNode, NodeCoverage nodeCoverage, boolean newCfg) {
		super(cfgNode.getInsnNode(), cfgNode.getLine());
		this.cfgNode = cfgNode;
		this.nodeCoverage = nodeCoverage;
		this.newCfg = newCfg;
	}
	
	public void setCovered(int count, boolean multitargetJumpSource) {
		if (count > 0) {
			setCovered();
		}
		/* for the case of multitargetJumpSource, we don't update covered branch right away, but
		 * wait until all other probes are set covered, then update its true covered branch
		 * to get the correct coverage infor for specific branch
		 *  */
		if (!multitargetJumpSource && nextNode != null) {
			nodeCoverage.updateCoveredBranchesForTc(nextNode.cfgNode, testIdx);
		}
		setCovered(null, count);
	}
	
	public void updateNextBranchCvgInCaseMultitargetJumpSources() {
		if (nextNode != null) {
			updateBranchCvg(nextNode);
		}
	}

	public void updateBranchCvg(ExtInstruction branchInsn) {
		Set<Integer> coverTcs = branchInsn.nodeCoverage.getUndupCoveredTcs().keySet();
		for (Integer coverTc : coverTcs) {
			if (nodeCoverage.isCovered(coverTc)) {
				nodeCoverage.updateCoveredBranchesForTc(branchInsn.cfgNode, coverTc);
			}
		}
	}

	/**
	 * in normal cases which only have atmost one of branches of a node point to a multitarget node,
	 * we can count exactly coverage of TRUE_FALSE branch by extracting coverage of TRUE node.
	 * but in special cases where both branches of a node point to multitarget node,
	 * there is no way we can distinguish coverage count for each branch, 
	 * so we will leave the max count for such cases which is of course lead to a potential bug in cases we don't know. 
	 * [sadly, we only can do as best as we can here]
	 */
	public void updateTargetBranchCvgInCaseMultitargetJumpSources() {
		List<CfgNode> trueFalseBranches = cfgNode.findBranches(BranchRelationship.TRUE_FALSE);
		if (!nodeCoverage.isCovered(testIdx) || CollectionUtils.isEmpty(trueFalseBranches)) {
			return;
		}
		CfgNode falseBranch = cfgNode.findBranch(BranchRelationship.FALSE);
		int falseCoveredFreq = getCoveredFreq(nodeCoverage.getCfgCoverage(), falseBranch, testIdx);
		int nodeCoveredFreq = nodeCoverage.getCoveredFreq(testIdx);
		for (CfgNode trueFalseBranch : trueFalseBranches) {
			int trueFalseBranchCvg = getCoveredFreq(nodeCoverage.getCfgCoverage(), trueFalseBranch, testIdx);
			if (nodeCoveredFreq - falseCoveredFreq > 0 && trueFalseBranchCvg > 0) {
				nodeCoverage.updateCoveredBranchesForTc(trueFalseBranch, testIdx);
			}
		}
	}
	
	private static int getCoveredFreq(CfgCoverage cfgCoverage, CfgNode branch, int testIdx) {
		if (branch == null) {
			return 0;
		}
		return cfgCoverage.getCoverage(branch).getCoveredFreq(testIdx);
	}
	
	private void setCovered(ExtInstruction coveredBranch, int count) {
		if (coveredBranch != null) {
			nodeCoverage.updateCoveredBranchesForTc(coveredBranch.cfgNode, testIdx);
		}

		// otherwise, mark covered and update all its predecessors
		nodeCoverage.setCovered(testIdx, count);
		if (predecessor != null) {
			predecessor.setCovered(this, count);
		}
	}
	
	public CfgNode getCfgNode() {
		return cfgNode;
	}

	public void setTestIdx(int testIdx) {
		this.testIdx = testIdx;
	}
	
	public void setPredecessor(Instruction predecessorInsn) {
		super.setPredecessor(predecessorInsn);
		this.predecessor = (ExtInstruction) predecessorInsn;
	}

	public void setNodePredecessor(ExtInstruction source, BranchRelationship branchRelationship) {
		source.nextNode = this;
		if (!newCfg) {
			return;
		}
		cfgNode.setPredecessor(source.cfgNode, branchRelationship);
	}
	
	public void setNodePredecessorForJump(ExtInstruction source, boolean multiTarget) {
		if (!newCfg) {
			return;
		}
		if (OpcodeUtils.isCondition(source.cfgNode.getInsnNode().getOpcode())) {
			source.cfgNode.setDecisionBranch(cfgNode, DecisionBranchType.TRUE);
		}
		/* jump to a multitarget label */
		if (multiTarget) {
			cfgNode.setPredecessor(source.cfgNode, BranchRelationship.TRUE_FALSE);
			return;
		}
		BranchRelationship branchRelationship = BranchRelationship.TRUE;
		
		cfgNode.setPredecessor(source.cfgNode, branchRelationship);
	}

	@Override
	public String toString() {
		return cfgNode.toString();
	}
}

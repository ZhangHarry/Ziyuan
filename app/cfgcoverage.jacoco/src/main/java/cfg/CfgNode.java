/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package cfg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;

import cfg.utils.OpcodeUtils;
import sav.common.core.utils.Assert;
import sav.common.core.utils.CollectionUtils;

/**
 * @author LLT
 * BranchRelationship, and DecisionBranchType are all about byte code level, not source code as previous design.
 */
public class CfgNode {
	public static int INVALID_IDX = -1;

	private AbstractInsnNode insnNode;
	private int idx = INVALID_IDX;
	private int line;
	
	private boolean isDecisionNode;
	// TODO-NICE TO HAVE: implement for convenient if necessary
	private boolean isNegated; // flag which indicates whether the logic of condition is negated by compiler.

	/* if node is inside a loop, keep that loop header (should be a decision node) */
	private List<CfgNode> loopHeaders;

	/* branches are all children of node */
	private List<CfgNode> branches;
	private Map<DecisionBranchType, CfgNode> decisionBranches;
	
	/* parent nodes */
	private List<CfgNode> predecessors;
	
	/* dominatees are decision nodes which controls this node, if this node is not a decision node,
	 * then its dominatee will be the previous decision node */
	private Set<CfgNode> dominators;
	private Set<CfgNode> loopDominators;
	/*
	 * dependentees is only not null if node is a decision node, dependentees
	 * are decision nodes which under control of this node
	 */
	private Set<CfgNode> dependentees;
	private Set<CfgNode> loopDependentees;
	/* keep type of relationship between this node with other nodes */
	private Map<Integer, BranchRelationship> branchTypes = new HashMap<Integer, BranchRelationship>();
	private List<SwitchCase> switchCases;
	
	public CfgNode(AbstractInsnNode insnNode, int line) {
		predecessors = new ArrayList<CfgNode>();
		this.insnNode = insnNode;
		this.line = line;
	}

	public void addBranch(CfgNode node, BranchRelationship branchRelationship) {
		branches = CollectionUtils.initIfEmpty(branches, 2);
		branches.add(node);
		addBranchRelationship(node, branchRelationship);
	}

	public void addBranchRelationship(CfgNode node, BranchRelationship branchRelationship) {
		addBranchRelationship(node.idx, branchRelationship);
		if (node.branchTypes.get(idx) != null) {
			node.addBranchRelationship(idx, branchRelationship);
		}
	}
	
	private void addBranchRelationship(int nodeIdx, BranchRelationship branchRelationship) {
		BranchRelationship curRelationship = branchTypes.get(nodeIdx);
		branchTypes.put(nodeIdx, BranchRelationship.merge(curRelationship, branchRelationship));
	}
	
	/**
	 * force change to new relationship
	 * */
	public void updateBranchRelationship(int nodeIdx, BranchRelationship branchRelationship) {
		branchTypes.put(nodeIdx, branchRelationship);
	}

	public void setPredecessor(CfgNode predecessor, BranchRelationship branchRelationship) {
		predecessor.addBranch(this, branchRelationship);
		predecessors.add(predecessor);
		addBranchRelationship(predecessor, branchRelationship);
	}

	public List<CfgNode> getBranches() {
		return branches;
	}
	
	public CfgNode getNext() {
		Assert.assertTrue(CollectionUtils.getSize(branches) <= 1, "node has more than 1 branch");
		return isLeaf() ? null : branches.get(0);
	}

	public List<CfgNode> getPredecessors() {
		return predecessors;
	}

	public AbstractInsnNode getInsnNode() {
		return insnNode;
	}

	public int getLine() {
		return line;
	}

	public boolean isLeaf() {
		return CollectionUtils.isEmpty(branches);
	}

	public void setIdx(int idx) {
		this.idx = idx;
	}
	
	public int getIdx() {
		return idx;
	}
	
	public boolean isDecisionNode() {
		return isDecisionNode;
	}

	public void setDecisionNode(boolean isDecisionNode) {
		this.isDecisionNode = isDecisionNode;
	}

	public boolean isInLoop() {
		return loopHeaders != null;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("node[%d,%s,line %d]", idx, OpcodeUtils.getCode(insnNode.getOpcode()), line));
		if (isDecisionNode) {
			sb.append(", decis");
			if (isNegated) {
				sb.append("(neg)");
			}
			if (decisionBranches != null) {
				int trueBranchId = decisionBranches.get(DecisionBranchType.TRUE).idx;
				int falseBranchId = decisionBranches.get(DecisionBranchType.FALSE).idx;
				sb.append("{T=").append(trueBranchId)
					.append(",F=").append(falseBranchId)
					.append("}");
				sb.append("(").append(trueBranchId).append("=").append(getBranchRelationship(trueBranchId))
					.append(",").append(falseBranchId).append("=").append(getBranchRelationship(falseBranchId))
					.append(")");
			} 
		} else if (insnNode instanceof JumpInsnNode) {
			sb.append(", jumpTo {");
			for (int i = 0; i < branches.size(); i++) {
				CfgNode branch = branches.get(i);
				sb.append(branch.getIdx());
				if (i != branches.size() - 1) {
					sb.append(", ");
				}
			}
			sb.append("}");
		}
		if (isLoopHeader()) {
			sb.append(", loopHeader");
		} else if (isInLoop()) {
			sb.append(", inloop");
		}
		sb.append("]");
		return sb.toString();
	}
	
	public boolean isLoopHeader() {
		return loopHeaders != null && loopHeaders.contains(this);
	} 

	/**
	 * note: in case of a loop has multiple conditions conjunction, 
	 * the loopHeader would not have a backwardJumpIdx;
	 */
	public int getBackwardJumpIdx() {
		if (isLeaf()) {
			return INVALID_IDX;
		}
		for (CfgNode child : branches) {
			/* if the node try to jump backward, it might be a loop header */
			if (child.idx < idx) {
				return child.idx;
			}
		}
		return INVALID_IDX;
	}
	
	public void addLoopHeader(CfgNode loopHeader) {
		if (loopHeaders == null) {
			loopHeaders = new ArrayList<CfgNode>(3); // there is not a normal case that we have more than 3-level loop.
		}
		CollectionUtils.addIfNotNullNotExist(loopHeaders, loopHeader);
	}

	/**
	 * if we have a loop like this:
	 * 10 instr a
	 * . 
	 * 15 decision node ()	
	 * .
	 * 20 decision node (i < a) jump to 10 
	 * 
	 * then node 20 has node 15 as its loopdominatee
	 * and node 15 has node 20 as its loopdependentee
	 * 
	 * @param node
	 * @param branchType 
	 */
	public void addDominator(CfgNode node, boolean loop, BranchRelationship branchType) {
		if (loop) {
			loopDominators = CollectionUtils.initIfEmpty(loopDominators);
			loopDominators.add(node);
		} else {
			dominators = CollectionUtils.initIfEmpty(dominators);
			dominators.add(node);
			addBranchRelationship(node, branchType);
		}
	}
	
	public void addDependentee(CfgNode node, boolean loop, BranchRelationship branchType) {
		if (loop) {
			loopDependentees = CollectionUtils.initIfEmpty(loopDependentees);
			loopDependentees.add(node);
		} else {
			dependentees = CollectionUtils.initIfEmpty(dependentees);
			dependentees.add(node);
			addBranchRelationship(node, branchType);
		}
	}
	
	public boolean isLoopHeaderOf(CfgNode node) {
		if (node.getLoopHeaders() != null && node.getLoopHeaders().contains(this)) {
			return true;
		}
		return false;
	}

	public List<CfgNode> getLoopHeaders() {
		return loopHeaders;
	}

	public Set<CfgNode> getDominators() {
		return dominators;
	}

	public Set<CfgNode> getLoopDominators() {
		return loopDominators;
	}

	public Set<CfgNode> getDependentees() {
		return dependentees;
	}

	public Set<CfgNode> getLoopDependentees() {
		return loopDependentees;
	}

	public void addChildsDependentees(CfgNode dependentee) {
		if (dependentee.getDependentees() != null) {
			BranchRelationship branchType = getBranchRelationship(dependentee.getIdx());
			for (CfgNode childDependentee : dependentee.getDependentees()) {
				addDependentee(childDependentee, false, branchType);
			}
		}
	}
	
	public BranchRelationship getBranchRelationship(int nodeIdx) {
		return branchTypes.get(nodeIdx);
	}
	
	/**
	 * if the branch is undefined, it will return an alternative node with 
	 * TRUE_FALSE relationship.
	 * if you need to get branch with exact relationship, call findBranch instead.
	 */
	public CfgNode getBranch(BranchRelationship relationship) {
		if (isLeaf()) {
			return null;
		}
		CfgNode branch = findBranch(relationship);
		if (branch == null) {
			branch = findBranch(BranchRelationship.TRUE_FALSE);
		}
		return branch;
	}

	public CfgNode findBranch(BranchRelationship relationship) {
		for (CfgNode branch : CollectionUtils.nullToEmpty(branches)) {
			if (getBranchRelationship(branch.getIdx()) == relationship) {
				return branch;
			}
		}
		return null;
	}
	
	public List<CfgNode> findBranches(BranchRelationship relationship) {
		List<CfgNode> result = new ArrayList<CfgNode>(2);
		for (CfgNode branch : CollectionUtils.nullToEmpty(branches)) {
			if (getBranchRelationship(branch.getIdx()) == relationship) {
				result.add(branch);
			}
		}
		return result;
	}
	
	public void setDecisionBranch(CfgNode branch, DecisionBranchType type) {
		if (decisionBranches == null) {
			decisionBranches = new HashMap<DecisionBranchType, CfgNode>();
		}
		decisionBranches.put(type, branch);
	}
	
	public CfgNode getDecisionBranch(DecisionBranchType type) {
		if (decisionBranches == null) {
			return null;
		}
		return decisionBranches.get(type);
	}
	
	public DecisionBranchType getDecisionBranchType(int nodeIdx) {
		if (decisionBranches == null) {
			return null;
		}
		for (DecisionBranchType type : DecisionBranchType.values()) {
			CfgNode branch = getDecisionBranch(type);
			if (branch != null && (branch.getIdx() == nodeIdx)) {
				return type;
			}
		}
		return null;
	}

	public List<SwitchCase> getSwitchCases() {
		return switchCases;
	}

	public void setSwitchCases(List<SwitchCase> switchCases) {
		this.switchCases = switchCases;
	}
	
}

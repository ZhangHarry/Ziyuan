/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package cfgcoverage.jacoco.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

import cfgcoverage.jacoco.analysis.data.CFG;
import cfgcoverage.jacoco.analysis.data.CfgCoverage;
import cfgcoverage.jacoco.analysis.data.CfgNode;
import cfgcoverage.jacoco.analysis.data.ExtInstruction;
import cfgcoverage.jacoco.analysis.data.NodeCoverage;
import codecoverage.jacoco.agent.JaCoCoUtils;
import sav.common.core.utils.Assert;
import sav.common.core.utils.ClassUtils;
import sav.common.core.utils.SignatureUtils;

/**
 * @author LLT
 *
 */
public class CfgCoverageBuilder {
	private String className;
	private List<String> targetMethods;
	private Map<String, CfgCoverage> methodCfgMap;
	private CfgCoverage cfgCoverage;
	private CFG cfg;
	
	/* internal info */
	private String methodId;
	private int nodeIdx;
	private int testcaseIdx;
	private List<String> testMethods;
	private boolean newCoverage;
	private State state;
	
	public CfgCoverageBuilder(List<String> targetMethods) {
		methodCfgMap = new HashMap<String, CfgCoverage>();
		this.targetMethods = targetMethods;
	}
	
	public CfgCoverageBuilder startClass(String name, String signature) {
		state = State.CLASS;
		this.className = JaCoCoUtils.getClassName(name);
		return this;
	}
	
	public CfgCoverageBuilder testcase(int testcaseIdx) {
		this.testcaseIdx = testcaseIdx;
		return this;
	}

	public CfgCoverageBuilder testcases(List<String> testMethods) {
		this.testMethods = testMethods;
		return this;
	}
	
	public void setTargetMethods(List<String> targetMethods) {
		this.targetMethods = targetMethods;
	}

	public boolean startMethod(MethodNode methodNode) {
		if (!targetMethods.contains(ClassUtils.toClassMethodStr(className, methodNode.name))) {
			return false;
		}
		Assert.assertTrue(state == State.CLASS, "expect state CLASS, get state ", state.toString());
		state = State.METHOD;
		methodId = createMethodId(methodNode);
		cfgCoverage = methodCfgMap.get(methodId);
		if (cfgCoverage == null) {
			newCoverage = true;
			cfg = new CFG();
			cfg.setMethodNode(methodNode);
			cfgCoverage = new CfgCoverage(cfg);
			methodCfgMap.put(methodId, cfgCoverage);
		} else {
			cfg = cfgCoverage.getCfg();
		}
		return true;
	}
	
	public ExtInstruction instruction(AbstractInsnNode node, int line) {
		Assert.assertTrue(state == State.METHOD, "expect state METHOD, get state ", state.toString());
		/* look up for existing node */
		CfgNode cfgNode = cfg.getNode(nodeIdx ++);
		ExtInstruction extInstruction;
		if (cfgNode == null) {
			cfgNode = new CfgNode(node, line);
			cfg.addNode(cfgNode);
			NodeCoverage nodeCoverage = cfgCoverage.addCoverage(cfgNode);
			extInstruction = new ExtInstruction(cfgNode, nodeCoverage, true);
		} else {
			extInstruction = new ExtInstruction(cfgNode, cfgCoverage.getCoverage(cfgNode), false);
		}
		extInstruction.setTestcase(testMethods.get(testcaseIdx));
		return extInstruction;
	}

	public void endMethod() {
		Assert.assertTrue(state == State.METHOD, "expect state METHOD, get state ", state.toString());
		if (newCoverage) {
			CFG.updateExitNodes(cfg);
			CFG.updateDecisionNodes(cfg);
			CFG.updateNodesInLoop(cfg);
		}
		reset();
		state = State.CLASS;
	}

	private void reset() {
		newCoverage = false;
		methodId = null;
		nodeIdx = 0;
	}
	
	private String createMethodId(MethodNode method) {
		String fullMethodName = ClassUtils.toClassMethodStr(className, method.name);
		return SignatureUtils.createMethodNameSign(fullMethodName, method.signature);
	}
	
	public void match(boolean b) {
		// nothing to do for now.
	}
	
	public Map<String, CfgCoverage> getMethodCfgMap() {
		return methodCfgMap;
	}
	
	public List<CfgCoverage> getCoverage() {
		return new ArrayList<CfgCoverage>(methodCfgMap.values());
	}
	
	public void endClass() {
		state = State.INIT;
	}

	private enum State {
		INIT,
		CLASS,
		METHOD,
	}

}

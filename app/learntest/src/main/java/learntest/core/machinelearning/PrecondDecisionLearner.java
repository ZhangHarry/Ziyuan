/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package learntest.core.machinelearning;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cfgcoverage.jacoco.analysis.data.CfgNode;
import icsetlv.common.dto.BreakpointValue;
import learntest.calculator.OrCategoryCalculator;
import learntest.core.AbstractLearningComponent;
import learntest.core.LearningMediator;
import learntest.core.commons.data.decision.CoveredBranches;
import learntest.core.commons.data.decision.DecisionNodeProbe;
import learntest.core.commons.data.decision.DecisionProbes;
import learntest.core.commons.data.sampling.SamplingResult;
import learntest.testcase.data.INodeCoveredData;
import libsvm.core.Category;
import libsvm.core.Divider;
import libsvm.core.Machine;
import libsvm.extension.ByDistanceNegativePointSelection;
import libsvm.extension.NegativePointSelection;
import libsvm.extension.PositiveSeparationMachine;
import sav.common.core.Pair;
import sav.common.core.SavException;
import sav.common.core.formula.Formula;
import sav.settings.SAVExecutionTimeOutException;
import sav.strategies.dto.execute.value.ExecVar;

/**
 * @author LLT 
 * different from DecisionLearner which does sampling randomly,
 * this learner using precondition (which is build based on classifier of node's dominatees) 
 * for sampling.
 */
public class PrecondDecisionLearner extends AbstractLearningComponent {
	protected static Logger log = LoggerFactory.getLogger(DecisionLearner.class);
	private static final int FORMULAR_LEARN_MAX_ATTEMPT = 5;
	protected LearnedDataProcessor dataPreprocessor;

	public PrecondDecisionLearner(LearningMediator mediator) {
		super(mediator);
	}
	
	public DecisionProbes learn(DecisionProbes inputProbes) throws SavException {
		List<CfgNode> decisionNodes = inputProbes.getCfg().getDecisionNodes();
		DecisionProbes probes = inputProbes;
		dataPreprocessor = new LearnedDataProcessor(mediator, inputProbes);
		for (CfgNode node : decisionNodes) {
			DecisionNodeProbe nodeProbe = probes.getNodeProbe(node);
			if (nodeProbe.areAllbranchesUncovered()) {
				continue;
			}
			OrCategoryCalculator preconditions = getPreconditions(probes, node);
			dataPreprocessor.sampleForBranchCvg(node, preconditions);
			dataPreprocessor.sampleForLoopCvg(node, preconditions);
			
			nodeProbe = probes.getNodeProbe(node);
			updatePrecondition(nodeProbe);
		}
		return probes;
	}
	
	protected void updatePrecondition(DecisionNodeProbe nodeProbe) throws SavException {
		/* at this point only 1 branch is missing at most */
		CoveredBranches coveredType = nodeProbe.getCoveredBranches();
		TrueFalseLearningResult trueFalseResult = generateTrueFalseFormula(nodeProbe, coveredType);
		Formula oneMore = generateLoopFormula(nodeProbe);
		Formula truefalseFormula = trueFalseResult == null ? null : trueFalseResult.formula;
		List<Divider> divider = trueFalseResult == null ? null : trueFalseResult.dividers;
		nodeProbe.setPrecondition(Pair.of(truefalseFormula, oneMore), divider);
	}

	protected OrCategoryCalculator getPreconditions(DecisionProbes probes, CfgNode node) {
		return probes.getPrecondition(node);
	}

	private TrueFalseLearningResult generateTrueFalseFormula(DecisionNodeProbe orgNodeProbe,
			CoveredBranches coveredType) throws SavException {
		/* only generate if both branches are covered */
		if (coveredType != CoveredBranches.TRUE_AND_FALSE || !orgNodeProbe.needToLearnPrecond()) {
			return null;
		}
		Formula trueFlaseFormula = null;
		/* do generate formula and return */
		NegativePointSelection negative = new ByDistanceNegativePointSelection();
		PositiveSeparationMachine mcm = new PositiveSeparationMachine(negative);
		trueFlaseFormula = generateInitialFormula(orgNodeProbe, mcm);
		System.currentTimeMillis();
		double acc = mcm.getModelAccuracy();
		List<Divider> dividers = mcm.getLearnedDividers();
		System.out.println("=============learned multiple cut: " + trueFlaseFormula);

		int time = 0;
		DecisionNodeProbe nodeProbe = orgNodeProbe;
		CfgNode node = nodeProbe.getNode();
		while (trueFlaseFormula != null && time < FORMULAR_LEARN_MAX_ATTEMPT
				&& nodeProbe.needToLearnPrecond()) {
			long startTime = System.currentTimeMillis();
			DecisionProbes probes = nodeProbe.getDecisionProbes();
			/* after running sampling, probes will be updated as well */
			SamplingResult sampleResult = dataPreprocessor.sampleForModel(nodeProbe, probes.getOriginalVars(),
					mcm.getDataPoints(), getPreconditions(probes, node), mcm.getLearnedDividers());
			INodeCoveredData newData = sampleResult.getNewData(nodeProbe);
			nodeProbe.getPreconditions().clearInvalidData(newData);
			mcm.getLearnedModels().clear();
			addDataPoints(probes.getLabels(), probes.getOriginalVars(), newData.getTrueValues(), Category.POSITIVE, mcm);
			addDataPoints(probes.getLabels(), probes.getOriginalVars(), newData.getFalseValues(), Category.NEGATIVE, mcm);
			System.out.println("true data after selective sampling" + newData.getTrueValues());
			System.out.println("false data after selective sampling" + nodeProbe.getFalseValues());

			mcm.train();
			Formula tmp = mcm.getLearnedMultiFormula(probes.getOriginalVars(), probes.getLabels());
			System.out.println("improved the formula: " + tmp);
			if (tmp == null) {
				break;
			}

			double accTmp = mcm.getModelAccuracy();
			acc = mcm.getModelAccuracy();
			if (!tmp.equals(trueFlaseFormula)) {
				trueFlaseFormula = tmp;
				dividers = mcm.getLearnedDividers();
				acc = accTmp;

				if (acc == 1.0) {
					break;
				}
			} else {
				break;
			}

			time++;
		}
		TrueFalseLearningResult result = new TrueFalseLearningResult();
		result.formula = trueFlaseFormula;
		result.dividers = dividers;
		return result;
	}
	
	private Formula generateInitialFormula(DecisionNodeProbe nodeProbe, PositiveSeparationMachine mcm)
			throws SAVExecutionTimeOutException {
		DecisionProbes probes = nodeProbe.getDecisionProbes();
		mcm.setDefaultParams();
		List<String> labels = probes.getLabels();
		mcm.setDataLabels(labels);
		mcm.setDefaultParams();
		for(BreakpointValue value: nodeProbe.getTrueValues()){
			addDataPoint(labels, probes.getOriginalVars(), value, Category.POSITIVE, mcm);
		}
		for(BreakpointValue value: nodeProbe.getFalseValues()){
			addDataPoint(labels, probes.getOriginalVars(), value, Category.NEGATIVE, mcm);
		}
		mcm.train();
		Formula newFormula = mcm.getLearnedMultiFormula(probes.getOriginalVars(), labels);
		
		return newFormula;
	}
	
	private void addDataPoints(List<String> labels, List<ExecVar> vars, List<BreakpointValue> values, Category category, Machine machine) {
		for (BreakpointValue value : values) {
			addDataPoint(labels, vars, value, category, machine);
		}
	}
	
	private void addDataPoint(List<String> labels, List<ExecVar> vars, BreakpointValue bValue, Category category, Machine machine) {
		double[] lineVals = new double[labels.size()];
		int i = 0;
		for (ExecVar var : vars) {
			final Double value = bValue.getValue(var.getLabel(), 0.0);
			lineVals[i++] = value;
		}
		int size = vars.size();
		for (int j = 0; j < size; j++) {
//			double value = bValue.getValue(vars.get(j).getLabel(), 0.0);
			for (int k = j; k < size; k++) {
//				lineVals[i ++] = value * bValue.getValue(vars.get(k).getLabel(), 0.0);
				lineVals[i ++] = 0.0;
			}
		}

		machine.addDataPoint(category, lineVals);
	}

	private Formula generateLoopFormula(DecisionNodeProbe nodeProbe) throws SavException {
		if (!nodeProbe.getNode().isLoopHeader() || !nodeProbe.getCoveredBranches().coversTrue()) {
			return null;
		}
		if (nodeProbe.getOneTimeValues().isEmpty() || nodeProbe.getMoreTimesValues().isEmpty()) {
			log.info("Missing once loop data");
			return null;
		} else if (nodeProbe.getMoreTimesValues().isEmpty()) {
			log.info("Missing more than once loop data");
			return null;
		}
		return generateConcreteLoopFormula(nodeProbe);
	}

	private Formula generateConcreteLoopFormula(DecisionNodeProbe nodeProbe) throws SavException {
		Formula formula = null;
		if (nodeProbe.needToLearnPrecond()) {
			NegativePointSelection negative = new ByDistanceNegativePointSelection();
			PositiveSeparationMachine mcm = new PositiveSeparationMachine(negative);
			formula = generateInitialFormula(nodeProbe, mcm);
			
			int times = 0;
			double acc = mcm.getModelAccuracy();
			List<ExecVar> originalVars = nodeProbe.getDecisionProbes().getOriginalVars();
			List<String> labels = nodeProbe.getDecisionProbes().getLabels();
			while(formula != null && times < FORMULAR_LEARN_MAX_ATTEMPT && nodeProbe.needToLearnPrecond()) {
				SamplingResult samples = dataPreprocessor.sampleForModel(nodeProbe, 
						originalVars, mcm.getDataPoints(), nodeProbe.getPreconditions(), mcm.getLearnedDividers());
				INodeCoveredData newData = samples.getNewData(nodeProbe);
				addDataPoints(labels, originalVars, newData.getMoreTimesValues(), Category.POSITIVE, mcm);
				addDataPoints(labels, originalVars, newData.getOneTimeValues(), Category.NEGATIVE, mcm);
				acc = mcm.getModelAccuracy();
				if (acc == 1.0) {
					break;
				}
				mcm.train();
				Formula tmp = mcm.getLearnedMultiFormula(originalVars, labels);
				double accTmp = mcm.getModelAccuracy();
				if (tmp == null) {
					break;
				}
				if (!tmp.equals(formula) && accTmp > acc) {
					formula = tmp;
					acc = accTmp;
				} else {
					break;
				}
				times ++;
			}
			
		}	
		
		return formula;
	}

	private static class TrueFalseLearningResult {
		Formula formula;
		List<Divider> dividers;
	}
}
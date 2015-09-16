/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package icsetlv;

import icsetlv.common.dto.BkpInvariantResult;
import icsetlv.common.dto.BreakpointData;
import icsetlv.common.dto.BreakpointValue;
import icsetlv.sampling.SelectiveSampling;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import libsvm.core.Category;
import libsvm.core.CategoryCalculator;
import libsvm.core.FormulaProcessor;
import libsvm.core.Machine;
import libsvm.core.Machine.DataPoint;
import libsvm.extension.ISelectiveSampling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sav.common.core.formula.Formula;
import sav.common.core.utils.Assert;
import sav.common.core.utils.CollectionUtils;
import sav.strategies.dto.BreakPoint;
import sav.strategies.dto.execute.value.ExecValue;
import sav.strategies.dto.execute.value.ExecVar;
import sav.strategies.dto.execute.value.ExecVarType;

/**
 * @author LLT
 *
 */
public class InvariantLearner implements CategoryCalculator {
	protected static Logger log = LoggerFactory.getLogger(InvariantLearner.class);
	private InvariantMediator mediator;
	private Machine machine;
	private BreakPoint currentBreakpoint;
	
	public InvariantLearner(InvariantMediator mediator) {
		this.mediator = mediator;
		machine = mediator.getMachine();
	}

	public List<BkpInvariantResult> learn(List<BreakpointData> bkpsData) {
		List<BkpInvariantResult> result = new ArrayList<BkpInvariantResult>();
		for (BreakpointData bkpData : bkpsData) {
			log.info("Start to learn at " + bkpData.getBkp());
			if (bkpData.getPassValues().isEmpty() && bkpData.getFailValues().isEmpty()) {
				continue;
			}
			Formula formula = null;
			if (bkpData.getFailValues().isEmpty()) {
				log.info("This line is likely not a bug!");
				formula = Formula.TRUE;
			} else if (bkpData.getPassValues().isEmpty()) {
				log.info("This line is likely a bug!");
				formula = Formula.FALSE;
			} else {
				/* collect variable labels */
				List<ExecVar> allVars = collectAllVars(bkpData);
				if (allVars.isEmpty()) {
					continue;
				}

				ISelectiveSampling handler = getSelectiveSampling(bkpData.getBkp(), allVars);
				machine.setSelectiveSamplingHandler(handler);

				formula = learn(bkpData, allVars);
			}
			result.add(new BkpInvariantResult(bkpData.getBkp(), formula));
		}
		return result;
	}

	public ISelectiveSampling getSelectiveSampling(BreakPoint breakpoint, List<ExecVar> allVars) {
		SelectiveSampling handler = new SelectiveSampling(mediator);
		handler.setup(breakpoint, allVars);
		return handler;
	}

	/**
	 * apply svm
	 */
	private Formula learn(BreakpointData bkpData, List<ExecVar> allVars) {
		mediator.logBkpData(bkpData, allVars);
		/* handle boolean variables first */
		List<ExecVar> boolVars = extractBoolVars(allVars);
		Formula formula = learnFromBoolVars(boolVars, bkpData);
		if (formula != null) {
			return formula;
		}
		/* find divider for all variables */
		allVars.removeAll(boolVars);
		// Configure data for SVM machine
		machine.resetData();
		this.currentBreakpoint = bkpData.getBkp();
		List<String> allLables = extractLabels(allVars);
		machine.setDataLabels(allLables);
		addDataPoints(allVars, bkpData.getPassValues(), bkpData.getFailValues());

		if (machine.isPerformArtificialDataSynthesis()) {
			if (machine.artificialDataSynthesis(this)) {
				machine.train();
			} else if (log.isDebugEnabled()) {
				log.debug("Skip SVM training due to conflicting generated data.");
			}
		} else {
			machine.train();
		}

		return machine.getLearnedLogic(new FormulaProcessor<ExecVar>(allVars), true);
	}

	@Override
	public Category getCategory(final DataPoint dataPoint) {
		if (this.currentBreakpoint == null) {
			return null;
		}

		final List<String> labels = machine.getDataLabels();
		final int numberOfFeatures = dataPoint.getNumberOfFeatures();
		Map<String, Object> instrVarMap = new HashMap<String, Object>(numberOfFeatures);
		for (int i = 0; i < numberOfFeatures; i++) {
			// TODO NPN improve this part
			instrVarMap.put(labels.get(i), dataPoint.getValue(i));
		}

		try {
			final List<BreakpointData> datas = mediator.instDebugAndCollectData(
					Arrays.asList(this.currentBreakpoint), instrVarMap);
			Assert.assertTrue(datas.size() == 1, "There should be 1 and only 1 breakpoint.");
			final BreakpointData data = datas.get(0);
			if (CollectionUtils.isEmpty(data.getFailValues())
					^ CollectionUtils.isEmpty(data.getPassValues())) {
				return CollectionUtils.isEmpty(data.getPassValues()) ? Category.NEGATIVE
						: Category.POSITIVE;
			} else {
				// Cannot determine the category
				return null;
			}
		} catch (Exception e) {
			return null;
		}

	}

	private void addDataPoints(List<ExecVar> allVars, List<BreakpointValue> passValues,
			List<BreakpointValue> failValues) {
		for (BreakpointValue bValue : passValues) {
			addDataPoint(allVars, bValue, Category.POSITIVE);
		}

		for (BreakpointValue bValue : failValues) {
			addDataPoint(allVars, bValue, Category.NEGATIVE);
		}
	}

	private void addDataPoint(List<ExecVar> allVars, BreakpointValue bValue, Category category) {
		double[] lineVals = new double[allVars.size()];
		int i = 0;
		for (ExecVar var : allVars) {
			final Double value = bValue.getValue(var.getLabel(), 0.0);
			lineVals[i++] = value;
		}

		machine.addDataPoint(category, lineVals);
	}

	private List<String> extractLabels(List<ExecVar> allVars) {
		List<String> labels = new ArrayList<String>(allVars.size());
		for (ExecVar var : allVars) {
			labels.add(var.getVarId());
		}
		return labels;
	}

	private List<ExecVar> collectAllVars(BreakpointData bkpData) {
		Set<ExecVar> allVars = new HashSet<ExecVar>();
		for (ExecValue bkpVal : bkpData.getFailValues()) {
			collectExecVar(bkpVal.getChildren(), allVars);
		}
		for (ExecValue bkpVal : bkpData.getPassValues()) {
			collectExecVar(bkpVal.getChildren(), allVars);
		}
		return new ArrayList<ExecVar>(allVars);
	}
	
	private void collectExecVar(List<ExecValue> vals, Set<ExecVar> vars) {
		if (CollectionUtils.isEmpty(vals)) {
			return;
		}
		for (ExecValue val : vals) {
			if (val == null || CollectionUtils.isEmpty(val.getChildren())) {
				String varId = val.getVarId();
				vars.add(new ExecVar(varId, val.getType()));
			}
			collectExecVar(val.getChildren(), vars);
		}
	}

	private Formula learnFromBoolVars(List<ExecVar> boolVars, BreakpointData bkpData) {
		BooleanDivider divider = new BooleanDivider();
		return divider.divide(boolVars, bkpData);
	}

	private List<ExecVar> extractBoolVars(List<ExecVar> allVars) {
		List<ExecVar> result = new ArrayList<ExecVar>();
		for (ExecVar var : allVars) {
			if (var.getType() == ExecVarType.BOOLEAN) {
				result.add(var);
			}
		}
		return result;
	}

}

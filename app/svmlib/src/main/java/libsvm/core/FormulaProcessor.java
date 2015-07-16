/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package libsvm.core;

import java.util.ArrayList;
import java.util.List;

import sav.common.core.formula.Formula;
import sav.common.core.formula.LIAAtom;
import sav.common.core.formula.LIATerm;
import sav.common.core.formula.Operator;
import sav.common.core.formula.StringVar;

/**
 * @author LLT
 * 
 */
public class FormulaProcessor implements IDividerProcessor<Formula> {
	private List<String> dataLabels;

	public Formula process(Divider divider) {
		// a1*x1 + a2*x2 + ... + an*xn >= b
		CoefficientProcessing coefficientProcessing = new CoefficientProcessing();
		double[] thetas = coefficientProcessing
				.process(divider.getLinearExpr());
		List<LIATerm> liaTerms = new ArrayList<LIATerm>();
		for (int i = 0; i < thetas.length - 1; i++) {
			if (Double.compare(thetas[i], 0) == 0) {
				continue;
			}
			liaTerms.add(new LIATerm(new StringVar(dataLabels.get(i)), thetas[i]));
		}
		LIAAtom atom = new LIAAtom(liaTerms, Operator.GE, thetas[thetas.length - 1]);
		return atom;
	}
}

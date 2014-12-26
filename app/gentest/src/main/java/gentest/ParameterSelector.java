/**
 * Copyright TODO
 */
package gentest;

import gentest.data.LocalVariable;
import gentest.data.Sequence;
import gentest.data.variable.ISelectedVariable;
import gentest.data.variable.ReferenceVariable;
import gentest.value.generator.ValueGenerator;

import java.lang.reflect.Type;
import java.util.List;

import sav.common.core.SavException;
import sav.common.core.utils.CollectionUtils;
import sav.common.core.utils.Randomness;

/**
 * @author LLT
 * ParamterSelector actually do the both thing
 * automatic generate value for entered type, and statements 
 * for that value.
 */
public class ParameterSelector {
	private Sequence seq;
	
	public ParameterSelector() {
	}
	
	public void setSequence(Sequence methodDecl) {
		this.seq = methodDecl;
	}
	
	public ISelectedVariable selectParam(Class<?> clazz, Type type, int firstStmtIdx,
			int firstVarIdx) throws SavException {
		SelectionMode selectingMode = randomChooseSelectorType(clazz);
		switch (selectingMode) {
		case REFERENCE:
			return selectReferenceParam(clazz);
		default:
			return selectGeneratedParam(clazz, type, firstStmtIdx, firstVarIdx);
		}
	}
	
	/**
	 * generate new value for parameter.
	 */
	private ISelectedVariable selectGeneratedParam(Class<?> clazz,
			Type type, int firstStmtIdx, int firstVarIdx) throws SavException {
		return ValueGenerator.generate(clazz, type, firstStmtIdx, firstVarIdx);
	}

	/**
	 * return value of parameter by selecting from variables of
	 * previous method call.
	 */
	private ISelectedVariable selectReferenceParam(Class<?> type) {
		LocalVariable randomVisibleVar = Randomness.randomMember(seq
				.getVariablesByType(type));
		return new ReferenceVariable(randomVisibleVar);
	}

	/**
	 * only do random select type if there is any variables for type
	 * available in the sequence.
	 * Otherwise, just exclude REFERENCE option out of selectionMode.
	 */
	public SelectionMode randomChooseSelectorType(Class<?> type) {
		List<LocalVariable> existedVars = seq.getVariablesByType(type);
		if (CollectionUtils.isEmpty(existedVars)) {
			return SelectionMode.GENERATE_NEW;
		}
		if (Randomness.randomBoolFromDistribution(3, 2)) {
			return SelectionMode.REFERENCE;
		}
		return SelectionMode.GENERATE_NEW;
	}
	
	private static enum SelectionMode {
		REFERENCE,
		GENERATE_NEW
	}
}

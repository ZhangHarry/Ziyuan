/**
 * Copyright TODO
 */
package gentest;


import gentest.data.LocalVariable;
import gentest.data.MethodCall;
import gentest.data.Sequence;
import gentest.data.statement.Rmethod;
import gentest.data.variable.ISelectedVariable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import sav.common.core.SavException;

/**
 * @author LLT
 */
public class TestcaseGenerator {
	private ParameterSelector parameterSelector;
	private RuntimeExecutor executor;
	private Sequence seq;
	
	public TestcaseGenerator() {
		executor = new RuntimeExecutor();
		parameterSelector = new ParameterSelector();
	}
	
	private void refresh(Sequence seq) {
		this.seq = seq;
		executor.reset(seq);
		parameterSelector.setSequence(seq);
	}
	
	/**
	 * For each method in init list,
	 * check if method receiver already exits and initialize if necessary,
	 * then prepare inputs for the method,
	 * and append the current sequence.
	 *  
	 */
	public Sequence generateSequence(List<MethodCall> methods)
			throws SavException {
		Sequence seq = new Sequence();
		refresh(seq);
		for (int i = 0; i < methods.size(); i++) {
			MethodCall method = methods.get(i);
			/* prepare method receiver */
			LocalVariable receiver = seq.getReceiver(method.getDeclaringType());
			if (receiver == null) {
				ISelectedVariable param = parameterSelector.selectParam(
						method.getDeclaringType(), seq.getStmtsSize(),
						seq.getVarsSize());
				seq.appendReceiver(param, method.getDeclaringType());
				executor.executeReceiver(param);
			}
			List<ISelectedVariable> selectParams = selectParams(method);
			int[] inVars = new int[selectParams.size()];
			for (int j = 0; j < selectParams.size(); j++) {
				ISelectedVariable param = selectParams.get(j);
				inVars[j] = param.getReturnVarId();
			}
			Rmethod rmethod = Rmethod.of(method.getMethod(),
					seq.getReceiver(method.getDeclaringType()).getVarId());
			rmethod.setInVarIds(inVars);
			seq.appendMethodExecStmts(rmethod, selectParams);
			if (!executor.execute(rmethod, selectParams)) {
				// stop append method whenever execution fail.
				return seq;
			}
		}
		return seq;
	}

	/**
	 * auto generate value for all needed parameters of method
	 */
	private List<ISelectedVariable> selectParams(MethodCall methodCall) throws SavException {
		Method method = methodCall.getMethod();
		return selectParams(method.getParameterTypes());
	}

	private List<ISelectedVariable> selectParams(Class<?>[] paramTypes)
			throws SavException {
		List<ISelectedVariable> params = new ArrayList<ISelectedVariable>();
		int firstStmtIdx = seq.getStmtsSize();
		int firstVarIdx = seq.getVarsSize();
		for (Class<?> paramType : paramTypes) {
			ISelectedVariable param = parameterSelector.selectParam(paramType,
					firstStmtIdx, firstVarIdx);
			params.add(param);
			firstStmtIdx += param.getStmts().size();
			firstVarIdx += param.getNewVariables().size();
		}
		return params;
	}
	
	public RuntimeExecutor getExecutor() {
		return executor;
	}
}

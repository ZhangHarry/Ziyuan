/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package tzuyu.engine.junit;

import java.util.ArrayList;
import java.util.List;

import sav.common.core.utils.StringUtils;
import tzuyu.engine.TzConfiguration;
import tzuyu.engine.junit.printer.JOutputPrinter;
import tzuyu.engine.model.Variable;
import tzuyu.engine.runtime.RArrayDeclaration;

/**
 * @author LLT
 * 
 */
public class RArrayDeclarationJWriter extends AbstractStmtJWriter {
	private String declaredClass;
	private String declaredName;
	private List<String> params;

	public RArrayDeclarationJWriter(TzConfiguration config,
			VariableRenamer renamer, RArrayDeclaration rArrayDeclaration,
			Variable newVar, List<Variable> inputVars) {
		super(config, renamer);
		init(rArrayDeclaration, newVar, inputVars);
	}

	private void init(RArrayDeclaration statement, Variable newVar,
			List<Variable> inputVars) {
		declaredClass = statement.getElementType().getSimpleName();
		declaredName = renamer.getRenamedVar(newVar.getStmtIdx(), newVar.getArgIdx());
		params = new ArrayList<String>();
		for (int i = 0; i < inputVars.size(); i++) {
			Variable var = inputVars.get(i);
			params.add(getParamStr(var));
		}
	}

	@Override
	public void write(JOutputPrinter sb) {
		sb.append(declaredClass).append("[] ").append(declaredName).append(" = ")
			.append(newClazzToken).append(declaredClass).append("[]{ ");
		sb.append(StringUtils.join(params, ", "));
		sb.append("}");
	}

}

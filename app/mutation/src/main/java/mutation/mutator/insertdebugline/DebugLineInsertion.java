/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package mutation.mutator.insertdebugline;

import japa.parser.ast.CompilationUnit;
import japa.parser.ast.Node;
import japa.parser.ast.expr.AssignExpr;
import japa.parser.ast.expr.BooleanLiteralExpr;
import japa.parser.ast.expr.NameExpr;
import japa.parser.ast.stmt.AssertStmt;
import japa.parser.ast.stmt.ExpressionStmt;
import japa.parser.ast.stmt.ReturnStmt;
import japa.parser.ast.stmt.Statement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import mutation.mutator.AbstractMutationVisitor;
import mutation.parser.ClassDescriptor;
import sav.strategies.dto.ClassLocation;

/**
 * @author LLT
 *
 */
public class DebugLineInsertion extends AbstractMutationVisitor {
	private List<ClassLocation> locations;
	private ClassDescriptor clazzDesc;
	private Map<ClassLocation, DebugLineResult> returnStmts;
	private Map<ClassLocation, Integer> insertMap;
	private int curPos;
	
	public DebugLineInsertion() {
		
	}
	
	public DebugLineInsertion(ClassDescriptor classDescriptor, List<ClassLocation> locations) {
		reset(classDescriptor, locations);
	}
	
	public void reset(ClassDescriptor classDescriptor, List<ClassLocation> locations) {
		this.clazzDesc = classDescriptor;
		this.locations = locations;
	}
	
	public List<DebugLineResult> insert(CompilationUnit cu) {
		insertMap = new HashMap<ClassLocation, Integer>();
		returnStmts = new HashMap<ClassLocation, DebugLineResult>();
		curPos = 0;
		cu.accept(this, true);
		// collect data
		List<DebugLineResult> result = new ArrayList<DebugLineResult>();
		for (Entry<ClassLocation, Integer> entry : insertMap.entrySet()) {
			AssertStmt newStmt = new AssertStmt(new BooleanLiteralExpr(true));
			newStmt.setBeginLine(entry.getValue() + 1);
			result.add(new AddedLineData(entry.getKey(), newStmt));
		}
		result.addAll(returnStmts.values());
		Collections.sort(result, new Comparator<DebugLineResult>() {

			@Override
			public int compare(DebugLineResult o1, DebugLineResult o2) {
				int val1 = o1.getLocation().getLineNo();
				int val2 = o2.getLocation().getLineNo();
				return (val1 < val2 ? -1 : (val1 == val2 ? 0
						: 1));
			}
			
		});
		return result;
	}
	
	@Override
	protected boolean beforeVisit(Node node) {
		for (ClassLocation location : locations) {
			if (location.getLineNo() >= node.getBeginLine()
					&& location.getLineNo() <= node.getEndLine()) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	protected boolean beforeMutate(Node node) {
		if (!(node instanceof Statement)) {
			return false;
		}
		for (; curPos < locations.size(); curPos ++) {
			ClassLocation loc = getCurrentLocation();
			if (loc .getLineNo() == node.getBeginLine()) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public boolean mutate(ExpressionStmt n) {
		insertMap.put(getCurrentLocation(), n.getEndLine());
		return false;
	}

	private ClassLocation getCurrentLocation() {
		return locations.get(curPos);
	}

	@Override
	public boolean mutate(ReturnStmt n) {
		String newVarName = generateNewVarName();
		NameExpr varNameExpr = new NameExpr(newVarName);
		AssignExpr expr = new AssignExpr();
		expr.setTarget(varNameExpr);
		expr.setValue(n.getExpr());
		expr.setOperator(AssignExpr.Operator.assign);
		List<Node> newNodes = new ArrayList<Node>();
		newNodes.add(new ExpressionStmt(expr));
		newNodes.add(new ReturnStmt(varNameExpr));
		ClassLocation curLoc = getCurrentLocation();
		returnStmts.put(curLoc, new ReplacedLineData(curLoc, 
				n, newNodes));
		return false;
	}

	/**
	 * TODO: generate and check if the name already existed in current scope.
	 * it's the job of classDesc
	 */
	private String generateNewVarName() {
		return "tzzzzzzuyu";
	}
}

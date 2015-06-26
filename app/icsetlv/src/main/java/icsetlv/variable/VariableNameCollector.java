/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package icsetlv.variable;

import icsetlv.common.exception.IcsetlvException;
import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.Node;
import japa.parser.ast.body.VariableDeclaratorId;
import japa.parser.ast.expr.Expression;
import japa.parser.ast.expr.FieldAccessExpr;
import japa.parser.ast.expr.NameExpr;
import japa.parser.ast.expr.ThisExpr;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sav.common.core.Constants;
import sav.common.core.Logger;
import sav.common.core.SavRtException;
import sav.common.core.utils.BreakpointUtils;
import sav.common.core.utils.ClassUtils;
import sav.common.core.utils.CollectionUtils;
import sav.common.core.utils.StringUtils;
import sav.strategies.dto.BreakPoint;
import sav.strategies.dto.BreakPoint.Variable;
import sav.strategies.dto.BreakPoint.Variable.VarScope;


/**
 * @author LLT
 *
 */
public class VariableNameCollector {
	private Logger<?> log = Logger.getDefaultLogger();
	private List<String> srcFolders;
	private VarNameCollectionMode collectionMode;
	
	public VariableNameCollector(VarNameCollectionMode collectionMode, String... srcFolders) {
		this.srcFolders = new ArrayList<String>();
		CollectionUtils.addIfNotNullNotExist(this.srcFolders, srcFolders);
		this.collectionMode = collectionMode;
	}
	
	public void updateVariables(Collection<BreakPoint> brkps) throws IcsetlvException {
		Map<String, List<BreakPoint>> brkpsMap = BreakpointUtils.initBrkpsMap(brkps);

		for (String clzName : brkpsMap.keySet()) {
			File sourceFile = getSourceFile(clzName);
			if (sourceFile == null) {
				log.debug("Class", clzName, "doesn't exist in source folder(s)", srcFolders);
				continue;
			}
			List<Integer> lines = BreakpointUtils.extractLineNo(brkpsMap.get(clzName));
			
			VarNameVisitor visitor = new VarNameVisitor(collectionMode, lines);
			CompilationUnit cu;
			try {
				cu = JavaParser.parse(sourceFile);
				cu.accept(visitor, true);
				Map<Integer, List<Variable>> map = visitor.getResult();
				
				List<BreakPoint> breakpoints = brkpsMap.get(clzName);
				for(BreakPoint breakpoint: breakpoints){
					Integer lineNumber = breakpoint.getLineNo();
					breakpoint.setVars(map.get(lineNumber));
				}
			} catch (ParseException e) {
				log.error(e.getMessage());
				throw new SavRtException(e);
			} catch (IOException e) {
				log.error(e.getMessage());
				throw new SavRtException(e);
			}
		}	
	}

	private File getSourceFile(String clzName) {
		for (String srcFolder : srcFolders) {
			File file = new File(ClassUtils.getJFilePath(srcFolder, clzName));
			if (file.exists()) {
				return file;
			}
		}
		return null;
	}

	private static class VarNameVisitor extends DefaultVoidVisitor {
		private VarNameCollectionMode mode;
		private Map<Integer, List<Variable>> result;
		private List<Integer> lines;
		
		public VarNameVisitor(VarNameCollectionMode collectionMode, List<Integer> lines) {
			this.mode = collectionMode;
			this.lines = lines;
			result = new HashMap<Integer, List<Variable>>();
			for (Integer line : lines) {
				result.put(line, new ArrayList<BreakPoint.Variable>());
			}
		}
		
		@Override
		protected boolean beforehandleNode(Node node) {
			return lines.contains(node.getBeginLine());
		}
		
		@Override
		protected boolean beforeVisit(Node node) {
			for (Integer lineNo : lines) {
				if (lineNo >= node.getBeginLine() && lineNo <= node.getEndLine()) {
					return true;
				}
			}
			return false;
		}
		
		@Override
		public boolean handleNode(FieldAccessExpr n) {
			List<String> nameFragments = new ArrayList<String>();
			Expression scope = n;
			while (scope instanceof FieldAccessExpr) {
				FieldAccessExpr fieldAccessExpr = (FieldAccessExpr) scope;
				nameFragments.add(fieldAccessExpr.getField());
				scope = fieldAccessExpr.getScope();
			}
			VarScope varScope = VarScope.UNDEFINED;
			if (scope instanceof ThisExpr) {
				varScope = VarScope.THIS;
			} else if (scope instanceof NameExpr) {
				varScope = VarScope.UNDEFINED;
				nameFragments.add(((NameExpr)scope).getName());
			}

			String name = CollectionUtils.getLast(nameFragments);
			Collections.reverse(nameFragments);
			String fullName; 
			switch (mode) {
			case FULL_NAME:
				fullName = StringUtils.join(nameFragments, Constants.DOT); 
				break;
			case HIGHEST_LEVEL_VAR:
				fullName = name;
				break;
			default:
				fullName = StringUtils.join(nameFragments, Constants.DOT);
			}
			Variable var = new Variable(name, fullName);
			var.setScope(varScope);
			add(n.getBeginLine(), var);
			
			return false;
		}
		
		@Override
		public boolean handleNode(NameExpr n) {
			add(n.getBeginLine(), new Variable(n.getName()));
			return false;
		}
		
		@Override
		public boolean handleNode(VariableDeclaratorId n) {
			add(n.getBeginLine(), new Variable(n.getName()));
			return false;
		}
		
		private void add(int lineNumber, Variable var){
			CollectionUtils.addIfNotNullNotExist(result.get(lineNumber), var);
		}

		public Map<Integer, List<Variable>> getResult() {
			return result;
		}
		
	}
	
	public static enum VarNameCollectionMode {
		FULL_NAME, /* eg: with variable a.b.c, we add a variable with id a.b.c*/
		HIGHEST_LEVEL_VAR /* eg: with variable a.b.c, we only add a variable with id a */
	}
}

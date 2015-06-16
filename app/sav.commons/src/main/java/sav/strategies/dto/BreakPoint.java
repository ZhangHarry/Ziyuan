/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package sav.strategies.dto;

import java.util.ArrayList;
import java.util.List;

import sav.common.core.utils.Assert;
import sav.common.core.utils.StringUtils;

/**
 * @author LLT
 * 
 */
public class BreakPoint extends ClassLocation {
	private List<Variable> vars;
	private int charStart;
	private int charEnd;
	
	public BreakPoint(String className, String methodSign, int lineNo) {
		super(className, methodSign, lineNo);
		vars = new ArrayList<Variable>();
	}
	
	public BreakPoint(String className, int lineNo, Variable... newVars) {
		this(className, null, lineNo);
		if (newVars != null) {
			addVars(newVars);
		}
	}
	
	public void addVars(Variable... newVars) {
		for (Variable newVar : newVars) {
			vars.add(newVar);
		}
	}
	
	public List<Variable> getVars() {
		return vars;
	}

	public void setVars(List<Variable> vars) {
		this.vars = vars;
	}
	
	public boolean valid() {
		return lineNo > 0;
	}
	
	public String getMethodSign() {
		Assert.notNull(methodSign, "missing method name!");
		return methodSign;
	}
	
	public int getCharStart() {
		return charStart;
	}

	public void setCharStart(int charStart) {
		this.charStart = charStart;
	}

	public int getCharEnd() {
		return charEnd;
	}

	public void setCharEnd(int charEnd) {
		this.charEnd = charEnd;
	}

	@Override
	public String toString() {
		return "BreakPoint [classCanonicalName=" + classCanonicalName
				+ ", methodName=" + methodSign + ", lineNo=" + lineNo
				+ ", vars=" + vars + ", charStart=" + charStart + ", charEnd="
				+ charEnd + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((classCanonicalName == null) ? 0 : classCanonicalName
						.hashCode());
		result = prime * result + lineNo;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BreakPoint other = (BreakPoint) obj;
		if (classCanonicalName == null) {
			if (other.classCanonicalName != null)
				return false;
		} else if (!classCanonicalName.equals(other.classCanonicalName))
			return false;
		if (lineNo != other.lineNo)
			return false;
		return true;
	}

	public static class Variable {
		private String name;
		private String fullName;
		private VarScope scope;
		
		public Variable(String name, String fullName, VarScope scope) {
			this.name = name;
			this.fullName = fullName;
			this.scope = scope;
		}
		
		public Variable(String name, String fullName) {
			this(name, fullName, VarScope.UNDEFINED);
		}

		public Variable(String name) {
			this.name = name;
			this.fullName = name;
			scope = VarScope.UNDEFINED;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getFullName() {
			return fullName;
		}

		public void setFullName(String fullName) {
			this.fullName = fullName;
		}
		
		public String getId() {
			return StringUtils.dotJoin(scope.getDisplayName(), fullName);
		}

		public VarScope getScope() {
			return scope;
		}

		public void setScope(VarScope scope) {
			this.scope = scope;
		}

		@Override
		public String toString() {
			return "Variable [name=" + name + ", fullName=" + fullName
					+ ", scope=" + scope + "]";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((fullName == null) ? 0 : fullName.hashCode());
			result = prime * result + ((scope == null) ? 0 : scope.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Variable other = (Variable) obj;
			if (fullName == null) {
				if (other.fullName != null)
					return false;
			} else if (!fullName.equals(other.fullName))
				return false;
			if (scope != other.scope)
				return false;
			return true;
		}



		public static enum VarScope {
			THIS ("this"),
			UNDEFINED ("");
			private String displayName;
			
			private VarScope(String displayName) {
				this.displayName = displayName;
			}
			
			public String getDisplayName() {
				return displayName;
			}
		}
	}
	
}

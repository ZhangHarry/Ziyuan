package microbat.model;

import microbat.util.MicroBatUtil;
import sav.strategies.dto.execute.value.ExecValue;

public class InterestedVariable {
	/**
	 * The location means that the root of this variable is reachable (and defined) at 
	 * <code>lineNumber</code> of <code>className</code>
	 */
	private String visitingClassName;
	private int lineNumber;
	
	
	private ExecValue variable;
	
	public InterestedVariable(String className, int lineNumber,
			ExecValue variable) {
		super();
		this.visitingClassName = className;
		this.lineNumber = lineNumber;
		this.variable = variable;
	}
	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((visitingClassName == null) ? 0 : visitingClassName.hashCode());
		result = prime * result + lineNumber;
		result = prime * result
				+ ((variable.getVarId() == null) ? 0 : variable.getVarId().hashCode());
		return result;
	}

	@Override
	public String toString() {
		return "InterestedVariable [className=" + visitingClassName + ", lineNumber="
				+ lineNumber + ", variable=" + variable.getVarId() + "]";
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof InterestedVariable){
			InterestedVariable that = (InterestedVariable)obj;
			boolean isTheSame = MicroBatUtil.isTheSameVariable(this, that);
			
			return isTheSame;
		}
		
		return false;
	}


	public String getVisitingClassName() {
		return visitingClassName;
	}

	public void setVisitingClassName(String className) {
		this.visitingClassName = className;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public void setLineNumber(int lineNumber) {
		this.lineNumber = lineNumber;
	}

	public ExecValue getVariable() {
		return variable;
	}

	public void setVariable(ExecValue variable) {
		this.variable = variable;
	}
	
	
}

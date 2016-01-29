package microbat.model.variable;

public abstract class Variable {
	protected String type;
	protected String variableName;
	
	/**
	 * The id of an object (non-primitive type) is its object id. 
	 * For primitive type:
	 * if it is a field, its id is: its parent's object id + field name;
	 * if it is an array element, its id is: its parent's object id + index;
	 * if it is a local variable, its id is: its scope (i.e., class[startLine, endLine]) + variable name.
	 * if it is a virtual variable, its id is: "virtual var" + the order of the relevant return-trace-node. 
	 * 
	 * <br>
	 * <br>
	 * Note that if the user want to concanate a variable ID, such as local variable ID, field ID, etc. He
	 * or she should use the following three static method: <br>
	 * 
	 * <code>Variable.concanateFieldVarID()</code><br>
	 * <code>Variable.concanateArrayElementVarID()</code><br>
	 * <code>Variable.concanateLocalVarID()</code><br>
	 */
	protected String varID;

	public Variable(String name, String type){
		this.variableName = name;
		this.type = type;
	}
	
	public String getName() {
		return variableName;
	}

	public void setName(String variableName) {
		this.variableName = variableName;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	public String getVarID() {
		return varID;
	}

	/**
	 * Note that if the user want to concanate a variable ID, such as local variable ID, field ID, etc. He
	 * or she should use the following three static method: <br>
	 * 
	 * <code>Variable.concanateFieldVarID()</code><br>
	 * <code>Variable.concanateArrayElementVarID()</code><br>
	 * <code>Variable.concanateLocalVarID()</code><br>
	 * 
	 * @param varID
	 */
	public void setVarID(String varID) {
		this.varID = varID;
	}
	
	public static String concanateFieldVarID(String parentID, String fieldName){
		return parentID + "." + fieldName;
	}
	
	public static String concanateArrayElementVarID(String parentID, String indexValueString){
		return parentID + "[" + indexValueString + "]";
	}
	
	public static String concanateLocalVarID(String className, String varName, int startLine, int endLine){
		return className + "[" + startLine + "," + endLine + "] " + varName;	
	}
	
	

	public abstract String getSimpleName();
}

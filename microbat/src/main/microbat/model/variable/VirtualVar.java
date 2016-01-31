package microbat.model.variable;

/**
 * A virtual variable is a variable connecting the read/written relation between a return statement
 * and a method invocation. 
 * 
 * @author "linyun"
 *
 */
public class VirtualVar extends Variable {
	
	public static final String VIRTUAL_TYPE = "virtual variable";
	public static final String VIRTUAL_PREFIX = "vir_";

	public VirtualVar(String name, String type) {
		super(name, type);
	}

	@Override
	public String getSimpleName() {
		return this.variableName;
	}

	@Override
	public Variable clone() {
		VirtualVar var = new VirtualVar(variableName, type);
		var.setVarID(varID);
		return var;
	}

}

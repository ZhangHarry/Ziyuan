package tzuyu.engine.model;

import java.io.PrintStream;
import java.util.List;

import tzuyu.engine.TzConfiguration;

public abstract class StatementKind {

	protected Boolean longFormat;
	protected Integer stringMaxLength; 
	
	public abstract Class<?> getReturnType();

	public abstract List<Class<?>> getInputTypes();

	public abstract ExecutionOutcome execute(Object[] inputVals, PrintStream out);

	public abstract boolean isPrimitive();

	public abstract boolean hasNoArguments();

	public abstract String toParseableString();

	public StatementKind(TzConfiguration configuration) {
		config(configuration);
	}
	
	public void config(TzConfiguration config) {
		config(config.isLongFormat(), config.getStringMaxLength());
	}
	
	public void config (boolean longFormat, int stringMaxLength) {
		this.longFormat = longFormat;
		this.stringMaxLength = stringMaxLength;
	}
	
	public abstract void appendCode(Variable newVar, List<Variable> inputVars,
			StringBuilder b);

	public abstract boolean hasReceiverParameter();

	public boolean isConstructor() {
		return false;
	}

	public boolean isStatic() {
		return false;
	}

	@Override
	public String toString() {
		return toParseableString();
	}
}

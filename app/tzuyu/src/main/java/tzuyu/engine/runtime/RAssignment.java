package tzuyu.engine.runtime;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import sav.common.core.iface.IPrintStream;
import sav.common.core.utils.ObjectUtils;
import tzuyu.engine.TzConfiguration;
import tzuyu.engine.model.ExecutionOutcome;
import tzuyu.engine.model.Sequence;
import tzuyu.engine.model.StatementKind;
import tzuyu.engine.utils.PrimitiveTypes;
import tzuyu.engine.utils.StringEscapeUtils;

public class RAssignment extends StatementKind implements Serializable {

	private static final long serialVersionUID = -3724786577328099815L;

	private final Class<?> type;
	private Object value;

	private RAssignment(Class<?> t, Object v) {
		this.type = t;
		this.value = v;
	}

	public Object getValue() {
		return value;
	}

	@Override
	public Class<?> getReturnType() {
		return this.type;
	}

	@Override
	public List<Class<?>> getInputTypes() {
		return Collections.emptyList();
	}
	
	/**
	 * if the return type is not the same with assigned value
	 * (in that case, type of assigned value must be 
	 * 					the implementation of the return type)
	 */
	@Override
	public List<Class<?>> getAllDeclaredTypes() {
		List<Class<?>> types = super.getAllDeclaredTypes();
		Class<?> objClass = ObjectUtils.getObjClass(value, type);
		if (type != objClass) {
			types.add(objClass);
		}
		return types;
	}

	@Override
	public ExecutionOutcome execute(Object[] inputVals, IPrintStream out) {
		List<Object> outVars = new ArrayList<Object>(1);
		outVars.add(this.value);
		return new NormalExecution(this.value, outVars.toArray(), 0);
	}

	public static StatementKind statementForAssignment(Class<?> t, Object v,
			TzConfiguration config) {
		return new RAssignment(t, v);
	}

	@Override
	public boolean isPrimitive() {
		return true;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (!(o instanceof RAssignment)) {
			return false;
		}

		RAssignment other = (RAssignment) o;

		return this.type.equals(other.type)
				&& ObjectUtils.equalsWithNull(this.value, other.value);
	}

	/**
	 * Returns a hash code value for this PrimitiveOrStringOrNullDeclInfo
	 */
	@Override
	public int hashCode() {
		return this.type.hashCode()
				+ (this.value == null ? 0 : this.value.hashCode());
	}

	public static RAssignment nullOrZeroDecl(Class<?> type,
			TzConfiguration config) {
		if (String.class.equals(type))
			return new RAssignment(String.class, "");
		if (Character.TYPE.equals(type))
			return new RAssignment(Character.TYPE, 'a');
		if (Byte.TYPE.equals(type))
			return new RAssignment(Byte.TYPE, (byte) 0);
		if (Short.TYPE.equals(type))
			return new RAssignment(Short.TYPE, (short) 0);
		if (Integer.TYPE.equals(type))
			return new RAssignment(Integer.TYPE,
					(Integer.valueOf(0)).intValue());
		if (Long.TYPE.equals(type))
			return new RAssignment(Long.TYPE, (Long.valueOf(0)).longValue());
		if (Float.TYPE.equals(type))
			return new RAssignment(Float.TYPE, (Float.valueOf(0)).floatValue());
		if (Double.TYPE.equals(type))
			return new RAssignment(Double.TYPE,
					(Double.valueOf(0)).doubleValue());
		if (Boolean.TYPE.equals(type))
			return new RAssignment(Boolean.TYPE, false);
		return new RAssignment(type, null);
	}

	public static Sequence sequenceForPrimitive(Object o, TzConfiguration config) {
		if (o == null)
			throw new IllegalArgumentException("o is null");
		Class<?> cls = o.getClass();
		if (!PrimitiveTypes.isBoxedOrPrimitiveOrStringOrEnumType(cls)) {
			throw new IllegalArgumentException(
					"o is not a boxed primitive or String");
		}
		if (cls.equals(String.class)
				&& !PrimitiveTypes.stringLengthOK((String) o, config.getStringMaxLength())) {
			throw new IllegalArgumentException("o is a string of length > "
					+ config.getStringMaxLength());
		}

		return Sequence.create(RAssignment.statementForAssignment(
				PrimitiveTypes.primitiveType(cls), o, config));
	}

	@Override
	public boolean hasNoArguments() {
		return true;
	}

	@Override
	public String toParseableString() {
		String valStr = null;
		if (value == null) {
			valStr = "null";
		} else {
			Class<?> valueClass = PrimitiveTypes
					.primitiveType(value.getClass());
			if (String.class.equals(valueClass)) {
				valStr = "\"" + StringEscapeUtils.escapeJava(value.toString())
						+ "\"";
			} else if (char.class.equals(valueClass)) {
				valStr = Integer.toHexString((Character) value);
			} else {
				valStr = value.toString();
			}
		}
		return type.getName() + ":" + valStr;
	}

	@Override
	public boolean hasReceiverParameter() {
		return false;
	}

}

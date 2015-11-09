package icsetlv.trial.variable;

import icsetlv.DefaultValues;
import icsetlv.common.dto.BreakpointValue;
import icsetlv.common.utils.PrimitiveUtils;
import icsetlv.trial.heuristic.HeuristicIgnoringFieldRule;
import icsetlv.variable.DebugValueExtractor;
import icsetlv.variable.JdiParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sav.common.core.SavException;
import sav.common.core.SavRtException;
import sav.strategies.dto.BreakPoint;
import sav.strategies.dto.BreakPoint.Variable;
import sav.strategies.dto.BreakPoint.Variable.VarScope;
import sav.strategies.dto.execute.value.ArrayValue;
import sav.strategies.dto.execute.value.ExecValue;
import sav.strategies.dto.execute.value.PrimitiveValue;
import sav.strategies.dto.execute.value.ReferenceValue;
import sav.strategies.dto.execute.value.StringValue;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ArrayReference;
import com.sun.jdi.ArrayType;
import com.sun.jdi.BooleanType;
import com.sun.jdi.BooleanValue;
import com.sun.jdi.ClassType;
import com.sun.jdi.Field;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.PrimitiveType;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.StringReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Type;
import com.sun.jdi.Value;
import com.sun.jdi.event.BreakpointEvent;

public class DebugValueExtractor2 {
	protected static Logger log = LoggerFactory.getLogger(DebugValueExtractor.class);
	private static final String TO_STRING_SIGN= "()Ljava/lang/String;";
	private static final String TO_STRING_NAME= "toString";
	private static final Pattern OBJECT_ACCESS_PATTERN = Pattern.compile("^\\.([^.\\[]+)(\\..+)*(\\[.+)*$");
	private static final Pattern ARRAY_ACCESS_PATTERN = Pattern.compile("^\\[(\\d+)\\](.*)$");
	private static final int MAX_ARRAY_ELEMENT_TO_COLLECT = 5;

	/**
	 * In order to handle the graph structure of objects, this map is used to remember which object has been analyzed
	 * to construct a graph of objects.
	 */
	private Map<Long, ReferenceValue> objectPool = new HashMap<>();
	
	//private int valRetrieveLevel;
	
	public DebugValueExtractor2() {
		//this.valRetrieveLevel = DefaultValues.DEBUG_VALUE_RETRIEVE_LEVEL;
	}
	
//	public DebugValueExtractor2(int valRetrieveLevel) {
//		this.valRetrieveLevel = valRetrieveLevel;
//	}
	
	public final BreakpointValue extractValue(BreakPoint bkp, BreakpointEvent event)
			throws IncompatibleThreadStateException, AbsentInformationException, SavException {
		if (bkp == null) {
			return null;
		}
		
		BreakpointValue bkVal = new BreakpointValue(bkp.getId());
		ThreadReference thread = event.thread();
		synchronized (thread) {
			if (!thread.frames().isEmpty()) {
				StackFrame frame = findFrameByLocation(thread.frames(), event.location());
				Method method = frame.location().method();
				ReferenceType refType;
				ObjectReference objRef = null;
				if (method.isStatic()) {
					refType = method.declaringType();
				} else {
					objRef = frame.thisObject();
					refType = objRef.referenceType();
				}
				/*
				 * LOCALVARIABLES MUST BE NAVIGATED BEFORE FIELDS, because: in
				 * case a class field and a local variable in method have the
				 * same name, and the breakpoint variable with that name has the
				 * scope UNDEFINED, it must be the variable in the method.
				 */
				final Map<Variable, JdiParam> allVariables = new HashMap<Variable, JdiParam>();
				final List<LocalVariable> visibleVars = frame.visibleVariables();
				final List<Field> allFields = refType.allFields();
				for (Variable bpVar : bkp.getVars()) {
					// First check local variable
					LocalVariable matchedLocalVariable = findMatchedLocalVariable(bpVar, visibleVars);
					
					JdiParam param = null;
					if (matchedLocalVariable != null) {
						param = recursiveMatch(frame, matchedLocalVariable, bpVar.getFullName());
					} else {
						// Then check class fields (static & non static)
						Field matchedField = findMatchedField(bpVar, allFields);

						if (matchedField != null) {
							if (matchedField.isStatic()) {
								param = JdiParam.staticField(matchedField, refType, refType.getValue(matchedField));
							} else {
								Value value = objRef == null ? null : objRef.getValue(matchedField);
								param = JdiParam.nonStaticField(matchedField, objRef, value);
							}
							if (param.getValue() != null && !matchedField.name().equals(bpVar.getFullName())) {
								param = recursiveMatch(param, extractSubProperty(bpVar.getFullName()));
							}
							
							System.currentTimeMillis();
						}
					}
					if (param != null) {
						allVariables.put(bpVar, param);
					}
				}

				if (!allVariables.isEmpty()) {
					collectValue(bkVal, thread, allVariables);
				}
			}
		}
		return bkVal;
	}
	
	private LocalVariable findMatchedLocalVariable(Variable bpVar, List<LocalVariable> visibleVars){
		LocalVariable match = null;
		if (bpVar.getScope() != VarScope.THIS) {
			for (LocalVariable localVar : visibleVars) {
				if (localVar.name().equals(bpVar.getParentName())) {
					match = localVar;
					break;
				}
			}
		}
		
		return match;
	}
	
	private Field findMatchedField(Variable bpVar, List<Field> allFields){
		Field matchedField = null;
		for (Field field : allFields) {
			if (field.name().equals(bpVar.getParentName())) {
				matchedField = field;
				break;
			}
		}
		
		return matchedField;
	}

	protected void collectValue(BreakpointValue bkVal, ThreadReference thread,
			final Map<Variable, JdiParam> allVariables) throws SavException {
		for (Entry<Variable, JdiParam> entry : allVariables.entrySet()) {
			Variable var = entry.getKey();
			String varId = var.getId();
			Value value = entry.getValue().getValue();
			appendVarVal(bkVal, varId, value, 1, thread);
		}
		
		System.currentTimeMillis();
	}
	
	protected String extractSubProperty(final String fullName) {
		// obj idx
		int idx = fullName.indexOf(".");
		int arrIndex = fullName.indexOf("[");
		if ((idx < 0) || (arrIndex >= 0 && arrIndex < idx)) {
			idx = arrIndex;
		}  
		if (idx >= 0) {
			return fullName.substring(idx);
		}
		return fullName;
	}
	
	protected JdiParam recursiveMatch(final StackFrame frame, final LocalVariable match, final String fullName) {
		Value value = frame.getValue(match);
		JdiParam param = JdiParam.localVariable(match, value);
		if (!match.name().equals(fullName)) {
			return recursiveMatch(param , extractSubProperty(fullName));
		}
		return param;
	}
	
	protected JdiParam recursiveMatch(JdiParam param, final String property) {
		if (StringUtils.isBlank(property)) {
			return param;
		}
		Value value = param.getValue();
		if (value == null) {
			// cannot get property for a null object
			return null;
		}
		JdiParam subParam = null;
		String subProperty = null;
		// NOTE: must check Array before Object because ArrayReferenceImpl
		// implements both ArrayReference and ObjectReference (by extending
		// ObjectReferenceImpl)
		if (ArrayReference.class.isAssignableFrom(value.getClass())) {
			ArrayReference array = (ArrayReference) value;
			// Can access to the array's length or values
			if (".length".equals(property)) {
				subParam = JdiParam.nonStaticField(null, array, array.virtualMachine().mirrorOf(array.length()));
				// No sub property is available after this
			} else {
				final Matcher matcher = ARRAY_ACCESS_PATTERN.matcher(property);
				if (matcher.matches()) {
					int index = Integer.valueOf(matcher.group(1));
					subParam = JdiParam.arrayElement(array, index, getArrayEleValue(array, index)); 
					// After this we can have access to another dimension of the
					// array or access to the retrieved object's property
					subProperty = matcher.group(2);
				}
			}
		} else if (ObjectReference.class.isAssignableFrom(value.getClass())) {
			ObjectReference object = (ObjectReference) value;
			final Matcher matcher = OBJECT_ACCESS_PATTERN.matcher(property);
			if (matcher.matches()) {
				final String propertyName = matcher.group(1);
				Field propertyField = null;
				for (Field field : object.referenceType().allFields()) {
					if (field.name().equals(propertyName)) {
						propertyField = field;
						break;
					}
				}
				if (propertyField != null) {
					subParam = JdiParam.nonStaticField(propertyField, object, object.getValue(propertyField));
					subProperty = matcher.group(2);
					if (sav.common.core.utils.StringUtils.isEmpty(subProperty)) {
						subProperty = matcher.group(3);
					}
				}
			}
		}
		return recursiveMatch(subParam, subProperty);
	}

	private Value getArrayEleValue(ArrayReference array, int index) {
		if (array == null) {
			return null;
		}
		if (index >= array.length()) {
			return null;
		}
		return array.getValue(index);
	}

	/** append execution value*/
	private void appendVarVal(ExecValue parent, String varId,
			Value value, int level, ThreadReference thread) {
//		if (level > valRetrieveLevel || varId.endsWith("serialVersionUID")) {
//			return;
//		}
		if (value == null) {
			appendNullVarVal(parent, varId);
			return;
		}
		level++;
		Type type = value.type();
		if (type instanceof PrimitiveType) {
			/* TODO LLT: add Primitive type && refactor */
			if (type instanceof BooleanType) {
				parent.add(sav.strategies.dto.execute.value.BooleanValue.of(varId, ((BooleanValue)value).booleanValue()));
			} else {
				parent.add(new PrimitiveValue(varId, value.toString()));
			}
		} else if (type instanceof ArrayType) { 
			appendArrVarVal(parent, varId, (ArrayReference)value, level, thread);
		} else if (type instanceof ClassType) {
			/**
			 * if the class name is "String"
			 */
			if (PrimitiveUtils.isString(type.name())) {
				parent.add(new StringValue(varId, toPrimitiveValue((ClassType) type, (ObjectReference)value, thread)));
			} 
			/**
			 * if the class name is "Integer", "Float", ...
			 */
			else if (PrimitiveUtils.isPrimitiveType(type.name())) {
				parent.add(new PrimitiveValue(varId, toPrimitiveValue((ClassType) type, (ObjectReference)value, thread)));
			} 
			/**
			 * if the class is an arbitrary complicated class
			 */
			else {
				appendClassVarVal(parent, varId, (ObjectReference) value, level, thread);
			}
		}
	}

	private synchronized String toPrimitiveValue(ClassType type, ObjectReference value,
			ThreadReference thread) {
		Method method = type.concreteMethodByName(TO_STRING_NAME,
				TO_STRING_SIGN);
		if (method != null) {
			try {
				if (thread.isSuspended()) {
					if (value instanceof StringReference) {
						return ((StringReference) value).value();
					}
					Value toStringValue = value.invokeMethod(thread, method,
							new ArrayList<Value>(),
							ObjectReference.INVOKE_SINGLE_THREADED);
					return toStringValue.toString();
					
				}
			} catch (Exception e) {
				// ignore.
				log.warn(e.getMessage());
			}
		}
		return null;
	}
	
	private void appendNullVarVal(ExecValue parent, String varId) {
		ReferenceValue val = ReferenceValue.nullValue(varId);
		parent.add(val);
	}

	/**
	 * add a given variable to its parent
	 * 
	 * @param parent
	 * @param varId
	 * @param objRef
	 * @param level
	 * @param thread
	 */
	private void appendClassVarVal(ExecValue parent, String varId,
			ObjectReference objRef, int level, ThreadReference thread) {
		
		ClassType type = (ClassType) objRef.type();
		long refID = objRef.uniqueID();
		
		/**
		 * Here, check whether this object has been parsed.
		 */
		ReferenceValue val = this.objectPool.get(refID);
		if(val == null){
			val = new ReferenceValue(varId, false, refID, type);	
			this.objectPool.put(refID, val);
			
			Map<Field, Value> fieldValueMap = objRef.getValues(type.allFields());
			for (Field field : type.allFields()) {
				
				boolean isIgnore = HeuristicIgnoringFieldRule.isForIgnore(type.name(), field.name());
				if(!isIgnore){
					String childVarID = val.getChildId(field.name());
					Value childVarValue = fieldValueMap.get(field);
					
					appendVarVal(val, childVarID, childVarValue, level, thread);					
				}
				
			}
		}
		
		parent.add(val);
	}

	private void appendArrVarVal(ExecValue parent, String varId,
			ArrayReference value, int level, ThreadReference thread) {
		
		ArrayValue val = new ArrayValue(varId);
		val.setValue(value);
		String componentType = ((ArrayType)value.type()).componentTypeName();
		val.setComponentType(componentType);
		val.setReferenceID(value.uniqueID());
		
		//add value of elements
		for (int i = 0; i < value.length() /*&& i < MAX_ARRAY_ELEMENT_TO_COLLECT*/; i++) {
			String varID = val.getElementId(i);
			Value varValue = getArrayEleValue(value, i);
			appendVarVal(val, varID, varValue, level, thread);
		}
		parent.add(val);
	}
	/***/
	protected StackFrame findFrameByLocation(List<StackFrame> frames,
			Location location) throws AbsentInformationException {
		for (StackFrame frame : frames) {
			if (areLocationsEqual(frame.location(), location)) {
				return frame;
			}
		}
		throw new SavRtException("Can not find frame");
	}
	
	private boolean areLocationsEqual(Location location1, Location location2) throws AbsentInformationException {
		return location1.compareTo(location2) == 0;
	}
	
//	public int getValRetrieveLevel() {
//		return valRetrieveLevel;
//	}
//	
//	public void setValRetrieveLevel(int valRetrieveLevel) {
//		this.valRetrieveLevel = valRetrieveLevel;
//	}
	
}

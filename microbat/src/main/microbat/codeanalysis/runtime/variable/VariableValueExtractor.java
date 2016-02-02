package microbat.codeanalysis.runtime.variable;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import microbat.codeanalysis.runtime.herustic.HeuristicIgnoringFieldRule;
import microbat.codeanalysis.runtime.jpda.expr.ExpressionParser;
import microbat.codeanalysis.runtime.jpda.expr.ParseException;
import microbat.model.BreakPoint;
import microbat.model.BreakPointValue;
import microbat.model.value.ArrayValue;
import microbat.model.value.VarValue;
import microbat.model.value.PrimitiveValue;
import microbat.model.value.ReferenceValue;
import microbat.model.value.StringValue;
import microbat.model.variable.ArrayElementVar;
import microbat.model.variable.FieldVar;
import microbat.model.variable.LocalVar;
import microbat.model.variable.Variable;
import microbat.util.PrimitiveUtils;

import org.apache.commons.lang.StringUtils;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ArrayReference;
import com.sun.jdi.ArrayType;
import com.sun.jdi.BooleanType;
import com.sun.jdi.BooleanValue;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.ClassType;
import com.sun.jdi.Field;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.InvocationException;
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
@SuppressWarnings("restriction")
public class VariableValueExtractor {
//	protected static Logger log = LoggerFactory.getLogger(DebugValueExtractor.class);
	private static final String TO_STRING_SIGN= "()Ljava/lang/String;";
	private static final String TO_STRING_NAME= "toString";
	private static final Pattern OBJECT_ACCESS_PATTERN = Pattern.compile("^\\.([^.\\[]+)(\\..+)*(\\[.+)*$");
	private static final Pattern ARRAY_ACCESS_PATTERN = Pattern.compile("^\\[(\\d+)\\](.*)$");
	//private static final int MAX_ARRAY_ELEMENT_TO_COLLECT = 5;

	/**
	 * In order to handle the graph structure of objects, this map is used to remember which object has been analyzed
	 * to construct a graph of objects.
	 */
	private Map<Long, ReferenceValue> objectPool = new HashMap<>();
	
	
	private BreakPoint bkp;
	private ThreadReference thread;
	private Location loc;
	
	public VariableValueExtractor(BreakPoint bkp, ThreadReference thread,
			Location loc) {
		this.bkp = bkp;
		this.thread = thread;
		this.loc = loc;
	}

	public final BreakPointValue extractValue()
			throws IncompatibleThreadStateException, AbsentInformationException {
		if (bkp == null) {
			return null;
		}
		
		BreakPointValue bkVal = new BreakPointValue(bkp.getId());
		//ThreadReference thread = event.thread();
		synchronized (thread) {
			if (!thread.frames().isEmpty()) {
				//StackFrame frame = findFrameByLocation(thread.frames(), event.location());
				final StackFrame frame = findFrameByLocation(thread.frames(), loc);
				Method method = frame.location().method();
				ReferenceType refType;
				ObjectReference objRef = null;
				if (method.isStatic()) {
					refType = method.declaringType();
				} else {
					objRef = frame.thisObject();
					refType = objRef.referenceType();
				}
				/**
				 * LOCALVARIABLES MUST BE NAVIGATED BEFORE FIELDS, because: in
				 * case a class field and a local variable in method have the
				 * same name, and the breakpoint variable with that name has the
				 * scope UNDEFINED, it must be the variable in the method.
				 */
				final Map<Variable, JDIParam> allVariables = new HashMap<Variable, JDIParam>();
				final List<LocalVariable> visibleVars = frame.visibleVariables();
				final List<Field> allFields = refType.allFields();
				
				List<Variable> allVisibleVariables = collectAllVariable(visibleVars, allFields);
				bkp.setAllVisibleVariables(allVisibleVariables);
				
				for (Variable bpVar : bkp.getAllVisibleVariables()) {
					// First check local variable
					LocalVariable matchedLocalVariable = findMatchedLocalVariable(bpVar, visibleVars);
					
					JDIParam param = null;
					if (matchedLocalVariable != null) {
						param = recursiveMatch(frame, matchedLocalVariable, bpVar.getName());
					} 
					else {
						// Then check class fields (static & non static)
						Field matchedField = findMatchedField(bpVar, allFields);

						if (matchedField != null) {
							if (matchedField.isStatic()) {
								param = JDIParam.staticField(matchedField, refType, refType.getValue(matchedField));
							} else {
								Value value = objRef == null ? null : objRef.getValue(matchedField);
								param = JDIParam.nonStaticField(matchedField, objRef, value);
							}
							
							if (param.getValue() != null && !matchedField.name().equals(bpVar.getName())) {
								param = recursiveMatch(param, extractSubProperty(bpVar.getName()));
							}
							
							System.currentTimeMillis();
						}
					}
					if (param != null) {
						allVariables.put(bpVar, param);
					}
				}

				if (!allVariables.isEmpty()) {
//					long t1 = System.currentTimeMillis();
					collectValue(bkVal, objRef, thread, allVariables);
//					long t2 = System.currentTimeMillis();
//					System.out.println("collectValue() takes time " + (t2-t1) + "s");
				}
			}
		}
		return bkVal;
	}
	
	private List<Variable> collectAllVariable(List<LocalVariable> visibleVars, List<Field> allFields) {
		List<Variable> varList = new ArrayList<>();
		for(LocalVariable lv: visibleVars){
//			Var var = new Var(lv.name(), lv.name(), VarScope.UNDEFINED);
			LocalVar var = new LocalVar(lv.name(), lv.typeName(), 
					bkp.getDeclaringCompilationUnitName(), bkp.getLineNo());
			varList.add(var);
		}
		for(Field field: allFields){
			FieldVar var = new FieldVar(field.isStatic(), field.name(), field.typeName());
			var.setDeclaringType(field.declaringType().name());
			varList.add(var);
		}
		return varList;
	}

	private LocalVariable findMatchedLocalVariable(Variable bpVar, List<LocalVariable> visibleVars){
		LocalVariable match = null;
		if (bpVar instanceof LocalVar) {
			for (LocalVariable localVar : visibleVars) {
				if (localVar.name().equals(bpVar.getName())) {
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
			if (field.name().equals(bpVar.getName())) {
				matchedField = field;
				break;
			}
		}
		
		return matchedField;
	}

	protected void collectValue(BreakPointValue bkVal, ObjectReference objRef, ThreadReference thread,
			Map<Variable, JDIParam> allVariables){
		
		if(objRef != null){
			LocalVar variable = new LocalVar("this", objRef.type().toString(), 
					bkp.getDeclaringCompilationUnitName(), bkp.getLineNo());
			appendClassVarVal(bkVal, variable, objRef, 1, thread, true);			
		}
		
		for (Entry<Variable, JDIParam> entry : allVariables.entrySet()) {
			Variable var = entry.getKey();
			
//			String varId = var.getId();
//			if(var.getScope().equals(VarScope.THIS)){
//				varId = varId.substring(varId.indexOf("this.")+5, varId.length());						
//			}
			 
			JDIParam param = entry.getValue();
			Value value = param.getValue();
			boolean isField = (param.getField() != null);
			
			if(var.getName().contains("br")){
				System.currentTimeMillis();
			}
			
			if(!isField){
				LocalVar variable = new LocalVar(var.getName(), value.type().toString(), 
						bkp.getDeclaringCompilationUnitName(), bkp.getLineNo());
				appendVarVal(bkVal, variable, value, 1, thread, true);				
				System.currentTimeMillis();
			}
			
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
	
	protected JDIParam recursiveMatch(final StackFrame frame, final LocalVariable match, final String fullName) {
		Value value = frame.getValue(match);
		JDIParam param = JDIParam.localVariable(match, value);
		if (!match.name().equals(fullName)) {
			return recursiveMatch(param , extractSubProperty(fullName));
		}
		return param;
	}
	
	protected JDIParam recursiveMatch(JDIParam param, final String property) {
		if (StringUtils.isBlank(property)) {
			return param;
		}
		Value value = param.getValue();
		if (value == null) {
			// cannot get property for a null object
			return null;
		}
		JDIParam subParam = null;
		String subProperty = null;
		/** 
		 * 	NOTE: must check Array before Object because ArrayReferenceImpl
		 *	implements both ArrayReference and ObjectReference (by extending
		 *	ObjectReferenceImpl)
		 * 
		 */
		if (ArrayReference.class.isAssignableFrom(value.getClass())) {
			ArrayReference array = (ArrayReference) value;
			// Can access to the array's length or values
			if (".length".equals(property)) {
				subParam = JDIParam.nonStaticField(null, array, array.virtualMachine().mirrorOf(array.length()));
				// No sub property is available after this
			} else {
				final Matcher matcher = ARRAY_ACCESS_PATTERN.matcher(property);
				if (matcher.matches()) {
					int index = Integer.valueOf(matcher.group(1));
					subParam = JDIParam.arrayElement(array, index, getArrayEleValue(array, index)); 
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
					subParam = JDIParam.nonStaticField(propertyField, object, object.getValue(propertyField));
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

	/** 
	 * 
	 * append execution value
	 * 
	 */
	private void appendVarVal(VarValue parent, Variable variable, Value value, int level, 
			ThreadReference thread, boolean isRoot) {
		level++;
		
		if (value == null) {
			appendNullVarVal(parent, variable);
			return;
		}
//		System.out.println(level);
		
		Type type = value.type();
		
		if (type instanceof PrimitiveType) {
			if (type instanceof BooleanType) {
				microbat.model.value.BooleanValue ele = 
						new microbat.model.value.BooleanValue(((BooleanValue)value).booleanValue(), isRoot, variable);
				parent.addChild(ele);
				ele.addParent(parent);
				ele.setPrimitiveID(parent);
			} else {
				PrimitiveValue ele = new PrimitiveValue(value.toString(), isRoot, variable);
				parent.addChild(ele);
				ele.addParent(parent);
				ele.setPrimitiveID(parent);
			}
		} else if (type instanceof ArrayType) { 
			appendArrVarVal(parent, variable, (ArrayReference)value, level, thread, isRoot);
		} else if (type instanceof ClassType) {
			/**
			 * if the class name is "String"
			 */
			if (PrimitiveUtils.isString(type.name())) {
				String pValue = toPrimitiveValue((ClassType) type, (ObjectReference)value, thread);
				StringValue ele = new StringValue(pValue, isRoot, variable);
				ele.setVarID(String.valueOf(((ObjectReference)value).uniqueID()));
				parent.addChild(ele);
				ele.addParent(parent);
			} 
			/**
			 * if the class name is "Integer", "Float", ...
			 */
			else if (PrimitiveUtils.isPrimitiveType(type.name())) {
				String pValue = toPrimitiveValue((ClassType) type, (ObjectReference)value, thread);
				PrimitiveValue ele = new PrimitiveValue(pValue, isRoot, variable);
				ele.setVarID(String.valueOf(((ObjectReference)value).uniqueID()));
				parent.addChild(ele);
				ele.addParent(parent);
			} 
			/**
			 * if the class is an arbitrary complicated class
			 */
			else {
				appendClassVarVal(parent, variable, (ObjectReference) value, level, thread, isRoot);
			}
		}
		
		
	}

	private synchronized String toPrimitiveValue(ClassType type, ObjectReference value,
			ThreadReference thread) {
		Method method = type.concreteMethodByName(TO_STRING_NAME, TO_STRING_SIGN);
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
//				log.warn(e.getMessage());
			}
		}
		return null;
	}
	
	private void appendNullVarVal(VarValue parent, Variable variable) {
		ReferenceValue val = new ReferenceValue(true, false, variable);
		parent.addChild(val);
		val.addParent(parent);
	}
	
	private Value retriveExpression(final StackFrame frame, String expression){
		ExpressionParser.GetFrame frameGetter = new ExpressionParser.GetFrame() {
            @Override
            public StackFrame get()
                throws IncompatibleThreadStateException
            {
            	return frame;
                
            }
        };
        
        try {
        	ExpressionParser.evaluate(expression, frame.virtualMachine(), frameGetter);
        	Value val = ExpressionParser.getMassagedValue();
			return val;
			
		} catch (ParseException e) {
			//e.printStackTrace();
		} catch (InvocationException e) {
			e.printStackTrace();
		} catch (InvalidTypeException e) {
			e.printStackTrace();
		} catch (ClassNotLoadedException e) {
			e.printStackTrace();
		} catch (IncompatibleThreadStateException e) {
			e.printStackTrace();
		}
        
        return null;
	}

	/**
	 * add a given variable to its parent
	 * 
	 * @param parent
	 * @param varName
	 * @param objRef
	 * @param level
	 * @param thread
	 */
	private void appendClassVarVal(VarValue parent, Variable variable, ObjectReference objRef, 
			int level, ThreadReference thread, boolean isRoot) {
		
		ClassType type = (ClassType) objRef.type();
		long refID = objRef.uniqueID();
		
		/**
		 * Here, check whether this object has been parsed.
		 */
		ReferenceValue val = this.objectPool.get(refID);
		if(val == null){
			val = new ReferenceValue(false, refID, isRoot, variable);	
			setMessageValue(thread, val);
			
			this.objectPool.put(refID, val);
			
			boolean needParseFields = HeuristicIgnoringFieldRule.isNeedParsingFields(type);
			
			if(needParseFields){
				Map<Field, Value> fieldValueMap = objRef.getValues(type.allFields());
				for (Field field : type.allFields()) {
					
					boolean isIgnore = HeuristicIgnoringFieldRule.isForIgnore(type.name(), field.name());
					if(!isIgnore){
//						String childVarID = val.getChildId(field.name());
						Value childVarValue = fieldValueMap.get(field);
						FieldVar var = new FieldVar(field.isStatic(), field.name(), childVarValue.type().toString());
						
						appendVarVal(val, var, childVarValue, level, thread, false);				
					}
					
				}
				
			}
		}
		/**
		 * handle the case of alias variable
		 */
		else if(!val.getVarName().equals(variable.getName())){
			ReferenceValue cachedValue = val/*.clone()*/;
			val = new ReferenceValue(false, refID, isRoot, variable);	
			val.setChildren(cachedValue.getChildren());
			for(VarValue child: cachedValue.getChildren()){
				child.addParent(val);
			}
		}
		
		parent.addChild(val);
		val.addParent(parent);
	}

	private void setMessageValue(ThreadReference thread, ReferenceValue val) {
		StackFrame frame;
		try {
			frame = findFrameByLocation(thread.frames(), loc);
			Value value = retriveExpression(frame, val.getVarName());
			if(value != null){
				String message = value.toString();
				val.setStringValue(message);					
			}
			else{
				System.currentTimeMillis();
			}
		} catch (AbsentInformationException e) {
			e.printStackTrace();
		} catch (IncompatibleThreadStateException e) {
			e.printStackTrace();
		}
	}

	private void appendArrVarVal(VarValue parent, Variable variable,
			ArrayReference value, int level, ThreadReference thread, boolean isRoot) {
		
		ArrayValue arrayVal = new ArrayValue(false, isRoot, variable);
//		arrayVal.setValue(value);
		String componentType = ((ArrayType)value.type()).componentTypeName();
		arrayVal.setComponentType(componentType);
		arrayVal.setReferenceID(value.uniqueID());
		
		setMessageValue(thread, arrayVal);
		
		//add value of elements
		for (int i = 0; i < value.length() /*&& i < MAX_ARRAY_ELEMENT_TO_COLLECT*/; i++) {
			String varName = String.valueOf(i);
			Value elementValue = getArrayEleValue(value, i);
			ArrayElementVar var = new ArrayElementVar(varName, elementValue.type().toString());
			
			appendVarVal(arrayVal, var, elementValue, level, thread, false);
		}
		
		parent.addChild(arrayVal);
		arrayVal.addParent(parent);
	}
	/***/
	protected StackFrame findFrameByLocation(List<StackFrame> frames,
			Location location) throws AbsentInformationException {
		for (StackFrame frame : frames) {
			if (areLocationsEqual(frame.location(), location)) {
				return frame;
			}
		}
		
		throw new AbsentInformationException("Can not find frame");
	}
	
	private boolean areLocationsEqual(Location location1, Location location2) throws AbsentInformationException {
		//return location1.compareTo(location2) == 0;
		return location1.equals(location2);
	}
	
}

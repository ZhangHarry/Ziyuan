/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package microbat.model.value;

import microbat.model.variable.FieldVar;
import microbat.model.variable.Variable;

/**
 * @author LLT, modified by Yun Lin
 *
 */

public class ReferenceValue extends VarValue {
	protected static final String NULL_CODE = "isNull";
	
//	private ClassType classType; 
	
	public ReferenceValue(boolean isNull, boolean isRoot, Variable variable) {
		super(isRoot, variable);
		
		FieldVar field = new FieldVar(false, "isNull", "boolean");
		BooleanValue child = new BooleanValue(isNull, false, field);
		child.setVarID("isNull");
		
		addChild(child);
		child.addParent(this);
	}
	
	public ReferenceValue(boolean isNull, long referenceID, boolean isRoot, Variable variable) {
		super(isRoot, variable);
		this.variable.setVarID(String.valueOf(referenceID));
		
		FieldVar field = new FieldVar(false, "isNull", "boolean");
		BooleanValue child = new BooleanValue(isNull, false, field);
		child.setVarID("isNull");
		
		addChild(child);
		child.addParent(this);
		
//		setReferenceID(referenceID);
//		setClassType(type);
	}
	
//	public void setClassType(ClassType type) {
//		this.classType = type;
//	}
	
	public String getClassType(){
		return this.variable.getType();
	}
	
	public String toString(){
		StringBuffer buffer = new StringBuffer();
		if(getClassType() != null){
			buffer.append(getClassType() + ": ");			
		}
		else{
			buffer.append("unknown type: ");
		}
		
		buffer.append(getVarID());
		String print = buffer.toString();
		
		return print;
	}
	
	public static ReferenceValue nullValue(Variable var) {
		return new ReferenceValue(true, false, var);
	}
	
	public String getReferenceID() {
		return getVarID();
	}

	public void setReferenceID(long referenceID) {
		this.variable.setVarID(String.valueOf(referenceID));
	}
	
	public String getConciseTypeName(){
		String qualifiedName = getClassType();
		String conciseName = qualifiedName.substring(qualifiedName.lastIndexOf(".")+1, qualifiedName.length());
		return conciseName;
	}

	@Override
	public boolean isTheSameWith(GraphNode nodeAfter) {
		
		if(nodeAfter instanceof ReferenceValue){
			ReferenceValue thatRef = (ReferenceValue)nodeAfter;
			
			if(this.isDefinedToStringMethod() && thatRef.isDefinedToStringMethod()){
				return this.getStringValue().equals(thatRef.getStringValue());
			}
			else if(!this.isDefinedToStringMethod() && !thatRef.isDefinedToStringMethod()){
				return true;
			}
		}
		
		
		return false;
	}
	
	/**
	 * if the toString() of an object is undefined, the default toString() may return something like
	 * "pack.Class@12fa231". Based on this observation, I build this method.
	 * @param stringValue
	 * @return
	 */
	public boolean isDefinedToStringMethod(){
		if(stringValue == null){
			return false;
		}
		else{
			if(stringValue.contains("@") && stringValue.contains(".")){
				return false;
			}
			else{
				return true;
			}
		}
	}
	
	@Override
	public String getManifestationValue() {
//		String manifestation = stringValue;
//		manifestation = manifestation.substring(0, manifestation.indexOf("("));
//		manifestation = manifestation + "(id=" + variable.getVarID() + ")";
		return stringValue;
	}
	
//	@Override
//	public ReferenceValue clone(){
//		
////		ReferenceValue(String id, boolean isNull, long referenceID, ClassType type, 
////				boolean isRoot, boolean isField, boolean isStatic)
//		
//		BooleanValue isNullChild = (BooleanValue) getChildren().get(0);
//		boolean isNull = isNullChild.getBoolValue();
//		
//		ReferenceValue clonedValue = new ReferenceValue(getVarName(), isNull, this.getReferenceID(), this.getClassType(),
//				isRoot, isField, isStatic);
//		List<VarValue> clonedChildren = cloneChildren(clonedValue.getChildren());
//		for(VarValue clonedChild: clonedChildren){
//			clonedValue.addChild(clonedChild);
//			clonedChild.addParent(clonedValue);
//		}
//		
//		
//		return clonedValue;
//	}
	
//	private List<VarValue> cloneChildren(List<VarValue> children){
//		List<VarValue> clonedList = new ArrayList<>();
//		for(VarValue child: children){
//			VarValue clonedChild = child.clone();
//			clonedList.add(clonedChild);
//		}
//		
//		return clonedList;
//	}

//	public String getMessageValue() {
//		return messageValue;
//	}
//
//	public void setMessageValue(String messageValue) {
//		this.messageValue = messageValue;
//	}
}

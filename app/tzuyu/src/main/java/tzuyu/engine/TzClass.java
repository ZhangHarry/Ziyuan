/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package tzuyu.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tzuyu.engine.model.ClassInfo;
import tzuyu.engine.utils.ReflectionUtils;
import analyzer.ClassAnalyzer;
import analyzer.ClassVisitor;

/**
 * @author LLT 
 * @author Spencer Xiao
 * replace Analytics.
 */
public class TzClass {
	// class name
	private String className;
	private List<String> methods;
	
	private Map<Class<?>, ClassInfo> typeMap;
	private Class<?> target;

	private TzConfiguration configuration;
	
	public TzClass() {
		
	}
	
	public TzClass(Class<?> targetClass, List<String> methods) {
		ClassAnalyzer analyzer = new ClassAnalyzer(targetClass, methods);
		setClasses(targetClass, analyzer.analysis());
	}

	public void setClasses(Class<?> targetClass, Map<Class<?>, ClassInfo> map) {
		typeMap = map;
		target = targetClass;
		className = targetClass.getSimpleName();
	}

	public ClassInfo getClassInfo(Class<?> type) {
		// TODO LLT: for test, to be removed.
		// return typeMap.get(type);
		ClassInfo classInfo = typeMap.get(type);
		if (classInfo == null) {
			return ClassVisitor.forStore(typeMap).visitClass(type);
		}
		return classInfo;
	}
	
	public Map<Class<?>, ClassInfo> getTypeMap() {
		return typeMap;
	}

	public ClassInfo getTargetClassInfo() {
		return typeMap.get(target);
	}

	public Class<?> getTarget() {
		return target;
	}

	/**
	 * Get a list of accessible subclasses(exclusive interfaces) of the
	 * specified class or interface from the referenced class list. TODO [LLT]:
	 * move to utils?
	 * 
	 * @param superClass
	 * @return
	 */
	public List<ClassInfo> getAccessibleClasses(Class<?> superClass) {
		List<ClassInfo> subClasses = new ArrayList<ClassInfo>();
		Set<Class<?>> classes = typeMap.keySet();

		for (Class<?> subClass : classes) {
			if (ReflectionUtils.canBeUsedAs(subClass, superClass)
					&& ReflectionUtils.isVisible(subClass)
					&& !subClass.isInterface()) {
				subClasses.add(typeMap.get(subClass));
			}
		}

		return subClasses;
	}

	public TzConfiguration getConfiguration() {
		return configuration;
	}

	public void setConfiguration(TzConfiguration configuration) {
		this.configuration = configuration;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String clazzName) {
		this.className = clazzName;
	}
	
	public List<String> getMethods() {
		return methods;
	}
	
	public void setMethods(List<String> methods) {
		this.methods = methods;
	}

}

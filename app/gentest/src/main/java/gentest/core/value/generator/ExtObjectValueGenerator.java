/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package gentest.core.value.generator;

import gentest.core.commons.utils.GenTestUtils;
import gentest.core.commons.utils.MethodUtils;
import gentest.core.data.variable.GeneratedVariable;
import gentest.main.GentestConstants;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.List;

import sav.common.core.Pair;
import sav.common.core.SavException;
import sav.common.core.utils.Randomness;


/**
 * @author LLT
 *
 */
public class ExtObjectValueGenerator extends ObjectValueGenerator {
	private Class<?> implClazz;
	private Type type;
	private List<Method> methodcalls;
	private Pair<Class<?>, Type>[] paramTypes;
	
	public ExtObjectValueGenerator(Class<?> implClazz, Type type,
			List<String> methodSigns) {
		this.implClazz = implClazz;
		initMethodCalls(methodSigns);
		this.type = type;
	}
	
	public ExtObjectValueGenerator(List<String> methodSigns, Class<?> implClazz) {
		this.implClazz = implClazz;
		methodcalls = MethodUtils.findMethods(implClazz, methodSigns);
	}
	
	protected ExtObjectValueGenerator(Class<?> implClazz, List<Method> methods) {
		this.implClazz = implClazz;
		this.methodcalls = methods;
	}

	private void initMethodCalls(List<String> methodSigns) {
		methodcalls = new ArrayList<Method>();
		List<Method> initMethods;
		if (methodSigns == null) {
			initMethods = getCandidatesMethodForObjInit(implClazz);
		} else {
			initMethods = MethodUtils.findMethods(implClazz, methodSigns);
		}
		List<Method> methodsSeq = Randomness
				.randomSequence(
						initMethods,
						GentestConstants.OBJECT_VALUE_GENERATOR_MAX_SELECTED_METHODS);
		for (Method method : methodsSeq) {
			if (MethodUtils.isPublic(method)) {
				methodcalls.add(method);
			}
		}
	}

	private List<Method> getCandidatesMethodForObjInit(Class<?> targetClazz) {
		Method[] declaredMethods = targetClazz.getDeclaredMethods();
		List<Method> methods = new ArrayList<Method>(declaredMethods.length);
		for (Method method : declaredMethods) {
			boolean isExcluded = false;
			for (String excludePref : GentestConstants.OBJ_INIT_EXCLUDED_METHOD_PREFIXIES) {
				if (method.getName().startsWith(excludePref)) {
					isExcluded = true;
					continue;
				}
			}
			if (!isExcluded) {
				methods.add(method);
			}
		}
		return methods;
	}
	
	@Override
	public final boolean doAppend(GeneratedVariable variable, int level, Class<?> type)
			throws SavException {
		boolean canInit = super.doAppend(variable, level, implClazz);
		int varId = variable.getLastVarId();
		variable.commitReturnVarIdIfNotExist();
		if (canInit) {
			doAppendMethod(variable, level, varId);
		}
		return canInit;
	}

	protected void doAppendMethod(GeneratedVariable variable, int level, int scopeId)
			throws SavException {
		// generate value for method call
		for (Method method : methodcalls) {
			variable.newCuttingPoint();
			doAppendMethod(variable, level, scopeId, false, method);
		}
	}
	
	@Override
	protected Pair<Class<?>, Type> getParamType(Class<?> clazz, Type type) {
		if (type instanceof TypeVariable<?>) {
			TypeVariable<?> typeVar = (TypeVariable<?>) type;
			return getContentType((Class<?>)typeVar.getGenericDeclaration(), typeVar.getName());
		}
		return super.getParamType(clazz, type);
	}
	
	protected Pair<Class<?>, Type> getContentType(Class<?> declaredClazz, String name) {
		if (type instanceof ParameterizedType) {
			TypeVariable<?>[] typeParameters = declaredClazz.getTypeParameters();
			int paramIdx = 0;
			for (;paramIdx < typeParameters.length; paramIdx++) {
				if (name.equals(typeParameters[paramIdx].getName())) {
					break;
				}
			}
			if (paramIdx < typeParameters.length) {
				return getParamType(paramIdx);
			}
		}
		return new Pair<Class<?>, Type>(Object.class, null);
	}
	
	@SuppressWarnings("unchecked")
	public Pair<Class<?>, Type> getParamType(int paramIdx) {
		if (paramTypes == null) {
			paramTypes = new Pair[((ParameterizedType) type).getActualTypeArguments().length];
		}
		Pair<Class<?>, Type> paramType = paramTypes[paramIdx];
		if (paramType == null) {
			Type compType = ((ParameterizedType) type)
					.getActualTypeArguments()[paramIdx];
			if (compType instanceof Class<?>) {
				paramType = new Pair<Class<?>, Type>(
						GenTestUtils.toClassItselfOrItsDelegate((Class<?>) compType),
						null);
			} else if (compType instanceof ParameterizedType) {
				paramType = new Pair<Class<?>, Type>(
						(Class<?>) ((ParameterizedType) compType)
						.getRawType(),
						compType);
			} else {
				paramType = new Pair<Class<?>, Type>(Object.class, null);
			}
			paramTypes[paramIdx] = paramType;
		}
		return paramType;
	}
	
	protected Pair<Class<?>, Type> getContentType(Type type, int idxType) {
		if (type instanceof ParameterizedType) {
			Type compType = ((ParameterizedType) type).getActualTypeArguments()[idxType];
			if (compType instanceof Class<?>) {
				return new Pair<Class<?>, Type>((Class<?>) compType, null);
			}
			if (compType instanceof ParameterizedType) {
				return new Pair<Class<?>, Type>(
						(Class<?>) ((ParameterizedType) compType).getRawType(),
						compType);
			}
		}
		return new Pair<Class<?>, Type>(Object.class, null);
	}

}

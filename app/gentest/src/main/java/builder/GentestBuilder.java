/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package builder;

import gentest.data.MethodCall;
import gentest.data.Sequence;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import sav.common.core.Logger;
import sav.common.core.Pair;
import sav.common.core.SavException;
import sav.common.core.utils.CollectionUtils;
import sav.common.core.utils.SignatureUtils;

/**
 * @author LLT
 *
 */
@SuppressWarnings("unchecked")
public abstract class GentestBuilder<T extends GentestBuilder<T>> {
	protected Logger<?> logger = Logger.getDefaultLogger();
	protected int numberOfTcs;
	protected Class<?> clazz;
	private boolean specificMethod = false;
	protected List<MethodCall> methodCalls;
	
	public GentestBuilder(int numberOfTcs) {
		methodCalls = new ArrayList<MethodCall>();
		this.numberOfTcs = numberOfTcs;
	}
	
	public T forClass(Class<?> clazz) {
		/*
		 * clean the previous class declaration,
		 * if previously,
		 * the class is entered without specific method, mean we have to add all methods of the class into testing method list.
		 * otherwise, just reset the current class
		 * and reset flat specificMethod to
		 */
		if (this.clazz != null && !specificMethod) {
			addAllMethods(methodCalls, clazz);
		}
		specificMethod = false;
		this.clazz = clazz;
		return (T) this;
	}
	
	private void addAllMethods(List<MethodCall> methodCalls, Class<?> targetClass) {
		for (Method method : targetClass.getMethods()) {
			addMethodCall(targetClass, method);
		}
	}

	private MethodCall addMethodCall(Class<?> targetClass, Method method) {
		MethodCall methodCall = toMethodCall(method, targetClass);
		CollectionUtils.addIfNotNull(methodCalls,
				methodCall);
		return methodCall;
	}
	
	

	public T method(String methodNameOrSign) {
		specificMethod = true;
		findAndAddTestingMethod(methodNameOrSign);
		return (T) this;
	}
	
	protected MethodCall findAndAddTestingMethod(String methodNameOrSign) {
		Method testingMethod = findTestingMethod(clazz, methodNameOrSign);
		return addMethodCall(clazz, testingMethod);
	}
	
	protected MethodCall toMethodCall(Method method, Class<?> receiverType) {
		if (verifyMethod(method)) {
			return MethodCall.of(method, receiverType);
		}
		return null;
	}
	
	protected static boolean verifyMethod(Method method) {
		return Modifier.isPublic(method.getModifiers());
	}
	
	protected static Method findTestingMethod(Class<?> clazz, String methodNameOrSign) {
		if (clazz != null) {
			/* try to find if input is method name */
			for (Method method : clazz.getMethods()) {
				if (method.getName().equals(methodNameOrSign)) {
					return method;
				}
			}
			/* try to find if input is method signature */
			for (Method method : clazz.getMethods()) {
				if (SignatureUtils.getSignature(method).equals(methodNameOrSign)) {
					return method;
				}
			}
			/* cannot find class */
			throw new IllegalArgumentException(String.format("cannot find method %s in class %s", methodNameOrSign
					, clazz.getName()));
		}
		/* class not yet declared */
		throw new IllegalArgumentException(
				String.format(
						"The class for method %s is not set. Expect forClass() is called before method(String methodNameOrSign)",
						methodNameOrSign));
	}
	
	public abstract Pair<List<Sequence>, List<Sequence>> generate()
			throws SavException;
}

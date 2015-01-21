/**
 * Copyright TODO
 */
package gentest.core.commons.utils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import sav.common.core.utils.SignatureUtils;
import sav.common.core.utils.StringUtils;

/**
 * @author LLT
 */
public class MethodUtils {
	private MethodUtils() {}
	
	public static boolean isStatic(Method method) {
		return Modifier.isStatic(method.getModifiers());
	}
	
	public static boolean isPublic(Method method) {
		return Modifier.isPublic(method.getModifiers());
	}
	
	public static List<Method> findMethods(Class<?> clazz, List<String> methodFullSigns) {
		List<Method> result = new ArrayList<Method>();
		for (Method method : clazz.getMethods()) {
			if (methodFullSigns.contains(StringUtils.join("", method.getName(),
					SignatureUtils.getSignature(method)))) {
				result.add(method);
				continue;
			}
		}
		
		return result;
	}
	
	public static Method findMethod(Class<?> clazz, String methodNameOrSign) {
		/* try to find if input is method signature */
		for (Method method : clazz.getMethods()) {
			if (SignatureUtils.getSignature(method).equals(methodNameOrSign)) {
				return method;
			}
		}
		/* try to find if input is method name */
		for (Method method : clazz.getMethods()) {
			if (method.getName().equals(methodNameOrSign)) {
				return method;
			}
		}
		/* cannot find method for class */
		throw new IllegalArgumentException(String.format("cannot find method %s in class %s", methodNameOrSign
				, clazz.getName()));
	}
}

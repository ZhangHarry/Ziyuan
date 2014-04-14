package analyzer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import tzuyu.engine.model.ClassInfo;
import tzuyu.engine.model.ConstructorInfo;
import tzuyu.engine.model.FieldInfo;
import tzuyu.engine.model.MethodInfo;
import tzuyu.engine.utils.CollectionUtils;
import tzuyu.engine.utils.PrimitiveTypes;

public class ClassVisitor {

	private Map<Class<?>, ClassInfo> classesCached;
	
	public ClassVisitor() {
		classesCached = new HashMap<Class<?>, ClassInfo>();
	}
	
	public ClassVisitor(Map<Class<?>, ClassInfo> classesCached) {
		this.classesCached = classesCached;
	}
	
	public Map<Class<?>, ClassInfo> getReferencedClasses() {
		return Collections.unmodifiableMap(classesCached);
	}

	public ClassInfo visitClass(Class<?> type) {
		if (classesCached.containsKey(type)) {
			return classesCached.get(type);
		}

		if (PrimitiveTypes.isBoxedOrPrimitiveOrStringOrEnumType(type)
				|| Filter.isInFilterList(type)) {
			ClassInfo pritmitive = new ClassInfoPrimitive(type);
			classesCached.put(type, pritmitive);
			return pritmitive;
		} else if (type.isArray()) {
			Class<?> componentType = type.getComponentType();

			ClassInfo base = visitClass(componentType);

			ClassInfoArray array = new ClassInfoArray(type, base);

			classesCached.put(type, array);

			return array;
		} else {
			ClassInfoReference reference = new ClassInfoReference(type);
			classesCached.put(type, reference);

			ClassInfo sc = null;
			if (type.getSuperclass() != null) {
				sc = visitClass(type.getSuperclass());
			}

			Class<?>[] interfaces = type.getInterfaces();
			List<ClassInfo> inList = new LinkedList<ClassInfo>();
			for (Class<?> in : interfaces) {
				if (Filter.filterClass(in)) {
					inList.add(visitClass(in));
				}
			}
			ClassInfo[] ins = inList.toArray(new ClassInfo[inList.size()]);

			Class<?>[] innerClasses = type.getDeclaredClasses();
			List<ClassInfo> innerList = new LinkedList<ClassInfo>();
			for (Class<?> innerClass : innerClasses) {
				if (Filter.filterClass(innerClass)) {
					innerList.add(visitClass(innerClass));
				}
			}
			ClassInfo[] inners = innerList.toArray(new ClassInfo[innerList
					.size()]);

			Field[] declaredFields = type.getDeclaredFields();
			List<FieldInfo> fieldList = new LinkedList<FieldInfo>();
			for (Field field : declaredFields) {
				if (Filter.filterField(field)) {
					FieldInfo fieldInfo = visitField(reference, field);
					fieldList.add(fieldInfo);
				}
			}
			FieldInfo[] fields = fieldList.toArray(new FieldInfo[fieldList
					.size()]);

			Method[] declaredMethods = type.getDeclaredMethods();
			List<MethodInfo> methodList = new LinkedList<MethodInfo>();
			for (Method method : declaredMethods) {
				if (Filter.filterMethod(method)) {
					methodList.add(visitMethod(reference, method));
				}
			}
			MethodInfo[] methods = methodList.toArray(new MethodInfo[methodList
					.size()]);

			Constructor<?>[] declaredCtors = type.getDeclaredConstructors();
			List<ConstructorInfo> ctorList = new LinkedList<ConstructorInfo>();
			for (Constructor<?> ctor : declaredCtors) {
				if (Filter.filterConstructor(ctor)) {
					ConstructorInfo constructor = visitConstructor(reference, ctor);
					CollectionUtils.addIfNotNull(ctorList, constructor);
				}
			}
			ConstructorInfo[] ctors = ctorList
					.toArray(new ConstructorInfo[ctorList.size()]);

			reference.initialize(sc, ins, inners, fields, methods, ctors);

			return reference;
		}
	}

	private ConstructorInfo visitConstructor(ClassInfo parent,
			Constructor<?> ctor) {
		Class<?>[] inputTypes = ctor.getParameterTypes();
		ClassInfo[] parameterTypes = new ClassInfo[inputTypes.length];
		for (int i = 0; i < inputTypes.length; i++) {
			parameterTypes[i] = visitClass(inputTypes[i]);
		}

		Class<?>[] exceptions = ctor.getExceptionTypes();
		ClassInfo[] exceptionTypes = new ClassInfo[exceptions.length];
		for (int i = 0; i < exceptions.length; i++) {
			exceptionTypes[i] = visitClass(exceptions[i]);
		}

		if (!ctor.isAccessible()) {
			ctor.setAccessible(true);
		}
		int access = ctor.getModifiers();
		String name = ctor.getName();
		// TODO LLT: private constructor
		if (Modifier.isPublic(access)) {
			return new ConstructorInfoImpl(parent, ctor, name, parameterTypes,
					exceptionTypes, access);
		}
		return null;
	}

	private FieldInfo visitField(ClassInfo parent, Field field) {
		Class<?> type = field.getType();
		ClassInfo classInfo = visitClass(type);
		String name = field.getName();

		if (!field.isAccessible()) {
			field.setAccessible(true);
		}
		int access = field.getModifiers();

		if (PrimitiveTypes.isBoxedOrPrimitiveOrStringOrEnumType(type)) {
			return new FieldInfoPrimitive(parent, field, name, classInfo,
					access);
		} else if (type.isArray()) {
			return new FieldInfoArray(parent, field, name, classInfo, access);
		} else {
			return new FieldInfoReference(parent, field, name, classInfo,
					access);
		}
	}

	private MethodInfo visitMethod(ClassInfo parent, Method method) {
		String name = method.getName();
		int access = method.getModifiers();
		ClassInfo returnType = visitClass(method.getReturnType());

		Class<?>[] inputTypes = method.getParameterTypes();
		ClassInfo[] parameterTypes = new ClassInfo[inputTypes.length];
		for (int i = 0; i < inputTypes.length; i++) {
			parameterTypes[i] = visitClass(inputTypes[i]);
		}

		Class<?>[] excepts = method.getExceptionTypes();
		ClassInfo[] exceptions = new ClassInfo[excepts.length];
		for (int i = 0; i < excepts.length; i++) {
			exceptions[i] = visitClass(excepts[i]);
		}

		return new MethodInfoImpl(parent, method, name, access, returnType,
				parameterTypes, exceptions);
	}

	public static ClassVisitor forStore(Map<Class<?>, ClassInfo> typeMap) {
		return new ClassVisitor(typeMap);
	}
}

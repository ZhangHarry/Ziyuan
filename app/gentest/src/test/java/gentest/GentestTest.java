/**
 * Copyright TODO
 */
package gentest;

import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import gentest.core.RandomTester;
import gentest.core.data.MethodCall;
import gentest.core.data.Sequence;
import gentest.junit.TestsPrinter;
import sav.common.core.Pair;
import sav.common.core.SavException;
import sav.commons.testdata.BoundedStack;
import testdata.InvisibleParamTypeClass;
import testdata.RunBigDecimal;

/**
 * @author LLT
 *
 */
public class GentestTest {
	
	@Test
	public void testBigDecimal() throws SecurityException, SavException, FileNotFoundException {
		RandomTester tester = new RandomTester(5, 5, 100);
		List<Method> methods = new ArrayList<Method>();
		addMethods(methods, RunBigDecimal.class);
		Pair<List<Sequence>, List<Sequence>> result = tester.test(toMethodCalls(methods));
		TestsPrinter printer = new TestsPrinter("testdata.test.result.runbigdecimal", 
				"testdata.test.result.runbigdecimal", "setSubMatrix", "RunBigDecimal", "./src/test/java");
		printer.printTests(result);
	}

	@Test
	public void test() throws SecurityException, SavException, FileNotFoundException {
		RandomTester tester = new RandomTester(5, 5, 100);
		List<Method> methods = new ArrayList<Method>();
		addMethods(methods, BoundedStack.class);
		Pair<List<Sequence>, List<Sequence>> result = tester.test(toMethodCalls(methods));
		TestsPrinter printer = new TestsPrinter("testdata.test.result.boundedstack", 
				"testdata.test.result.boundedstack", "test", "BoundedStack", "./src/test/java");
		printer.printTests(result);
	}

	protected List<MethodCall> toMethodCalls(List<Method> methods) {
		List<MethodCall> methodCalls = new ArrayList<MethodCall>(methods.size());
		for (Method method : methods) {
			methodCalls.add(MethodCall.of(method, method.getDeclaringClass()));
		}
		return methodCalls;
	}

	protected void addMethods(List<Method> methods, Class<?> clazz) {
		for (Method method : clazz.getDeclaredMethods()) {
			if (Modifier.isPublic(method.getModifiers())) {
				methods.add(method);
			}
		}
	}
	
	@Test
	public void testInValidMethod() throws Exception {
		RandomTester tester = new RandomTester(5, 5, 100);
		List<Method> methods = new ArrayList<Method>();
		addMethods(methods, InvisibleParamTypeClass.class);
		Pair<List<Sequence>, List<Sequence>> result = tester.test(toMethodCalls(methods));
		TestsPrinter printer = new TestsPrinter("testdata.test.result.invalidMethod", 
				"testdata.test.result.invalidMethod", "test", "InvisibleParamTypeClass", "./src/test/java");
		printer.printTests(result);
	}
	
}
 
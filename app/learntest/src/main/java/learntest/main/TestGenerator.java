package learntest.main;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

import gentest.builder.RandomTraceGentestBuilder;
import gentest.core.commons.utils.MethodUtils;
import gentest.core.data.MethodCall;
import gentest.core.data.Sequence;
import gentest.injection.GentestModules;
import gentest.injection.TestcaseGenerationScope;
import gentest.junit.FileCompilationUnitPrinter;
import gentest.junit.TestsPrinter;
import gentest.junit.TestsPrinter.PrintOption;
import learntest.core.commons.utils.DomainUtils;
import learntest.core.gentest.GentestResult;
import learntest.gentest.TestSeqGenerator;
import learntest.util.LearnTestUtil;
import sav.common.core.Pair;
import sav.common.core.SavException;
import sav.common.core.SystemVariables;
import sav.strategies.dto.AppJavaClassPath;
import sav.strategies.dto.execute.value.ExecVar;

/**
 * 
 * will be replace with learntest.core.gentest.TestGenerator.
 */
public class TestGenerator extends learntest.core.gentest.TestGenerator {
	private static Logger log = LoggerFactory.getLogger(TestGenerator.class);
	public static int NUMBER_OF_INIT_TEST = 1;
	private static String prefix = "test";
	private ClassLoader prjClassLoader;
	
	public TestGenerator(AppJavaClassPath appClasspath) {
		super(appClasspath);
		this.prjClassLoader = appClasspath.getPreferences().get(SystemVariables.PROJECT_CLASSLOADER);
	}
	
	public GentestResult genTest() throws ClassNotFoundException, SavException {
		String mSig = LearnTestUtil.getMethodWthSignature(LearnTestConfig.targetClassName, 
				LearnTestConfig.targetMethodName, LearnTestConfig.getMethodLineNumber());
		
		Class<?> clazz = LearnTestUtil.retrieveClass(LearnTestConfig.targetClassName);
		RandomTraceGentestBuilder builder = new RandomTraceGentestBuilder(NUMBER_OF_INIT_TEST)
										.classLoader(prjClassLoader)
										.queryMaxLength(1)
										.testPerQuery(1)
										.forClass(clazz)
										.method(mSig);
		String testSourceFolder = LearnTestUtil.retrieveTestSourceFolder();
		
		boolean isL2T = LearnTestConfig.isL2TApproach;
		String packageName = LearnTestConfig.getTestPackageName(isL2T);
		String simpleClassName = LearnTestConfig.getSimpleClassName();
		TestsPrinter printer = new TestsPrinter(packageName, null, prefix, simpleClassName, testSourceFolder);
		Pair<List<Sequence>, List<Sequence>> pair = builder.generate();
		GentestResult result = new GentestResult();
		result.setJunitClassNames(printer.printTests(pair));
		result.setJunitfiles(((FileCompilationUnitPrinter) printer.getCuPrinter()).getGeneratedFiles());
		return result;
	}
	
	public GentestResult genTestAccordingToSolutions(List<double[]> solutions, List<ExecVar> vars) 
			throws ClassNotFoundException, SavException {
		return genTestAccordingToSolutions(solutions, vars, PrintOption.OVERRIDE);
	}
	
	/**
	 * @param printOption whether to append existing test file or create a new one.
	 */
	public GentestResult genTestAccordingToSolutions(List<double[]> solutions, List<ExecVar> vars, PrintOption printOption) 
			throws ClassNotFoundException, SavException {
		MethodCall target = findTargetMethod();
		if (target == null) {
			return null;
		}
		
		GentestModules injectorModule = new GentestModules(prjClassLoader);
		injectorModule.enter(TestcaseGenerationScope.class);
		List<Module> modules = new ArrayList<Module>();
		modules.add(injectorModule);
		Injector injector = Guice.createInjector(modules);
		TestSeqGenerator generator = injector.getInstance(TestSeqGenerator.class);
		generator.setTarget(target);
		
		GentestResult result = new GentestResult();
		List<Sequence> sequences = new ArrayList<Sequence>();
		//int index = 0;
		Set<String> failToSetVars = new HashSet<String>();
		for (double[] solution : solutions) {
			result.addInputData(DomainUtils.toBreakpointValue(solution, vars));
			//sequences.add(generator.generateSequence(input, variables.get(index ++)));
			sequences.add(generator.generateSequence(solution, vars, failToSetVars));
		}
		if (!failToSetVars.isEmpty()) {
			log.debug("Cannot modify value for variables: {}", failToSetVars);
		}
		injectorModule.exit(TestcaseGenerationScope.class);

		TestsPrinter printer = new TestsPrinter(LearnTestConfig.getResultedTestPackage(LearnTestConfig.isL2TApproach),
				null, prefix, LearnTestConfig.getSimpleClassName(), LearnTestUtil.retrieveTestSourceFolder(),
				printOption);
		result.setJunitClassNames(printer.printTests(Pair.of(sequences, new ArrayList<Sequence>(0))));
		result.setJunitfiles(((FileCompilationUnitPrinter) printer.getCuPrinter()).getGeneratedFiles());
		return result;
	}

	private MethodCall findTargetMethod() throws ClassNotFoundException {
		return findTargetMethod(LearnTestConfig.targetClassName, LearnTestConfig.targetMethodName);
	}
	
	private MethodCall findTargetMethod(String targetClassName, String targetmethodName) throws ClassNotFoundException {
		Class<?> clazz = LearnTestUtil.retrieveClass(targetClassName);
		Method method = MethodUtils.findMethod(clazz, targetmethodName);
		if (Modifier.isPublic(method.getModifiers())) {
			return MethodCall.of(method, clazz);
		}
		return null;
	}
	
}

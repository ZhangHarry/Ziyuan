/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package learntest.core.gentest;

import java.lang.reflect.Method;
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
import gentest.core.data.MethodCall;
import gentest.core.data.Sequence;
import gentest.injection.GentestModules;
import gentest.injection.TestcaseGenerationScope;
import gentest.junit.FileCompilationUnitPrinter;
import gentest.junit.PrinterParams;
import gentest.junit.TestsPrinter;
import gentest.junit.TestsPrinter.PrintOption;
import learntest.core.commons.LearntestExceptionType;
import learntest.core.commons.utils.DomainUtils;
import learntest.gentest.TestSeqGenerator;
import sav.common.core.Pair;
import sav.common.core.SavException;
import sav.common.core.SystemVariables;
import sav.common.core.utils.ClassUtils;
import sav.common.core.utils.CollectionUtils;
import sav.common.core.utils.SingleTimer;
import sav.strategies.dto.AppJavaClassPath;
import sav.strategies.dto.execute.value.ExecVar;

/**
 * @author LLT
 * 
 */
/* LLT: THIS IS CREATED FOR A PURPOSE! */
public class TestGenerator {
	private static Logger log = LoggerFactory.getLogger(TestGenerator.class);
	protected ClassLoader prjClassLoader;
	
	public TestGenerator(AppJavaClassPath appClasspath) {
		this.prjClassLoader = appClasspath.getPreferences().get(SystemVariables.PROJECT_CLASSLOADER);
	}
	
	public GentestResult genTest(GentestParams params) throws ClassNotFoundException, SavException {
		PrinterParams printerParams = params.getPrinterParams();
		TestsPrinter printer = new TestsPrinter(printerParams);
		if (!params.generateMainClass()) {
			return gentest(params, printer);
		} 
		/* if main class is required to be generated, we need to do a little more */
		MainClassJWriter cuWriter = new MainClassJWriter(printerParams.getPkg(),
				printerParams.getClassPrefix());
		printer.setCuWriter(cuWriter);
		GentestResult result = gentest(params, printer);
		/* add main class */
		result.setMainClassName(TestsPrinter.getJunitClassName(cuWriter.getMainClass()));
		FileCompilationUnitPrinter cuPrinter = new FileCompilationUnitPrinter(printerParams.getSrcPath());
		cuPrinter.print(CollectionUtils.listOf(cuWriter.getMainClass(), 1));
		result.setMainClassFile(CollectionUtils.getFirstElement(cuPrinter.getGeneratedFiles()));
		return result;
	}
	
	protected GentestResult gentest(GentestParams params, TestsPrinter printer) throws SavException {
		log.debug("start random gentest..");
		SingleTimer timer = SingleTimer.start("random gentest");
		Class<?> clazz = loadClass(params.getTargetClassName());
		RandomTraceGentestBuilder gentest = new RandomTraceGentestBuilder(params.getNumberOfTcs())
										.classLoader(prjClassLoader)
										.methodExecTimeout(params.getMethodExecTimeout())
										.queryMaxLength(params.getQueryMaxLength())
										.testPerQuery(params.getTestPerQuery())
										.forClass(clazz)
										.method(params.getMethodSignature());
		Pair<List<Sequence>, List<Sequence>> pair = gentest.generate() ;
		GentestResult result = new GentestResult();
		result.setJunitClassNames(printer.printTests(pair));
		result.setJunitfiles(((FileCompilationUnitPrinter) printer.getCuPrinter()).getGeneratedFiles());
		log.debug("generated junit classes: {}", result.getJunitClassNames());
		log.debug(timer.getResult());
		return result;
	}

	/**
	 * @param printOption whether to append existing test file or create a new one.
	 */
	public GentestResult genTestAccordingToSolutions(GentestParams params, List<double[]> solutions, List<ExecVar> vars,
			PrintOption printOption) throws SavException {
		MethodCall target = createMethodCall(params);
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
		TestsPrinter printer = new TestsPrinter(params.getPrinterParams());
		result.setJunitClassNames(printer.printTests(Pair.of(sequences, new ArrayList<Sequence>(0))));
		result.setJunitfiles(((FileCompilationUnitPrinter) printer.getCuPrinter()).getGeneratedFiles());
		return result;
	}

	private MethodCall createMethodCall(GentestParams params) throws SavException {
		Class<?> targetClazz = loadClass(params.getTargetClassName());
		Method method = ClassUtils.loockupMethod(targetClazz, params.getMethodSignature());
		if (method == null) {
			throw new SavException(String.format("Cannot find method %s in class %s!", params.getMethodSignature(),
					params.getTargetClassName()), LearntestExceptionType.METHOD_NOT_FOUND);
		}
		return MethodCall.of(method, targetClazz);
	}
	
	private Class<?> loadClass(String className) throws SavException {
		try {
			return prjClassLoader.loadClass(className);
		} catch (ClassNotFoundException e) {
			throw new SavException(e, LearntestExceptionType.CLASS_NOT_FOUND);
		}
	}
	
}

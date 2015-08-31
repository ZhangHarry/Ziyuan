package tzuyu.core.main;

import icsetlv.InvariantMediator;
import icsetlv.common.dto.BreakpointData;
import icsetlv.common.dto.BreakpointValue;
import icsetlv.sampling.IlpSolver;
import icsetlv.sampling.SelectiveSampling;
import japa.parser.JavaParser;
import japa.parser.ast.CompilationUnit;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import mutation.io.DebugLineFileWriter;
import mutation.mutator.VariableSubstitution;
import mutation.mutator.insertdebugline.DebugLineData;
import mutation.parser.ClassAnalyzer;
import mutation.parser.ClassDescriptor;
import mutation.parser.JParser;

import org.apache.commons.io.FileUtils;

import sav.common.core.Constants;
import sav.common.core.Pair;
import sav.common.core.formula.Eq;
import sav.common.core.formula.Formula;
import sav.common.core.formula.Var;
import sav.common.core.utils.ClassUtils;
import sav.common.core.utils.CollectionUtils;
import sav.common.core.utils.JunitUtils;
import sav.commons.AbstractTest;
import sav.commons.TestConfiguration;
import sav.strategies.IApplicationContext;
import sav.strategies.dto.BreakPoint;
import sav.strategies.vm.VMConfiguration;
import slicer.javaslicer.JavaSlicer;
import tzuyu.core.inject.ApplicationData;
import tzuyu.core.mutantbug.FilesBackup;
import tzuyu.core.mutantbug.Recompiler;
import assertion.invchecker.InvChecker;
import assertion.visitor.AddAssertStmtVisitor;
import assertion.visitor.GetLearningLocationsVisitor;

public class AssertionGeneration extends TzuyuCore {

	public AssertionGeneration(IApplicationContext appContext, ApplicationData appData) {
		super(appContext, appData);
	}
		
	@Override
	public void genAssertion(AssertionGenerationParams params) throws Exception {
		
		FilesBackup backup = null;
		
		try {
			String srcFolder = appData.getAppSrc();
			String className = params.getTestingClassName();
		
			// the original file
			File origFile = new File(ClassUtils.getJFilePath(srcFolder, className));
			
			// create new file with assertions
			File newFile = addAssertionToFile(srcFolder, className);
	
			// back up the original file
			backup = FilesBackup.startBackup();
			backup.backup(origFile);
			
			// overwrite the original file with the new file
			FileUtils.copyFile(newFile, origFile, false);
						
			// recompile the new file
			Recompiler recompiler = new Recompiler(appData.getVmConfig());
			recompiler.recompileJFile(appData.getAppTarget(), newFile);
			
			// add locations used to learn new assertion
			// getTestCases(params);
			testJavaSlicer();
			List<BreakPoint> locations = addLearningLocations(srcFolder, className);
			
			// learn assertions for new locations
			machineLearningForAssertion(locations, params);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// restore the original file
			if (backup != null) {
				backup.restoreAll();
			}
		}
		
	}

	public void testJavaSlicer() {
		try {
			JavaSlicer slicer = new JavaSlicer();
		
			VMConfiguration vmConfig = AbstractTest.initVmConfig1();
			vmConfig.addClasspath(TestConfiguration
					.getTzAssembly(Constants.TZUYU_JAVASLICER_ASSEMBLY));
			slicer.setVmConfig(vmConfig);
			
			String targetClass = "sav.commons.testdata.assertion.TestInput"; // TestInput.class.getName();
			String testClass = "sav.commons.testdata.assertion.TestInput1"; // TestInput1.class.getName();
			BreakPoint bkp1 = new BreakPoint(targetClass, "foo", 6);
			List<BreakPoint> breakpoints = Arrays.asList(bkp1);
			List<String> analyzedClasses = Arrays.asList(targetClass);
			List<String> testClassMethods = JunitUtils.extractTestMethods(Arrays
					.asList(testClass));
			
			slicer.setFiltering(analyzedClasses, null);
			List<BreakPoint> result = slicer.slice(breakpoints, testClassMethods);
			if (result.isEmpty()) {
				System.out.println("EMPTY RESULT!!!");
			}
			for (BreakPoint bkp : result) {
				System.out.println(bkp);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	 
	public List<String> getTestCases(AssertionGenerationParams params) {
		List<String> junitClassNames = new ArrayList<String>(params.getJunitClassNames());
		
		while (true) {
			try {
				List<String> randomTests = generateNewTests(
						params.getTestingClassName(),
						params.getMethodName(),
						params.getVerificationMethod(),
						params.getNumberOfTestCases());
				junitClassNames.addAll(randomTests);
				break;
			} catch (Throwable exception) {

			}
		}
		
		List<String> tests = new ArrayList<String>();
		for (int i = 1; i <= params.getNumberOfTestCases(); i++) {
			tests.add(junitClassNames.get(0) + "." + "test" + i);
		}
		
		return tests;
	}
	
	public void machineLearningForAssertion(List<BreakPoint> locations, AssertionGenerationParams params) 
			throws Exception
	{
		// get random test cases
		List<String> tests = getTestCases(params);
		
		// LearnInvariants learnInvariant = new LearnInvariants(appData.getVmConfig(), params);
		// List<BkpInvariantResult> invariants = learnInvariant.learn(locations, 
		//		junitClassNames, appData.getAppSrc());
		
		// collect data at break points
		VMConfiguration config = appData.getVmConfig();
		InvariantMediator im = new InvariantMediator();
		List<BreakpointData> bkpsData = im.debugTestAndCollectData(config, tests, locations);

		InvChecker ic = new InvChecker();
		IlpSolver solver = new IlpSolver(new HashMap<String, Pair<Double, Double>>(), false);
		SelectiveSampling ss = new SelectiveSampling(im);
		
		List<Pair<BreakpointData, List<Formula>>> finalInvs = new ArrayList<Pair<BreakpointData, List<Formula>>>();
		
		// learn template invs for each breakponit data
		for (int i = 0; i < bkpsData.size(); i++) {
			int oldInvsSize = 0;
			
			// when check with template, the correct invs will decrease at each step,
			// so we stop when the numbers of invs before and after the loop are equal
			while(true) {
				Pair<BreakpointData, List<Formula>> bkpInvs = ic.check(bkpsData.get(i));
				List<Formula> invs = bkpInvs.b;
				
				int newInvsSize = invs.size();
				
				if (newInvsSize == oldInvsSize) {
					finalInvs.add(bkpInvs);
					break;
				} else {
					oldInvsSize = newInvsSize;
				}
			
				List<List<Eq<?>>> assignments = new ArrayList<List<Eq<?>>>();
			
				for (Formula inv : invs) {
					solver.reset();
					inv.accept(solver);
					assignments.addAll(solver.getResult());
					System.out.println("inv = " + inv);
					System.out.println("res = " + solver.getResult());
				}
				
				assignments = mutate(assignments);
				
				for (List<Eq<?>> valSet : assignments) {
					List<BreakpointData> newBkpsData = im.instDebugAndCollectData(
							CollectionUtils.listOf(locations.get(i)), ss.toInstrVarMap(valSet));
					BreakpointData newBkpData = newBkpsData.get(0);
				
					List<BreakpointValue> newPassValues = new ArrayList<BreakpointValue>(bkpsData.get(i).getPassValues());
					List<BreakpointValue> newFailValues = new ArrayList<BreakpointValue>(bkpsData.get(i).getFailValues());
					
					// should be simplified
					newPassValues.addAll(newBkpData.getPassValues());
					newFailValues.addAll(newBkpData.getFailValues());
					
					newPassValues = simplify(newPassValues);
					newFailValues = simplify(newFailValues);
					
					bkpsData.get(i).setPassValues(newPassValues);
					bkpsData.get(i).setFailValues(newFailValues);
				}
			}
		}
		
		for (int i = 0; i < bkpsData.size(); i++) {
			System.out.println("Final Invs = " + finalInvs.get(i).b);
		}
	}

	public List<BreakPoint> addLearningLocations(String srcFolder, String className) throws Exception {
		List<BreakPoint> locations = new ArrayList<BreakPoint>();
		
		File file = new File(ClassUtils.getJFilePath(srcFolder, className));
		
		FileInputStream in = new FileInputStream(file);
		CompilationUnit cu = JavaParser.parse(in);
		
		GetLearningLocationsVisitor visitor = new GetLearningLocationsVisitor(className);
		
		visitor.visit(cu, locations);
		
		return locations;
	}
	
	public File addAssertionToFile(String srcFolder, String className) throws Exception {
		// the original file
		File file = new File(ClassUtils.getJFilePath(srcFolder, className));
		
		// get class analyser
		JParser parser = new JParser(srcFolder, new ArrayList<String>());
		ClassAnalyzer classAnalyser = new ClassAnalyzer(srcFolder, parser);
		
		// get variable substitution with type information for variable
		List<ClassDescriptor> classDecriptors = classAnalyser.analyzeJavaFile(file);
		VariableSubstitution subst = new VariableSubstitution(classDecriptors.get(0));
		
		// parse the original file
		FileInputStream in = new FileInputStream(file);
		CompilationUnit cu = JavaParser.parse(in);
		
		// the visitor used to add assertions
		AddAssertStmtVisitor visitor = new AddAssertStmtVisitor(cu.getImports(), subst);
		
		// visit the original file
		List<DebugLineData> arg = new ArrayList<DebugLineData>();
		visitor.visit(cu, arg);
		
		// write the new file
		DebugLineFileWriter writer = new DebugLineFileWriter(srcFolder);
		File newFile = writer.write(arg, className);
		
		// display the new file
		String content = FileUtils.readFileToString(newFile);
		System.out.println(content);
		
		// return the new file
		return newFile;
	}
	
	private List<List<Eq<?>>> mutate(List<List<Eq<?>>> assignments) {
		List<List<Eq<?>>> newAssignments = new ArrayList<List<Eq<?>>>();
		
		for (int k = 0; k <= 1; k++) {
			for (List<Eq<?>> assignment : assignments) {
				newAssignments.add(assignment);
				
				for (int i = 0; i < assignment.size(); i++) {
					List<Eq<?>> newAssignment = new ArrayList<Eq<?>>();
					
					for (int j = 0; j < assignment.size(); j++) {
						if (i == j) {
							Eq<?> e = assignment.get(j);
							Var v = e.getVar();
							Object value = e.getValue();
							
							if (value instanceof Integer) {
								Integer iValue = (Integer)value;
								iValue = k == 0 ? iValue + 1 : iValue - 1;
								newAssignment.add(new Eq<Integer>(v, iValue));
							} else if (value instanceof Double) {
								Double dValue = (Double)value;
								dValue += k == 0 ? dValue + 1.0 : dValue - 1.0;
								newAssignment.add(new Eq<Double>(v, dValue));
							} else {
								newAssignment.add(e);
							} 
						} else {
							newAssignment.add(assignment.get(j));
						}
					}
					
					newAssignments.add(newAssignment);
				}
			}
		}
		
		return newAssignments;
	}
	
	private boolean duplicateBreakpointValue(BreakpointValue bkpValues1, BreakpointValue bkpValues2) {
		double[] values1 = bkpValues1.getAllValues();
		double[] values2 = bkpValues2.getAllValues();
		
		if (values1.length != values2.length) {
			return false;
		}
		
		for (int j = 0; j < values1.length; j++) {
			if (values1[j] != values2[j]) {
				return false;
			}
		}
		
		return true;
	}
	
	private boolean duplicateListBreakpointValue(BreakpointValue bkpValues, List<BreakpointValue> bkpsValues, int index) {
		for (int i = index + 1; i < bkpsValues.size(); i++) {
			if (duplicateBreakpointValue(bkpValues, bkpsValues.get(i))) {
				return true;
			}
		}
		
		return false;
	}
	
	private List<BreakpointValue> simplify(List<BreakpointValue> bkpsValues) {
		List<BreakpointValue> newBkpsValues = new ArrayList<BreakpointValue>();
		
		for (int i = 0; i < bkpsValues.size(); i++) {
			if (!duplicateListBreakpointValue(bkpsValues.get(i), bkpsValues, i)) {
				newBkpsValues.add(bkpsValues.get(i));
			}
		}
		
		return newBkpsValues;
	}
	
}

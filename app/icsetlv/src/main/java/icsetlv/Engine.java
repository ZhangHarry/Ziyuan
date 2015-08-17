package icsetlv;

import icsetlv.common.dto.BkpInvariantResult;
import icsetlv.variable.TestcasesExecutor;

import java.util.ArrayList;
import java.util.List;

import libsvm.core.KernelType;
import libsvm.core.Machine;
import libsvm.core.MachineType;
import libsvm.core.Parameter;
import libsvm.extension.FeatureSelectionMachine;
import sav.strategies.dto.BreakPoint;
import sav.strategies.dto.BreakPoint.Variable;
import sav.strategies.vm.VMConfiguration;

/**
 * Wrapper class used to centralize the configuration and executing of test
 * cases then use the test results for SVM learning.
 * 
 * Usage:
 * <ol>
 * <li>new</li>
 * <li>configure
 * <ul>
 * <li>set up environment variables</li>
 * <li>define the list of breakpoints (class name + method name + code line
 * number)</li>
 * <li>define the list of test cases (passed + failed)</li>
 * </ul>
 * <li>run</li>
 * <ul>
 * <li>the test cases will be run and variables will be collected at the
 * configured breakpoints</li>
 * <li>test result is used to build SVM data points</li>
 * <li>SVM machine is setup + run for each breakpoint</li>
 * </ul>
 * <li>get results (i.e.: the predicates learned from the values at the
 * breakpoints)</li>
 * </ol>
 * 
 * @author Nguyen Phuoc Nguong Phuc (npn)
 * 
 */
public class Engine {
	private TestcasesExecutor testcaseExecutor;
	private VMConfiguration vmConfig = initVmConfig();
	private Machine machine = getDefaultMachine();
	private List<String> testcases = new ArrayList<String>();
	private List<BreakPoint> breakPoints = new ArrayList<BreakPoint>();

	public Engine reset() {
		return new Engine();
	}

	public List<BkpInvariantResult> run() throws Exception {
		InvariantMediator learner = new InvariantMediator();
		learner.setTcExecutor(getTestcaseExecutor());
		learner.setMachine(getMachine());
		return learner.learn(vmConfig, testcases, breakPoints);
	}	

	public void setMachine(Machine machine) {
		this.machine = machine;
	}

	private Machine getMachine() {
		return this.machine;
	}

	private Machine getDefaultMachine() {
//		final Machine machine = new PositiveSeparationMachine(new RandomNegativePointSelection());
		Machine machine = new FeatureSelectionMachine();
		return machine.setParameter(new Parameter().setMachineType(MachineType.C_SVC)
				.setKernelType(KernelType.LINEAR).setEps(0.00001).setUseShrinking(false)
				.setPredictProbability(false).setC(Double.MAX_VALUE));
	}
	
	private VMConfiguration initVmConfig() {
		return new VMConfiguration();
	}

	public Engine setJavaHome(final String javaHome) {
		vmConfig.setJavaHome(javaHome);
		return this;
	}

	public Engine setPort(final int portNumber) {
		vmConfig.setPort(portNumber);
		return this;
	}

	public Engine setDebug(final boolean debugEnabled) {
		vmConfig.setDebug(debugEnabled);
		return this;
	}

	public Engine addToClassPath(final String path) {
		vmConfig.addClasspath(path);
		return this;
	}
	
	public Engine addProgramArgument(String argument) {
		vmConfig.addProgramArgs(argument);
		return this;
	}

	public Engine addBreakPoint(final String className, final String methodName,
			final int lineNumber, final String... variableNames) {
		BreakPoint breakPoint = new BreakPoint(className, methodName, lineNumber);
		for (String variableName : variableNames) {
			breakPoint.addVars(new Variable(variableName));
		}
		
		return addBreakPoint(breakPoint);
	}
	
	public Engine addBreakPoint(BreakPoint breakPoint) {
		breakPoints.add(breakPoint);
		return this;
	}

	public Engine addTestcase(final String testcase) {
		testcases.add(testcase);
		return this;
	}
	
	public Engine addTestcases(final List<String> testcases) {
		for(String testcase: testcases){
			addTestcase(testcase);
		}
		return this;
	}
	
	public void setTestcaseExecutor(TestcasesExecutor testcaseExecutor) {
		this.testcaseExecutor = testcaseExecutor;
	}
	
	public TestcasesExecutor getTestcaseExecutor() {
		if (testcaseExecutor == null) {
			testcaseExecutor = new TestcasesExecutor(DefaultValues.DEBUG_VALUE_RETRIEVE_LEVEL);
		}
		return testcaseExecutor;
	}

}

package learntest.main;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jacop.core.Domain;
import org.jacop.floats.core.FloatIntervalDomain;

import icsetlv.DefaultValues;
import icsetlv.common.dto.BreakpointValue;
import japa.parser.ParseException;
import learntest.breakpoint.data.DecisionBkpsData;
import learntest.breakpoint.data.DecisionLocation;
import learntest.cfg.CfgHandlerAdapter;
import learntest.cfg.ICfgHandler;
import learntest.cfg.CfgHandlerAdapter.CfgAproach;
import learntest.exception.LearnTestException;
import learntest.sampling.JavailpSelectiveSampling;
import learntest.testcase.TestcasesExecutorwithLoopTimes;
import learntest.testcase.data.BreakpointData;
import learntest.testcase.data.BreakpointDataBuilder;
import learntest.util.LearnTestUtil;
import sav.common.core.SavException;
import sav.common.core.utils.CollectionUtils;
import sav.settings.SAVExecutionTimeOutException;
import sav.settings.SAVTimer;
import sav.strategies.dto.AppJavaClassPath;
import sav.strategies.dto.execute.value.ExecValue;
import sav.strategies.dto.execute.value.ExecVar;

public class LearnTest {
	
	private AppJavaClassPath appClassPath;
	private TestcasesExecutorwithLoopTimes tcExecutor;
	
	private BreakpointDataBuilder dtBuilder;
	
	public LearnTest(AppJavaClassPath appClassPath){
		this.appClassPath = appClassPath;
	}
	
	public void setTcExecutor(TestcasesExecutorwithLoopTimes tcExecutor) {
		this.tcExecutor = tcExecutor;
	}
	
	public RunTimeInfo run(boolean random) throws LearnTestException {
		 try {
			 LearnTestParams params = LearnTestParams.initFromLearnTestConfig();
			 params.setRandomDecision(random);
			return run(params);
		} catch (ParseException e) {
			throw new LearnTestException(e);
		} catch (IOException e) {
			throw new LearnTestException(e);
		} catch (ClassNotFoundException e) {
			throw new LearnTestException(e);
		} catch (SavException e) {
			throw new LearnTestException(e);
		}
	}
	
	public RunTimeInfo run(LearnTestParams params) throws LearnTestException, ParseException, IOException, SavException, ClassNotFoundException {
		SAVTimer.startCount();
		/* collect testcases in project */
		List<String> testcases = collectExistingTestcases(params.getTestClass());
		if (CollectionUtils.isEmpty(testcases)) {
			return null;
		}
		
		ICfgHandler cfgHandler = new CfgHandlerAdapter(appClassPath, params, CfgAproach.SOURCE_CODE_LEVEL);
		dtBuilder = new BreakpointDataBuilder(cfgHandler.getDecisionBkpsData());
		
		System.currentTimeMillis();
		
		long time = -1;
		double coverage = 0;
		int testCnt = 1;
		
		JavailpSelectiveSampling selectiveSampling = null;
		DecisionLearner learner = null;
		
		try{
			/**
			 * run testcases
			 */
			initTcExecutor(cfgHandler.getDecisionBkpsData());
			tcExecutor.setup(appClassPath, testcases);
			tcExecutor.run();
			Map<DecisionLocation, BreakpointData> result = tcExecutor.getResult();
			
			/* why return null if currentTestInputValues empty? this list is only for last breakpoint, 
			 * not for the whole testcase */
			if (CollectionUtils.isEmpty(tcExecutor.getCurrentTestInputValues())) {
				return null;
			}
			
			if (result.isEmpty()) {
				List<BreakpointValue> tests = tcExecutor.getCurrentTestInputValues();
				if (tests != null && !tests.isEmpty()) {
					BreakpointValue test = tests.get(0);
					Set<ExecVar> allVars = new HashSet<ExecVar>();
					collectExecVar(test.getChildren(), allVars);
					List<ExecVar> vars = new ArrayList<ExecVar>(allVars);
					List<BreakpointValue> list = new ArrayList<BreakpointValue>();
					list.add(test);
					
					/* GENERATE NEW TESTCASES */
					new TestGenerator().genTestAccordingToSolutions(getSolutions(list, vars), vars);
					System.out.println("Total test cases number: " + testCnt);
					coverage = 1;
				}
			} else {
				tcExecutor.setjResultFileDeleteOnExit(true);
				//tcExecutor.setSingleMode();
				tcExecutor.setInstrMode(true);
				//selectiveSampling = new JacopSelectiveSampling(tcExecutor);
				selectiveSampling = new JavailpSelectiveSampling(tcExecutor);
				selectiveSampling.addPrevValues(tcExecutor.getCurrentTestInputValues());
				learner = new DecisionLearner(selectiveSampling, cfgHandler, params.isRandomDecision());
				learner.learn(result);
				coverage = learner.getCoverage();
				
				try{
					List<Domain[]> domainList = getSolutions(learner.getRecords(), learner.getOriginVars());
					new TestGenerator().genTestAccordingToSolutions(domainList, learner.getOriginVars());					
				}catch(Exception e){}
				
				testCnt = selectiveSampling.getTotalNum();
				System.out.println("Total test cases number: " + testCnt);
			}
			
			time = SAVTimer.getExecutionTime();		
		} catch(SAVExecutionTimeOutException e){
			if (learner != null) {
				coverage = learner.getCoverage();
				List<Domain[]> domainList = getSolutions(learner.getRecords(), learner.getOriginVars());
				new TestGenerator().genTestAccordingToSolutions(domainList, learner.getOriginVars());
				testCnt = selectiveSampling.getTotalNum();
				System.out.println("Total test cases number: " + testCnt);
			}
			e.printStackTrace();
		}		
		
		RunTimeInfo info = new RunTimeInfo(time, coverage, testCnt);
		return info;
	}
	
	/*private List<Domain[]> getFullSolutions(List<BreakpointValue> records, List<ExecVar> originVars) {
		List<Domain[]> res = new ArrayList<Domain[]>();
		int size = originVars.size();
		for (BreakpointValue record : records) {
			Domain[] solution = new Domain[size + (size + 1) * size / 2];
			int i = 0;
			for (; i < size; i++) {
				double value = record.getValue(originVars.get(i).getLabel(), 0.0).doubleValue();
				solution[i] = new FloatIntervalDomain(value, value);
			}
			for(int j = 0; j < size; j ++) {
				double value = record.getValue(originVars.get(j).getLabel(), 0.0).doubleValue();
				for(int k = j; k < size; k ++) {
					double tmp = value * record.getValue(originVars.get(k).getLabel(), 0.0).doubleValue();
					solution[i ++] = new FloatIntervalDomain(tmp, tmp);
				}
			}
			res.add(solution);
		}
		return res;
	}*/
		
	private List<Domain[]> getSolutions(List<BreakpointValue> records, List<ExecVar> originVars) {
		List<Domain[]> res = new ArrayList<Domain[]>();
		int size = originVars.size();
		for (BreakpointValue record : records) {
			Domain[] solution = new Domain[size];
			for (int i = 0; i < size; i++) {
				double value = record.getValue(originVars.get(i).getLabel(), 0.0).doubleValue();
				solution[i] = new FloatIntervalDomain(value, value);
			}
			res.add(solution);
		}
		return res;
	}

	private List<String> collectExistingTestcases(String testClass) {
		org.eclipse.jdt.core.dom.CompilationUnit cu = LearnTestUtil.findCompilationUnitInProject(testClass);
		List<org.eclipse.jdt.core.dom.MethodDeclaration> mList = LearnTestUtil.findTestingMethod(cu);
		
		List<String> result = new ArrayList<String>();
		for(org.eclipse.jdt.core.dom.MethodDeclaration m: mList){
			String testcaseName = testClass + "." + m.getName();
			result.add(testcaseName);
		}
		
		return result;
	}
	
	private void initTcExecutor(DecisionBkpsData decisionBkpsData) {
		if (tcExecutor == null) {
			tcExecutor = new TestcasesExecutorwithLoopTimes(DefaultValues.DEBUG_VALUE_RETRIEVE_LEVEL);
		}
		tcExecutor.setBuilder(dtBuilder);
		tcExecutor.setDecisionBkpsData(decisionBkpsData);
	}	
	
	private void collectExecVar(List<ExecValue> vals, Set<ExecVar> vars) {
		if (CollectionUtils.isEmpty(vals)) {
			return;
		}
		for (ExecValue val : vals) {
			if (val == null || CollectionUtils.isEmpty(val.getChildren())) {
				String varId = val.getVarId();
				vars.add(new ExecVar(varId, val.getType()));
			}
			collectExecVar(val.getChildren(), vars);
		}
	}

}

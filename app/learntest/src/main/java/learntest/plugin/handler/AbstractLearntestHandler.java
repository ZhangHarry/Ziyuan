/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package learntest.plugin.handler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Queue;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cfgcoverage.jacoco.CfgJaCoCoConfigs;
import cfgcoverage.jacoco.analysis.data.CfgNode;
import icsetlv.common.dto.BreakpointValue;
import learntest.core.JDartLearntest;
import learntest.core.LearnTestParams;
import learntest.core.LearnTestParams.LearntestSystemVariable;
import learntest.core.RunTimeInfo;
import learntest.core.commons.data.LearnTestApproach;
import learntest.core.commons.data.classinfo.TargetClass;
import learntest.core.commons.data.classinfo.TargetMethod;
import learntest.core.commons.exception.LearnTestException;
import learntest.core.machinelearning.CfgNodeDomainInfo;
import learntest.core.machinelearning.FormulaInfo;
import learntest.plugin.LearnTestConfig;
import learntest.plugin.LearntestPlugin;
import learntest.plugin.commons.PluginException;
import learntest.plugin.export.io.excel.Trial;
import learntest.plugin.utils.IProjectUtils;
import learntest.plugin.utils.IResourceUtils;
import learntest.plugin.utils.IStatusUtils;
import learntest.plugin.utils.JdartConstants;
import learntest.plugin.utils.LearnTestUtil;
import sav.common.core.Constants;
import sav.common.core.ModuleEnum;
import sav.common.core.SavException;
import sav.common.core.SavRtException;
import sav.common.core.SystemVariables;
import sav.common.core.utils.CollectionUtils;
import sav.common.core.utils.TextFormatUtils;
import sav.settings.SAVTimer;
import sav.strategies.dto.AppJavaClassPath;

/**
 * @author LLT
 *
 */
public abstract class AbstractLearntestHandler extends AbstractHandler {
	private AppJavaClassPath appClasspath;
	private Logger log = LoggerFactory.getLogger(AbstractLearntestHandler.class);

	/* PLUGIN HANDLER */
	@Override
	public Object execute(ExecutionEvent arg0) throws ExecutionException {
		Job job = new Job(getJobName()) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					setup(LearnTestConfig.getINSTANCE().getProjectName());
					prepareData();
					execute(monitor);
				} catch (CoreException e) {
					e.printStackTrace();
				} finally {
					monitor.done();
				}
				return IStatusUtils.afterRunning(monitor);
			}

		};
		job.schedule();

		return null;
	}
	
	private void setup(String projectName) throws CoreException {
		LearntestPlugin.initLogger(projectName);
	}

	protected abstract IStatus execute(IProgressMonitor monitor) throws CoreException;

	protected abstract String getJobName();

	public void refreshProject() {
		IWorkspaceRoot myWorkspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		IProject iProject = myWorkspaceRoot.getProject(LearnTestConfig.getINSTANCE().getProjectName());

		try {
			iProject.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	protected void handleException(Exception e) {
		log.debug("error: {}", (Object) e.getStackTrace());
	}

	protected void prepareData() throws CoreException {
		appClasspath = initAppJavaClassPath();
	}

	public AppJavaClassPath getAppClasspath() {
		if (appClasspath == null) {
			appClasspath = initAppJavaClassPath();
		}
		return appClasspath;
	}

	private AppJavaClassPath initAppJavaClassPath() {
		try {
			IProject project = IProjectUtils.getProject(LearnTestConfig.getINSTANCE().getProjectName());
			IJavaProject javaProject = IProjectUtils.getJavaProject(project);
			
			AppJavaClassPath appClasspath = new AppJavaClassPath();
			appClasspath.setJavaHome(IProjectUtils.getJavaHome(javaProject));
			String outputPath = IProjectUtils.getTargetFolder(javaProject);
			appClasspath.setTarget(outputPath);
			appClasspath.setTestTarget(outputPath);
			String testSrc = IProjectUtils.getTestSourceFolder(javaProject);
			if (testSrc == null) {
				/* create test folder in target project */
				testSrc = IProjectUtils.createSourceFolder(javaProject, "test");
			}
			appClasspath.setTestSrc(testSrc);
			appClasspath.addClasspaths(IProjectUtils.getPrjectClasspath(javaProject));
			appClasspath.getPreferences().set(SystemVariables.PROJECT_CLASSLOADER,
					IProjectUtils.getPrjClassLoader(javaProject));
			appClasspath.getPreferences().set(SystemVariables.TESTCASE_TIMEOUT,
					Constants.DEFAULT_JUNIT_TESTCASE_TIMEOUT);
			appClasspath.getPreferences().set(CfgJaCoCoConfigs.DUPLICATE_FILTER, true);
			return appClasspath;
		} catch (CoreException ex) {
			throw new SavRtException(ex);
		} catch (PluginException e) {
			throw new SavRtException(e);
		}
	}

	protected LearnTestParams initLearntestParamsFromPreference() throws CoreException {
		return initLearntestParams(LearnTestConfig.getINSTANCE());
	}

	protected LearnTestParams initLearntestParams(LearnTestConfig config) throws CoreException {
		try {
			LearnTestParams params = new LearnTestParams();
			params.setApproach(config.isL2TApproach() ? LearnTestApproach.L2T : LearnTestApproach.RANDOOP);
			try {
				TargetMethod targetMethod = initTargetMethod(config);
				params.setTargetMethod(targetMethod);
			} catch (JavaModelException e) {
				throw new SavException(e, ModuleEnum.UNSPECIFIED, e.getMessage());
			}
			setSystemConfig(params);
			return params;
		} catch (SavException e) {
			throw new SavRtException(e);
		}
	}

	private static TargetMethod initTargetMethod(LearnTestConfig config) throws SavException, JavaModelException {
		TargetClass targetClass = new TargetClass(config.getTargetClassName());
		TargetMethod method = new TargetMethod(targetClass);
		method.setMethodName(config.getTargetMethodName());
		method.setLineNum(config.getMethodLineNumber());
		MethodDeclaration methodDeclaration = LearnTestUtil.findSpecificMethod(method.getClassName(),
				method.getMethodName(), method.getLineNum());
		method.setMethodSignature(LearnTestUtil.getMethodSignature(methodDeclaration));
		List<String> paramNames = new ArrayList<String>(CollectionUtils.getSize(methodDeclaration.parameters()));
		List<String> paramTypes = new ArrayList<String>(paramNames.size());
		for (Object obj : methodDeclaration.parameters()) {
			if (obj instanceof SingleVariableDeclaration) {
				SingleVariableDeclaration svd = (SingleVariableDeclaration) obj;
				paramNames.add(svd.getName().getIdentifier());
				paramTypes.add(svd.getType().toString());
			}
		}
		method.setParams(paramNames);
		method.setParamTypes(paramTypes);
		return method;
	}

	public void setSystemConfig(LearnTestParams params) throws CoreException {
		try {
			params.getSystemConfig().set(LearntestSystemVariable.JDART_APP_PROPRETIES,
					IResourceUtils.getResourceAbsolutePath(JdartConstants.BUNDLE_ID, "libs/jdart/jpf.properties"));
			params.getSystemConfig().set(LearntestSystemVariable.JDART_SITE_PROPRETIES,
					IResourceUtils.getResourceAbsolutePath(JdartConstants.BUNDLE_ID, "libs/jpf.properties"));
		} catch (PluginException e) {
			throw new CoreException(IStatusUtils.exception(e, e.getMessage()));
		}
	}

	/* END PLUGIN HANDLER */

	protected Trial evaluateLearntestForSingleMethod(LearnTestParams params) {
		try {
			
			log.info("");
			log.info("WORKING METHOD: {}, line {}", params.getTargetMethod().getMethodFullName(),
					params.getTargetMethod().getLineNum());
			log.info("-----------------------------------------------------------------------------------------------");

			// params.setMaxTcs(50);
			// l2t params
			LearnTestParams l2tParams = params;
			// randoop params
			LearnTestParams randoopParam = params.createNew();

			RunTimeInfo l2tAverageInfo = new RunTimeInfo();
			RunTimeInfo ranAverageInfo = new RunTimeInfo();
			RunTimeInfo jdartInfo = null;

			randoopParam.setApproach(LearnTestApproach.RANDOOP);
			log.info("run jdart..");
			jdartInfo = runJdart(randoopParam);

			l2tParams.setApproach(LearnTestApproach.L2T);
			l2tParams.setInitialTests(randoopParam.getInitialTests());
			l2tParams.setMaxTcs(ranAverageInfo.getTestCnt());
			log.info("run l2t..");
			runLearntest(l2tAverageInfo, l2tParams);

			randoopParam.setApproach(LearnTestApproach.RANDOOP);
			randoopParam.setInitialTests(l2tParams.getInitialTests());
			randoopParam.setMaxTcs(l2tAverageInfo.getTestCnt());
			log.info("run randoop..");
			runLearntest(ranAverageInfo, randoopParam);

			TargetMethod method = params.getTargetMethod();
			log.info("Result: ");
			log.info("lt2: {}", l2tAverageInfo);
			log.info("randoop: {}", ranAverageInfo);
			printLearnedFormulas(l2tAverageInfo.getLearnedFormulas(), l2tAverageInfo.getLogFile());
			setBetterBranch(l2tAverageInfo, ranAverageInfo);
			return new Trial(method.getMethodFullName(), method.getMethodLength(), method.getLineNum(), l2tAverageInfo,
					ranAverageInfo, jdartInfo);
		} catch (Exception e) {
			handleException(e);
		}
		return null;
	}

	private void setBetterBranch(RunTimeInfo l2tAverageInfo, RunTimeInfo ranAverageInfo) {
		StringBuffer sBuffer = new StringBuffer();
		sBuffer.append("l2t covered branch ============================= \n");
		sBuffer.append("true branch : \n");
		for (Entry<String, Collection<BreakpointValue>> entry : l2tAverageInfo.getTrueSample().entrySet()){
			sBuffer.append(entry.getKey()+" "+":"+entry.getValue().size()+" "+entry.getValue().toString()+"\n");
		}
		
		sBuffer.append("false branch : \n");
		for (Entry<String, Collection<BreakpointValue>> entry : l2tAverageInfo.getFalseSample().entrySet()){
			sBuffer.append(entry.getKey()+" "+":"+entry.getValue().size()+" "+entry.getValue().toString()+"\n");
		}
		
		sBuffer.append("ran covered branch ============================= \n");
		sBuffer.append("true branch : \n");
		for (Entry<String, Collection<BreakpointValue>> entry : ranAverageInfo.getTrueSample().entrySet()){
			sBuffer.append(entry.getKey()+" "+":"+entry.getValue().size()+" "+entry.getValue().toString()+"\n");
		}
		
		sBuffer.append("false branch : \n");
		for (Entry<String, Collection<BreakpointValue>> entry : ranAverageInfo.getFalseSample().entrySet()){
			sBuffer.append(entry.getKey()+" "+":"+entry.getValue().size()+" "+entry.getValue().toString()+"\n");
		}
		RunTimeInfo.write(l2tAverageInfo.getLogFile(), sBuffer.toString());

		HashMap<CfgNode, CfgNodeDomainInfo> domainMap = l2tAverageInfo.getDomainMap();
		StringBuffer l2tWorseSb = new StringBuffer(), ranWorseSb = new StringBuffer();
		HashMap<CfgNode, Boolean> visited = new HashMap<>();
		for (FormulaInfo info : l2tAverageInfo.getLearnedFormulas()){
			if (!visited.containsKey(info.getNode()) 
					&& info.getLearnedState() == info.VALID) {
				Queue<CfgNode> queue = new LinkedList<>();
				queue.add(info.getNode());
				recordBetterInfo(info.getNode(), domainMap,
						l2tWorseSb, ranWorseSb, info, ranAverageInfo, l2tAverageInfo);	
				while (!queue.isEmpty()) {
					CfgNode node = queue.poll();
					for (CfgNode dominatee : domainMap.get(node).getDominatees()){					
						if (!visited.containsKey(dominatee)) {
							recordBetterInfo(dominatee, domainMap, 
									l2tWorseSb, ranWorseSb, info, ranAverageInfo, l2tAverageInfo);		
							queue.add(dominatee);
						}				
					}
					visited.put(node, true);
				}
			}
		}
		l2tAverageInfo.l2tWorseThanRand += l2tWorseSb.toString();
		l2tAverageInfo.randWorseThanl2t += ranWorseSb.toString();
	}

	private void recordBetterInfo(CfgNode dominatee, HashMap<CfgNode,CfgNodeDomainInfo> domainMap, StringBuffer l2tWorseSb, StringBuffer ranWorseSb, 
			FormulaInfo info, RunTimeInfo ranAverageInfo, RunTimeInfo l2tAverageInfo) {
		StringBuffer sBuffer = new StringBuffer();
		String formula = info.getTrueFalseFormula().get(info.getTrueFalseFormula().size()-1);
		CfgNode ancient = info.getNode();
		List<CfgNode> dominators = domainMap.get(dominatee).getDominators();
				
		sBuffer.append("differen branch data ponits : =========================================\n");
		sBuffer.append(dominatee + ", ancient node : " + info.getNode()+"\n");
		Collection<BreakpointValue> ranF = ranAverageInfo.getFalseSample().get(dominatee.toString()), 
				ranT = ranAverageInfo.getTrueSample().get(dominatee.toString()),
				l2tF = l2tAverageInfo.getFalseSample().get(dominatee.toString()), 
				l2tT = l2tAverageInfo.getTrueSample().get(dominatee.toString());

		sBuffer.append("if ran better than l2t in false :"+"\n");
		if (checkIfBetter(ranF, l2tF, sBuffer)) {
			l2tWorseSb.append("false : "+dominatee+", ancient node : " + ancient +","+ formula);
			l2tWorseSb.append(", domainator nodes : " + dominators  +";");
		}


		sBuffer.append("if ran better than l2t in true :"+"\n");
		if (checkIfBetter(ranT, l2tT, sBuffer)) {
			l2tWorseSb.append("true : "+dominatee+", ancient node : " + ancient +","+ formula);
			l2tWorseSb.append(", domainator nodes : " + dominators  +";");
		}


		sBuffer.append("if l2t better than randoop in false :"+"\n");
		if (checkIfBetter(l2tF, ranF, sBuffer)) {
			ranWorseSb.append("false : "+dominatee+", ancient node : " + ancient +","+ formula);
			ranWorseSb.append(", domainator nodes : " + dominators +";");
		}


		sBuffer.append("if l2t better than randoop in true :"+"\n");
		if (checkIfBetter(l2tT, ranT, sBuffer)) {
			ranWorseSb.append("true : "+dominatee+", ancient node : " + ancient +","+ formula);
			ranWorseSb.append(", domainator nodes : " + dominators +";");
		}
		
		RunTimeInfo.write(l2tAverageInfo.getLogFile(), sBuffer.toString());
	}

	private boolean checkIfBetter(Collection<BreakpointValue> first, Collection<BreakpointValue> second, StringBuffer sBuffer) {

		if (null!=first && first.size() > 0 &&  
				(null == second || second.size()==0)) {
			log.info("size : "+first.size());
			sBuffer.append(first+"\n");
			return true;
		}
		return false;
		
	}

	private void printLearnedFormulas(List<FormulaInfo> list, String logFile) {
		StringBuffer sb = new StringBuffer();
		sb.append("learned formulas : =====================================");
		for (FormulaInfo formulaInfo : list) {
			sb.append(formulaInfo + "\n");
		}
		log.info(sb.toString());
		RunTimeInfo.write(logFile, sb.toString());
	}

	private RunTimeInfo runJdart(LearnTestParams params) throws Exception {
		JDartLearntest learntest = new JDartLearntest(getAppClasspath());
		return learntest.jdart(params);
	}

	private RunTimeInfo runLearntest(RunTimeInfo runInfo, LearnTestParams params) throws Exception {
		RunTimeInfo l2tInfo = runLearntest(params);
		if (runInfo != null && l2tInfo != null) {
			runInfo.add(l2tInfo);
		} else {
			runInfo = null;
		}
		return runInfo;
	}

	/**
	 * To test new version of learntest which uses another cfg and jacoco for
	 * code coverage.
	 */
	public RunTimeInfo runLearntest(LearnTestParams params) throws Exception {
		try {
			SAVTimer.enableExecutionTimeout = true;
			SAVTimer.exeuctionTimeout = 50000000;
			learntest.core.LearnTest learntest = new learntest.core.LearnTest(getAppClasspath());
			RunTimeInfo runtimeInfo = learntest.run(params);

			if (runtimeInfo != null) {
				if (runtimeInfo.getLineCoverageResult() != null) {
					log.info("Line coverage result:");
					log.info(runtimeInfo.getLineCoverageResult().getDisplayText());
				}
				log.info("{} RESULT:", StringUtils.upperCase(params.getApproach().getName()));
				log.info("TIME: {}; COVERAGE: {}; CNT: {}", TextFormatUtils.printTimeString(runtimeInfo.getTime()),
						runtimeInfo.getCoverage(), runtimeInfo.getTestCnt());
				log.info("TOTAL COVERAGE INFO: \n{}", runtimeInfo.getCoverageInfo());
			}
			return runtimeInfo;
		} catch (Exception e) {
			throw new LearnTestException(e);
		}
	}
}

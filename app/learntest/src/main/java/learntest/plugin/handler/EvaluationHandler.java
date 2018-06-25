package learntest.plugin.handler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import learntest.core.LearnTestParams;
import learntest.core.Visitor;
import learntest.core.commons.LearntestConstants;
import learntest.core.commons.data.classinfo.MethodInfo;
import learntest.local.timer.TimerTrialExcelHandler;
import learntest.plugin.LearnTestConfig;
import learntest.plugin.ProjectSetting;
import learntest.plugin.export.io.excel.MultiTrial;
import learntest.plugin.export.io.excel.Trial;
import learntest.plugin.export.io.excel.TrialExcelHandler;
import learntest.plugin.handler.filter.classfilter.ClassNameFilter;
import learntest.plugin.handler.filter.classfilter.ITypeFilter;
import learntest.plugin.handler.filter.classfilter.TestableClassFilter;
import learntest.plugin.handler.filter.methodfilter.IMethodFilter;
import learntest.plugin.handler.filter.methodfilter.MethodNameFilter;
import learntest.plugin.handler.filter.methodfilter.NestedBlockChecker;
import learntest.plugin.handler.filter.methodfilter.TestableMethodFilter;
import learntest.plugin.utils.IMethodUtils;
import learntest.plugin.utils.IProjectUtils;
import learntest.plugin.utils.IStatusUtils;
import learntest.plugin.utils.LearnTestUtil;
import sav.common.core.SavRtException;
import sav.common.core.utils.FileUtils;
import sav.common.core.utils.SingleTimer;
import sav.settings.SAVTimer;

public class EvaluationHandler extends AbstractLearntestHandler {
	private static Logger log = LoggerFactory.getLogger(EvaluationHandler.class);
	private static final int EVALUATIONS_PER_METHOD = 3;
	private static final int MAX_TRY_TIMES_PER_METHOD = 20;
	private List<IMethodFilter> methodFilters;
	private List<ITypeFilter> classFilters;
	private List<String> allPTValidMethods; // a list of valid methods whose all parameters and fields are primitive type
	private List<String> somePTValidMethods;// a list of valid methods who has any parameters or fields that is not primitive type

	static {
	}

	private int curMethodIdx = 0;

	@Override
	protected IStatus execute(IProgressMonitor monitor) {
		SingleTimer timer = SingleTimer.start("Evaluation all methods");
		curMethodIdx = 0;
		String projectName = LearnTestConfig.getInstance().getProjectName();
		final List<IPackageFragmentRoot> roots = IProjectUtils
				.getSourcePkgRoots(IProjectUtils.getJavaProject(projectName));
		TrialExcelHandler excelHandler = null;
		try {
			String outputFolder = ProjectSetting.getLearntestOutputFolder(projectName);
			log.info("learntest output folder: {}", outputFolder);
			excelHandler = new TimerTrialExcelHandler(outputFolder, projectName);
			initFilters();
		} catch (Exception e1) {
			handleException(e1);
			return Status.CANCEL_STATUS;
		}
		SAVTimer.enableExecutionTimeout = true;
		SAVTimer.exeuctionTimeout = 300000;
		RunTimeCananicalInfo overalInfo = new RunTimeCananicalInfo(0, 0, 0);
		try {
			for (IPackageFragmentRoot root : roots) {
				for (IJavaElement element : root.getChildren()) {
					if (element instanceof IPackageFragment) {
						RunTimeCananicalInfo info = runEvaluation((IPackageFragment) element, excelHandler, monitor);
						overalInfo.add(info);
					}
				}
			}
			log.info(overalInfo.toString());
			extractFilterInfo();
		} catch (JavaModelException e) {
			handleException(e);
		} catch (OperationCanceledException e) {
			timer.logResults(log);
			log.info(e.getMessage());
			return IStatusUtils.cancel();
		}
		timer.logResults(log);
		return Status.OK_STATUS;
	}

	/**
	 * log filter information
	 */
	private void extractFilterInfo() {
		for (ITypeFilter iTypeFilter : classFilters) {
			if (iTypeFilter instanceof TestableClassFilter) {
				TestableClassFilter filter = (TestableClassFilter)iTypeFilter;
				log.info("TestableClassFilter : ");
				log.info("ok : {}, interface : {}, abstract class : {}, not public class : {}", 
						filter.getOk().size(), 
						filter.getInterfaces().size(), 
						filter.getAbstracts().size(), 
						filter.getNotPublicClasses().size());
			}
		}
		
		for (IMethodFilter iMethodFilter : methodFilters) {
			if (iMethodFilter instanceof TestableMethodFilter) {
				TestableMethodFilter filter = (TestableMethodFilter) iMethodFilter;
				log.info("TestableMethodFilter : ");
				log.info("ok : {}, "
						+ "empty vars : {}, "
						+ "not public methods : {}, "
						+ "abstract methods : {}, "
						+ "native methods : {}, "
						+ "no primitive vars : {}, "
						+ "all primitive vars : {}, "
						+ "some primitive vars : {}", 
						filter.getOk().size(), 
						filter.getEmptyVars().size(),
						filter.getNotPublicMethods().size(), 
						filter.getAbstracts().size(), 
						filter.getNatives().size(), 
						filter.getNoPrimitiveVars().size(), 
						filter.getAllPrimitiveVars().size(),
						filter.getSomePrimitiveVars().size());
			}else if (iMethodFilter instanceof NestedBlockChecker) {
				NestedBlockChecker filter = (NestedBlockChecker) iMethodFilter;
				log.info("NestedBlockChecker : ");
				log.info("ok : {}, invalid nested block : {}", filter.getOk().size(), filter.getInvalid().size());
			}
		}
		
		log.info("all primitive vars : {}, some primitive vars : {}, ",
				allPTValidMethods.size(), somePTValidMethods.size());
		log.info("allPTValidMethods : ");
		for (String s : allPTValidMethods) {
			log.info(s);
		}
		log.info("somePTValidMethods : ");
		for (String s : somePTValidMethods) {
			log.info(s);
		}
	}

	private void initFilters() {
		methodFilters = new ArrayList<IMethodFilter>();
		methodFilters.add(new TestableMethodFilter());
		methodFilters.add(new NestedBlockChecker());
		methodFilters.add(new MethodNameFilter(LearntestConstants.EXCLUSIVE_METHOD_FILE_NAME, false));
		methodFilters.add(new MethodNameFilter(LearntestConstants.SKIP_METHOD_FILE_NAME, false));
//		methodFilters.add(new MethodNameFilter(LearntestConstants.CHECK_METHOD_FILE_NAME, true));// only reserve checked methods
		classFilters = Arrays.asList(new TestableClassFilter(), new ClassNameFilter(getExclusiveClasses(), false));
		allPTValidMethods = new LinkedList<>();
		somePTValidMethods = new LinkedList<>();
	}
	
	private List<String> getExclusiveClasses(){
		return Arrays.asList("org.jblas.ComplexFloatMatrix","org.jblas.SimpleBlas");
	}

	private RunTimeCananicalInfo runEvaluation(IPackageFragment pkg, TrialExcelHandler excelHandler,
			IProgressMonitor monitor) throws JavaModelException {
		RunTimeCananicalInfo info = new RunTimeCananicalInfo();
		for (IJavaElement javaElement : pkg.getChildren()) {
			if (javaElement instanceof IPackageFragment) {
				runEvaluation((IPackageFragment) javaElement, excelHandler, monitor);
			} else if (javaElement instanceof ICompilationUnit) {
				ICompilationUnit icu = (ICompilationUnit) javaElement;
				CompilationUnit cu = LearnTestUtil.convertICompilationUnitToASTNode(icu);
				boolean valid = true;
				for (ITypeFilter classFilter : classFilters) {
					if (!classFilter.isValid(cu)) {
						valid = false;
						continue;
					}
				}
				if (!valid) {
					continue;
				}
				TestableMethodCollector collector = new TestableMethodCollector(cu, methodFilters);
				cu.accept(collector);
				List<MethodInfo> validMethods = collector.getValidMethods();
				allPTValidMethods.addAll(collector.getAllPTValidMethods());
				somePTValidMethods.addAll(collector.getSomePTValidMethods());
				updateRuntimeInfo(info, cu, collector.getTotalMethodNum(), validMethods.size());
				evaluateForMethodList(excelHandler, validMethods, monitor, cu);
			}
		}
		log.info("package : {} ", pkg.getElementName());
		return info;
	}

	private void updateRuntimeInfo(RunTimeCananicalInfo info, CompilationUnit cu, int totalMethods, int validMethods) {
		int length0 = cu.getLineNumber(cu.getStartPosition() + cu.getLength() - 1);
		info.addTotalLen(length0);
		info.validNum += validMethods;
		info.totalNum += totalMethods;
	}

	protected void evaluateForMethodList(TrialExcelHandler excelHandler, List<MethodInfo> targetMethods,
			IProgressMonitor monitor, CompilationUnit cu) {
		if (targetMethods.isEmpty()) {
			return;
		}
		for (MethodInfo targetMethod : targetMethods) {			
			log.info("-----------------------------------------------------------------------------------------------");
			log.info("Method {}", ++curMethodIdx);
			MultiTrial multiTrial = new MultiTrial();
			multiTrial.setVarType(targetMethod.isVarType());
			int validTrial = 0;
			for (int i = 0; i < MAX_TRY_TIMES_PER_METHOD && validTrial < EVALUATIONS_PER_METHOD; i++) {
				checkJobCancelation(monitor);
				try {
					LearnTestParams params = initLearntestParams(targetMethod);
					Trial trial = evaluateLearntestForSingleMethod(params, cu);
					if (trial != null) {
						validTrial++;
						multiTrial.addTrial(trial);
						trial.setVarType(targetMethod.isVarType());
					}
				} catch (Exception e) {
					handleException(e);
				}
			}
			multiTrial.setAvgInfo();
			if (!multiTrial.isEmpty()) {
				try {
					excelHandler.export(multiTrial);
				} catch (Exception e) {
					handleException(e);
				}
			}
			logSuccessfulMethod(targetMethod);
		}
	}


	private void checkJobCancelation(IProgressMonitor monitor) {
		if (monitor != null && monitor.isCanceled()) {
			throw new OperationCanceledException("Operation cancelled!");
		}
	}

	private static void logSuccessfulMethod(MethodInfo targetMethod) {
		try {
			FileUtils.appendFile(LearntestConstants.EXCLUSIVE_METHOD_FILE_NAME,
					IMethodUtils.getMethodId(targetMethod.getMethodFullName(), targetMethod.getLineNum()) + "\n");
		} catch (SavRtException e) {
			// ignore
		}
	}

	private LearnTestParams initLearntestParams(MethodInfo targetMethod) throws CoreException {
		LearnTestParams params = new LearnTestParams(getAppClasspath(), targetMethod);
		setSystemConfig(params);
		return params;
	}

	class RunTimeCananicalInfo {
		int validNum;
		int totalNum;
		int totalLen;

		public RunTimeCananicalInfo() {

		}

		public RunTimeCananicalInfo(int validNum, int totalNum, int totalLen) {
			this.validNum = validNum;
			this.totalNum = totalNum;
			this.totalLen = totalLen;
		}

		public void add(RunTimeCananicalInfo info) {
			totalNum += info.totalNum;
			validNum += info.validNum;
			totalLen += info.totalLen;
		}

		public void addTotalLen(int totalLen) {
			this.totalLen += totalLen;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("total valid methods: ").append(validNum).append("\n").append("total methods: ").append(totalNum)
					.append("\n").append("total LOC: ").append(totalLen);
			return sb.toString();
		}
	}

	@Override
	protected String getJobName() {
		return "Do evaluation";
	}

}

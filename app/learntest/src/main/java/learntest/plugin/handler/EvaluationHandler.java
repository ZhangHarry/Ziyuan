package learntest.plugin.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;

import learntest.core.JDartLearntest;
import learntest.core.commons.data.classinfo.TargetMethod;
import learntest.io.excel.Trial;
import learntest.io.excel.TrialExcelHandler;
import learntest.main.LearnTestConfig;
import learntest.main.LearnTestParams;
import learntest.main.RunTimeInfo;
import learntest.plugin.handler.filter.classfilter.ClassNameFilter;
import learntest.plugin.handler.filter.classfilter.TargetClassFilter;
import learntest.plugin.handler.filter.classfilter.TestableClassFilter;
import learntest.plugin.handler.filter.methodfilter.MethodNameFilter;
import learntest.plugin.handler.filter.methodfilter.NestedBlockChecker;
import learntest.plugin.handler.filter.methodfilter.TargetMethodFilter;
import learntest.plugin.handler.filter.methodfilter.TestableMethodFilter;
import learntest.plugin.utils.IProjectUtils;
import learntest.util.LearnTestUtil;
import sav.common.core.utils.ClassUtils;
import sav.common.core.utils.PrimitiveUtils;
import sav.settings.SAVTimer;

public class EvaluationHandler extends AbstractLearntestHandler {
	private static final List<TargetMethodFilter> DEFAULT_METHOD_FILTERS;
	private static final List<TargetClassFilter> DEFAULT_CLASS_FILTERS;
	private List<TargetMethodFilter> methodFilters;
	private List<TargetClassFilter> classFilters;
	static {
		DEFAULT_METHOD_FILTERS = Arrays.asList(new TestableMethodFilter(),
				new NestedBlockChecker());
		DEFAULT_CLASS_FILTERS = Arrays.asList(new TestableClassFilter());
	}
	
	@Override
	protected IStatus execute(IProgressMonitor monitor) {
		final List<IPackageFragmentRoot> roots = IProjectUtils.findTargetSourcePkgRoots(LearnTestUtil.getJavaProject());
		TrialExcelHandler excelHandler = null;
		try {
			excelHandler = new TrialExcelHandler(LearnTestConfig.projectName);
			initFilters(excelHandler.readOldTrials());
		} catch (Exception e1) {
			handleException(e1);
			return Status.CANCEL_STATUS;
		}
		SAVTimer.enableExecutionTimeout = true;
		SAVTimer.exeuctionTimeout = 300000;
		RunTimeCananicalInfo overalInfo = new RunTimeCananicalInfo(0, 0, 0);
		try {
			for(IPackageFragmentRoot root: roots){
				for (IJavaElement element : root.getChildren()) {
					if (element instanceof IPackageFragment) {
						RunTimeCananicalInfo info = runEvaluation((IPackageFragment) element, excelHandler);
						overalInfo.add(info);
					}
				}
			}
			System.out.println(overalInfo.toString());
		} catch (JavaModelException e) {
			handleException(e);
		}
		return Status.OK_STATUS;
	}

	private void initFilters(Collection<Trial> oldTrials) {
		methodFilters = new ArrayList<TargetMethodFilter>(DEFAULT_METHOD_FILTERS);
		classFilters = new ArrayList<TargetClassFilter>(DEFAULT_CLASS_FILTERS);
		classFilters.add(new ClassNameFilter(getExcludedClasses()));
		// to excluded tested methods.
//		addMethodFilter(oldTrials);
	}

	@SuppressWarnings("unused")
	private void addMethodFilter(Collection<Trial> oldTrials) {
		if (oldTrials.isEmpty()) {
			return;
		}
		Set<String> methods = new HashSet<String>(oldTrials.size());
		for (Trial trial : oldTrials) {
			methods.add(MethodNameFilter.toMethodId(trial.getMethodName(), trial.getMethodStartLine()));
		}
		methodFilters.add(new MethodNameFilter(methods));
	}

	private List<String> getExcludedClasses() {
		/* TODO - temporary hard code */
		return Arrays.asList("org.apache.tools.ant.Main");
	}

	private RunTimeCananicalInfo runEvaluation(IPackageFragment pkg, TrialExcelHandler excelHandler)
			throws JavaModelException {
		RunTimeCananicalInfo info = new RunTimeCananicalInfo();
		for (IJavaElement javaElement : pkg.getChildren()) {
			if (javaElement instanceof IPackageFragment) {
				runEvaluation((IPackageFragment) javaElement, excelHandler);
			} else if (javaElement instanceof ICompilationUnit) {
				ICompilationUnit icu = (ICompilationUnit) javaElement;
				CompilationUnit cu = LearnTestUtil.convertICompilationUnitToASTNode(icu);
				for (TargetClassFilter classFilter : classFilters) {
					if (!classFilter.isValid(cu)) {
						continue;
					}
				}
				MethodCollector collector = new MethodCollector(cu);
				cu.accept(collector);
				updateRuntimeInfo(info, cu, collector);
				evaluateForMethodList(excelHandler, cu, collector.mdList);
			}
		}
		return info;
	}

	private void updateRuntimeInfo(RunTimeCananicalInfo info, CompilationUnit cu, MethodCollector collector) {
		int length0 = cu.getLineNumber(cu.getStartPosition() + cu.getLength() - 1);
		info.addTotalLen(length0);
		info.validNum += collector.mdList.size();
		info.totalNum += collector.totalMethodNum;
	}

	private void evaluateForMethodList(TrialExcelHandler excelHandler, CompilationUnit cu,
			List<MethodDeclaration> validMethods) {
		if (validMethods.isEmpty()) {
			return;
		}
		
		String className = LearnTestUtil.getFullNameOfCompilationUnit(cu);
		LearnTestConfig.targetClassName = className;
		for (MethodDeclaration method : validMethods) {
			TargetMethod targetMethod = initTargetMethod(className, cu, method);

			log("working method: " + targetMethod.getMethodFullName());
			try {
				LearnTestParams params = initLearntestParams(targetMethod);
				LearnTestParams l2tParams = params;
				LearnTestParams randoopParam = params.createNew();
				
				RunTimeInfo l2tAverageInfo = new RunTimeInfo();
				RunTimeInfo ranAverageInfo = new RunTimeInfo();
				
				log("run jdart..");
				LearnTestConfig.isL2TApproach = true; // TODO to remove
				l2tParams.setLearnByPrecond(true);
				RunTimeInfo jdartInfo = runJdart(l2tParams);
				
				log("run l2t..");
				runLearntest(l2tAverageInfo, l2tParams, true);
				
				log("run randoop..");
				runLearntest(ranAverageInfo, randoopParam, false);
				
				if (l2tAverageInfo.isNotZero() && ranAverageInfo.isNotZero()) {
					String fullMN = ClassUtils.toClassMethodStr(LearnTestConfig.targetClassName,
							LearnTestConfig.targetMethodName);
					int start = cu.getLineNumber(method.getStartPosition());
					int end = cu.getLineNumber(method.getStartPosition() + method.getLength());
					int length = end - start + 1;

					Trial trial = new Trial(fullMN, length, start, l2tAverageInfo, ranAverageInfo, jdartInfo);
					excelHandler.export(trial);
				}
			} catch (Exception e) {
				handleException(e);
			}

		}
	}
	
	private RunTimeInfo runJdart(LearnTestParams params) throws Exception {
		JDartLearntest learntest = new JDartLearntest(getAppClasspath());
		return learntest.jdart(params);
	}

	private LearnTestParams initLearntestParams(TargetMethod targetMethod) {
		LearnTestParams params = new LearnTestParams(targetMethod);
		setSystemConfig(params);
		return params;
	}

	private GenerateTestHandler testHandler = new GenerateTestHandler();
	private RunTimeInfo runLearntest(RunTimeInfo runInfo, LearnTestParams params, boolean l2tApproach) throws Exception {
		LearnTestConfig.isL2TApproach = l2tApproach;
		params.setLearnByPrecond(l2tApproach);
		RunTimeInfo l2tInfo = testHandler.runLearntest(params);
		if (runInfo != null && l2tInfo != null) {
			runInfo.add(l2tInfo);
		} else {
			runInfo = null;
		}
		Thread.sleep(5000);
		return runInfo;
	}

	class FieldAccessChecker extends ASTVisitor{
		boolean isFieldAccess = false;
		
		public boolean visit(SimpleName name){
			IBinding binding = name.resolveBinding();
			if(binding instanceof IVariableBinding){
				IVariableBinding vb = (IVariableBinding)binding;
				if(vb.isField()){
					if(vb.getType().isPrimitive()){
						isFieldAccess = true;						
					}
					
					if(vb.getType().isArray()){
						if(vb.getType().getElementType().isPrimitive()){
							isFieldAccess = true;
						}
					}
				}
			}
			
			return false;
		}
	}
	
	
	
	class DecisionStructureChecker extends ASTVisitor {

		private boolean isStructured = false;

		public boolean visit(IfStatement stat) {
			this.setStructured(true);
			return false;
		}

		public boolean visit(DoStatement stat) {
			this.setStructured(true);
			return false;
		}

		public boolean visit(EnhancedForStatement stat) {
			this.setStructured(true);
			return false;
		}

		public boolean visit(ForStatement stat) {
			this.setStructured(true);
			return false;
		}

		public boolean isStructured() {
			return isStructured;
		}

		public void setStructured(boolean isStructured) {
			this.isStructured = isStructured;
		}
	}
	
	class MethodCollector extends ASTVisitor {
		List<MethodDeclaration> mdList = new ArrayList<MethodDeclaration>();
		CompilationUnit cu;
		int totalMethodNum = 0;
		
		public MethodCollector(CompilationUnit cu){
			this.cu = cu;
		}
		
		public boolean visit(MethodDeclaration md) {
			boolean shouldTest = true;
			for (TargetMethodFilter filter : methodFilters) {
				if (!filter.isValid(cu, md)) {
					shouldTest = false;
				}
			}
			if (shouldTest) {
				mdList.add(md);
			}
			return false;
		}
		
		@SuppressWarnings("unused")
		private boolean containsAtLeastOnePrimitiveType(List<?> parameters){
			for (Object obj : parameters) {
				if (obj instanceof SingleVariableDeclaration) {
					SingleVariableDeclaration svd = (SingleVariableDeclaration) obj;
					Type type = svd.getType();
					
					if(type.isPrimitiveType()){
						return true;
					}
					
					if(type.isArrayType()){
						ArrayType aType = (ArrayType)type;
						if(aType.getElementType().isPrimitiveType()){
							return true;
						}
					}
				}

			}

			return false;
		}

		@SuppressWarnings("unused")
		private boolean containsAllPrimitiveType(List<?> parameters){
			for (Object obj : parameters) {
				if (obj instanceof SingleVariableDeclaration) {
					SingleVariableDeclaration svd = (SingleVariableDeclaration) obj;
					Type type = svd.getType();
					String typeString = type.toString();
					
					if(!PrimitiveUtils.isPrimitive(typeString) || svd.getExtraDimensions() > 0){
						return false;
					}
				}

			}

			return true;
		}
		
		
		@SuppressWarnings({ "rawtypes", "unused" })
		private boolean containsArrayOrString(List parameters) {
			for (Object obj : parameters) {
				if (obj instanceof SingleVariableDeclaration) {
					SingleVariableDeclaration svd = (SingleVariableDeclaration) obj;
					Type type = svd.getType();
					if (type.isArrayType() || type.toString().contains("String")) {
						return true;
					}
				}

			}

			return false;
		}
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
			sb.append("total valid methods: ").append(validNum).append("\n")
				.append("total methods: ").append(totalNum).append("\n")
				.append("total LOC: ").append(totalLen);
			return sb.toString();
		}
	}

	@Override
	protected String getJobName() {
		return "Do evaluation";
	}

}

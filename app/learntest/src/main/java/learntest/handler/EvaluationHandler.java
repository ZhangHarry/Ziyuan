package learntest.handler;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.WhileStatement;

import icsetlv.common.utils.PrimitiveUtils;
import learntest.io.excel.Trial;
import learntest.io.excel.TrialExcelHandler;
import learntest.main.LearnTestConfig;
import learntest.main.RunTimeInfo;
import learntest.util.LearnTestUtil;
import sav.common.core.utils.ClassUtils;
import sav.settings.SAVTimer;

public class EvaluationHandler extends AbstractLearntestHandler {

	@Override
	protected IStatus execute(IProgressMonitor monitor) {
		final List<IPackageFragmentRoot> roots = LearnTestUtil.findMainPackageRootInProjects();
		TrialExcelHandler excelHandler = null;
		try {
			excelHandler = new TrialExcelHandler(LearnTestConfig.projectName);
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

	private RunTimeCananicalInfo runEvaluation(IPackageFragment pkg, TrialExcelHandler excelHandler)
			throws JavaModelException {
		RunTimeCananicalInfo info = new RunTimeCananicalInfo();
		for (IJavaElement javaElement : pkg.getChildren()) {
			if (javaElement instanceof IPackageFragment) {
				runEvaluation((IPackageFragment) javaElement, excelHandler);
			} else if (javaElement instanceof ICompilationUnit) {
				ICompilationUnit icu = (ICompilationUnit) javaElement;
				CompilationUnit cu = LearnTestUtil.convertICompilationUnitToASTNode(icu);
				if (cu.types().isEmpty()) {
					continue;
				}
				AbstractTypeDeclaration type = (AbstractTypeDeclaration) cu.types().get(0);
				if (isInterfaceOrAbstractType(type)) {
					continue; 
				}
				
				MethodCollector collector = new MethodCollector(cu);
				cu.accept(collector);
				updateRuntimeInfo(info, cu, collector);
				evaluateForMethodList(excelHandler, cu, collector.mdList);
			}
		}
		return info;
	}

	private boolean isInterfaceOrAbstractType(AbstractTypeDeclaration type) {
		if (!(type instanceof TypeDeclaration)) {
			return false;
		}
		TypeDeclaration td = (TypeDeclaration) type;
		return td.isInterface() || Modifier.isAbstract(type.getFlags());
	}

	private void updateRuntimeInfo(RunTimeCananicalInfo info, CompilationUnit cu, MethodCollector collector) {
		int length0 = cu.getLineNumber(cu.getStartPosition() + cu.getLength() - 1);
		info.addTotalLen(length0);
		info.validNum += collector.mdList.size();
		info.totalNum += collector.totalMethodNum;
	}

	private void evaluateForMethodList(TrialExcelHandler excelHandler, CompilationUnit cu,
			List<MethodDeclaration> validMethods) {
		if (!validMethods.isEmpty()) {
			String className = LearnTestUtil.getFullNameOfCompilationUnit(cu);
			LearnTestConfig.targetClassName = className;
			for (MethodDeclaration method : validMethods) {
				String simpleMethodName = method.getName().getIdentifier();
				LearnTestConfig.targetMethodName = simpleMethodName;
				
				int lineNumber = cu.getLineNumber(method.getName().getStartPosition());
				LearnTestConfig.targetMethodLineNum = String.valueOf(lineNumber);

				System.out.println("working method: " + LearnTestConfig.targetClassName + "."
						+ LearnTestConfig.targetMethodName);

				try {
					int times = 1;
					
					RunTimeInfo l2tAverageInfo = new RunTimeInfo();
					RunTimeInfo ranAverageInfo = new RunTimeInfo();
					for (int i = 0; i < times; i++) {
						runLearntest(l2tAverageInfo, true);
						runLearntest(ranAverageInfo, false);
					}
					
					if (l2tAverageInfo.isNotZero() && ranAverageInfo.isNotZero()) {
						l2tAverageInfo.reduceByTimes(times);
						ranAverageInfo.reduceByTimes(times);
						String fullMN = ClassUtils.toClassMethodStr(LearnTestConfig.targetClassName,
								LearnTestConfig.targetMethodName);
						int start = cu.getLineNumber(method.getStartPosition());
						int end = cu.getLineNumber(method.getStartPosition() + method.getLength());
						int length = end - start + 1;

						Trial trial = new Trial(fullMN, l2tAverageInfo.getTime(), l2tAverageInfo.getCoverage(),
								l2tAverageInfo.getTestCnt(), ranAverageInfo.getTime(), ranAverageInfo.getCoverage(),
								ranAverageInfo.getTestCnt(), length, start);
						excelHandler.export(trial);
					}
				} catch (Exception e) {
					handleException(e);
					System.currentTimeMillis();
				} catch (java.lang.NoClassDefFoundError error){
					error.printStackTrace();
				}

			}

		}
	}

	private GenerateTestHandler testHandler = new GenerateTestHandler();
	private RunTimeInfo runLearntest(RunTimeInfo runInfo, boolean l2tApproach) throws InterruptedException {
		LearnTestConfig.isL2TApproach = l2tApproach;
		RunTimeInfo l2tInfo = testHandler.generateTest(l2tApproach);
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
	
	class ChildJudgeStatementChecker extends ASTVisitor {

		boolean containJudge = false;
		boolean containIfJudge = false;

		private Statement parentStatement;

		public ChildJudgeStatementChecker(Statement parentStat) {
			this.parentStatement = parentStat;
		}

		public boolean visit(DoStatement stat) {
			if (stat != parentStatement) {
				containJudge = true;
				return false;
			} else {
				return true;
			}
		}

		public boolean visit(EnhancedForStatement stat) {
			if (stat != parentStatement) {
				containJudge = true;
				return false;
			} else {
				return true;
			}
		}

		public boolean visit(ForStatement stat) {
			if (stat != parentStatement) {
				containJudge = true;
				return false;
			} else {
				return true;
			}
		}

		public boolean visit(IfStatement stat) {
			if (stat != parentStatement) {
				containJudge = true;
				containIfJudge = true;
				return false;
			} else {
				return true;
			}
		}

		public boolean visit(SwitchStatement stat) {
			if (stat != parentStatement) {
				containJudge = true;
				containIfJudge = true;
				return false;
			} else {
				return true;
			}
		}

		public boolean visit(WhileStatement stat) {
			if (stat != parentStatement) {
				containJudge = true;
				return false;
			} else {
				return true;
			}
		}
	}

	class NestedBlockChecker extends ASTVisitor {
		boolean isNestedJudge = false;

		private void checkChildNestCondition(Statement stat, boolean isParentLoop) {
			ChildJudgeStatementChecker checker = new ChildJudgeStatementChecker(stat);
			stat.accept(checker);
			if ((!isParentLoop && checker.containJudge) || (isParentLoop && checker.containIfJudge)) {
				isNestedJudge = true;
			}
		}

		public boolean visit(SwitchStatement stat) {
			if (isNestedJudge) {
				return false;
			}
			checkChildNestCondition(stat, false);

			return false;
		}

		public boolean visit(IfStatement stat) {
			if (isNestedJudge) {
				return false;
			}
			checkChildNestCondition(stat, false);

			return false;
		}

		public boolean visit(DoStatement stat) {
			if (isNestedJudge) {
				return false;
			}
			checkChildNestCondition(stat, true);
			return false;
		}

		public boolean visit(EnhancedForStatement stat) {
			if (isNestedJudge) {
				return false;
			}
			checkChildNestCondition(stat, true);
			return false;
		}

		public boolean visit(ForStatement stat) {
			if (isNestedJudge) {
				return false;
			}
			checkChildNestCondition(stat, true);
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
			if(md.isConstructor()){
				return false;
			}
			
			if (!md.parameters().isEmpty()) {
				boolean isPublic = false;
				for (Object obj : md.modifiers()) {
					if (obj instanceof Modifier) {
						Modifier modifier = (Modifier) obj;
						if (modifier.isPublic()) {
							isPublic = true;
						}
					}
				}

				if (isPublic) {
					totalMethodNum++;
					NestedBlockChecker checker = new NestedBlockChecker();
					md.accept(checker);
					if (checker.isNestedJudge) {
						FieldAccessChecker checker2 = new FieldAccessChecker();
						md.accept(checker2);
						
						//if(checker2.isFieldAccess){
							//if(containsAtLeastOnePrimitiveType(md.parameters())){
								mdList.add(md);								
							//}
						//}
					}

				}
			}

			return false;
		}
		
		public boolean containsAtLeastOnePrimitiveType(List parameters){
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

		public boolean containsAllPrimitiveType(List parameters){
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
		
		
		@SuppressWarnings("rawtypes")
		public boolean containsArrayOrString(List parameters) {
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

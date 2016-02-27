package microbat.util;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import sav.commons.TestConfiguration;
import sav.strategies.dto.AppJavaClassPath;


public class MicroBatUtil {
	
	public static String getProjectPath(){
		IWorkspaceRoot myWorkspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		IProject iProject = myWorkspaceRoot.getProject(Settings.projectName);
		
		String projectPath = iProject.getLocationURI().getPath();
//		projectPath = projectPath.substring(1, projectPath.length());
		projectPath = projectPath.replace("/", File.separator);
		
		return projectPath;
	}
	
	public static AppJavaClassPath constructClassPaths(){
		AppJavaClassPath appClassPath = new AppJavaClassPath();
		
		String projectPath = getProjectPath();
		IWorkspaceRoot myWorkspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		IProject iProject = myWorkspaceRoot.getProject(Settings.projectName);
		IJavaProject javaProject = JavaCore.create(iProject);
		
		/**
		 * setting depended jars into classpath
		 */
		String workspacePath = projectPath.substring(0, projectPath.indexOf(File.separator+Settings.projectName)); 
		for(IClasspathEntry classpathEntry: javaProject.readRawClasspath()){
			if(classpathEntry.getEntryKind()==IClasspathEntry.CPE_LIBRARY){
				String path = classpathEntry.getPath().toOSString();
				path = workspacePath + path;
				appClassPath.addClasspath(path);
			}
		}
		
		/**
		 * setting junit lib into classpath
		 */
		String userDir = System.getProperty("user.dir");
		String junitDir = userDir + File.separator + "dropins" + File.separator + "junit_lib";
		String junitPath = junitDir + File.separator + "junit.jar";
		String hamcrestCorePath = junitDir + File.separator + "org.hamcrest.core.jar";
		appClassPath.addClasspath(junitPath);
		appClassPath.addClasspath(hamcrestCorePath);
		
		/**
		 * setting output folder
		 */
		String outputFolder = "";
		try {
			for(String seg: javaProject.getOutputLocation().segments()){
				if(!seg.equals(Settings.projectName)){
					outputFolder += seg + File.separator;
				}
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		if(outputFolder.length() == 0){
			outputFolder = "bin";
		}
		
		
		String outputPath = projectPath + File.separator + outputFolder; 
		
		appClassPath.setJavaHome(TestConfiguration.getJavaHome());
		appClassPath.addClasspath(outputPath);
		appClassPath.setWorkingDirectory(projectPath);
		appClassPath.setLaunchClass(Settings.lanuchClass);
		
		return appClassPath;
		
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T asT(Object obj) {
		return (T) obj;
	}
	
	public static String combineTraceNodeExpression(String className, int lineNumber){
		String exp = className + " line:" + lineNumber;
		return exp;
	}
	
	/**
	 * For string1: a b c d
	 *     string2: a f c d
	 * The result is a c d
	 * @param nodeList1
	 * @param nodeList2
	 * @param comparator
	 * @return
	 */
	public static Object[] generateCommonNodeList(Object[] nodeList1,
			Object[] nodeList2) {
		int[][] commonLengthTable = buildLeveshteinTable(nodeList1, nodeList2);

		int commonLength = commonLengthTable[nodeList1.length][nodeList2.length];
		Object[] commonList = new Object[commonLength];

		for (int k = commonLength - 1, i = nodeList1.length, j = nodeList2.length; (i > 0 && j > 0);) {
			if (nodeList1[i - 1].equals(nodeList2[j - 1])) {
				commonList[k] = nodeList1[i - 1];
				k--;
				i--;
				j--;
			} else {
				if (commonLengthTable[i - 1][j] >= commonLengthTable[i][j - 1])
					i--;
				else
					j--;
			}
		}

		return commonList;
	}
	
	public static int[][] buildLeveshteinTable(Object[] nodeList1, Object[] nodeList2){
		int[][] commonLengthTable = new int[nodeList1.length + 1][nodeList2.length + 1];
		for (int i = 0; i < nodeList1.length + 1; i++)
			commonLengthTable[i][0] = 0;
		for (int j = 0; j < nodeList2.length + 1; j++)
			commonLengthTable[0][j] = 0;

		for (int i = 1; i < nodeList1.length + 1; i++)
			for (int j = 1; j < nodeList2.length + 1; j++) {
				if (nodeList1[i - 1].equals(nodeList2[j - 1]))
					commonLengthTable[i][j] = commonLengthTable[i - 1][j - 1] + 1;
				else {
					commonLengthTable[i][j] = (commonLengthTable[i - 1][j] >= commonLengthTable[i][j - 1]) ? commonLengthTable[i - 1][j]
							: commonLengthTable[i][j - 1];
				}

			}
		
		return commonLengthTable;
	}
	
//	/**
//	 * @param variable1
//	 * @param variable2
//	 * @return
//	 */
//	public static boolean isTheSameVariable(InterestedVariable v1, InterestedVariable v2) {
//		VarValue var1 = v1.getVariable();
//		VarValue var2 = v2.getVariable();
//		
//		if(var1 instanceof ReferenceValue && var2 instanceof ReferenceValue){
//			ReferenceValue rv1 = (ReferenceValue)var1;
//			ReferenceValue rv2 = (ReferenceValue)var2;
//			if(rv1.getReferenceID() == rv2.getReferenceID()){
//				return true;
//			}
//		}
//		/**
//		 * Otherwise, it means var1 and var2 should be primitive variable, and they are either
//		 * local variable or field.
//		 */
//		else if(!(var1 instanceof ReferenceValue) && !(var2 instanceof ReferenceValue)){
//			if(var1.getVarName().equals(var2.getVarName())){
//				
//				if(var1.isLocalVariable() && var2.isLocalVariable()){
//					boolean isEqualRange = isEqualRange(var1, v1, var2, v2);
//					return isEqualRange;
//				}
//				else if(var1.isField() && var2.isField()){
//					ReferenceValue parent1 = (ReferenceValue)var1.getParents().get(0);
//					ReferenceValue parent2 = (ReferenceValue)var2.getParents().get(0);
//					
//					if(parent1.getReferenceID() == parent2.getReferenceID()){
//						return true;
//					}
//				}
//				
//			}
//		}
//		
//		return false;
//	}
//
//
//	private static boolean isEqualRange(VarValue var1, InterestedVariable v1, VarValue var2, InterestedVariable v2) {
//		String varID = var1.getVariablePath();
//		for(LocalVariableScope lvs: Settings.localVariableScopes.getVariableScopes()){
//			if(varID.equals(lvs.getVariableName())){
//				boolean isRootVar1InScope = lvs.getStartLine() <= v1.getLineNumber() && lvs.getEndLine() >= v1.getLineNumber();
//				boolean isRootVar2InScope = lvs.getStartLine() <= v2.getLineNumber() && lvs.getEndLine() >= v2.getLineNumber();
//				
//				return isRootVar1InScope && isRootVar2InScope;
//			}
//		}
//		
//		return false;
//	}
}

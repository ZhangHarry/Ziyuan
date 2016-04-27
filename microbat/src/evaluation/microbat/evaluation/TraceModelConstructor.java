package microbat.evaluation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import microbat.codeanalysis.ast.LocalVariableScope;
import microbat.codeanalysis.ast.VariableScopeParser;
import microbat.codeanalysis.bytecode.MicrobatByteCodeAnalyzer;
import microbat.codeanalysis.runtime.ProgramExecutor;
import microbat.model.BreakPoint;
import microbat.model.trace.Trace;
import microbat.util.Settings;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdi.TimeoutException;

import sav.common.core.SavException;
import sav.commons.TestConfiguration;
import sav.strategies.dto.AppJavaClassPath;

public class TraceModelConstructor {
	/**
	 * Take care of project name, buggy class name, buggy line number, ...
	 */
	private void setup(){
		//TODO
	}
	
	public Trace constructTraceModel(AppJavaClassPath appClassPath, List<BreakPoint> executingStatements)
			throws TimeoutException{
		
		setup();
		
		ProgramExecutor tcExecutor = new ProgramExecutor();
		
		/** 1. clear some static common variables **/
		clearOldData();
		
		
		/** 2. parse read/written variables**/
		MicrobatByteCodeAnalyzer slicer = new MicrobatByteCodeAnalyzer(executingStatements);
		List<BreakPoint> runningStatements = null;
		try {
			System.out.println("start analyzing byte code ...");
			runningStatements = slicer.parsingBreakPoints(appClassPath);
			System.out.println("finish analyzing byte code ...!");
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
		/**
		 * 3. find the variable scope for:
		 * 1) Identifying the same local variable in different trace nodes.
		 * 2) Generating variable ID for local variable.
		 */
		List<String> classScope = parseScope(runningStatements);
		parseLocalVariables(classScope);
		
		/** 4. extract runtime variables*/
		tcExecutor.setConfig(appClassPath);
		try {
			tcExecutor.run(runningStatements, new SubProgressMonitor(new NullProgressMonitor(), 0));
		} catch (SavException e) {
			e.printStackTrace();
		}
		
		/** 5. construct dominance relation*/
		Trace trace = tcExecutor.getTrace();
		trace.constructDomianceRelation();
		
		return trace;
	}
	
	
	private void parseLocalVariables(final List<String> classScope) {
		VariableScopeParser vsParser = new VariableScopeParser();
		vsParser.parseLocalVariableScopes(classScope);
		List<LocalVariableScope> lvsList = vsParser.getVariableScopeList();
		Settings.localVariableScopes.setVariableScopes(lvsList);
	}
	
	private List<String> parseScope(List<BreakPoint> breakpoints) {
		List<String> classes = new ArrayList<>();
		for(BreakPoint bp: breakpoints){
			if(!classes.contains(bp.getClassCanonicalName())){
				classes.add(bp.getClassCanonicalName());
			}
		}
		return classes;
	}
	
	private void clearOldData(){
		Settings.interestedVariables.clear();
		Settings.localVariableScopes.clear();
		Settings.potentialCorrectPatterns.clear();
	}
	
	private AppJavaClassPath constructClassPaths(){
		IWorkspaceRoot myWorkspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		IProject iProject = myWorkspaceRoot.getProject(Settings.projectName);
		String projectPath = iProject.getLocationURI().getPath();
		projectPath = projectPath.substring(1, projectPath.length());
		projectPath = projectPath.replace("/", File.separator);
		
		String binPath = projectPath + File.separator + "bin"; 
		
		AppJavaClassPath appClassPath = new AppJavaClassPath();
		appClassPath.setJavaHome(TestConfiguration.getJavaHome());
		
		appClassPath.addClasspath(binPath);
		appClassPath.setWorkingDirectory(projectPath);
		
		return appClassPath;
		
	}
}

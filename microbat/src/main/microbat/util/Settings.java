package microbat.util;

import java.util.HashMap;
import java.util.Stack;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.CompilationUnit;

import microbat.Activator;
import microbat.codeanalysis.ast.LocalVariableScopes;
import microbat.handler.CheckingState;
import microbat.model.UserInterestedVariables;
import microbat.model.trace.PotentialCorrectPatternList;
import microbat.preference.MicrobatPreference;
import microbat.views.ImageUI;

public class Settings {
	
	
	public static String projectName;
	public static String lanuchClass;
	
	public static String buggyClassName;
	public static String buggyLineNumber;
	
	public static boolean isRecordSnapshot;
	public static int stepLimit;
	
	public static int distribtionLayer = 3;
	public static ImageUI imageUI = new ImageUI();
	
	/**
	 * The portion remains in a trace node when propagating suspiciousness. 
	 */
	public static double remainingRate = 0.5;
	
	static{
		if(Activator.getDefault() != null){
			try{
				projectName = Activator.getDefault().getPreferenceStore().getString(MicrobatPreference.TARGET_PORJECT);
				lanuchClass = Activator.getDefault().getPreferenceStore().getString(MicrobatPreference.LANUCH_CLASS);
				buggyClassName = Activator.getDefault().getPreferenceStore().getString(MicrobatPreference.CLASS_NAME);
				buggyLineNumber = Activator.getDefault().getPreferenceStore().getString(MicrobatPreference.LINE_NUMBER);
				String isRecord = Activator.getDefault().getPreferenceStore().getString(MicrobatPreference.RECORD_SNAPSHORT);
				isRecordSnapshot = isRecord.equals("true");
				String limitNumString = Activator.getDefault().getPreferenceStore().getString(MicrobatPreference.STEP_LIMIT);
				stepLimit = Integer.valueOf(limitNumString);
				if(stepLimit == 0){
					stepLimit = 5000;
				}
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
	public static PotentialCorrectPatternList potentialCorrectPatterns = new PotentialCorrectPatternList();
	
	/**
	 * the variables checked by user as wrong.
	 */
	public static UserInterestedVariables interestedVariables = new UserInterestedVariables();
	
	/**
	 * This variable is to trace whether the variables in different lines are the same
	 * local variable.
	 */
	public static LocalVariableScopes localVariableScopes = new LocalVariableScopes();

	/**
	 * This stack allow user to undo his checking operations.
	 */
	public static Stack<CheckingState> checkingStateStack = new Stack<>();
	
	/**
	 * The following two map is used to trade space for time.
	 */
	public static HashMap<String, CompilationUnit> compilationUnitMap = new HashMap<>();
	public static HashMap<String, ICompilationUnit> iCompilationUnitMap = new HashMap<>();
}

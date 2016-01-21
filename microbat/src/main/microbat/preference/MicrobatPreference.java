package microbat.preference;

import microbat.Activator;
import microbat.util.Settings;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class MicrobatPreference extends PreferencePage implements
		IWorkbenchPreferencePage {

	public MicrobatPreference() {
	}

	public MicrobatPreference(String title) {
		super(title);
	}

	public MicrobatPreference(String title, ImageDescriptor image) {
		super(title, image);
	}

	@Override
	public void init(IWorkbench workbench) {
		this.defaultTargetProject = Activator.getDefault().getPreferenceStore().getString(TARGET_PORJECT);
		this.defaultClassName = Activator.getDefault().getPreferenceStore().getString(CLASS_NAME);
		this.defaultLineNumber = Activator.getDefault().getPreferenceStore().getString(LINE_NUMBER);
	}
	
	public static final String TARGET_PORJECT = "targetProjectName";
	public static final String CLASS_NAME = "className";
	public static final String LINE_NUMBER = "lineNumber";

	
	private Combo projectCombo;
	private Text classNameText;
	private Text lineNumberText;
	
	private String defaultTargetProject = "";
	private String defaultClassName = "";
	private String defaultLineNumber = "";
	
	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		
		composite.setLayout(layout);
		
		Label projectLabel = new Label(composite, SWT.NONE);
		projectLabel.setText("Target Project");
		
		projectCombo = new Combo(composite, SWT.BORDER);
		projectCombo.setItems(getProjectsInWorkspace());
		projectCombo.setText(this.defaultTargetProject);
		GridData comboData = new GridData(SWT.FILL, SWT.FILL, true, false);
		comboData.horizontalSpan = 2;
		projectCombo.setLayoutData(comboData);
		
		createSeedStatementGroup(composite);
		
		return composite;
	}
	
	private void createSeedStatementGroup(Composite parent){
		Group seedStatementGroup = new Group(parent, SWT.NONE);
		seedStatementGroup.setText("Seed statement");
		GridData seedStatementGroupData = new GridData(SWT.FILL, SWT.FILL, true, true);
		seedStatementGroupData.horizontalSpan = 3;
		seedStatementGroup.setLayoutData(seedStatementGroupData);
		
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		
		seedStatementGroup.setLayout(layout);
		
		Label classNameLabel = new Label(seedStatementGroup, SWT.NONE);
		classNameLabel.setText("Class Name: ");
		classNameText = new Text(seedStatementGroup, SWT.BORDER);
		classNameText.setText(this.defaultClassName);
		GridData classNameTextData = new GridData(SWT.FILL, SWT.FILL, true, false);
		classNameTextData.horizontalSpan = 2;
		classNameText.setLayoutData(classNameTextData);
		
		Label lineNumberLabel = new Label(seedStatementGroup, SWT.NONE);
		lineNumberLabel.setText("Line Number: ");
		lineNumberText = new Text(seedStatementGroup, SWT.BORDER);
		lineNumberText.setText(this.defaultLineNumber);
		GridData lineNumTextData = new GridData(SWT.FILL, SWT.FILL, true, false);
		lineNumTextData.horizontalSpan = 2;
		lineNumberText.setLayoutData(lineNumTextData);
	}
	
	public boolean performOk(){
		IEclipsePreferences preferences = ConfigurationScope.INSTANCE.getNode("Clonepedia");
		preferences.put(TARGET_PORJECT, this.projectCombo.getText());
		preferences.put(CLASS_NAME, this.classNameText.getText());
		preferences.put(LINE_NUMBER, this.lineNumberText.getText());
		
		Activator.getDefault().getPreferenceStore().putValue(TARGET_PORJECT, this.projectCombo.getText());
		Activator.getDefault().getPreferenceStore().putValue(CLASS_NAME, this.classNameText.getText());
		Activator.getDefault().getPreferenceStore().putValue(LINE_NUMBER, this.lineNumberText.getText());
		
		confirmChanges();
		
		return true;
		
	}
	
	private void confirmChanges(){
		Settings.projectName = this.projectCombo.getText();
		Settings.buggyClassName = this.classNameText.getText();
		Settings.buggyLineNumber = this.lineNumberText.getText();
	}
	
	private String[] getProjectsInWorkspace(){
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		IProject[] projects = root.getProjects();
		
		String[] projectStrings = new String[projects.length];
		for(int i=0; i<projects.length; i++){
			projectStrings[i] = projects[i].getName();
		}
		
		return projectStrings;
	}

}

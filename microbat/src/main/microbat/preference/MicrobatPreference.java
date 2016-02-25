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
import org.eclipse.swt.widgets.Button;
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
		this.defaultLanuchClass = Activator.getDefault().getPreferenceStore().getString(LANUCH_CLASS);
		this.defaultRecordSnapshot = Activator.getDefault().getPreferenceStore().getString(RECORD_SNAPSHORT);
	}
	
	public static final String TARGET_PORJECT = "targetProjectName";
	public static final String CLASS_NAME = "className";
	public static final String LINE_NUMBER = "lineNumber";
	public static final String LANUCH_CLASS = "lanuchClass";
	public static final String RECORD_SNAPSHORT = "recordSnapshot";

	
	private Combo projectCombo;
	private Text lanuchClassText;
	private Text classNameText;
	private Text lineNumberText;
	private Button recordSnapshotButton;
	
	private String defaultTargetProject = "";
	private String defaultLanuchClass = "";
	private String defaultClassName = "";
	private String defaultLineNumber = "";
	private String defaultRecordSnapshot = "true";
	
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
		
		Label lanuchClassLabel = new Label(seedStatementGroup, SWT.NONE);
		lanuchClassLabel.setText("Lanuch Class: ");
		lanuchClassText = new Text(seedStatementGroup, SWT.BORDER);
		lanuchClassText.setText(this.defaultLanuchClass);
		GridData lanuchClassTextData = new GridData(SWT.FILL, SWT.FILL, true, false);
		lanuchClassTextData.horizontalSpan = 2;
		lanuchClassText.setLayoutData(lanuchClassTextData);
		
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
		
		recordSnapshotButton = new Button(seedStatementGroup, SWT.CHECK);
		recordSnapshotButton.setText("Record Snapshot");
		GridData recordButtonData = new GridData(SWT.FILL, SWT.FILL, true, false);
		recordButtonData.horizontalSpan = 3;
		recordSnapshotButton.setLayoutData(recordButtonData);
		boolean selected = this.defaultRecordSnapshot.equals("true");
		recordSnapshotButton.setSelection(selected);
	}
	
	public boolean performOk(){
		IEclipsePreferences preferences = ConfigurationScope.INSTANCE.getNode("microbat.preference");
		preferences.put(TARGET_PORJECT, this.projectCombo.getText());
		preferences.put(LANUCH_CLASS, this.lanuchClassText.getText());
		preferences.put(CLASS_NAME, this.classNameText.getText());
		preferences.put(LINE_NUMBER, this.lineNumberText.getText());
		preferences.put(RECORD_SNAPSHORT, String.valueOf(this.recordSnapshotButton.getSelection()));
		
		Activator.getDefault().getPreferenceStore().putValue(TARGET_PORJECT, this.projectCombo.getText());
		Activator.getDefault().getPreferenceStore().putValue(LANUCH_CLASS, this.lanuchClassText.getText());
		Activator.getDefault().getPreferenceStore().putValue(CLASS_NAME, this.classNameText.getText());
		Activator.getDefault().getPreferenceStore().putValue(LINE_NUMBER, this.lineNumberText.getText());
		Activator.getDefault().getPreferenceStore().putValue(RECORD_SNAPSHORT, String.valueOf(this.recordSnapshotButton.getSelection()));
		
		confirmChanges();
		
		return true;
		
	}
	
	private void confirmChanges(){
		Settings.projectName = this.projectCombo.getText();
		Settings.lanuchClass = this.lanuchClassText.getText();
		Settings.buggyClassName = this.classNameText.getText();
		Settings.buggyLineNumber = this.lineNumberText.getText();
		Settings.isRecordSnapshot = this.recordSnapshotButton.getSelection();
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

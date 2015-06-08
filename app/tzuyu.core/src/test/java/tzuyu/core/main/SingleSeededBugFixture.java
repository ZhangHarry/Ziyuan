package tzuyu.core.main;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.PropertyConfigurator;

import sav.commons.TestConfiguration;
import faultLocalization.FaultLocalizationReport;
import faultLocalization.LineCoverageInfo;
import fit.TimedActionFixture;

public class SingleSeededBugFixture extends TimedActionFixture {
	protected static final String LINE_FEED = "<br/>";
	// Parameters
	protected List<String> programClasses = new ArrayList<String>();
	protected List<String> programTestClasses = new ArrayList<String>();
	protected boolean useSlicer = true;
	private String expectedBugLine;

	// Results
	protected FaultLocalizationReport report;
	protected double maxSuspiciousness = -1.0;
	private double foundLineSuspiciousness = -1.0;

	private SystemConfiguredDataProvider context = new SystemConfiguredDataProvider();
	private TzuyuCore program;
	
	/* set up log4j to use test configuration */
	static {
		PropertyConfigurator.configure(TestConfiguration
				.getTestResources("sav.commons") + "/test-log4j.properties");
	}
	
	public void projectClassPath(final String path) throws FileNotFoundException {
		context.addProjectClassPath(path);
	}
	
	public void projectClassPaths(List<String> paths) throws FileNotFoundException {
		for (String path : paths) {
			context.addProjectClassPath(path);
		}
	}

	protected TzuyuCore getProgram() {
		if (program == null) {
			program = new TzuyuCore(context, context.getAppData());
		}
		return program;
	}

	public void javaHome(final String path) {
		context.setJavaHome(path);
	}

	public void tracerJarPath(final String path) {
		context.setTracerJarPath(path);
	}

	public void programClass(final String clazz) {
		programClasses.add(clazz);
	}

	public void programTestClass(final String clazz) {
		programTestClasses.add(clazz);
	}

	public void expectedBugLine(final String line) {
		expectedBugLine = line;
	}

	public void useSlicer(final boolean useSlicer) {
		this.useSlicer = useSlicer;
	}

	public final boolean analyze() throws Exception {
		report = getProgram().faultLocalization(programClasses, programTestClasses, useSlicer);
		checkAnalyzedResults();
		return true;
	}
	
	public final boolean analyze2(List<String> testingPackages) throws Exception {
		report = getProgram().faultLocalization2(programClasses,
				testingPackages, programTestClasses, useSlicer);
		checkAnalyzedResults();
		return true;
	}

	protected void checkAnalyzedResults() {
		for (LineCoverageInfo info : report.getLineCoverageInfos()) {
			if (maxSuspiciousness < info.getSuspiciousness()) {
				maxSuspiciousness = info.getSuspiciousness();
			}
			if (StringUtils.isNotEmpty(expectedBugLine)
					&& expectedBugLine.equals(info.getLocation().getId())) {
				foundLineSuspiciousness = info.getSuspiciousness();
			}
		}
	}

	public boolean hasNoFailedTest() {
		return report.getCoverageReport().getFailTests() == null
				|| report.getCoverageReport().getFailTests().size() <= 0;
	}

	public boolean bugWasFound() {
		return foundLineSuspiciousness > 0;
	}

	public boolean foundBugHasMaxSuspiciousness() {
		return bugWasFound() && foundLineSuspiciousness == maxSuspiciousness;
	}

	protected double getSmallestSuspiciousnessInTopThree() {
		int i = 0;
		double min = 2;
		for (LineCoverageInfo info : report.getLineCoverageInfos()) {
			if (info.getSuspiciousness() < min) {
				min = info.getSuspiciousness();
				i++;
			}
			if (i >= 3) {
				break;
			}
		}
		return min;
	}

	public boolean foundBugIsInTopThree() {
		return foundLineSuspiciousness >= getSmallestSuspiciousnessInTopThree();
	}

	public void show() throws Exception {
		final Object result = method(0).invoke(getActor());
		cells.last().more = td(result.toString());
	}

	public String lineCoverageInfo() {
		final StringBuilder builder = new StringBuilder();
		if (report.getLineCoverageInfos() == null || report.getLineCoverageInfos().size() == 0) {
			builder.append("No line coverage information was detected.");
		} else {
			for (LineCoverageInfo info : report.getLineCoverageInfos()) {
				if (info.getSuspiciousness() > 0.0) {
					builder.append(info.toString()).append(LINE_FEED);
				}
			}
		}
		return builder.toString();
	}

	public void setContext(SystemConfiguredDataProvider context) {
		this.context = context;
	}

	public SystemConfiguredDataProvider getContext() {
		return context;
	}
	
	public FaultLocalizationReport getReport() {
		return report;
	}
}

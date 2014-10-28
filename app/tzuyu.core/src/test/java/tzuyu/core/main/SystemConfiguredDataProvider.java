package tzuyu.core.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import sav.common.core.utils.ConfigUtils;
import codecoverage.jacoco.JavaCoCo;
import faultLocalization.SuspiciousnessCalculator.SuspiciousnessCalculationAlgorithm;

/**
 * Data provider which gets configurations from System properties.
 * 
 * @author Nguyen Phuoc Nguong Phuc (phuc@sutd.edu.sg)
 * 
 */
public class SystemConfiguredDataProvider extends TestApplicationContext {
	private static final String TZUYU_ALGO = "TZUYU_ALGO";
	private static final String JAVA_HOME = "java.home";
	private static final String JAVA_CLASS_FILE_EXTENSION = ".class";
	private static final String JAVA_JAR_FILE_EXTENSION = ".jar";
	private String javaHome;
	private String tracerJarPath;
	
	@Override
	protected void initClasspath() {
		// do nothing
	}
	
	public void addProjectClassPath(final String path) throws FileNotFoundException {
		File folder = new File(path);
		if (!folder.exists()) {
			throw new FileNotFoundException("The path " + path + " does not exist.");
		}
		if (isClassFile(folder)) {
			// Only 1 file is provided
			// That file must contain both program code + test code
			projectClasspath.add(folder.getParent());
		} else if (folder.isDirectory() || isJarFile(folder)) {
			// A directory or a JAR file is provided
			// Scan the directory for classes and mark them as either program
			// code or test code
			projectClasspath.add(folder.getAbsolutePath());
		} else {
			throw new UnsupportedOperationException("The path " + path
					+ " is neither a .class/.jar file nor a directory.");
		}
	}

	private boolean isClassFile(File file) {
		return file.isFile() && file.getName().endsWith(JAVA_CLASS_FILE_EXTENSION);
	}

	private boolean isJarFile(File file) {
		return file.isFile() && file.getName().endsWith(JAVA_JAR_FILE_EXTENSION);
	}

	@Override
	public List<String> getProjectClasspath() {
		return projectClasspath;
	}

	public void setTracerJarPath(final String path) {
		tracerJarPath = path;
	}
	
	@Override
	protected String getTracerJarPath() {
		return tracerJarPath;
	}

	public void setJavaHome(final String path) {
		javaHome = path;
	}
	
	@Override
	protected String getJavahome() {
		return StringUtils.isNotBlank(javaHome) ? javaHome : ConfigUtils.getProperty(JAVA_HOME);
	}

	@Override
	public SuspiciousnessCalculationAlgorithm getSuspiciousnessCalculationAlgorithm() {
		final String algo = ConfigUtils.getProperty(TZUYU_ALGO);
		if (StringUtils.isNotBlank(algo)) {
			return SuspiciousnessCalculationAlgorithm.valueOf(algo);
		} else {
			return SuspiciousnessCalculationAlgorithm.TARANTULA;
		}
	}

	public List<String> getProjectClassPath() {
		return projectClasspath;
	}
	
	public JavaCoCo getJaCoco() {
		return (JavaCoCo)codeCoverageTool;
	}
}

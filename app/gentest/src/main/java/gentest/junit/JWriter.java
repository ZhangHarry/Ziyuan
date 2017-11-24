/**
 * Copyright TODO
 */
package gentest.junit;

import gentest.core.data.Sequence;
import gentest.core.data.statement.Statement;
import gentest.junit.CompilationUnitBuilder.MethodBuilder;
import gentest.junit.variable.VariableNamer;
import japa.parser.ast.CompilationUnit;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author LLT
 *
 */
public class JWriter implements ICompilationUnitWriter {
	protected static final String JUNIT_TEST_ANNOTATION = "Test";
	protected static final String DEPRECATION_SUPPRESS_WARNING = "SuppressWarnings(\"deprecation\")";
	protected static final String JUNIT_TEST_ANNOTATION_IMPORT = "org.junit.Test";
	protected static final String JUNIT_ASSERT_CLAZZ = "org.junit.Assert";
	private String clazzSimpleName;
	private String packageName;
	private String methodPrefix;
	protected Set<String> duplicateImports;
	
	public JWriter() {
		duplicateImports = new HashSet<String>();
	}
	
	@Override
	public CompilationUnit write(List<Sequence> methods, String pkgName, String classSimpleName, String methodPrefix) {
		setPackageName(pkgName);
		setClazzName(classSimpleName);
		setMethodPrefix(methodPrefix);
		return write(methods);
	}
	
	public CompilationUnit write(List<Sequence> methods) {
		VariableNamer varNamer = new VariableNamer();
		CompilationUnitBuilder cu = new CompilationUnitBuilder();
		/* package */
		cu.pakage(getPackageName());
		/* import */
		cu.imports(JUNIT_TEST_ANNOTATION_IMPORT);
		duplicateImports.clear();
		for (Sequence method : methods) {
			duplicateImports.addAll(cu.imports(method.getDeclaredTypes()));
		}
		cu.startType(getClazzName())
			.markAnnotation(DEPRECATION_SUPPRESS_WARNING);
		AstNodeConverter astConverter = new AstNodeConverter(varNamer, duplicateImports);
		for (int i = 0; i < methods.size(); i++) {
			Sequence method = methods.get(i);
			varNamer.reset(method);
			MethodBuilder methodBuilder = cu.startMethod(getMethodName(i));
			methodBuilder.throwException(Throwable.class.getSimpleName());
			methodBuilder.markAnnotation(JUNIT_TEST_ANNOTATION);
			if (!method.getStatementByType(
					Statement.RStatementKind.EVALUATION_METHOD).isEmpty()) {
				cu.imports(JUNIT_ASSERT_CLAZZ);
			}
			for (Statement stmt : method.getStmts()) {
				astConverter.reset();
				stmt.accept(astConverter);
				methodBuilder.statement(astConverter.getResult());
			}
			methodBuilder.endMethod();
		}
		return cu.endType().getResult();
	}

	protected String getMethodName(int sequenceIdx) {
		return getMethodPrefix() + (sequenceIdx + 1);
	}

	public String getClazzName() {
		return clazzSimpleName;
	}

	public void setClazzName(String clazzName) {
		this.clazzSimpleName = clazzName;
	}

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public String getMethodPrefix() {
		return methodPrefix;
	}

	public void setMethodPrefix(String methodPrefix) {
		this.methodPrefix = methodPrefix;
	}
}

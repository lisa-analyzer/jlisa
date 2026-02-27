package it.unive.jlisa.frontend.visitors;

import it.unive.jlisa.frontend.ParserContext;
import java.util.Map;

import it.unive.jlisa.frontend.ParsingEnvironment;
import it.unive.lisa.analysis.nonrelational.Environment;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class BaseUnitASTVisitor extends JavaASTVisitor {

	protected String pkg;

	/**
	 * Map from simple names to fully qualified names
	 */
	protected Map<String, String> imports;

	public BaseUnitASTVisitor(
			ParsingEnvironment environment,
			String pkg,
			Map<String, String> imports) {
		super(environment);
		this.pkg = pkg;
		this.imports = imports;
	}

	public String getPackage() {
		return pkg != null ? pkg + "." : "";
	}

}
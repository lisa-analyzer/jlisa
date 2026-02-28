package it.unive.jlisa.frontend.visitors;

import it.unive.jlisa.frontend.ParsingEnvironment;
import java.util.Map;

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
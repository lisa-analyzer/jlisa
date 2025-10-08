package it.unive.jlisa.frontend.visitors;

import it.unive.jlisa.frontend.ParserContext;
import java.util.Map;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class BaseUnitASTVisitor extends JavaASTVisitor {

	protected String pkg;

	/**
	 * Map from simple names to fully qualified names
	 */
	protected Map<String, String> imports;

	public BaseUnitASTVisitor(
			ParserContext parserContext,
			String source,
			String pkg,
			Map<String, String> imports,
			CompilationUnit compilationUnit) {
		super(parserContext, source, compilationUnit);
		this.pkg = pkg;
		this.imports = imports;
	}

	public String getPackage() {
		return pkg != null ? pkg + "." : "";
	}

}
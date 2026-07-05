package it.unive.jlisa.frontend;

import it.unive.lisa.program.SourceCodeLocation;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;

public final class ParsingEnvironment {

	private final ParserContext parserContext;
	private final String source;
	private final CompilationUnit astUnit;
	private final CompilationUnit[] allAstUnits;

	public ParsingEnvironment(
			ParserContext parserContext,
			String source,
			CompilationUnit astUnit) {
		this(parserContext, source, astUnit, null);
	}

	public ParsingEnvironment(
			ParserContext parserContext,
			String source,
			CompilationUnit astUnit,
			CompilationUnit[] allAstUnits) {
		this.parserContext = parserContext;
		this.source = source;
		this.astUnit = astUnit;
		this.allAstUnits = allAstUnits;
	}

	public ParserContext parserContext() {
		return parserContext;
	}

	public String source() {
		return source;
	}

	public CompilationUnit astUnit() {
		return astUnit;
	}

	public CompilationUnit[] allAstUnits() {
		return allAstUnits;
	}

	public SourceCodeLocation getSourceCodeLocation(
			ASTNode node) {
		int startPos = node.getStartPosition();
		return parserContext().getLocationManager(source(), astUnit().getLineNumber(startPos),
				astUnit().getColumnNumber(startPos)).getCurrentLocation();
	}
}
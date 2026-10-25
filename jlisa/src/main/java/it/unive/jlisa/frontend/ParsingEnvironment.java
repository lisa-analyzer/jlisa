package it.unive.jlisa.frontend;

import it.unive.jlisa.program.JavaLocation;
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

	/**
	 * Returns the location of the given node, from its first to its last
	 * character.
	 *
	 * @param node the node
	 *
	 * @return the location
	 */
	public JavaLocation getSourceCodeLocation(
			ASTNode node) {
		int start = node.getStartPosition();
		int end = node.getLength() > 0 ? start + node.getLength() - 1 : start;
		return new JavaLocation(source,
				astUnit.getLineNumber(start), astUnit.getColumnNumber(start),
				astUnit.getLineNumber(end), astUnit.getColumnNumber(end));
	}
}
package it.unive.jlisa.frontend.visitors;

import it.unive.jlisa.frontend.ParserContext;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class BaseCodeElementASTVisitor extends JavaASTVisitor {

	protected BaseUnitASTVisitor container;

	public BaseCodeElementASTVisitor(
			ParserContext parserContext,
			String source,
			CompilationUnit compilationUnit,
			BaseUnitASTVisitor container) {
		super(parserContext, source, compilationUnit);
		this.container = container;
	}

}
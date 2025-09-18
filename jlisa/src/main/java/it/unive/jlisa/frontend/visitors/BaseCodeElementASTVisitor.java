package it.unive.jlisa.frontend.visitors;

import org.eclipse.jdt.core.dom.CompilationUnit;

import it.unive.jlisa.frontend.ParserContext;

public class BaseCodeElementASTVisitor extends JavaASTVisitor {
    
    protected BaseUnitASTVisitor container;

	public BaseCodeElementASTVisitor(ParserContext parserContext, String source, CompilationUnit compilationUnit, BaseUnitASTVisitor container) {
        super(parserContext, source, compilationUnit);
        this.container = container;
    }

}
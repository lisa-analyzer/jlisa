package it.unive.jlisa.frontend.visitors;

import it.unive.jlisa.frontend.ParsingEnvironment;

public class BaseCodeElementASTVisitor extends JavaASTVisitor {

	protected BaseUnitASTVisitor container;

	public BaseCodeElementASTVisitor(
			ParsingEnvironment environment,
			BaseUnitASTVisitor container) {
		super(environment);
		this.container = container;
	}

}
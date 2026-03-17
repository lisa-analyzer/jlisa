package it.unive.jlisa.frontend.visitors;

import it.unive.jlisa.frontend.ParsingEnvironment;
import it.unive.jlisa.frontend.visitors.scope.Scope;

public abstract class ScopedVisitor<S extends Scope> extends JavaASTVisitor {
	S scope;

	protected ScopedVisitor(
			ParsingEnvironment env,
			S scope) {
		super(env);
		this.scope = scope;
	}

	public S getScope() {
		return scope;
	}
}
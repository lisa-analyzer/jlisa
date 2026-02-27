package it.unive.jlisa.frontend.visitors.scope;

public interface EnclosableScope<S extends Scope> {

    S getParentScope();
}

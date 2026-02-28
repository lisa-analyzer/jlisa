package it.unive.jlisa.frontend.visitors;

public interface ContextedVisitor<C> {
	C getContext();
}
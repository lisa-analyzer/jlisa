package it.unive.jlisa.frontend.visitors.scope;

import it.unive.jlisa.frontend.util.JavaLocalVariableTracker;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.util.frontend.ControlFlowTracker;

public final class ClassScope extends Scope implements EnclosableScope<ClassScope> {
	private final JavaClassType enclosingClass; // null if top-level
	private final UnitScope unitScope;
	private final ClassScope parentScope;
	CompilationUnit lisaUnit;

	public ClassScope(
			UnitScope unitScope,
			ClassScope parentScope,
			JavaClassType enclosingClass,
			CompilationUnit lisaUnit) {
		this.parentScope = parentScope;
		this.enclosingClass = enclosingClass;
		this.unitScope = unitScope;
		this.lisaUnit = lisaUnit;
	}

	public JavaClassType getEnclosingClass() {
		return enclosingClass;
	}

	public UnitScope unitScope() {
		return unitScope;
	}

	public MethodScope toMethodScope(
			CFG cfg,
			JavaLocalVariableTracker tracker,
			ControlFlowTracker flowTracker) {
		return new MethodScope(this, cfg, tracker, flowTracker);
	}

	public CompilationUnit getLiSACompilationUnit() {
		return this.lisaUnit;
	}

	public UnitScope getUnitScope() {
		return this.unitScope;
	}

	@Override
	public ClassScope getParentScope() {
		return parentScope;
	}
}
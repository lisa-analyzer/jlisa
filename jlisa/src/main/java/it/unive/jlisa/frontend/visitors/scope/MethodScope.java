package it.unive.jlisa.frontend.visitors.scope;

import it.unive.jlisa.frontend.util.JavaLocalVariableTracker;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.util.frontend.ControlFlowTracker;

public final class MethodScope extends Scope implements EnclosableScope<ClassScope> {
	private final ClassScope parentScope;
	private CFG cfg;
	private JavaLocalVariableTracker tracker;
	private ControlFlowTracker flowTracker;
	private Expression switchItem;

	public MethodScope(
			ClassScope parentScope,
			CFG cfg,
			JavaLocalVariableTracker tracker,
			ControlFlowTracker controlFlowTracker) {
		this.parentScope = parentScope;
		this.cfg = cfg;
		this.tracker = tracker;
		this.flowTracker = controlFlowTracker;
	}

	@Override
	public ClassScope getParentScope() {
		return this.parentScope;
	}

	public CFG getCFG() {
		return this.cfg;
	}

	public JavaLocalVariableTracker getTracker() {
		return this.tracker;
	}

	public ControlFlowTracker getControlFlowTracker() {
		return this.flowTracker;
	}

	public Expression getSwitchItem() {
		return this.switchItem;
	}

	public void setSwitchItem(
			Expression switchItem) {
		this.switchItem = switchItem;
	}
}
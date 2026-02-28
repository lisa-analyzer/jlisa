package it.unive.jlisa.frontend.visitors.contexts;

import it.unive.jlisa.frontend.ParserContext;
import it.unive.jlisa.frontend.util.JavaLocalVariableTracker;
import it.unive.jlisa.frontend.visitors.BaseUnitASTVisitor;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.util.frontend.ControlFlowTracker;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class CodeElementVisitorContext extends VisitorContext {
	protected final BaseUnitASTVisitor container;
	protected final CFG cfg;
	protected final JavaLocalVariableTracker tracker;
	protected final ControlFlowTracker control;

	public CodeElementVisitorContext(
			ParserContext parserContext,
			String source,
			CompilationUnit compilationUnit,
			BaseUnitASTVisitor container,
			CFG cfg,
			JavaLocalVariableTracker tracker,
			ControlFlowTracker control) {
		super(parserContext, source, compilationUnit);
		this.container = container;
		this.cfg = cfg;
		this.tracker = tracker;
		this.control = control;
	}

	public BaseUnitASTVisitor getContainer() {
		return container;
	}

	public CFG getCfg() {
		return cfg;
	}

	public JavaLocalVariableTracker getTracker() {
		return tracker;
	}

	public ControlFlowTracker getControl() {
		return control;
	}
}
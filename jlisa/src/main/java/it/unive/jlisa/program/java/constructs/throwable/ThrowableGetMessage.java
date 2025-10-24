package it.unive.jlisa.program.java.constructs.throwable;

import it.unive.jlisa.program.type.*;
import it.unive.lisa.analysis.*;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.*;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.PushAny;

public class ThrowableGetMessage extends UnaryExpression implements PluggableStatement {
	protected Statement originating;

	public ThrowableGetMessage(
			CFG cfg,
			CodeLocation location,
			Expression expr) {
		super(cfg, location, "getMessage", expr);
	}

	public static it.unive.jlisa.program.java.constructs.throwable.ThrowableGetMessage build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new it.unive.jlisa.program.java.constructs.throwable.ThrowableGetMessage(cfg, location, params[0]);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

	@Override
	public void setOriginatingStatement(
			Statement st) {
		originating = st;
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdUnarySemantics(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression expression,
			StatementStore<A> expressions)
			throws SemanticException {
		return interprocedural.getAnalysis().smallStepSemantics(state,
				new PushAny(getProgram().getTypes().getStringType(), getLocation()),
				originating);
	}
}

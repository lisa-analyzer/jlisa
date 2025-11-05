package it.unive.jlisa.program.java.constructs.doublew;

import it.unive.jlisa.program.operator.JavaDoubleIsNaNOperator;
import it.unive.jlisa.program.type.JavaBooleanType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.*;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.InstrumentedReceiver;

public class DoubleIsNaN extends UnaryExpression implements PluggableStatement {
	protected Statement originating;

	public DoubleIsNaN(
			CFG cfg,
			CodeLocation location,
			Expression expr) {
		super(cfg, location, "isNaN", expr);
	}

	public static DoubleIsNaN build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new DoubleIsNaN(cfg, location, params[0]);
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
	public <A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> AnalysisState<A> fwdUnarySemantics(
					InterproceduralAnalysis<A, D> interprocedural,
					AnalysisState<A> state,
					SymbolicExpression expr,
					StatementStore<A> expressions)
					throws SemanticException {
		InstrumentedReceiver receiver = new InstrumentedReceiver(JavaBooleanType.INSTANCE, false, getLocation());
		it.unive.lisa.symbolic.value.UnaryExpression un = new it.unive.lisa.symbolic.value.UnaryExpression(
				JavaBooleanType.INSTANCE,
				expr,
				JavaDoubleIsNaNOperator.INSTANCE,
				getLocation());
		return interprocedural.getAnalysis().assign(state, receiver, un, originating);
	}
}

package it.unive.jlisa.program.java.constructs.random;

import it.unive.jlisa.program.type.JavaLongType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.UnaryExpression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.PushAny;

public  class NextLong extends UnaryExpression implements PluggableStatement {
	protected Statement originating;

	public NextLong(CFG cfg, CodeLocation location, Expression param) {
		super(cfg, location, "nextLong", param);
	}

	public static NextLong build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new NextLong(cfg, location, params[0]);
	}

	@Override
	protected int compareSameClassAndParams(Statement o) {
		return 0; 
	}


	@Override
	public <A extends AbstractLattice<A>,
		D extends AbstractDomain<A>> AnalysisState<A> fwdUnarySemantics(InterproceduralAnalysis<A, D> interprocedural, AnalysisState<A> state, SymbolicExpression expr, StatementStore<A> expressions) throws SemanticException {
		return interprocedural.getAnalysis().smallStepSemantics(state, new PushAny(JavaLongType.INSTANCE, getLocation()), originating);
	}

	@Override
	public void setOriginatingStatement(Statement st) {
		originating = st;
	}
}

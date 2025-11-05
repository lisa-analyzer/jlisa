package it.unive.jlisa.program.java.constructs.integer;

import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.type.JavaClassType;
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
import it.unive.lisa.symbolic.SymbolicExpression;

public class IntegerValueOf extends it.unive.lisa.program.cfg.statement.UnaryExpression implements PluggableStatement {
	protected Statement originating;

	public IntegerValueOf(
			CFG cfg,
			CodeLocation location,
			Expression expr) {
		super(cfg, location, "valueOf", JavaClassType.getIntegerWrapperType(), expr);
	}

	public static IntegerValueOf build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new IntegerValueOf(cfg, location, params[0]);
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
			SymbolicExpression expr,
			StatementStore<A> expressions)
			throws SemanticException {
		JavaClassType intWrapper = JavaClassType.getIntegerWrapperType();
		JavaNewObj wrap = new JavaNewObj(
				getCFG(),
				getLocation(),
				intWrapper.getReference(),
				new Expression[] { getSubExpression() });

		AnalysisState<A> sem = wrap.forwardSemantics(state, interprocedural, expressions);
		getMetaVariables().addAll(wrap.getMetaVariables());
		return sem;
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}
}

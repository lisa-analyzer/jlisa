package it.unive.jlisa.program.java.constructs.strictmath;

import it.unive.jlisa.program.operator.JavaStrictMathGetExponentOperator;
import it.unive.jlisa.program.type.JavaIntType;
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
import it.unive.lisa.symbolic.value.UnaryExpression;

public class StrictMathGetExponent extends it.unive.lisa.program.cfg.statement.UnaryExpression
		implements
		PluggableStatement {
	protected Statement originating;

	public StrictMathGetExponent(
			CFG cfg,
			CodeLocation location,
			Expression arg) {
		super(cfg, location, "strictgetExponent", arg);
	}

	public static StrictMathGetExponent build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new StrictMathGetExponent(cfg, location, params[0]);
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
		UnaryExpression expo = new UnaryExpression(
				JavaIntType.INSTANCE,
				expr,
				JavaStrictMathGetExponentOperator.INSTANCE,
				getLocation());

		return interprocedural.getAnalysis().smallStepSemantics(state, expo, originating);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}
}

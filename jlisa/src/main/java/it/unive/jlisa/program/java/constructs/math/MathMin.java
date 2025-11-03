package it.unive.jlisa.program.java.constructs.math;

import it.unive.jlisa.program.operator.JavaMathMin;
import it.unive.jlisa.program.type.JavaDoubleType;
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
import it.unive.lisa.symbolic.value.BinaryExpression;

public class MathMin extends it.unive.lisa.program.cfg.statement.BinaryExpression implements PluggableStatement {
	protected Statement originating;

	public MathMin(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression right) {
		super(cfg, location, "min", left, right);
	}

	public static MathMin build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new MathMin(cfg, location, params[0], params[1]);
	}

	@Override
	public void setOriginatingStatement(
			Statement st) {
		originating = st;
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdBinarySemantics(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression left,
			SymbolicExpression right,
			StatementStore<A> expressions)
			throws SemanticException {
		BinaryExpression max = new BinaryExpression(
				JavaDoubleType.INSTANCE,
				left,
				right,
				JavaMathMin.INSTANCE,
				getLocation());

		return interprocedural.getAnalysis().smallStepSemantics(state, max, originating);
	}
}

package it.unive.jlisa.program.cfg.expression;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.comparison.LessOrEqual;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;

/**
 * The less-or-equal comparison of Java, unboxing its operands that are wrapper objects
 * (see {@link JavaUnboxing}).
 */
public class JavaComparisonLE extends LessOrEqual {

	/**
	 * Builds the comparison.
	 *
	 * @param cfg      the cfg that this expression belongs to
	 * @param location the location where this expression is defined
	 * @param left     the left operand
	 * @param right    the right operand
	 */
	public JavaComparisonLE(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression right) {
		super(cfg, location, left, right);
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
		AnalysisState<A> unboxed = JavaUnboxing.compare(interprocedural, state, left, right, ComparisonLe.INSTANCE,
				getStaticType(), this);
		return unboxed != null ? unboxed : super.fwdBinarySemantics(interprocedural, state, left, right, expressions);
	}
}

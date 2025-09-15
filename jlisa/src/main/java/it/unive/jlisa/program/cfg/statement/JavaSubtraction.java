package it.unive.jlisa.program.cfg.statement;

import it.unive.lisa.analysis.*;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingSub;
import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.Type;

/**
 * An expression modeling the subtraction operation ({@code -}). Both operands'
 * types must be instances of {@link NumericType}. The type of this expression
 * is the common numerical type of its operands, according to the type
 * inference.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class JavaSubtraction
		extends
		it.unive.lisa.program.cfg.statement.BinaryExpression {

	/**
	 * Builds the subtraction.
	 *
	 * @param cfg      the {@link CFG} where this operation lies
	 * @param location the location where this literal is defined
	 * @param left     the left-hand side of this operation
	 * @param right    the right-hand side of this operation
	 */
	public JavaSubtraction(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression right) {
		super(cfg, location, "-", left.getStaticType().commonSupertype(right.getStaticType()), left, right);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0; // no extra fields to compare
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdBinarySemantics(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression left,
			SymbolicExpression right,
			StatementStore<A> expressions)
			throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		if (analysis.getRuntimeTypesOf(state, left, this).stream().noneMatch(Type::isNumericType))
			return state.bottomExecution();
		if (analysis.getRuntimeTypesOf(state, right, this).stream().noneMatch(Type::isNumericType))
			return state.bottomExecution();

		return analysis.smallStepSemantics(
				state,
				new BinaryExpression(getStaticType(), left, right, NumericNonOverflowingSub.INSTANCE, getLocation()),
				this);
	}

}

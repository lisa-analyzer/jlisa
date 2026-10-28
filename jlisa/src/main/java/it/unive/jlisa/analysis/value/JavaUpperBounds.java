package it.unive.jlisa.analysis.value;

import it.unive.jlisa.lattices.BoundsAndEqualities;
import it.unive.jlisa.program.type.JavaByteType;
import it.unive.jlisa.program.type.JavaCharType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.program.type.JavaLongType;
import it.unive.jlisa.program.type.JavaShortType;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.numeric.UpperBounds;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.lattices.symbolic.DefiniteIdSet;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.symbolic.value.operator.binary.LogicalAnd;
import it.unive.lisa.symbolic.value.operator.binary.LogicalOr;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.type.Type;

/**
 * The upper bounds of {@link UpperBounds} (for each identifier, the identifiers
 * that are surely greater than it), with the changes needed by Java programs:
 * <ul>
 * <li>a weak identifier (e.g., a summary of the arrays created at the same
 * point) keeps only the relations that hold before and after an
 * assignment;</li>
 * <li>{@link #satisfies} also answers {@link Satisfiability#NOT_SATISFIED}, and
 * handles conjunctions, disjunctions and negations (e.g., the check
 * {@code i < 0 || i >= a.length} of an array access);</li>
 * <li>{@link #assume} handles conjunctions, and negations of comparisons
 * between integral values.</li>
 * </ul>
 * The relations given by {@code x = y + c} and {@code x = y - c} hold only if
 * the operation does not overflow, and a copy {@code x = y} makes {@code x} and
 * {@code y} equal: both are handled by
 * {@link ConstantPropagationWithRelations}, which knows the intervals and the
 * equalities.
 *
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public class JavaUpperBounds extends UpperBounds {

	@Override
	public ValueEnvironment<DefiniteIdSet> assign(
			ValueEnvironment<DefiniteIdSet> state,
			Identifier id,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (state.isBottom())
			return state;
		ValueEnvironment<DefiniteIdSet> result = super.assign(state, id, expression, pp, oracle);
		// a weak identifier stands for more than one location: the relations
		// must also hold for the locations that are not assigned
		if (id.isWeak())
			result = BoundsAndEqualities.join(state, result);
		return result;
	}

	@Override
	public ValueEnvironment<DefiniteIdSet> assume(
			ValueEnvironment<DefiniteIdSet> state,
			ValueExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		if (state.isBottom())
			return state;
		if (expression instanceof BinaryExpression
				&& ((BinaryExpression) expression).getOperator() == LogicalAnd.INSTANCE) {
			BinaryExpression and = (BinaryExpression) expression;
			if (!(and.getLeft() instanceof ValueExpression) || !(and.getRight() instanceof ValueExpression))
				return state;
			ValueEnvironment<DefiniteIdSet> left = assume(state, (ValueExpression) and.getLeft(), src, dest,
					oracle);
			return assume(left, (ValueExpression) and.getRight(), src, dest, oracle);
		}
		if (expression instanceof UnaryExpression
				&& ((UnaryExpression) expression).getOperator() == LogicalNegation.INSTANCE) {
			BinaryExpression negated = negate(((UnaryExpression) expression).getExpression());
			return negated == null ? state : super.assume(state, negated, src, dest, oracle);
		}
		return super.assume(state, expression, src, dest, oracle);
	}

	// the comparison equivalent to the negation of the given one, if it is a
	// comparison between integral values (with floating point values, both
	// x < y and x >= y are false when x or y is NaN), or null
	private static BinaryExpression negate(
			SymbolicExpression expression) {
		if (!(expression instanceof BinaryExpression))
			return null;
		BinaryExpression comparison = (BinaryExpression) expression;
		if (!isIntegral(comparison.getLeft().getStaticType()) || !isIntegral(comparison.getRight().getStaticType()))
			return null;
		BinaryOperator op = comparison.getOperator();
		BinaryOperator opposite;
		if (op == ComparisonLt.INSTANCE)
			opposite = ComparisonGe.INSTANCE;
		else if (op == ComparisonLe.INSTANCE)
			opposite = ComparisonGt.INSTANCE;
		else if (op == ComparisonGt.INSTANCE)
			opposite = ComparisonLe.INSTANCE;
		else if (op == ComparisonGe.INSTANCE)
			opposite = ComparisonLt.INSTANCE;
		else if (op == ComparisonNe.INSTANCE)
			opposite = ComparisonEq.INSTANCE;
		else
			return null;
		return new BinaryExpression(comparison.getStaticType(), comparison.getLeft(), comparison.getRight(),
				opposite, comparison.getCodeLocation());
	}

	private static boolean isIntegral(
			Type type) {
		return type instanceof JavaIntType || type instanceof JavaLongType || type instanceof JavaShortType
				|| type instanceof JavaByteType || type instanceof JavaCharType;
	}

	@Override
	public Satisfiability satisfies(
			ValueEnvironment<DefiniteIdSet> state,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (state.isBottom())
			return Satisfiability.BOTTOM;

		if (expression instanceof UnaryExpression
				&& ((UnaryExpression) expression).getOperator() == LogicalNegation.INSTANCE) {
			SymbolicExpression arg = ((UnaryExpression) expression).getExpression();
			return arg instanceof ValueExpression ? satisfies(state, (ValueExpression) arg, pp, oracle).negate()
					: Satisfiability.UNKNOWN;
		}

		if (!(expression instanceof BinaryExpression))
			return Satisfiability.UNKNOWN;
		BinaryExpression bexp = (BinaryExpression) expression;
		BinaryOperator op = bexp.getOperator();
		SymbolicExpression left = bexp.getLeft();
		SymbolicExpression right = bexp.getRight();

		if (op == LogicalAnd.INSTANCE || op == LogicalOr.INSTANCE) {
			if (!(left instanceof ValueExpression) || !(right instanceof ValueExpression))
				return Satisfiability.UNKNOWN;
			Satisfiability l = satisfies(state, (ValueExpression) left, pp, oracle);
			Satisfiability r = satisfies(state, (ValueExpression) right, pp, oracle);
			return op == LogicalAnd.INSTANCE ? l.and(r) : l.or(r);
		}

		if (!(left instanceof Identifier) || !(right instanceof Identifier))
			return Satisfiability.UNKNOWN;
		// x < y or y < x, as known by the upper bounds
		boolean lt = state.getState((Identifier) left).contains((Identifier) right);
		boolean gt = state.getState((Identifier) right).contains((Identifier) left);
		if (!lt && !gt)
			return Satisfiability.UNKNOWN;

		if (op == ComparisonLt.INSTANCE || op == ComparisonLe.INSTANCE)
			return lt ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
		if (op == ComparisonGt.INSTANCE || op == ComparisonGe.INSTANCE)
			return gt ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
		if (op == ComparisonEq.INSTANCE)
			return Satisfiability.NOT_SATISFIED;
		if (op == ComparisonNe.INSTANCE)
			return Satisfiability.SATISFIED;
		return Satisfiability.UNKNOWN;
	}
}

package it.unive.jlisa.program.cfg.expression;

import it.unive.jlisa.program.type.JavaBooleanType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.binary.LogicalOr;

/**
 * Helpers shared by the statements accessing the cells of arrays one by one
 * (e.g., {@link JavaArrayAccess}, {@code System.arraycopy} and
 * {@code String.getChars}): the cells of an array are accessed through
 * constant indexes only when there are few of them.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public final class ArrayCells {

	/**
	 * The maximum number of cells of an array that are accessed through
	 * constant indexes. The cells after them are not tracked: a read gives any
	 * value of the element type, and a write changes only the first cells.
	 */
	public static final int MAX_ENUMERATED_CELLS = 64;

	private ArrayCells() {
	}

	/**
	 * Yields the integer constant the given expression evaluates to in the
	 * given state, if it is a single constant.
	 *
	 * @param <A>        the kind of {@link AbstractLattice} of the state
	 * @param <D>        the kind of {@link AbstractDomain} of the analysis
	 * @param analysis   the analysis
	 * @param state      the state
	 * @param expression the expression
	 * @param context    the interprocedural context
	 * @param pp         the program point evaluating the expression
	 *
	 * @return the constant, or {@code null} if the expression is not a single
	 *             integer constant
	 *
	 * @throws SemanticException if the analysis fails
	 */
	public static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> Integer constant(
			Analysis<A, D> analysis,
			AnalysisState<A> state,
			SymbolicExpression expression,
			ScopeId<A> context,
			ProgramPoint pp)
			throws SemanticException {
		ExpressionSet rewritten = analysis.rewrite(state, expression, context, pp);
		if (rewritten.size() != 1)
			return null;
		SymbolicExpression e = rewritten.iterator().next();
		if (e instanceof Constant && ((Constant) e).getValue() instanceof Integer)
			return (Integer) ((Constant) e).getValue();
		return null;
	}

	/**
	 * Yields the disjunction of the given conditions.
	 *
	 * @param location the location of the disjunction
	 * @param first    the first condition
	 * @param others   the other conditions
	 *
	 * @return the disjunction
	 */
	public static BinaryExpression or(
			CodeLocation location,
			BinaryExpression first,
			BinaryExpression... others) {
		BinaryExpression result = first;
		for (BinaryExpression other : others)
			result = new BinaryExpression(JavaBooleanType.INSTANCE, result, other, LogicalOr.INSTANCE, location);
		return result;
	}
}

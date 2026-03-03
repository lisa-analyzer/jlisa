package it.unive.jlisa.analysis.value;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.ValueExpression;

/**
 * Optional interface that a value domain may implement to expose
 * <em>existential satisfiability</em>: the ability to answer whether there
 * exists at least one concrete program execution in which a given expression
 * evaluates to a truthy/satisfied value.
 * <p>
 * This is the under-approximation counterpart of the standard {@code satisfies}
 * method (which is an over-approximation). It is used by
 * {@link it.unive.jlisa.checkers.AssertChecker} to produce correct
 * {@code FALSE} verdicts: if the existential check returns {@code true} for the
 * negation of an assert condition, there exists a concrete run that violates
 * the assertion.
 * <p>
 * Implementations may use unchecked casts to convert the raw
 * {@link ValueEnvironment} parameter to the concrete element type they expect.
 *
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public interface ExistentialSatisfiabilityProvider {

	/**
	 * Returns {@code true} if there exists at least one concrete program
	 * execution in which {@code expr} evaluates to a truthy (satisfied) value,
	 * based on the under-approximation components of the abstract state.
	 * <p>
	 * This method is sound for generating {@code FALSE} verdicts: it only
	 * returns {@code true} when a concrete violating run is guaranteed to
	 * exist.
	 *
	 * @param env    the current value environment (raw wildcard to allow use
	 *                   from generic contexts)
	 * @param expr   the expression to check
	 * @param pp     the current program point
	 * @param oracle the semantic oracle for type information
	 *
	 * @return {@code true} if a concrete satisfying execution is guaranteed to
	 *             exist
	 *
	 * @throws SemanticException if an error occurs during evaluation
	 */
	boolean existentiallySatisfies(
			ValueEnvironment<?> env,
			ValueExpression expr,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException;
}

package it.unive.jlisa.checkers;

import it.unive.lisa.program.cfg.statement.Statement;
import java.util.Objects;

/**
 * A definite violation found by the {@link AssertChecker}: every execution
 * reaches the target statement, and violates there the property of the given
 * kind.
 *
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public final class Violation {

	/**
	 * The kind of this violation.
	 */
	private final ViolationKind kind;

	/**
	 * The statement where this violation happens.
	 */
	private final Statement target;

	/**
	 * Builds a violation.
	 *
	 * @param kind   the kind of the violation
	 * @param target the statement where the violation happens
	 */
	public Violation(
			ViolationKind kind,
			Statement target) {
		this.kind = Objects.requireNonNull(kind, "kind");
		this.target = Objects.requireNonNull(target, "target");
	}

	/**
	 * Yields the kind of this violation.
	 *
	 * @return the kind
	 */
	public ViolationKind getKind() {
		return kind;
	}

	/**
	 * Yields the statement where this violation happens. For an assertion, it
	 * is the assertion itself.
	 *
	 * @return the statement
	 */
	public Statement getTarget() {
		return target;
	}

	@Override
	public String toString() {
		return kind + " @ " + target.getLocation();
	}
}

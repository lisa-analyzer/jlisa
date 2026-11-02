package it.unive.jlisa.checkers;

import it.unive.lisa.checks.verification.ReproducingInput;
import it.unive.lisa.program.cfg.statement.Statement;
import java.util.List;
import java.util.Objects;

/**
 * A definite violation of a warning of the {@link AssertChecker}, confirmed by
 * the verification of warnings performed after the analysis: an execution
 * reaches the target statement, and violates there the property of the given
 * kind. If the violation has been confirmed with concrete values, the inputs
 * reproducing it are also available.
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
	 * The inputs reproducing this violation, in the order they are consumed.
	 */
	private final List<ReproducingInput> inputs;

	/**
	 * Builds a violation.
	 *
	 * @param kind   the kind of the violation
	 * @param target the statement where the violation happens
	 */
	public Violation(
			ViolationKind kind,
			Statement target) {
		this(kind, target, null);
	}

	/**
	 * Builds a violation.
	 *
	 * @param kind   the kind of the violation
	 * @param target the statement where the violation happens
	 * @param inputs the inputs reproducing the violation, in the order they
	 *                   are consumed (can be {@code null} if none are known)
	 */
	public Violation(
			ViolationKind kind,
			Statement target,
			List<ReproducingInput> inputs) {
		this.kind = Objects.requireNonNull(kind, "kind");
		this.target = Objects.requireNonNull(target, "target");
		this.inputs = inputs == null ? List.of() : List.copyOf(inputs);
	}

	/**
	 * Returns the kind of this violation.
	 *
	 * @return the kind
	 */
	public ViolationKind getKind() {
		return kind;
	}

	/**
	 * Returns the statement where this violation happens. For an assertion, it
	 * is the assertion itself.
	 *
	 * @return the statement
	 */
	public Statement getTarget() {
		return target;
	}

	/**
	 * Returns the inputs reproducing this violation, in the order they are
	 * consumed.
	 *
	 * @return the inputs (empty if none are known)
	 */
	public List<ReproducingInput> getInputs() {
		return inputs;
	}

	@Override
	public String toString() {
		return kind + " @ " + target.getLocation();
	}
}

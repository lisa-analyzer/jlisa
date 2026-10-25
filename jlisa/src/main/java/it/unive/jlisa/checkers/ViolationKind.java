package it.unive.jlisa.checkers;

/**
 * The kinds of violations found by the {@link AssertChecker}.
 *
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public enum ViolationKind {

	/**
	 * An assertion that does not hold.
	 */
	ASSERT,

	/**
	 * A runtime exception that is not caught. The {@link AssertChecker} does
	 * not report violations of this kind yet: the statement that throws the
	 * exception is not known where the checker finds it, at the end of the
	 * entry point.
	 */
	RUNTIME_EXCEPTION;
}

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
	 * A runtime exception that is not caught. Its violation is reported at the
	 * statement that raises the exception, found by
	 * {@link RuntimeExceptionTarget}.
	 */
	RUNTIME_EXCEPTION;
}

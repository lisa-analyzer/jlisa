package it.unive.jlisa.witness.validator.search;

import java.util.List;

/**
 * A run that violates the property, as needed to write its witness: every
 * {@code Verifier.nondet*()} call in execution order with the value it
 * returned, and the stack of the thread at the violation.
 *
 * @param calls     the calls, in execution order
 * @param exception the type of the uncaught exception or error (e.g.
 *                      {@code java.lang.AssertionError})
 * @param frames    the frames of the thread at the violation, the innermost
 *                      first
 */
public record Trace(
		List<Call> calls,
		String exception,
		List<Frame> frames) {

	/** At most this many calls are recorded: a longer run has no trace. */
	public static final int MAX_CALLS = 5000;

	/**
	 * One call of a {@code Verifier.nondet*()} method.
	 *
	 * @param method the name of the method (e.g. {@code nondetInt})
	 * @param type   its return type (e.g. {@code int},
	 *                   {@code java.lang.String})
	 * @param value  the value it returned: an {@code Integer}, {@code Long},
	 *                   {@code Short}, {@code Byte}, {@code Character},
	 *                   {@code Boolean}, {@code Float}, {@code Double} or
	 *                   {@code String}
	 * @param caller where it was called from
	 */
	public record Call(
			String method,
			String type,
			Object value,
			Frame caller) {
	}

	/**
	 * A program point of the run.
	 *
	 * @param sourcePath the path of the source file from the package root (e.g.
	 *                       {@code Main.java}, {@code org/foo/A.java}), or
	 *                       {@code null} if unknown
	 * @param line       the line, or -1 if unknown
	 * @param codeIndex  the bytecode index in the method
	 * @param className  the class
	 * @param methodName the method
	 */
	public record Frame(
			String sourcePath,
			int line,
			long codeIndex,
			String className,
			String methodName) {
	}
}

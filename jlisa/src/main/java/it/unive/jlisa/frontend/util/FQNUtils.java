package it.unive.jlisa.frontend.util;

public final class FQNUtils {

	private FQNUtils() {
	}

	/**
	 * Builds a fully qualified name from package, optional outer class chain,
	 * and simple name.
	 *
	 * @param pkg        the package name (may be empty)
	 * @param outer      the outer class name chain (may be null)
	 * @param simpleName the simple class name
	 *
	 * @return the fully qualified name
	 */
	public static String buildFQN(
			String pkg,
			String outer,
			String simpleName) {
		return (pkg.isEmpty() ? "" : pkg + ".") + (outer == null ? "" : outer + ".") + simpleName;
	}
}

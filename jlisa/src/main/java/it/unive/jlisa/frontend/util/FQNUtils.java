package it.unive.jlisa.frontend.util;

import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;

public final class FQNUtils {

	private FQNUtils() {
	}

	/**
	 * Builds the fully qualified name of a top-level type declared in the
	 * given compilation unit.
	 *
	 * @param cu   the compilation unit declaring the type
	 * @param type the top-level type declaration
	 *
	 * @return the fully qualified name
	 */
	public static String buildFQN(
			CompilationUnit cu,
			AbstractTypeDeclaration type) {
		String pkg = cu.getPackage() == null ? "" : cu.getPackage().getName().getFullyQualifiedName();
		return buildFQN(pkg, null, type.getName().toString());
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

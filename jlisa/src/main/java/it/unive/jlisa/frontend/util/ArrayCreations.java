package it.unive.jlisa.frontend.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.PrimitiveType;

/**
 * The creations of arrays of arrays whose elements must be created one by one
 * (e.g., {@code new int[n][m]}, with a dimension that is not constant followed
 * by other ones): as Java creates them with a loop, they are translated into
 * calls to methods of a class added to the program, creating the arrays with
 * loops, so that each element is a different array. The creations whose
 * dimensions followed by other ones are constant (e.g., {@code new int[2][m]})
 * are created element by element by the frontend instead.
 */
public final class ArrayCreations {

	/**
	 * The name of the class with the methods creating the arrays.
	 */
	public static final String CLASS = "JLisaArrayCreation";

	/**
	 * The name of the file of the class with the methods creating the arrays.
	 */
	public static final String FILE = CLASS + ".java";

	private ArrayCreations() {
	}

	/**
	 * Returns whether the given creation is translated into a call to a method
	 * creating the array (see {@link #method(ArrayCreation)}): a creation with
	 * a dimension that is not constant followed by other dimensions, of
	 * elements of primitive types.
	 *
	 * @param node the creation
	 *
	 * @return whether the creation is translated into a call
	 */
	public static boolean isTranslated(
			ArrayCreation node) {
		if (!(node.getType().getElementType() instanceof PrimitiveType))
			return false;
		List<?> dimensions = node.dimensions();
		for (int i = 0; i < dimensions.size() - 1; i++)
			if (!(dimensions.get(i) instanceof NumberLiteral))
				return true;
		return false;
	}

	/**
	 * Returns the name of the method creating the array of the given creation
	 * (see {@link #isTranslated(ArrayCreation)}), whose parameters are the
	 * given dimensions of the creation.
	 *
	 * @param node the creation
	 *
	 * @return the name of the method
	 */
	public static String method(
			ArrayCreation node) {
		return method(node.getType().getElementType().toString(), node.dimensions().size(),
				node.getType().getDimensions());
	}

	private static String method(
			String element,
			int given,
			int dimensions) {
		return "new_" + element + "_" + given + "_" + dimensions;
	}

	/**
	 * Returns the source of the class with the methods creating the arrays of
	 * the creations of the given compilation units that are translated into
	 * calls (see {@link #isTranslated(ArrayCreation)}), or {@code null} if
	 * there is none.
	 *
	 * @param units the compilation units
	 *
	 * @return the source of the class, or {@code null}
	 */
	public static String source(
			CompilationUnit[] units) {
		// element type, given dimensions and dimensions of each method
		Set<String> methods = new TreeSet<>();
		for (CompilationUnit unit : units)
			unit.accept(new ASTVisitor() {

				@Override
				public boolean visit(
						ArrayCreation node) {
					if (isTranslated(node)) {
						String element = node.getType().getElementType().toString();
						int dimensions = node.getType().getDimensions();
						// the method of each number of given dimensions calls
						// the one of a dimension less
						for (int given = node.dimensions().size(); given > 0; given--, dimensions--)
							methods.add(element + " " + given + " " + dimensions);
					}
					return true;
				}
			});
		if (methods.isEmpty())
			return null;

		StringBuilder source = new StringBuilder("public final class " + CLASS + " {\n");
		for (String m : methods) {
			String[] parts = m.split(" ");
			String element = parts[0];
			int given = Integer.parseInt(parts[1]);
			int dimensions = Integer.parseInt(parts[2]);
			String type = element + "[]".repeat(dimensions);
			List<String> params = new ArrayList<>();
			List<String> args = new ArrayList<>();
			for (int i = 0; i < given; i++) {
				params.add("int d" + i);
				if (i > 0)
					args.add("d" + i);
			}
			source.append("  public static ").append(type).append(" ").append(method(element, given, dimensions))
					.append("(").append(String.join(", ", params)).append(") {\n");
			if (given == 1)
				source.append("    return new ").append(element).append("[d0]").append("[]".repeat(dimensions - 1))
						.append(";\n");
			else {
				String nested = method(element, given - 1, dimensions - 1) + "(" + String.join(", ", args) + ")";
				// the following dimensions are checked (e.g., they are not
				// negative) also when there is no element
				source.append("    if (d0 == 0) ").append(nested).append(";\n");
				source.append("    ").append(type).append(" a = new ").append(element).append("[d0]")
						.append("[]".repeat(dimensions - 1)).append(";\n");
				source.append("    for (int i = 0; i < d0; i++) a[i] = ").append(nested).append(";\n");
				source.append("    return a;\n");
			}
			source.append("  }\n");
		}
		return source.append("}\n").toString();
	}
}

package it.unive.jlisa.program.language;

import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.language.hierarchytraversal.HierarchyTraversalStrategy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The traversal of the Java class hierarchy, in the order of the Java lookup of
 * methods and fields: the unit, then its superclasses up to the root, then the
 * interfaces of all of them (breadth first). Each unit is visited once.
 * <p>
 * Why not LiSA's {@code SingleInheritanceTraversalStrategy}: it climbs the
 * hierarchy through the first immediate ancestor of each unit, but in Java a
 * class has a superclass and possibly interfaces, and
 * {@link ClassUnit#getImmediateAncestors()} returns their union in an order
 * that is not the one of the declarations (it is built through hash maps). The
 * climb can then go through an interface and never reach the superclass: its
 * methods and fields are missed (e.g., the constructor called by
 * {@code super()} becomes an open call), and whether this happens depends on
 * unrelated parts of the program. Here the superclasses and the interfaces are
 * taken from their own lists, in the order of the declarations.
 * </p>
 * <p>
 * Some library interfaces are declared as classes (e.g.,
 * {@code java.util.List}, see the {@code java-libraries} files): a class
 * implementing one of them has it among its superclasses, after the one it
 * extends (the frontend adds the superclass first). The climb follows the first
 * superclass only, and the others are visited with the interfaces.
 * </p>
 */
public class JavaTraversalStrategy implements HierarchyTraversalStrategy {

	/**
	 * The singleton instance of this class.
	 */
	public static final JavaTraversalStrategy INSTANCE = new JavaTraversalStrategy();

	private JavaTraversalStrategy() {
	}

	@Override
	public Iterable<CompilationUnit> traverse(
			Statement st,
			CompilationUnit start) {
		List<CompilationUnit> order = new ArrayList<>();
		Set<CompilationUnit> seen = new HashSet<>();

		// the unit and its superclasses
		CompilationUnit current = start;
		while (current != null && seen.add(current)) {
			order.add(current);
			current = superclass(current);
		}

		// then the interfaces of all of them
		for (int i = 0; i < order.size(); i++)
			for (CompilationUnit ancestor : ancestors(order.get(i)))
				if (seen.add(ancestor))
					order.add(ancestor);
		return order;
	}

	// the superclass of the unit (the first declared one), if any: interfaces
	// have none
	private static CompilationUnit superclass(
			CompilationUnit unit) {
		if (unit instanceof ClassUnit cu && !cu.getSuperclasses().isEmpty())
			return cu.getSuperclasses().get(0);
		return null;
	}

	// the immediate ancestors of the unit in the order of the declarations:
	// the superclasses, then the interfaces
	private static List<CompilationUnit> ancestors(
			CompilationUnit unit) {
		List<CompilationUnit> ancestors = new ArrayList<>();
		if (unit instanceof ClassUnit cu) {
			ancestors.addAll(cu.getSuperclasses());
			ancestors.addAll(cu.getInterfaces());
		} else
			ancestors.addAll(unit.getImmediateAncestors());
		return ancestors;
	}
}

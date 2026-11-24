package it.unive.jlisa.triage;

import it.unive.jlisa.program.cfg.expression.JavaDecrement;
import it.unive.jlisa.program.cfg.expression.JavaIncrement;
import it.unive.jlisa.program.cfg.statement.JavaAssignment;
import it.unive.jlisa.program.cfg.statement.JavaCompoundAssignment;
import it.unive.jlisa.program.cfg.statement.global.JavaAccessInstanceGlobal;
import it.unive.jlisa.program.libraries.LibrarySpecificationProvider;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.NativeCFG;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.BinaryExpression;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.UnaryExpression;
import it.unive.lisa.triage.model.HeapModel;
import it.unive.lisa.triage.model.HeapWrites;
import it.unive.lisa.util.datastructures.graph.GraphVisitor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * The contents of the heap that the code of the members of a Java program
 * writes, as the verification of the warnings uses them (see
 * {@link HeapWrites}). The fields of the classes of the program (the ones that
 * are not library classes) are only written by assignments (also compound
 * assignments, increments and decrements) of instance fields: a member writes
 * the fields, of any class, with the names of the fields it assigns. The fields
 * of library classes and the cells of arrays might be written by any member, as
 * the semantics of library methods (natives) and of several statements (e.g.,
 * string concatenations, array allocations) write them: these are the fields
 * declared by library classes, the ones that natives write through unqualified
 * names (e.g., {@code value}, also when a class of the program declares a field
 * with that name), and the internal fields of the models of the library. Natives write no field
 * of the classes of the program, except the reflective ones and the clones of
 * objects, whose writes are unknown. The calls that natives and statements
 * perform on behalf of the program (e.g., of the methods converting objects to
 * strings) are part of the call graph, so the writes of their targets are found
 * through it (as for {@link JavaGlobalWrites}).
 */
public class JavaHeapWrites
		implements
		HeapWrites {

	/**
	 * The package of the reflective natives.
	 */
	private static final String REFLECTION_PACKAGE = "java.lang.reflect.";

	/**
	 * The unit of the natives on classes (e.g., creating instances).
	 */
	private static final String CLASS_UNIT = "java.lang.Class";

	/**
	 * The name of the natives cloning objects.
	 */
	private static final String CLONE = "clone";

	/**
	 * The fields that the natives of the library write, by their unqualified
	 * names: the library classes declare almost none of their fields (e.g., the
	 * {@code value} of string builders, atomic integers and boxed numbers, the
	 * {@code message} of exceptions, the metadata of classes). The internal
	 * fields of the models of the library (e.g., the entries of maps, see
	 * {@link it.unive.jlisa.program.java.constructs.map.MapModel#keyField}) have
	 * names starting with {@code $}, and are written by them too. This list
	 * must be kept in sync with the natives.
	 */
	private static final Set<String> NATIVE_FIELDS = Set.of("value", "length", "capacity", "message", "name",
			"type", "clazz", "modifiers", "superClass", "interfaces", "parameterTypes", "returnType",
			"declaredMethods", "declaredFields", "isArray");

	private final Map<CodeMember, Written> cache = new HashMap<>();

	/**
	 * The fields of the classes of each program, as the information telling
	 * them apart from the ones of library classes.
	 */
	private final Map<Program, Fields> fields = new IdentityHashMap<>();

	@Override
	public synchronized Written writtenBy(
			CodeMember member) {
		if (cache.containsKey(member))
			return cache.get(member);
		Written written = compute(member);
		cache.put(member, written);
		return written;
	}

	private Written compute(
			CodeMember member) {
		Unit unit = member.getDescriptor().getUnit();
		Fields known = fields.computeIfAbsent(unit.getProgram(), Fields::new);
		if (member instanceof NativeCFG) {
			String name = unit.getName();
			if (name.startsWith(REFLECTION_PACKAGE) || name.equals(CLASS_UNIT)
					|| member.getDescriptor().getName().equals(CLONE))
				return null;
			return known.libraryWrites;
		}
		if (!(member instanceof CFG))
			return null;
		Set<String> assigned = new HashSet<>();
		// every statement and sub-expression of the cfg
		((CFG) member).accept(new GraphVisitor<CFG, Statement, Edge, Void>() {

			@Override
			public boolean visit(
					Void tool,
					CFG graph,
					Statement node) {
				collect(node, assigned);
				return true;
			}
		}, null);
		if (assigned.isEmpty())
			return known.libraryWrites;
		return new Written(field -> assigned.contains(simpleName(field)), true).union(known.libraryWrites);
	}

	/**
	 * Adds to the given set the name of the instance field assigned by the
	 * given statement, if any.
	 */
	private static void collect(
			Statement st,
			Set<String> assigned) {
		Expression target = null;
		if (st instanceof JavaAssignment || st instanceof JavaCompoundAssignment)
			target = ((BinaryExpression) st).getLeft();
		else if (st instanceof JavaIncrement || st instanceof JavaDecrement)
			target = ((UnaryExpression) st).getSubExpression();
		if (target instanceof JavaAccessInstanceGlobal)
			assigned.add(((JavaAccessInstanceGlobal) target).getTarget());
	}

	/**
	 * Returns the name of the field with the given name, without the class
	 * qualifying it (see {@link HeapModel#QUALIFIER}).
	 */
	private static String simpleName(
			String field) {
		int sep = field.lastIndexOf(HeapModel.QUALIFIER);
		return sep < 0 ? field : field.substring(sep + HeapModel.QUALIFIER.length());
	}

	/**
	 * The fields of the classes of a program.
	 */
	private static final class Fields {

		/**
		 * The names of the classes of the program that are not library classes.
		 */
		private final Set<String> programClasses = new HashSet<>();

		/**
		 * The names of the fields of library classes.
		 */
		private final Set<String> libraryFields = new HashSet<>();

		/**
		 * The contents written by any member: the fields of library classes and
		 * the cells of arrays (a single test, shared by all the members, so
		 * that the unions of their writes keep it once).
		 */
		private final Written libraryWrites = new Written(this::isLibraryField, true);

		private Fields(
				Program program) {
			for (Unit unit : program.getUnits()) {
				if (!(unit instanceof CompilationUnit))
					continue;
				CompilationUnit cu = (CompilationUnit) unit;
				if (isProgramClass(cu))
					programClasses.add(cu.getName());
				else
					for (Global g : cu.getInstanceGlobals(false))
						libraryFields.add(g.getName());
			}
		}

		/**
		 * A class is a class of the program if it is not a library class and
		 * the program has its code.
		 */
		private static boolean isProgramClass(
				CompilationUnit unit) {
			if (LibrarySpecificationProvider.isLibraryAvailable(unit.getName()))
				return false;
			for (CodeMember member : unit.getCodeMembers())
				if (member instanceof CFG)
					return true;
			for (CodeMember member : unit.getInstanceCodeMembers(false))
				if (member instanceof CFG)
					return true;
			return false;
		}

		/**
		 * Returns whether the field with the given name (qualified by the class
		 * declaring it, or not) might be a field of a library class.
		 */
		private boolean isLibraryField(
				String field) {
			int sep = field.lastIndexOf(HeapModel.QUALIFIER);
			if (sep < 0)
				return libraryFields.contains(field) || NATIVE_FIELDS.contains(field) || field.startsWith("$");
			return !programClasses.contains(field.substring(0, sep));
		}
	}
}

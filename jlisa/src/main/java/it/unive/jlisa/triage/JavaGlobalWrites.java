package it.unive.jlisa.triage;

import it.unive.jlisa.frontend.InitializedClassSet;
import it.unive.jlisa.program.cfg.expression.PostfixAddition;
import it.unive.jlisa.program.cfg.expression.PostfixSubtraction;
import it.unive.jlisa.program.cfg.expression.PrefixAddition;
import it.unive.jlisa.program.cfg.expression.PrefixSubtraction;
import it.unive.jlisa.program.cfg.statement.JavaAssignment;
import it.unive.jlisa.program.cfg.statement.JavaCompoundAssignment;
import it.unive.jlisa.program.cfg.statement.global.JavaAccessGlobal;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.NativeCFG;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.BinaryExpression;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.UnaryExpression;
import it.unive.lisa.triage.model.GlobalWrites;
import it.unive.lisa.util.datastructures.graph.GraphVisitor;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The static fields that the code of the members of a Java program writes, as
 * the verification of the warnings uses them (see {@link GlobalWrites}): the
 * static fields assigned (also by compound assignments, increments and
 * decrements) by the statements of a method, excluding the methods it calls.
 * Library methods (natives) write no static field of the program, except the
 * reflective writes of fields, whose written fields are unknown. The calls that
 * natives perform on behalf of the program (e.g., reflective invocations) are
 * part of the call graph, so the fields written by their targets are found
 * through it.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class JavaGlobalWrites
		implements
		GlobalWrites {

	/**
	 * The unit of the reflective field accesses.
	 */
	private static final String FIELD_UNIT = "java.lang.reflect.Field";

	private final Map<CodeMember, Set<String>> cache = new HashMap<>();

	@Override
	public synchronized Set<String> writtenBy(
			CodeMember member) {
		if (cache.containsKey(member))
			return cache.get(member);
		Set<String> written = compute(member);
		cache.put(member, written);
		return written;
	}

	/**
	 * {@inheritDoc}<br>
	 * <br>
	 * Static initializers are initializers: they run at most once, before the
	 * first access to the static fields of their class, including writes.
	 */
	@Override
	public boolean isInitializer(
			CodeMember member) {
		return member.getDescriptor().getName().endsWith(InitializedClassSet.SUFFIX_CLINIT);
	}

	private static Set<String> compute(
			CodeMember member) {
		if (member instanceof NativeCFG) {
			// reflective writes of fields might write any static field
			boolean reflectiveWrite = member.getDescriptor().getUnit().getName().equals(FIELD_UNIT)
					&& member.getDescriptor().getName().startsWith("set");
			return reflectiveWrite ? null : Collections.emptySet();
		}
		if (!(member instanceof CFG))
			return null;
		Set<String> written = new HashSet<>();
		// every statement and sub-expression of the cfg
		((CFG) member).accept(new GraphVisitor<CFG, Statement, Edge, Void>() {

			@Override
			public boolean visit(
					Void tool,
					CFG graph,
					Statement node) {
				collect(node, written);
				return true;
			}
		}, null);
		return written;
	}

	/**
	 * Adds to the given set the name of the static field written by the given
	 * statement, if any.
	 */
	private static void collect(
			Statement st,
			Set<String> written) {
		Expression target = null;
		if (st instanceof JavaAssignment || st instanceof JavaCompoundAssignment)
			target = ((BinaryExpression) st).getLeft();
		else if (st instanceof PrefixAddition || st instanceof PrefixSubtraction
				|| st instanceof PostfixAddition || st instanceof PostfixSubtraction)
			target = ((UnaryExpression) st).getSubExpression();
		if (target instanceof JavaAccessGlobal)
			written.add(((JavaAccessGlobal) target).getVariableName());
	}
}

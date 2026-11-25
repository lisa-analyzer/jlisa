package it.unive.jlisa.triage;

import it.unive.jlisa.frontend.InitializedClassSet;
import it.unive.jlisa.program.cfg.expression.JavaDecrement;
import it.unive.jlisa.program.cfg.expression.JavaIncrement;
import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.cfg.statement.JavaAssignment;
import it.unive.jlisa.program.cfg.statement.JavaCompoundAssignment;
import it.unive.jlisa.program.cfg.statement.global.JavaAccessGlobal;
import it.unive.lisa.program.CompilationUnit;
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
import it.unive.lisa.program.cfg.statement.call.Call.CallType;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.triage.model.GlobalWrites;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.datastructures.graph.GraphVisitor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
	 * first access to the static fields of their class, including writes. This
	 * holds only for the static fields of their own class: a static initializer
	 * writing the static fields of other classes (e.g., {@code A.x = 5} in the
	 * one of {@code B}) writes them the first time its class is used, also
	 * after they have been accessed, and it is not an initializer. The same
	 * holds for the code it runs: a static initializer is an initializer only
	 * if it calls nothing but other static initializers that are initializers
	 * (e.g., the one of its superclass) and code of the libraries (that writes
	 * no static field of the program), as the callees of initializers are not
	 * followed (e.g., {@code static { Helper.bump(); }}, with {@code bump}
	 * writing {@code Counter.n}, is not an initializer).
	 */
	@Override
	public synchronized boolean isInitializer(
			CodeMember member) {
		return isInitializer(member, new HashSet<>());
	}

	private boolean isInitializer(
			CodeMember member,
			Set<CodeMember> visiting) {
		if (!visiting.add(member))
			// a cycle of static initializers: each is checked on its own
			return true;
		String unit = member.getDescriptor().getUnit().getName();
		String simpleName = unit.substring(unit.lastIndexOf('.') + 1);
		// the static initializer of its class (not, e.g., a method of the
		// program whose name ends with the suffix)
		if (!member.getDescriptor().getName().equals(simpleName + InitializedClassSet.SUFFIX_CLINIT)
				|| member.getDescriptor().isInstance() || member.getDescriptor().getFormals().length != 0)
			return false;
		Set<String> written = writtenBy(member);
		if (written == null || !written.stream().allMatch(name -> name.startsWith(unit + "::")))
			return false;
		if (!(member instanceof CFG))
			return true;
		// the code it calls
		Program program = member.getDescriptor().getUnit().getProgram();
		boolean[] calls = { true };
		((CFG) member).accept(new GraphVisitor<CFG, Statement, Edge, Void>() {

			@Override
			public boolean visit(
					Void tool,
					CFG graph,
					Statement node) {
				calls[0] &= harmless(node, program, visiting);
				return true;
			}
		}, null);
		return calls[0];
	}

	/**
	 * Whether the given statement calls only code that writes no static field
	 * of the program, or static initializers that are initializers.
	 */
	private boolean harmless(
			Statement st,
			Program program,
			Set<CodeMember> visiting) {
		if (st instanceof JavaNewObj)
			return library(classOf(((JavaNewObj) st).getStaticType()));
		if (!(st instanceof UnresolvedCall))
			return true;
		UnresolvedCall call = (UnresolvedCall) st;
		if (call.getCallType() == CallType.STATIC) {
			Unit target = call.getQualifier() == null ? null : program.getUnit(call.getQualifier());
			if (target == null)
				return false;
			if (call.getTargetName().endsWith(InitializedClassSet.SUFFIX_CLINIT)
					&& call.getParameters().length == 0) {
				// another static initializer
				for (CodeMember clinit : target.getCodeMembersByName(call.getTargetName()))
					if (!isInitializer(clinit, visiting))
						return false;
				return true;
			}
			return library(target);
		}
		// an instance call: harmless only if the receiver is an object of
		// the libraries
		return call.getParameters().length > 0
				&& library(classOf(call.getParameters()[0].getStaticType()));
	}

	/**
	 * The unit of the objects of the given type, if it is (a reference to) a
	 * unit.
	 */
	private static Unit classOf(
			Type type) {
		Type inner = type.isReferenceType() ? type.asReferenceType().getInnerType() : type;
		return inner.isUnitType() ? inner.asUnitType().getUnit() : null;
	}

	/**
	 * Whether the given unit is a class of the libraries (all its code members
	 * are natives), whose code writes no static field of the program (except
	 * the reflective writes of fields).
	 */
	private static boolean library(
			Unit unit) {
		if (unit == null || unit.getName().equals(FIELD_UNIT))
			return false;
		List<CodeMember> members = new ArrayList<>(unit.getCodeMembers());
		if (unit instanceof CompilationUnit)
			members.addAll(((CompilationUnit) unit).getInstanceCodeMembers(true));
		return members.stream().allMatch(NativeCFG.class::isInstance);
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
		else if (st instanceof JavaIncrement || st instanceof JavaDecrement)
			target = ((UnaryExpression) st).getSubExpression();
		if (target instanceof JavaAccessGlobal)
			written.add(((JavaAccessGlobal) target).getVariableName());
	}
}

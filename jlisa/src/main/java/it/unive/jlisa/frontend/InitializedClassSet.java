package it.unive.jlisa.frontend;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.lattices.InverseSetLattice;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.Call.CallType;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

public class InitializedClassSet extends InverseSetLattice<InitializedClassSet, String> {

	public static final String INFO_KEY = "clinit";

	public static final String SUFFIX_CLINIT = "_clinit";

	public InitializedClassSet() {
		super(new TreeSet<String>(), false);
	}

	public InitializedClassSet(
			Set<String> elements) {
		super(elements, elements.isEmpty());
	}

	/**
	 * Builds the lattice.
	 *
	 * @param elements the elements that are contained in the lattice
	 * @param isTop    whether or not this is the top or bottom element of the
	 *                     lattice, valid only if the set of elements is empty
	 */
	public InitializedClassSet(
			Set<String> elements,
			boolean isTop) {
		super(elements, isTop);
	}

	@Override
	public InitializedClassSet wideningAux(
			InitializedClassSet other)
			throws SemanticException {
		return other.elements.containsAll(elements) ? other : top();
	}

	@Override
	public InitializedClassSet top() {
		return new InitializedClassSet(Collections.emptySet(), true);
	}

	@Override
	public InitializedClassSet bottom() {
		return new InitializedClassSet(Collections.emptySet(), false);
	}

	@Override
	public InitializedClassSet mk(
			Set<String> set) {
		return new InitializedClassSet(set);
	}

	/**
	 * Adds a new String to this set. This method has no side effect: a new
	 * {@link InitializedClassSet} is created, modified and returned.
	 * 
	 * @param id the identifier to add
	 * 
	 * @return the new set
	 */
	public InitializedClassSet add(
			String id) {
		Set<String> res = new HashSet<>(elements);
		res.add(id);
		return new InitializedClassSet(res);
	}

	@Override
	public StructuredRepresentation representation() {
		if (isTop())
			return new StringRepresentation("{}");
		return super.representation();
	}

	public static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> initialize(
			AnalysisState<A> state,
			JavaReferenceType reftype,
			Statement init,
			InterproceduralAnalysis<A, D> interprocedural) throws SemanticException {
		AnalysisState<A> result = state;
		InitializedClassSet info = state.getExecutionInfo(InitializedClassSet.INFO_KEY, InitializedClassSet.class);
		if (info == null)
			info = new InitializedClassSet();

		// if needed, calling the class initializer (if the class has one)
		String className = reftype.getInnerType().toString();
		String simpleName = className.contains(".")
				? className.substring(className.lastIndexOf(".") + 1)
				: className;
		String name = simpleName + InitializedClassSet.SUFFIX_CLINIT;

		if (!JavaClassType.lookup(className).getUnit().getCodeMembersByName(name).isEmpty() 
				&& !info.contains(className)) {
			UnresolvedCall clinit = new UnresolvedCall(
					init.getCFG(),
					init.getLocation(),
					CallType.STATIC,
					className,
					name,
					new Expression[0]);

			result = state.storeExecutionInfo(InitializedClassSet.INFO_KEY, info.add(className));
			result.withExecutionExpression(new Skip(init.getLocation()));
			result = clinit.forwardSemanticsAux(interprocedural, result, new ExpressionSet[0], new StatementStore<>(result.bottom()));
		}

		return result;
	}
}

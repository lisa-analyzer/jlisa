package it.unive.jlisa.program;

import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.lattices.SetLattice;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class LoadedClassSet extends SetLattice<LoadedClassSet, Type> {

	public static final String INFO_KEY = "loaded";

	// public static final String SUFFIX_CLINIT = "_clinit";

	public LoadedClassSet() {
		super(new HashSet<Type>(), false);
	}

	public LoadedClassSet(
			Set<Type> elements) {
		super(elements, elements.isEmpty());
	}

	/**
	 * Builds the lattice.
	 *
	 * @param elements the elements that are contained in the lattice
	 * @param isTop    whether or not this is the top or bottom element of the
	 *                     lattice, valid only if the set of elements is empty
	 */
	public LoadedClassSet(
			Set<Type> elements,
			boolean isTop) {
		super(elements, isTop);
	}

	@Override
	public LoadedClassSet wideningAux(
			LoadedClassSet other)
			throws SemanticException {
		LoadedClassSet res = new LoadedClassSet(elements, isTop);
		res.elements.addAll(other.elements);
		return res;
	}

	@Override
	public LoadedClassSet top() {
		return new LoadedClassSet(Collections.emptySet(), true);
	}

	@Override
	public LoadedClassSet bottom() {
		return new LoadedClassSet(Collections.emptySet(), false);
	}

	@Override
	public LoadedClassSet mk(
			Set<Type> set) {
		return new LoadedClassSet(set);
	}

	private LoadedClassSet add(
			Type id) {
		Set<Type> res = new HashSet<>(elements);
		res.add(id);
		return new LoadedClassSet(res);
	}

	@Override
	public StructuredRepresentation representation() {
		if (isTop())
			return new StringRepresentation("{}");
		return super.representation();
	}

	public static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> addLoadedClass(
			AnalysisState<A> state,
			InterproceduralAnalysis<A, D> interprocedural,
			Type t) {

		AnalysisState<A> result = state;
		LoadedClassSet loadedClasses = state.getExecutionInfo(LoadedClassSet.INFO_KEY, LoadedClassSet.class);
		if (loadedClasses == null)
			loadedClasses = new LoadedClassSet();

		result = state.storeExecutionInfo(LoadedClassSet.INFO_KEY, loadedClasses.add(t));
		return result;
	}

	public static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> boolean isClassLoaded(
			AnalysisState<A> state,
			Type t) {

		LoadedClassSet loadedClasses = state.getExecutionInfo(LoadedClassSet.INFO_KEY, LoadedClassSet.class);
		if (loadedClasses == null)
			return false;

		return loadedClasses.elements.contains(t);

	}

	public static SymbolicExpression getLoadedClassHandle(
			Type t,
			CodeLocation loc) {
		String s = "__" + t.toString();
		JavaReferenceType r = new JavaReferenceType(JavaClassType.getClassMetaType());
		return new GlobalVariable(r, s, loc);
	}

}

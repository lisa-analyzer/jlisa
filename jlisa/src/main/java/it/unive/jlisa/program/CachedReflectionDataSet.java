package it.unive.jlisa.program;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.lattices.SetLattice;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import it.unive.lisa.type.Type;

/* A set containing the classes that have their reflection data cached.
* In the concrete world, reflection data (methods and fields) is
* loaded during getField/getMethod like operations.
* This is used every time getField/getMethod is invoked, if the reflection data is not loaded,
* then it must be cached before returning a handle to the meta object.
*/

public class CachedReflectionDataSet extends SetLattice<CachedReflectionDataSet, Type> {

	public static final String INFO_KEY = "cached-reflection-data";

	public CachedReflectionDataSet() {
		super(new HashSet<Type>(), false);
	}

	public CachedReflectionDataSet(
			Set<Type> elements) {
		super(elements, elements.isEmpty());
	}

	public CachedReflectionDataSet(
			Set<Type> elements,
			boolean isTop) {
		super(elements, isTop);
	}

	@Override
	public CachedReflectionDataSet wideningAux(
			CachedReflectionDataSet other)
			throws SemanticException {
		CachedReflectionDataSet res = new CachedReflectionDataSet(elements, isTop);
		res.elements.addAll(other.elements);
		return res;
	}

	@Override
	public CachedReflectionDataSet top() {
		return new CachedReflectionDataSet(Collections.emptySet(), true);
	}

	@Override
	public CachedReflectionDataSet bottom() {
		return new CachedReflectionDataSet(Collections.emptySet(), false);
	}

	@Override
	public CachedReflectionDataSet mk(
			Set<Type> set) {
		return new CachedReflectionDataSet(set);
	}

	private CachedReflectionDataSet add(
			Type id) {
		Set<Type> res = new HashSet<>(elements);
		res.add(id);
		return new CachedReflectionDataSet(res);
	}

	@Override
	public StructuredRepresentation representation() {
		if (isTop())
			return new StringRepresentation("{}");
		return super.representation();
	}

	public static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> cacheReflectionData(
			AnalysisState<A> state,
			InterproceduralAnalysis<A, D> interprocedural,
			Type t) {

		AnalysisState<A> result = state;
		CachedReflectionDataSet loadedClasses = state.getExecutionInfo(CachedReflectionDataSet.INFO_KEY, CachedReflectionDataSet.class);
		if (loadedClasses == null)
			loadedClasses = new CachedReflectionDataSet();

		result = state.storeExecutionInfo(CachedReflectionDataSet.INFO_KEY, loadedClasses.add(t));
		return result;
	}

	public static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> boolean isClassReflectionDataCached(
			AnalysisState<A> state,
			Type t) {

		CachedReflectionDataSet loadedClasses = state.getExecutionInfo(CachedReflectionDataSet.INFO_KEY, CachedReflectionDataSet.class);
		if (loadedClasses == null)
			return false;

		return loadedClasses.elements.contains(t);

	}
}


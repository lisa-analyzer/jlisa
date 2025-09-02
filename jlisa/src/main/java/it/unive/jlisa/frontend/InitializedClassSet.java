package it.unive.jlisa.frontend;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.InverseSetLattice;
import it.unive.lisa.lattices.symbolic.DefiniteIdSet;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

public class InitializedClassSet extends InverseSetLattice<InitializedClassSet, String> {

	public static final String INFO_KEY = "clinit";
	
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
	 * Adds a new {@link Identifier} to this set. This method has no side
	 * effect: a new {@link DefiniteIdSet} is created, modified and returned.
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
}

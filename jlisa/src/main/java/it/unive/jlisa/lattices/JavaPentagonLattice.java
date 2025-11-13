package it.unive.jlisa.lattices;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.combination.ValueCartesianCombination;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.lattices.symbolic.DefiniteIdSet;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.representation.MapRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;

public class JavaPentagonLattice
		extends
		ValueCartesianCombination<JavaPentagonLattice, ValueEnvironment<IntInterval>, ValueEnvironment<DefiniteIdSet>> {

	/**
	 * Builds a new reduced product between intervals and upper bounds.
	 */
	public JavaPentagonLattice() {
		super(
				new ValueEnvironment<>(IntInterval.TOP),
				new ValueEnvironment<>(new DefiniteIdSet(Collections.emptySet())));
	}

	/**
	 * Builds a new reduced product between the given intervals and upper
	 * bounds.
	 *
	 * @param first  the intervals
	 * @param second the upper bounds
	 */
	public JavaPentagonLattice(
			ValueEnvironment<IntInterval> first,
			ValueEnvironment<DefiniteIdSet> second) {
		super(first, second);
	}

	@Override
	public JavaPentagonLattice mk(
			ValueEnvironment<IntInterval> first,
			ValueEnvironment<DefiniteIdSet> second) {
		return new JavaPentagonLattice(first, second);
	}

	@Override
	public boolean lessOrEqualAux(
			JavaPentagonLattice other)
			throws SemanticException {
		if (!first.lessOrEqual(other.first))
			return false;

		for (Entry<Identifier, DefiniteIdSet> entry : other.second)
			for (Identifier bound : entry.getValue()) {
				if (second.getState(entry.getKey()).contains(bound))
					continue;

				IntInterval state = first.getState(entry.getKey());
				IntInterval boundState = first.getState(bound);
				if (state.isBottom() || boundState.isTop() || state.getHigh().compareTo(boundState.getLow()) < 0)
					continue;

				return false;
			}

		return true;
	}

	@Override
	public JavaPentagonLattice lubAux(
			JavaPentagonLattice other)
			throws SemanticException {
		ValueEnvironment<IntInterval> newIntervals = first.lub(other.first);

		// lub performs the intersection between the two
		// this effectively builds s'
		ValueEnvironment<DefiniteIdSet> newBounds = second.lub(other.second);

		// the following builds s''
		for (Identifier x : second.getKeys()) {
			DefiniteIdSet closure = newBounds.getState(x);

			IntInterval b2_x = other.first.getState(x);
			if (!b2_x.isBottom()) {
				for (Identifier y : second.getState(x)) {
					IntInterval b2_y = other.first.getState(y);
					if (!b2_y.isBottom() && b2_x.getHigh().compareTo(b2_y.getLow()) < 0) {
						closure = closure.add(y);
					}
				}
			}

			newBounds = newBounds.putState(x, closure);
		}

		// the following builds s'''
		for (Identifier x : other.second.getKeys()) {
			DefiniteIdSet closure = newBounds.getState(x);

			IntInterval b1_x = first.getState(x);
			if (!b1_x.isBottom())
				for (Identifier y : other.second.getState(x)) {
					IntInterval b1_y = first.getState(y);
					if (!b1_y.isBottom() && b1_x.getHigh().compareTo(b1_y.getLow()) < 0)
						closure = closure.add(y);
				}

			newBounds = newBounds.putState(x, closure);
		}

		return new JavaPentagonLattice(newIntervals, newBounds);
	}

	/**
	 * Performs a reduction of the current state, refining the upper bounds
	 * based on the intervals. This is a closure operation that ensures that the
	 * upper bounds are consistent with the intervals.
	 *
	 * @return the reduced state
	 *
	 * @throws SemanticException if an error occurs during the reduction
	 */
	public JavaPentagonLattice closure()
			throws SemanticException {
		ValueEnvironment<DefiniteIdSet> newBounds = new ValueEnvironment<>(second.lattice, second.getMap());

		for (Identifier id1 : first.getKeys()) {
			Set<Identifier> closure = new HashSet<>();
			for (Identifier id2 : first.getKeys())
				if (!id1.equals(id2)) {
					IntInterval firstInt = first.getState(id1);
					IntInterval secondInt = first.getState(id2);
					if (firstInt.isBottom() || secondInt.isBottom())
						continue;
					if (first.getState(id1).getHigh().compareTo(first.getState(id2).getLow()) < 0)
						closure.add(id2);
				}
			if (!closure.isEmpty())
				// glb is the union
				newBounds = newBounds.putState(id1, newBounds.getState(id1).glb(new DefiniteIdSet(closure)));

		}

		return new JavaPentagonLattice(first, newBounds);
	}

	@Override
	public JavaPentagonLattice store(
			Identifier target,
			Identifier source)
			throws SemanticException {
		return new JavaPentagonLattice(first.store(target, source), second.store(target, source));
	}

	@Override
	public StructuredRepresentation representation() {
		if (isTop())
			return Lattice.topRepresentation();
		if (isBottom())
			return Lattice.bottomRepresentation();
		Map<StructuredRepresentation, StructuredRepresentation> mapping = new HashMap<>();
		for (Identifier id : CollectionUtils.union(first.getKeys(), second.getKeys()))
			mapping.put(
					new StringRepresentation(id),
					new StringRepresentation(
							first.getState(id).toString() + ", " + second.getState(id).representation()));
		return new MapRepresentation(mapping);
	}

}
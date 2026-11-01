package it.unive.jlisa.lattices;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.value.ValueLattice;
import it.unive.lisa.lattices.symbolic.DefiniteIdSet;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.util.representation.ListRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The upper bounds of the identifiers (for each identifier, the identifiers
 * that are surely greater than it, as in the Pentagons of Logozzo and
 * Fähndrich, and optionally the ones that are surely greater than or equal to
 * it), as a lattice where the relations hold on every execution:
 * <ul>
 * <li>the least upper bound keeps the relations that hold in both elements (the
 * one of {@link ValueEnvironment} also keeps the identifiers known by one
 * element only, with relations that may not hold in the other);</li>
 * <li>the identifiers inside the relations are renamed when a scope is pushed
 * or popped, and removed when they are forgotten, so that a relation keeps
 * naming the same variables.</li>
 * </ul>
 *
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public class UpperBoundsLattice
		implements
		BaseLattice<UpperBoundsLattice>,
		ValueLattice<UpperBoundsLattice> {

	private final ValueEnvironment<DefiniteIdSet> bounds;

	// for each identifier x, the identifiers y with x <= y (empty when the
	// upper bounds keep the strict relations only)
	private final ValueEnvironment<DefiniteIdSet> atMost;

	/**
	 * Builds the lattice element, with the strict relations only.
	 *
	 * @param bounds the upper bounds of each identifier
	 */
	public UpperBoundsLattice(
			ValueEnvironment<DefiniteIdSet> bounds) {
		this(bounds, new ValueEnvironment<>(bounds.lattice));
	}

	/**
	 * Builds the lattice element.
	 *
	 * @param bounds the upper bounds of each identifier
	 * @param atMost for each identifier x, the identifiers y with x <= y
	 */
	public UpperBoundsLattice(
			ValueEnvironment<DefiniteIdSet> bounds,
			ValueEnvironment<DefiniteIdSet> atMost) {
		this.bounds = bounds;
		this.atMost = atMost;
	}

	/**
	 * Returns, for each identifier x, the identifiers y with x <= y.
	 *
	 * @return the non-strict relations
	 */
	public ValueEnvironment<DefiniteIdSet> getAtMost() {
		return atMost;
	}

	/**
	 * Returns the upper bounds of each identifier.
	 *
	 * @return the upper bounds
	 */
	public ValueEnvironment<DefiniteIdSet> getBounds() {
		return bounds;
	}

	@Override
	public UpperBoundsLattice top() {
		return new UpperBoundsLattice(bounds.top(), atMost.top());
	}

	@Override
	public UpperBoundsLattice bottom() {
		return new UpperBoundsLattice(bounds.bottom(), new ValueEnvironment<>(atMost.lattice));
	}

	@Override
	public boolean isTop() {
		// a map without relations is top
		return bounds.isTop() && (atMost.isTop() || atMost.getKeys().isEmpty());
	}

	@Override
	public boolean isBottom() {
		return bounds.isBottom();
	}

	@Override
	public UpperBoundsLattice lubAux(
			UpperBoundsLattice other)
			throws SemanticException {
		if (isBottom())
			return other;
		if (other.isBottom())
			return this;
		return new UpperBoundsLattice(join(bounds, other.bounds), join(atMost, other.atMost));
	}

	@Override
	public UpperBoundsLattice wideningAux(
			UpperBoundsLattice other)
			throws SemanticException {
		// the relations of an identifier can only decrease along a chain
		return lubAux(other);
	}

	@Override
	public UpperBoundsLattice glbAux(
			UpperBoundsLattice other)
			throws SemanticException {
		return new UpperBoundsLattice(meet(bounds, other.bounds), meet(atMost, other.atMost));
	}

	@Override
	public UpperBoundsLattice narrowingAux(
			UpperBoundsLattice other)
			throws SemanticException {
		return glbAux(other);
	}

	@Override
	public boolean lessOrEqualAux(
			UpperBoundsLattice other)
			throws SemanticException {
		return contains(bounds, other.bounds) && (other.atMost.isBottom() || other.atMost.isTop()
				|| other.atMost.getKeys().isEmpty() || contains(atMost, other.atMost));
	}

	/**
	 * Returns, for each identifier, the relations holding in both the given
	 * maps (an identifier missing from a map has no relations).
	 *
	 * @param a the first map
	 * @param b the second map
	 *
	 * @return the relations holding in both
	 *
	 * @throws SemanticException if an error occurs during the computation
	 */
	public static ValueEnvironment<DefiniteIdSet> join(
			ValueEnvironment<DefiniteIdSet> a,
			ValueEnvironment<DefiniteIdSet> b)
			throws SemanticException {
		if (a.isBottom())
			return b;
		if (b.isBottom())
			return a;
		ValueEnvironment<DefiniteIdSet> result = new ValueEnvironment<>(a.lattice);
		Set<Identifier> keys = b.getKeys();
		for (Identifier key : a.getKeys())
			if (keys.contains(key)) {
				Set<Identifier> common = new HashSet<>(a.getState(key).elements);
				common.retainAll(b.getState(key).elements);
				if (!common.isEmpty())
					result = result.putState(key, new DefiniteIdSet(common));
			}
		return result;
	}

	// for each identifier, the relations holding in either
	private static ValueEnvironment<DefiniteIdSet> meet(
			ValueEnvironment<DefiniteIdSet> a,
			ValueEnvironment<DefiniteIdSet> b)
			throws SemanticException {
		if (a.isBottom())
			return a;
		if (b.isBottom())
			return b;
		ValueEnvironment<DefiniteIdSet> result = a;
		for (Identifier key : b.getKeys()) {
			Set<Identifier> all = new HashSet<>(a.getState(key).elements);
			all.addAll(b.getState(key).elements);
			if (!all.isEmpty())
				result = result.putState(key, new DefiniteIdSet(all));
		}
		return result;
	}

	// whether a has all the relations of b
	private static boolean contains(
			ValueEnvironment<DefiniteIdSet> a,
			ValueEnvironment<DefiniteIdSet> b) {
		if (a.isBottom())
			return true;
		if (b.isBottom())
			return false;
		for (Identifier key : b.getKeys())
			if (!a.getState(key).elements.containsAll(b.getState(key).elements))
				return false;
		return true;
	}

	@Override
	public boolean knowsIdentifier(
			Identifier id) {
		return bounds.knowsIdentifier(id) || atMost.knowsIdentifier(id);
	}

	@Override
	public UpperBoundsLattice store(
			Identifier target,
			Identifier source)
			throws SemanticException {
		return new UpperBoundsLattice(bounds.store(target, source), atMost.store(target, source));
	}

	@Override
	public UpperBoundsLattice pushScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		Lifter lifter = id -> (Identifier) id.pushScope(token, pp);
		return new UpperBoundsLattice(liftElements(bounds.pushScope(token, pp), lifter),
				liftElements(atMost.pushScope(token, pp), lifter));
	}

	@Override
	public UpperBoundsLattice popScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		Lifter lifter = id -> (Identifier) id.popScope(token, pp);
		return new UpperBoundsLattice(liftElements(bounds.popScope(token, pp), lifter),
				liftElements(atMost.popScope(token, pp), lifter));
	}

	@Override
	public UpperBoundsLattice forgetIdentifier(
			Identifier id,
			ProgramPoint pp)
			throws SemanticException {
		return forgetIdentifiersIf(id::equals, pp);
	}

	@Override
	public UpperBoundsLattice forgetIdentifiers(
			Iterable<Identifier> ids,
			ProgramPoint pp)
			throws SemanticException {
		Set<Identifier> forgotten = new HashSet<>();
		ids.forEach(forgotten::add);
		return forgetIdentifiersIf(forgotten::contains, pp);
	}

	@Override
	public UpperBoundsLattice forgetIdentifiersIf(
			Predicate<Identifier> test,
			ProgramPoint pp)
			throws SemanticException {
		// the forgotten identifiers are removed from the relations of the
		// others too, otherwise they would be related to the others when
		// assigned again
		Lifter lifter = id -> test.test(id) ? null : id;
		return new UpperBoundsLattice(liftElements(bounds.forgetIdentifiersIf(test, pp), lifter),
				liftElements(atMost.forgetIdentifiersIf(test, pp), lifter));
	}

	// a renaming of identifiers, giving null for an identifier to drop
	@FunctionalInterface
	private interface Lifter {
		Identifier lift(
				Identifier id)
				throws SemanticException;
	}

	// the relations where the related identifiers are renamed as the keys of
	// the map were (e.g., when a scope is pushed or popped), so that the
	// relations keep naming the same variables
	private static ValueEnvironment<DefiniteIdSet> liftElements(
			ValueEnvironment<DefiniteIdSet> env,
			Lifter lifter)
			throws SemanticException {
		if (env.isBottom() || env.isTop() || env.getKeys().isEmpty())
			return env;
		// the map is rebuilt only if some related identifier changes
		ValueEnvironment<DefiniteIdSet> result = env;
		for (Identifier key : env.getKeys()) {
			Set<Identifier> elements = env.getState(key).elements;
			Set<Identifier> lifted = null;
			for (Identifier element : elements) {
				Identifier l = lifter.lift(element);
				if (lifted == null && l != element) {
					lifted = new HashSet<>();
					for (Identifier previous : elements) {
						if (previous == element)
							break;
						lifted.add(previous);
					}
				}
				if (lifted != null && l != null)
					lifted.add(l);
			}
			if (lifted == null)
				continue;
			if (lifted.isEmpty())
				result = result.forgetIdentifier(key, null);
			else
				result = result.putState(key, new DefiniteIdSet(lifted));
		}
		return result;
	}

	@Override
	public StructuredRepresentation representation() {
		if (atMost.isBottom() || atMost.isTop() || atMost.getKeys().isEmpty())
			return bounds.representation();
		return new ListRepresentation(bounds.representation(), atMost.representation());
	}

	@Override
	public int hashCode() {
		return Objects.hash(bounds, atMost);
	}

	@Override
	public boolean equals(
			Object obj) {
		return obj instanceof UpperBoundsLattice other && Objects.equals(bounds, other.bounds)
				&& Objects.equals(atMost, other.atMost);
	}

	@Override
	public String toString() {
		return atMost.isBottom() || atMost.isTop() || atMost.getKeys().isEmpty() ? bounds.toString()
				: bounds + " " + atMost;
	}
}

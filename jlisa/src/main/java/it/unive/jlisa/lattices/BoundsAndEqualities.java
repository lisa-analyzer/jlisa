package it.unive.jlisa.lattices;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.combination.ValueCartesianCombination;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.lattices.symbolic.DefiniteIdSet;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The relations between identifiers used by
 * {@link it.unive.jlisa.analysis.value.ConstantPropagationWithRelations}: for
 * each identifier, the identifiers surely greater than it (upper bounds) and
 * the identifiers surely equal to it. Both are relations that must hold: an
 * identifier missing from a map has no relations. The operations of
 * {@link ValueEnvironment} instead treat a missing identifier as bottom (right
 * for the values of the identifiers, not for these relations): a relation
 * holding on one branch only would survive a join. Here a join keeps, for each
 * identifier, the relations holding on both branches, and an element is smaller
 * than another if it has at least all its relations.
 *
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public class BoundsAndEqualities
		extends
		ValueCartesianCombination<BoundsAndEqualities, ValueEnvironment<DefiniteIdSet>,
				ValueEnvironment<DefiniteIdSet>> {

	/**
	 * Builds the relations.
	 *
	 * @param bounds     the upper bounds of each identifier
	 * @param equalities the identifiers equal to each identifier
	 */
	public BoundsAndEqualities(
			ValueEnvironment<DefiniteIdSet> bounds,
			ValueEnvironment<DefiniteIdSet> equalities) {
		super(bounds, equalities);
	}

	@Override
	public BoundsAndEqualities mk(
			ValueEnvironment<DefiniteIdSet> first,
			ValueEnvironment<DefiniteIdSet> second) {
		return new BoundsAndEqualities(first, second);
	}

	@Override
	public BoundsAndEqualities store(
			Identifier target,
			Identifier source)
			throws SemanticException {
		return new BoundsAndEqualities(first.store(target, source), second.store(target, source));
	}

	@Override
	public BoundsAndEqualities lubAux(
			BoundsAndEqualities other)
			throws SemanticException {
		return mk(join(first, other.first), join(second, other.second));
	}

	@Override
	public BoundsAndEqualities upchainAux(
			BoundsAndEqualities other)
			throws SemanticException {
		return lubAux(other);
	}

	@Override
	public BoundsAndEqualities wideningAux(
			BoundsAndEqualities other)
			throws SemanticException {
		// the relations of an identifier can only decrease along a chain
		return lubAux(other);
	}

	@Override
	public BoundsAndEqualities glbAux(
			BoundsAndEqualities other)
			throws SemanticException {
		return mk(meet(first, other.first), meet(second, other.second));
	}

	@Override
	public BoundsAndEqualities downchainAux(
			BoundsAndEqualities other)
			throws SemanticException {
		return glbAux(other);
	}

	@Override
	public BoundsAndEqualities narrowingAux(
			BoundsAndEqualities other)
			throws SemanticException {
		return glbAux(other);
	}

	@Override
	public boolean lessOrEqualAux(
			BoundsAndEqualities other)
			throws SemanticException {
		return contains(first, other.first) && contains(second, other.second);
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
	public BoundsAndEqualities pushScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		Lifter lifter = id -> (Identifier) id.pushScope(token, pp);
		return mk(liftElements(first.pushScope(token, pp), lifter), liftElements(second.pushScope(token, pp), lifter));
	}

	@Override
	public BoundsAndEqualities popScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		Lifter lifter = id -> (Identifier) id.popScope(token, pp);
		return mk(liftElements(first.popScope(token, pp), lifter), liftElements(second.popScope(token, pp), lifter));
	}

	@Override
	public BoundsAndEqualities forgetIdentifier(
			Identifier id,
			ProgramPoint pp)
			throws SemanticException {
		return forgetIdentifiersIf(id::equals, pp);
	}

	@Override
	public BoundsAndEqualities forgetIdentifiers(
			Iterable<Identifier> ids,
			ProgramPoint pp)
			throws SemanticException {
		Set<Identifier> forgotten = new HashSet<>();
		ids.forEach(forgotten::add);
		return forgetIdentifiersIf(forgotten::contains, pp);
	}

	@Override
	public BoundsAndEqualities forgetIdentifiersIf(
			Predicate<Identifier> test,
			ProgramPoint pp)
			throws SemanticException {
		// the forgotten identifiers are removed from the relations of the
		// others too, otherwise they would be related to the others when
		// assigned again
		Lifter lifter = id -> test.test(id) ? null : id;
		return mk(liftElements(first.forgetIdentifiersIf(test, pp), lifter),
				liftElements(second.forgetIdentifiersIf(test, pp), lifter));
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
		if (env.isBottom() || env.getKeys().isEmpty())
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

}

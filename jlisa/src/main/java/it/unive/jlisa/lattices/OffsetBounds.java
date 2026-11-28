package it.unive.jlisa.lattices;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.OutOfScopeIdentifier;
import it.unive.lisa.util.representation.MapRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * Relations with an offset between integral identifiers: for each identifier
 * {@code x}, the identifiers {@code y} with {@code x <= y + c}, with the
 * smallest known integer constant {@code c} (e.g., {@code c = -1} for
 * {@code x < y}, {@code c = 0} for {@code x <= y}, {@code c = 1} after
 * {@code x = x + 1} when {@code x <= y} held). The relations are meant on the
 * integers, without overflow: whoever adds one must make sure that it holds on
 * the values of the program. A relation that is missing is not known (the
 * element with no relations is the top one). Elements are immutable.
 * <p>
 * This is the part of the domain Stripes of Clousot with one variable and
 * coefficient 1 ({@code x - y > -c - 1}), kept without closure: as in the
 * Pentagons, the consequences of more relations are only computed when a
 * comparison is checked.
 *
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public final class OffsetBounds {

	/**
	 * The element without relations.
	 */
	public static final OffsetBounds EMPTY = new OffsetBounds(Collections.emptyMap());

	// x -> (y -> c), for x <= y + c; never empty inner maps
	private final Map<Identifier, Map<Identifier, Long>> relations;

	// the identifiers appearing in the relations, computed when first needed
	// (the element is immutable)
	private Set<Identifier> identifiers;

	private OffsetBounds(
			Map<Identifier, Map<Identifier, Long>> relations) {
		this.relations = relations;
	}

	/**
	 * Returns whether there are no relations.
	 *
	 * @return whether there are no relations
	 */
	public boolean isEmpty() {
		return relations.isEmpty();
	}

	/**
	 * Returns the offset {@code c} of the relation {@code x <= y + c}, or
	 * {@code null} if it is not known.
	 *
	 * @param x the smaller side
	 * @param y the identifier of the greater side
	 *
	 * @return the offset, or {@code null}
	 */
	public Long get(
			Identifier x,
			Identifier y) {
		Map<Identifier, Long> m = relations.get(x);
		return m == null ? null : m.get(y);
	}

	/**
	 * Returns the relations of the given identifier as the smaller side:
	 * {@code y -> c} for {@code x <= y + c}.
	 *
	 * @param x the identifier
	 *
	 * @return the relations (not modifiable)
	 */
	public Map<Identifier, Long> above(
			Identifier x) {
		Map<Identifier, Long> m = relations.get(x);
		return m == null ? Collections.emptyMap() : Collections.unmodifiableMap(m);
	}

	/**
	 * Returns the relations of the given identifier as the greater side:
	 * {@code x -> c} for {@code x <= y + c}.
	 *
	 * @param y the identifier
	 *
	 * @return the relations
	 */
	public Map<Identifier, Long> below(
			Identifier y) {
		Map<Identifier, Long> result = new HashMap<>();
		for (Map.Entry<Identifier, Map<Identifier, Long>> e : relations.entrySet()) {
			Long c = e.getValue().get(y);
			if (c != null)
				result.put(e.getKey(), c);
		}
		return result;
	}

	/**
	 * Returns the identifiers appearing in the relations.
	 *
	 * @return the identifiers
	 */
	public Set<Identifier> identifiers() {
		if (identifiers != null)
			return identifiers;
		Set<Identifier> result = new java.util.HashSet<>(relations.keySet());
		for (Map<Identifier, Long> m : relations.values())
			result.addAll(m.keySet());
		identifiers = Collections.unmodifiableSet(result);
		return identifiers;
	}

	/**
	 * Returns the element with the relation {@code x <= y + c} too, keeping the
	 * stronger offset if the relation is already known. Relations of an
	 * identifier with itself are not kept.
	 *
	 * @param x the smaller side
	 * @param y the identifier of the greater side
	 * @param c the offset
	 *
	 * @return the resulting element
	 */
	public OffsetBounds add(
			Identifier x,
			Identifier y,
			long c) {
		if (x.equals(y))
			return this;
		if ((x instanceof OutOfScopeIdentifier) != (y instanceof OutOfScopeIdentifier))
			// no relations between an enclosing scope and the current one
			// (see withoutMixed)
			return this;
		Long old = get(x, y);
		if (old != null && old <= c)
			return this;
		Map<Identifier, Map<Identifier, Long>> copy = new HashMap<>(relations);
		Map<Identifier, Long> m = new HashMap<>(above(x));
		m.put(y, c);
		copy.put(x, m);
		return new OffsetBounds(copy);
	}

	/**
	 * Returns the element without the relations of the given identifier (on
	 * either side).
	 *
	 * @param id the identifier
	 *
	 * @return the resulting element
	 */
	public OffsetBounds drop(
			Identifier id) {
		if (relations.isEmpty() || !identifiers().contains(id))
			return this;
		return rename(i -> i.equals(id) ? null : i);
	}

	/**
	 * Returns the element without the relations between an identifier of an
	 * enclosing scope (an {@link OutOfScopeIdentifier}, e.g., a variable of the
	 * caller while a callee is analyzed) and one of the current scope. They are
	 * dropped when a scope is pushed: in a recursion, they would relate each
	 * level to the previous one with a different offset, so that the states
	 * entering the levels would never repeat. The relations among the
	 * identifiers of the enclosing scopes are kept, so that they hold again
	 * when the scope is popped.
	 *
	 * @return the resulting element
	 */
	public OffsetBounds withoutMixed() {
		if (relations.isEmpty())
			return this;
		Map<Identifier, Map<Identifier, Long>> copy = new HashMap<>();
		boolean changed = false;
		for (Map.Entry<Identifier, Map<Identifier, Long>> e : relations.entrySet()) {
			boolean outer = e.getKey() instanceof OutOfScopeIdentifier;
			Map<Identifier, Long> m = new HashMap<>();
			for (Map.Entry<Identifier, Long> r : e.getValue().entrySet())
				if ((r.getKey() instanceof OutOfScopeIdentifier) == outer)
					m.put(r.getKey(), r.getValue());
				else
					changed = true;
			if (!m.isEmpty())
				copy.put(e.getKey(), m);
		}
		return changed ? new OffsetBounds(copy) : this;
	}

	/**
	 * Returns the element where the offsets of the relations of the given
	 * identifier are shifted for {@code id = id + k}: {@code id <= y + c}
	 * becomes {@code id <= y + c + k}, and {@code x <= id + c} becomes
	 * {@code x <= id + c - k}. Relations whose offset would not fit a
	 * {@code long} are dropped. The caller must make sure that {@code id + k}
	 * does not overflow.
	 *
	 * @param id the identifier
	 * @param k  the constant added to it
	 *
	 * @return the resulting element
	 */
	public OffsetBounds shift(
			Identifier id,
			long k) {
		Map<Identifier, Map<Identifier, Long>> copy = new HashMap<>();
		for (Map.Entry<Identifier, Map<Identifier, Long>> e : relations.entrySet()) {
			Map<Identifier, Long> m = new HashMap<>();
			boolean key = e.getKey().equals(id);
			for (Map.Entry<Identifier, Long> r : e.getValue().entrySet()) {
				try {
					if (key)
						m.put(r.getKey(), Math.addExact(r.getValue(), k));
					else if (r.getKey().equals(id))
						m.put(r.getKey(), Math.subtractExact(r.getValue(), k));
					else
						m.put(r.getKey(), r.getValue());
				} catch (ArithmeticException ex) {
					// dropped
				}
			}
			if (!m.isEmpty())
				copy.put(e.getKey(), m);
		}
		return new OffsetBounds(copy);
	}

	/**
	 * A renaming of identifiers, giving {@code null} for an identifier whose
	 * relations are dropped.
	 */
	@FunctionalInterface
	public interface Renaming {

		/**
		 * Returns the new identifier, or {@code null} to drop it.
		 *
		 * @param id the identifier
		 *
		 * @return the new identifier, or {@code null}
		 *
		 * @throws SemanticException if the renaming fails
		 */
		Identifier rename(
				Identifier id)
				throws SemanticException;
	}

	private interface SafeRenaming {
		Identifier rename(
				Identifier id);
	}

	private OffsetBounds rename(
			SafeRenaming renaming) {
		try {
			return renameAll(renaming::rename);
		} catch (SemanticException e) {
			// a safe renaming does not throw
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Returns the element where every identifier (on either side) is renamed:
	 * the relations of the identifiers renamed to {@code null} are dropped.
	 * Relations that become relations of an identifier with itself are dropped,
	 * and two relations that become the same keep the stronger offset.
	 *
	 * @param renaming the renaming
	 *
	 * @return the resulting element
	 *
	 * @throws SemanticException if the renaming fails
	 */
	public OffsetBounds renameAll(
			Renaming renaming)
			throws SemanticException {
		if (relations.isEmpty())
			return this;
		// nothing changes if no identifier of the relations is renamed
		Map<Identifier, Identifier> cache = new HashMap<>();
		boolean any = false;
		for (Identifier id : identifiers())
			if (renamed(id, renaming, cache) != id) {
				any = true;
				break;
			}
		if (!any)
			return this;
		boolean changed = false;
		Map<Identifier, Map<Identifier, Long>> copy = new HashMap<>();
		for (Map.Entry<Identifier, Map<Identifier, Long>> e : relations.entrySet()) {
			Identifier x = renamed(e.getKey(), renaming, cache);
			if (x != e.getKey())
				changed = true;
			if (x == null)
				continue;
			for (Map.Entry<Identifier, Long> r : e.getValue().entrySet()) {
				Identifier y = renamed(r.getKey(), renaming, cache);
				if (y != r.getKey())
					changed = true;
				if (y == null || y.equals(x))
					continue;
				Map<Identifier, Long> m = copy.computeIfAbsent(x, k -> new HashMap<>());
				Long old = m.get(y);
				m.put(y, old == null ? r.getValue() : Math.min(old, r.getValue()));
			}
		}
		if (!changed)
			return this;
		copy.values().removeIf(Map::isEmpty);
		return new OffsetBounds(copy);
	}

	private static Identifier renamed(
			Identifier id,
			Renaming renaming,
			Map<Identifier, Identifier> cache)
			throws SemanticException {
		if (cache.containsKey(id))
			return cache.get(id);
		Identifier r = renaming.rename(id);
		cache.put(id, r);
		return r;
	}

	/**
	 * Returns the relations holding in both elements, with the weaker (larger)
	 * offset.
	 *
	 * @param other the other element
	 *
	 * @return the least upper bound
	 */
	public OffsetBounds lub(
			OffsetBounds other) {
		return combine(other, false);
	}

	/**
	 * Returns the relations holding in both elements whose offset in the other
	 * element is not larger (the widening: an offset that grows along an
	 * iteration is dropped, so that the iterations stabilize).
	 *
	 * @param other the other element (the later one)
	 *
	 * @return the widening
	 */
	public OffsetBounds widening(
			OffsetBounds other) {
		return combine(other, true);
	}

	private OffsetBounds combine(
			OffsetBounds other,
			boolean widening) {
		if (relations.isEmpty() || other.relations.isEmpty())
			return EMPTY;
		Map<Identifier, Map<Identifier, Long>> result = new HashMap<>();
		for (Map.Entry<Identifier, Map<Identifier, Long>> e : relations.entrySet()) {
			Map<Identifier, Long> o = other.relations.get(e.getKey());
			if (o == null)
				continue;
			Map<Identifier, Long> m = new HashMap<>();
			for (Map.Entry<Identifier, Long> r : e.getValue().entrySet()) {
				Long c = o.get(r.getKey());
				if (c == null)
					continue;
				if (widening && c > r.getValue())
					continue;
				m.put(r.getKey(), Math.max(c, r.getValue()));
			}
			if (!m.isEmpty())
				result.put(e.getKey(), m);
		}
		return result.isEmpty() ? EMPTY : new OffsetBounds(result);
	}

	/**
	 * Returns the relations holding in either element, with the stronger
	 * (smaller) offset.
	 *
	 * @param other the other element
	 *
	 * @return the greatest lower bound
	 */
	public OffsetBounds glb(
			OffsetBounds other) {
		if (other.relations.isEmpty())
			return this;
		OffsetBounds result = this;
		for (Map.Entry<Identifier, Map<Identifier, Long>> e : other.relations.entrySet())
			for (Map.Entry<Identifier, Long> r : e.getValue().entrySet())
				result = result.add(e.getKey(), r.getKey(), r.getValue());
		return result;
	}

	/**
	 * Returns whether this element has all the relations of the other, with an
	 * offset that is not larger.
	 *
	 * @param other the other element
	 *
	 * @return whether this element is less than or equal to the other
	 */
	public boolean lessOrEqual(
			OffsetBounds other) {
		for (Map.Entry<Identifier, Map<Identifier, Long>> e : other.relations.entrySet())
			for (Map.Entry<Identifier, Long> r : e.getValue().entrySet()) {
				Long c = get(e.getKey(), r.getKey());
				if (c == null || c > r.getValue())
					return false;
			}
		return true;
	}

	/**
	 * Returns a representation of the relations, as {@code x -> {y + c}}.
	 *
	 * @return the representation
	 */
	public StructuredRepresentation representation() {
		Map<StructuredRepresentation, StructuredRepresentation> map = new TreeMap<>();
		for (Map.Entry<Identifier, Map<Identifier, Long>> e : relations.entrySet()) {
			TreeMap<String, Long> sorted = new TreeMap<>();
			for (Map.Entry<Identifier, Long> r : e.getValue().entrySet())
				sorted.put(r.getKey().toString(), r.getValue());
			StringBuilder sb = new StringBuilder();
			for (Map.Entry<String, Long> r : sorted.entrySet()) {
				if (sb.length() > 0)
					sb.append(", ");
				sb.append("<= ").append(r.getKey()).append(r.getValue() >= 0 ? " + " : " - ")
						.append(Math.abs(r.getValue()));
			}
			map.put(new StringRepresentation(e.getKey().toString()), new StringRepresentation(sb.toString()));
		}
		return new MapRepresentation(map);
	}

	@Override
	public int hashCode() {
		return Objects.hash(relations);
	}

	@Override
	public boolean equals(
			Object obj) {
		return obj instanceof OffsetBounds other && relations.equals(other.relations);
	}

	@Override
	public String toString() {
		return representation().toString();
	}
}

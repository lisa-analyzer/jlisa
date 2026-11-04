package it.unive.jlisa.lattices;

import it.unive.lisa.lattices.SetLattice;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * The kinds of value that a floating point value may have (see {@link Kind}).
 * An element of this lattice is the set of the kinds that the value may have:
 * bottom is the empty set (no value), top is the set of all the kinds (any
 * value), the least upper bound is the union, the greatest lower bound is the
 * intersection and the order is the inclusion, all inherited from
 * {@link SetLattice}. Unlike other set lattices, the set of all the elements is
 * finite: top is the set of all the kinds, not the empty set marked as top.
 *
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public class FPFlags
		extends
		SetLattice<FPFlags, FPFlags.Kind> {

	/**
	 * A kind of floating point value.
	 */
	public enum Kind {

		/**
		 * A finite number.
		 */
		FINITE("finite"),

		/**
		 * Not a number.
		 */
		NAN("NaN"),

		/**
		 * Plus infinity.
		 */
		PLUS_INF("+Inf"),

		/**
		 * Minus infinity.
		 */
		MINUS_INF("-Inf");

		private final String name;

		private Kind(
				String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	/**
	 * The top element: any kind of value.
	 */
	public static final FPFlags TOP = new FPFlags(EnumSet.allOf(Kind.class));

	/**
	 * The bottom element: no value.
	 */
	public static final FPFlags BOTTOM = new FPFlags(EnumSet.noneOf(Kind.class));

	/**
	 * Finite numbers only.
	 */
	public static final FPFlags ONLY_FINITE = of(Kind.FINITE);

	private FPFlags(
			EnumSet<Kind> kinds) {
		super(Collections.unmodifiableSet(kinds), false);
	}

	/**
	 * Returns the element with the given kinds.
	 *
	 * @param first the first kind
	 * @param rest  the other kinds
	 *
	 * @return the element
	 */
	public static FPFlags of(
			Kind first,
			Kind... rest) {
		return new FPFlags(EnumSet.of(first, rest));
	}

	/**
	 * Returns the element with the given kinds.
	 *
	 * @param kinds the kinds
	 *
	 * @return the element
	 */
	public static FPFlags of(
			Set<Kind> kinds) {
		return new FPFlags(kinds.isEmpty() ? EnumSet.noneOf(Kind.class) : EnumSet.copyOf(kinds));
	}

	/**
	 * Returns the element of the given value.
	 *
	 * @param value the value
	 *
	 * @return the element
	 */
	public static FPFlags of(
			double value) {
		if (Double.isNaN(value))
			return of(Kind.NAN);
		if (value == Double.POSITIVE_INFINITY)
			return of(Kind.PLUS_INF);
		if (value == Double.NEGATIVE_INFINITY)
			return of(Kind.MINUS_INF);
		return ONLY_FINITE;
	}

	/**
	 * Returns whether this element contains one of the infinities.
	 *
	 * @return whether the value may be infinite
	 */
	public boolean mayBeInfinite() {
		return contains(Kind.PLUS_INF) || contains(Kind.MINUS_INF);
	}

	/**
	 * Returns this element without the given kind.
	 *
	 * @param kind the kind to remove
	 *
	 * @return the element
	 */
	public FPFlags without(
			Kind kind) {
		EnumSet<Kind> kinds = EnumSet.noneOf(Kind.class);
		kinds.addAll(elements);
		kinds.remove(kind);
		return new FPFlags(kinds);
	}

	@Override
	public FPFlags top() {
		return TOP;
	}

	@Override
	public FPFlags bottom() {
		return BOTTOM;
	}

	@Override
	public boolean isTop() {
		return elements.size() == Kind.values().length;
	}

	@Override
	public FPFlags mk(
			Set<Kind> set) {
		return of(set);
	}
}

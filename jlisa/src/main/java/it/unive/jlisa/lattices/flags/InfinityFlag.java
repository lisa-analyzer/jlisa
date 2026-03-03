package it.unive.jlisa.lattices.flags;

/**
 * A non-flat 6-element lattice that tracks the IEEE 754 infiniteness of a
 * floating-point abstract value, distinguishing between positive and negative
 * infinity.
 *
 * <pre>
 *        TOP (POSSIBLY_INFINITE_OR_FINITE)
 *       /                |               \
 * DEFINITELY_INF         |       DEFINITELY_FINITE
 * (either sign)          |
 *     /       \
 * POS_INF   NEG_INF
 *     \       /
 *       BOTTOM
 * </pre>
 * <p>
 * Notable lattice operations:
 * <ul>
 * <li>{@code lub(POS_INF, NEG_INF) = DEFINITELY_INF}</li>
 * <li>{@code lub(DEFINITELY_INF, DEFINITELY_FINITE) = TOP}</li>
 * <li>{@code glb(POS_INF, NEG_INF) = BOTTOM}</li>
 * <li>{@code glb(DEFINITELY_INF, DEFINITELY_FINITE) = BOTTOM}</li>
 * </ul>
 *
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public enum InfinityFlag {

	/**
	 * Bottom element — used only in unreachable / bottom abstract states.
	 */
	BOTTOM,

	/**
	 * The value is definitely positive infinity
	 * ({@code Double.POSITIVE_INFINITY}).
	 */
	POS_INF,

	/**
	 * The value is definitely negative infinity
	 * ({@code Double.NEGATIVE_INFINITY}).
	 */
	NEG_INF,

	/**
	 * The value is definitely infinite (either positive or negative).
	 */
	DEFINITELY_INF,

	/**
	 * The value is definitely finite (not infinite, not NaN).
	 */
	DEFINITELY_FINITE,

	/**
	 * Top element — the value may be finite or infinite.
	 */
	POSSIBLY_INFINITE_OR_FINITE;

	/**
	 * Returns {@code true} if this is the top element
	 * ({@link #POSSIBLY_INFINITE_OR_FINITE}).
	 *
	 * @return {@code true} iff this == POSSIBLY_INFINITE_OR_FINITE
	 */
	public boolean isTop() {
		return this == POSSIBLY_INFINITE_OR_FINITE;
	}

	/**
	 * Returns {@code true} if this is the bottom element ({@link #BOTTOM}).
	 *
	 * @return {@code true} iff this == BOTTOM
	 */
	public boolean isBottom() {
		return this == BOTTOM;
	}

	/**
	 * Returns the least upper bound of this flag and {@code other}.
	 *
	 * @param other the other flag
	 *
	 * @return the least upper bound
	 */
	public InfinityFlag lub(
			InfinityFlag other) {
		if (this == BOTTOM)
			return other;
		if (other == BOTTOM)
			return this;
		if (this == other)
			return this;
		if (this == POSSIBLY_INFINITE_OR_FINITE || other == POSSIBLY_INFINITE_OR_FINITE)
			return POSSIBLY_INFINITE_OR_FINITE;
		// POS_INF ∪ NEG_INF = DEFINITELY_INF
		if ((this == POS_INF || this == NEG_INF) && (other == POS_INF || other == NEG_INF))
			return DEFINITELY_INF;
		// DEFINITELY_INF ∪ {POS_INF or NEG_INF} = DEFINITELY_INF
		if (this == DEFINITELY_INF && (other == POS_INF || other == NEG_INF))
			return DEFINITELY_INF;
		if (other == DEFINITELY_INF && (this == POS_INF || this == NEG_INF))
			return DEFINITELY_INF;
		// any infinity ∪ FINITE = ⊤
		return POSSIBLY_INFINITE_OR_FINITE;
	}

	/**
	 * Returns the greatest lower bound of this flag and {@code other}.
	 *
	 * @param other the other flag
	 *
	 * @return the greatest lower bound
	 */
	public InfinityFlag glb(
			InfinityFlag other) {
		if (this == POSSIBLY_INFINITE_OR_FINITE)
			return other;
		if (other == POSSIBLY_INFINITE_OR_FINITE)
			return this;
		if (this == other)
			return this;
		// DEFINITELY_INF ∩ POS_INF = POS_INF
		if (this == DEFINITELY_INF && other == POS_INF)
			return POS_INF;
		if (other == DEFINITELY_INF && this == POS_INF)
			return POS_INF;
		// DEFINITELY_INF ∩ NEG_INF = NEG_INF
		if (this == DEFINITELY_INF && other == NEG_INF)
			return NEG_INF;
		if (other == DEFINITELY_INF && this == NEG_INF)
			return NEG_INF;
		// POS_INF ∩ NEG_INF = ⊥, DEFINITELY_INF ∩ FINITE = ⊥, etc.
		return BOTTOM;
	}

	/**
	 * Returns the widened value of this flag with respect to {@code other}. For
	 * this lattice, widening equals lub.
	 *
	 * @param other the other flag
	 *
	 * @return the widened flag
	 */
	public InfinityFlag widening(
			InfinityFlag other) {
		return lub(other);
	}

	/**
	 * Returns {@code true} if {@code this ⊑ other} in the lattice order.
	 *
	 * @param other the other flag
	 *
	 * @return {@code true} iff this is less-or-equal to other
	 */
	public boolean lessOrEqual(
			InfinityFlag other) {
		if (this == BOTTOM)
			return true;
		if (other == POSSIBLY_INFINITE_OR_FINITE)
			return true;
		if (this == other)
			return true;
		// POS_INF ⊑ DEFINITELY_INF, NEG_INF ⊑ DEFINITELY_INF
		if (other == DEFINITELY_INF && (this == POS_INF || this == NEG_INF))
			return true;
		return false;
	}

	/**
	 * Returns the infinity flag for the negation of the represented value.
	 * Negating a positive infinity yields negative infinity and vice versa;
	 * other values are unaffected.
	 *
	 * @return the infinity flag after negation
	 */
	public InfinityFlag negate() {
		switch (this) {
		case POS_INF:
			return NEG_INF;
		case NEG_INF:
			return POS_INF;
		default:
			return this;
		}
	}
}

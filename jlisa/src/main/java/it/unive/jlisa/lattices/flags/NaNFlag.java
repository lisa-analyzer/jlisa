package it.unive.jlisa.lattices.flags;

/**
 * A flat 4-element lattice that tracks whether the concrete values represented
 * by an abstract floating-point element may be {@code NaN} (Not-a-Number).
 *
 * <pre>
 *           TOP (POSSIBLY_NAN)
 *          /                   \
 * DEFINITELY_NAN     DEFINITELY_NOT_NAN
 *          \                   /
 *                  BOTTOM
 * </pre>
 *
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public enum NaNFlag {

	/**
	 * Bottom element — used only in unreachable / bottom abstract states.
	 */
	BOTTOM,

	/**
	 * The abstract value is definitely {@code NaN} (e.g., {@code 0.0 / 0.0}).
	 */
	DEFINITELY_NAN,

	/**
	 * The abstract value is definitely not {@code NaN} (e.g., casting an
	 * integer to {@code double}).
	 */
	DEFINITELY_NOT_NAN,

	/**
	 * Top element — the value may or may not be {@code NaN}.
	 */
	POSSIBLY_NAN;

	/**
	 * Returns {@code true} if this is the top element ({@link #POSSIBLY_NAN}).
	 *
	 * @return {@code true} iff this == POSSIBLY_NAN
	 */
	public boolean isTop() {
		return this == POSSIBLY_NAN;
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
	public NaNFlag lub(
			NaNFlag other) {
		if (this == BOTTOM)
			return other;
		if (other == BOTTOM)
			return this;
		if (this == other)
			return this;
		return POSSIBLY_NAN;
	}

	/**
	 * Returns the greatest lower bound of this flag and {@code other}.
	 *
	 * @param other the other flag
	 *
	 * @return the greatest lower bound
	 */
	public NaNFlag glb(
			NaNFlag other) {
		if (this == POSSIBLY_NAN)
			return other;
		if (other == POSSIBLY_NAN)
			return this;
		if (this == other)
			return this;
		return BOTTOM;
	}

	/**
	 * Returns the widened value of this flag with respect to {@code other}. For
	 * flat lattices, widening equals lub.
	 *
	 * @param other the other flag
	 *
	 * @return the widened flag
	 */
	public NaNFlag widening(
			NaNFlag other) {
		return lub(other);
	}

	/**
	 * Returns {@code true} if {@code this ⊑ other} in the flat lattice order.
	 *
	 * @param other the other flag
	 *
	 * @return {@code true} iff this is less-or-equal to other
	 */
	public boolean lessOrEqual(
			NaNFlag other) {
		return other.isTop() || this.isBottom() || this == other;
	}
}

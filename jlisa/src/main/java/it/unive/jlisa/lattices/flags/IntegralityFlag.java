package it.unive.jlisa.lattices.flags;

/**
 * A flat 4-element lattice that tracks whether the concrete values represented
 * by an abstract element are always integers (whole numbers).
 *
 * <pre>
 *         TOP (UNKNOWN_INTEGRALITY)
 *        /                         \
 *  INTEGRAL                   NON_INTEGRAL
 *        \                         /
 *                  BOTTOM
 * </pre>
 * <p>
 * Integer-typed variables ({@code byte}, {@code short}, {@code int},
 * {@code long}) are always {@link #INTEGRAL} by construction. This flag is most
 * useful for {@code float}/{@code double} values.
 *
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public enum IntegralityFlag {

	/**
	 * Bottom element — used only in unreachable / bottom abstract states.
	 */
	BOTTOM,

	/**
	 * All concrete values are whole numbers (e.g., {@code 0.0}, {@code 1.0}, or
	 * the result of casting an integer to {@code double}).
	 */
	INTEGRAL,

	/**
	 * At least one concrete value is not a whole number (e.g., {@code 0.5},
	 * {@code Math.PI}).
	 */
	NON_INTEGRAL,

	/**
	 * Top element — we cannot determine whether the value is integral.
	 */
	UNKNOWN_INTEGRALITY;

	/**
	 * Returns {@code true} if this is the top element
	 * ({@link #UNKNOWN_INTEGRALITY}).
	 *
	 * @return {@code true} iff this == UNKNOWN_INTEGRALITY
	 */
	public boolean isTop() {
		return this == UNKNOWN_INTEGRALITY;
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
	public IntegralityFlag lub(
			IntegralityFlag other) {
		if (this == BOTTOM)
			return other;
		if (other == BOTTOM)
			return this;
		if (this == other)
			return this;
		return UNKNOWN_INTEGRALITY;
	}

	/**
	 * Returns the greatest lower bound of this flag and {@code other}.
	 *
	 * @param other the other flag
	 *
	 * @return the greatest lower bound
	 */
	public IntegralityFlag glb(
			IntegralityFlag other) {
		if (this == UNKNOWN_INTEGRALITY)
			return other;
		if (other == UNKNOWN_INTEGRALITY)
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
	public IntegralityFlag widening(
			IntegralityFlag other) {
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
			IntegralityFlag other) {
		return other.isTop() || this.isBottom() || this == other;
	}
}

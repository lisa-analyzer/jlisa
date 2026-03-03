package it.unive.jlisa.lattices.flags;

/**
 * A flat 4-element lattice that tracks the parity (even/odd) of an abstract
 * integer value. This flag is applicable to {@code byte}, {@code short},
 * {@code int}, and {@code long} variables.
 *
 * <pre>
 *       TOP (UNKNOWN_PARITY)
 *      /                    \
 *   EVEN                   ODD
 *      \                    /
 *               BOTTOM
 * </pre>
 *
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public enum ParityFlag {

	/**
	 * Bottom element — used only in unreachable / bottom abstract states.
	 */
	BOTTOM,

	/**
	 * All concrete values are even (divisible by 2).
	 */
	EVEN,

	/**
	 * All concrete values are odd (not divisible by 2).
	 */
	ODD,

	/**
	 * Top element — the parity of the value is unknown.
	 */
	UNKNOWN_PARITY;

	/**
	 * Returns {@code true} if this is the top element
	 * ({@link #UNKNOWN_PARITY}).
	 *
	 * @return {@code true} iff this == UNKNOWN_PARITY
	 */
	public boolean isTop() {
		return this == UNKNOWN_PARITY;
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
	public ParityFlag lub(
			ParityFlag other) {
		if (this == BOTTOM)
			return other;
		if (other == BOTTOM)
			return this;
		if (this == other)
			return this;
		return UNKNOWN_PARITY;
	}

	/**
	 * Returns the greatest lower bound of this flag and {@code other}.
	 *
	 * @param other the other flag
	 *
	 * @return the greatest lower bound
	 */
	public ParityFlag glb(
			ParityFlag other) {
		if (this == UNKNOWN_PARITY)
			return other;
		if (other == UNKNOWN_PARITY)
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
	public ParityFlag widening(
			ParityFlag other) {
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
			ParityFlag other) {
		return other.isTop() || this.isBottom() || this == other;
	}

	/**
	 * Returns the parity flag for the sum of two values with this parity and
	 * {@code other} parity. Addition XOR: EVEN+EVEN=EVEN, ODD+ODD=EVEN,
	 * EVEN+ODD=ODD.
	 *
	 * @param other the parity of the addend
	 *
	 * @return the parity of the sum
	 */
	public ParityFlag add(
			ParityFlag other) {
		if (this == BOTTOM || other == BOTTOM)
			return BOTTOM;
		if (this == UNKNOWN_PARITY || other == UNKNOWN_PARITY)
			return UNKNOWN_PARITY;
		// XOR: same parity → EVEN, different → ODD
		return this == other ? EVEN : ODD;
	}

	/**
	 * Returns the parity flag for the product of two values with this parity
	 * and {@code other} parity. Multiplication: EVEN*anything=EVEN,
	 * ODD*ODD=ODD.
	 *
	 * @param other the parity of the multiplier
	 *
	 * @return the parity of the product
	 */
	public ParityFlag multiply(
			ParityFlag other) {
		if (this == BOTTOM || other == BOTTOM)
			return BOTTOM;
		if (this == EVEN || other == EVEN)
			return EVEN;
		if (this == ODD && other == ODD)
			return ODD;
		return UNKNOWN_PARITY;
	}
}

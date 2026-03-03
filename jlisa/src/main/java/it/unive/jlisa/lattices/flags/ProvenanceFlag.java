package it.unive.jlisa.lattices.flags;

/**
 * A flat 4-element lattice that tracks the provenance (origin) of an abstract
 * value. This flag distinguishes between values that arose from
 * non-deterministic input (and are therefore <em>exact</em>
 * over-approximations) and values that were widened or otherwise imprecisely
 * computed by the analysis.
 *
 * <pre>
 *         TOP (MIXED)
 *        /           \
 *    INPUT         ANALYSIS
 *        \           /
 *           BOTTOM
 * </pre>
 * <p>
 * Key invariant: if {@code provenance == INPUT} then the under-approximation of
 * the enclosing {@link it.unive.jlisa.lattices.JavaFlaggedInterval} equals its
 * over-approximation (the element is exact).
 *
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public enum ProvenanceFlag {

	/**
	 * Bottom element — used only in unreachable / bottom abstract states.
	 */
	BOTTOM,

	/**
	 * Every value in the over-approximation is concretely reachable because it
	 * derives directly from a non-deterministic input call (e.g.,
	 * {@code Verifier.nondetInt()}).
	 */
	INPUT,

	/**
	 * The interval is a strict over-approximation whose imprecision stems from
	 * the analysis itself (widening, join, unknown library stub, etc.).
	 */
	ANALYSIS,

	/**
	 * Top element — both {@link #INPUT} and {@link #ANALYSIS} sources
	 * contributed to this value.
	 */
	MIXED;

	/**
	 * Returns {@code true} if this is the top element ({@link #MIXED}).
	 *
	 * @return {@code true} iff this == MIXED
	 */
	public boolean isTop() {
		return this == MIXED;
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
	public ProvenanceFlag lub(
			ProvenanceFlag other) {
		if (this == BOTTOM)
			return other;
		if (other == BOTTOM)
			return this;
		if (this == other)
			return this;
		return MIXED;
	}

	/**
	 * Returns the greatest lower bound of this flag and {@code other}.
	 *
	 * @param other the other flag
	 *
	 * @return the greatest lower bound
	 */
	public ProvenanceFlag glb(
			ProvenanceFlag other) {
		if (this == MIXED)
			return other;
		if (other == MIXED)
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
	public ProvenanceFlag widening(
			ProvenanceFlag other) {
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
			ProvenanceFlag other) {
		return other.isTop() || this.isBottom() || this == other;
	}
}

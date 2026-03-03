package it.unive.jlisa.lattices;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.util.representation.MapRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.HashMap;
import java.util.Map;

/**
 * A composite lattice element that pairs a {@link ConstantValue} with a
 * {@link JavaFlaggedInterval}.
 * <p>
 * The {@link ConstantValue} component handles non-numeric / symbolic constants
 * (strings, booleans, …) as well as precise constant propagation. The
 * {@link JavaFlaggedInterval} component handles numeric ranges with
 * type-bounded overflow semantics, provenance tracking, under-approximation,
 * and flag annotations (NaN, infinity, integrality, parity).
 * <p>
 * <b>TOP semantics:</b> if {@link ConstantValue} is TOP <em>and</em>
 * {@link JavaFlaggedInterval} is either TOP or BOTTOM, the combined element is
 * considered TOP (a non-numeric TOP constant may carry an interval BOTTOM
 * without invalidating the top status).
 *
 * @see ConstantValueIntInterval for the simpler variant using plain
 *          {@link it.unive.lisa.util.numeric.IntInterval}
 *
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public class ConstantValueFlaggedInterval
		implements
		BaseLattice<ConstantValueFlaggedInterval> {

	/** The top element. */
	public static final ConstantValueFlaggedInterval TOP = new ConstantValueFlaggedInterval(ConstantValue.TOP,
			JavaFlaggedInterval.TOP);

	/** The bottom element. */
	public static final ConstantValueFlaggedInterval BOTTOM = new ConstantValueFlaggedInterval(ConstantValue.BOTTOM,
			JavaFlaggedInterval.BOTTOM);

	private final ConstantValue constantValue;
	private final JavaFlaggedInterval flaggedInterval;

	/**
	 * Constructs a combined lattice element.
	 *
	 * @param constantValue   the constant-value component
	 * @param flaggedInterval the flagged-interval component
	 */
	public ConstantValueFlaggedInterval(
			ConstantValue constantValue,
			JavaFlaggedInterval flaggedInterval) {
		this.constantValue = constantValue;
		this.flaggedInterval = flaggedInterval;
	}

	/**
	 * Returns the {@link ConstantValue} component.
	 *
	 * @return the constant-value component
	 */
	public ConstantValue getConstantValue() {
		return constantValue;
	}

	/**
	 * Returns the {@link JavaFlaggedInterval} component.
	 *
	 * @return the flagged-interval component
	 */
	public JavaFlaggedInterval getFlaggedInterval() {
		return flaggedInterval;
	}

	@Override
	public boolean isTop() {
		// ConstantValue takes precedence: a string TOP may have interval BOTTOM
		return constantValue.isTop() && (flaggedInterval.isTop() || flaggedInterval.isBottom());
	}

	@Override
	public boolean isBottom() {
		return constantValue.isBottom() && flaggedInterval.isBottom();
	}

	@Override
	public ConstantValueFlaggedInterval top() {
		return TOP;
	}

	@Override
	public ConstantValueFlaggedInterval bottom() {
		return BOTTOM;
	}

	@Override
	public StructuredRepresentation representation() {
		if (isTop())
			return Lattice.topRepresentation();
		if (isBottom())
			return Lattice.bottomRepresentation();
		Map<StructuredRepresentation, StructuredRepresentation> mapping = new HashMap<>();
		mapping.put(new StringRepresentation("constantValue"), constantValue.representation());
		mapping.put(new StringRepresentation("interval"), flaggedInterval.representation());
		return new MapRepresentation(mapping);
	}

	@Override
	public ConstantValueFlaggedInterval lubAux(
			ConstantValueFlaggedInterval other)
			throws SemanticException {
		return new ConstantValueFlaggedInterval(
				constantValue.lub(other.getConstantValue()),
				flaggedInterval.lub(other.getFlaggedInterval()));
	}

	@Override
	public boolean lessOrEqualAux(
			ConstantValueFlaggedInterval other)
			throws SemanticException {
		if (constantValue.isTop() || other.getConstantValue().isTop())
			return flaggedInterval.lessOrEqual(other.getFlaggedInterval());
		return constantValue.lessOrEqual(other.getConstantValue());
	}

	@Override
	public ConstantValueFlaggedInterval glbAux(
			ConstantValueFlaggedInterval other)
			throws SemanticException {
		return new ConstantValueFlaggedInterval(
				constantValue.glb(other.getConstantValue()),
				flaggedInterval.glb(other.getFlaggedInterval()));
	}

	@Override
	public ConstantValueFlaggedInterval narrowingAux(
			ConstantValueFlaggedInterval other)
			throws SemanticException {
		return new ConstantValueFlaggedInterval(
				constantValue.narrowing(other.getConstantValue()),
				flaggedInterval.narrowing(other.getFlaggedInterval()));
	}

	@Override
	public ConstantValueFlaggedInterval wideningAux(
			ConstantValueFlaggedInterval other)
			throws SemanticException {
		return new ConstantValueFlaggedInterval(
				constantValue.widening(other.getConstantValue()),
				flaggedInterval.widening(other.getFlaggedInterval()));
	}
}

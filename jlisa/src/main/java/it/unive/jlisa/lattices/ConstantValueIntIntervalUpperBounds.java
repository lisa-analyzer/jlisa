package it.unive.jlisa.lattices;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.lattices.symbolic.DefiniteIdSet;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.representation.MapRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class ConstantValueIntIntervalUpperBounds
		implements
		BaseLattice<ConstantValueIntIntervalUpperBounds> {

	public static ConstantValueIntIntervalUpperBounds TOP = new ConstantValueIntIntervalUpperBounds(ConstantValue.TOP,
			IntInterval.TOP, new DefiniteIdSet(new HashSet<>(), true));
	public static ConstantValueIntIntervalUpperBounds BOTTOM = new ConstantValueIntIntervalUpperBounds(
			ConstantValue.BOTTOM,
			IntInterval.BOTTOM,
			new DefiniteIdSet(new HashSet<>(), false));
	private ConstantValue constantValue;
	private IntInterval intInterval;
	private DefiniteIdSet definiteIdSet;

	public ConstantValueIntIntervalUpperBounds(
			ConstantValue constantValue,
			IntInterval intInterval,
			DefiniteIdSet definiteIdSet) {
		this.constantValue = constantValue;
		this.intInterval = intInterval;
		this.definiteIdSet = definiteIdSet;
	}

	public ConstantValue getConstantValue() {
		return constantValue;
	}

	public IntInterval getIntInterval() {
		return intInterval;
	}

	public DefiniteIdSet getDefiniteIdSet() {
		return definiteIdSet;
	}

	@Override
	public boolean isTop() {
		// Determining TOP is a bit subtle: ConstantValue takes precedence over
		// IntInterval.
		// If ConstantValue is TOP and represents a String, then IntInterval
		// will be BOTTOM,
		// but the combined ConstantValueIntInterval should still be considered
		// TOP.
		// Conversely, if ConstantValue is a number and is TOP, it represents
		// any possible number,
		// so IntInterval should also be TOP in that case.
		return constantValue.isTop() && (intInterval.isTop() || intInterval.isBottom());
	}

	@Override
	public boolean isBottom() {
		return constantValue.isBottom() && intInterval.isBottom();
	}

	@Override
	public ConstantValueIntIntervalUpperBounds top() {
		return TOP;
	}

	@Override
	public ConstantValueIntIntervalUpperBounds bottom() {
		return BOTTOM;
	}

	@Override
	public StructuredRepresentation representation() {
		if (isTop()) {
			return Lattice.topRepresentation();
		}
		if (isBottom()) {
			return Lattice.bottomRepresentation();
		}
		Map<StructuredRepresentation, StructuredRepresentation> mapping = new HashMap<>();
		mapping.put(new StringRepresentation("constantValue"), constantValue.representation());
		mapping.put(new StringRepresentation("intInterval"), intInterval.representation());
		mapping.put(new StringRepresentation("upperBounds"), definiteIdSet.representation());
		return new MapRepresentation(mapping);
	}

	@Override
	public ConstantValueIntIntervalUpperBounds lubAux(
			ConstantValueIntIntervalUpperBounds other)
			throws SemanticException {
		return new ConstantValueIntIntervalUpperBounds(
				constantValue.lub(other.getConstantValue()),
				intInterval.lub(other.getIntInterval()),
				definiteIdSet.lub(other.getDefiniteIdSet()));
	}

	@Override
	public boolean lessOrEqualAux(
			ConstantValueIntIntervalUpperBounds other)
			throws SemanticException {
		if (constantValue.isTop() || other.getConstantValue().isTop()) {
			return intInterval.lessOrEqual(other.getIntInterval());
		}
		return constantValue.lessOrEqual(other.getConstantValue());
	}

	@Override
	public ConstantValueIntIntervalUpperBounds glbAux(
			ConstantValueIntIntervalUpperBounds other)
			throws SemanticException {
		return new ConstantValueIntIntervalUpperBounds(constantValue.glb(
				other.getConstantValue()),
				intInterval.glb(other.getIntInterval()), definiteIdSet.glb(other.getDefiniteIdSet()));
	}

	@Override
	public ConstantValueIntIntervalUpperBounds narrowingAux(
			ConstantValueIntIntervalUpperBounds other)
			throws SemanticException {
		return new ConstantValueIntIntervalUpperBounds(constantValue.narrowing(
				other.getConstantValue()),
				intInterval.narrowing(other.getIntInterval()), definiteIdSet.narrowing(other.getDefiniteIdSet()));
	}

	@Override
	public ConstantValueIntIntervalUpperBounds wideningAux(
			ConstantValueIntIntervalUpperBounds other)
			throws SemanticException {
		return new ConstantValueIntIntervalUpperBounds(constantValue.widening(
				other.getConstantValue()),
				intInterval.widening(other.getIntInterval()), definiteIdSet.widening(other.getDefiniteIdSet()));
	}

}

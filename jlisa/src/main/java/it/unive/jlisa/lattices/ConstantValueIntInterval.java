package it.unive.jlisa.lattices;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.representation.MapRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.HashMap;
import java.util.Map;

public class ConstantValueIntInterval
		implements
		BaseLattice<ConstantValueIntInterval> {

	public static ConstantValueIntInterval TOP = new ConstantValueIntInterval(ConstantValue.TOP, IntInterval.TOP);
	public static ConstantValueIntInterval BOTTOM = new ConstantValueIntInterval(ConstantValue.BOTTOM,
			IntInterval.BOTTOM);
	private ConstantValue constantValue;
	private IntInterval intInterval;

	public ConstantValueIntInterval(
			ConstantValue constantValue,
			IntInterval intInterval) {
		this.constantValue = constantValue;
		this.intInterval = intInterval;
	}

	public ConstantValue getConstantValue() {
		return constantValue;
	}

	public IntInterval getIntInterval() {
		return intInterval;
	}

	@Override
	public boolean isTop() {
		return constantValue.isTop() && intInterval.isTop();
	}

	@Override
	public boolean isBottom() {
		return constantValue.isBottom() && intInterval.isBottom();
	}

	@Override
	public ConstantValueIntInterval top() {
		return TOP;
	}

	@Override
	public ConstantValueIntInterval bottom() {
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
		return new MapRepresentation(mapping);
	}

	@Override
	public ConstantValueIntInterval lubAux(
			ConstantValueIntInterval other)
			throws SemanticException {
		return new ConstantValueIntInterval(
				constantValue.lub(other.getConstantValue()),
				intInterval.lub(other.getIntInterval()));
	}

	@Override
	public boolean lessOrEqualAux(
			ConstantValueIntInterval other)
			throws SemanticException {
		if (constantValue.isTop() || other.getConstantValue().isTop()) {
			return intInterval.lessOrEqual(other.getIntInterval());
		}
		return constantValue.lessOrEqual(other.getConstantValue());
	}

	@Override
	public ConstantValueIntInterval glbAux(
			ConstantValueIntInterval other)
			throws SemanticException {
		return new ConstantValueIntInterval(constantValue.glb(
				other.getConstantValue()),
				intInterval.glb(other.getIntInterval()));
	}

	@Override
	public ConstantValueIntInterval narrowingAux(
			ConstantValueIntInterval other)
			throws SemanticException {
		return new ConstantValueIntInterval(constantValue.narrowing(
				other.getConstantValue()),
				intInterval.narrowing(other.getIntInterval()));
	}

	@Override
	public ConstantValueIntInterval wideningAux(
			ConstantValueIntInterval other)
			throws SemanticException {
		return new ConstantValueIntInterval(constantValue.widening(
				other.getConstantValue()),
				intInterval.widening(other.getIntInterval()));
	}

}

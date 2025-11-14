package it.unive.jlisa.lattices;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.combination.ValueCartesianCombination;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.symbolic.value.Identifier;

public class ConstantValueWithPentagon
		extends
		ValueCartesianCombination<ConstantValueWithPentagon, JavaPentagonLattice, ValueEnvironment<ConstantValue>>
		implements
		BaseLattice<ConstantValueWithPentagon> {

	public ConstantValueWithPentagon() {
		super(
				new JavaPentagonLattice(),
				new ValueEnvironment<>(new ConstantValue()));
	}

	public ConstantValueWithPentagon(
			JavaPentagonLattice first,
			ValueEnvironment<ConstantValue> second) {
		super(first, second);
	}

	@Override
	public ConstantValueWithPentagon mk(
			JavaPentagonLattice first,
			ValueEnvironment<ConstantValue> second) {
		return new ConstantValueWithPentagon(first, second);
	}

	@Override
	public ConstantValueWithPentagon store(
			Identifier target,
			Identifier source)
			throws SemanticException {
		return new ConstantValueWithPentagon(first.store(target, source), second.store(target, source));
	}

	@Override
	public boolean lessOrEqualAux(
			ConstantValueWithPentagon other)
			throws SemanticException {
		return first.lessOrEqual(other.first) && second.lessOrEqual(other.second);
	}

	@Override
	public ConstantValueWithPentagon lubAux(
			ConstantValueWithPentagon other)
			throws SemanticException {
		return new ConstantValueWithPentagon(
				first.lub(other.first),
				second.lub(other.second));
	}

	@Override
	public ConstantValueWithPentagon glbAux(
			ConstantValueWithPentagon other)
			throws SemanticException {
		return new ConstantValueWithPentagon(first.glb(
				other.first),
				second.glb(other.second));
	}

	@Override
	public ConstantValueWithPentagon narrowingAux(
			ConstantValueWithPentagon other)
			throws SemanticException {
		return new ConstantValueWithPentagon(first.narrowing(
				other.first),
				second.narrowing(other.second));
	}

	@Override
	public ConstantValueWithPentagon wideningAux(
			ConstantValueWithPentagon other)
			throws SemanticException {
		return new ConstantValueWithPentagon(first.widening(
				other.first),
				second.widening(other.second));
	}
}
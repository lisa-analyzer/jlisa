package it.unive.jlisa.analysis;

import it.unive.jlisa.lattices.ConstantValue;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.combination.ValueCartesianCombination;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.representation.MapRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import org.apache.commons.collections4.CollectionUtils;

public class ConstantPropWithIntervalLattice
		extends
		ValueCartesianCombination<ConstantPropWithIntervalLattice, ValueEnvironment<ConstantValue>,
				ValueEnvironment<IntInterval>> {
	public static ConstantPropWithIntervalLattice TOP = new ConstantPropWithIntervalLattice(ConstantValue.TOP,
			IntInterval.TOP);
	public static ConstantPropWithIntervalLattice BOTTOM = new ConstantPropWithIntervalLattice();

	private ConstantPropWithIntervalLattice() {
		super(
				new ValueEnvironment<>(ConstantValue.TOP),
				new ValueEnvironment<>(IntInterval.TOP));
	}

	public ConstantPropWithIntervalLattice(
			ValueEnvironment<ConstantValue> constantPropagation,
			ValueEnvironment<IntInterval> interval) {

		super(constantPropagation, interval);
	}

	public ConstantPropWithIntervalLattice(
			ConstantValue constantValue,
			IntInterval interval) {
		super(
				new ValueEnvironment<>(constantValue),
				new ValueEnvironment<>(interval));
	}

	@Override
	public ConstantPropWithIntervalLattice pushScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {

		return new ConstantPropWithIntervalLattice(first.pushScope(token, pp), second.pushScope(token, pp));
	}

	@Override
	public ConstantPropWithIntervalLattice popScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		return new ConstantPropWithIntervalLattice(first.popScope(token, pp), second.popScope(token, pp));
	}

	@Override
	public boolean knowsIdentifier(
			Identifier id) {
		return first.knowsIdentifier(id) || second.knowsIdentifier(id);
	}

	@Override
	public ConstantPropWithIntervalLattice forgetIdentifier(
			Identifier id,
			ProgramPoint pp)
			throws SemanticException {
		return new ConstantPropWithIntervalLattice(first.forgetIdentifier(id, pp), second.forgetIdentifier(id, pp));
	}

	@Override
	public ConstantPropWithIntervalLattice forgetIdentifiers(
			Iterable<Identifier> ids,
			ProgramPoint pp)
			throws SemanticException {
		return new ConstantPropWithIntervalLattice(first.forgetIdentifiers(ids, pp), second.forgetIdentifiers(ids, pp));
	}

	@Override
	public ConstantPropWithIntervalLattice forgetIdentifiersIf(
			Predicate<Identifier> test,
			ProgramPoint pp)
			throws SemanticException {
		return new ConstantPropWithIntervalLattice(first.forgetIdentifiersIf(test, pp),
				second.forgetIdentifiersIf(test, pp));
	}

	@Override
	public ConstantPropWithIntervalLattice mk(
			ValueEnvironment<ConstantValue> constantPropagation,
			ValueEnvironment<IntInterval> interval) {
		return new ConstantPropWithIntervalLattice(constantPropagation, interval);
	}

	@Override
	public ConstantPropWithIntervalLattice top() {
		return TOP;
	}

	@Override
	public boolean isTop() {
		return first.isTop() && second.isTop();
	}

	@Override
	public ConstantPropWithIntervalLattice bottom() {
		return BOTTOM;
	}

	@Override
	public boolean isBottom() {
		return first.isBottom() && second.isBottom();
	}

	@Override
	public boolean lessOrEqualAux(
			ConstantPropWithIntervalLattice other)
			throws SemanticException {
		if (second.lessOrEqual(other.second)) {
			return true;
		}
		if (first.lessOrEqual(other.first)) {
			return true;
		}
		return false;
	}

	@Override
	public <D extends Lattice<D>> Collection<D> getAllLatticeInstances(
			Class<D> lattice) {
		return super.getAllLatticeInstances(lattice);
	}

	@Override
	public ConstantPropWithIntervalLattice lubAux(
			ConstantPropWithIntervalLattice other)
			throws SemanticException {
		return mk(first.lub(other.first), second.lub(other.second));
	}

	@Override
	public ConstantPropWithIntervalLattice wideningAux(
			ConstantPropWithIntervalLattice other)
			throws SemanticException {
		return mk(first.widening(other.first), second.widening(other.second));
	}

	@Override
	public ConstantPropWithIntervalLattice glbAux(
			ConstantPropWithIntervalLattice other)
			throws SemanticException {
		return mk(first.glb(other.first), second.glb(other.second));
	}

	@Override
	public ConstantPropWithIntervalLattice narrowingAux(
			ConstantPropWithIntervalLattice other)
			throws SemanticException {
		return mk(first.narrowing(other.first), second.narrowing(other.second));
	}

	@Override
	public ConstantPropWithIntervalLattice store(
			Identifier target,
			Identifier source)
			throws SemanticException {
		return mk(first.store(target, source), second.store(target, source));
	}

	@Override
	public StructuredRepresentation representation() {
		if (isTop())
			return Lattice.topRepresentation();
		if (isBottom())
			return Lattice.bottomRepresentation();
		Map<StructuredRepresentation, StructuredRepresentation> mapping = new HashMap<>();
		for (Identifier id : CollectionUtils.union(first.getKeys(), second.getKeys()))
			mapping.put(
					new StringRepresentation(id),
					new StringRepresentation(
							first.getState(id).toString() + ", " + second.getState(id).representation()));
		return new MapRepresentation(mapping);
	}

}
package it.unive.jlisa.analysis.value;

import it.unive.jlisa.lattices.ReachLattice;
import it.unive.jlisa.lattices.ReachLattice.ReachabilityStatus;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.combination.ValueLatticeProduct;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.analysis.value.ValueLattice;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.cfg.controlFlow.ControlFlowStructure;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import java.util.Map;

public class Reachability<D extends ValueDomain<L>,
		L extends ValueLattice<L>> implements ValueDomain<ValueLatticeProduct<ReachLattice, L>> {

	private final D values;

	public Reachability(
			D values) {
		this.values = values;
	}

	@Override
	public ValueLatticeProduct<ReachLattice, L> assign(
			ValueLatticeProduct<ReachLattice, L> state,
			Identifier id,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		L v = values.assign(state.second, id, expression, pp, oracle);

		ReachLattice r = state.first;
		if (r.isBottom() || r.isTop()) {
			// no guards present, we can just return
			if (v == state.second)
				return state;
			return new ValueLatticeProduct<>(r, v);
		}

		for (ControlFlowStructure cfs : pp.getCFG().getDescriptor().getControlFlowStructures())
			if (cfs.getFirstFollower() == pp) {
				Map<ProgramPoint, ReachabilityStatus> map = r.mkNewFunction(r.function, true);
				Statement condition = cfs.getCondition();
				if (map != null)
					map.remove(condition);
				ReachabilityStatus reach = r.getState(condition);
				if (reach == null)
					// in some situations, e.g., if a loop ends with an if,
					// pp is the first follower of a condition that has not been
					// analyzed yet; in this case, we keep the current
					// reachability
					reach = r.lattice;
				r = new ReachLattice(reach, map == null || map.isEmpty() ? null : map);
				break;
			}

		if (r == state.first && v == state.second)
			return state;
		return new ValueLatticeProduct<>(r, v);
	}

	@Override
	public ValueLatticeProduct<ReachLattice, L> smallStepSemantics(
			ValueLatticeProduct<ReachLattice, L> state,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		L v = values.smallStepSemantics(state.second, expression, pp, oracle);

		ReachLattice r = state.first;
		if (r.isBottom() || r.isTop()) {
			// no guards present, we can just return
			if (v == state.second)
				return state;
			return new ValueLatticeProduct<>(r, v);
		}

		for (ControlFlowStructure cfs : pp.getCFG().getDescriptor().getControlFlowStructures())
			if (cfs.getCondition() == pp) {
				// for guards we keep the reachability of the first time
				// we encounter them
				ReachabilityStatus reach = r.getState(pp);
				if (reach != null)
					r = new ReachLattice(reach, r.function);
				break;
			} else if (cfs.getFirstFollower() == pp) {
				Map<ProgramPoint, ReachabilityStatus> map = r.mkNewFunction(r.function, true);
				Statement condition = cfs.getCondition();
				if (map != null)
					map.remove(condition);
				ReachabilityStatus reach = r.getState(condition);
				if (reach == null)
					// in some situations, e.g., if a loop ends with an if,
					// pp is the first follower of a condition that has not been
					// analyzed yet; in this case, we keep the current
					// reachability
					reach = r.lattice;
				r = new ReachLattice(reach, map == null || map.isEmpty() ? null : map);
				break;
			}

		if (r == state.first && v == state.second)
			return state;
		return new ValueLatticeProduct<>(r, v);
	}

	@Override
	public ValueLatticeProduct<ReachLattice, L> assume(
			ValueLatticeProduct<ReachLattice, L> state,
			ValueExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		L v = values.assume(state.second, expression, src, dest, oracle);

		ReachLattice r = state.first;
		Map<ProgramPoint, ReachabilityStatus> map = r.mkNewFunction(r.function, false);
		ReachabilityStatus prev = map.put(src, state.first.lattice);
		if (prev != null && prev != state.first.lattice)
			throw new SemanticException("Conflicting reachability information for " + src + " at " + src.getLocation()
					+ ": " + prev + " vs " + state.first.lattice);

		Satisfiability sat = values.satisfies(state.second, expression, src, oracle);
		if (sat == Satisfiability.BOTTOM || sat == Satisfiability.NOT_SATISFIED)
			r = new ReachLattice(ReachabilityStatus.UNREACHABLE, map);
		else if (sat == Satisfiability.SATISFIED)
			// we have to keep the same reachability of the condition
			r = new ReachLattice(state.first.lattice, map);
		else
			// we may take both branches
			r = new ReachLattice(ReachabilityStatus.POSSIBLY_REACHABLE, map);

		return new ValueLatticeProduct<>(r, v);
	}

	@Override
	public Satisfiability satisfies(
			ValueLatticeProduct<ReachLattice, L> state,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (state.first.isBottom())
			return Satisfiability.BOTTOM;
		return values.satisfies(state.second, expression, pp, oracle);
	}

	@Override
	public ValueLatticeProduct<ReachLattice, L> makeLattice() {
		return new ValueLatticeProduct<>(new ReachLattice(), values.makeLattice());
	}

}

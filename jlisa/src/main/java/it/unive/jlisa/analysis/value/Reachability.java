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
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
		return updateReachability(state, pp, v);
	}

	@Override
	public ValueLatticeProduct<ReachLattice, L> smallStepSemantics(
			ValueLatticeProduct<ReachLattice, L> state,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		L v = values.smallStepSemantics(state.second, expression, pp, oracle);
		return updateReachability(state, pp, v);
	}

	private ValueLatticeProduct<ReachLattice, L> updateReachability(
			ValueLatticeProduct<ReachLattice, L> state,
			ProgramPoint pp,
			L v)
			throws SemanticException {
		ReachLattice r = state.first;
		if (r.isBottom() || r.isTop() || r.function == null || r.function.isEmpty() || !(pp instanceof Statement)) {
			// no guards present, we can just return
			if (v == state.second)
				return state;
			return new ValueLatticeProduct<>(r, v);
		}

		// reachability is the same for statements and sub-expressions,
		// so we inspect the root statement
		Statement current = (Statement) pp;
		if (current instanceof Call) {
			Call original = (Call) current;
			while (original.getSource() != null)
				original = original.getSource();
			if (original != current)
				current = original;
		}
		if (current instanceof Expression)
			current = ((Expression) current).getRootStatement();

		Set<Statement> toRemove = new HashSet<>();
		ReachabilityStatus status = null;
		for (ControlFlowStructure cfs : current.getCFG().getDescriptor().getControlFlowStructures())
			if (cfs.getCondition() == current) {
				// for guards we keep the reachability of the first time
				// we encounter them: this help with loops that are reachable
				// but where the lub on the guard would make the condition
				// become possibly reachable, thus making the analysis
				// less precise
				ReachabilityStatus reach = r.getState(current);
				status = reach != null ? reach : r.lattice;
				break;
			} else if (cfs.getFirstFollower() == current) {
				Statement condition = cfs.getCondition();
				toRemove.add(condition);
				ReachabilityStatus reach = r.getState(condition);
				if (reach == null)
					// in some situations, e.g., if a loop ends with an if,
					// current is the first follower of a condition that has not
					// been analyzed yet; in this case, we keep the current
					// reachability
					reach = r.lattice;
				else if (cfs.allStatements().stream()
						.filter(st -> st != cfs.getFirstFollower() && st != cfs.getCondition())
						.anyMatch(Statement::stopsExecution))
					// if the execution of the cfs can terminate, the
					// reachability after the the cfs is not restored
					// as some executions might terminate the execution
					// TODO this is over-conservative: the stopping
					// statement might be unreachable
					reach = r.lattice;
				status = status == null ? reach : status.lub(reach);
			}

		// TODO the reachability after a call that does not throw exceptions/
		// halt the execution should be restored to the reachability before
		// the call, but there is no way of doing it right now

		if (!toRemove.isEmpty()) {
			Map<ProgramPoint, ReachabilityStatus> map = r.mkNewFunction(r.function, true);
			if (map != null)
				toRemove.forEach(map::remove);
			r = new ReachLattice(status, map == null || map.isEmpty() ? null : map);
		} else if (status != null)
			// we might have a new status from guards
			r = new ReachLattice(status, r.function);

		if (v.isBottom())
			// sanity check: if the state is bottom, the reachability is
			// overridden
			// we still take the modifications to the function into account
			// to remove guards that are no longer needed
			r = new ReachLattice(ReachabilityStatus.UNREACHABLE, r.function);

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
		if (!(src instanceof Statement)) {
			if (v == state.second)
				return state;
			return new ValueLatticeProduct<>(r, v);
		}

		Statement current = (Statement) src;
		if (current instanceof Call) {
			Call original = (Call) current;
			while (original.getSource() != null)
				original = original.getSource();
			if (original != current)
				current = original;
		}
		if (current instanceof Expression)
			current = ((Expression) current).getRootStatement();

		Map<ProgramPoint, ReachabilityStatus> map = r.mkNewFunction(r.function, false);
		ReachabilityStatus prev = map.put(current, state.first.lattice);
		if (prev != null && prev != state.first.lattice)
			throw new SemanticException(
					"Conflicting reachability information for " + current + " at " + current.getLocation()
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

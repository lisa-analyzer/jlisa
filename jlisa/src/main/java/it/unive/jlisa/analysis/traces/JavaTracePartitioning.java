package it.unive.jlisa.analysis.traces;

import it.unive.jlisa.program.cfg.controlflow.switches.Switch;
import it.unive.jlisa.program.cfg.controlflow.switches.SwitchCase;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.traces.TracePartitioning;
import it.unive.lisa.lattices.traces.Branching;
import it.unive.lisa.lattices.traces.ExecutionTrace;
import it.unive.lisa.lattices.traces.LoopIteration;
import it.unive.lisa.lattices.traces.LoopSummary;
import it.unive.lisa.lattices.traces.TraceLattice;
import it.unive.lisa.lattices.traces.TraceToken;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.cfg.controlFlow.ControlFlowStructure;
import it.unive.lisa.program.cfg.controlFlow.IfThenElse;
import it.unive.lisa.program.cfg.controlFlow.Loop;
import it.unive.lisa.symbolic.SymbolicExpression;
import java.util.Map;
import java.util.Map.Entry;

public class JavaTracePartitioning<A extends AbstractLattice<A>,
		D extends AbstractDomain<A>> extends TracePartitioning<A, D> {

	// FIXME: to change visibility on LiSA side
	private final int max_loop_iterations;
	private final int max_conditions;

	public JavaTracePartitioning(
			int maxLoopIterations,
			int maxConditions,
			D domain) {
		super(maxLoopIterations, maxConditions, domain);
		max_loop_iterations = maxLoopIterations;
		max_conditions = maxConditions;

	}

	public JavaTracePartitioning(
			D domain) {
		super(domain);
		this.max_loop_iterations = 5;
		this.max_conditions = 5;
	}

	public TraceLattice<A> assume(
			TraceLattice<A> state,
			SymbolicExpression expression,
			ProgramPoint src,
			ProgramPoint dest)
			throws SemanticException {
		if (state.isBottom())
			return state;

		ControlFlowStructure struct = src.getCFG().getControlFlowStructureOf(src);
		Map<ExecutionTrace, A> result = state.mkNewFunction(null, false);

		if (state.isTop() || state.function == null) {
			ExecutionTrace trace = ExecutionTrace.EMPTY;
			ExecutionTrace nextTrace = generateTraceFor(trace, struct, src, dest);
			result.put(nextTrace, state.lattice.top());
		} else
			for (Entry<ExecutionTrace, A> trace : state) {
				A st = trace.getValue();
				ExecutionTrace tokens = trace.getKey();
				A assume = domain.assume(st, expression, src, dest);
				if (assume.isBottom())
					// we only keep traces that can escape the loop
					continue;

				ExecutionTrace nextTrace = generateTraceFor(tokens, struct, src, dest);
				// when we hit one of the limits, more traces can get smashed
				// into one
				A prev = result.get(nextTrace);
				result.put(nextTrace, prev == null ? assume : assume.lub(prev));
			}

		if (result.isEmpty())
			// no traces pass the condition, so this branch is unreachable
			return state.bottom();

		return new TraceLattice<>(state.lattice, result);
	}

	// FIXME: to change visibility on LiSA side
	private ExecutionTrace generateTraceFor(
			ExecutionTrace trace,
			ControlFlowStructure struct,
			ProgramPoint src,
			ProgramPoint dest) {
		if (struct instanceof Loop && ((Loop) struct).getBody().contains(dest)) {
			// on loop exits we do not generate new traces
			TraceToken prev = trace.lastLoopTokenFor(src);
			if (prev == null)
				if (max_loop_iterations > 0)
					return trace.push(new LoopIteration(src, 0));
				else
					return trace.push(new LoopSummary(src));
			else if (prev instanceof LoopIteration) {
				LoopIteration li = (LoopIteration) prev;
				if (li.getIteration() < max_loop_iterations)
					return trace.push(new LoopIteration(src, li.getIteration() + 1));
				else
					return trace.push(new LoopSummary(src));
			}
			// we do nothing on loop summaries as we already reached
			// the maximum iterations for this loop
		} else if (struct instanceof IfThenElse && trace.numberOfBranches() < max_conditions) {
			return trace.push(new Branching(src, ((IfThenElse) struct).getTrueBranch().contains(dest)));
		} else if (struct instanceof Switch
				&& trace.numberOfBranches() + ((Switch) struct).getNumberOfCases() < max_conditions) {
			Switch switchStruct = (Switch) struct;
			ExecutionTrace res = trace;
			for (SwitchCase c : switchStruct.getSwitchCases())
				res = res.push(new Branching(src, c.getBody().contains(dest)));

			if (switchStruct.getDefaultSwitchCase() != null)
				res = res.push(new Branching(src, switchStruct.getDefaultSwitchCase().getBody().contains(dest)));

			return res;
		}

		// no known conditional structure, or no need to push new tokens
		return trace;
	}

}

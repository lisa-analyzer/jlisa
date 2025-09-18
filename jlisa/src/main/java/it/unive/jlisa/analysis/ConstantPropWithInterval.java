package it.unive.jlisa.analysis;

import it.unive.jlisa.analysis.value.ConstantPropagation;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.numeric.Interval;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;

public class ConstantPropWithInterval
		implements
		ValueDomain<ConstantPropWithIntervalLattice> {

	private final ConstantPropagation constantPropagation = new ConstantPropagation();

	private final Interval intervals = new Interval();

	@Override
	public ConstantPropWithIntervalLattice assign(
			ConstantPropWithIntervalLattice state,
			Identifier id,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return new ConstantPropWithIntervalLattice(
				constantPropagation.assign(state.first, id, expression, pp, oracle),
				intervals.assign(state.second, id, expression, pp, oracle));
	}

	@Override
	public ConstantPropWithIntervalLattice smallStepSemantics(
			ConstantPropWithIntervalLattice state,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return new ConstantPropWithIntervalLattice(
				constantPropagation.smallStepSemantics(state.first, expression, pp, oracle),
				intervals.smallStepSemantics(state.second, expression, pp, oracle));
	}

	@Override
	public ConstantPropWithIntervalLattice assume(
			ConstantPropWithIntervalLattice state,
			ValueExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		return new ConstantPropWithIntervalLattice(
				constantPropagation.smallStepSemantics(state.first, expression, dest, oracle),
				intervals.smallStepSemantics(state.second, expression, dest, oracle));
	}

	@Override
	public Satisfiability satisfies(
			ConstantPropWithIntervalLattice state,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		Satisfiability constantPropSatisfiability = constantPropagation.satisfies(state.first, expression, pp, oracle);
		Satisfiability intervalsSatisfiability = intervals.satisfies(state.second, expression, pp, oracle);

		if (constantPropSatisfiability.isTop() || constantPropSatisfiability.isBottom()) {
			return intervalsSatisfiability;
		}
		return constantPropSatisfiability;
	}

	@Override
	public ConstantPropWithIntervalLattice makeLattice() {
		return new ConstantPropWithIntervalLattice(constantPropagation.makeLattice(), intervals.makeLattice());
	}

	@Override
	public ConstantPropWithIntervalLattice applyReplacement(
			ConstantPropWithIntervalLattice state,
			HeapDomain.HeapReplacement r,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return new ConstantPropWithIntervalLattice(
				constantPropagation.applyReplacement(state.first, r, pp, oracle),
				intervals.applyReplacement(state.second, r, pp, oracle));
	}

	@Override
	public boolean equals(
			Object o) {
		if (o == null || getClass() != o.getClass())
			return false;

		ConstantPropWithInterval that = (ConstantPropWithInterval) o;
		return constantPropagation.equals(that.constantPropagation) && intervals.equals(that.intervals);
	}

	@Override
	public int hashCode() {
		int result = constantPropagation.hashCode();
		result = 31 * result + intervals.hashCode();
		return result;
	}
}

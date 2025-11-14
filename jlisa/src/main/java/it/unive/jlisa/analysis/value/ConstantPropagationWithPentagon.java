package it.unive.jlisa.analysis.value;

import it.unive.jlisa.lattices.ConstantValueWithPentagon;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;

public class ConstantPropagationWithPentagon
		implements
		ValueDomain<ConstantValueWithPentagon> {
	JavaPentagon pentagon = new JavaPentagon();
	ConstantPropagation constantPropagation = new ConstantPropagation();

	@Override
	public ConstantValueWithPentagon assign(
			ConstantValueWithPentagon state,
			Identifier id,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return new ConstantValueWithPentagon(
				pentagon.assign(state.first, id, expression, pp, oracle),
				constantPropagation.assign(state.second, id, expression, pp, oracle));
	}

	@Override
	public ConstantValueWithPentagon smallStepSemantics(
			ConstantValueWithPentagon state,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return new ConstantValueWithPentagon(
				pentagon.smallStepSemantics(state.first, expression, pp, oracle),
				constantPropagation.smallStepSemantics(state.second, expression, pp, oracle));
	}

	@Override
	public ConstantValueWithPentagon assume(
			ConstantValueWithPentagon state,
			ValueExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		Satisfiability sat = satisfies(state, expression, src, oracle);
		if (sat == Satisfiability.NOT_SATISFIED)
			return state.bottom();
		if (sat == Satisfiability.SATISFIED)
			return state;
		ValueExpression e = expression.removeNegations();
		if (e instanceof BinaryExpression be) {
			// assume ultimately assigns a variable, so we need this sanity
			// check
			// to avoid introducing mappings on ids that we cannot track
			ValueExpression left = (ValueExpression) be.getLeft();
			ValueExpression right = (ValueExpression) be.getRight();
			if (left instanceof Identifier id && (!id.canBeAssigned() || !canProcess(id, src, oracle)))
				return state;
			else if (right instanceof Identifier id && (!id.canBeAssigned() || !canProcess(id, src, oracle)))
				return state;
		}
		return new ConstantValueWithPentagon(
				pentagon.assume(state.first, expression, src, dest, oracle),
				constantPropagation.assume(state.second, expression, src, dest, oracle));
	}

	@Override
	public Satisfiability satisfies(
			ConstantValueWithPentagon state,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		Satisfiability sat = constantPropagation.satisfies(state.second, expression, pp, oracle);
		switch (sat) {
		case NOT_SATISFIED:
		case SATISFIED:
			return sat;
		case BOTTOM:
		case UNKNOWN:
		default:
			Satisfiability sat_pentagon = pentagon.satisfies(state.first, expression, pp, oracle);
			if (sat_pentagon == Satisfiability.SATISFIED || sat_pentagon == Satisfiability.NOT_SATISFIED)
				return sat_pentagon;
			return sat;
		}
	}

	@Override
	public ConstantValueWithPentagon makeLattice() {
		return new ConstantValueWithPentagon(pentagon.makeLattice(), constantPropagation.makeLattice());
	}

	public boolean canProcess(
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle) {
		return constantPropagation.canProcess(expression, pp, oracle);
	}
}

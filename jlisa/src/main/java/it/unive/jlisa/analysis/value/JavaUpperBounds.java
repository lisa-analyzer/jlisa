package it.unive.jlisa.analysis.value;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.numeric.UpperBounds;
import it.unive.lisa.lattices.symbolic.DefiniteIdSet;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.binary.*;

public class JavaUpperBounds extends UpperBounds {

	@Override
	public Satisfiability satisfies(
			ValueEnvironment<DefiniteIdSet> state,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (!(expression instanceof BinaryExpression))
			return Satisfiability.UNKNOWN;

		BinaryExpression bexp = (BinaryExpression) expression;
		SymbolicExpression left = bexp.getLeft();
		SymbolicExpression right = bexp.getRight();
		BinaryOperator operator = bexp.getOperator();

		if (!(left instanceof Identifier && right instanceof Identifier))
			return Satisfiability.UNKNOWN;

		Identifier x = (Identifier) left;
		Identifier y = (Identifier) right;

		if (operator instanceof ComparisonLt) {
			if (state.getState(x).contains(y))
				return Satisfiability.SATISFIED;
			else if (state.getState(y).contains(x))
				return Satisfiability.NOT_SATISFIED;
			else
				return Satisfiability.UNKNOWN;
		} else if (operator instanceof ComparisonLe) {
			if (state.getState(x).contains(y))
				return Satisfiability.SATISFIED;
			return Satisfiability.UNKNOWN;
		} else if (operator instanceof ComparisonGt) {
			if (state.getState(y).contains(x))
				return Satisfiability.SATISFIED;
			else if (state.getState(x).contains(y))
				return Satisfiability.NOT_SATISFIED;
			else
				return Satisfiability.UNKNOWN;
		} else if (operator instanceof ComparisonGe) {
			if (state.getState(y).contains(x))
				return Satisfiability.SATISFIED;
			return Satisfiability.UNKNOWN;
		}

		return Satisfiability.UNKNOWN;
	}
}

package it.unive.jlisa.analysis.value;

import it.unive.jlisa.lattices.JavaPentagonLattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.lattices.symbolic.DefiniteIdSet;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.RemainderOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import java.util.Collections;

public class JavaPentagon
		implements
		ValueDomain<JavaPentagonLattice> {

	private final JavaUpperBounds upperbounds = new JavaUpperBounds();

	private final JavaNumericInterval intervals = new JavaNumericInterval();

	@Override
	public JavaPentagonLattice makeLattice() {
		return new JavaPentagonLattice(intervals.makeLattice(), upperbounds.makeLattice());
	}

	@Override
	public JavaPentagonLattice assign(
			JavaPentagonLattice state,
			Identifier id,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		ValueEnvironment<IntInterval> newIntervals = intervals.assign(state.first, id, expression, pp, oracle);
		ValueEnvironment<DefiniteIdSet> newBounds = upperbounds.assign(state.second, id, expression, pp, oracle);

		if (expression instanceof BinaryExpression) {
			BinaryExpression be = (BinaryExpression) expression;
			BinaryOperator op = be.getOperator();

			if (op instanceof SubtractionOperator) {
				if (be.getLeft() instanceof Identifier) {
					Identifier x = (Identifier) be.getLeft();

					if (be.getRight() instanceof Identifier) {
						// r = x - y
						Identifier y = (Identifier) be.getRight();
						if (newBounds.getState(y).contains(x)) {
							IntInterval glb = newIntervals.getState(id)
									.glb(new IntInterval(MathNumber.ONE, MathNumber.PLUS_INFINITY));
							newIntervals = newIntervals.putState(id, glb);
						}
						IntInterval intv = state.first.getState(y);
						if (!intv.isBottom() && intv.getLow().compareTo(MathNumber.ZERO) > 0)
							newBounds = state.second.putState(id, state.second.getState(x).add(x));
						else
							newBounds = state.second
									.putState(id, new DefiniteIdSet(Collections.emptySet(), true).top());
					}
				}
			} else if (op instanceof RemainderOperator && be.getRight() instanceof Identifier) {
				// r = u % d
				Identifier d = (Identifier) be.getRight();
				MathNumber low = state.first.getState(d).getLow();
				if (low.isPositive() || low.isZero())
					newBounds = newBounds.putState(id, new DefiniteIdSet(Collections.singleton(d)));
				else
					newBounds = newBounds.putState(id, new DefiniteIdSet(Collections.emptySet(), true).top());
			}

		}

		return new JavaPentagonLattice(newIntervals, newBounds).closure();
	}

	@Override
	public JavaPentagonLattice smallStepSemantics(
			JavaPentagonLattice state,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return new JavaPentagonLattice(
				intervals.smallStepSemantics(state.first, expression, pp, oracle),
				upperbounds.smallStepSemantics(state.second, expression, pp, oracle));
	}

	@Override
	public JavaPentagonLattice assume(
			JavaPentagonLattice state,
			ValueExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		if (state.isBottom())
			return state;
		return new JavaPentagonLattice(
				intervals.assume(state.first, expression, src, dest, oracle),
				upperbounds.assume(state.second, expression, src, dest, oracle));
	}

	@Override
	public Satisfiability satisfies(
			JavaPentagonLattice state,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return intervals.satisfies(state.first, expression, pp, oracle)
				.glb(upperbounds.satisfies(state.second, expression, pp, oracle));
	}

}

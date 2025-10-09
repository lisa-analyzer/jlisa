package it.unive.jlisa.program.cfg.expression;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.logic.Or;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.operator.binary.LogicalOr;
import it.unive.lisa.type.Type;

public class JavaOr extends Or {

	public JavaOr(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression right) {
		super(cfg, location, left, right);
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> forwardSemantics(
			AnalysisState<A> state,
			InterproceduralAnalysis<A, D> interprocedural,
			StatementStore<A> expressions)
			throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		AnalysisState<A> result = state.bottomExecution();

		AnalysisState<A> leftState = getLeft().forwardSemantics(state, interprocedural, expressions);

		for (SymbolicExpression left : leftState.getExecutionExpressions()) {
			Satisfiability sat = analysis.satisfies(state, left, this);
			if (sat == Satisfiability.SATISFIED)
				result = result.lub(analysis.smallStepSemantics(state, left, this));
			else if (sat == Satisfiability.NOT_SATISFIED) {
				AnalysisState<A> rightState = getRight().forwardSemantics(leftState, interprocedural, expressions);
				for (SymbolicExpression right : rightState.getExecutionExpressions())
					result = result.lub(fwdBinarySemantics(interprocedural, state, left, right, expressions));
			} else {
				AnalysisState<A> rightState = getRight().forwardSemantics(leftState, interprocedural, expressions);
//				result = result.lub(leftState);
				if (rightState.getExecutionExpressions().isEmpty()) {
//					result = result.lub(rightState);
				} else {
					for (SymbolicExpression right : rightState.getExecutionExpressions())
						result = result.lub(fwdBinarySemantics(interprocedural, state, left, right, expressions));
				}
			}
		}

		return result;
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdBinarySemantics(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression left,
			SymbolicExpression right,
			StatementStore<A> expressions)
			throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();

		if (analysis.getRuntimeTypesOf(state, left, this).stream().noneMatch(Type::isBooleanType))
			return state.bottomExecution();
		if (analysis.getRuntimeTypesOf(state, right, this).stream().noneMatch(Type::isBooleanType))
			return state.bottomExecution();

		return analysis.smallStepSemantics(
				state,
				new BinaryExpression(getStaticType(), left, right, LogicalOr.INSTANCE, getLocation()),
				this);
	}

}

package it.unive.jlisa.program.cfg.expression;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.logic.Or;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.operator.binary.LogicalOr;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
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
		ScopeId<A> context = interprocedural.getCurrentContext();
		AnalysisState<A> result = state.bottomExecution();

		AnalysisState<A> leftState = getLeft().forwardSemantics(state, interprocedural, expressions);
		expressions.put(getLeft(), leftState);

		for (SymbolicExpression left : leftState.getExecutionExpressions()) {
			Satisfiability sat = analysis.satisfies(leftState, left, context, this);
			if (sat == Satisfiability.SATISFIED)
				result = result.lub(leftState);
			else {
				// the right operand is evaluated only where the left one
				// is false. Where the left one is true, the result is
				// computed without the effects of the right operand
				AnalysisState<A> evaluated = leftState;
				AnalysisState<A> shortCircuit = null;
				if (sat == Satisfiability.UNKNOWN) {
					ProgramPoint value = new ValuePoint(this);
					SymbolicExpression negation = new UnaryExpression(BoolType.INSTANCE, left,
							LogicalNegation.INSTANCE, getLocation());
					evaluated = analysis.assume(leftState, negation, context, value, value);
					shortCircuit = analysis.assume(leftState, left, context, value, value)
							.withExecutionExpression(left);
				}
				AnalysisState<A> rightState = getRight().forwardSemantics(evaluated, interprocedural, expressions);
				expressions.put(getRight(), rightState);
				AnalysisState<A> joined = shortCircuit == null ? rightState : rightState.lub(shortCircuit);
				for (SymbolicExpression right : rightState.getExecutionExpressions())
					result = result.lub(fwdBinarySemantics(interprocedural, joined, left, right, expressions));

				// the right operand has no normal execution state
				if (shortCircuit != null && rightState.getExecutionExpressions().isEmpty())
					result = result.lub(shortCircuit);
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
		ScopeId<A> context = interprocedural.getCurrentContext();

		if (analysis.getRuntimeTypesOf(state, left, context, this).stream().noneMatch(Type::isBooleanType))
			return state.bottomExecution();
		if (analysis.getRuntimeTypesOf(state, right, context, this).stream().noneMatch(Type::isBooleanType))
			return state.bottomExecution();

		return analysis.smallStepSemantics(
				state,
				new BinaryExpression(getStaticType(), left, right, LogicalOr.INSTANCE, getLocation()),
				context, this);
	}

}

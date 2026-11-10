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
import it.unive.lisa.program.cfg.statement.logic.And;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.symbolic.value.operator.binary.LogicalAnd;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.type.Type;

public class JavaAnd extends And {

	public JavaAnd(
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
			if (sat == Satisfiability.NOT_SATISFIED)
				// only this value of the left operand decides the result: the
				// state may hold other values too (e.g., a/b == c/d and a/b ==
				// NaN, when the division may be by zero)
				result = result.lub(leftState.withExecutionExpression(left));
			else {
				// the right operand is evaluated only where the left one
				// is true. Where the left one is false, the result is
				// computed without the effects of the right operand
				AnalysisState<A> evaluated = leftState;
				AnalysisState<A> shortCircuit = null;
				if (sat == Satisfiability.UNKNOWN) {
					ProgramPoint value = new ValuePoint(this);
					SymbolicExpression negation = new UnaryExpression(BoolType.INSTANCE, left,
							LogicalNegation.INSTANCE, getLocation());
					evaluated = analysis.assume(leftState, left, context, value, value);
					shortCircuit = analysis.assume(leftState, negation, context, value, value)
							.withExecutionExpression(left);
				}
				AnalysisState<A> rightState = getRight().forwardSemantics(evaluated, interprocedural, expressions);
				expressions.put(getRight(), rightState);

				AnalysisState<A> joined = shortCircuit == null ? rightState : rightState.lub(shortCircuit);
				for (SymbolicExpression right : rightState.getExecutionExpressions())
					if (sat == Satisfiability.SATISFIED && comparesReferences(left))
						// true && right is right: the value domains need not
						// decide again a comparison of references inside the
						// conjunction (e.g., the heap decides r != null, the
						// values r.length > 0). Other conditions are kept,
						// since right alone does not hold once the state is
						// joined with states where left is false (e.g., at
						// loop heads)
						result = result.lub(rightOperand(interprocedural, joined, right));
					else
						result = result.lub(fwdBinarySemantics(interprocedural, joined, left, right, expressions));

				// the right operand has no normal execution state
				if (shortCircuit != null && rightState.getExecutionExpressions().isEmpty())
					result = result.lub(shortCircuit);

				// no normal execution state
				if (rightState.getExecutionExpressions().isEmpty()) {
					result = result.lub(rightState);
				}
			}
		}

		if (leftState.getExecutionExpressions().isEmpty())
			result = result.lub(leftState);

		return result;
	}

	/**
	 * Returns whether the given condition compares references (e.g.,
	 * {@code r != null}), which the heap decides but the value domains do not.
	 *
	 * @param condition the condition
	 *
	 * @return {@code true} if the condition is {@code ==} or {@code !=} between
	 *             references or null
	 */
	static boolean comparesReferences(
			SymbolicExpression condition) {
		if (!(condition instanceof BinaryExpression b)
				|| !(b.getOperator() == ComparisonEq.INSTANCE || b.getOperator() == ComparisonNe.INSTANCE))
			return false;
		return isReference(b.getLeft()) || isReference(b.getRight());
	}

	private static boolean isReference(
			SymbolicExpression e) {
		Type type = e.getStaticType();
		return type.isReferenceType() || type.isNullType() || type.isPointerType();
	}

	// the result of the conjunction when the left operand is surely true: the
	// value of the right operand, if it is a boolean
	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> rightOperand(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression right)
			throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();
		if (analysis.getRuntimeTypesOf(state, right, context, this).stream().noneMatch(Type::isBooleanType))
			return state.bottomExecution();
		return analysis.smallStepSemantics(state, right, context, this);
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
				new BinaryExpression(getStaticType(), left, right, LogicalAnd.INSTANCE, getLocation()),
				context, this);
	}

}

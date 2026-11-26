package it.unive.jlisa.program.cfg.expression;

import it.unive.jlisa.program.cfg.JavaExceptions;
import it.unive.lisa.analysis.*;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.program.cfg.statement.evaluation.EvaluationOrder;
import it.unive.lisa.program.cfg.statement.evaluation.LeftToRightEvaluation;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.List;

public class JavaUnresolvedCall extends UnresolvedCall {

	public JavaUnresolvedCall(
			CFG cfg,
			CodeLocation location,
			CallType callType,
			String qualifier,
			String targetName,
			Expression... parameters) {
		super(cfg, location, callType, qualifier, targetName, Untyped.INSTANCE, parameters);
	}

	public JavaUnresolvedCall(
			CFG cfg,
			CodeLocation location,
			CallType callType,
			String qualifier,
			String targetName,
			Type staticType,
			Expression... parameters) {
		this(cfg, location, callType, qualifier, targetName, LeftToRightEvaluation.INSTANCE, staticType, parameters);
	}

	private JavaUnresolvedCall(
			CFG cfg,
			CodeLocation location,
			CallType callType,
			String qualifier,
			String targetName,
			EvaluationOrder order,
			Type staticType,
			Expression... parameters) {
		super(cfg, location, callType, qualifier, targetName, order, staticType, parameters);
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> forwardSemanticsAux(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			ExpressionSet[] params,
			StatementStore<A> expressions)
			throws SemanticException {

		// each value of the receiver is evaluated through the analysis, that
		// might evaluate it only in the executions computing it
		ExpressionSet receiver = params[0];
		AnalysisState<A> result = state.bottomExecution();
		for (SymbolicExpression rec : receiver)
			result = result.lub(receiver.size() == 1 ? callOn(interprocedural, state, rec, params, expressions)
					: ofCombination(interprocedural, state, s -> callOn(interprocedural, s, rec, params, expressions),
							rec));
		return result;
	}

	// the call on the given value of the receiver: a null receiver raises
	// NullPointerException, and the call happens in the other executions
	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> callOn(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression rec,
			ExpressionSet[] params,
			StatementStore<A> expressions)
			throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();
		JavaExceptions.Raising<A, D> raising = new JavaExceptions.Raising<>(interprocedural, this, List.of());
		AnalysisState<A> notNull = raising.raiseIfNull(rec, state);
		AnalysisState<A> result = state.bottomExecution();
		if (!notNull.getExecutionState().isBottom())
			for (Type recType : analysis.getRuntimeTypesOf(notNull, rec, context, this))
				if (recType.isPointerType() && recType.asPointerType().getInnerType().isUnitType()) {
					result = super.forwardSemanticsAux(interprocedural, notNull, params, expressions);
					break;
				}
		return raising.moveFrom(result);
	}
}

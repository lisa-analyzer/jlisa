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

		// get receiver
		ExpressionSet receiver = params[0];
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();
		AnalysisState<A> result = state.bottomExecution();
		// a null receiver raises NullPointerException, and the call happens
		// in the other executions
		JavaExceptions.Raising<A, D> raising = new JavaExceptions.Raising<>(interprocedural, this, List.of());
		for (SymbolicExpression rec : receiver) {
			// the executions where the receiver computed this value
			// (if they are not known exactly, the exceptions might be raised
			// only in some of them)
			AnalysisState<A> computing = restrictTo(interprocedural, state, rec);
			boolean exact = receiver.size() == 1 || restrictsExactly(interprocedural, state, rec);
			AnalysisState<A> notNull = raising.certainOnlyIf(exact).raiseIfNull(rec, computing);
			if (notNull.getExecutionState().isBottom())
				continue;
			for (Type recType : analysis.getRuntimeTypesOf(notNull, rec, context, this))
				if (recType.isPointerType() && recType.asPointerType().getInnerType().isUnitType()) {
					AnalysisState<A> called = super.forwardSemanticsAux(interprocedural, notNull, params, expressions);
					result = result.lub(exact ? called : analysis.uncertainErrors(notNull, called, context, this));
					break;
				}
		}
		return raising.moveFrom(result);
	}
}

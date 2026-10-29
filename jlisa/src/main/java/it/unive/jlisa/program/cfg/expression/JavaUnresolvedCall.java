package it.unive.jlisa.program.cfg.expression;

import it.unive.jlisa.program.type.JavaClassType;
import it.unive.lisa.analysis.*;
import it.unive.lisa.analysis.AnalysisState.Error;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.program.cfg.statement.evaluation.EvaluationOrder;
import it.unive.lisa.program.cfg.statement.evaluation.LeftToRightEvaluation;
import it.unive.lisa.symbolic.CFGThrow;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.Set;

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
		AnalysisState<A> erroneousState = state.bottom();
		Error error = null;

		for (SymbolicExpression rec : receiver) {
			Set<Type> types = analysis.getRuntimeTypesOf(state, rec, context, this);
			for (Type recType : types) {
				if (recType.isPointerType()) {
					Type inner = recType.asPointerType().getInnerType();
					if (inner.isNullType()) {
						// builds the exception
						JavaClassType npeType = JavaClassType.getNullPointerExceptionType();
						JavaNewObj call = new JavaNewObj(getCFG(), getLocation(),
								npeType.getReference(), new Expression[0]);
						AnalysisState<A> npeState = call.forwardSemanticsAux(interprocedural, state,
								new ExpressionSet[0], expressions);

						// assign exception to variable thrower
						for (SymbolicExpression th : npeState.getExecutionExpressions()) {
							CFGThrow throwVar = new CFGThrow(getCFG(), npeType.getReference(), getLocation());
							AnalysisState<A> thrown = analysis.assign(npeState, throwVar, th, context, this);

							// deletes the receiver of the constructor
							thrown = thrown.forgetIdentifiers(call.getMetaVariables(), this);
							erroneousState = erroneousState.lub(thrown.withExecutionExpression(throwVar));
							error = new Error(npeType.getReference(), this);
						}
						continue;
					} else if (!inner.isUnitType())
						continue;
					else
						result = result.lub(super.forwardSemanticsAux(interprocedural, state, params, expressions));
				}
			}
		}

		if (error == null)
			return result;
		return analysis.moveExecutionToError(result, erroneousState, error, context, this);
	}
}

package it.unive.jlisa.program.cfg.expression;

import it.unive.jlisa.program.type.JavaClassType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalysisState.Error;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
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

	public JavaUnresolvedCall(
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
		AnalysisState<A> result = state.bottomExecution();

		for (SymbolicExpression rec : receiver) {
			Set<Type> types = analysis.getRuntimeTypesOf(state, rec, this);
			for (Type recType : types) {
				if (recType.isPointerType()) {
					Type inner = recType.asPointerType().getInnerType();
					if (inner.isNullType()) {
						// builds the exception
						JavaClassType npeType = JavaClassType.getNullPoiterExceptionType();
						JavaNewObj call = new JavaNewObj(getCFG(), getLocation(), "NullPointerException",
								npeType.getReference(), new Expression[0]);
						state = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0], expressions);

						// assign exception to variable thrower
						CFGThrow throwVar = new CFGThrow(getCFG(), npeType.getReference(), getLocation());
						state = analysis.assign(state, throwVar,
								state.getExecutionExpressions().elements.stream().findFirst().get(), this);

						// deletes the receiver of the constructor
						state = state.forgetIdentifiers(call.getMetaVariables(), this);
						result = result.lub(analysis.moveExecutionToError(state.withExecutionExpression(throwVar),
								new Error(npeType.getReference(), this)));
						continue;
					} else if (!inner.isUnitType())
						continue;
					else
						result = result.lub(super.forwardSemanticsAux(interprocedural, state, params, expressions));
				}
			}
		}

		return result;
	}
}

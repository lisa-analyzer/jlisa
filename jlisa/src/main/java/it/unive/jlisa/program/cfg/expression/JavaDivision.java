package it.unive.jlisa.program.cfg.expression;

import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaDoubleType;
import it.unive.jlisa.program.type.JavaFloatType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalysisState.Error;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.numeric.Division;
import it.unive.lisa.symbolic.CFGThrow;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;

public class JavaDivision extends Division {

	public JavaDivision(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression right) {
		super(cfg, location, left, right);
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

		if (analysis.getDynamicTypeOf(state, left, context, this) == JavaDoubleType.INSTANCE
				|| analysis.getDynamicTypeOf(state, right, context, this) == JavaDoubleType.INSTANCE
				|| analysis.getDynamicTypeOf(state, left, context, this) == JavaFloatType.INSTANCE
				|| analysis.getDynamicTypeOf(state, right, context, this) == JavaFloatType.INSTANCE)
			// a floating point division by zero raises no exception: its value
			// is computed by the domains, as in IEEE 754 (1.0 / 0.0 is
			// POSITIVE_INFINITY, 1.0 / -0.0 is NEGATIVE_INFINITY, 0.0 / 0.0 is
			// NaN)
			return super.fwdBinarySemantics(interprocedural, state, left, right, expressions);

		BinaryExpression expr = new BinaryExpression(
				getCFG().getProgram().getTypes().getBooleanType(),
				right,
				new Constant(getCFG().getProgram().getTypes().getIntegerType(), 0, getLocation()),
				ComparisonEq.INSTANCE,
				getLocation());

		Satisfiability sat = analysis.satisfies(state, expr, context, this);

		if (sat == Satisfiability.SATISFIED) {
			JavaClassType arithExc = JavaClassType.getArithmeticExceptionType();
			JavaNewObj call = new JavaNewObj(getCFG(), getLocation(), arithExc.getReference(),
					new Expression[0]);
			state = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0], expressions);

			// assign exception to variable thrower
			CFGThrow throwVar = new CFGThrow(getCFG(), arithExc.getReference(), getLocation());
			state = analysis.assign(state, throwVar,
					state.getExecutionExpressions().elements.stream().findFirst().get(), context, this);

			// deletes the receiver of the constructor
			// and all the metavariables from subexpressions
			state = state.forgetIdentifiers(call.getMetaVariables(), this);
			state = state.forgetIdentifiers(getLeft().getMetaVariables(), this);
			state = state.forgetIdentifiers(getRight().getMetaVariables(), this);
			return analysis.moveExecutionToError(state.withExecutionExpression(throwVar),
					new Error(arithExc.getReference(), this), context, this);
		} else if (sat == Satisfiability.NOT_SATISFIED)
			return super.fwdBinarySemantics(interprocedural, state, left, right, expressions);
		else if (sat == Satisfiability.BOTTOM) {
			return state.bottomExecution();
		} else {
			// restrict each half to the executions it describes
			AnalysisState<A> okState = analysis.assume(state, new UnaryExpression(
					getCFG().getProgram().getTypes().getBooleanType(), expr, LogicalNegation.INSTANCE, getLocation()),
					context, this, this);
			AnalysisState<A> koState = analysis.assume(state, expr, context, this, this);

			AnalysisState<
					A> noExceptionState = super.fwdBinarySemantics(interprocedural, okState, left, right, expressions);

			JavaClassType arithExc = JavaClassType.getArithmeticExceptionType();
			JavaNewObj call = new JavaNewObj(getCFG(), getLocation(), arithExc.getReference(),
					new Expression[0]);
			koState = call.forwardSemanticsAux(interprocedural, koState, new ExpressionSet[0], expressions);

			// assign exception to variable thrower
			CFGThrow throwVar = new CFGThrow(getCFG(), arithExc.getReference(), getLocation());
			koState = analysis.assign(koState, throwVar,
					koState.getExecutionExpressions().elements.stream().findFirst().get(), context, this);

			// deletes the receiver of the constructor
			// and all the metavariables from subexpressions
			koState = koState.forgetIdentifiers(call.getMetaVariables(), this);
			koState = koState.forgetIdentifiers(getLeft().getMetaVariables(), this);
			koState = koState.forgetIdentifiers(getRight().getMetaVariables(), this);
			return analysis.moveExecutionToError(noExceptionState, koState.withExecutionExpression(throwVar),
					new Error(arithExc.getReference(), this), context, this);
		}
	}
}

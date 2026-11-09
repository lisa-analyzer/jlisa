package it.unive.jlisa.program.java.constructs.floatw;

import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.operator.JavaFloatParseFloatOperator;
import it.unive.jlisa.program.operator.JavaIsFloatParsableOperator;
import it.unive.jlisa.program.type.JavaBooleanType;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaLongType;
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
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.UnaryExpression;
import it.unive.lisa.symbolic.CFGThrow;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

public class FloatParseFloat extends UnaryExpression implements PluggableStatement {
	protected Statement originating;

	public FloatParseFloat(
			CFG cfg,
			CodeLocation location,
			Expression expr) {
		super(cfg, location, "parseFloat", expr);
	}

	public static FloatParseFloat build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new FloatParseFloat(cfg, location, params[0]);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

	@Override
	public void setOriginatingStatement(
			Statement st) {
		originating = st;
	}

	@Override
	public <A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> AnalysisState<A> fwdUnarySemantics(
					InterproceduralAnalysis<A, D> interprocedural,
					AnalysisState<A> state,
					SymbolicExpression expr,
					StatementStore<A> expressions)
					throws SemanticException {
		Type stringType = getProgram().getTypes().getStringType();

		GlobalVariable var = new GlobalVariable(Untyped.INSTANCE, "value", getLocation());
		HeapDereference derefExpr = new HeapDereference(stringType, expr, getLocation());
		AccessChild accessExpr = new AccessChild(stringType, derefExpr, var, getLocation());

		// need to check for NumberFormatExcetion
		it.unive.lisa.symbolic.value.UnaryExpression isParsable = new it.unive.lisa.symbolic.value.UnaryExpression(
				JavaLongType.INSTANCE,
				accessExpr,
				JavaIsFloatParsableOperator.INSTANCE,
				getLocation());

		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();

		Satisfiability sat = analysis.satisfies(state, isParsable, context, originating);
		if (sat == Satisfiability.SATISFIED) {
			it.unive.lisa.symbolic.value.UnaryExpression un = new it.unive.lisa.symbolic.value.UnaryExpression(
					JavaLongType.INSTANCE,
					accessExpr,
					JavaFloatParseFloatOperator.INSTANCE,
					getLocation());

			return analysis.smallStepSemantics(state, un, context, originating);
		} else if (sat == Satisfiability.NOT_SATISFIED) {
			// builds the exception
			JavaClassType nfeType = JavaClassType.getNumberFormatException();
			JavaNewObj call = new JavaNewObj(getCFG(), getLocation(),
					nfeType.getReference(), new Expression[0]);
			state = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0], expressions);

			// assign exception to variable thrower
			CFGThrow throwVar = new CFGThrow(getCFG(), nfeType.getReference(), getLocation());
			state = analysis.assign(state, throwVar,
					state.getExecutionExpressions().elements.stream().findFirst().get(), context, this);

			// deletes the receiver of the constructor
			// and all the metavariables from subexpressions
			state = state.forgetIdentifiers(call.getMetaVariables(), this);
			state = state.forgetIdentifiers(getSubExpression().getMetaVariables(), this);
			return analysis.moveExecutionToError(state.withExecutionExpression(throwVar),
					new Error(nfeType.getReference(), originating), context, this);
		} else {
			// restrict each half to the executions it describes
			AnalysisState<A> okState = analysis.assume(state, isParsable, context, originating, originating);
			AnalysisState<A> koState = analysis.assume(state,
					new it.unive.lisa.symbolic.value.UnaryExpression(JavaBooleanType.INSTANCE, isParsable,
							LogicalNegation.INSTANCE, getLocation()),
					context, originating, originating);

			it.unive.lisa.symbolic.value.UnaryExpression un = new it.unive.lisa.symbolic.value.UnaryExpression(
					JavaLongType.INSTANCE,
					accessExpr,
					JavaFloatParseFloatOperator.INSTANCE,
					getLocation());

			AnalysisState<A> noExceptionState = analysis.smallStepSemantics(okState, un, context, originating);

			// builds the exception
			JavaClassType nfeType = JavaClassType.getNumberFormatException();
			JavaNewObj call = new JavaNewObj(getCFG(), getLocation(),
					nfeType.getReference(), new Expression[0]);
			koState = call.forwardSemanticsAux(interprocedural, koState, new ExpressionSet[0], expressions);

			// assign exception to variable thrower
			CFGThrow throwVar = new CFGThrow(getCFG(), nfeType.getReference(), getLocation());
			koState = analysis.assign(koState, throwVar,
					koState.getExecutionExpressions().elements.stream().findFirst().get(), context, this);

			// deletes the receiver of the constructor
			// and all the metavariables from subexpressions
			koState = koState.forgetIdentifiers(call.getMetaVariables(), this);
			koState = koState.forgetIdentifiers(getSubExpression().getMetaVariables(), this);
			return analysis.moveExecutionToError(noExceptionState, koState.withExecutionExpression(throwVar),
					new Error(nfeType.getReference(), originating), context, this);
		}
	}
}

package it.unive.jlisa.program.java.constructs.doublew;

import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.operator.JavaDoubleParseDoubleOperator;
import it.unive.jlisa.program.operator.JavaIsDoubleParsableOperator;
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
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

public class DoubleParseDouble extends UnaryExpression implements PluggableStatement {
	protected Statement originating;

	public DoubleParseDouble(
			CFG cfg,
			CodeLocation location,
			Expression expr) {
		super(cfg, location, "parseDouble", expr);
	}

	public static DoubleParseDouble build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new DoubleParseDouble(cfg, location, params[0]);
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
				JavaIsDoubleParsableOperator.INSTANCE,
				getLocation());

		Analysis<A, D> analysis = interprocedural.getAnalysis();

		Satisfiability sat = analysis.satisfies(state, isParsable, originating);
		if (sat == Satisfiability.SATISFIED) {
			it.unive.lisa.symbolic.value.UnaryExpression un = new it.unive.lisa.symbolic.value.UnaryExpression(
					JavaLongType.INSTANCE,
					accessExpr,
					JavaDoubleParseDoubleOperator.INSTANCE,
					getLocation());

			return analysis.smallStepSemantics(state, un, originating);
		} else if (sat == Satisfiability.NOT_SATISFIED) {
			// builds the exception
			JavaClassType nfeType = JavaClassType.getNumberFormatException();
			JavaNewObj call = new JavaNewObj(getCFG(), getLocation(),
					nfeType.getReference(), new Expression[0]);
			state = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0], expressions);

			// assign exception to variable thrower
			CFGThrow throwVar = new CFGThrow(getCFG(), nfeType.getReference(), getLocation());
			state = analysis.assign(state, throwVar,
					state.getExecutionExpressions().elements.stream().findFirst().get(), this);

			// deletes the receiver of the constructor
			// and all the metavariables from subexpressions
			state = state.forgetIdentifiers(call.getMetaVariables(), this);
			state = state.forgetIdentifiers(getSubExpression().getMetaVariables(), this);
			return analysis.moveExecutionToError(state.withExecutionExpression(throwVar),
					new Error(nfeType.getReference(), originating), this);
		} else {
			it.unive.lisa.symbolic.value.UnaryExpression un = new it.unive.lisa.symbolic.value.UnaryExpression(
					JavaLongType.INSTANCE,
					accessExpr,
					JavaDoubleParseDoubleOperator.INSTANCE,
					getLocation());

			AnalysisState<A> noExceptionState = analysis.smallStepSemantics(state, un, originating);

			// builds the exception
			JavaClassType nfeType = JavaClassType.getNumberFormatException();
			JavaNewObj call = new JavaNewObj(getCFG(), getLocation(),
					nfeType.getReference(), new Expression[0]);
			state = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0], expressions);

			// assign exception to variable thrower
			CFGThrow throwVar = new CFGThrow(getCFG(), nfeType.getReference(), getLocation());
			state = analysis.assign(state, throwVar,
					state.getExecutionExpressions().elements.stream().findFirst().get(), this);

			// deletes the receiver of the constructor
			// and all the metavariables from subexpressions
			state = state.forgetIdentifiers(call.getMetaVariables(), this);
			state = state.forgetIdentifiers(getSubExpression().getMetaVariables(), this);
			AnalysisState<A> exceptionState = analysis.moveExecutionToError(state.withExecutionExpression(throwVar),
					new Error(nfeType.getReference(), originating), this);
			return exceptionState.lub(noExceptionState);
		}
	}
}

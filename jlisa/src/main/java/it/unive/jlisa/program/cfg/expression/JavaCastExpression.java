package it.unive.jlisa.program.cfg.expression;

import java.util.Collections;

import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.AnalysisState.Error;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.UnaryExpression;
import it.unive.lisa.symbolic.CFGThrow;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.binary.TypeCast;
import it.unive.lisa.symbolic.value.operator.binary.TypeCheck;
import it.unive.lisa.symbolic.value.operator.binary.TypeConv;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeTokenType;
import it.unive.lisa.type.Untyped;

public class JavaCastExpression extends UnaryExpression {

	private final Type type;

	public JavaCastExpression(CFG cfg, CodeLocation location, Expression subExpression, Type type) {
		super(cfg, location, "cast", subExpression);
		this.type = type.isInMemoryType() ? new JavaReferenceType(type) : type;
	}

	@Override
	public <A extends AbstractLattice<A>,
	D extends AbstractDomain<A>> AnalysisState<A> fwdUnarySemantics(InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state, SymbolicExpression expr, StatementStore<A> expressions) throws SemanticException {
		Constant typeConv = new Constant(new TypeTokenType(Collections.singleton(type)), type, getLocation());

		Analysis<A, D> analysis = interprocedural.getAnalysis();

		if (type.isReferenceType()) {
			// checking for ClassCastException
			TypeTokenType typeToken = new TypeTokenType(Collections.singleton(type));
			BinaryExpression tc = new BinaryExpression(Untyped.INSTANCE, expr, new Constant(typeToken, 0, getLocation()), TypeCheck.INSTANCE, getLocation());
			Satisfiability sat = analysis.satisfies(state, tc, this);
			if (sat == Satisfiability.NOT_SATISFIED) {
				// builds the exception
				JavaClassType ccExc = JavaClassType.getClassCastExceptionType();
				JavaNewObj call = new JavaNewObj(getCFG(), getLocation(), "ClassCastException", ccExc.getReference(), new Expression[0]);
				state = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0], expressions);
				
				// assign exception to variable thrower
				CFGThrow throwVar = new CFGThrow(getCFG(), ccExc.getReference(), getLocation());
				state = analysis.assign(state, throwVar, state.getExecutionExpressions().elements.stream().findFirst().get(), this);
				
				// deletes the receiver of the constructor
				state = state.forgetIdentifiers(call.getMetaVariables(), this);
				return analysis.moveExecutionToError(state.withExecutionExpression(throwVar), new Error(ccExc.getReference(), this));
			} else if (sat == Satisfiability.SATISFIED) {
				BinaryExpression castExpression =  new BinaryExpression(type, expr, typeConv, TypeCast.INSTANCE, getLocation());
				return analysis.smallStepSemantics(state, castExpression, this);
			} else if (sat == Satisfiability.UNKNOWN) {
				BinaryExpression castExpression =  new BinaryExpression(type, expr, typeConv, TypeCast.INSTANCE, getLocation());
				AnalysisState<A> noExceptionState = analysis.smallStepSemantics(state, castExpression, this);
				
				// builds the exception
				JavaClassType ccExc = JavaClassType.getClassCastExceptionType();
				JavaNewObj call = new JavaNewObj(getCFG(), getLocation(), "ClassCastException", ccExc.getReference(), new Expression[0]);
				state = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0], expressions);
				
				// assign exception to variable thrower
				CFGThrow throwVar = new CFGThrow(getCFG(), ccExc.getReference(), getLocation());
				state = analysis.assign(state, throwVar, state.getExecutionExpressions().elements.stream().findFirst().get(), this);
				
				// deletes the receiver of the constructor
				state = state.forgetIdentifiers(call.getMetaVariables(), this);
				AnalysisState<A> exceptionState = analysis.moveExecutionToError(state.withExecutionExpression(throwVar), new Error(ccExc.getReference(), this));	
				return exceptionState.lub(noExceptionState);
			} else {
				return state.bottom();
			}
		} else {
			BinaryExpression castExpression =  new BinaryExpression(type, expr, typeConv, TypeConv.INSTANCE, getLocation());
			return analysis.smallStepSemantics(state, castExpression, this);
		}	
	}

	@Override
	protected int compareSameClassAndParams(Statement o) {
		return 0;
	}

	@Override
	public String toString() {
		return "(" + type+ ") " + getSubExpression();
	}
}

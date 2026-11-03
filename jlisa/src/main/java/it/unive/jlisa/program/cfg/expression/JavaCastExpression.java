package it.unive.jlisa.program.cfg.expression;

import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaReferenceType;
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
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.UnaryExpression;
import it.unive.lisa.symbolic.CFGThrow;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.symbolic.value.operator.binary.TypeCast;
import it.unive.lisa.symbolic.value.operator.binary.TypeCheck;
import it.unive.lisa.symbolic.value.operator.binary.TypeConv;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeTokenType;
import it.unive.lisa.type.Untyped;
import java.util.Collections;
import java.util.Set;

public class JavaCastExpression extends UnaryExpression {

	private final Type type;

	public JavaCastExpression(
			CFG cfg,
			CodeLocation location,
			Expression subExpression,
			Type type) {
		super(cfg, location, "cast", subExpression);
		this.type = type.isInMemoryType() ? new JavaReferenceType(type) : type;
	}

	@Override
	public <A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> AnalysisState<A> fwdUnarySemantics(
					InterproceduralAnalysis<A, D> interprocedural,
					AnalysisState<A> state,
					SymbolicExpression expr,
					StatementStore<A> expressions)
					throws SemanticException {
		Constant typeConv = new Constant(new TypeTokenType(Collections.singleton(type)), type, getLocation());
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();

		if (type.isReferenceType()) {
			Set<Type> types = analysis.getRuntimeTypesOf(state, expr, context, this);
			AnalysisState<A> result = state.bottomExecution();
			AnalysisState<A> erroneousState = state.bottom();
			Error error = null;

			// the cast fails only if the value is not an instance of the type
			// (null is never an instance, but its cast succeeds: the check
			// can only be assumed when the cast fails), built as instanceof
			// does
			BinaryExpression instanceOf = new BinaryExpression(Untyped.INSTANCE, expr,
					new Constant(new TypeTokenType(Collections.singleton(type)), 0, getLocation()),
					TypeCheck.INSTANCE, getLocation());
			it.unive.lisa.symbolic.value.UnaryExpression notInstanceOf = new it.unive.lisa.symbolic.value.UnaryExpression(getProgram().getTypes().getBooleanType(),
					instanceOf, LogicalNegation.INSTANCE, getLocation());
			AnalysisState<A> failing = null;

			for (Type t : types) {
				boolean safe = t.isReferenceType() && t.asReferenceType().getInnerType().isNullType()
						|| t.canBeAssignedTo(type);

				if (safe) {
					BinaryExpression castExpression = new BinaryExpression(type, expr, typeConv, TypeCast.INSTANCE,
							getLocation());
					result = result.lub(analysis.smallStepSemantics(state, castExpression, context, this));
				} else {
					// builds the exception
					JavaClassType ccExc = JavaClassType.getClassCastExceptionType();
					JavaNewObj call = new JavaNewObj(getCFG(), getLocation(), ccExc.getReference(),
							new Expression[0]);
					if (failing == null) {
						failing = analysis.assume(state, notInstanceOf, context, this, this);
						if (failing.getExecutionState().isBottom())
							// the domain cannot represent the failure of the
							// check: keep the original state
							failing = state;
					}
					AnalysisState<A> excState = call.forwardSemanticsAux(interprocedural, failing,
							new ExpressionSet[0], expressions);

					// assign exception to variable thrower
					CFGThrow throwVar = new CFGThrow(getCFG(), ccExc.getReference(), getLocation());
					excState = analysis.assign(excState, throwVar,
							excState.getExecutionExpressions().elements.stream().findFirst().get(), context, this);

					// deletes the receiver of the constructor
					// and all the metavariables from subexpressions
					excState = excState.forgetIdentifiers(call.getMetaVariables(), this);
					excState = excState.forgetIdentifiers(getSubExpression().getMetaVariables(), this);
					erroneousState = erroneousState.lub(excState.withExecutionExpression(throwVar));
					error = new Error(ccExc.getReference(), this);
				}
			}

			if (error == null)
				return result;
			return analysis.moveExecutionToError(result, erroneousState, error, context, this);
		} else {
			BinaryExpression castExpression = new BinaryExpression(type, expr, typeConv, TypeConv.INSTANCE,
					getLocation());
			return analysis.smallStepSemantics(state, castExpression, context, this);
		}
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

	@Override
	public String toString() {
		return "(" + type + ") " + getSubExpression();
	}
}

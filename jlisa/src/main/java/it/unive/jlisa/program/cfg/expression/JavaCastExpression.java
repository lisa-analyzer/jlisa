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
import it.unive.lisa.symbolic.value.operator.binary.TypeCast;
import it.unive.lisa.symbolic.value.operator.binary.TypeConv;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeTokenType;
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

		if (type.isReferenceType()) {
			Set<Type> types = analysis.getRuntimeTypesOf(state, expr, this);
			AnalysisState<A> result = state.bottomExecution();

			for (Type t : types) {
				boolean safe = t.isReferenceType() && t.asReferenceType().getInnerType().isNullType()
						|| t.canBeAssignedTo(type);

				if (safe) {
					BinaryExpression castExpression = new BinaryExpression(type, expr, typeConv, TypeCast.INSTANCE,
							getLocation());
					result = result.lub(analysis.smallStepSemantics(state, castExpression, this));
				} else {
					// builds the exception
					JavaClassType ccExc = JavaClassType.getClassCastExceptionType();
					JavaNewObj call = new JavaNewObj(getCFG(), getLocation(), ccExc.getReference(),
							new Expression[0]);
					AnalysisState<A> excState = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0],
							expressions);

					// assign exception to variable thrower
					CFGThrow throwVar = new CFGThrow(getCFG(), ccExc.getReference(), getLocation());
					excState = analysis.assign(excState, throwVar,
							excState.getExecutionExpressions().elements.stream().findFirst().get(), this);

					// deletes the receiver of the constructor
					// and all the metavariables from subexpressions
					excState = excState.forgetIdentifiers(call.getMetaVariables(), this);
					excState = excState.forgetIdentifiers(getSubExpression().getMetaVariables(), this);
					result = result.lub(analysis.moveExecutionToError(excState.withExecutionExpression(throwVar),
							new Error(ccExc.getReference(), this), this));
				}
			}

			return result;
		} else {
			BinaryExpression castExpression = new BinaryExpression(type, expr, typeConv, TypeConv.INSTANCE,
					getLocation());
			return analysis.smallStepSemantics(state, castExpression, this);
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

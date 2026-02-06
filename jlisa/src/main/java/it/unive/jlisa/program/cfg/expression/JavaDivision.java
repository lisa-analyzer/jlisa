package it.unive.jlisa.program.cfg.expression;

import it.unive.jlisa.program.cfg.statement.global.JavaAccessGlobal;
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
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.numeric.Division;
import it.unive.lisa.symbolic.CFGThrow;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;

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

		BinaryExpression expr = new BinaryExpression(
				getCFG().getProgram().getTypes().getBooleanType(),
				right,
				new Constant(getCFG().getProgram().getTypes().getIntegerType(), 0, getLocation()),
				ComparisonEq.INSTANCE,
				getLocation());

		if (analysis.satisfies(state, expr, this) == Satisfiability.SATISFIED) {
			// no division by zero exception for floating point numbers
			if (analysis.getDynamicTypeOf(state, left, this) == JavaDoubleType.INSTANCE
					|| analysis.getDynamicTypeOf(state, right, this) == JavaDoubleType.INSTANCE
					|| analysis.getDynamicTypeOf(state, left, this) == JavaFloatType.INSTANCE
					|| analysis.getDynamicTypeOf(state, right, this) == JavaFloatType.INSTANCE) {

				JavaAccessGlobal accessGlobal;
				if (analysis.getDynamicTypeOf(state, left, this) == JavaDoubleType.INSTANCE
						|| analysis.getDynamicTypeOf(state, right, this) == JavaDoubleType.INSTANCE) {
					accessGlobal = new JavaAccessGlobal(
							getCFG(),
							getLocation(),
							getProgram().getUnit("java.lang.Double"),
							new Global(
									getLocation(),
									getProgram().getUnit("java.lang.Double"),
									"POSITIVE_INFINITY",
									false,
									JavaDoubleType.INSTANCE));
				} else
					accessGlobal = new JavaAccessGlobal(
							getCFG(),
							getLocation(),
							getProgram().getUnit("java.lang.Float"),
							new Global(
									getLocation(),
									getProgram().getUnit("java.lang.Float"),
									"POSITIVE_INFINITY",
									false,
									JavaDoubleType.INSTANCE));

				return accessGlobal.forwardSemantics(state, interprocedural, expressions);
			} else {
				JavaClassType arithExc = JavaClassType.getArithmeticExceptionType();
				JavaNewObj call = new JavaNewObj(getCFG(), getLocation(), arithExc.getReference(),
						new Expression[0]);
				state = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0], expressions);

				// assign exception to variable thrower
				CFGThrow throwVar = new CFGThrow(getCFG(), arithExc.getReference(), getLocation());
				state = analysis.assign(state, throwVar,
						state.getExecutionExpressions().elements.stream().findFirst().get(), this);

				// deletes the receiver of the constructor
				// and all the metavariables from subexpressions
				state = state.forgetIdentifiers(call.getMetaVariables(), this);
				state = state.forgetIdentifiers(getLeft().getMetaVariables(), this);
				state = state.forgetIdentifiers(getRight().getMetaVariables(), this);
				return analysis.moveExecutionToError(state.withExecutionExpression(throwVar),
						new Error(arithExc.getReference(), this), this);
			}
		} else if (analysis.satisfies(state, expr, this) == Satisfiability.NOT_SATISFIED)
			return super.fwdBinarySemantics(interprocedural, state, left, right, expressions);
		else {
			AnalysisState<
					A> noExceptionState = super.fwdBinarySemantics(interprocedural, state, left, right, expressions);

			JavaClassType arithExc = JavaClassType.getArithmeticExceptionType();
			JavaNewObj call = new JavaNewObj(getCFG(), getLocation(), arithExc.getReference(),
					new Expression[0]);
			state = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0], expressions);

			// assign exception to variable thrower
			CFGThrow throwVar = new CFGThrow(getCFG(), arithExc.getReference(), getLocation());
			state = analysis.assign(state, throwVar,
					state.getExecutionExpressions().elements.stream().findFirst().get(), this);

			// deletes the receiver of the constructor
			// and all the metavariables from subexpressions
			state = state.forgetIdentifiers(call.getMetaVariables(), this);
			state = state.forgetIdentifiers(getLeft().getMetaVariables(), this);
			state = state.forgetIdentifiers(getRight().getMetaVariables(), this);
			AnalysisState<A> exceptionState = analysis.moveExecutionToError(state.withExecutionExpression(throwVar),
					new Error(arithExc.getReference(), this), this);

			return exceptionState.lub(noExceptionState);
		}
	}
}
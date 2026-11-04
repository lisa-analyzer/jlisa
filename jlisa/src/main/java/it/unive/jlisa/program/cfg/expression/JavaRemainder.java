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
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.numeric.Remainder;
import it.unive.lisa.symbolic.CFGThrow;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.type.Type;

/**
 * The {@code %} of Java: a remainder, whose result has the sign of the dividend
 * ({@code -7 % 3} is {@code -1}). A divisor 0 raises an
 * {@code ArithmeticException} on integers. On floating point numbers, it gives
 * NaN.
 *
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public class JavaRemainder extends Remainder {

	/**
	 * Builds the remainder.
	 *
	 * @param cfg      the cfg that this expression belongs to
	 * @param location the location where this expression is defined
	 * @param left     the dividend
	 * @param right    the divisor
	 */
	public JavaRemainder(
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

		BinaryExpression expr = new BinaryExpression(
				getCFG().getProgram().getTypes().getBooleanType(),
				right,
				new Constant(getCFG().getProgram().getTypes().getIntegerType(), 0, getLocation()),
				ComparisonEq.INSTANCE,
				getLocation());

		Satisfiability sat = analysis.satisfies(state, expr, context, this);
		if (sat == Satisfiability.NOT_SATISFIED)
			return super.fwdBinarySemantics(interprocedural, state, left, right, expressions);
		if (sat == Satisfiability.BOTTOM)
			return state.bottomExecution();

		// restrict each half to the executions it describes
		AnalysisState<A> okState = sat == Satisfiability.SATISFIED ? state.bottomExecution()
				: super.fwdBinarySemantics(interprocedural, analysis.assume(state, new UnaryExpression(
						getCFG().getProgram().getTypes().getBooleanType(), expr, LogicalNegation.INSTANCE,
						getLocation()), context, this, this), left, right, expressions);
		AnalysisState<A> koState = sat == Satisfiability.SATISFIED ? state
				: analysis.assume(state, expr, context, this, this);

		Type leftType = analysis.getDynamicTypeOf(state, left, context, this);
		Type rightType = analysis.getDynamicTypeOf(state, right, context, this);
		if (leftType == JavaDoubleType.INSTANCE || rightType == JavaDoubleType.INSTANCE
				|| leftType == JavaFloatType.INSTANCE || rightType == JavaFloatType.INSTANCE) {
			// no exception for floating point numbers: x % 0.0 is NaN
			boolean isDouble = leftType == JavaDoubleType.INSTANCE || rightType == JavaDoubleType.INSTANCE;
			String unitName = isDouble ? "java.lang.Double" : "java.lang.Float";
			Type fieldType = isDouble ? JavaDoubleType.INSTANCE : JavaFloatType.INSTANCE;
			JavaAccessGlobal nan = new JavaAccessGlobal(getCFG(), getLocation(), getProgram().getUnit(unitName),
					new Global(getLocation(), getProgram().getUnit(unitName), "NaN", false, fieldType));
			return okState.lub(nan.forwardSemantics(koState, interprocedural, expressions));
		}

		JavaClassType arithExc = JavaClassType.getArithmeticExceptionType();
		JavaNewObj call = new JavaNewObj(getCFG(), getLocation(), arithExc.getReference(), new Expression[0]);
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
		return analysis.moveExecutionToError(okState, koState.withExecutionExpression(throwVar),
				new Error(arithExc.getReference(), this), context, this);
	}
}

package it.unive.jlisa.program.java.constructs.stringbuilder;

import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.type.JavaBooleanType;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaIntType;
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
import it.unive.lisa.program.cfg.statement.BinaryExpression;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.CFGThrow;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

/**
 * The constructor {@code StringBuilder(int capacity)}: the builder is empty and
 * has the given capacity, and a {@link NegativeArraySizeException} is raised if
 * the capacity is negative.
 */
public class StringBuilderCapacityConstructor extends BinaryExpression implements PluggableStatement {
	protected Statement originating;

	public StringBuilderCapacityConstructor(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression right) {
		super(cfg, location, "StringBuilder", left, right);
	}

	public static StringBuilderCapacityConstructor build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new StringBuilderCapacityConstructor(cfg, location, params[0], params[1]);
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
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdBinarySemantics(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression left,
			SymbolicExpression right,
			StatementStore<A> expressions)
			throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();

		// capacity < 0
		it.unive.lisa.symbolic.value.BinaryExpression negative = new it.unive.lisa.symbolic.value.BinaryExpression(
				JavaBooleanType.INSTANCE,
				right, new Constant(JavaIntType.INSTANCE, 0, getLocation()), ComparisonLt.INSTANCE, getLocation());
		Satisfiability sat = analysis.satisfies(state, negative, context, this);

		if (sat == Satisfiability.NOT_SATISFIED)
			return init(analysis, context, state, left, right);

		JavaClassType excType = JavaClassType.getNegativeArraySizeExceptionType();
		if (sat == Satisfiability.SATISFIED) {
			AnalysisState<A> thrown = raise(interprocedural, state, excType, expressions);
			AnalysisState<A> result = state.bottomExecution();
			for (SymbolicExpression th : thrown.getExecutionExpressions())
				result = result.lub(analysis.moveExecutionToError(thrown.withExecutionExpression(th),
						new Error(excType.getReference(), originating), context, this));
			return result;
		}

		// restrict each half to the executions it describes
		AnalysisState<A> okState = analysis.assume(state,
				new it.unive.lisa.symbolic.value.UnaryExpression(JavaBooleanType.INSTANCE, negative,
						LogicalNegation.INSTANCE, getLocation()),
				context, this, this);
		AnalysisState<A> koState = analysis.assume(state, negative, context, this, this);
		return analysis.moveExecutionToError(init(analysis, context, okState, left, right),
				raise(interprocedural, koState, excType, expressions),
				new Error(excType.getReference(), originating), context, this);
	}

	/**
	 * Initializes the builder to the empty string with the given capacity.
	 */
	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> init(
			Analysis<A, D> analysis,
			ScopeId<A> context,
			AnalysisState<A> state,
			SymbolicExpression left,
			SymbolicExpression right)
			throws SemanticException {
		Type stringType = getProgram().getTypes().getStringType();
		Constant emptyString = new Constant(stringType, "", getLocation());

		GlobalVariable var = new GlobalVariable(Untyped.INSTANCE, "value", getLocation());
		AccessChild access = new AccessChild(stringType, left, var, getLocation());

		GlobalVariable capVar = new GlobalVariable(Untyped.INSTANCE, "capacity", getLocation());
		AccessChild capAccess = new AccessChild(JavaIntType.INSTANCE, left, capVar, getLocation());

		AnalysisState<A> tmp = analysis.assign(state, access, emptyString, context, originating);
		return analysis.assign(tmp, capAccess, right, context, originating);
	}

	/**
	 * Yields the state where the given exception is built and thrown, whose
	 * execution expressions are the thrown exceptions.
	 */
	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> raise(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			JavaClassType excType,
			StatementStore<A> expressions)
			throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();
		JavaNewObj call = new JavaNewObj(getCFG(), getLocation(), excType.getReference(), new Expression[0]);
		state = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0], expressions);
		AnalysisState<A> result = state.bottomExecution();
		for (SymbolicExpression th : state.getExecutionExpressions()) {
			// assign exception to variable thrower
			CFGThrow throwVar = new CFGThrow(getCFG(), excType.getReference(), getLocation());
			AnalysisState<A> tmp = analysis.assign(state, throwVar, th, context, this);

			// deletes the receiver of the constructor
			// and all the metavariables from subexpressions
			tmp = tmp.forgetIdentifiers(call.getMetaVariables(), this)
					.forgetIdentifiers(getLeft().getMetaVariables(), this)
					.forgetIdentifiers(getRight().getMetaVariables(), this);
			result = result.lub(tmp.withExecutionExpression(throwVar));
		}
		return result;
	}
}

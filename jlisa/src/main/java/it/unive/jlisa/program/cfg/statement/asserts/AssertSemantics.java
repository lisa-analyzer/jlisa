package it.unive.jlisa.program.cfg.statement.asserts;

import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.type.JavaClassType;
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
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.CFGThrow;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;

/**
 * The semantics shared by assertions, as executed with assertions enabled:
 * execution continues after the assertion only if its condition holds, while an
 * {@link java.lang.AssertionError} is thrown if it does not.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
final class AssertSemantics {

	private AssertSemantics() {
	}

	/**
	 * Yields the state where the condition of an assertion holds, and the state
	 * where an {@link java.lang.AssertionError} is thrown since the condition
	 * does not hold.
	 *
	 * @param <A>             the kind of {@link AbstractLattice} produced by
	 *                            the domain {@code D}
	 * @param <D>             the kind of {@link AbstractDomain} to run during
	 *                            the analysis
	 * @param interprocedural the interprocedural analysis of the program
	 * @param state           the state where the condition has been evaluated
	 * @param holds           the state of the assertion's evaluation, to
	 *                            continue from if the condition holds
	 * @param condition       the condition of the assertion
	 * @param assertion       the assertion
	 * @param expressions     the cache where analysis states of intermediate
	 *                            expressions are stored
	 *
	 * @return the state after the assertion
	 *
	 * @throws SemanticException if something goes wrong during the computation
	 */
	static <A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> AnalysisState<A> check(
					InterproceduralAnalysis<A, D> interprocedural,
					AnalysisState<A> state,
					AnalysisState<A> holds,
					SymbolicExpression condition,
					Statement assertion,
					StatementStore<A> expressions)
					throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();
		AnalysisState<A> result = analysis.assume(holds, condition, context, assertion, assertion);

		UnaryExpression negated = new UnaryExpression(assertion.getProgram().getTypes().getBooleanType(),
				condition, LogicalNegation.INSTANCE, assertion.getLocation());
		AnalysisState<A> fails = analysis.assume(state, negated, context, assertion, assertion);
		if (fails.getExecutionState().isBottom())
			// the assertion always holds
			return result;

		// builds the error
		JavaClassType type = JavaClassType.getAssertionErrorType();
		JavaNewObj call = new JavaNewObj(assertion.getCFG(), assertion.getLocation(), type.getReference(),
				new Expression[0]);
		AnalysisState<A> errorState = call.forwardSemanticsAux(interprocedural, fails, new ExpressionSet[0],
				expressions);

		// assign the error to the variable of the thrower
		CFGThrow thrown = new CFGThrow(assertion.getCFG(), type.getReference(), assertion.getLocation());
		AnalysisState<A> thrownState = errorState.bottomExecution();
		for (SymbolicExpression ref : errorState.getExecutionExpressions())
			thrownState = thrownState.lub(analysis.assign(errorState, thrown, ref, context, assertion));
		thrownState = thrownState.forgetIdentifiers(call.getMetaVariables(), assertion);
		return analysis.moveExecutionToError(result, thrownState.withExecutionExpression(thrown),
				new Error(type.getReference(), assertion), context, assertion);
	}
}

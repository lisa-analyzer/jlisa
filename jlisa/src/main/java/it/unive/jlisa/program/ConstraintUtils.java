

package it.unive.jlisa.program;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import it.unive.lisa.analysis.*;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.analysis.value.ValueLattice;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.SimpleAbstractState;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;

public class ConstraintUtils {

	@SuppressWarnings({ "rawtypes" })
	static public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> Stream<BinaryExpression> extractConstraints(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression expr,
			ProgramPoint pp)
			throws SemanticException {

		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();
		if (!(analysis.domain instanceof SimpleAbstractDomain))
			return null;
		SimpleAbstractDomain<?, ?, ?> innerDomain = (SimpleAbstractDomain<?, ?, ?>) analysis.domain;

		assert (innerDomain != null);
		ValueDomain vdom = (ValueDomain) innerDomain.valueDomain;

		SimpleAbstractState simpleAbstractState = (SimpleAbstractState) state.getExecutionState();

		ValueLattice env = (ValueLattice) simpleAbstractState.valueState;

		SemanticOracle oracle = innerDomain.makeOracle(simpleAbstractState, context);

		ExpressionSet rewritten = analysis.rewrite(state, expr, context, pp);

		return StreamSupport.stream(rewritten.spliterator(), false)
				.map(ex -> (ValueExpression) ex)
				.flatMap(vex -> {
					try {
						return vdom.constraints(null, env, vex, pp, oracle).stream();
					} catch (SemanticException e) {
						return null;
					}
				});
	}
}

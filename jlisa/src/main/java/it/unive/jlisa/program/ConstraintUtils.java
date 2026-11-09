
package it.unive.jlisa.program;

import it.unive.lisa.analysis.*;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.analysis.value.ValueLattice;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ConstraintUtils {

	@SuppressWarnings({ "rawtypes" })
	static public <A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> Stream<BinaryExpression> extractConstraints(
					InterproceduralAnalysis<A, D> interprocedural,
					AnalysisState<A> state,
					SymbolicExpression expr,
					ProgramPoint pp)
					throws SemanticException {

		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();
		// the domain and the state are not necessarily the ones of a
		// SimpleAbstractDomain (e.g., they might be wrapped): their value
		// components are looked up instead
		ValueDomain vdom = analysis.domain.getDomainInstance(ValueDomain.class);
		ValueLattice env = state.getExecutionState().getLatticeInstance(ValueLattice.class);
		if (vdom == null || env == null)
			return Stream.empty();

		SemanticOracle oracle = analysis.domain.makeOracle(state.getExecutionState(), context);

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

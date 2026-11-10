
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

	/**
	 * Returns the lattice of the given value domain in the given state. Looking
	 * up any {@link ValueLattice} would also collect the lattices inside the
	 * one of the domain (e.g., the ones of the participants of a whole-value
	 * analysis), and combine them with it: the lattice is looked up through the
	 * class of the lattices of the domain instead.
	 *
	 * @param vdom  the value domain, possibly {@code null}
	 * @param state the state
	 *
	 * @return the lattice of the domain in the state, or {@code null} if the
	 *             domain is {@code null} or the state has no such lattice
	 *
	 * @throws SemanticException if the lookup fails
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static ValueLattice valueLattice(
			ValueDomain vdom,
			Lattice<?> state)
			throws SemanticException {
		if (vdom == null)
			return null;
		return (ValueLattice) state.getLatticeInstance((Class) vdom.makeLattice().getClass());
	}

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
		ValueLattice env = valueLattice(vdom, state.getExecutionState());
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

package it.unive.jlisa.interprocedural.callgraph.opencallpolicy;

import it.unive.lisa.analysis.*;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.interprocedural.OpenCallPolicy;
import it.unive.lisa.program.cfg.statement.call.OpenCall;

public class ThrowSemanticExceptionPolicy implements OpenCallPolicy {
	public static final ThrowSemanticExceptionPolicy INSTANCE = new ThrowSemanticExceptionPolicy();

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> apply(
			OpenCall call,
			AnalysisState<A> entryState,
			Analysis<A, D> analysis,
			ExpressionSet[] params)
			throws SemanticException {
		throw new SemanticException("Failed to find a target for " + call.toString());
	}
}

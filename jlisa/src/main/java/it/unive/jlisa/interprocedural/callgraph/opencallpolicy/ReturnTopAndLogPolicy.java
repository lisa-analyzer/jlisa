package it.unive.jlisa.interprocedural.callgraph.opencallpolicy;

import it.unive.lisa.analysis.*;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.interprocedural.OpenCallPolicy;
import it.unive.lisa.program.cfg.statement.call.OpenCall;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.Skip;
import java.util.logging.Logger;

public class ReturnTopAndLogPolicy implements OpenCallPolicy {
	public static final ReturnTopAndLogPolicy INSTANCE = new ReturnTopAndLogPolicy();
	private final Logger LOG = Logger.getLogger(ReturnTopAndLogPolicy.class.getName());

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> apply(
			OpenCall call,
			AnalysisState<A> entryState,
			Analysis<A, D> analysis,
			ExpressionSet[] params)
			throws SemanticException {
		LOG.info("Failed to find a target for " + call.toString());
		// throw new SemanticException("Failed to find a target for " +
		// call.toString());
		if (call.getStaticType().isVoidType())
			return analysis.smallStepSemantics(entryState, new Skip(call.getLocation()), call);
		else {
			PushAny pushany = new PushAny(call.getStaticType(), call.getLocation());
			Identifier var = call.getMetaVariable();
			return analysis.assign(entryState, var, pushany, call);
		}
	}
}

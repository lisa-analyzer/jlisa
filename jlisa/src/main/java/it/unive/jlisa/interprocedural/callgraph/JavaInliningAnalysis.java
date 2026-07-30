package it.unive.jlisa.interprocedural.callgraph;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.inlining.InliningAnalysis;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * An extension of {@link InliningAnalysis} that returns top when the maximum
 * call stack depth is reached, instead of throwing an exception.
 */
public class JavaInliningAnalysis<A extends AbstractLattice<A>,
		D extends AbstractDomain<A>>
		extends
		InliningAnalysis<A, D> {

	private static final Logger LOG = LogManager.getLogger(JavaInliningAnalysis.class);

	private int remainingCalls;

	/**
	 * Builds the analysis, using an infinite call stack depth.
	 */
	public JavaInliningAnalysis() {
		this(1);
	}

	/**
	 * Builds the analysis.
	 *
	 * @param k the maximum call stack depth. A negative value means infinite
	 *              depth. If a call chain exceeds this depth, top is returned
	 */
	public JavaInliningAnalysis(
			int k) {
		super(k);
		this.remainingCalls = k;
	}

	@Override
	public AnalysisState<A> getAbstractResultOf(
			CFGCall call,
			AnalysisState<A> entryState,
			ExpressionSet[] parameters,
			StatementStore<A> expressions)
			throws SemanticException {
		if (remainingCalls == 0) {
			LOG.warn("Maximum call stack depth reached for call {} at {}. Returning top.", call, call.getLocation());
			if (call.returnsVoid(null))
				return entryState.topExecution();
			else
				return entryState.topExecution().withExecutionExpression(call.getMetaVariable());
		}

		remainingCalls--;
		AnalysisState<A> result = super.getAbstractResultOf(
				call,
				entryState,
				parameters,
				expressions);
		remainingCalls++;
		return result;
	}

}

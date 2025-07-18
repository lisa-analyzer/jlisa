package it.unive.jlisa.program.cfg.controlflow.switches.instrumentations;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Statement;

/**
 * Instrumentation for switch statements: represent the default case check
 * 
 * @author <a href="mailto:luca.olivieri@unive.it">Luca Olivieri</a>
 */
public class SwitchDefault extends it.unive.lisa.program.cfg.statement.NaryExpression{

	/**
	 * Builds the break, happening at the given location in the program.
	 * 
	 * @param cfg      the cfg that this statement belongs to
	 * @param location the location where this statement is defined within the
	 *                     program
	 */
	public SwitchDefault(CFG cfg, CodeLocation location) {
		super(cfg, location, "default");
	}

	@Override
	protected int compareSameClassAndParams(Statement o) {
		return 0;
	}



	@Override
	public <A extends AbstractState<A>> AnalysisState<A> forwardSemanticsAux(InterproceduralAnalysis<A> interprocedural,
			AnalysisState<A> state, ExpressionSet[] params, StatementStore<A> expressions) throws SemanticException {
		return state; // do nothing
	}

}

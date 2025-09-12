package it.unive.jlisa.program.cfg.expression.instrumentations;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;

/**
 * Statement that instrument an empty body block of a control flow structure
 * TODO: it had implemented because a bug related to graph dumping in LiSA v1.0
 * TODO: to remove after LiSA update and replace with
 * it.unive.lisa.program.cfg.statement.NoOp
 * 
 * @author <a href="mailto:luca.olivieri@unive.it">Luca Olivieri</a>
 */
public class EmptyBody extends it.unive.lisa.program.cfg.statement.NaryExpression {

	/**
	 * Builds the no-op, happening at the given location in the program.
	 * 
	 * @param cfg      the cfg that this statement belongs to
	 * @param location the location where this statement is defined within the
	 *                     program
	 */
	public EmptyBody(
			CFG cfg,
			CodeLocation location) {
		super(cfg, location, "EMPTY_BLOCK", new Expression[0]);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

	@Override
	public <A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> AnalysisState<A> forwardSemanticsAux(
					InterproceduralAnalysis<A, D> interprocedural,
					AnalysisState<A> state,
					ExpressionSet[] params,
					StatementStore<A> expressions)
					throws SemanticException {
		return state; // do nothing
	}

}

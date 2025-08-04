package it.unive.jlisa.program.java.constructs.printwriter;

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
import it.unive.lisa.program.cfg.statement.NaryExpression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;

public class PrintWriterFlush extends NaryExpression implements PluggableStatement {
	protected Statement originating;

	public PrintWriterFlush(CFG cfg, CodeLocation location) {
		super(cfg, location, "flush");
	}

	public static PrintWriterFlush build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new PrintWriterFlush(cfg, location);
	}

	@Override
	protected int compareSameClassAndParams(Statement o) {
		return 0; 
	}
	
	@Override
	public void setOriginatingStatement(Statement st) {
		originating = st;
	}

	@Override
	public <A extends AbstractLattice<A>,
		D extends AbstractDomain<A>> AnalysisState<A> forwardSemanticsAux(InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state, ExpressionSet[] params, StatementStore<A> expressions) throws SemanticException {
		// nothing to do
		return state;
	}
}

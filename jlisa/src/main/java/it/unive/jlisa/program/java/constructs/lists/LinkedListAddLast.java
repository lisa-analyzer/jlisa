package it.unive.jlisa.program.java.constructs.lists;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;

public class LinkedListAddLast
		extends
		it.unive.lisa.program.cfg.statement.BinaryExpression
		implements
		PluggableStatement {
	protected Statement originating;

	public LinkedListAddLast(
			CFG cfg,
			CodeLocation location,
			Expression rec,
			Expression arg) {
		super(cfg, location, "addLast", rec, arg);
	}

	public static LinkedListAddLast build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new LinkedListAddLast(cfg, location, params[0], params[1]);
	}

	@Override
	public void setOriginatingStatement(
			Statement st) {
		originating = st;
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdBinarySemantics(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression left,
			SymbolicExpression right,
			StatementStore<A> expressions)
			throws SemanticException {
		return state;
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}
}

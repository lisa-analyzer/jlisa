package it.unive.jlisa.program.java.constructs.map;

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
import it.unive.lisa.program.cfg.statement.UnaryExpression;
import it.unive.lisa.symbolic.SymbolicExpression;

/**
 * The constructor {@code HashMap()} of {@code java.util.HashMap}: maps are not
 * modeled, so the construction of an empty map has no effect, and in
 * particular it does not modify other objects (without a model, the call
 * would be an open call, whose effects on the heap are unknown).
 */
public class HashMapEmptyConstructor extends UnaryExpression implements PluggableStatement {
	protected Statement originating;

	public HashMapEmptyConstructor(
			CFG cfg,
			CodeLocation location,
			Expression param) {
		super(cfg, location, "HashMap", param);
	}

	public static HashMapEmptyConstructor build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new HashMapEmptyConstructor(cfg, location, params[0]);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

	@Override
	public <A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> AnalysisState<A> fwdUnarySemantics(
					InterproceduralAnalysis<A, D> interprocedural,
					AnalysisState<A> state,
					SymbolicExpression expr,
					StatementStore<A> expressions)
					throws SemanticException {
		return state;
	}

	@Override
	public void setOriginatingStatement(
			Statement st) {
		originating = st;
	}

}

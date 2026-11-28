package it.unive.jlisa.program.java.constructs.lists;

import it.unive.jlisa.program.SyntheticCodeLocationManager;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;

public class ArrayListConstructor
		extends
		it.unive.lisa.program.cfg.statement.UnaryExpression
		implements
		PluggableStatement {
	protected Statement originating;

	/**
	 * The location of the allocation of {@code $elementData}, different from
	 * the one of the list.
	 */
	private final CodeLocation elementDataLocation;

	public ArrayListConstructor(
			CFG cfg,
			CodeLocation location,
			Expression arg) {
		super(cfg, location, "ArrayList", arg);
		elementDataLocation = new SyntheticCodeLocationManager("list-elementdata-" + location.toString())
				.nextLocation();
	}

	public static ArrayListConstructor build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new ArrayListConstructor(cfg, location, params[0]);
	}

	@Override
	public void setOriginatingStatement(
			Statement st) {
		originating = st;
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdUnarySemantics(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression expr,
			StatementStore<A> expressions)
			throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();
		return ListModel.init(analysis, context, state, expr, elementDataLocation, this);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}
}

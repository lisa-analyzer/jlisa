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

/**
 * The constructor that copies a collection (e.g., {@code new ArrayList<>(c)}):
 * see {@link ListModel#copy}. A collection that is not modeled gives a copy
 * that is not modeled either.
 */
public class ListCopyConstructor
		extends
		it.unive.lisa.program.cfg.statement.BinaryExpression
		implements
		PluggableStatement {
	protected Statement originating;

	/**
	 * The location of the allocation of the array of the copy.
	 */
	private final CodeLocation elementDataLocation;

	public ListCopyConstructor(
			CFG cfg,
			CodeLocation location,
			Expression rec,
			Expression source) {
		super(cfg, location, "ArrayList", rec, source);
		elementDataLocation = new SyntheticCodeLocationManager("copy-elementdata-" + location.toString())
				.nextLocation();
	}

	public static ListCopyConstructor build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new ListCopyConstructor(cfg, location, params[0], params[1]);
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
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();
		if (!ListModel.isModeled(analysis, context, state, right, this))
			return state;
		return ListModel.copy(analysis, context, state, left, right, elementDataLocation, false, this);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}
}

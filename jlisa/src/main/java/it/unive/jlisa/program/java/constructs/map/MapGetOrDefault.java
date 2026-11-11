package it.unive.jlisa.program.java.constructs.map;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.TernaryExpression;
import it.unive.lisa.symbolic.SymbolicExpression;

/**
 * {@code Map::getOrDefault}: the value of the key (as {@link MapGet}) if the
 * key is in the map, the default value otherwise. Whether the key is in the map
 * is the result of {@link MapContainsKey}.
 */
public class MapGetOrDefault extends TernaryExpression implements PluggableStatement {
	protected Statement originating;

	public MapGetOrDefault(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression middle,
			Expression right) {
		super(cfg, location, "getOrDefault", left, middle, right);
	}

	public static MapGetOrDefault build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new MapGetOrDefault(cfg, location, params[0], params[1], params[2]);
	}

	@Override
	public void setOriginatingStatement(
			Statement st) {
		originating = st;
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdTernarySemantics(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression left,
			SymbolicExpression middle,
			SymbolicExpression right,
			StatementStore<A> expressions)
			throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();
		CodeLocation location = getLocation();

		MapContainsKey contains = new MapContainsKey(getCFG(), location, getLeft(), getMiddle());
		MapGet get = new MapGet(getCFG(), location, getLeft(), getMiddle());
		// the constructs above take the sub-expressions as theirs
		for (Expression sub : getSubExpressions())
			sub.setParentStatement(this);
		get.setOriginatingStatement(originating);

		AnalysisState<A> tested = contains.fwdBinarySemantics(interprocedural, state, left, middle, expressions);
		AnalysisState<A> result = state.bottomExecution();
		boolean present = false, absent = false;
		for (SymbolicExpression e : tested.getExecutionExpressions()) {
			Satisfiability sat = analysis.satisfies(tested, e, context, this);
			present |= sat != Satisfiability.NOT_SATISFIED;
			absent |= sat != Satisfiability.SATISFIED;
		}
		if (present)
			result = result.lub(get.fwdBinarySemantics(interprocedural, state, left, middle, expressions));
		if (absent)
			result = result.lub(analysis.smallStepSemantics(state, right, context, this));
		getMetaVariables().addAll(contains.getMetaVariables());
		getMetaVariables().addAll(get.getMetaVariables());
		return MapModel.nullKey(interprocedural, state, result, left, middle, this, getSubExpressions(),
				expressions);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}
}

package it.unive.jlisa.program.java.constructs.map;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NaryExpression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

/**
 * A method that adds keys to a map that are not modeled one by one (e.g.,
 * {@code putAll}, {@code putIfAbsent}): see {@link MapModel#addUnknownKeys}.
 * The returned value is unknown.
 */
public class MapPutUnknown extends NaryExpression implements PluggableStatement {
	protected Statement originating;

	public MapPutUnknown(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		super(cfg, location, "putUnknown", params);
	}

	public static MapPutUnknown build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new MapPutUnknown(cfg, location, params);
	}

	@Override
	public void setOriginatingStatement(
			Statement st) {
		originating = st;
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> forwardSemanticsAux(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			ExpressionSet[] params,
			StatementStore<A> expressions)
			throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();
		AnalysisState<A> result = state.bottomExecution();
		for (SymbolicExpression map : params[0])
			if (MapModel.isTracked(analysis, context, state, map, this))
				result = result.lub(MapModel.addUnknownKeys(analysis, context, state, map, this));
			else
				result = result.lub(state);
		Type returned = originating instanceof Expression e ? e.getStaticType() : Untyped.INSTANCE;
		if (returned.isVoidType())
			return result;
		return analysis.smallStepSemantics(result, new PushAny(returned, getLocation()), context, this);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}
}

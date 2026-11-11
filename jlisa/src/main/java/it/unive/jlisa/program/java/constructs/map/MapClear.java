package it.unive.jlisa.program.java.constructs.map;

import it.unive.jlisa.program.type.JavaIntType;
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
import it.unive.lisa.program.cfg.statement.UnaryExpression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.value.Constant;

public class MapClear extends UnaryExpression implements PluggableStatement {
	protected Statement originating;

	public MapClear(
			CFG cfg,
			CodeLocation location,
			Expression expr) {
		super(cfg, location, "clear", expr);
	}

	public static MapClear build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new MapClear(cfg, location, params[0]);
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

		CodeLocation location = getLocation();
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();
		AccessChild accessSize = MapModel.size(expr, location);

		Constant zero = new Constant(JavaIntType.INSTANCE, 0, location);
		AnalysisState<A> tmp = analysis.assign(state, accessSize, zero, context, this);
		tmp = MapModel.clear(analysis, context, tmp, expr, this);

		// TODO: remove key entries

		return tmp;
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

}

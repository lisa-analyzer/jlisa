package it.unive.jlisa.program.java.constructs.map;

import it.unive.jlisa.program.type.JavaBooleanType;
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
import it.unive.lisa.program.cfg.statement.BinaryExpression;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.value.Constant;

/**
 * The constructor {@code HashMap(Map m)} of {@code java.util.HashMap}: the new
 * map has the size of {@code m}, but its entries are not copied, so the map is
 * marked as having unknown keys (see {@link MapGet}).
 */
public class MapCopyConstructor extends BinaryExpression implements PluggableStatement {
	protected Statement originating;

	public MapCopyConstructor(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression right) {
		super(cfg, location, "Map", left, right);
	}

	public static MapCopyConstructor build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new MapCopyConstructor(cfg, location, params[0], params[1]);
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
		CodeLocation location = getLocation();
		AccessChild newSize = MapModel.size(left, location);
		AccessChild oldSize = MapModel.size(right, location);
		AnalysisState<A> tmp = analysis.assign(state, newSize, oldSize, context, this);
		tmp = analysis.assign(tmp, MapGet.unknownKeys(left, location),
				new Constant(JavaBooleanType.INSTANCE, true, location), context, this);
		return analysis.smallStepSemantics(tmp, left, context, this);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}
}

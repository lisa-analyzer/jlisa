package it.unive.jlisa.program.java.constructs.map;

import it.unive.jlisa.program.type.JavaBooleanType;
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
import it.unive.lisa.symbolic.value.PushFromConstraints;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import java.util.Set;

public class ConcurrentMapEmptyConstructor extends UnaryExpression implements PluggableStatement {
	protected Statement originating;

	public ConcurrentMapEmptyConstructor(
			CFG cfg,
			CodeLocation location,
			Expression expr) {
		super(cfg, location, "ConcurrentMap", expr);
	}

	public static ConcurrentMapEmptyConstructor build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new ConcurrentMapEmptyConstructor(cfg, location, params[0]);
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

		CodeLocation location = getLocation();

		AccessChild accessSize = MapModel.size(expr, location);

		PushFromConstraints size = new PushFromConstraints(JavaIntType.INSTANCE, getLocation(), Set.of(
				new it.unive.lisa.symbolic.value.BinaryExpression(
						JavaBooleanType.INSTANCE,
						new Constant(JavaIntType.INSTANCE, 0, getLocation()),
						accessSize,
						ComparisonLe.INSTANCE,
						getLocation())));

		return analysis.assign(state, accessSize, size, context, this);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}
}

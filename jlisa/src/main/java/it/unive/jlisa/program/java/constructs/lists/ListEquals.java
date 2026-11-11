package it.unive.jlisa.program.java.constructs.lists;

import it.unive.jlisa.program.java.constructs.map.MapModel;
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
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.PushAny;

/**
 * {@code List::equals}: a list is equal to itself, otherwise the result is
 * unknown.
 */
public class ListEquals extends BinaryExpression implements PluggableStatement {
	protected Statement originating;

	public ListEquals(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression right) {
		super(cfg, location, "equals", JavaBooleanType.INSTANCE, left, right);
	}

	public static ListEquals build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new ListEquals(cfg, location, params[0], params[1]);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
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
		SymbolicExpression res = MapModel.sameObject(analysis, context, state, left, right, this)
				? new Constant(JavaBooleanType.INSTANCE, true, location)
				: new PushAny(JavaBooleanType.INSTANCE, location);
		return analysis.smallStepSemantics(state, res, context, this);
	}
}

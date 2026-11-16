package it.unive.jlisa.program.java.constructs.map;

import it.unive.jlisa.program.java.constructs.lists.ListModel;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.BinaryExpression;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;

public class ConcurrentMapGet extends BinaryExpression implements PluggableStatement {
	protected Statement originating;

	public ConcurrentMapGet(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression right) {
		super(cfg, location, "get", left, right);
	}

	public static ConcurrentMapGet build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new ConcurrentMapGet(cfg, location, params[0], params[1]);
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

		// as for the other maps, but a ConcurrentMap is not null safe: a key
		// that might be null raises a NullPointerException (checked first, also
		// on an empty map)
		MapGet get = new MapGet(getCFG(), getLocation(), getLeft(), getRight());
		// MapGet takes the sub-expressions as its own
		getLeft().setParentStatement(this);
		getRight().setParentStatement(this);
		get.setOriginatingStatement(originating);
		AnalysisState<A> res = get.fwdBinarySemantics(interprocedural, state, left, right, expressions);
		getMetaVariables().addAll(get.getMetaVariables());
		return ListModel.nullArgument(interprocedural, state, res, right, this, getSubExpressions(), expressions);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}
}

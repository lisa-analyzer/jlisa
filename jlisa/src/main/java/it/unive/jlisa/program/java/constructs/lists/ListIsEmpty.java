package it.unive.jlisa.program.java.constructs.lists;

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
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;

public class ListIsEmpty
		extends
		it.unive.lisa.program.cfg.statement.UnaryExpression
		implements
		PluggableStatement {
	protected Statement originating;

	public ListIsEmpty(
			CFG cfg,
			CodeLocation location,
			Expression rec) {
		super(cfg, location, "isEmpty", rec);
	}

	public static ListIsEmpty build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new ListIsEmpty(cfg, location, params[0]);
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
		if (!ListModel.isModeled(analysis, context, state, expr, this))
			return analysis.smallStepSemantics(state, new PushAny(JavaBooleanType.INSTANCE, getLocation()), context,
					this);
		return analysis.smallStepSemantics(state,
				ListModel.compareSize(expr, ComparisonEq.INSTANCE, 0, getLocation()), context, this);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}
}

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
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;

public class IteratorHasNext
		extends
		it.unive.lisa.program.cfg.statement.UnaryExpression
		implements
		PluggableStatement {
	protected Statement originating;

	public IteratorHasNext(
			CFG cfg,
			CodeLocation location,
			Expression rec) {
		super(cfg, location, "hasNext", rec);
	}

	public static IteratorHasNext build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new IteratorHasNext(cfg, location, params[0]);
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
		if (!ListModel.isModeledIterator(analysis, context, state, expr, this))
			return analysis.smallStepSemantics(state, new PushAny(JavaBooleanType.INSTANCE, location), context,
					this);

		// there is a next element if the cursor is before the end of the list
		return analysis.smallStepSemantics(state,
				new BinaryExpression(JavaBooleanType.INSTANCE, ListModel.cursor(expr, location),
						ListModel.size(ListModel.iteratedList(expr, location), location), ComparisonLt.INSTANCE,
						location),
				context, this);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}
}

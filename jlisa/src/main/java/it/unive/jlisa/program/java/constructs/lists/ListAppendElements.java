package it.unive.jlisa.program.java.constructs.lists;

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

/**
 * A method that appends elements to a list (e.g. {@code addAll}): the list has
 * at least the elements it had, which are kept, and the other ones are unknown.
 */
public class ListAppendElements extends NaryExpression implements PluggableStatement {
	protected Statement originating;

	public ListAppendElements(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		super(cfg, location, "appendElements", params);
	}

	public static ListAppendElements build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new ListAppendElements(cfg, location, params);
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
		for (SymbolicExpression list : params[0])
			if (ListModel.isModeled(analysis, context, state, list, this)) {
				AnalysisState<A> modifiable = ListModel.allowed(analysis, context, state, list, ListModel.FIXED_SIZE,
						this);
				AnalysisState<A> done = modifiable.getExecutionState().isBottom() ? modifiable
						: ListModel.change(analysis, context, modifiable, list, ListModel.SizeChange.GROWS, true, this);
				result = result
						.lub(ListModel.unsupported(interprocedural, state, done, list, ListModel.FIXED_SIZE, this,
								getSubExpressions(), expressions));
			} else
				result = result.lub(state);

		// the returned value is unknown
		Type returned = originating instanceof Expression e ? e.getStaticType() : getStaticType();
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

package it.unive.jlisa.program.java.constructs.lists;

import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalysisState.Error;
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
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;

public class LinkedListGetLast
		extends
		it.unive.lisa.program.cfg.statement.UnaryExpression
		implements
		PluggableStatement {
	protected Statement originating;

	public LinkedListGetLast(
			CFG cfg,
			CodeLocation location,
			Expression rec) {
		super(cfg, location, "getLast", rec);
	}

	public static LinkedListGetLast build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new LinkedListGetLast(cfg, location, params[0]);
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
		PushAny any = new PushAny(new JavaReferenceType(JavaClassType.getObjectType()), location);
		if (!ListModel.isModeled(analysis, context, state, expr, this))
			return analysis.smallStepSemantics(state, any, context, this);

		// the last element is in the cell at index size - 1, each cell under
		// the condition that size denotes it (as for the cells of the arrays)
		AnalysisState<A> result = state.bottomExecution();
		for (int i = 1; i <= ListModel.MAX_TRACKED_ELEMENTS; i++) {
			AnalysisState<A> cell = analysis.assume(state,
					ListModel.compareSize(expr, ComparisonEq.INSTANCE, i, location), context, this, this);
			if (cell.getExecutionState().isBottom())
				continue;
			result = result.lub(
					analysis.smallStepSemantics(cell, ListModel.element(expr, i - 1, location), context, this));
		}

		// the elements after the tracked cells are unknown
		BinaryExpression untracked = ListModel.compareSize(expr, ComparisonGt.INSTANCE,
				ListModel.MAX_TRACKED_ELEMENTS, location);
		AnalysisState<A> beyond = analysis.assume(state, untracked, context, this, this);
		if (!beyond.getExecutionState().isBottom())
			result = result.lub(analysis.smallStepSemantics(beyond, any, context, this));

		// the empty list raises NoSuchElementException
		AnalysisState<A> empty = analysis.assume(state,
				ListModel.compareSize(expr, ComparisonLe.INSTANCE, 0, location), context, this, this);
		if (empty.getExecutionState().isBottom())
			return result;
		String exc = "java.util.NoSuchElementException";
		AnalysisState<A> raised = ListModel.raise(interprocedural, empty, exc, this, getSubExpressions(),
				expressions);
		Error error = new Error(JavaClassType.lookup(exc).getReference(), this);
		if (result.getExecutionState().isBottom())
			return analysis.moveExecutionToError(raised, error, context, this);
		return analysis.moveExecutionToError(result, raised, error, context, this);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}
}

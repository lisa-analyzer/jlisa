package it.unive.jlisa.program.java.constructs.lists;

import it.unive.jlisa.program.type.JavaBooleanType;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaIntType;
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
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingAdd;

public class IteratorNext
		extends
		it.unive.lisa.program.cfg.statement.UnaryExpression
		implements
		PluggableStatement {
	protected Statement originating;

	public IteratorNext(
			CFG cfg,
			CodeLocation location,
			Expression rec) {
		super(cfg, location, "next", rec);
	}

	public static IteratorNext build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new IteratorNext(cfg, location, params[0]);
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
		if (!ListModel.isModeledIterator(analysis, context, state, expr, this))
			return analysis.smallStepSemantics(state, any, context, this);

		AccessChild cursor = ListModel.cursor(expr, location);
		AccessChild list = ListModel.iteratedList(expr, location);
		BinaryExpression hasNext = new BinaryExpression(JavaBooleanType.INSTANCE, cursor,
				ListModel.size(list, location), ComparisonLt.INSTANCE, location);
		AnalysisState<A> next = analysis.assume(state, hasNext, context, this, this);

		// the element is in the cell at the cursor, each cell under the
		// condition that the cursor denotes it (as for the cells of the arrays)
		AnalysisState<A> result = state.bottomExecution();
		if (!next.getExecutionState().isBottom()) {
			for (int i = 0; i < ListModel.MAX_TRACKED_ELEMENTS; i++) {
				AnalysisState<A> cell = analysis.assume(next, new BinaryExpression(JavaBooleanType.INSTANCE, cursor,
						new Constant(JavaIntType.INSTANCE, i, location), ComparisonEq.INSTANCE, location), context,
						this, this);
				if (cell.getExecutionState().isBottom())
					continue;
				result = result.lub(
						analysis.smallStepSemantics(cell, ListModel.element(list, i, location), context, this));
			}

			// the elements after the tracked cells are unknown
			AnalysisState<A> beyond = analysis.assume(next,
					new BinaryExpression(JavaBooleanType.INSTANCE, cursor,
							new Constant(JavaIntType.INSTANCE, ListModel.MAX_TRACKED_ELEMENTS, location),
							ComparisonGe.INSTANCE, location),
					context, this, this);
			if (!beyond.getExecutionState().isBottom())
				result = result.lub(analysis.smallStepSemantics(beyond, any, context, this));

			// the cursor moves to the following element, keeping the
			// returned element on the stack
			AnalysisState<A> moved = result.bottomExecution();
			for (SymbolicExpression element : result.getExecutionExpressions()) {
				AnalysisState<A> tmp = analysis.assign(result, cursor,
						new BinaryExpression(JavaIntType.INSTANCE, cursor,
								new Constant(JavaIntType.INSTANCE, 1, location), NumericNonOverflowingAdd.INSTANCE,
								location),
						context, this);
				moved = moved.lub(tmp.withExecutionExpression(element));
			}
			result = moved;
		}

		// there is no next element: NoSuchElementException
		AnalysisState<A> end = analysis.assume(state,
				new BinaryExpression(JavaBooleanType.INSTANCE, cursor, ListModel.size(list, location),
						ComparisonGe.INSTANCE, location),
				context, this, this);
		if (end.getExecutionState().isBottom())
			return result;
		String exc = "java.util.NoSuchElementException";
		AnalysisState<A> raised = ListModel.raise(interprocedural, end, exc, this, getSubExpressions(),
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

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
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.binary.LogicalOr;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;

public class ListGet
		extends
		it.unive.lisa.program.cfg.statement.BinaryExpression
		implements
		PluggableStatement {
	protected Statement originating;

	public ListGet(
			CFG cfg,
			CodeLocation location,
			Expression rec,
			Expression index) {
		super(cfg, location, "get", rec, index);
	}

	public static ListGet build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new ListGet(cfg, location, params[0], params[1]);
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
		PushAny any = new PushAny(new JavaReferenceType(JavaClassType.getObjectType()), location);
		if (!ListModel.isModeled(analysis, context, state, left, this))
			return analysis.smallStepSemantics(state, any, context, this);

		// the index is out of bounds if it is negative or not smaller than size
		BinaryExpression outOfBounds = new BinaryExpression(JavaBooleanType.INSTANCE,
				new BinaryExpression(JavaBooleanType.INSTANCE, right, new Constant(JavaIntType.INSTANCE, 0, location),
						ComparisonLt.INSTANCE, location),
				new BinaryExpression(JavaBooleanType.INSTANCE, right, ListModel.size(left, location),
						ComparisonGe.INSTANCE, location),
				LogicalOr.INSTANCE, location);
		AnalysisState<A> inBounds = analysis.assume(state,
				new UnaryExpression(JavaBooleanType.INSTANCE, outOfBounds, LogicalNegation.INSTANCE, location),
				context, this, this);

		// the element is in the cell at the index, each cell under the
		// condition that the index denotes it (as for the cells of the arrays)
		AnalysisState<A> result = state.bottomExecution();
		if (!inBounds.getExecutionState().isBottom()) {
			for (int i = 0; i < ListModel.MAX_TRACKED_ELEMENTS; i++) {
				AnalysisState<A> cell = analysis.assume(inBounds, new BinaryExpression(JavaBooleanType.INSTANCE,
						right, new Constant(JavaIntType.INSTANCE, i, location), ComparisonEq.INSTANCE, location),
						context, this, this);
				if (cell.getExecutionState().isBottom())
					continue;
				result = result.lub(
						analysis.smallStepSemantics(cell, ListModel.element(left, i, location), context, this));
			}

			// the elements after the tracked cells are unknown
			AnalysisState<A> beyond = analysis.assume(inBounds,
					new BinaryExpression(JavaBooleanType.INSTANCE, right,
							new Constant(JavaIntType.INSTANCE, ListModel.MAX_TRACKED_ELEMENTS, location),
							ComparisonGe.INSTANCE, location),
					context, this, this);
			if (!beyond.getExecutionState().isBottom())
				result = result.lub(analysis.smallStepSemantics(beyond, any, context, this));
		}

		AnalysisState<A> out = analysis.assume(state, outOfBounds, context, this, this);
		if (out.getExecutionState().isBottom())
			return result;
		String exc = "java.lang.IndexOutOfBoundsException";
		AnalysisState<A> raised = ListModel.raise(interprocedural, out, exc, this, getSubExpressions(),
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

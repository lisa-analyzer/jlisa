package it.unive.jlisa.program.java.constructs.sets;

import it.unive.jlisa.program.java.constructs.lists.ListModel;
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
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.PushFromConstraints;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import java.util.HashSet;
import java.util.Set;

public class SetAdd
		extends
		it.unive.lisa.program.cfg.statement.BinaryExpression
		implements
		PluggableStatement {
	protected Statement originating;

	public SetAdd(
			CFG cfg,
			CodeLocation location,
			Expression rec,
			Expression arg) {
		super(cfg, location, "add", rec, arg);
	}

	public static SetAdd build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new SetAdd(cfg, location, params[0], params[1]);
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
		if (!ListModel.isModeled(analysis, context, state, left, this))
			return analysis.smallStepSemantics(state, new PushAny(JavaBooleanType.INSTANCE, location), context,
					this);

		// the element is written after the other ones, but it might already
		// be in the set: the new size is unknown, between the old one (at least
		// 1) and the old one plus 1, and so is the returned value
		AnalysisState<A> boxed = ListModel.box(interprocedural, state, right, getRight(), this, expressions);
		AnalysisState<A> added = boxed.bottomExecution();
		for (SymbolicExpression value : boxed.getExecutionExpressions())
			added = added.lub(ListModel.append(analysis, context, boxed, left, value, this));
		AccessChild size = ListModel.size(left, location);
		AnalysisState<A> result = added.bottomExecution();
		for (int k = 1; k <= ListModel.MAX_TRACKED_ELEMENTS + 1; k++) {
			boolean beyond = k == ListModel.MAX_TRACKED_ELEMENTS + 1;
			AnalysisState<A> after = analysis.assume(added,
					ListModel.compareSize(left, beyond ? ComparisonGe.INSTANCE : ComparisonEq.INSTANCE, k, location),
					context, this, this);
			if (after.getExecutionState().isBottom())
				continue;
			Set<BinaryExpression> bounds = new HashSet<>();
			bounds.add(new BinaryExpression(JavaBooleanType.INSTANCE,
					new Constant(JavaIntType.INSTANCE, Math.max(k - 1, 1), location), size, ComparisonLe.INSTANCE,
					location));
			if (!beyond)
				bounds.add(new BinaryExpression(JavaBooleanType.INSTANCE,
						new Constant(JavaIntType.INSTANCE, k, location), size, ComparisonGe.INSTANCE, location));
			result = result.lub(analysis.assign(after, size,
					new PushFromConstraints(JavaIntType.INSTANCE, location, bounds), context, this));
		}
		return analysis.smallStepSemantics(result, new PushAny(JavaBooleanType.INSTANCE, location), context, this);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}
}

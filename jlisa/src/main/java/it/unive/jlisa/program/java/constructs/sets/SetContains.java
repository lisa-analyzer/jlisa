package it.unive.jlisa.program.java.constructs.sets;

import it.unive.jlisa.program.java.constructs.lists.ListModel;
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
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;

public class SetContains
		extends
		it.unive.lisa.program.cfg.statement.BinaryExpression
		implements
		PluggableStatement {
	protected Statement originating;

	public SetContains(
			CFG cfg,
			CodeLocation location,
			Expression rec,
			Expression arg) {
		super(cfg, location, "contains", rec, arg);
	}

	public static SetContains build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new SetContains(cfg, location, params[0], params[1]);
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
		PushAny any = new PushAny(JavaBooleanType.INSTANCE, location);
		if (!ListModel.isModeled(analysis, context, state, left, this))
			return analysis.smallStepSemantics(state, any, context, this);

		// the empty set contains nothing, a set with an element that is surely
		// the object contains it, otherwise the result is unknown
		AnalysisState<A> result = state.bottomExecution();
		AnalysisState<A> empty = analysis.assume(state,
				ListModel.compareSize(left, ComparisonEq.INSTANCE, 0, location), context, this, this);
		if (!empty.getExecutionState().isBottom())
			result = result.lub(analysis.smallStepSemantics(empty,
					new Constant(JavaBooleanType.INSTANCE, false, location), context, this));
		for (int k = 1; k <= ListModel.MAX_TRACKED_ELEMENTS; k++) {
			boolean beyond = k == ListModel.MAX_TRACKED_ELEMENTS;
			AnalysisState<A> sized = analysis.assume(state,
					ListModel.compareSize(left, beyond ? ComparisonGe.INSTANCE : ComparisonEq.INSTANCE, k, location),
					context, this, this);
			if (sized.getExecutionState().isBottom())
				continue;
			boolean found = false;
			for (int i = 0; i < k && !found; i++)
				found = MapModel.sameObject(analysis, context, sized, ListModel.element(left, i, location), right,
						this);
			result = result.lub(analysis.smallStepSemantics(sized,
					found ? new Constant(JavaBooleanType.INSTANCE, true, location) : any, context, this));
		}
		return MapModel.single(analysis, context, result, JavaBooleanType.INSTANCE, this);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}
}

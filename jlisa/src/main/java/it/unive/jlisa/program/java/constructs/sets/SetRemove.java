package it.unive.jlisa.program.java.constructs.sets;

import it.unive.jlisa.program.java.constructs.lists.ListModel;
import it.unive.jlisa.program.java.constructs.map.MapModel;
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
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;

/**
 * {@code Set::remove}: if an element of the set is surely the object, it is
 * removed (the set has no other element equal to it); otherwise, the set has at
 * most the elements it had, and they are unknown.
 */
public class SetRemove
		extends
		it.unive.lisa.program.cfg.statement.BinaryExpression
		implements
		PluggableStatement {
	protected Statement originating;

	public SetRemove(
			CFG cfg,
			CodeLocation location,
			Expression rec,
			Expression arg) {
		super(cfg, location, "remove", rec, arg);
	}

	public static SetRemove build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new SetRemove(cfg, location, params[0], params[1]);
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

		AnalysisState<A> modifiable = ListModel.allowed(analysis, context, state, left, ListModel.FIXED_SIZE, this);
		AnalysisState<A> done = modifiable.bottomExecution();
		for (int k = 0; k <= ListModel.MAX_TRACKED_ELEMENTS && !modifiable.getExecutionState().isBottom(); k++) {
			boolean beyond = k == ListModel.MAX_TRACKED_ELEMENTS;
			AnalysisState<A> sized = analysis.assume(modifiable,
					ListModel.compareSize(left, beyond ? ComparisonGe.INSTANCE : ComparisonEq.INSTANCE, k, location),
					context, this, this);
			if (sized.getExecutionState().isBottom())
				continue;
			int found = -1;
			for (int i = 0; !beyond && i < k && found < 0; i++)
				if (MapModel.sameObject(analysis, context, sized, ListModel.element(left, i, location), right, this))
					found = i;
			if (found < 0) {
				done = done.lub(ListModel.change(analysis, context, sized, left, ListModel.SizeChange.SHRINKS, false,
						this));
				continue;
			}
			// the elements after the removed one are moved back
			AnalysisState<A> tmp = sized;
			for (int i = found; i < k - 1; i++)
				tmp = analysis.assign(tmp, ListModel.element(left, i, location),
						ListModel.element(left, i + 1, location), context, this);
			done = done.lub(analysis.assign(tmp, ListModel.size(left, location),
					new Constant(JavaIntType.INSTANCE, k - 1, location), context, this));
		}
		AnalysisState<A> result = ListModel.unsupported(interprocedural, state, done, left, ListModel.FIXED_SIZE,
				this, getSubExpressions(), expressions);
		return analysis.smallStepSemantics(result, any, context, this);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}
}

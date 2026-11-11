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
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.PushAny;

public class ListAdd
		extends
		it.unive.lisa.program.cfg.statement.BinaryExpression
		implements
		PluggableStatement {
	protected Statement originating;

	public ListAdd(
			CFG cfg,
			CodeLocation location,
			Expression rec,
			Expression arg) {
		super(cfg, location, "add", rec, arg);
	}

	public static ListAdd build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new ListAdd(cfg, location, params[0], params[1]);
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
		if (!ListModel.isModeled(analysis, context, state, left, this))
			return analysis.smallStepSemantics(state, new PushAny(JavaBooleanType.INSTANCE, getLocation()), context,
					this);
		AnalysisState<A> modifiable = ListModel.allowed(analysis, context, state, left, ListModel.FIXED_SIZE, this);
		AnalysisState<A> boxed = ListModel.box(interprocedural, modifiable, right, getRight(), this, expressions);
		AnalysisState<A> added = boxed.bottomExecution();
		for (SymbolicExpression value : boxed.getExecutionExpressions())
			added = added.lub(ListModel.append(analysis, context, boxed, left, value, this));
		AnalysisState<A> done = modifiable.getExecutionState().isBottom() ? added
				: analysis.smallStepSemantics(added, new Constant(JavaBooleanType.INSTANCE, true, getLocation()),
						context, this);
		return ListModel.unsupported(interprocedural, state, done, left, ListModel.FIXED_SIZE, this,
				getSubExpressions(), expressions);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}
}

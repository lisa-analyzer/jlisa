package it.unive.jlisa.program.java.constructs.longw;

import it.unive.jlisa.program.operator.JavaLongValueOfOperator;
import it.unive.jlisa.program.type.JavaLongType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.UnaryExpression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

public class LongValueOf extends UnaryExpression implements PluggableStatement {
	protected Statement originating;

	public LongValueOf(
			CFG cfg,
			CodeLocation location,
			Expression exp) {
		super(cfg, location, "valueOf", exp);
	}

	public static LongValueOf build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new LongValueOf(cfg, location, params[0]);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
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
		Type longType = expr.getStaticType();
		Analysis<A, D> analysis = interprocedural.getAnalysis();

		GlobalVariable var = new GlobalVariable(Untyped.INSTANCE, "value", getLocation());
		HeapDereference derefLeft = new HeapDereference(longType, expr, getLocation());
		AccessChild accessLeft = new AccessChild(longType, derefLeft, var, getLocation());

		it.unive.lisa.symbolic.value.UnaryExpression valueOf = new it.unive.lisa.symbolic.value.UnaryExpression(
				JavaLongType.INSTANCE,
				accessLeft,
				JavaLongValueOfOperator.INSTANCE,
				getLocation());

		return analysis.smallStepSemantics(state, valueOf, originating);

	}
}

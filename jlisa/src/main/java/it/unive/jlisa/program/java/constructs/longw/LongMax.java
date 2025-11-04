package it.unive.jlisa.program.java.constructs.longw;

import it.unive.jlisa.program.operator.JavaLongMaxOperator;
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
import it.unive.lisa.program.cfg.statement.BinaryExpression;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

public class LongMax extends BinaryExpression implements PluggableStatement {
	protected Statement originating;

	protected LongMax(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression right) {
		super(cfg, location, "max", left, right);
	}

	public static LongMax build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new LongMax(cfg, location, params[0], params[1]);
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

		Type longType = left.getStaticType();
		Analysis<A, D> analysis = interprocedural.getAnalysis();

		GlobalVariable var = new GlobalVariable(Untyped.INSTANCE, "value", getLocation());
		HeapDereference derefLeft = new HeapDereference(longType, left, getLocation());
		AccessChild accessLeft = new AccessChild(longType, derefLeft, var, getLocation());

		HeapDereference derefRight = new HeapDereference(longType, right, getLocation());
		AccessChild accessRight = new AccessChild(longType, derefRight, var, getLocation());

		it.unive.lisa.symbolic.value.BinaryExpression max = new it.unive.lisa.symbolic.value.BinaryExpression(
				JavaLongType.INSTANCE,
				accessLeft,
				accessRight,
				JavaLongMaxOperator.INSTANCE,
				getLocation());

		return analysis.smallStepSemantics(state, max, originating);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

}

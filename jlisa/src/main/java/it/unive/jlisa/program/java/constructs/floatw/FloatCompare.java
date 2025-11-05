package it.unive.jlisa.program.java.constructs.floatw;

import it.unive.jlisa.program.operator.JavaFloatCompareOperator;
import it.unive.jlisa.program.type.JavaIntType;
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

public class FloatCompare extends BinaryExpression implements PluggableStatement {
	protected Statement originating;

	protected FloatCompare(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression right) {
		super(cfg, location, "float-compare", left, right);
	}

	public static FloatCompare build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new FloatCompare(cfg, location, params[0], params[1]);
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

		Type floatType = left.getStaticType();
		Analysis<A, D> analysis = interprocedural.getAnalysis();

		GlobalVariable var = new GlobalVariable(Untyped.INSTANCE, "value", getLocation());
		HeapDereference derefLeft = new HeapDereference(floatType, left, getLocation());
		AccessChild accessLeft = new AccessChild(floatType, derefLeft, var, getLocation());

		HeapDereference derefRight = new HeapDereference(floatType, right, getLocation());
		AccessChild accessRight = new AccessChild(floatType, derefRight, var, getLocation());

		it.unive.lisa.symbolic.value.BinaryExpression rotate = new it.unive.lisa.symbolic.value.BinaryExpression(
				JavaIntType.INSTANCE,
				accessLeft,
				accessRight,
				JavaFloatCompareOperator.INSTANCE,
				getLocation());

		return analysis.smallStepSemantics(state, rotate, originating);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

}

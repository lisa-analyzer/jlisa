package it.unive.jlisa.program.java.constructs.longw;

import it.unive.jlisa.program.operator.JavaLongByteValueOperator;
import it.unive.jlisa.program.type.JavaByteType;
import it.unive.jlisa.program.type.JavaLongType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
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

public class LongByteValue extends UnaryExpression implements PluggableStatement {
	protected Statement originating;

	public LongByteValue(
			CFG cfg,
			CodeLocation location,
			Expression exp) {
		super(cfg, location, "byteValue", exp);
	}

	public static LongByteValue build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new LongByteValue(cfg, location, params[0]);
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
		Type longType = JavaLongType.INSTANCE;
		Type byteType = JavaByteType.INSTANCE;

		GlobalVariable var = new GlobalVariable(Untyped.INSTANCE, "value", getLocation());
		HeapDereference deref = new HeapDereference(longType, expr, getLocation());
		AccessChild accessExpr = new AccessChild(longType, deref, var, getLocation());

		it.unive.lisa.symbolic.value.UnaryExpression byteValue = new it.unive.lisa.symbolic.value.UnaryExpression(
				byteType,
				accessExpr,
				JavaLongByteValueOperator.INSTANCE,
				getLocation());

		return interprocedural.getAnalysis().smallStepSemantics(state, byteValue, originating);

	}
}

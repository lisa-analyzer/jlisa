package it.unive.jlisa.program.java.constructs.floatw;

import it.unive.jlisa.program.type.JavaFloatType;
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
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.type.Untyped;

public class FloatValue extends UnaryExpression implements PluggableStatement {
	protected Statement originating;

	public FloatValue(
			CFG cfg,
			CodeLocation location,
			Expression expr) {
		super(cfg, location, "floatValue", expr);
	}

	public static FloatValue build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new FloatValue(cfg, location, params[0]);
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
	public <A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> AnalysisState<A> fwdUnarySemantics(
					InterproceduralAnalysis<A, D> interprocedural,
					AnalysisState<A> state,
					SymbolicExpression expr,
					StatementStore<A> expressions)
					throws SemanticException {
		GlobalVariable var = new GlobalVariable(Untyped.INSTANCE, "value", getLocation());
		AccessChild access = new AccessChild(JavaFloatType.INSTANCE, expr, var, getLocation());
		return interprocedural.getAnalysis().smallStepSemantics(state, access, originating);
	}
}

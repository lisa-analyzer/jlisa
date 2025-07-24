package it.unive.jlisa.program.java.constructs.random;

import it.unive.jlisa.program.type.JavaFloatType;
import it.unive.jlisa.program.type.JavaIntType;
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
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.PushFromConstraints;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.type.Untyped;

public  class NextFloat extends UnaryExpression implements PluggableStatement {
	protected Statement originating;

	public NextFloat(CFG cfg, CodeLocation location, Expression param) {
		super(cfg, location, "nextFloat", param);
	}

	public static NextFloat build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new NextFloat(cfg, location, params[0]);
	}

	@Override
	protected int compareSameClassAndParams(Statement o) {
		return 0; 
	}


	@Override
	public <A extends AbstractLattice<A>,
		D extends AbstractDomain<A>> AnalysisState<A> fwdUnarySemantics(InterproceduralAnalysis<A, D> interprocedural, AnalysisState<A> state, SymbolicExpression expr, StatementStore<A> expressions) throws SemanticException {
		Constant zero = new Constant(JavaIntType.INSTANCE, 0, getLocation());
		Constant one = new Constant(JavaIntType.INSTANCE, 1, getLocation());
		Variable v = new Variable(JavaIntType.INSTANCE, "v", getLocation());
		// 0 <= v
		BinaryExpression const1 = new BinaryExpression(Untyped.INSTANCE, zero, v, ComparisonLe.INSTANCE, getLocation());
		// 1 >= v
		BinaryExpression const2 = new BinaryExpression(Untyped.INSTANCE, one, v, ComparisonGe.INSTANCE, getLocation());

		return interprocedural.getAnalysis().smallStepSemantics(state, new PushFromConstraints(JavaFloatType.INSTANCE, getLocation(), const1, const2), originating);
	}

	@Override
	public void setOriginatingStatement(Statement st) {
		originating = st;
	}
}

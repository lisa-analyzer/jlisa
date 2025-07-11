package it.unive.jlisa.program.java.constructs.random;

import it.unive.lisa.analysis.AbstractState;
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
import it.unive.lisa.symbolic.value.PushAny;

public class RandomEmptyConstructor extends UnaryExpression implements PluggableStatement {
	protected Statement originating;

	public RandomEmptyConstructor(CFG cfg, CodeLocation location, Expression param) {
		super(cfg, location, "Random", param);
	}

	public static RandomEmptyConstructor build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new RandomEmptyConstructor(cfg, location, params[0]);
	}

	@Override
	protected int compareSameClassAndParams(Statement o) {
		return 0; 
	}


	@Override
	public <A extends AbstractState<A>> AnalysisState<A> fwdUnarySemantics(InterproceduralAnalysis<A> interprocedural, AnalysisState<A> state, SymbolicExpression expr, StatementStore<A> expressions) throws SemanticException {
		return state.smallStepSemantics(new PushAny(getStaticType(), getLocation()), originating);
	}

	@Override
	public void setOriginatingStatement(Statement st) {
		originating = st;
	}

}

package it.unive.jlisa.program.cfg.expression;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.UnaryExpression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;

public class JavaNewArray extends UnaryExpression {

	private final Type type;

	public JavaNewArray(CFG cfg, CodeLocation location, Expression subExpression, Type type) {
		super(cfg, location, "new", subExpression);
		this.type = type;
	}

	@Override
	public <A extends AbstractState<A>> AnalysisState<A> fwdUnarySemantics(InterproceduralAnalysis<A> interprocedural,
			AnalysisState<A> state, SymbolicExpression expr, StatementStore<A> expressions) throws SemanticException {
		return state.top();
	}

	@Override
	protected int compareSameClassAndParams(Statement o) {
		return 0;
	}
	
	@Override
	public String toString() {
		return "new " +  type + "[" + getSubExpression() +"]"; 
	}
}

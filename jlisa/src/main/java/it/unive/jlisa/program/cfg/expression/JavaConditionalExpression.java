package it.unive.jlisa.program.cfg.expression;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;

/**
 * Conditional operator/expression
 * 
 * condition ? expr1 : expr2
 * 
 * https://docs.oracle.com/javase/tutorial/java/nutsandbolts/op2.html
 * 
 * @author <a href="mailto:luca.olivieri@unive.it">Luca Olivieri</a>
 */
public class JavaConditionalExpression extends it.unive.lisa.program.cfg.statement.TernaryExpression {

	/**
	 * Builds the construct.
	 * @param cfg     the cfg containing this expression
	 * @param location   the location where this construct is defined
	 * @param condition    the condition of conditional operator
	 * @param expr1 the expression of then
	 * @param expr2 the expression of else
	 */
	public JavaConditionalExpression(CFG cfg, CodeLocation location, Expression condition,
			Expression expr1, Expression expr2) {
		super(cfg, location, "?", condition, expr1, expr2);
	}

	@Override
	protected int compareSameClassAndParams(Statement o) {
		return 0;
	}

	@Override
	public <A extends AbstractState<A>> AnalysisState<A> fwdTernarySemantics(InterproceduralAnalysis<A> interprocedural,
			AnalysisState<A> state, SymbolicExpression left, SymbolicExpression middle, SymbolicExpression right,
			StatementStore<A> expressions) throws SemanticException {
		
		AnalysisState<A> result = state.smallStepSemantics(left, this);
		
		Satisfiability sat = result.satisfies(left, this);
		
		switch(sat) {
			case SATISFIED:
				return result.smallStepSemantics(middle, this);
			case NOT_SATISFIED:
				return result.smallStepSemantics(right, this);
			case BOTTOM:
				return result.bottom();
			case UNKNOWN:
			default:
				return result.smallStepSemantics(middle, this).lub(result.smallStepSemantics(right, this));
		}
		
	}


	
}

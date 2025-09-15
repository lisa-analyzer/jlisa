package it.unive.jlisa.program.cfg.expression;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
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
 * Conditional operator/expression condition ? expr1 : expr2
 * https://docs.oracle.com/javase/tutorial/java/nutsandbolts/op2.html
 * 
 * @author <a href="mailto:luca.olivieri@unive.it">Luca Olivieri</a>
 */
public class JavaConditionalExpression extends it.unive.lisa.program.cfg.statement.TernaryExpression {

	/**
	 * Builds the construct.
	 * 
	 * @param cfg       the cfg containing this expression
	 * @param location  the location where this construct is defined
	 * @param condition the condition of conditional operator
	 * @param expr1     the expression of then
	 * @param expr2     the expression of else
	 */
	public JavaConditionalExpression(
			CFG cfg,
			CodeLocation location,
			Expression condition,
			Expression expr1,
			Expression expr2) {
		super(cfg, location, "?", condition, expr1, expr2);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

	@Override
	public <A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> AnalysisState<A> fwdTernarySemantics(
					InterproceduralAnalysis<A, D> interprocedural,
					AnalysisState<A> state,
					SymbolicExpression left,
					SymbolicExpression middle,
					SymbolicExpression right,
					StatementStore<A> expressions)
					throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		AnalysisState<A> result = analysis.smallStepSemantics(state, left, this);
		Satisfiability sat = analysis.satisfies(result, left, this);

		switch (sat) {
		case SATISFIED:
			return analysis.smallStepSemantics(result, middle, this);
		case NOT_SATISFIED:
			return analysis.smallStepSemantics(result, right, this);
		case BOTTOM:
			return result.bottomExecution();
		case UNKNOWN:
		default:
			return analysis.smallStepSemantics(result, middle, this)
					.lub(analysis.smallStepSemantics(result, right, this));
		}

	}

}

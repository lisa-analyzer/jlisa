package it.unive.jlisa.program.cfg.expression;

import it.unive.jlisa.program.type.JavaBooleanType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;

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
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> forwardSemantics(
			AnalysisState<A> entryState,
			InterproceduralAnalysis<A, D> interprocedural,
			StatementStore<A> expressions)
			throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();
		Expression[] subExprs = getSubExpressions();
		CodeLocation location = getLocation();

		AnalysisState<A> condState = subExprs[0].forwardSemantics(entryState, interprocedural, expressions);
		expressions.put(subExprs[0], condState);

		Identifier ternaryMeta = new Variable(getStaticType(), "$ternary" + location, location);
		getMetaVariables().add(ternaryMeta);
		ProgramPoint value = new ValuePoint(this);

		AnalysisState<A> res = entryState.bottomExecution();
		for (SymbolicExpression cond : condState.getExecutionExpressions()) {

			Satisfiability sat = analysis.satisfies(entryState, cond, context, this);

			if (sat != Satisfiability.NOT_SATISFIED && sat != Satisfiability.BOTTOM) {
				AnalysisState<A> tmp = analysis.assume(condState, cond, context, value, value);
				AnalysisState<A> branchEvaluated = evalBranch(tmp, interprocedural, analysis, ternaryMeta, subExprs[1],
						expressions);
				res = res.lub(branchEvaluated);
			}

			if (sat != Satisfiability.SATISFIED && sat != Satisfiability.BOTTOM) {
				SymbolicExpression negation = new UnaryExpression(JavaBooleanType.INSTANCE, cond,
						LogicalNegation.INSTANCE, location);

				AnalysisState<A> tmp = analysis.assume(condState, negation, context, value, value);

				AnalysisState<A> branchEvaluated = evalBranch(tmp, interprocedural, analysis, ternaryMeta, subExprs[2],
						expressions);
				res = res.lub(branchEvaluated);
			}

		}

		for (Expression s : subExprs) {
			getMetaVariables().addAll(s.getMetaVariables());
			s.getMetaVariables().clear();
		}

		return analysis.smallStepSemantics(res, ternaryMeta, context, this);
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
		ScopeId<A> context = interprocedural.getCurrentContext();
		AnalysisState<A> result = analysis.smallStepSemantics(state, left, context, this);
		Satisfiability sat = analysis.satisfies(result, left, context, this);

		switch (sat) {
		case SATISFIED:
			return analysis.smallStepSemantics(result, middle, context, this);
		case NOT_SATISFIED:
			return analysis.smallStepSemantics(result, right, context, this);
		case BOTTOM:
			return result.bottomExecution();
		case UNKNOWN:
		default:
			// each value is computed where its condition holds, and kept in a
			// meta variable, since the two values belong to different states.
			// The condition is assumed at a program point that is not a
			// statement, so that reachability is not changed: the two values
			// are not branches of the control flow
			ProgramPoint value = new ValuePoint(this);
			Identifier meta = new Variable(getStaticType(), "ternary@" + getLocation(), getLocation());
			getMetaVariables().add(meta);
			SymbolicExpression negation = new UnaryExpression(BoolType.INSTANCE, left, LogicalNegation.INSTANCE,
					getLocation());
			AnalysisState<
					A> whenTrue = analysis.assign(analysis.assume(result, left, context, value, value), meta, middle,
							context, this);
			AnalysisState<A> whenFalse = analysis.assign(analysis.assume(result, negation, context, value, value), meta,
					right, context, this);
			return analysis.smallStepSemantics(whenTrue.lub(whenFalse), meta, context, this);
		}

	}

	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> evalBranch(
			AnalysisState<A> state,
			InterproceduralAnalysis<A, D> interprocedural,
			Analysis<A, D> analysis,
			Identifier meta,
			Expression branch,
			StatementStore<A> expressions)
			throws SemanticException {

		ScopeId<A> context = interprocedural.getCurrentContext();

		AnalysisState<A> evaluated = branch.forwardSemantics(state, interprocedural, expressions);
		expressions.put(branch, evaluated);

		AnalysisState<A> out = state.bottomExecution();
		for (SymbolicExpression s : evaluated.getExecutionExpressions()) {
			out = out.lub(analysis.assign(evaluated, meta, s, context, this));
		}
		return out;
	}

}

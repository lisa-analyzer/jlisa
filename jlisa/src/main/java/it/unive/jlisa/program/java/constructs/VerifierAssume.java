package it.unive.jlisa.program.java.constructs;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.FixpointInfo;
import it.unive.lisa.analysis.ProgramState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.ReachabilityProduct;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;

/**
 * A pluggable implementation of {@code Verifier.assume(boolean)} for SV-COMP
 * benchmarks. Unlike the standard {@link Assume} construct,
 * {@code VerifierAssume} treats the condition as a <em>model restriction</em>
 * (an input filter), not a conditional guard. This means that after the call
 * succeeds, the execution state is always considered <em>REACHABLE</em>: the
 * abstract interpreter narrows the value domain as usual, but the reachability
 * component is unconditionally restored to {@code REACHABLE} so that subsequent
 * assertions are not weakened by the unknown satisfiability of the assume
 * condition.
 */
public class VerifierAssume extends it.unive.lisa.program.cfg.statement.UnaryExpression
		implements
		PluggableStatement {

	/**
	 * The statement that originated this construct during call resolution.
	 */
	protected Statement originating;

	/**
	 * Builds a new {@code VerifierAssume} expression.
	 *
	 * @param cfg      the CFG that this expression belongs to
	 * @param location the source code location of this expression
	 * @param arg      the boolean expression passed to {@code Verifier.assume}
	 */
	public VerifierAssume(
			CFG cfg,
			CodeLocation location,
			Expression arg) {
		super(cfg, location, "verifier_assume", arg);
	}

	/**
	 * Factory method used by {@link it.unive.lisa.program.cfg.NativeCFG} to
	 * instantiate this construct from a call site.
	 *
	 * @param cfg      the CFG that this expression belongs to
	 * @param location the source code location of this expression
	 * @param params   the actual arguments; {@code params[0]} is the boolean
	 *                     condition
	 *
	 * @return a new {@code VerifierAssume} instance
	 */
	public static VerifierAssume build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new VerifierAssume(cfg, location, params[0]);
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
		AnalysisState<A> narrowed = interprocedural.getAnalysis().assume(
				state, expr, originating, originating);

		// If the condition is provably false the path is genuinely unreachable.
		if (narrowed.getExecutionState().isBottom())
			return narrowed;

		// Verifier.assume is a model restriction: all remaining paths are
		// still REACHABLE. Restore the reachability status so that downstream
		// assertions are not unnecessarily weakened.
		A exec = narrowed.getExecutionState();
		if (exec instanceof ReachabilityProduct) {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			A restored = (A) ((ReachabilityProduct) exec).setToReachable();
			ExpressionSet exprs = narrowed.getExecutionExpressions();
			FixpointInfo info = narrowed.getExecutionInformation();
			return narrowed.withExecution(new ProgramState<>(restored, exprs, info));
		}

		return narrowed;
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}
}

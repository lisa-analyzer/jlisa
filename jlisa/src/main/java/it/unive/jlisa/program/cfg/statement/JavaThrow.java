package it.unive.jlisa.program.cfg.statement;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalysisState.Error;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.Throw;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;

public class JavaThrow extends Throw {

	public JavaThrow(
			CFG cfg,
			CodeLocation location,
			Expression expression) {
		super(cfg, location, expression);
	}

	@Override
	public Statement withValue(
			Expression value) {
		return new JavaThrow(getCFG(), getLocation(), value);
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdUnarySemantics(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression expr,
			StatementStore<A> expressions)
			throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		Identifier meta = getMetaVariable();
		AnalysisState<A> sem = analysis.assign(state, meta, expr, this);
		// we forget the meta variables before moving as the operation only
		// affects the normal execution, and won't be effective after we
		// move the state to the exception
		sem = sem.forgetIdentifiers(getSubExpression().getMetaVariables(), this);
		AnalysisState<A> moved = analysis.moveExecutionToError(sem, new Error(expr.getStaticType(), this), this);
		return moved;
	}
}

package it.unive.jlisa.program.java.constructs.library;

import it.unive.jlisa.program.operator.JavaLibraryFunction;
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

/**
 * A static method of the Java library with one primitive parameter, whose
 * result is computed by a {@link JavaLibraryFunction}. Subclasses only choose
 * the function.
 */
public abstract class StaticUnaryFunction extends UnaryExpression implements PluggableStatement {
	protected Statement originating;

	private final JavaLibraryFunction function;

	protected StaticUnaryFunction(
			CFG cfg,
			CodeLocation location,
			JavaLibraryFunction function,
			Expression arg) {
		super(cfg, location, function.toString(), arg);
		this.function = function;
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
		it.unive.lisa.symbolic.value.UnaryExpression un = new it.unive.lisa.symbolic.value.UnaryExpression(
				function.getResultType(),
				expr,
				function,
				getLocation());
		return interprocedural.getAnalysis().smallStepSemantics(state, un, interprocedural.getCurrentContext(),
				originating);
	}
}

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
import it.unive.lisa.program.cfg.statement.BinaryExpression;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;

/**
 * A static method of the Java library with two primitive parameters, whose
 * result is computed by a {@link JavaLibraryFunction}. Subclasses only choose
 * the function.
 */
public abstract class StaticBinaryFunction extends BinaryExpression implements PluggableStatement {
	protected Statement originating;

	private final JavaLibraryFunction function;

	protected StaticBinaryFunction(
			CFG cfg,
			CodeLocation location,
			JavaLibraryFunction function,
			Expression left,
			Expression right) {
		super(cfg, location, function.toString(), left, right);
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
			D extends AbstractDomain<A>> AnalysisState<A> fwdBinarySemantics(
					InterproceduralAnalysis<A, D> interprocedural,
					AnalysisState<A> state,
					SymbolicExpression left,
					SymbolicExpression right,
					StatementStore<A> expressions)
					throws SemanticException {
		it.unive.lisa.symbolic.value.BinaryExpression bin = new it.unive.lisa.symbolic.value.BinaryExpression(
				function.getResultType(),
				left,
				right,
				function,
				getLocation());
		return interprocedural.getAnalysis().smallStepSemantics(state, bin, interprocedural.getCurrentContext(),
				originating);
	}
}

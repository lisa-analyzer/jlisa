package it.unive.jlisa.program.cfg.statement.asserts;

import it.unive.lisa.analysis.*;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.UnaryExpression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import it.unive.lisa.type.VoidType;
import java.util.Set;

/**
 * Simple assert statement in Java {@code assert Expression1} where
 * {@code Expression1} is a boolean expression
 * 
 * @link https://docs.oracle.com/javase/8/docs/technotes/guides/language/assert.html
 * 
 * @author <a href="mailto:luca.olivieri@unive.it">Luca Olivieri</a>
 */
public class SimpleAssert extends UnaryExpression implements AssertStatement {

	/**
	 * Builds the construct.
	 * 
	 * @param location   the location where this construct is defined
	 * @param program    the program of the analysis
	 * @param expression the assert's expression to hold
	 */
	public SimpleAssert(
			CFG cfg,
			CodeLocation loc,
			Expression expression) {
		super(cfg, loc, "assert", VoidType.INSTANCE, expression);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

	@Override
	public <A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> AnalysisState<A> fwdUnarySemantics(
					InterproceduralAnalysis<A, D> interprocedural,
					AnalysisState<A> state,
					SymbolicExpression expr,
					StatementStore<A> expressions)
					throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		if (analysis.getRuntimeTypesOf(state, expr, this).stream().noneMatch(Type::isBooleanType))
			return state.bottomExecution();

		return analysis.smallStepSemantics(
				state,
				new it.unive.lisa.symbolic.value.UnaryExpression(VoidType.INSTANCE, expr, new UnaryOperator() {
					@Override
					public Set<Type> typeInference(
							TypeSystem types,
							Set<Type> argument) {
						return Set.of(VoidType.INSTANCE);
					}

					public String toString() {
						return "assert";
					}
				}, getLocation()), this);
	}

	@Override
	public String toString() {
		return "assert " + getSubExpression().toString();
	}

}

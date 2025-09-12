package it.unive.jlisa.program.cfg.statement.asserts;

import it.unive.lisa.analysis.*;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.BinaryExpression;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import it.unive.lisa.type.VoidType;
import java.util.Set;

/**
 * Simple assert statement {@code assert Expression1 : Expression2} where
 * {@code Expression1} is a boolean expression; {@code Expression2} is an
 * expression that has a value. (It cannot be an invocation of a method that is
 * declared void.)
 * 
 * @link https://docs.oracle.com/javase/8/docs/technotes/guides/language/assert.html
 * 
 * @author <a href="mailto:luca.olivieri@unive.it">Luca Olivieri</a>
 */
public class AssertionStatement extends BinaryExpression implements AssertStatement {

	/**
	 * Builds the construct.
	 * 
	 * @param location   the location where this construct is defined
	 * @param program    the program of the analysis
	 * @param expression the assert's expression
	 * @param message    the message to print if the expression is not hold
	 */
	public AssertionStatement(
			CFG cfg,
			CodeLocation location,
			Expression expression,
			Expression message) {
		super(cfg, location, "assert", VoidType.INSTANCE, expression, message);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
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
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		AnalysisState<A> result = analysis.smallStepSemantics(
				state,
				new it.unive.lisa.symbolic.value.UnaryExpression(VoidType.INSTANCE, left, new UnaryOperator() {
					@Override
					public Set<Type> typeInference(
							TypeSystem types,
							Set<Type> argument) {
						return Set.of(VoidType.INSTANCE);
					}
				}, getLocation()), this);

		result = result.lub(analysis.smallStepSemantics(
				state,
				new it.unive.lisa.symbolic.value.UnaryExpression(VoidType.INSTANCE, right, new UnaryOperator() {
					@Override
					public Set<Type> typeInference(
							TypeSystem types,
							Set<Type> argument) {
						return Set.of(VoidType.INSTANCE);
					}
				}, getLocation()), this));

		return result;
	}

	public String toString() {
		return "assert " + getLeft() + " : " + getRight();
	}

}

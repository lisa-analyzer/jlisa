package it.unive.jlisa.program.cfg.statement.asserts;

import java.util.Set;

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
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import it.unive.lisa.type.VoidType;

/**
 * Simple assert statement in Java
 * 
 * {@code assert Expression1}
 * 
 * where {@code Expression1} is a boolean expression
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
	public SimpleAssert(CFG cfg, CodeLocation loc, Expression expression) {
		super(cfg, loc, "assert", VoidType.INSTANCE, expression);
	}

	@Override
	protected int compareSameClassAndParams(Statement o) {
		return 0;
	}

	@Override
	public <A extends AbstractState<A>> AnalysisState<A> fwdUnarySemantics(InterproceduralAnalysis<A> interprocedural,
			AnalysisState<A> state, SymbolicExpression expr, StatementStore<A> expressions) throws SemanticException {
		if (state.getState().getRuntimeTypesOf(expr, this, state.getState()).stream().noneMatch(Type::isBooleanType))
			return state.bottom();

		return state.smallStepSemantics(
				new it.unive.lisa.symbolic.value.UnaryExpression(VoidType.INSTANCE, expr, new UnaryOperator() {
					@Override
					public Set<Type> typeInference(TypeSystem types, Set<Type> argument) {
						return Set.of(VoidType.INSTANCE);
					}
				}, getLocation()), this);
	}

}

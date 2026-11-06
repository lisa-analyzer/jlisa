package it.unive.jlisa.program.cfg.expression;

import it.unive.jlisa.program.operator.JavaArithmeticOperators;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.numeric.Multiplication;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.type.Type;
import java.util.Set;

/**
 * Java's multiplication, that wraps around on overflow for integral types (see
 * {@link JavaArithmeticOperators}).
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class JavaMultiplication extends Multiplication {

	/**
	 * Builds the multiplication.
	 *
	 * @param cfg      the {@link CFG} where this operation lies
	 * @param location the location where this literal is defined
	 * @param left     the left-hand side of this operation
	 * @param right    the right-hand side of this operation
	 */
	public JavaMultiplication(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression right) {
		super(cfg, location, left, right);
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdBinarySemantics(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression left,
			SymbolicExpression right,
			StatementStore<A> expressions)
			throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();
		Set<Type> leftTypes = analysis.getRuntimeTypesOf(state, left, context, this);
		Set<Type> rightTypes = analysis.getRuntimeTypesOf(state, right, context, this);
		Type type = JavaArithmeticOperators.promote(leftTypes, rightTypes);
		if (type.isUntyped())
			return super.fwdBinarySemantics(interprocedural, state, left, right, expressions);

		return analysis.smallStepSemantics(
				state,
				new BinaryExpression(type, left, right, JavaArithmeticOperators.mul(type), getLocation()),
				context, this);
	}
}

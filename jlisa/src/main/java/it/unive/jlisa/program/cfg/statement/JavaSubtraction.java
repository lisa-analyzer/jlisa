package it.unive.jlisa.program.cfg.statement;

import it.unive.jlisa.program.operator.JavaArithmeticOperators;
import it.unive.lisa.analysis.*;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.Type;
import java.util.Set;

/**
 * An expression modeling the subtraction operation ({@code -}). Both operands'
 * types must be instances of {@link NumericType}. The type of this expression
 * is the common numerical type of its operands, according to the type
 * inference.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class JavaSubtraction
		extends
		it.unive.lisa.program.cfg.statement.BinaryExpression {

	/**
	 * Builds the subtraction.
	 *
	 * @param cfg      the {@link CFG} where this operation lies
	 * @param location the location where this literal is defined
	 * @param left     the left-hand side of this operation
	 * @param right    the right-hand side of this operation
	 */
	public JavaSubtraction(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression right) {
		super(cfg, location, "-", left.getStaticType().commonSupertype(right.getStaticType()), left, right);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0; // no extra fields to compare
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
		if (leftTypes.stream().noneMatch(Type::isNumericType))
			return state.bottomExecution();
		Set<Type> rightTypes = analysis.getRuntimeTypesOf(state, right, context, this);
		if (rightTypes.stream().noneMatch(Type::isNumericType))
			return state.bottomExecution();

		Type type = JavaArithmeticOperators.promote(leftTypes, rightTypes);
		if (type.isUntyped())
			type = getStaticType();
		return analysis.smallStepSemantics(
				state,
				new BinaryExpression(type, left, right, JavaArithmeticOperators.sub(type), getLocation()),
				context, this);
	}

}

package it.unive.jlisa.program.cfg.expression;

import it.unive.jlisa.program.JavaLocation;
import it.unive.jlisa.program.type.JavaBooleanType;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
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
import it.unive.lisa.type.Type;

/**
 * The unboxing of a {@code Boolean} used as a condition (e.g., of an
 * {@code if}, of a loop, of a conditional expression, or as an operand of a
 * logical operator): the condition is the wrapped {@code boolean} value (JLS
 * 5.1.8), not the reference.
 */
public class JavaUnboxBoolean extends UnaryExpression {

	/**
	 * Builds the unboxing.
	 *
	 * @param cfg           the cfg that this expression belongs to
	 * @param location      the location where this expression is defined
	 * @param subExpression the unboxed expression
	 */
	public JavaUnboxBoolean(
			CFG cfg,
			CodeLocation location,
			Expression subExpression) {
		super(cfg, location, "unbox", JavaBooleanType.INSTANCE, subExpression);
	}

	/**
	 * Yields the given expression, used as a condition, unboxed if its static
	 * type is {@code Boolean}.
	 *
	 * @param e the expression
	 *
	 * @return the condition
	 */
	public static Expression ifBoxed(
			Expression e) {
		if (e == null)
			return null;
		Type t = e.getStaticType();
		if (t instanceof JavaReferenceType ref && ref.getInnerType() instanceof JavaClassType inner
				&& inner.equals(JavaClassType.getBooleanWrapperType()))
			return new JavaUnboxBoolean(e.getCFG(), unboxed(e.getLocation()), e);
		return e;
	}

	private static CodeLocation unboxed(
			CodeLocation location) {
		if (!(location instanceof JavaLocation loc))
			return location;
		// same source position as the unboxed expression, but a different
		// location, as no two statements can share one
		return new JavaLocation(loc.getSourceFile(), loc.getLine(), loc.getCol(), loc.getEndLine(), loc.getEndCol()) {

			@Override
			public String getCodeLocation() {
				return super.getCodeLocation() + "(unboxed)";
			}
		};
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdUnarySemantics(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression expr,
			StatementStore<A> expressions)
			throws SemanticException {
		return interprocedural.getAnalysis().smallStepSemantics(state, JavaUnboxing.unbox(expr, getLocation()),
				interprocedural.getCurrentContext(), this);
	}

	@Override
	public String toString() {
		return getSubExpression().toString();
	}
}

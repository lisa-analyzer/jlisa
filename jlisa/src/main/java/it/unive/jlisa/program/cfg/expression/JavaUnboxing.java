package it.unive.jlisa.program.cfg.expression;

import it.unive.jlisa.program.type.JavaCharType;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

/**
 * Unboxing of the operands of the numerical comparisons of Java ({@code <},
 * {@code <=}, {@code >}, {@code >=}): operands that are references to wrapper
 * objects (e.g., {@code Long}) are compared through the primitive values they
 * wrap (JLS 15.20.1), never as references.
 */
final class JavaUnboxing {

	private JavaUnboxing() {
	}

	/**
	 * Yields the primitive value wrapped by the given expression, if it is a
	 * reference to a wrapper object, or the expression itself otherwise.
	 *
	 * @param e        the expression
	 * @param location the location of the unboxing
	 *
	 * @return the unboxed expression
	 */
	static SymbolicExpression unbox(
			SymbolicExpression e,
			CodeLocation location) {
		return unbox(e, null, location);
	}

	/**
	 * Yields the primitive value wrapped by the given expression, compared with
	 * the given other operand: if the expression is a reference to a wrapper
	 * object, its value; if it is a reference to an object of another class
	 * (e.g., an {@code Object} yielded by a collection) and the other operand
	 * is a primitive value, the value of the object as a primitive of the type
	 * of the other operand, as the object is unboxed when the comparison is
	 * executed (it must then be a wrapper object); the expression itself
	 * otherwise.
	 *
	 * @param e        the expression
	 * @param other    the other operand of the comparison (can be
	 *                     {@code null} if unknown)
	 * @param location the location of the unboxing
	 *
	 * @return the unboxed expression
	 */
	static SymbolicExpression unbox(
			SymbolicExpression e,
			SymbolicExpression other,
			CodeLocation location) {
		Type t = e.getStaticType();
		if (!(t instanceof JavaReferenceType jrt) || !(jrt.getInnerType() instanceof JavaClassType inner))
			return e;
		Type unwrapped;
		if (JavaClassType.isWrapperType(inner))
			unwrapped = JavaClassType.getUnwrappedType(inner);
		else if (other != null && isPrimitive(other.getStaticType()))
			unwrapped = other.getStaticType();
		else
			return e;
		GlobalVariable value = new GlobalVariable(Untyped.INSTANCE, "value", location);
		HeapDereference deref = new HeapDereference(inner, e, location);
		return new AccessChild(unwrapped, deref, value, location);
	}

	private static boolean isPrimitive(
			Type type) {
		return type.isNumericType() || type.isBooleanType() || type instanceof JavaCharType;
	}

	/**
	 * Yields the semantics of the numerical comparison with the given operator
	 * of the given operands, unboxing the operands that are wrapper objects, or
	 * {@code null} if no operand is a wrapper object (the comparison has then
	 * the usual semantics).
	 *
	 * @param <A>             the type of the abstract states
	 * @param <D>             the type of the domain
	 * @param interprocedural the interprocedural analysis
	 * @param state           the state where the comparison is evaluated
	 * @param left            the left operand
	 * @param right           the right operand
	 * @param operator        the comparison
	 * @param type            the static type of the comparison
	 * @param pp              the comparison
	 *
	 * @return the resulting state, or {@code null}
	 *
	 * @throws SemanticException if something goes wrong during the computation
	 */
	static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> compare(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression left,
			SymbolicExpression right,
			BinaryOperator operator,
			Type type,
			ProgramPoint pp)
			throws SemanticException {
		SymbolicExpression l = unbox(left, right, pp.getLocation());
		SymbolicExpression r = unbox(right, left, pp.getLocation());
		if (l == left && r == right)
			return null;
		return interprocedural.getAnalysis().smallStepSemantics(state,
				new BinaryExpression(type, l, r, operator, pp.getLocation()), interprocedural.getCurrentContext(),
				pp);
	}
}

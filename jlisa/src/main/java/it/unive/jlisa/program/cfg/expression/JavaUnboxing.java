package it.unive.jlisa.program.cfg.expression;

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
		Type t = e.getStaticType();
		if (t instanceof JavaReferenceType jrt && jrt.getInnerType() instanceof JavaClassType inner
				&& JavaClassType.isWrapperType(inner)) {
			GlobalVariable value = new GlobalVariable(Untyped.INSTANCE, "value", location);
			HeapDereference deref = new HeapDereference(inner, e, location);
			return new AccessChild(JavaClassType.getUnwrappedType(inner), deref, value, location);
		}
		return e;
	}

	/**
	 * Yields the semantics of the numerical comparison with the given
	 * operator of the given operands, unboxing the operands that are wrapper
	 * objects, or {@code null} if no operand is a wrapper object (the
	 * comparison has then the usual semantics).
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
	 * @throws SemanticException if something goes wrong during the
	 *                               computation
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
		SymbolicExpression l = unbox(left, pp.getLocation());
		SymbolicExpression r = unbox(right, pp.getLocation());
		if (l == left && r == right)
			return null;
		return interprocedural.getAnalysis().smallStepSemantics(state,
				new BinaryExpression(type, l, r, operator, pp.getLocation()), interprocedural.getCurrentContext(),
				pp);
	}
}

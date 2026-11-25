package it.unive.jlisa.program.java.constructs.stringbuilder;

import it.unive.lisa.symbolic.value.operator.binary.LogicalOr;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.jlisa.program.operator.JavaStringLengthOperator;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.program.type.JavaBooleanType;
import it.unive.jlisa.program.cfg.JavaExceptions;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.function.Function;

/**
 * Shared helper for the in-place {@code StringBuilder} mutators
 * ({@code insert}, {@code delete}, {@code deleteCharAt}, {@code reverse},
 * {@code setCharAt}, ...), all of which compute their receiver's new
 * {@code value} field from its own current content.
 */
final class StringBuilderMutationSupport {

	private StringBuilderMutationSupport() {
	}

	/**
	 * Mutates {@code receiver.value} in place, computing the new value from a
	 * snapshot of the old one rather than referencing {@code
	 * receiver.value} directly within its own assignment's RHS:
	 * self-referencing a heap field in its own assignment is unsound in this
	 * framework, since the heap domain applies the assignment's structural side
	 * effects on the target BEFORE rewriting the RHS, so an embedded reference
	 * to the same field would resolve against the already-updated heap state
	 * and evaluate to top.
	 */
	static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> mutateValue(
			Analysis<A, D> analysis,
			ScopeId<?> context,
			AnalysisState<A> state,
			SymbolicExpression receiver,
			Type stringType,
			CodeLocation location,
			ProgramPoint pp,
			Function<SymbolicExpression, SymbolicExpression> newValue)
			throws SemanticException {
		GlobalVariable var = new GlobalVariable(Untyped.INSTANCE, "value", location);
		HeapDereference deref = new HeapDereference(JavaClassType.lookup("java.lang.StringBuilder"), receiver,
				location);
		AccessChild currentAccess = new AccessChild(stringType, deref, var, location);
		AccessChild writeTarget = new AccessChild(stringType, receiver, var, location);

		Variable oldValue = new Variable(stringType, "old_value@" + location, location);
		AnalysisState<A> snapshot = analysis.assign(state, oldValue, currentAccess, context, pp);

		AnalysisState<A> result = analysis.assign(snapshot, writeTarget, newValue.apply(oldValue), context, pp);
		return result.forgetIdentifier(oldValue, pp);
	}

	/**
	 * Yields the length of the contents of the given builder.
	 *
	 * @param receiver   the builder
	 * @param stringType the type of strings
	 * @param location   the location
	 *
	 * @return the length
	 */
	static SymbolicExpression length(
			SymbolicExpression receiver,
			Type stringType,
			CodeLocation location) {
		HeapDereference deref = new HeapDereference(JavaClassType.lookup("java.lang.StringBuilder"), receiver,
				location);
		return new UnaryExpression(JavaIntType.INSTANCE,
				new AccessChild(stringType, deref, new GlobalVariable(Untyped.INSTANCE, "value", location), location),
				JavaStringLengthOperator.INSTANCE, location);
	}

	/**
	 * Raises {@code StringIndexOutOfBoundsException} exactly in the executions of
	 * the given state where the given index is out of the contents of the
	 * given builder (negative, or not smaller than their length; greater than
	 * it if {@code inclusive}, e.g. for the offset of {@code insert}),
	 * yielding the other executions.
	 *
	 * @param <A>        the kind of the abstract state
	 * @param <D>        the kind of the abstract domain
	 * @param raising    where the exception is raised
	 * @param state      the state
	 * @param receiver   the builder
	 * @param stringType the type of strings
	 * @param index      the index
	 * @param inclusive  whether the length is a valid index
	 * @param location   the location
	 *
	 * @return the executions where the index is within the bounds
	 *
	 * @throws SemanticException if the analysis fails
	 */
	static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> checkIndex(
			JavaExceptions.Raising<A, D> raising,
			AnalysisState<A> state,
			SymbolicExpression receiver,
			Type stringType,
			SymbolicExpression index,
			boolean inclusive,
			CodeLocation location)
			throws SemanticException {
		BinaryExpression outOfBounds = new BinaryExpression(JavaBooleanType.INSTANCE,
				new BinaryExpression(JavaBooleanType.INSTANCE, index, new Constant(JavaIntType.INSTANCE, 0, location),
						ComparisonLt.INSTANCE, location),
				new BinaryExpression(JavaBooleanType.INSTANCE, index, length(receiver, stringType, location),
						inclusive ? ComparisonGt.INSTANCE : ComparisonGe.INSTANCE, location),
				LogicalOr.INSTANCE, location);
		return raising.raiseWhere(outOfBounds, true, state, "java.lang.StringIndexOutOfBoundsException");
	}
}

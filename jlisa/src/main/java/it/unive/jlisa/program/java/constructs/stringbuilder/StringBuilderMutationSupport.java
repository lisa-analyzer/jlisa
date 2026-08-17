package it.unive.jlisa.program.java.constructs.stringbuilder;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
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
	 * Mutates {@code receiver.value} in place, computing the new value from
	 * a snapshot of the old one rather than referencing {@code
	 * receiver.value} directly within its own assignment's RHS:
	 * self-referencing a heap field in its own assignment is unsound in this
	 * framework, since the heap domain applies the assignment's structural
	 * side effects on the target BEFORE rewriting the RHS, so an embedded
	 * reference to the same field would resolve against the already-updated
	 * heap state and evaluate to top.
	 */
	static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> mutateValue(
			Analysis<A, D> analysis,
			AnalysisState<A> state,
			SymbolicExpression receiver,
			Type stringType,
			CodeLocation location,
			ProgramPoint pp,
			Function<SymbolicExpression, SymbolicExpression> newValue)
			throws SemanticException {
		GlobalVariable var = new GlobalVariable(Untyped.INSTANCE, "value", location);
		HeapDereference deref = new HeapDereference(stringType, receiver, location);
		AccessChild currentAccess = new AccessChild(stringType, deref, var, location);
		AccessChild writeTarget = new AccessChild(stringType, receiver, var, location);

		Variable oldValue = new Variable(stringType, "old_value@" + location, location);
		AnalysisState<A> snapshot = analysis.assign(state, oldValue, currentAccess, pp);

		AnalysisState<A> result = analysis.assign(snapshot, writeTarget, newValue.apply(oldValue), pp);
		return result.forgetIdentifier(oldValue, pp);
	}
}

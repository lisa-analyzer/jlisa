package it.unive.jlisa.analysis.heap;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import it.unive.jlisa.program.operator.NaryExpression;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.heap.pointbased.AllocationSiteBasedAnalysis;
import it.unive.lisa.analysis.heap.pointbased.FieldSensitivePointBasedHeap;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.heap.allocations.AllocationSite;
import it.unive.lisa.lattices.heap.allocations.HeapAllocationSite;
import it.unive.lisa.lattices.heap.allocations.HeapEnvWithFields;
import it.unive.lisa.lattices.heap.allocations.StackAllocationSite;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.MemoryPointer;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.type.Type;

/**
 * A field-insensitive program point-based {@link AllocationSiteBasedAnalysis}.
 * The implementation follows X. Rival and K. Yi, "Introduction to Static
 * Analysis An Abstract Interpretation Perspective", Section 8.3.4
 * 
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 * 
 * @see <a href=
 *          "https://mitpress.mit.edu/books/introduction-static-analysis">https://mitpress.mit.edu/books/introduction-static-analysis</a>
 */
public class JavaFieldSensitivePointBasedHeap
		extends
		FieldSensitivePointBasedHeap {

	@Override
	public ExpressionSet rewritePushAny(
			PushAny expression,
			HeapEnvWithFields state,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (expression.getStaticType().isPointerType()) {
			Type inner = expression.getStaticType().asPointerType().getInnerType();
			CodeLocation loc = expression.getCodeLocation();
			HeapAllocationSite site = new HeapAllocationSite(inner, "unknown@" + loc.getCodeLocation(), false, loc);
			return new ExpressionSet(new MemoryPointer(expression.getStaticType(), site, loc));
		}
		return new ExpressionSet(expression);
	}

	@Override
	public ExpressionSet rewriteValueExpression(
			ValueExpression expression,
			ExpressionSet[] subExpressions,
			HeapEnvWithFields state,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		Set<SymbolicExpression> result = new HashSet<>();
		SymbolicExpression[] res = new SymbolicExpression[subExpressions.length];
		for (int i = 0; i < subExpressions.length; ++i) {
			ExpressionSet set = subExpressions[i];
			for (SymbolicExpression expr : set) {
				res[i] = expr;
			}
		}
		NaryExpression e = new NaryExpression(
				expression.getStaticType(),
				res,
				((NaryExpression) expression).getOperator(),
				expression.getCodeLocation());
		result.add(e);
		return new ExpressionSet(result);
	}

	/* FIXME: this is cloned to change the behavior of process() */
	@Override
	public Pair<HeapEnvWithFields, List<HeapReplacement>> assign(
			HeapEnvWithFields state,
			Identifier id,
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (state.isBottom())
			return Pair.of(state, List.of());
		Pair<HeapEnvWithFields, List<HeapReplacement>> sss = smallStepSemantics(state, expression, pp, oracle);
		HeapEnvWithFields result = state.bottom();
		List<HeapReplacement> replacements = new LinkedList<>();
		sss.getRight().forEach(replacements::add);
		ExpressionSet rhsExps;
		boolean rhsIsReceiver = false;

		expression = expression.removeTypingExpressions();

		if (expression instanceof Identifier) {
			rhsExps = new ExpressionSet(resolveIdentifier(state, (Identifier) expression, pp));
			rhsIsReceiver = ((Identifier) expression).isInstrumentedReceiver();
		} else if (expression.mightNeedRewriting())
			rhsExps = rewrite(state, expression, pp, oracle);
		else
			rhsExps = new ExpressionSet(expression);

		for (SymbolicExpression rhs : rhsExps)
			result = result.lub(process(id, pp, oracle, sss.getLeft(), replacements, rhs, rhsIsReceiver));

		if (!id.isWeak() && state.knowsIdentifier(id)) {
			// we might make some location unreachable,
			// so we have to perform garbage collection
			HeapReplacement r = new HeapReplacement();
			r.addSource(id);
			replacements.addAll(state.expand(r));
		}

		return Pair.of(result, replacements);
	}

	private HeapEnvWithFields process(
			Identifier id,
			ProgramPoint pp,
			SemanticOracle oracle,
			HeapEnvWithFields sss,
			List<HeapReplacement> replacements,
			SymbolicExpression rhs,
			boolean rhsIsReceiver)
			throws SemanticException {
		if (rhs instanceof MemoryPointer) {
			if (!(((MemoryPointer) rhs).getReferencedLocation() instanceof AllocationSite))
				throw new SemanticException("Cannot assign a non-allocation site location");
			// we have x = y, where y is a pointer to an allocation site
			AllocationSite rhs_ref = (AllocationSite) ((MemoryPointer) rhs).getReferencedLocation();
			if (id instanceof MemoryPointer) {
				// we have x = y, where both are pointers
				// we perform *x = *y so that x and y become aliases
				Identifier lhs_ref = ((MemoryPointer) id).getReferencedLocation();
				return store(sss, lhs_ref, rhs_ref);
			} else if (rhs_ref instanceof StackAllocationSite
					// if we are allocating, we just perform normal aliasing
					// as there is nothing to copy
					&& !((StackAllocationSite) rhs_ref).isAllocation()
					// if rhs is an instrumented receiver, it corresponds to
					// something that is still on the stack while being
					// initialized (eg with a constructor call) so we
					// perform normal aliasing as there is nothing to copy
					&& !rhsIsReceiver
					&& !getAllocatedAt(sss, ((StackAllocationSite) rhs_ref).getLocationName()).isEmpty())
				// for stack elements, assignment works as a shallow copy
				// since there are no pointers to alias
				return shallowCopy(sss, id, (StackAllocationSite) rhs_ref, replacements);
			else {
				// aliasing: id and star_y points to the same object
				return store(sss, id, rhs_ref);
			}
		} else if (rhs instanceof AllocationSite) {
			// this whole branch is custom just for java
			// we have x = y, where y is a pointer to an allocation site
			AllocationSite rhs_ref = (AllocationSite) rhs;
			if (id instanceof MemoryPointer) {
				// we have x = y, where both are pointers
				// we perform *x = *y so that x and y become aliases
				Identifier lhs_ref = ((MemoryPointer) id).getReferencedLocation();
				return store(sss, lhs_ref, rhs_ref);
			} else if (rhs_ref instanceof StackAllocationSite
					// if we are allocating, we just perform normal aliasing
					// as there is nothing to copy
					&& !((StackAllocationSite) rhs_ref).isAllocation()
					// if rhs is an instrumented receiver, it corresponds to
					// something that is still on the stack while being
					// initialized (eg with a constructor call) so we
					// perform normal aliasing as there is nothing to copy
					&& !rhsIsReceiver
					&& !getAllocatedAt(sss, ((StackAllocationSite) rhs_ref).getLocationName()).isEmpty())
				// for stack elements, assignment works as a shallow copy
				// since there are no pointers to alias
				return shallowCopy(sss, id, (StackAllocationSite) rhs_ref, replacements);
			else {
				// aliasing: id and star_y points to the same object
				return store(sss, id, rhs_ref);
			}
		} else
			return sss;
	}
}

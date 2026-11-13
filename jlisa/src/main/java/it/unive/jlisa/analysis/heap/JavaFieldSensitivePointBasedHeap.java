package it.unive.jlisa.analysis.heap;

import it.unive.jlisa.program.operator.NaryExpression;
import it.unive.jlisa.type.JavaTypeSystem;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.heap.pointbased.AllocationSiteBasedAnalysis;
import it.unive.lisa.analysis.heap.pointbased.FieldSensitivePointBasedHeap;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.lattices.heap.allocations.AllocationSite;
import it.unive.lisa.lattices.heap.allocations.AllocationSites;
import it.unive.lisa.lattices.heap.allocations.HeapAllocationSite;
import it.unive.lisa.lattices.heap.allocations.HeapEnvWithFields;
import it.unive.lisa.lattices.heap.allocations.NullAllocationSite;
import it.unive.lisa.lattices.heap.allocations.StackAllocationSite;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.HeapLocation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.MemoryPointer;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.symbolic.value.operator.binary.LogicalAnd;
import it.unive.lisa.symbolic.value.operator.binary.LogicalOr;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.datastructures.trie.PatriciaTrieMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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

	/**
	 * Builds a context-insensitive, field-sensitive heap domain.
	 */
	public JavaFieldSensitivePointBasedHeap() {
		super();
	}

	/**
	 * Builds a field-sensitive heap domain.
	 *
	 * @param contextSensitive whether or not allocation sites should be
	 *                             qualified by the interprocedural context
	 *                             where the allocation happens
	 */
	public JavaFieldSensitivePointBasedHeap(
			boolean contextSensitive) {
		super(contextSensitive);
	}

	/**
	 * {@inheritDoc}<br>
	 * <br>
	 * The cells of arrays are named by the expressions of their indexes (e.g.,
	 * {@code a[i]} and {@code a[0]} are different cells, even if {@code i} is
	 * {@code 0}): a cell of an array of references that does not point to any
	 * location might be any cell written through another expression, or a cell
	 * never written, so it points to the locations of all the cells of the same
	 * array and to null. Without this, the cell would be taken as the object
	 * whose fields are accessed (see
	 * {@link FieldSensitivePointBasedHeap#rewriteAccessChild}), and an access
	 * to a cell of the array it points to would access a cell of the array
	 * containing it instead.
	 */
	@Override
	protected Set<SymbolicExpression> resolveIdentifier(
			HeapEnvWithFields state,
			Identifier v,
			ProgramPoint pp) {
		if (v instanceof AllocationSite && ((AllocationSite) v).getField() != null
				&& !state.getKeys().contains(v)) {
			AllocationSite cell = (AllocationSite) v;
			if (isArray(state, cell)) {
				Set<SymbolicExpression> result = new HashSet<>();
				for (Identifier other : state.getKeys())
					if (other instanceof AllocationSite && sameObject((AllocationSite) other, cell))
						result.addAll(super.resolveIdentifier(state, other, pp));
				// or a cell never written, which is null
				result.add(new MemoryPointer(pp.getProgram().getTypes().getReference(cell.getStaticType()),
						NullAllocationSite.INSTANCE, cell.getCodeLocation()));
				return result;
			}
		}
		return super.resolveIdentifier(state, v, pp);
	}

	// whether the given cell is a cell of an array, that is, its object (with
	// the same name and context, and no field) is an array
	private static boolean isArray(
			HeapEnvWithFields state,
			AllocationSite cell) {
		if (state.fields.function == null)
			return false;
		for (AllocationSite site : state.fields.getKeys())
			if (site.getField() == null && site.getStaticType().isArrayType()
					&& site.getLocationName().equals(cell.getLocationName())
					&& Objects.equals(site.getContext(), cell.getContext()))
				return true;
		return false;
	}

	// whether the given site is another cell (or field) of the object of the
	// given cell
	private static boolean sameObject(
			AllocationSite other,
			AllocationSite cell) {
		return other.getField() != null && other.getLocationName().equals(cell.getLocationName())
				&& Objects.equals(other.getContext(), cell.getContext());
	}

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

	@Override
	protected PatriciaTrieMap<AllocationSite, ExpressionSet> addField(
			AllocationSite site,
			SymbolicExpression field,
			PatriciaTrieMap<AllocationSite, ExpressionSet> mapping) {
		if (site.getField() != null)
			// we do not track fields of fields
			return mapping;
		Set<SymbolicExpression> tmp = new HashSet<>(mapping.getOrDefault(site, new ExpressionSet()).elements());
		tmp.add(field);
		return mapping.put(site, new ExpressionSet(tmp));
	}

	@Override
	protected HeapEnvWithFields process(
			HeapEnvWithFields sss,
			Identifier id,
			SymbolicExpression rhs,
			ProgramPoint pp,
			SemanticOracle oracle,
			List<HeapReplacement> replacements,
			boolean rhsIsReceiver)
			throws SemanticException {

		if (JavaTypeSystem.isPrimitive(id.getStaticType()) && JavaTypeSystem.isPrimitive(rhs.getStaticType()))
			return sss;

		if (rhs instanceof AllocationSite) {
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
					&& !getAllocatedAt(sss, rhs_ref.getLocationName(), rhs_ref.getContext()).isEmpty())
				// for stack elements, assignment works as a shallow copy
				// since there are no pointers to alias
				return shallowCopy(sss, id, (StackAllocationSite) rhs_ref, contextOf(oracle), replacements);
			else {
				// aliasing: id and star_y points to the same object
				if (!sss.knowsIdentifier(rhs_ref))
					return store(sss, id, rhs_ref);
				// if rhs_ref is a pointer, we dereference it so that id points
				// to the same destination
				AllocationSites state = sss.getState(rhs_ref);
				HeapEnvWithFields result = sss.bottom();
				for (AllocationSite site : state)
					result = result.lub(store(sss, id, site));
				return result;
			}
		} else
			return super.process(sss, id, rhs, pp, oracle, replacements, rhsIsReceiver);
	}

	/*
	 * FIXME: this is cloned from AllocationSiteBasedAnalysis to add indirection
	 * on equality checks.
	 */
	@Override
	public Satisfiability satisfies(
			HeapEnvWithFields state,
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (state.isTop())
			return Satisfiability.UNKNOWN;

		// negation
		if (expression instanceof UnaryExpression) {
			UnaryExpression un = (UnaryExpression) expression;
			if (un.getOperator() == LogicalNegation.INSTANCE)
				return satisfies(state, un.getExpression(), pp, oracle).negate();
		}

		if (expression instanceof BinaryExpression) {
			BinaryExpression bin = (BinaryExpression) expression;
			if (bin.getOperator() == LogicalAnd.INSTANCE)
				return satisfies(state, bin.getLeft(), pp, oracle).and(satisfies(state, bin.getRight(), pp, oracle));
			else if (bin.getOperator() == LogicalOr.INSTANCE)
				return satisfies(state, bin.getLeft(), pp, oracle).or(satisfies(state, bin.getRight(), pp, oracle));
			else if (bin.getOperator() == ComparisonNe.INSTANCE) {
				BinaryExpression negatedBin = new BinaryExpression(
						bin.getStaticType(),
						bin.getLeft(),
						bin.getRight(),
						ComparisonEq.INSTANCE,
						expression.getCodeLocation());
				return satisfies(state, negatedBin, pp, oracle).negate();
			} else if (bin.getOperator() == ComparisonEq.INSTANCE) {
				SymbolicExpression leftExpr = bin.getLeft();
				SymbolicExpression rightExpr = bin.getRight();
				if (!mightBeReference(leftExpr.getStaticType()) || !mightBeReference(rightExpr.getStaticType()))
					// a comparison between primitive values (e.g., two int
					// fields) is not about pointers: the rewriting of a field
					// of a possibly-null receiver contains null, that must not
					// be compared as a pointer
					return Satisfiability.UNKNOWN;

				ExpressionSet rhsExps;
				ExpressionSet lhsExps;
				if (leftExpr instanceof Identifier)
					lhsExps = new ExpressionSet(resolveIdentifier(state, (Identifier) leftExpr, pp));
				else if (expression.mightNeedRewriting())
					lhsExps = rewrite(state, leftExpr, pp, oracle);
				else
					lhsExps = new ExpressionSet(leftExpr);

				if (rightExpr instanceof Identifier)
					rhsExps = new ExpressionSet(resolveIdentifier(state, (Identifier) rightExpr, pp));
				else if (expression.mightNeedRewriting())
					rhsExps = rewrite(state, rightExpr, pp, oracle);
				else
					rhsExps = new ExpressionSet(rightExpr);

				Set<HeapLocation> lhsFiltered = new HashSet<>(), rhsFiltered = new HashSet<>();
				for (SymbolicExpression l : lhsExps) {
					if (l instanceof MemoryPointer)
						lhsFiltered.add(((MemoryPointer) l).getReferencedLocation());
					else if (l instanceof HeapLocation && state.knowsIdentifier((HeapLocation) l))
						// indirection without an explicit dereference
						// following the new semantics
						lhsFiltered.addAll(state.getState((HeapLocation) l).elements());
					else if (l instanceof NullAllocationSite)
						lhsFiltered.add((NullAllocationSite) l);
					else
						continue;
				}
				for (SymbolicExpression r : rhsExps) {
					if (r instanceof MemoryPointer)
						rhsFiltered.add(((MemoryPointer) r).getReferencedLocation());
					else if (r instanceof HeapLocation && state.knowsIdentifier((HeapLocation) r))
						// indirection without an explicit dereference
						// following the new semantics
						rhsFiltered.addAll(state.getState((HeapLocation) r).elements());
					else if (r instanceof NullAllocationSite)
						rhsFiltered.add((NullAllocationSite) r);
					else
						continue;
				}

				Satisfiability sat = Satisfiability.BOTTOM;
				for (HeapLocation lp : lhsFiltered)
					for (HeapLocation rp : rhsFiltered) {
						// left is null
						if (lp.equals(NullAllocationSite.INSTANCE))
							if (rp.equals(NullAllocationSite.INSTANCE))
								sat = sat.lub(Satisfiability.SATISFIED);
							else
								sat = sat.lub(Satisfiability.NOT_SATISFIED);
						// right is null
						else if (rp.equals(NullAllocationSite.INSTANCE))
							sat = sat.lub(Satisfiability.NOT_SATISFIED);

						// right is strong
						else if (!rp.isWeak())
							if (rp.equals(lp))
								sat = sat.lub(Satisfiability.SATISFIED);
							else if (!lp.isWeak())
								sat = sat.lub(Satisfiability.NOT_SATISFIED);
							else
								sat = sat.lub(Satisfiability.UNKNOWN);
						// left is strong
						else if (!lp.isWeak())
							if (rp.equals(lp))
								sat = sat.lub(Satisfiability.SATISFIED);
							else if (!rp.isWeak())
								sat = sat.lub(Satisfiability.NOT_SATISFIED);
							else
								sat = sat.lub(Satisfiability.UNKNOWN);
					}

				// FIXME: we may improve this check
				return sat != Satisfiability.BOTTOM ? sat : Satisfiability.UNKNOWN;
			}
		}

		return Satisfiability.UNKNOWN;
	}

	private static boolean mightBeReference(
			Type type) {
		return type.isPointerType() || type.isReferenceType() || type.isNullType() || type.isUntyped();
	}
}

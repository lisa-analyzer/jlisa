package it.unive.jlisa.analysis.heap;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import it.unive.jlisa.analysis.JavaNullConstant;
import it.unive.jlisa.program.type.JavaNullType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.heap.pointbased.AllocationSiteBasedAnalysis;
import it.unive.lisa.analysis.heap.pointbased.FieldSensitivePointBasedHeap;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.lattices.heap.allocations.HeapEnvWithFields;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.HeapExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.HeapLocation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.MemoryPointer;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;

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

	private final Rewriter rewriter = new Rewriter();

	@Override
	public ExpressionSet rewrite(
			HeapEnvWithFields state,
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
					throws SemanticException {
		return expression.accept(rewriter, state, pp);
	}

	// FIXME: this method can be removed with the new snapshot
	@Override
	public Pair<HeapEnvWithFields, List<HeapReplacement>> assign(HeapEnvWithFields state, Identifier id,
			SymbolicExpression expression, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
		if (state.isBottom())
			return Pair.of(state, List.of());
		return super.assign(state, id, expression, pp, oracle);
	}

	@Override
	public Pair<HeapEnvWithFields, List<HeapReplacement>> assume(HeapEnvWithFields state, SymbolicExpression expression,
			ProgramPoint src, ProgramPoint dest, SemanticOracle oracle) throws SemanticException {	
		Satisfiability sat = satisfies(state, expression, dest, oracle);
		if (sat == Satisfiability.SATISFIED || sat == Satisfiability.UNKNOWN)
			return Pair.of(state, List.of());
		else 
			return Pair.of(state.bottom(), List.of());		
	}

	@Override
	public Satisfiability satisfies(HeapEnvWithFields state, SymbolicExpression expression, ProgramPoint pp,
			SemanticOracle oracle) throws SemanticException {

		// negation
		if (expression instanceof UnaryExpression un) {
			if (un.getOperator() == LogicalNegation.INSTANCE)
				return satisfies(state, un.getExpression(), pp, oracle).negate();
		}

		if (expression instanceof BinaryExpression bin) {

			// !=
			if (bin.getOperator() == ComparisonNe.INSTANCE) {
				BinaryExpression negatedBin = new BinaryExpression(bin.getStaticType(), bin.getLeft(), bin.getRight(), ComparisonEq.INSTANCE, expression.getCodeLocation());
				return satisfies(state, negatedBin, pp, oracle).negate();
			}

			// ==
			if (bin.getOperator() == ComparisonEq.INSTANCE) {
				SymbolicExpression leftExpr = bin.getLeft();
				SymbolicExpression rightExpr = bin.getRight();

				ExpressionSet rhsExps;
				ExpressionSet lhsExps;
				if (leftExpr instanceof Identifier) {
					lhsExps = new ExpressionSet(resolveIdentifier(state, (Identifier) leftExpr, pp));
				} else if (expression.mightNeedRewriting())
					lhsExps = rewrite(state, leftExpr, pp, oracle);
				else
					lhsExps = new ExpressionSet(leftExpr);

				if (rightExpr instanceof Identifier) {
					rhsExps = new ExpressionSet(resolveIdentifier(state, (Identifier) rightExpr, pp));
				} else if (expression.mightNeedRewriting())
					rhsExps = rewrite(state, rightExpr, pp, oracle);
				else
					rhsExps = new ExpressionSet(rightExpr);

				Satisfiability sat = Satisfiability.BOTTOM;
				for (SymbolicExpression l : lhsExps) {
					for (SymbolicExpression r : rhsExps) {
						if (l instanceof MemoryPointer && r instanceof MemoryPointer) {
							HeapLocation lp = ((MemoryPointer) l).getReferencedLocation();
							HeapLocation rp = ((MemoryPointer) r).getReferencedLocation();

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
					}
				}
				
				// FIXME: we may improve this check
				return sat != Satisfiability.BOTTOM ? sat : super.satisfies(state, expression, pp, oracle);
			}
		}

		return super.satisfies(state, expression, pp, oracle);
	}

	/**
	 * A {@link it.unive.lisa.analysis.heap.BaseHeapDomain.Rewriter} for the
	 * {@link FieldSensitivePointBasedHeap} domain.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public class Rewriter
	extends
	FieldSensitivePointBasedHeap.Rewriter {

		// TODO: access child and dereference with null receiver

		@Override
		public ExpressionSet visit(HeapExpression expression, ExpressionSet[] subExpressions, Object... params)
				throws SemanticException {
			if (expression instanceof JavaNullConstant) {
				MemoryPointer mp = new MemoryPointer(
						new JavaReferenceType(JavaNullType.INSTANCE),
						NullAllocationSite.INSTANCE,
						NullAllocationSite.INSTANCE.getCodeLocation());
				return new ExpressionSet(mp);

			}
			else
				return super.visit(expression, subExpressions, params);
		}
	}
}

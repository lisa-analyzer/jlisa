package it.unive.jlisa.analysis;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

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
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;

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
		return expression.accept(rewriter, state);
	}


	@Override
	public Pair<HeapEnvWithFields, List<HeapReplacement>> assume(HeapEnvWithFields state, SymbolicExpression expression,
			ProgramPoint src, ProgramPoint dest, SemanticOracle oracle) throws SemanticException {
		if (expression instanceof BinaryExpression) {
			BinaryExpression bin = (BinaryExpression) expression;
			SymbolicExpression leftExpr = bin.getLeft();
			SymbolicExpression rightExpr = bin.getRight();

			ExpressionSet rhsExps;
			ExpressionSet lhsExps;
			if (leftExpr instanceof Identifier) {
				lhsExps = new ExpressionSet(resolveIdentifier(state, (Identifier) leftExpr));
			} else if (expression.mightNeedRewriting())
				lhsExps = rewrite(state, leftExpr, src, oracle);
			else
				lhsExps = new ExpressionSet(leftExpr);

			if (rightExpr instanceof Identifier) {
				rhsExps = new ExpressionSet(resolveIdentifier(state, (Identifier) rightExpr));
			} else if (expression.mightNeedRewriting())
				rhsExps = rewrite(state, rightExpr, src, oracle);
			else
				rhsExps = new ExpressionSet(rightExpr);


			for (SymbolicExpression l : lhsExps)
				for (SymbolicExpression r : rhsExps) {
					if (l instanceof MemoryPointer && r instanceof MemoryPointer) {
						HeapLocation lp = ((MemoryPointer) l).getReferencedLocation();
						HeapLocation rp = ((MemoryPointer) r).getReferencedLocation();
						
						// ==
						if (bin.getOperator() == ComparisonEq.INSTANCE)
							// left is null
							if (lp.equals(NullAllocationSite.INSTANCE))
								if (rp.equals(NullAllocationSite.INSTANCE))
									return Pair.of(state, List.of());
								else
									return Pair.of(state.bottom(), List.of());
							// right is null
							else if (rp.equals(NullAllocationSite.INSTANCE))
								return Pair.of(state.bottom(), List.of());
						
						// !=
						if (bin.getOperator() == ComparisonNe.INSTANCE)
							// left is null
							if (lp.equals(NullAllocationSite.INSTANCE))
								if (rp.equals(NullAllocationSite.INSTANCE))
									return Pair.of(state.bottom(), List.of());
								else
									return Pair.of(state, List.of());
							// right is null
							else if (rp.equals(NullAllocationSite.INSTANCE))
								return Pair.of(state, List.of());
					}
				}
		}

		return super.assume(state, expression, src, dest, oracle);
	}

	@Override
	public Satisfiability satisfies(HeapEnvWithFields state, SymbolicExpression expression, ProgramPoint pp,
			SemanticOracle oracle) throws SemanticException {
		if (expression instanceof BinaryExpression) {
			BinaryExpression bin = (BinaryExpression) expression;
			SymbolicExpression left = bin.getLeft();
			SymbolicExpression right = bin.getRight();
			System.err.println(left);
			System.err.println(right);

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

package it.unive.jlisa.analysis;

import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.heap.pointbased.AllocationSiteBasedAnalysis;
import it.unive.lisa.analysis.heap.pointbased.FieldSensitivePointBasedHeap;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.lattices.heap.allocations.HeapEnvWithFields;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.HeapExpression;
import it.unive.lisa.symbolic.value.MemoryPointer;

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

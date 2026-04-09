package it.unive.jlisa.analysis.heap;

import it.unive.jlisa.analysis.heap.pointbased.RecencyAbsFieldSensitivePointBasedHeap;
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
import it.unive.lisa.program.annotations.Annotation;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.MemoryAllocation;
import it.unive.lisa.symbolic.value.MemoryPointer;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.type.Type;
import java.util.HashSet;
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
public class JavaRecencyAbsFieldSensitivePointBasedHeap
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
		public ExpressionSet visit(
				PushAny expression,
				Object... params)
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
		public ExpressionSet visit(
				ValueExpression expression,
				ExpressionSet[] subExpressions,
				Object... params)
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
		public ExpressionSet visit(
				MemoryAllocation expression,
				Object... params)
				throws SemanticException {
			String pp = expression.getCodeLocation().getCodeLocation();
			HeapEnvWithFields state = (HeapEnvWithFields) params[0];

			boolean weak;
			
			Set<AllocationSite> st = getAllocatedAt(state, pp);
			
//			if(st.size() == 1) {
//				AllocationSite ns = null;
//				for(AllocationSite s : st) {
//					ns = s.toWeak();
//					st.remove(s);
//				}
//				st.add(ns);
//			} else {
//				
//			}
			
			if (!st.isEmpty())
				weak = true;
			else
				weak = false;

			weak = false;
			
			AllocationSite e;
			if (expression.isStackAllocation())
				e = new StackAllocationSite(expression.getStaticType(), pp, weak, expression.getCodeLocation());
			else
				e = new HeapAllocationSite(expression.getStaticType(), pp, weak, expression.getCodeLocation());
			e.setAllocation(true);

			// propagates the annotations of expression
			// to the newly created allocation site
			for (Annotation ann : expression.getAnnotations())
				e.getAnnotations().addAnnotation(ann);

			return new ExpressionSet(e);
		}
	}
}

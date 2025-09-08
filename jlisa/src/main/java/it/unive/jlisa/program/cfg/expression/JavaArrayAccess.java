package it.unive.jlisa.program.cfg.expression;

import it.unive.jlisa.analysis.JavaFieldSensitivePointBasedHeap;
import it.unive.jlisa.program.type.JavaArrayType;
import it.unive.jlisa.program.type.JavaBooleanType;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.continuations.Exception;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.heap.HeapLattice;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.lattices.heap.allocations.HeapEnvWithFields;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.BinaryExpression;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.type.Untyped;

public class JavaArrayAccess extends BinaryExpression {


	public JavaArrayAccess(CFG cfg, CodeLocation location, Expression left, Expression right) {
		super(cfg, location, "[]", left, right);
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdBinarySemantics(
			InterproceduralAnalysis<A, D> interprocedural, AnalysisState<A> state, SymbolicExpression left,
			SymbolicExpression right, StatementStore<A> expressions) throws SemanticException {		
        if (!left.getStaticType().isReferenceType() || !left.getStaticType().asReferenceType().getInnerType().isArrayType())
        	return state.bottom();
        
//		// need to check in-bound
        JavaArrayType arrayType = (JavaArrayType) ((JavaReferenceType) left.getStaticType()).getInnerType();
        HeapDereference container = new HeapDereference(arrayType, left, getLocation()); 		    	
//        Variable lenProperty = new Variable(JavaIntType.INSTANCE, "len", getLocation());
//		AccessChild lenAccess = new AccessChild(Untyped.INSTANCE, container, lenProperty, getLocation());
        Analysis<A, D> analysis = interprocedural.getAnalysis();        
//		it.unive.lisa.symbolic.value.BinaryExpression bin = new it.unive.lisa.symbolic.value.BinaryExpression(
//				JavaBooleanType.INSTANCE, right, lenAccess, ComparisonGe.INSTANCE, getLocation());
//
//		Satisfiability sat = analysis.satisfies(state, bin, this);
//		if (sat == Satisfiability.SATISFIED) {
//			// throw exception (definitely)
//			JavaClassType indexOob = JavaClassType.lookup("ArrayIndexOutOfBoundsException", null);
//			return analysis.moveExecutionToError(state, new Exception(indexOob, this));
//		} else if (sat == Satisfiability.NOT_SATISFIED) {
			AccessChild access = new AccessChild(arrayType.getInnerType(), container, right, getLocation());
			return analysis.smallStepSemantics(state, access, this);
//		} else {
//			JavaClassType indexOob = JavaClassType.lookup("ArrayIndexOutOfBoundsException", null);
//			AnalysisState<A> exceptionState = analysis.moveExecutionToError(state, new Exception(indexOob, this));
//			AccessChild access = new AccessChild(arrayType.getInnerType(), container, right, getLocation());
//			AnalysisState<A> noExceptionState = analysis.smallStepSemantics(state, access, this);
//			return noExceptionState.lub(exceptionState);
//		}
	}

	@Override
	protected int compareSameClassAndParams(Statement o) {
		return 0;
	}
	
	@Override
	public String toString() {
		return getLeft() + "[" + getRight() +"]";
	}
}

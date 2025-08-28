package it.unive.jlisa.program.cfg.expression;

import it.unive.jlisa.program.type.JavaArrayType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.BinaryExpression;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.type.ReferenceType;

public class JavaArrayAccess extends BinaryExpression {


	public JavaArrayAccess(CFG cfg, CodeLocation location, Expression left, Expression right) {
		super(cfg, location, "[]", left, right);
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdBinarySemantics(
			InterproceduralAnalysis<A, D> interprocedural, AnalysisState<A> state, SymbolicExpression left,
			SymbolicExpression right, StatementStore<A> expressions) throws SemanticException {
		AnalysisState<A> result = state.bottom();				
        Analysis<A, D> analysis = interprocedural.getAnalysis();
        if (!left.getStaticType().isReferenceType() || !left.getStaticType().asReferenceType().getInnerType().isArrayType())
        	return state.bottom();
        
        JavaArrayType arrayType = (JavaArrayType) ((ReferenceType) left.getStaticType()).getInnerType();
        HeapDereference container = new HeapDereference(arrayType, left, getLocation()); 		    	
        AccessChild access = new AccessChild(arrayType.getInnerType(), container, right, getLocation());
        result = result.lub(analysis.smallStepSemantics(state, access, getEvaluationPredecessor()));  		        	
		return result;
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

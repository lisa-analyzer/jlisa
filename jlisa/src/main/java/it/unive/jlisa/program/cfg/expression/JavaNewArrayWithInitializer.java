package it.unive.jlisa.program.cfg.expression;

import it.unive.jlisa.program.type.JavaArrayType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NaryExpression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.heap.MemoryAllocation;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.InstrumentedReceiver;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.ReferenceType;
import it.unive.lisa.type.Type;

public class JavaNewArrayWithInitializer extends NaryExpression{
	
	public JavaNewArrayWithInitializer(CFG cfg, CodeLocation location,
			Expression[] subExpressions, Type type) {
		super(cfg, location, "new", type, subExpressions);
	}

	@Override
	protected int compareSameClassAndParams(Statement o) {
		return 0;
	}
	
	@Override
	public String toString() {
		String params = "";
		Expression[] exprs = getSubExpressions();
		
		for(int i = 0; i < exprs.length; ++i) {
			
			params += exprs[i].toString();
			
			if( i < exprs.length - 1) {
				params += ", ";
			}
		}
		return "new " + getStaticType() + "{" +  params + "}"; 
	}

	@Override
	public <A extends AbstractLattice<A>,
		D extends AbstractDomain<A>> AnalysisState<A> forwardSemanticsAux(InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state, ExpressionSet[] params, StatementStore<A> expressions) throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ReferenceType refType = (ReferenceType) getStaticType();
		MemoryAllocation created = new MemoryAllocation(refType.getInnerType(), getLocation(), true);
		HeapReference ref = new HeapReference(refType, created, getLocation());

		AnalysisState<A> allocated = analysis.smallStepSemantics(state, created, this);

		InstrumentedReceiver array = new InstrumentedReceiver(refType, true, getLocation());

		AnalysisState<A> tmp = analysis.assign(allocated, array, ref, this);

		Type contentType = ((JavaArrayType) refType.getInnerType()).getInnerType();

		Variable lenProperty = new Variable(JavaIntType.INSTANCE, "len", getLocation());

		AccessChild lenAccess = new AccessChild(refType.getInnerType(), array, lenProperty, getLocation());

		Constant length = new Constant(JavaIntType.INSTANCE, params.length, getLocation());

		tmp = analysis.assign(tmp, lenAccess, length, getEvaluationPredecessor());

		int i = 0;
		
		for(ExpressionSet exprs : params){

			for (SymbolicExpression expr : exprs) {
				
				Variable var = new Variable(contentType, "" + i, getLocation());
				AccessChild access = new AccessChild(contentType, array, var, getLocation());
					
				AnalysisState<A> init = state.bottom();	
					
				init = init.lub(analysis.assign(tmp, access, expr, getEvaluationPredecessor()));
				
				tmp = init;
				
			}
			
			i += 1;
		} 

		getMetaVariables().add(array);

		return analysis.smallStepSemantics(tmp, array, this);
	}

}

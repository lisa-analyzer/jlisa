package it.unive.jlisa.program.cfg.expression;

import it.unive.jlisa.program.type.JavaArrayType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.UnaryExpression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.heap.MemoryAllocation;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.InstrumentedReceiver;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.ReferenceType;
import it.unive.lisa.type.Type;


public class JavaNewArray extends UnaryExpression {

	public JavaNewArray(CFG cfg, CodeLocation location, Expression subExpression, Type type) {
		super(cfg, location, "new", type, subExpression);
	}

	@Override
	public <A extends AbstractState<A>> AnalysisState<A> fwdUnarySemantics(InterproceduralAnalysis<A> interprocedural,
			AnalysisState<A> state, SymbolicExpression expr, StatementStore<A> expressions) throws SemanticException {	
		ReferenceType refType = (ReferenceType) getStaticType();
		MemoryAllocation created = new MemoryAllocation(refType.getInnerType(), getLocation(), true);
		HeapReference ref = new HeapReference(refType, created, getLocation());

		AnalysisState<A> allocated = state.smallStepSemantics(created, this);	

		InstrumentedReceiver array = new InstrumentedReceiver(refType, true, getLocation());

		AnalysisState<A> tmp = allocated.assign(array, ref, this);

		Type contentType = ((JavaArrayType) refType.getInnerType()).getInnerType();

		Variable lenProperty = new Variable(JavaIntType.INSTANCE, "len", getLocation());

		AccessChild lenAccess = new AccessChild(refType.getInnerType(), array, lenProperty, getLocation());

		tmp = tmp.assign(lenAccess, expr, getEvaluationPredecessor());
		
		if(!(expr instanceof Constant)) {
			throw new UnsupportedOperationException("For the moment we only support constant length arrays.");
		}
		
		for(int i = 0; i < (Integer)((Constant) expr).getValue(); i++) {
			Variable var = new Variable(JavaIntType.INSTANCE, "" + i, getLocation());
			AccessChild access = new AccessChild(contentType, array, var, getLocation());

			AnalysisState<A> tmp2 = contentType.defaultValue(getCFG(), getLocation()).forwardSemantics(tmp, interprocedural, expressions);
			AnalysisState<A> init = state.bottom();
			
			for(SymbolicExpression v : tmp2.getComputedExpressions()) {
				init = init.lub(tmp2.assign(access, v, getEvaluationPredecessor()));
			}
			
			tmp = init;
						
		} 

		getMetaVariables().add(array);
		
		return tmp.smallStepSemantics(array, this);
	}

	@Override
	protected int compareSameClassAndParams(Statement o) {
		return 0;
	}

	@Override
	public String toString() {
		return "new " + getStaticType() + "[" + getSubExpression() +"]"; 
	}
}

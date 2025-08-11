package it.unive.jlisa.program.cfg.statement.literal;

import it.unive.jlisa.program.type.JavaClassType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.InstrumentedReceiverRef;
import it.unive.lisa.program.cfg.statement.literal.Literal;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.heap.MemoryAllocation;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.type.ReferenceType;
import it.unive.lisa.type.Untyped;


public class JavaStringLiteral extends Literal<String> {
	public JavaStringLiteral(
			CFG cfg,
			CodeLocation location,
			String value) {
		super(cfg, location, value, new ReferenceType(JavaClassType.lookup("String", null)));
	}
	
	@Override
	public String toString() {
		return "\"" + getValue() + "\"";
	}

	public <A extends AbstractLattice<A>,
	D extends AbstractDomain<A>> AnalysisState<A> forwardSemantics(
			AnalysisState<A> entryState,
			InterproceduralAnalysis<A, D> interprocedural,
			StatementStore<A> expressions)
					throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		JavaClassType stringType = JavaClassType.lookup("String", null);
		ReferenceType reftype = (ReferenceType) new ReferenceType(stringType);
		MemoryAllocation created = new MemoryAllocation(reftype.getInnerType(), getLocation(), false);
		HeapReference ref = new HeapReference(reftype, created, getLocation());

		AnalysisState<A> allocated = analysis.smallStepSemantics(entryState, created, this);

		InstrumentedReceiverRef paramThis = new InstrumentedReceiverRef(getCFG(), getLocation(), false, reftype);

		// we also have to add the receiver inside the state
		AnalysisState<A> callstate = paramThis.forwardSemantics(allocated, interprocedural, expressions);

		// we store a reference to the newly created region in the receiver
		AnalysisState<A> tmp = entryState.bottom();
		for (SymbolicExpression rec : callstate.getComputedExpressions()) {
			GlobalVariable var = new GlobalVariable(Untyped.INSTANCE, "value", getLocation());
			AccessChild access = new AccessChild(stringType, ref, var, getLocation());
			AnalysisState<A> sem = interprocedural.getAnalysis().assign(callstate, access, new Constant(stringType, getValue(), getLocation()), this);
			tmp = tmp.lub(analysis.assign(sem, rec, ref, paramThis));
		}
		
		// we store the approximation of the receiver in the sub-expressions
		expressions.put(paramThis, tmp);

		// now remove the instrumented receiver
		expressions.forget(paramThis);
		for (SymbolicExpression v : callstate.getComputedExpressions())
			if (v instanceof Identifier)
				getMetaVariables().add((Identifier) v);

		return tmp;
	}
}

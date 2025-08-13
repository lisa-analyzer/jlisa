package it.unive.jlisa.program.java.constructs.string;

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
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.InstrumentedReceiverRef;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.UnaryExpression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.heap.MemoryAllocation;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.type.ReferenceType;
import it.unive.lisa.type.Untyped;


public class StringToString extends UnaryExpression implements PluggableStatement {
	protected Statement originating;

	public StringToString(CFG cfg, CodeLocation location, Expression exp) {
		super(cfg, location, "toString", exp);
	}

	public static StringToString build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new StringToString(cfg, location, params[0]);
	}

	@Override
	protected int compareSameClassAndParams(Statement o) {
		return 0; 
	}


	@Override
	public void setOriginatingStatement(Statement st) {
		originating = st;
	}


	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdUnarySemantics(
			InterproceduralAnalysis<A, D> interprocedural, AnalysisState<A> state, SymbolicExpression expr,
			StatementStore<A> expressions) throws SemanticException {
		JavaClassType stringType = JavaClassType.lookup("String", null);
		ReferenceType reftype = (ReferenceType) new ReferenceType(stringType);
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		
		GlobalVariable var = new GlobalVariable(Untyped.INSTANCE, "value", getLocation());
		HeapDereference deref = new HeapDereference(stringType, expr, getLocation());
		AccessChild accessExpr = new AccessChild(stringType, deref, var, getLocation());

		// allocate the string
		MemoryAllocation created = new MemoryAllocation(reftype.getInnerType(), getLocation(), false);
		HeapReference ref = new HeapReference(reftype, created, getLocation());
		AnalysisState<A> allocated = analysis.smallStepSemantics(state, created, this);

		InstrumentedReceiverRef paramThis = new InstrumentedReceiverRef(getCFG(), getLocation(), false, reftype);

		// we also have to add the receiver inside the state
		AnalysisState<A> callstate = paramThis.forwardSemantics(allocated, interprocedural, expressions);

		// we store a reference to the newly created region in the receiver
		AnalysisState<A> tmp = callstate.bottom();
		for (SymbolicExpression rec : callstate.getComputedExpressions()) {
			AccessChild access = new AccessChild(stringType, ref, var, getLocation());
			AnalysisState<A> sem = analysis.assign(callstate, access, accessExpr, this);
			tmp = tmp.lub(analysis.assign(sem, rec, ref, paramThis));
		}

		// we store the approximation of the receiver in the sub-expressions
		expressions.put(paramThis, tmp);

		// now remove the instrumented receiver
		expressions.forget(paramThis);
		for (SymbolicExpression v : callstate.getComputedExpressions())
			if (v instanceof Identifier)
				getMetaVariables().add((Identifier) v);

		// return tmp
		return new AnalysisState<>(
				tmp.getState(),
				callstate.getComputedExpressions(),
				tmp.getFixpointInformation());
	}
}
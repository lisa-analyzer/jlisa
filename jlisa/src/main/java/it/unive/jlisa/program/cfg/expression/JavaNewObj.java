package it.unive.jlisa.program.cfg.expression;

import it.unive.jlisa.frontend.InitializedClassSet;
import it.unive.jlisa.program.type.JavaReferenceType;
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
import it.unive.lisa.program.cfg.statement.InstrumentedReceiverRef;
import it.unive.lisa.program.cfg.statement.NaryExpression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.Call.CallType;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.heap.MemoryAllocation;
import it.unive.lisa.symbolic.value.Identifier;
import org.apache.commons.lang3.ArrayUtils;

public class JavaNewObj extends NaryExpression {

	/**
	 * Builds the object allocation and initialization.
	 *
	 * @param cfg        the {@link CFG} where this operation lies
	 * @param type       the type of the object that is being created
	 * @param parameters the parameters of the constructor call
	 */
	public JavaNewObj(
			CFG cfg,
			CodeLocation location,
			JavaReferenceType type,
			Expression... parameters) {
		super(cfg, location, type.getInnerType().toString(), type, parameters);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

	@Override
	public String toString() {
		return "new " + super.toString();
	}

	@Override
	public <A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> AnalysisState<A> forwardSemanticsAux(
					InterproceduralAnalysis<A, D> interprocedural,
					AnalysisState<A> state,
					ExpressionSet[] params,
					StatementStore<A> expressions)
					throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		JavaReferenceType reftype = (JavaReferenceType) getStaticType();
		String className = reftype.getInnerType().toString();
		String simpleName = className.contains(".")
				? className.substring(className.lastIndexOf(".") + 1)
				: className;

		state = InitializedClassSet.initialize(state, reftype, this, interprocedural);

		MemoryAllocation created = new MemoryAllocation(reftype.getInnerType(), getLocation(), false);
		HeapReference ref = new HeapReference(reftype, created, getLocation());

		AnalysisState<A> allocated = analysis.smallStepSemantics(state, created, this);

		// we need to add the receiver to the parameters
		InstrumentedReceiverRef paramThis = new InstrumentedReceiverRef(getCFG(), getLocation(), false, reftype);
		Expression[] fullExpressions = ArrayUtils.insert(0, getSubExpressions(), paramThis);

		// we also have to add the receiver inside the state
		AnalysisState<A> callstate = paramThis.forwardSemantics(allocated, interprocedural, expressions);
		ExpressionSet[] fullParams = ArrayUtils.insert(0, params, callstate.getExecutionExpressions());

		// we store a reference to the newly created region in the receiver
		AnalysisState<A> tmp = state.bottomExecution();
		for (SymbolicExpression rec : callstate.getExecutionExpressions())
			tmp = tmp.lub(analysis.assign(callstate, rec, ref, paramThis));
		// we store the approximation of the receiver in the sub-expressions
		expressions.put(paramThis, tmp);

		// constructor call
		String clazz = reftype.getInnerType().toString();
		UnresolvedCall call = new UnresolvedCall(
				getCFG(),
				getLocation(),
				CallType.INSTANCE,
				clazz,
				simpleName,
				fullExpressions);
		AnalysisState<A> sem = call.forwardSemanticsAux(interprocedural, tmp, fullParams, expressions);

		// now remove the instrumented receiver
		expressions.forget(paramThis);
		for (SymbolicExpression v : callstate.getExecutionExpressions())
			if (v instanceof Identifier)
				// we leave the instrumented receiver in the program variables
				// until it is popped from the stack to keep a reference to the
				// newly created object and its fields
				getMetaVariables().add((Identifier) v);

		// finally, we leave a reference to the newly created object on the
		// stack; this correponds to the state after the constructor call
		// but with the receiver left on the stack
		return sem.withExecutionExpressions(callstate.getExecutionExpressions());
	}
}
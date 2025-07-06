package it.unive.jlisa.program.cfg.expression;

import it.unive.jlisa.program.type.ReferenceTypeManager;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NaryExpression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.heap.MemoryAllocation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.type.ReferenceType;
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
            SourceCodeLocation location,
            String constructName,
            ReferenceType type,
            Expression... parameters) {
        super(cfg, location, constructName, type, parameters);
    }

    @Override
    protected int compareSameClassAndParams(
            Statement o) {
        return 0;
    }

    @Override
    public <A extends AbstractState<A>> AnalysisState<A> forwardSemanticsAux(
            InterproceduralAnalysis<A> interprocedural,
            AnalysisState<A> state,
            ExpressionSet[] params,
            StatementStore<A> expressions)
            throws SemanticException {
    	ReferenceType reftype = (ReferenceType) getStaticType();
        MemoryAllocation created = new MemoryAllocation(reftype.getInnerType(), getLocation(), false);
        HeapReference ref = new HeapReference(reftype, created, getLocation());

        // we need to add the receiver to the parameters
        VariableRef paramThis = new VariableRef(getCFG(), getLocation(), "this@" + getLocation(), reftype);
        Expression[] fullExpressions = ArrayUtils.insert(0, getSubExpressions(), paramThis);

        // we also have to add the receiver inside the state
        AnalysisState<A> callstate = state.smallStepSemantics(created, paramThis);
        callstate = paramThis.forwardSemantics(callstate, interprocedural, expressions);

        
        AnalysisState<A> tmp = state.bottom();
        for (SymbolicExpression v : callstate.getComputedExpressions())
            tmp = tmp.lub(callstate.assign(v, ref, paramThis));
        ExpressionSet[] fullParams = ArrayUtils.insert(0, params, callstate.getComputedExpressions());
        expressions.put(paramThis, tmp);

        UnresolvedCall call = new UnresolvedCall(getCFG(), getLocation(), Call.CallType.INSTANCE, reftype.getInnerType().toString(),
                getConstructName(), fullExpressions);
        AnalysisState<A> sem = call.forwardSemanticsAux(interprocedural, tmp, fullParams, expressions);

        if (!call.getMetaVariables().isEmpty())
            sem = sem.forgetIdentifiers(call.getMetaVariables());

        // now remove the instrumented receiver
        expressions.forget(paramThis);
        for (SymbolicExpression v : callstate.getComputedExpressions())
            if (v instanceof Identifier)
                sem = sem.forgetIdentifier((Identifier) v);

        sem = sem.smallStepSemantics(created, this);

        AnalysisState<A> result = state.bottom();
        for (SymbolicExpression loc : sem.getComputedExpressions()) {
            ReferenceType staticType;
            try {
                staticType = ReferenceTypeManager.get(loc.getStaticType());
            } catch (Exception e) {
                return result.bottom();
            }
            HeapReference locref = new HeapReference(staticType, loc, getLocation());
            result = result.lub(sem.smallStepSemantics(locref, call));
        }

        return result;
    }
}
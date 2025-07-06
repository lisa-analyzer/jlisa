package it.unive.jlisa.program.cfg.statement.global;

import java.util.HashSet;
import java.util.Set;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.UnaryExpression;
import it.unive.lisa.program.language.hierarchytraversal.HierarcyTraversalStrategy;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

public class JavaAccessGlobal extends UnaryExpression {

    private final String target;

    public JavaAccessGlobal(
            CFG cfg,
            CodeLocation location,
            Expression receiver,
            String target) {
        super(cfg, location, "::", Untyped.INSTANCE, receiver);
        this.target = target;
        receiver.setParentStatement(this);
    }

    /**
     * Yields the expression that determines the receiver of the global access
     * defined by this expression.
     *
     * @return the receiver of the access
     */
    public Expression getReceiver() {
        return getSubExpression();
    }

    /**
     * Yields the instance {@link Global} targeted by this expression.
     *
     * @return the global
     */
    public String getTarget() {
        return target;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((target == null) ? 0 : target.hashCode());
        return result;
    }

    @Override
    public boolean equals(
            Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        JavaAccessGlobal other = (JavaAccessGlobal) obj;
        if (target == null) {
            if (other.target != null)
                return false;
        } else if (!target.equals(other.target))
            return false;
        return true;
    }

    @Override
    protected int compareSameClassAndParams(
            Statement o) {
        JavaAccessGlobal other = (JavaAccessGlobal) o;
        return target.compareTo(other.target);
    }

    @Override
    public String toString() {
        return getSubExpression() + "::" + target;
    }

    @Override
    public <A extends AbstractState<A>> AnalysisState<A> fwdUnarySemantics(
            InterproceduralAnalysis<A> interprocedural,
            AnalysisState<A> state,
            SymbolicExpression expr,
            StatementStore<A> expressions)
            throws SemanticException {
        CodeLocation loc = getLocation();

        AnalysisState<A> result = state.bottom();
        boolean atLeastOne = false;
        Set<Type> types = state.getState().getRuntimeTypesOf(expr, this, state.getState());

        for (Type recType : types)
            if (recType.isPointerType()) {
                Type inner = recType.asPointerType().getInnerType();
                if (!inner.isUnitType())
                    continue;

                HeapDereference container = new HeapDereference(inner, expr, loc);
                CompilationUnit unit = inner.asUnitType().getUnit();

                Set<CompilationUnit> seen = new HashSet<>();
                HierarcyTraversalStrategy strategy = getProgram().getFeatures().getTraversalStrategy();
                for (CompilationUnit cu : strategy.traverse(this, unit))
                    if (seen.add(unit)) {
                        Global global = cu.getInstanceGlobal(target, false);
                        if (global != null) {
                            GlobalVariable var = global.toSymbolicVariable(loc);
                            AccessChild access = new AccessChild(var.getStaticType(), container, var, loc);
                            result = result.lub(state.smallStepSemantics(access, this));
                            atLeastOne = true;
                        }
                    }
            }

        if (atLeastOne)
            return result;

        // worst case: we are accessing a global that we know nothing about
        Set<Type> rectypes = new HashSet<>();
        for (Type t : types)
            if (t.isPointerType())
                rectypes.add(t.asPointerType().getInnerType());

        if (rectypes.isEmpty())
            return state.bottom();

        Type rectype = Type.commonSupertype(rectypes, Untyped.INSTANCE);
        GlobalVariable var = new GlobalVariable(Untyped.INSTANCE, target, new Annotations(), getLocation());
        HeapDereference container = new HeapDereference(rectype, expr, getLocation());
        AccessChild access = new AccessChild(Untyped.INSTANCE, container, var, getLocation());
        return state.smallStepSemantics(access, this);
    }
}

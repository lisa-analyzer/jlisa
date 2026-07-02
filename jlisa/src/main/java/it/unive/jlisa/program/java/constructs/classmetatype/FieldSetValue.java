package it.unive.jlisa.program.java.constructs.classmetatype;

import it.unive.jlisa.program.ReflectionCache;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.TernaryExpression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

public class FieldSetValue extends TernaryExpression implements PluggableStatement {
    protected Statement originating;

    public FieldSetValue(
            CFG cfg,
            CodeLocation location,
            Expression left,
            Expression middle,
            Expression right) {
        super(cfg, location, "set", left, middle, right);
    }

    public static FieldSetValue build(
            CFG cfg,
            CodeLocation location,
            Expression... params) {
        return new FieldSetValue(cfg, location, params[0], params[1], params[2]);
    }

    @Override
    public void setOriginatingStatement(Statement st) {
        originating = st;
    }

    @Override
    public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdTernarySemantics(
            InterproceduralAnalysis<A, D> interprocedural,
            AnalysisState<A> state,
            SymbolicExpression left,
            SymbolicExpression middle,
            SymbolicExpression right,
            StatementStore<A> expressions)
            throws SemanticException {

            return state;

        // Analysis<A, D> analysis = interprocedural.getAnalysis();
        //
        // // Reload reflection cache: evaluate Class name and Field name stored inside the Field meta-object
        // Type fieldMetaType = JavaClassType.getFieldMetaType();
        // Type classMetaType = JavaClassType.getClassMetaType();
        // Type objectType = JavaClassType.getObjectType();
        // Type stringType = getProgram().getTypes().getStringType();
        // CodeLocation loc = getLocation();
        //
        // // dereference the Field meta-object: (*field)
        // HeapDereference derefField = new HeapDereference(fieldMetaType, left, loc);
        //
        // // (*field)->clazz  (reference to Class meta-object)
        // GlobalVariable clazzVar = new GlobalVariable(Untyped.INSTANCE, "clazz", loc);
        // AccessChild accessClazzRef = new AccessChild(new JavaReferenceType(classMetaType), derefField, clazzVar, loc);
        //
        // // (*(*field)->clazz)
        // HeapDereference derefClazz = new HeapDereference(classMetaType, accessClazzRef, loc);
        //
        // // (*(*field)->clazz)->name  (actual class name string)
        // GlobalVariable clazzNameVar = new GlobalVariable(Untyped.INSTANCE, "name", loc);
        // AccessChild accessClazzName = new AccessChild(stringType, derefClazz, clazzNameVar, loc);
        //
        // // (*field)->name  (reference to String object)
        // GlobalVariable fieldNameVar = new GlobalVariable(Untyped.INSTANCE, "name", loc);
        // AccessChild accessFieldNameRef = new AccessChild(new JavaReferenceType(stringType), derefField,
        //     fieldNameVar, loc);
        //
        // // (*(*field)->name)->value  (actual field name constant)
        // HeapDereference derefFieldName = new HeapDereference(stringType, accessFieldNameRef, loc);
        // GlobalVariable fieldValueVar = new GlobalVariable(Untyped.INSTANCE, "value", loc);
        // AccessChild accessFieldName = new AccessChild(stringType, derefFieldName, fieldValueVar, loc);
        //
        // it.unive.lisa.symbolic.value.BinaryExpression isFieldDefined = new it.unive.lisa.symbolic.value.BinaryExpression(
        //         stringType,
        //         accessClazzName,
        //         accessFieldName,
        //         JavaIsFieldDefinedOperator.INSTANCE,
        //         loc);
        //
        // // Avoid stale cache values when resolution is unknown.
        // ReflectionCache.lastField = null;
        //
        // // force domain to evaluate the predicate so that ReflectionCache gets populated
        // Satisfiability sat = analysis.satisfies(state, isFieldDefined, originating);
        //
        // // if predicate unsat, we cannot resolve the field here
        // if (sat == Satisfiability.NOT_SATISFIED)
        //     return state.topExecution();
        //
        // Global field = ReflectionCache.lastField;
        //
        // if (field == null)
        //     return state.topExecution();
        // if (field.isInstance()) {
        //     HeapDereference container = new HeapDereference(objectType, middle, loc);
        //     GlobalVariable var = field.toSymbolicVariable(loc);
        //     AccessChild access = new AccessChild(field.getStaticType(), container, var, loc);
        //     return analysis.assign(state, access, right, this);
        // }
        //
        // GlobalVariable access = field.toSymbolicVariable(loc);
        // return analysis.assign(state, access, right, this);
    }

    @Override
    protected int compareSameClassAndParams(Statement o) {
        return 0;
    }
}

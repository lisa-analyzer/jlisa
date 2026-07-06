package it.unive.jlisa.program.java.constructs.classmetatype;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.Modifier;

import it.unive.jlisa.program.cfg.statement.JavaAssignment;
import it.unive.jlisa.program.type.JavaBooleanType;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.Reachability;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.SimpleAbstractDomain;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.analysis.value.ValueLattice;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.lattices.ReachabilityProduct;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.lattices.SimpleAbstractState;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.TernaryExpression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
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

        // left is the Field object;
        // middle is the object to set the field of;
        // right is the new value

        Analysis<A, D> analysis = interprocedural.getAnalysis();
        CodeLocation loc = getLocation();

        Type fieldMetaType = JavaClassType.getFieldMetaType();
        Type stringType = getProgram().getTypes().getStringType();
        JavaReferenceType refStringType = new JavaReferenceType(stringType);

        GlobalVariable nameVar = new GlobalVariable(Untyped.INSTANCE, "name", loc);
        GlobalVariable valueVar = new GlobalVariable(Untyped.INSTANCE, "value", loc);

        HeapDereference derefField = new HeapDereference(fieldMetaType, left, loc);
        AccessChild accessName = new AccessChild(refStringType, derefField, nameVar, loc);

        // access field name
        HeapDereference derefName = new HeapDereference(stringType, accessName, loc);
        AccessChild accessFieldNameValue = new AccessChild(refStringType, derefName, valueVar, loc);

        Set<BinaryExpression> constraints = getConstraints(analysis, state, accessFieldNameValue);

        Set<Type> targetTypes = analysis.getRuntimeTypesOf(state, middle, this);

        AnalysisState<A> result = state.bottomExecution();

        for (BinaryExpression fieldNameConstraint : constraints) {

            Satisfiability sat = isStaticField(interprocedural, state, derefField, expressions);

            if (sat == Satisfiability.SATISFIED) {
                // TODO
            }
            else if (sat == Satisfiability.NOT_SATISFIED) {
                // instance field
                String fieldName = (String) ((Constant)fieldNameConstraint.getLeft()).getValue();
                GlobalVariable fieldVar = new GlobalVariable(Untyped.INSTANCE, fieldName, loc);

                // for every target type
                for (Type targetType : targetTypes) {

                    // safety: the cast is safe since the targetType is always a subclass of Object
                    HeapDereference derefTarget = new HeapDereference((JavaReferenceType)targetType, middle, loc);

                    AccessChild access = new AccessChild(Untyped.INSTANCE, middle, fieldVar, loc);
                    Set<Type> targetFieldTypes = analysis.getRuntimeTypesOf(state, access, this);

                    for (Type targetFieldType : targetFieldTypes) {

                        access = new AccessChild(targetFieldType, derefTarget, fieldVar, loc);

                        // NOTE: this getMiddle() is wrong, it should be a FieldAccess
                        JavaAssignment assign = new JavaAssignment(getCFG(), loc, getMiddle(), getRight());

                        AnalysisState<A> t = assign.fwdBinarySemantics(interprocedural, state, access, right, expressions);

                        result = result.lub(t);
                    }
                }
            }
            else {
                result = state.topExecution();
            }
        }

        return result;
    }

    @Override
    protected int compareSameClassAndParams(Statement o) {
        return 0;
    }

    private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> Set<BinaryExpression> getConstraints(Analysis<A, D> analysis, AnalysisState<A> state, SymbolicExpression expr) {

            Set<BinaryExpression> constraints = new HashSet<>();

            try {
                    Class<?> c = Reachability.class;
                    Field f = c.getDeclaredField("domain");

                    f.setAccessible(true);

                    SimpleAbstractDomain<?, ?, ?> innerDomain = (SimpleAbstractDomain<?, ?, ?>) f.get(analysis.domain);

                    ValueDomain vdom = (ValueDomain) innerDomain.valueDomain;

                    Object executionState = state.getExecutionState();
                    ReachabilityProduct<?> reachabilityProduct = (ReachabilityProduct<?>) executionState;

                    SimpleAbstractState simpleAbstractState = (SimpleAbstractState) reachabilityProduct.second;

                    ValueLattice env = (ValueLattice) simpleAbstractState.valueState;

                    SemanticOracle oracle = innerDomain.makeOracle(simpleAbstractState);

                    ValueExpression ex = (ValueExpression) analysis.rewrite(state, expr, this).iterator().next();

                    constraints = vdom.constraints(null, env, ex, this, oracle);
            }
            catch (Exception e) {
            }

            return constraints;
    }

    private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> Satisfiability isStaticField(
            InterproceduralAnalysis<A, D> interprocedural,
            AnalysisState<A> state,
            HeapDereference derefField,
            StatementStore<A> expressions)
            throws SemanticException {

            Analysis<A, D> analysis = interprocedural.getAnalysis();
            CodeLocation location = getLocation();
            Constant staticModifier = new Constant(JavaIntType.INSTANCE, Modifier.STATIC, location);

            GlobalVariable modifiersVar = new GlobalVariable(JavaIntType.INSTANCE, "modifiers", location);
            AccessChild accessModifiers = new AccessChild(JavaIntType.INSTANCE, derefField, modifiersVar, location);

            it.unive.lisa.symbolic.value.BinaryExpression and =
                new it.unive.lisa.symbolic.value.BinaryExpression(
                        JavaIntType.INSTANCE,
                        accessModifiers,
                        staticModifier,
                        it.unive.lisa.symbolic.value.operator.binary.BitwiseAnd.INSTANCE,
                        location);

            AnalysisState<A> andState = analysis.smallStepSemantics(state, and, this);

            Satisfiability sat = Satisfiability.BOTTOM;

            for (SymbolicExpression expr : andState.getExecutionExpressions()) {
                it.unive.lisa.symbolic.value.BinaryExpression eq = new it.unive.lisa.symbolic.value.BinaryExpression( JavaBooleanType.INSTANCE,
                        staticModifier, expr, ComparisonEq.INSTANCE, location);

                sat = sat.lub(analysis.satisfies(state, eq, this));
            }

            return sat;
        }
}

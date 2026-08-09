package it.unive.jlisa.program;

import it.unive.jlisa.program.type.JavaArrayType;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.*;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

public class ReflectionDataUtils {

        public static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> boolean isClassLoaded(
                AnalysisState<A> state,
                Type t,
                CodeLocation location) {

                assert(t != null);
                String clazzName = t.toString();
                GlobalVariable clazzId = new GlobalVariable(Untyped.INSTANCE, "__"+t.toString(), location);
                return state.knowsIdentifier(clazzId);
        }

        public static <A extends AbstractLattice<A>, D extends AbstractDomain<A>> boolean isClassReflectionDataCached(
                InterproceduralAnalysis<A, D> interprocedural,
                AnalysisState<A> state,
                SymbolicExpression clazz,
                ProgramPoint pp) throws SemanticException {

                JavaReferenceType wrappedFieldType = new JavaReferenceType(JavaClassType.getFieldMetaType());
                JavaClassType classMetaType = JavaClassType.getClassMetaType();
                JavaArrayType fieldArrType = JavaArrayType.lookup(wrappedFieldType, 1);
                JavaReferenceType refFieldArrType = new JavaReferenceType(fieldArrType);

                if (clazz instanceof GlobalVariable)
                        clazz = new HeapDereference(classMetaType, clazz, pp.getLocation());

                GlobalVariable declaredFieldsVar = new GlobalVariable(Untyped.INSTANCE, "declaredFields", pp.getLocation());

                AccessChild accessFields = new AccessChild(new JavaReferenceType(fieldArrType), clazz, declaredFieldsVar,
                        pp.getLocation());

                Analysis<A, D> analysis = interprocedural.getAnalysis();
                Type type = analysis.getDynamicTypeOf(state, accessFields, pp);
                assert(type != null);
                // either null* or java.lang.reflect.Field*[]*
                assert(type instanceof JavaReferenceType);

                if (type instanceof JavaReferenceType jrt && jrt.getInnerType().isNullType()) {
                        return false;
                }
                return true;
        }

        public static SymbolicExpression getLoadedClassHandle(Type t, CodeLocation loc) {
                String s = "__" + t.toString();
                JavaReferenceType jrt = JavaClassType.getClassMetaType().getReference();
                return new GlobalVariable(jrt, s, loc);
        }
}

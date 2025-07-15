package it.unive.jlisa.program.java.constructs.object;

import it.unive.jlisa.program.type.JavaClassType;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.cfg.*;
import it.unive.lisa.program.cfg.statement.*;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.ReferenceType;
import it.unive.lisa.type.VoidType;

public class ObjectConstructor extends NativeCFG {

    public ObjectConstructor(
            CodeLocation location,
            ClassUnit stringUnit) {
        super(new CodeMemberDescriptor(location, stringUnit, true, "Object", VoidType.INSTANCE,
                        new Parameter(location, "this", new ReferenceType(JavaClassType.lookup("Object", null)))),
                ObjectConstructor.JavaObjectConstructor.class);
    }


    public static class JavaObjectConstructor extends UnaryExpression implements PluggableStatement {
        protected Statement originating;

        public JavaObjectConstructor(CFG cfg, CodeLocation location, Expression param) {
            super(cfg, location, "Object", param);
        }

        public static ObjectConstructor.JavaObjectConstructor build(
                CFG cfg,
                CodeLocation location,
                Expression... params) {
            return new  ObjectConstructor.JavaObjectConstructor(cfg, location, params[0]);
        }

        @Override
        protected int compareSameClassAndParams(Statement o) {
            return 0; // todo
        }


        @Override
        public <A extends AbstractState<A>> AnalysisState<A> fwdUnarySemantics(InterproceduralAnalysis<A> interprocedural, AnalysisState<A> state, SymbolicExpression expr, StatementStore<A> expressions) throws SemanticException {
            return state; // do nothing.
        }

        @Override
        public void setOriginatingStatement(Statement st) {
            originating = st;
        }

        }
    }

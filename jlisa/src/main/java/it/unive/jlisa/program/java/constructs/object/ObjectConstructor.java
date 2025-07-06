package it.unive.jlisa.program.java.constructs.object;

import it.unive.jlisa.program.type.JavaClassType;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.NativeCFG;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.UnaryExpression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.ReferenceType;

public class ObjectConstructor extends NativeCFG {

    public ObjectConstructor(
            CodeLocation location,
            ClassUnit stringUnit) {
        super(new CodeMemberDescriptor(location, stringUnit, true, "Object", new ReferenceType(JavaClassType.lookup("Object", null)),
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
            return state.smallStepSemantics(expr, originating);
        }

        @Override
        public void setOriginatingStatement(Statement st) {
            originating = st;
        }

        }
    }

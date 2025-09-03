package it.unive.jlisa.program.java.constructs.object;

import it.unive.jlisa.program.cfg.JavaCodeMemberDescriptor;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.NativeCFG;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.UnaryExpression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.VoidType;

public class ObjectConstructor extends NativeCFG {

    public ObjectConstructor(
            CodeLocation location,
            ClassUnit stringUnit) {
        super(new JavaCodeMemberDescriptor(location, stringUnit, true, "Object", VoidType.INSTANCE,
                        new Parameter(location, "this", new JavaReferenceType(JavaClassType.lookup("Object", null)))),
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
        public <A extends AbstractLattice<A>,
		D extends AbstractDomain<A>> AnalysisState<A> fwdUnarySemantics(InterproceduralAnalysis<A, D> interprocedural, AnalysisState<A> state, SymbolicExpression expr, StatementStore<A> expressions) throws SemanticException {
            return state; // do nothing.
        }

        @Override
        public void setOriginatingStatement(Statement st) {
            originating = st;
        }

        }
    }

package it.unive.jlisa.program.java.constructs.object;


import it.unive.jlisa.program.type.JavaIntType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.*;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.PushAny;

public class ObjectHashCode extends UnaryExpression implements PluggableStatement {
    protected Statement originating;

    public ObjectHashCode(
            CFG cfg,
            CodeLocation location,
            Expression expr) {
        super(cfg, location, "PushbackReader", expr);
    }

    public static it.unive.jlisa.program.java.constructs.reader.pushbackreader.PushbackReaderConstructorWithSize build(
            CFG cfg,
            CodeLocation location,
            Expression... params) {
        return new it.unive.jlisa.program.java.constructs.reader.pushbackreader.PushbackReaderConstructorWithSize(cfg, location, params[0], params[1], params[2]);
    }

    @Override
    protected int compareSameClassAndParams(
            Statement o) {
        return 0;
    }

    @Override
    public void setOriginatingStatement(
            Statement st) {
        originating = st;
    }

    @Override
    public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdUnarySemantics(InterproceduralAnalysis<A, D> interprocedural, AnalysisState<A> state, SymbolicExpression expr, StatementStore<A> expressions) throws SemanticException {
        return interprocedural.getAnalysis().smallStepSemantics(state, new PushAny(JavaIntType.INSTANCE, getLocation()),
                originating);
    }
}

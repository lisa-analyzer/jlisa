package it.unive.jlisa.program.cfg.expression;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.*;
import it.unive.lisa.symbolic.SymbolicExpression;

public class PrefixPlus extends UnaryExpression {
    public PrefixPlus(CFG cfg, CodeLocation location, Expression subExpression) {
        super(cfg, location, "+", subExpression);
    }

    @Override
    public <A extends AbstractState<A>> AnalysisState<A> fwdUnarySemantics(InterproceduralAnalysis<A> interprocedural, AnalysisState<A> state, SymbolicExpression expr, StatementStore<A> expressions) throws SemanticException {
        return state;
    }

    @Override
    protected int compareSameClassAndParams(Statement o) {
        return 0;
    }
    @Override
    public String toString() {
        return getConstructName() + getSubExpression();
    }
}

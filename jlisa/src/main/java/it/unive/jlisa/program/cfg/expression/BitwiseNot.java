package it.unive.jlisa.program.cfg.expression;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.UnaryExpression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.operator.unary.BitwiseNegation;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

public class BitwiseNot extends UnaryExpression {

    public BitwiseNot(
            CFG cfg,
            CodeLocation loc,
            Expression expression) {
        super(cfg, loc, "~", Untyped.INSTANCE, expression);
    }

    @Override
    protected int compareSameClassAndParams(
            Statement o) {
        return 0;
    }

    @Override
    public <A extends AbstractState<A>> AnalysisState<A> fwdUnarySemantics(
            InterproceduralAnalysis<A> interprocedural,
            AnalysisState<A> state,
            SymbolicExpression expr,
            StatementStore<A> expressions)
            throws SemanticException {
        if (state.getState().getRuntimeTypesOf(expr, this, state.getState()).stream().noneMatch(Type::isNumericType))
            return state.bottom();
        //TODO: check that the type is numeric and integral
        return state.smallStepSemantics(
                new it.unive.lisa.symbolic.value.UnaryExpression(
                        Untyped.INSTANCE,
                        expr,
                        BitwiseNegation.INSTANCE,
                        getLocation()),
                this);
    }
}

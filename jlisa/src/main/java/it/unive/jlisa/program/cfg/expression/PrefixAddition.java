package it.unive.jlisa.program.cfg.expression;

import it.unive.jlisa.program.type.IntType;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.*;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingAdd;
import it.unive.lisa.type.Type;

public class PrefixAddition extends UnaryExpression implements MetaVariableCreator {
    public PrefixAddition(CFG cfg, CodeLocation location, Expression subExpression) {
        super(cfg, location, "++", subExpression);
    }

    @Override
    public Identifier getMetaVariable() {
        Expression e = getSubExpression();
        String name = "ret_value@" + this.getLocation();
        Variable var = new Variable(e.getStaticType(), name, getLocation());
        return var;
    }

    @Override
    public <A extends AbstractState<A>> AnalysisState<A> fwdUnarySemantics(InterproceduralAnalysis<A> interprocedural, AnalysisState<A> state, SymbolicExpression expr, StatementStore<A> expressions) throws SemanticException {
        if (state.getState().getRuntimeTypesOf(expr, this, state.getState()).stream().noneMatch(Type::isNumericType))
            return state.bottom();

        Identifier meta = getMetaVariable();
        state = state.assign(meta, expr, this);
        state = state.assign(
                expr,
                new BinaryExpression(
                        getStaticType(),
                        expr,
                        new Constant(IntType.INSTANCE, 1, getLocation()),
                        NumericNonOverflowingAdd.INSTANCE,
                        getLocation()),
                this);
        return state.smallStepSemantics(meta, this);

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

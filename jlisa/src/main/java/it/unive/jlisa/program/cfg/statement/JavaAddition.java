package it.unive.jlisa.program.cfg.statement;

import it.unive.jlisa.program.type.JavaByteType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.program.type.JavaShortType;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.type.StringType;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingAdd;
import it.unive.lisa.symbolic.value.operator.binary.StringConcat;
import it.unive.lisa.symbolic.value.operator.binary.TypeConv;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeTokenType;
import it.unive.lisa.type.Untyped;

import java.util.Collections;
import java.util.Set;

public class JavaAddition extends it.unive.lisa.program.cfg.statement.BinaryExpression {

    public JavaAddition(
            CFG cfg,
            CodeLocation location,
            Expression left,
            Expression right) {
        super(cfg, location, "+", inferType(left, right), left, right);
    }

    private static Type inferType(Expression left, Expression right) {
        Type leftType = left.getStaticType();
        Type rightType = right.getStaticType();

        if (!(leftType.isNumericType() || leftType.isStringType() || rightType.isNumericType() || rightType.isStringType())) {
            return Untyped.INSTANCE;
        }
        if (leftType.isStringType() || rightType.isStringType()) {
            return StringType.INSTANCE;
        }
        if (leftType.isNumericType() && rightType.isNumericType()) {
            // small types promoted to int for addition operation
            if (leftType instanceof JavaByteType || leftType instanceof JavaShortType) {
                leftType = JavaIntType.INSTANCE;
            }
            if (rightType instanceof JavaByteType || rightType instanceof JavaShortType) {
                rightType = JavaIntType.INSTANCE;
            }
            return leftType.commonSupertype(rightType);
        }

        return Untyped.INSTANCE;
    }

    @Override
    public <A extends AbstractState<A>> AnalysisState<A> fwdBinarySemantics(
            InterproceduralAnalysis<A> interprocedural,
            AnalysisState<A> state,
            SymbolicExpression left,
            SymbolicExpression right,
            StatementStore<A> expressions)
            throws SemanticException {
        Set<Type> leftTypes = state.getState().getRuntimeTypesOf(left, this, state.getState());
        Set<Type> rightTypes = state.getState().getRuntimeTypesOf(right, this, state.getState());
        Constant typeCast = new Constant(new TypeTokenType(Collections.singleton(getStaticType())), getStaticType(), this.getLocation());
        SymbolicExpression actualLeft = left;
        SymbolicExpression actualRight = right;
        if (leftTypes.stream().anyMatch(Type::isStringType) || rightTypes.stream().anyMatch(Type::isStringType)) {
            if (!left.getStaticType().equals(getStaticType())) {
                actualLeft =  new BinaryExpression(getStaticType(), left, typeCast, TypeConv.INSTANCE, this.getLocation());
            }
            if (!right.getStaticType().equals(getStaticType())) {
                actualRight =  new BinaryExpression(getStaticType(), right, typeCast, TypeConv.INSTANCE, this.getLocation());
            }
            return state.smallStepSemantics(
                    new BinaryExpression(
                            getStaticType(),
                            actualLeft,
                            actualRight,
                            StringConcat.INSTANCE,
                            getLocation()),
                    this);
        }
        if (leftTypes.stream().noneMatch(Type::isNumericType) || rightTypes.stream().noneMatch(Type::isNumericType)) {
            return state.bottom();
        }

        if (!left.getStaticType().equals(getStaticType())) {
            actualLeft =  new BinaryExpression(getStaticType(), left, typeCast, TypeConv.INSTANCE, this.getLocation());
        }
        if (!right.getStaticType().equals(getStaticType())) {
            actualRight =  new BinaryExpression(getStaticType(), right, typeCast, TypeConv.INSTANCE, this.getLocation());
        }
        return state.smallStepSemantics(
                new BinaryExpression(
                        getStaticType(),
                        actualLeft,
                        actualRight,
                        NumericNonOverflowingAdd.INSTANCE,
                        getLocation()),
                this);
    }

    @Override
    protected int compareSameClassAndParams(Statement o) {
        return 0;
    }
}

package it.unive.jlisa.program.cfg.statement;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Assignment;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.evaluation.RightToLeftEvaluation;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.binary.TypeConv;
import it.unive.lisa.type.TypeTokenType;

import java.util.Collections;

public class JavaAssignment extends Assignment {

    public JavaAssignment(
            CFG cfg,
            CodeLocation location,
            Expression target,
            Expression expression) {
        super(cfg, location, RightToLeftEvaluation.INSTANCE, target.getStaticType(), target, expression);
    }

    @Override
    public <A extends AbstractState<A>> AnalysisState<A> fwdBinarySemantics(
            InterproceduralAnalysis<A> interprocedural,
            AnalysisState<A> state,
            SymbolicExpression left,
            SymbolicExpression right,
            StatementStore<A> expressions)
            throws SemanticException {
        CodeLocation loc = getLocation();
        if (!right.getStaticType().equals(left.getStaticType())) {
        	if (right.getStaticType().isUntyped()) {
                return super.fwdBinarySemantics(interprocedural, state, left, right, expressions);
            } else if (right.getStaticType().canBeAssignedTo(left.getStaticType())) {
                 Constant typeCast = new Constant(new TypeTokenType(Collections.singleton(left.getStaticType())), left.getStaticType(), loc);

                 BinaryExpression castExpression =  new BinaryExpression(left.getStaticType(), right, typeCast, TypeConv.INSTANCE, loc);
                 return super.fwdBinarySemantics(interprocedural, state, left, castExpression, expressions);
             } else {
                return state.bottom();
            }
        }

        return super.fwdBinarySemantics(interprocedural, state, left, right, expressions);
    }
}

package it.unive.jlisa.program.java.constructs.charsequence;

import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.operator.JavaStringCharAtOperator;
import it.unive.jlisa.program.operator.JavaStringLengthOperator;
import it.unive.jlisa.program.type.*;
import it.unive.lisa.analysis.*;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.BinaryExpression;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.CFGThrow;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.binary.LogicalOr;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

public class CharSequenceLength extends BinaryExpression implements PluggableStatement {
    protected Statement originating;

    public CharSequenceLength(
            CFG cfg,
            CodeLocation location,
            Expression left,
            Expression right) {
        super(cfg, location, "length", left, right);
    }

    public static it.unive.jlisa.program.java.constructs.charsequence.CharSequenceLength build(
            CFG cfg,
            CodeLocation location,
            Expression... params) {
        return new it.unive.jlisa.program.java.constructs.charsequence.CharSequenceLength(cfg, location, params[0], params[1]);
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
    public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdBinarySemantics(
            InterproceduralAnalysis<A, D> interprocedural,
            AnalysisState<A> state,
            SymbolicExpression left,
            SymbolicExpression right,
            StatementStore<A> expressions)
            throws SemanticException {
        return interprocedural.getAnalysis().smallStepSemantics(state, new PushAny(JavaIntType.INSTANCE, getLocation()),
                originating);
    }
}

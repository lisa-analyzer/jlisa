package it.unive.jlisa.program.cfg.statement;

import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.type.*;
import it.unive.jlisa.type.JavaTypeSystem;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Assignment;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Return;
import it.unive.lisa.program.cfg.statement.evaluation.RightToLeftEvaluation;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.value.*;
import it.unive.lisa.symbolic.value.operator.binary.TypeCast;
import it.unive.lisa.symbolic.value.operator.binary.TypeConv;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeTokenType;
import it.unive.lisa.type.Untyped;
import java.util.Collections;
import java.util.Set;

public class JavaReturn extends Return {

        private final JavaAssignment conversion;

        public JavaReturn(
                CFG cfg,
                CodeLocation location,
                Expression expression) {
                super(cfg, location, expression);
                // NOTE: in this case, the lhs of the assignment is a dummy that is never actually used by the semantics
                this.conversion = new JavaAssignment(cfg, location, expression, expression);
        }

        private Identifier getReturnMetaVariable() {
                CFG cfg = getCFG();
                return new CFGReturn(cfg, cfg.getDescriptor().getReturnType(), getLocation());
        }

        @Override
        public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdUnarySemantics(
                InterproceduralAnalysis<A, D> interprocedural,
                AnalysisState<A> state,
                SymbolicExpression expr,
                StatementStore<A> expressions)
                throws SemanticException {
                Identifier meta = getReturnMetaVariable();
                return conversion.fwdBinarySemantics(interprocedural, state, meta, expr, expressions);
        }

}


package it.unive.jlisa.program.java.constructs.string.constructors;

import it.unive.jlisa.program.cfg.statement.JavaAssignment;
import it.unive.jlisa.program.cfg.statement.global.JavaAccessGlobal;
import it.unive.jlisa.program.cfg.statement.literal.IntLiteral;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.cfg.*;
import it.unive.lisa.program.cfg.statement.*;
import it.unive.lisa.program.cfg.statement.literal.StringLiteral;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.ReferenceType;

public class StringEmptyConstructor extends NativeCFG {

    public StringEmptyConstructor(
            CodeLocation location,
            ClassUnit classUnit,
            ReferenceType referenceType) {
        super(new CodeMemberDescriptor(location, classUnit, true, "String", referenceType,
                        new Parameter(location, "this", referenceType)),
                StringEmptyConstructor.StringEmptyConstructorStmt.class);
    }

    public static class StringEmptyConstructorStmt extends UnaryExpression implements PluggableStatement {
        protected Statement statement;

        protected StringEmptyConstructorStmt(CFG cfg, CodeLocation location, Expression expression) {
            super(cfg, location, "String", expression);
        }

        public static StringEmptyConstructor.StringEmptyConstructorStmt build(
                CFG cfg,
                CodeLocation location,
                Expression... params) {
            return new StringEmptyConstructor.StringEmptyConstructorStmt(cfg, location, params[0]);
        }

        @Override
        public void setOriginatingStatement(Statement statement) {
            this.statement = statement;
        }

        @Override
        public String toString() {
            return "String";
        }


        @Override
        protected int compareSameClass(Statement statement) {
            return 0;
        }

        @Override
        protected int compareSameClassAndParams(Statement statement) {
            return 0;
        }


        @Override
        public <A extends AbstractState<A>> AnalysisState<A> fwdUnarySemantics(InterproceduralAnalysis<A> interproceduralAnalysis, AnalysisState<A> analysisState, SymbolicExpression symbolicExpression, StatementStore<A> statementStore) throws SemanticException {
            Expression self = getSubExpressions()[0];
            JavaAccessGlobal value = new JavaAccessGlobal(statement.getCFG(), getLocation(), self, "value");
            JavaAssignment assign = new JavaAssignment(getCFG(), getLocation(), value, new StringLiteral(statement.getCFG(), getLocation(), ""));
            JavaAssignment length = new JavaAssignment(getCFG(), getLocation(), value, new IntLiteral(statement.getCFG(), getLocation(), 0));

            return analysisState.lub(assign.forwardSemantics(analysisState, interproceduralAnalysis, statementStore))
                    .lub(length.forwardSemantics(analysisState, interproceduralAnalysis, statementStore));
        }
    }
}

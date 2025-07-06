package it.unive.jlisa.program.java.constructs.string.constructors;

import it.unive.jlisa.program.cfg.statement.JavaAssignment;
import it.unive.jlisa.program.cfg.statement.global.JavaAccessGlobal;
import it.unive.jlisa.program.cfg.statement.literal.IntLiteral;
import it.unive.jlisa.program.type.JavaInstrumentedStringType;
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


public class StringLiteralConstructor extends NativeCFG {

    public StringLiteralConstructor(
            CodeLocation location,
            ClassUnit classUnit,
            ReferenceType referenceType) {
        super(new CodeMemberDescriptor(location, classUnit, true, "String", referenceType,
                        new Parameter(location, "this", referenceType),
                        new Parameter(location, "value", JavaInstrumentedStringType.INSTANCE)),
                StringLiteralConstructor.StringConstructorStmt.class);
    }

    public static class StringConstructorStmt extends BinaryExpression implements PluggableStatement {
        protected Statement statement;

        protected StringConstructorStmt(CFG cfg, CodeLocation location, Expression self, Expression value) {
            super(cfg, location, "String", self, value);
        }

        public static StringLiteralConstructor.StringConstructorStmt build(
                CFG cfg,
                CodeLocation location,
                Expression... params) {
            return new StringLiteralConstructor.StringConstructorStmt(cfg, location, params[0], params[1]);
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
        public <A extends AbstractState<A>> AnalysisState<A> fwdBinarySemantics(InterproceduralAnalysis<A> interprocedural, AnalysisState<A> state, SymbolicExpression left, SymbolicExpression right, StatementStore<A> expressions) throws SemanticException {
            if (!(getSubExpressions()[1] instanceof StringLiteral)) {
                return state.bottom();
            }
            StringLiteral literal = (StringLiteral) getSubExpressions()[1];
            Expression self = getSubExpressions()[0];
            JavaAccessGlobal value = new JavaAccessGlobal(statement.getCFG(), getLocation(), self, "value");
            JavaAssignment assign = new JavaAssignment(getCFG(), getLocation(), value, getSubExpressions()[1]);
            JavaAccessGlobal length = new JavaAccessGlobal(statement.getCFG(), getLocation(), self, "length");
            JavaAssignment assignLength = new JavaAssignment(getCFG(), getLocation(), length, new IntLiteral(this.getCFG(), getLocation(), literal.getValue().length()));

            return state.lub(assign.forwardSemantics(state, interprocedural, expressions))
                    .lub(assignLength.forwardSemantics(state, interprocedural, expressions));

        }
    }
}

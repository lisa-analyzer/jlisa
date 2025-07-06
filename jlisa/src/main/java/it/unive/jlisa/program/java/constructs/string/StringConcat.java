package it.unive.jlisa.program.java.constructs.string;

import it.unive.jlisa.program.java.constructs.string.constructors.StringCopyConstructor;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.*;
import it.unive.lisa.program.cfg.statement.BinaryExpression;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.string.Concat;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.program.type.StringType;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.ReferenceType;

/**
 * The native construct representing the concatenation operation. This construct
 * can be invoked on a string variable {@code x} with {@code x.concat(other)},
 * where {@code other} is the string to be appended to {@code x}.
 */

public class StringConcat extends NativeCFG {

	/**
	 * Builds the construct.
	 * 
	 * @param location   the location where this construct is defined
	 * @param stringUnit the unit where this construct is defined
	 */
	public StringConcat(
			CodeLocation location,
			ClassUnit stringUnit,
			ReferenceType referenceType) {
		super(new CodeMemberDescriptor(location, stringUnit, true, "concat", referenceType,
				new Parameter(location, "this", referenceType),
				new Parameter(location, "other", referenceType)),
				StringConcatStmt.class);
	}

	/**
	 * An expression modeling the string contains operation. The type of both
	 * operands must be {@link StringType}. The type of this expression is the
	 * {@link BoolType}.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public static class StringConcatStmt extends BinaryExpression implements PluggableStatement {
		private Statement statement;

		@Override
		public void setOriginatingStatement(
				Statement statement) {
			this.statement = statement;
		}

        protected StringConcatStmt(CFG cfg, CodeLocation location, Expression self, Expression value) {
            super(cfg, location, "String", self, value);
        }

        public static StringConcat.StringConcatStmt build(
                CFG cfg,
                CodeLocation location,
                Expression... params) {
            return new StringConcat.StringConcatStmt(cfg, location, params[0], params[1]);
        }


		@Override
		public <A extends AbstractState<A>> AnalysisState<A> fwdBinarySemantics(
				InterproceduralAnalysis<A> interprocedural, AnalysisState<A> state, SymbolicExpression left,
				SymbolicExpression right, StatementStore<A> expressions) throws SemanticException {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException("Unimplemented method 'fwdBinarySemantics'");
		}

		@Override
		protected int compareSameClassAndParams(Statement o) {
			return 0;
		}
	}
}

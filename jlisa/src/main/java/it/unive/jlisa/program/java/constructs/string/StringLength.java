package it.unive.jlisa.program.java.constructs.string;

import it.unive.jlisa.program.type.IntType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.types.JavaClassType;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.cfg.*;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.UnaryExpression;
import it.unive.lisa.program.type.StringType;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.ReferenceType;

/**
 * The native construct representing the length operation. This construct can be
 * invoked on a string variable {@code x} with {@code x.len()}.
 *
 */
public class StringLength extends NativeCFG {

	/**
	 * Builds the construct.
	 * 
	 * @param location   the location where this construct is defined
	 * @param stringUnit the unit where this construct is defined
	 */
	public StringLength(
			CodeLocation location,
			ClassUnit stringUnit,
			ReferenceType referenceType) {
		super(new CodeMemberDescriptor(location, stringUnit, true, "length", JavaIntType.INSTANCE,
				new Parameter(location, "this", referenceType)),
				StringLengthStmt.class);
	}

	/**
	 * An expression modeling the string length operation. The type of the
	 * operand must be {@link StringType}. The type of this expression is the
	 * {@link JavaIntType}.
	 * 
	 */
	public static class StringLengthStmt extends UnaryExpression implements PluggableStatement {
		Statement statement;

		protected StringLengthStmt(CFG cfg, CodeLocation location, Expression expression) {
			super(cfg, location, "length", expression);
		}


		/**
		 * Builds a new instance of this native call, according to the
		 * {@link PluggableStatement} contract.
		 * 
		 * @param cfg      the cfg where the native call happens
		 * @param location the location where the native call happens
		 * @param params   the parameters of the native call
		 * 
		 * @return the newly-built call
		 */
		public static StringLength.StringLengthStmt build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new StringLength.StringLengthStmt(cfg, location, params[0]);
		}

		@Override
		public void setOriginatingStatement(
				Statement st) {
			this.statement = st;
		}


		@Override
		public <A extends AbstractState<A>> AnalysisState<A> fwdUnarySemantics(InterproceduralAnalysis<A> interprocedural, AnalysisState<A> state, SymbolicExpression expr, StatementStore<A> expressions) throws SemanticException {

			HeapDereference container = new HeapDereference(JavaClassType.lookup("String").get(), expr, getLocation());

			AccessChild length = new AccessChild(
					IntType.INSTANCE,
					container,
					new Variable(IntType.INSTANCE, "length", getLocation()),
					getLocation());

			return state.smallStepSemantics(length, this);
		}

		@Override
		protected int compareSameClassAndParams(Statement o) {
			return 0;
		}
	}
}
package it.unive.jlisa.program.java.constructs.integer;

import it.unive.jlisa.program.cfg.JavaExceptions;
import it.unive.jlisa.program.operator.JavaIntegerParseIntOperator;
import it.unive.jlisa.program.operator.JavaIsIntParsableOperator;
import it.unive.jlisa.program.type.JavaBooleanType;
import it.unive.jlisa.program.type.JavaIntType;
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
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.UnaryExpression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

/**
 * {@link Integer#parseInt(String)}: the {@code int} represented by the string,
 * or {@link NumberFormatException} if it represents none.
 */
public class IntegerParseInt extends UnaryExpression implements PluggableStatement {

	/**
	 * The call implemented by this native.
	 */
	protected Statement originating;

	/**
	 * Builds the native.
	 *
	 * @param cfg      the cfg
	 * @param location the location
	 * @param expr     the string
	 */
	public IntegerParseInt(
			CFG cfg,
			CodeLocation location,
			Expression expr) {
		super(cfg, location, "parseInt", expr);
	}

	/**
	 * Builds the native.
	 *
	 * @param cfg      the cfg
	 * @param location the location
	 * @param params   the parameters (the string)
	 *
	 * @return the native
	 */
	public static IntegerParseInt build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new IntegerParseInt(cfg, location, params[0]);
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
	public <A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> AnalysisState<A> fwdUnarySemantics(
					InterproceduralAnalysis<A, D> interprocedural,
					AnalysisState<A> state,
					SymbolicExpression expr,
					StatementStore<A> expressions)
					throws SemanticException {
		Type stringType = getProgram().getTypes().getStringType();
		CodeLocation location = getLocation();
		AccessChild value = new AccessChild(stringType, new HeapDereference(stringType, expr, location),
				new GlobalVariable(Untyped.INSTANCE, "value", location), location);
		it.unive.lisa.symbolic.value.UnaryExpression parsable = new it.unive.lisa.symbolic.value.UnaryExpression(
				JavaBooleanType.INSTANCE, value, JavaIsIntParsableOperator.INSTANCE, location);

		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();

		// NumberFormatException is raised exactly where the string is not
		// parsable, and the number is computed in the other executions
		JavaExceptions.Raising<A, D> raising = JavaExceptions.Raising.ofNative(interprocedural, this, originating,
				getSubExpression());
		AnalysisState<A> parsed = raising.raiseWhere(
				new it.unive.lisa.symbolic.value.UnaryExpression(JavaBooleanType.INSTANCE, parsable,
						LogicalNegation.INSTANCE, location),
				true, state, "java.lang.NumberFormatException");
		if (!parsed.getExecutionState().isBottom())
			parsed = analysis.smallStepSemantics(parsed, new it.unive.lisa.symbolic.value.UnaryExpression(
					JavaIntType.INSTANCE, value, JavaIntegerParseIntOperator.INSTANCE, location), context, originating);
		return raising.moveFrom(parsed);
	}
}

package it.unive.jlisa.program.java.constructs.arrays;

import it.unive.jlisa.program.cfg.JavaExceptions;
import it.unive.jlisa.program.type.JavaBooleanType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.BinaryExpression;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

/**
 * {@link java.util.Arrays#copyOf(Object[], int)} (and its overloads for the
 * arrays of primitive values): a new array of the given length. It raises
 * {@link NullPointerException} if the array is {@code null}, and
 * {@link NegativeArraySizeException} if the length is negative. The contents of
 * the new array are not tracked: the result is any array.
 */
public class ArraysCopyOf extends BinaryExpression implements PluggableStatement {

	/**
	 * The call implemented by this native.
	 */
	protected Statement originating;

	/**
	 * Builds the native.
	 *
	 * @param cfg       the cfg
	 * @param location  the location
	 * @param original  the copied array
	 * @param newLength the length of the copy
	 */
	public ArraysCopyOf(
			CFG cfg,
			CodeLocation location,
			Expression original,
			Expression newLength) {
		super(cfg, location, "copyOf", original, newLength);
	}

	/**
	 * Builds the native.
	 *
	 * @param cfg      the cfg
	 * @param location the location
	 * @param params   the parameters (the array and the length of the copy)
	 *
	 * @return the native
	 */
	public static ArraysCopyOf build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new ArraysCopyOf(cfg, location, params[0], params[1]);
	}

	@Override
	public void setOriginatingStatement(
			Statement st) {
		originating = st;
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdBinarySemantics(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression left,
			SymbolicExpression right,
			StatementStore<A> expressions)
			throws SemanticException {
		CodeLocation loc = getLocation();
		JavaExceptions.Raising<A, D> raising = JavaExceptions.Raising.ofNative(interprocedural, this, originating,
				getSubExpressions());
		state = raising.raiseIfNull(left, state);
		state = raising.raiseWhere(new it.unive.lisa.symbolic.value.BinaryExpression(JavaBooleanType.INSTANCE, right,
				new Constant(JavaIntType.INSTANCE, 0, loc), ComparisonLt.INSTANCE, loc), true, state,
				"java.lang.NegativeArraySizeException");
		if (state.getExecutionState().isBottom())
			return raising.moveFrom(state.bottomExecution());
		// the copy has the type of the copied array
		Type type = originating instanceof Expression e ? e.getStaticType() : Untyped.INSTANCE;
		return raising.moveFrom(interprocedural.getAnalysis().smallStepSemantics(state, new PushAny(type, loc),
				interprocedural.getCurrentContext(), this));
	}
}

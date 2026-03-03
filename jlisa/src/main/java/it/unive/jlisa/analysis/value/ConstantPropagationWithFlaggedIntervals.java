package it.unive.jlisa.analysis.value;

import it.unive.jlisa.lattices.ConstantValue;
import it.unive.jlisa.lattices.ConstantValueFlaggedInterval;
import it.unive.jlisa.lattices.JavaFlaggedInterval;
import it.unive.jlisa.program.operator.NaryExpression;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.PushInv;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.binary.LogicalAnd;
import it.unive.lisa.symbolic.value.operator.binary.LogicalOr;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A composite non-relational value domain that combines:
 * <ol>
 * <li>{@link ConstantPropagation} — exact symbolic constant tracking for all
 * value types (strings, booleans, numbers).</li>
 * <li>{@link TypedFlaggedJavaInterval} — typed interval analysis with
 * provenance tracking, under-approximation, and flag annotations (NaN,
 * infinity, integrality, parity) for numeric types.</li>
 * </ol>
 * <p>
 * The combined lattice element is {@link ConstantValueFlaggedInterval}, pairing
 * a {@link ConstantValue} with a {@link JavaFlaggedInterval}.
 * <p>
 * This domain also implements {@link ExistentialSatisfiabilityProvider},
 * enabling {@link it.unive.jlisa.checkers.AssertChecker} to produce correct
 * {@code FALSE} verdicts when a concrete violating run is guaranteed to exist.
 *
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public class ConstantPropagationWithFlaggedIntervals
		implements
		BaseNonRelationalValueDomain<ConstantValueFlaggedInterval>,
		ExistentialSatisfiabilityProvider {

	private final ConstantPropagation constantPropagation = new ConstantPropagation();
	private final TypedFlaggedJavaInterval interval = new TypedFlaggedJavaInterval();

	@Override
	public ConstantValueFlaggedInterval top() {
		return ConstantValueFlaggedInterval.TOP;
	}

	@Override
	public ConstantValueFlaggedInterval bottom() {
		return ConstantValueFlaggedInterval.BOTTOM;
	}

	@Override
	public boolean canProcess(
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle) {
		return constantPropagation.canProcess(expression, pp, oracle)
				|| interval.canProcess(expression, pp, oracle);
	}

	@Override
	public ConstantValueFlaggedInterval evalConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return new ConstantValueFlaggedInterval(
				constantPropagation.evalConstant(constant, pp, oracle),
				interval.evalConstant(constant, pp, oracle));
	}

	@Override
	public ConstantValueFlaggedInterval evalUnaryExpression(
			UnaryExpression expression,
			ConstantValueFlaggedInterval arg,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return new ConstantValueFlaggedInterval(
				constantPropagation.evalUnaryExpression(expression, arg.getConstantValue(), pp, oracle),
				interval.evalUnaryExpression(expression, arg.getFlaggedInterval(), pp, oracle));
	}

	@Override
	public ConstantValueFlaggedInterval evalBinaryExpression(
			BinaryExpression expression,
			ConstantValueFlaggedInterval left,
			ConstantValueFlaggedInterval right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return new ConstantValueFlaggedInterval(
				constantPropagation.evalBinaryExpression(expression, left.getConstantValue(),
						right.getConstantValue(), pp, oracle),
				interval.evalBinaryExpression(expression, left.getFlaggedInterval(),
						right.getFlaggedInterval(), pp, oracle));
	}

	@Override
	public ConstantValueFlaggedInterval evalTernaryExpression(
			TernaryExpression expression,
			ConstantValueFlaggedInterval left,
			ConstantValueFlaggedInterval middle,
			ConstantValueFlaggedInterval right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return new ConstantValueFlaggedInterval(
				constantPropagation.evalTernaryExpression(expression, left.getConstantValue(),
						middle.getConstantValue(), right.getConstantValue(), pp, oracle),
				interval.evalTernaryExpression(expression, left.getFlaggedInterval(),
						middle.getFlaggedInterval(), right.getFlaggedInterval(), pp, oracle));
	}

	@Override
	public ConstantValueFlaggedInterval evalValueExpression(
			ValueExpression expression,
			ConstantValueFlaggedInterval[] subExpressions,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		ConstantValue[] constantValues = Arrays.stream(subExpressions)
				.map(ConstantValueFlaggedInterval::getConstantValue)
				.toArray(ConstantValue[]::new);

		JavaFlaggedInterval[] flaggedIntervals = Arrays.stream(subExpressions)
				.map(ConstantValueFlaggedInterval::getFlaggedInterval)
				.toArray(JavaFlaggedInterval[]::new);

		return new ConstantValueFlaggedInterval(
				constantPropagation.evalValueExpression(expression, constantValues, pp, oracle),
				interval.evalValueExpression(expression, flaggedIntervals, pp, oracle));
	}

	@Override
	public ConstantValueFlaggedInterval evalPushAny(
			PushAny pushAny,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return new ConstantValueFlaggedInterval(
				constantPropagation.evalPushAny(pushAny, pp, oracle),
				interval.evalPushAny(pushAny, pp, oracle));
	}

	@Override
	public ConstantValueFlaggedInterval evalPushInv(
			PushInv pushInv,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return new ConstantValueFlaggedInterval(
				constantPropagation.evalPushInv(pushInv, pp, oracle),
				interval.evalPushInv(pushInv, pp, oracle));
	}

	@Override
	public ConstantValueFlaggedInterval evalSkip(
			Skip skip,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return new ConstantValueFlaggedInterval(
				constantPropagation.evalSkip(skip, pp, oracle),
				interval.evalSkip(skip, pp, oracle));
	}

	@Override
	public ConstantValueFlaggedInterval evalTypeCast(
			BinaryExpression cast,
			ConstantValueFlaggedInterval left,
			ConstantValueFlaggedInterval right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return new ConstantValueFlaggedInterval(
				constantPropagation.evalTypeCast(cast, left.getConstantValue(), right.getConstantValue(), pp, oracle),
				interval.evalTypeCast(cast, left.getFlaggedInterval(), right.getFlaggedInterval(), pp, oracle));
	}

	@Override
	public ConstantValueFlaggedInterval evalTypeConv(
			BinaryExpression conv,
			ConstantValueFlaggedInterval left,
			ConstantValueFlaggedInterval right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return new ConstantValueFlaggedInterval(
				constantPropagation.evalTypeConv(conv, left.getConstantValue(), right.getConstantValue(), pp, oracle),
				interval.evalTypeConv(conv, left.getFlaggedInterval(), right.getFlaggedInterval(), pp, oracle));
	}

	@Override
	public Satisfiability satisfiesAbstractValue(
			ConstantValueFlaggedInterval value,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		Satisfiability sat = constantPropagation.satisfiesAbstractValue(value.getConstantValue(), pp, oracle);
		switch (sat) {
		case NOT_SATISFIED:
		case SATISFIED:
			return sat;
		case BOTTOM:
		case UNKNOWN:
		default:
			Satisfiability satIntv = interval.satisfiesAbstractValue(value.getFlaggedInterval(), pp, oracle);
			if (satIntv == Satisfiability.SATISFIED || satIntv == Satisfiability.NOT_SATISFIED)
				return satIntv;
			return sat;
		}
	}

	@Override
	public Satisfiability satisfiesUnaryExpression(
			UnaryExpression expression,
			ConstantValueFlaggedInterval arg,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		Satisfiability sat = constantPropagation.satisfiesUnaryExpression(expression, arg.getConstantValue(),
				pp, oracle);
		switch (sat) {
		case NOT_SATISFIED:
		case SATISFIED:
			return sat;
		case BOTTOM:
		case UNKNOWN:
		default:
			if (arg.getFlaggedInterval().isBottom())
				return sat;
			Satisfiability satIntv = interval.satisfiesUnaryExpression(expression, arg.getFlaggedInterval(),
					pp, oracle);
			if (satIntv == Satisfiability.SATISFIED || satIntv == Satisfiability.NOT_SATISFIED)
				return satIntv;
			return sat;
		}
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(
			BinaryExpression expression,
			ConstantValueFlaggedInterval left,
			ConstantValueFlaggedInterval right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		Satisfiability sat = constantPropagation.satisfiesBinaryExpression(expression, left.getConstantValue(),
				right.getConstantValue(), pp, oracle);
		switch (sat) {
		case NOT_SATISFIED:
		case SATISFIED:
			return sat;
		case BOTTOM:
		case UNKNOWN:
		default:
			if (left.getFlaggedInterval().isBottom() || right.getFlaggedInterval().isBottom())
				return sat;
			Satisfiability satIntv = interval.satisfiesBinaryExpression(expression, left.getFlaggedInterval(),
					right.getFlaggedInterval(), pp, oracle);
			if (satIntv == Satisfiability.SATISFIED || satIntv == Satisfiability.NOT_SATISFIED)
				return satIntv;
			return sat;
		}
	}

	@Override
	public Satisfiability satisfiesTernaryExpression(
			TernaryExpression expression,
			ConstantValueFlaggedInterval left,
			ConstantValueFlaggedInterval middle,
			ConstantValueFlaggedInterval right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		Satisfiability sat = constantPropagation.satisfiesTernaryExpression(expression,
				left.getConstantValue(), middle.getConstantValue(), right.getConstantValue(), pp, oracle);
		switch (sat) {
		case NOT_SATISFIED:
		case SATISFIED:
			return sat;
		case BOTTOM:
		case UNKNOWN:
		default:
			if (left.getFlaggedInterval().isBottom() || middle.getFlaggedInterval().isBottom()
					|| right.getFlaggedInterval().isBottom())
				return sat;
			Satisfiability satIntv = interval.satisfiesTernaryExpression(expression,
					left.getFlaggedInterval(), middle.getFlaggedInterval(), right.getFlaggedInterval(), pp, oracle);
			if (satIntv == Satisfiability.SATISFIED || satIntv == Satisfiability.NOT_SATISFIED)
				return satIntv;
			return sat;
		}
	}

	@Override
	public ValueEnvironment<ConstantValueFlaggedInterval> assume(
			ValueEnvironment<ConstantValueFlaggedInterval> environment,
			ValueExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		Satisfiability sat = satisfies(environment, expression, src, oracle);
		if (sat == Satisfiability.NOT_SATISFIED)
			return environment.bottom();
		if (sat == Satisfiability.SATISFIED)
			return environment;
		ValueExpression e = expression.removeNegations();
		if (e instanceof BinaryExpression be) {
			ValueExpression left = (ValueExpression) be.getLeft();
			ValueExpression right = (ValueExpression) be.getRight();
			if (left instanceof Identifier id && (!id.canBeAssigned() || !canProcess(id, src, oracle)))
				return environment;
			else if (right instanceof Identifier id && (!id.canBeAssigned() || !canProcess(id, src, oracle)))
				return environment;
		}
		return BaseNonRelationalValueDomain.super.assume(environment, expression, src, dest, oracle);
	}

	@Override
	public ValueEnvironment<ConstantValueFlaggedInterval> assumeConstant(
			ValueEnvironment<ConstantValueFlaggedInterval> environment,
			Constant expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		Pair<ValueEnvironment<ConstantValue>,
				ValueEnvironment<JavaFlaggedInterval>> environments = splitEnvironment(environment);
		ValueEnvironment<ConstantValue> cvEnv = constantPropagation.assumeConstant(
				environments.getLeft(), expression, src, dest, oracle);
		ValueEnvironment<JavaFlaggedInterval> fiEnv = interval.assumeConstant(
				environments.getRight(), expression, src, dest, oracle);
		return mergeEnvironments(environment, cvEnv, fiEnv);
	}

	@Override
	public ValueEnvironment<ConstantValueFlaggedInterval> assumeIdentifier(
			ValueEnvironment<ConstantValueFlaggedInterval> environment,
			Identifier expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		Pair<ValueEnvironment<ConstantValue>,
				ValueEnvironment<JavaFlaggedInterval>> environments = splitEnvironment(environment);
		ValueEnvironment<ConstantValue> cvEnv = constantPropagation.assumeIdentifier(
				environments.getLeft(), expression, src, dest, oracle);
		ValueEnvironment<JavaFlaggedInterval> fiEnv = interval.assumeIdentifier(
				environments.getRight(), expression, src, dest, oracle);
		return mergeEnvironments(environment, cvEnv, fiEnv);
	}

	@Override
	public ValueEnvironment<ConstantValueFlaggedInterval> assumeUnaryExpression(
			ValueEnvironment<ConstantValueFlaggedInterval> environment,
			UnaryExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		Pair<ValueEnvironment<ConstantValue>,
				ValueEnvironment<JavaFlaggedInterval>> environments = splitEnvironment(environment);
		ValueEnvironment<ConstantValue> cvEnv = constantPropagation.assumeUnaryExpression(
				environments.getLeft(), expression, src, dest, oracle);
		ValueEnvironment<JavaFlaggedInterval> fiEnv = interval.assumeUnaryExpression(
				environments.getRight(), expression, src, dest, oracle);
		return mergeEnvironments(environment, cvEnv, fiEnv);
	}

	@Override
	public ValueEnvironment<ConstantValueFlaggedInterval> assumeBinaryExpression(
			ValueEnvironment<ConstantValueFlaggedInterval> environment,
			BinaryExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		Pair<ValueEnvironment<ConstantValue>,
				ValueEnvironment<JavaFlaggedInterval>> environments = splitEnvironment(environment);
		ValueEnvironment<ConstantValue> cvEnv = constantPropagation.assumeBinaryExpression(
				environments.getLeft(), expression, src, dest, oracle);
		ValueEnvironment<JavaFlaggedInterval> fiEnv = interval.assumeBinaryExpression(
				environments.getRight(), expression, src, dest, oracle);
		return mergeEnvironments(environment, cvEnv, fiEnv);
	}

	@Override
	public ValueEnvironment<ConstantValueFlaggedInterval> assumeTernaryExpression(
			ValueEnvironment<ConstantValueFlaggedInterval> environment,
			TernaryExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		Pair<ValueEnvironment<ConstantValue>,
				ValueEnvironment<JavaFlaggedInterval>> environments = splitEnvironment(environment);
		ValueEnvironment<ConstantValue> cvEnv = constantPropagation.assumeTernaryExpression(
				environments.getLeft(), expression, src, dest, oracle);
		ValueEnvironment<JavaFlaggedInterval> fiEnv = interval.assumeTernaryExpression(
				environments.getRight(), expression, src, dest, oracle);
		return mergeEnvironments(environment, cvEnv, fiEnv);
	}

	@Override
	public ValueEnvironment<ConstantValueFlaggedInterval> assumeValueExpression(
			ValueEnvironment<ConstantValueFlaggedInterval> environment,
			ValueExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		Pair<ValueEnvironment<ConstantValue>,
				ValueEnvironment<JavaFlaggedInterval>> environments = splitEnvironment(environment);
		ValueEnvironment<ConstantValue> cvEnv = constantPropagation.assumeValueExpression(
				environments.getLeft(), expression, src, dest, oracle);
		ValueEnvironment<JavaFlaggedInterval> fiEnv = interval.assumeValueExpression(
				environments.getRight(), expression, src, dest, oracle);
		return mergeEnvironments(environment, cvEnv, fiEnv);
	}

	@Override
	public Satisfiability satisfiesConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return constantPropagation.satisfiesConstant(constant, pp, oracle);
	}

	@Override
	public Satisfiability satisfies(
			ValueEnvironment<ConstantValueFlaggedInterval> environment,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		// Handle NaryExpression (constant propagation override)
		if (expression instanceof NaryExpression) {
			Pair<ValueEnvironment<ConstantValue>,
					ValueEnvironment<JavaFlaggedInterval>> environments = splitEnvironment(environment);
			SymbolicExpression[] exprs = ((NaryExpression) expression).getAllOperand(0);
			ConstantValue[] args = new ConstantValue[exprs.length];
			for (int i = 0; i < exprs.length; ++i) {
				ConstantValue left = constantPropagation.eval(environments.getLeft(),
						(ValueExpression) exprs[i], pp, oracle);
				if (left.isBottom())
					return Satisfiability.BOTTOM;
				args[i] = left;
			}
			return constantPropagation.satisfiesNaryExpression((NaryExpression) expression, args, pp, oracle);
		}
		return BaseNonRelationalValueDomain.super.satisfies(environment, expression, pp, oracle);
	}

	// -------------------------------------------------------------------------
	// ExistentialSatisfiabilityProvider implementation
	// -------------------------------------------------------------------------

	@Override
	@SuppressWarnings("unchecked")
	public boolean existentiallySatisfies(
			ValueEnvironment<?> rawEnv,
			ValueExpression expr,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (!(expr instanceof BinaryExpression be))
			return false;
		// Conjunction (De Morgan result of ¬(A||B)): both conjuncts must be
		// independently existentially satisfiable. Sound for a non-relational
		// domain because variables are tracked independently.
		if (be.getOperator() instanceof LogicalAnd) {
			if (!(be.getLeft() instanceof ValueExpression vl)
					|| !(be.getRight() instanceof ValueExpression vr))
				return false;
			return existentiallySatisfies(rawEnv, vl, pp, oracle)
					&& existentiallySatisfies(rawEnv, vr, pp, oracle);
		}
		// Disjunction (De Morgan result of ¬(A&&B)): at least one disjunct
		// must be existentially satisfiable.
		if (be.getOperator() instanceof LogicalOr) {
			if (!(be.getLeft() instanceof ValueExpression vl)
					|| !(be.getRight() instanceof ValueExpression vr))
				return false;
			return existentiallySatisfies(rawEnv, vl, pp, oracle)
					|| existentiallySatisfies(rawEnv, vr, pp, oracle);
		}
		// Atomic case: x op c — delegate to the interval under-approximation.
		if (!(be.getLeft() instanceof Identifier leftId))
			return false;
		ValueEnvironment<ConstantValueFlaggedInterval> env = (ValueEnvironment<ConstantValueFlaggedInterval>) rawEnv;
		Pair<ValueEnvironment<ConstantValue>,
				ValueEnvironment<JavaFlaggedInterval>> environments = splitEnvironment(env);
		// Evaluate the right-hand side without calling the full eval()
		// pipeline, which requires a live SemanticOracle for event tracking.
		// The existential check only handles Constant and Identifier on the
		// right side; anything more complex is conservatively unsatisfiable.
		JavaFlaggedInterval rightAbstract = evalRightOracle(environments.getRight(), be, pp);
		if (rightAbstract == null)
			return false;
		return interval.existentiallySatisfiesEq(environments.getRight(), leftId, rightAbstract, pp);
	}

	/**
	 * Evaluates the right-hand operand of a binary expression without going
	 * through the full {@code eval()} pipeline (which requires a live
	 * {@link SemanticOracle}). Handles {@link Constant} and {@link Identifier}
	 * directly; returns {@code null} for anything more complex.
	 */
	private JavaFlaggedInterval evalRightOracle(
			ValueEnvironment<JavaFlaggedInterval> env,
			BinaryExpression be,
			ProgramPoint pp) {
		if (be.getRight() instanceof Constant c)
			return interval.evalConstant(c, pp, null);
		if (be.getRight() instanceof Identifier id) {
			JavaFlaggedInterval v = env.getState(id);
			return v != null ? v : interval.bottom();
		}
		return null;
	}

	// -------------------------------------------------------------------------
	// Environment helpers
	// -------------------------------------------------------------------------

	/**
	 * Splits a combined environment into separate constant-value and
	 * flagged-interval environments.
	 *
	 * @param environment the combined environment to split
	 *
	 * @return a pair of (constant-value env, flagged-interval env)
	 *
	 * @throws SemanticException if LiSA's environment operations throw
	 */
	public static Pair<ValueEnvironment<ConstantValue>, ValueEnvironment<JavaFlaggedInterval>> splitEnvironment(
			ValueEnvironment<ConstantValueFlaggedInterval> environment)
			throws SemanticException {

		ValueEnvironment<ConstantValue> cvEnvironment = new ValueEnvironment<>(ConstantValue.BOTTOM);
		ValueEnvironment<JavaFlaggedInterval> fiEnvironment = new ValueEnvironment<>(JavaFlaggedInterval.BOTTOM);

		for (Identifier id : environment.getKeys()) {
			ConstantValueFlaggedInterval value = environment.getState(id);
			if (value == null)
				continue;
			cvEnvironment = cvEnvironment.putState(id, value.getConstantValue());
			fiEnvironment = fiEnvironment.putState(id, value.getFlaggedInterval());
		}

		return Pair.of(cvEnvironment, fiEnvironment);
	}

	/**
	 * Merges separate constant-value and flagged-interval environments back
	 * into a combined environment.
	 *
	 * @param oldEnvironment the original combined environment (used to detect
	 *                           bottom transitions)
	 * @param constantEnv    the updated constant-value environment
	 * @param intervalEnv    the updated flagged-interval environment
	 *
	 * @return the merged combined environment
	 *
	 * @throws SemanticException if LiSA's environment operations throw
	 */
	public static ValueEnvironment<ConstantValueFlaggedInterval> mergeEnvironments(
			ValueEnvironment<ConstantValueFlaggedInterval> oldEnvironment,
			ValueEnvironment<ConstantValue> constantEnv,
			ValueEnvironment<JavaFlaggedInterval> intervalEnv)
			throws SemanticException {

		ValueEnvironment<
				ConstantValueFlaggedInterval> merged = new ValueEnvironment<>(ConstantValueFlaggedInterval.BOTTOM);

		Set<Identifier> allIds = new HashSet<>();
		allIds.addAll(constantEnv.getKeys());
		allIds.addAll(intervalEnv.getKeys());

		for (Identifier id : allIds) {
			ConstantValue constVal = constantEnv.getState(id);
			JavaFlaggedInterval fiVal = intervalEnv.getState(id);
			JavaFlaggedInterval oldFi = oldEnvironment.getState(id).getFlaggedInterval();
			if (oldFi != null && fiVal != null && !oldFi.isBottom() && fiVal.isBottom()) {
				// When the old interval is not BOTTOM but the new interval is
				// BOTTOM,
				// move the constant value to BOTTOM to maintain consistency.
				constVal = ConstantValue.BOTTOM;
			}
			if (constVal == null)
				constVal = ConstantValue.BOTTOM;
			if (fiVal == null)
				fiVal = JavaFlaggedInterval.BOTTOM;

			merged = merged.putState(id, new ConstantValueFlaggedInterval(constVal, fiVal));
		}
		return merged;
	}
}

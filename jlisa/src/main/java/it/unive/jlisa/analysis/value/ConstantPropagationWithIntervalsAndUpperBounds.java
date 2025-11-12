package it.unive.jlisa.analysis.value;

import it.unive.jlisa.lattices.ConstantValue;
import it.unive.jlisa.lattices.ConstantValueIntIntervalUpperBounds;
import it.unive.jlisa.program.operator.NaryExpression;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.numeric.UpperBounds;
import it.unive.lisa.lattices.symbolic.DefiniteIdSet;
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
import it.unive.lisa.util.numeric.IntInterval;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.tuple.Triple;

public class ConstantPropagationWithIntervalsAndUpperBounds
		implements
		BaseNonRelationalValueDomain<ConstantValueIntIntervalUpperBounds> {

	private final ConstantPropagation constantPropagation = new ConstantPropagation();
	private final JavaNumericInterval interval = new JavaNumericInterval();
	private final UpperBounds upperBounds = new UpperBounds();

	@Override
	public ConstantValueIntIntervalUpperBounds top() {
		return ConstantValueIntIntervalUpperBounds.TOP;
	}

	@Override
	public ConstantValueIntIntervalUpperBounds bottom() {
		return ConstantValueIntIntervalUpperBounds.BOTTOM;
	}

	@Override
	public boolean canProcess(
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle) {
		return constantPropagation.canProcess(expression, pp, oracle) || interval.canProcess(expression, pp, oracle);
	}

	@Override
	public ConstantValueIntIntervalUpperBounds evalConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		ConstantValue value = constantPropagation.evalConstant(constant, pp, oracle);
		return new ConstantValueIntIntervalUpperBounds(
				value,
				interval.evalConstant(constant, pp, oracle),
				new DefiniteIdSet(new HashSet<>(), false));
	}

	@Override
	public ConstantValueIntIntervalUpperBounds evalUnaryExpression(
			UnaryExpression expression,
			ConstantValueIntIntervalUpperBounds arg,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return new ConstantValueIntIntervalUpperBounds(
				constantPropagation.evalUnaryExpression(expression, arg.getConstantValue(), pp, oracle),
				interval.evalUnaryExpression(expression, arg.getIntInterval(), pp, oracle),
				new DefiniteIdSet(new HashSet<>(), false));
	}

	@Override
	public ConstantValueIntIntervalUpperBounds evalBinaryExpression(
			BinaryExpression expression,
			ConstantValueIntIntervalUpperBounds left,
			ConstantValueIntIntervalUpperBounds right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return new ConstantValueIntIntervalUpperBounds(
				constantPropagation.evalBinaryExpression(expression, left.getConstantValue(), right.getConstantValue(),
						pp, oracle),
				interval.evalBinaryExpression(expression, left.getIntInterval(), right.getIntInterval(), pp, oracle),
				new DefiniteIdSet(new HashSet<>(), false));
	}

	@Override
	public ConstantValueIntIntervalUpperBounds evalTernaryExpression(
			TernaryExpression expression,
			ConstantValueIntIntervalUpperBounds left,
			ConstantValueIntIntervalUpperBounds middle,
			ConstantValueIntIntervalUpperBounds right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return new ConstantValueIntIntervalUpperBounds(
				constantPropagation.evalTernaryExpression(expression, left.getConstantValue(),
						middle.getConstantValue(), right.getConstantValue(), pp, oracle),
				interval.evalTernaryExpression(expression, left.getIntInterval(), middle.getIntInterval(),
						right.getIntInterval(), pp, oracle),
				new DefiniteIdSet(new HashSet<>(), false));
	}

	@Override
	public ConstantValueIntIntervalUpperBounds evalValueExpression(
			ValueExpression expression,
			ConstantValueIntIntervalUpperBounds[] subExpressions,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		ConstantValue[] constantValues = Arrays.stream(subExpressions)
				.map(ConstantValueIntIntervalUpperBounds::getConstantValue)
				.toArray(ConstantValue[]::new);

		IntInterval[] intIntervals = Arrays.stream(subExpressions)
				.map(ConstantValueIntIntervalUpperBounds::getIntInterval)
				.toArray(IntInterval[]::new);

		return new ConstantValueIntIntervalUpperBounds(
				constantPropagation.evalValueExpression(expression, constantValues, pp, oracle),
				interval.evalValueExpression(expression, intIntervals, pp, oracle),
				new DefiniteIdSet(new HashSet<>(), false));
	}

	@Override
	public ConstantValueIntIntervalUpperBounds evalPushAny(
			PushAny pushAny,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return new ConstantValueIntIntervalUpperBounds(
				constantPropagation.evalPushAny(pushAny, pp, oracle),
				interval.evalPushAny(pushAny, pp, oracle),
				new DefiniteIdSet(new HashSet<>(), false));
	}

	@Override
	public ConstantValueIntIntervalUpperBounds evalPushInv(
			PushInv pushInv,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return new ConstantValueIntIntervalUpperBounds(
				constantPropagation.evalPushInv(pushInv, pp, oracle),
				interval.evalPushInv(pushInv, pp, oracle),
				new DefiniteIdSet(new HashSet<>(), false));
	}

	@Override
	public ConstantValueIntIntervalUpperBounds evalSkip(
			Skip skip,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return new ConstantValueIntIntervalUpperBounds(
				constantPropagation.evalSkip(skip, pp, oracle),
				interval.evalSkip(skip, pp, oracle),
				new DefiniteIdSet(new HashSet<>(), false));
	}

	@Override
	public ConstantValueIntIntervalUpperBounds evalTypeCast(
			BinaryExpression cast,
			ConstantValueIntIntervalUpperBounds left,
			ConstantValueIntIntervalUpperBounds right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return new ConstantValueIntIntervalUpperBounds(
				constantPropagation.evalTypeCast(cast, left.getConstantValue(), right.getConstantValue(), pp, oracle),
				interval.evalTypeCast(cast, left.getIntInterval(), right.getIntInterval(), pp, oracle),
				new DefiniteIdSet(new HashSet<>(), false));
	}

	@Override
	public ConstantValueIntIntervalUpperBounds evalTypeConv(
			BinaryExpression conv,
			ConstantValueIntIntervalUpperBounds left,
			ConstantValueIntIntervalUpperBounds right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return new ConstantValueIntIntervalUpperBounds(
				constantPropagation.evalTypeConv(conv, left.getConstantValue(), right.getConstantValue(), pp, oracle),
				interval.evalTypeConv(conv, left.getIntInterval(), right.getIntInterval(), pp, oracle),
				new DefiniteIdSet(new HashSet<>(), false));
	}

	@Override
	public Satisfiability satisfiesAbstractValue(
			ConstantValueIntIntervalUpperBounds value,
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
			Satisfiability sat_intv = interval.satisfiesAbstractValue(value.getIntInterval(), pp, oracle);
			if (sat_intv == Satisfiability.SATISFIED || sat_intv == Satisfiability.NOT_SATISFIED)
				return sat_intv;
			// we keep the same distinction between BOTTOM and UNKNOWN
			// that we got from constant propagation
			return sat;
		}
	}

	@Override
	public Satisfiability satisfiesUnaryExpression(
			UnaryExpression expression,
			ConstantValueIntIntervalUpperBounds arg,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		Satisfiability sat = constantPropagation.satisfiesUnaryExpression(expression, arg.getConstantValue(), pp,
				oracle);
		switch (sat) {
		case NOT_SATISFIED:
		case SATISFIED:
			return sat;
		case BOTTOM:
		case UNKNOWN:
		default:
			if (arg.getIntInterval().isBottom())
				// we keep the same distinction between BOTTOM and UNKNOWN
				// that we got from constant propagation
				return sat;
			Satisfiability sat_intv = interval.satisfiesUnaryExpression(expression, arg.getIntInterval(), pp, oracle);
			if (sat_intv == Satisfiability.SATISFIED || sat_intv == Satisfiability.NOT_SATISFIED)
				return sat_intv;
			// we keep the same distinction between BOTTOM and UNKNOWN
			// that we got from constant propagation
			return sat;
		}
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(
			BinaryExpression expression,
			ConstantValueIntIntervalUpperBounds left,
			ConstantValueIntIntervalUpperBounds right,
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
			if (left.getIntInterval().isBottom() || right.getIntInterval().isBottom())
				// we keep the same distinction between BOTTOM and UNKNOWN
				// that we got from constant propagation
				return sat;
			Satisfiability sat_intv = interval.satisfiesBinaryExpression(expression, left.getIntInterval(),
					right.getIntInterval(), pp, oracle);
			if (sat_intv == Satisfiability.SATISFIED || sat_intv == Satisfiability.NOT_SATISFIED)
				return sat_intv;
			// we keep the same distinction between BOTTOM and UNKNOWN
			// that we got from constant propagation
			return sat;
		}
	}

	@Override
	public Satisfiability satisfiesTernaryExpression(
			TernaryExpression expression,
			ConstantValueIntIntervalUpperBounds left,
			ConstantValueIntIntervalUpperBounds middle,
			ConstantValueIntIntervalUpperBounds right,
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
			if (left.getIntInterval().isBottom() || middle.getIntInterval().isBottom()
					|| right.getIntInterval().isBottom())
				// we keep the same distinction between BOTTOM and UNKNOWN
				// that we got from constant propagation
				return sat;
			Satisfiability sat_intv = interval.satisfiesTernaryExpression(expression,
					left.getIntInterval(), middle.getIntInterval(), right.getIntInterval(), pp, oracle);
			if (sat_intv == Satisfiability.SATISFIED || sat_intv == Satisfiability.NOT_SATISFIED)
				return sat_intv;
			// we keep the same distinction between BOTTOM and UNKNOWN
			// that we got from constant propagation
			return sat;
		}
	}

	@Override
	public ValueEnvironment<ConstantValueIntIntervalUpperBounds> assume(
			ValueEnvironment<ConstantValueIntIntervalUpperBounds> environment,
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
		return BaseNonRelationalValueDomain.super.assume(environment, expression, src, dest, oracle);
	}

	@Override
	public ValueEnvironment<ConstantValueIntIntervalUpperBounds> assumeConstant(
			ValueEnvironment<ConstantValueIntIntervalUpperBounds> environment,
			Constant expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		Triple<ValueEnvironment<ConstantValue>,
				ValueEnvironment<IntInterval>,
				ValueEnvironment<DefiniteIdSet>> environments = splitEnvironment(environment);
		ValueEnvironment<ConstantValue> constantValueEnvironment = constantPropagation
				.assumeConstant(environments.getLeft(), expression, src, dest, oracle);
		ValueEnvironment<IntInterval> intIntervalEnvironment = interval.assumeConstant(environments.getMiddle(),
				expression, src, dest, oracle);
		return mergeEnvironments(environment, constantValueEnvironment, intIntervalEnvironment,
				environments.getRight());
	}

	@Override
	public ValueEnvironment<ConstantValueIntIntervalUpperBounds> assumeIdentifier(
			ValueEnvironment<ConstantValueIntIntervalUpperBounds> environment,
			Identifier expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		Triple<ValueEnvironment<ConstantValue>,
				ValueEnvironment<IntInterval>,
				ValueEnvironment<DefiniteIdSet>> environments = splitEnvironment(environment);
		ValueEnvironment<ConstantValue> constantValueEnvironment = constantPropagation
				.assumeIdentifier(environments.getLeft(), expression, src, dest, oracle);
		ValueEnvironment<IntInterval> intIntervalEnvironment = interval.assumeIdentifier(environments.getMiddle(),
				expression, src, dest, oracle);
		return mergeEnvironments(environment, constantValueEnvironment, intIntervalEnvironment,
				environments.getRight());
	}

	@Override
	public ValueEnvironment<ConstantValueIntIntervalUpperBounds> assumeUnaryExpression(
			ValueEnvironment<ConstantValueIntIntervalUpperBounds> environment,
			UnaryExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		Triple<ValueEnvironment<ConstantValue>,
				ValueEnvironment<IntInterval>,
				ValueEnvironment<DefiniteIdSet>> environments = splitEnvironment(environment);
		ValueEnvironment<ConstantValue> constantValueEnvironment = constantPropagation
				.assumeUnaryExpression(environments.getLeft(), expression, src, dest, oracle);
		ValueEnvironment<IntInterval> intIntervalEnvironment = interval.assumeUnaryExpression(environments.getMiddle(),
				expression, src, dest, oracle);
		return mergeEnvironments(environment, constantValueEnvironment, intIntervalEnvironment,
				environments.getRight());
	}

	@Override
	public ValueEnvironment<ConstantValueIntIntervalUpperBounds> assumeBinaryExpression(
			ValueEnvironment<ConstantValueIntIntervalUpperBounds> environment,
			BinaryExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		Triple<ValueEnvironment<ConstantValue>,
				ValueEnvironment<IntInterval>,
				ValueEnvironment<DefiniteIdSet>> environments = splitEnvironment(environment);
		ValueEnvironment<ConstantValue> constantValueEnvironment = constantPropagation
				.assumeBinaryExpression(environments.getLeft(), expression, src, dest, oracle);
		ValueEnvironment<IntInterval> intIntervalEnvironment = interval.assumeBinaryExpression(environments.getMiddle(),
				expression, src, dest, oracle);
		return mergeEnvironments(environment, constantValueEnvironment, intIntervalEnvironment,
				environments.getRight());
	}

	@Override
	public ValueEnvironment<ConstantValueIntIntervalUpperBounds> assumeTernaryExpression(
			ValueEnvironment<ConstantValueIntIntervalUpperBounds> environment,
			TernaryExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		Triple<ValueEnvironment<ConstantValue>,
				ValueEnvironment<IntInterval>,
				ValueEnvironment<DefiniteIdSet>> environments = splitEnvironment(environment);
		ValueEnvironment<ConstantValue> constantValueEnvironment = constantPropagation
				.assumeTernaryExpression(environments.getLeft(), expression, src, dest, oracle);
		ValueEnvironment<
				IntInterval> intIntervalEnvironment = interval.assumeTernaryExpression(environments.getMiddle(),
						expression, src, dest, oracle);
		return mergeEnvironments(environment, constantValueEnvironment, intIntervalEnvironment,
				environments.getRight());
	}

	@Override
	public ValueEnvironment<ConstantValueIntIntervalUpperBounds> assumeValueExpression(
			ValueEnvironment<ConstantValueIntIntervalUpperBounds> environment,
			ValueExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		Triple<ValueEnvironment<ConstantValue>,
				ValueEnvironment<IntInterval>,
				ValueEnvironment<DefiniteIdSet>> environments = splitEnvironment(environment);
		ValueEnvironment<ConstantValue> constantValueEnvironment = constantPropagation
				.assumeValueExpression(environments.getLeft(), expression, src, dest, oracle);
		ValueEnvironment<IntInterval> intIntervalEnvironment = interval.assumeValueExpression(environments.getMiddle(),
				expression, src, dest, oracle);
		return mergeEnvironments(environment, constantValueEnvironment, intIntervalEnvironment,
				environments.getRight());
	}

	public static Triple<ValueEnvironment<ConstantValue>, ValueEnvironment<IntInterval>,
			ValueEnvironment<DefiniteIdSet>>

			splitEnvironment(
					ValueEnvironment<ConstantValueIntIntervalUpperBounds> environment)
					throws SemanticException {

		ValueEnvironment<ConstantValue> constantValueEnvironment = new ValueEnvironment<>(ConstantValue.BOTTOM);
		ValueEnvironment<IntInterval> intIntervalValueEnvironment = new ValueEnvironment<>(IntInterval.BOTTOM);
		ValueEnvironment<DefiniteIdSet> definiteIdSetValueEnvironment = new ValueEnvironment<>(
				new DefiniteIdSet(new HashSet<>(), false));
		for (Identifier id : environment.getKeys()) {
			ConstantValueIntIntervalUpperBounds value = environment.getState(id);
			if (value == null)
				continue;

			ConstantValue constant = value.getConstantValue();
			IntInterval interval = value.getIntInterval();
			DefiniteIdSet definiteIdSet = value.getDefiniteIdSet();
			constantValueEnvironment = constantValueEnvironment.putState(id, constant);
			intIntervalValueEnvironment = intIntervalValueEnvironment.putState(id, interval);
			definiteIdSetValueEnvironment = definiteIdSetValueEnvironment.putState(id, definiteIdSet);
		}

		return Triple.of(constantValueEnvironment, intIntervalValueEnvironment, definiteIdSetValueEnvironment);
	}

	public static ValueEnvironment<ConstantValueIntIntervalUpperBounds> mergeEnvironments(
			ValueEnvironment<ConstantValueIntIntervalUpperBounds> oldEnvironment,
			ValueEnvironment<ConstantValue> constantEnv,
			ValueEnvironment<IntInterval> intervalEnv,
			ValueEnvironment<DefiniteIdSet> definiteIdSetEnv)
			throws SemanticException {

		ValueEnvironment<ConstantValueIntIntervalUpperBounds> merged = new ValueEnvironment<>(
				ConstantValueIntIntervalUpperBounds.BOTTOM);

		Set<Identifier> allIds = new HashSet<>();
		allIds.addAll(constantEnv.getKeys());
		allIds.addAll(intervalEnv.getKeys());
		for (Identifier id : allIds) {
			ConstantValue constVal = constantEnv.getState(id);
			IntInterval intVal = intervalEnv.getState(id);
			DefiniteIdSet definiteIdSetVal = definiteIdSetEnv.getState(id);
			IntInterval oldInterval = oldEnvironment.getState(id).getIntInterval();
			if (oldInterval != null && intVal != null && !oldInterval.isBottom() && intVal.isBottom()) {
				// When the old interval is not BOTTOM but the new interval is
				// BOTTOM,
				// we should also move the constant value to BOTTOM to maintain
				// consistency.
				constVal = ConstantValue.BOTTOM;
			}
			if (constVal == null)
				constVal = ConstantValue.BOTTOM;
			if (intVal == null)
				intVal = IntInterval.BOTTOM;
			if (definiteIdSetEnv == null)
				definiteIdSetVal = new DefiniteIdSet(new HashSet<>(), false);
			ConstantValueIntIntervalUpperBounds combined = new ConstantValueIntIntervalUpperBounds(constVal, intVal,
					definiteIdSetVal);
			merged = merged.putState(id, combined);
		}
		return merged;
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
			ValueEnvironment<ConstantValueIntIntervalUpperBounds> environment,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		Triple<ValueEnvironment<ConstantValue>,
				ValueEnvironment<IntInterval>,
				ValueEnvironment<DefiniteIdSet>> environments = splitEnvironment(environment);
		// Note: Since ConstantPropagation overrides `satisfies` to handle the
		// satisfiability of n-ary expressions, we need to include the
		// corresponding
		// logic here as a temporary workaround. This is necessary because
		// BaseNonRelationalValueDomain does not yet support
		// `satisfiesNaryExpression`.
		if (expression instanceof NaryExpression) {
			SymbolicExpression[] exprs = ((NaryExpression) expression).getAllOperand(0);
			ConstantValue[] args = new ConstantValue[exprs.length];
			for (int i = 0; i < exprs.length; ++i) {
				ConstantValue left = constantPropagation.eval(environments.getLeft(), (ValueExpression) exprs[i], pp,
						oracle);
				if (left.isBottom())
					return Satisfiability.BOTTOM;
				args[i] = left;
			}

			return constantPropagation.satisfiesNaryExpression((NaryExpression) expression, args, pp, oracle);
		}

		return BaseNonRelationalValueDomain.super.satisfies(environment, expression, pp, oracle);
	}

}
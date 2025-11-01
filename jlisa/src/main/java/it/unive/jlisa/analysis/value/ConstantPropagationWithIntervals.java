package it.unive.jlisa.analysis.value;

import it.unive.jlisa.lattices.ConstantValue;
import it.unive.jlisa.lattices.ConstantValueIntInterval;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.numeric.Interval;
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
import org.apache.commons.lang3.tuple.Pair;

public class ConstantPropagationWithIntervals implements BaseNonRelationalValueDomain<ConstantValueIntInterval> {

	private final ConstantPropagation constantPropagation = new ConstantPropagation();
	private final Interval interval = new Interval();

	@Override
	public ConstantValueIntInterval top() {
		return ConstantValueIntInterval.TOP;
	}

	@Override
	public ConstantValueIntInterval bottom() {
		return ConstantValueIntInterval.BOTTOM;
	}

	@Override
	public boolean canProcess(
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle) {
		return constantPropagation.canProcess(expression, pp, oracle) || interval.canProcess(expression, pp, oracle);
	}

	@Override
	public ConstantValueIntInterval evalNullConstant(
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return new ConstantValueIntInterval(
				constantPropagation.evalNullConstant(pp, oracle),
				interval.evalNullConstant(pp, oracle));
	}

	@Override
	public ConstantValueIntInterval evalNonNullConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return new ConstantValueIntInterval(
				constantPropagation.evalNonNullConstant(constant, pp, oracle),
				interval.evalNonNullConstant(constant, pp, oracle));
	}

	@Override
	public ConstantValueIntInterval evalUnaryExpression(
			UnaryExpression expression,
			ConstantValueIntInterval arg,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return new ConstantValueIntInterval(
				constantPropagation.evalUnaryExpression(expression, arg.getConstantValue(), pp, oracle),
				interval.evalUnaryExpression(expression, arg.getIntInterval(), pp, oracle));
	}

	@Override
	public ConstantValueIntInterval evalBinaryExpression(
			BinaryExpression expression,
			ConstantValueIntInterval left,
			ConstantValueIntInterval right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return new ConstantValueIntInterval(
				constantPropagation.evalBinaryExpression(expression, left.getConstantValue(), right.getConstantValue(),
						pp, oracle),
				interval.evalBinaryExpression(expression, left.getIntInterval(), right.getIntInterval(), pp, oracle));
	}

	@Override
	public ConstantValueIntInterval evalTernaryExpression(
			TernaryExpression expression,
			ConstantValueIntInterval left,
			ConstantValueIntInterval middle,
			ConstantValueIntInterval right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return new ConstantValueIntInterval(
				constantPropagation.evalTernaryExpression(expression, left.getConstantValue(),
						middle.getConstantValue(), right.getConstantValue(), pp, oracle),
				interval.evalTernaryExpression(expression, left.getIntInterval(), middle.getIntInterval(),
						right.getIntInterval(), pp, oracle));
	}

	@Override
	public ConstantValueIntInterval evalValueExpression(
			ValueExpression expression,
			ConstantValueIntInterval[] subExpressions,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		ConstantValue[] constantValues = Arrays.stream(subExpressions)
				.map(ConstantValueIntInterval::getConstantValue)
				.toArray(ConstantValue[]::new);

		IntInterval[] intIntervals = Arrays.stream(subExpressions)
				.map(ConstantValueIntInterval::getIntInterval)
				.toArray(IntInterval[]::new);

		return new ConstantValueIntInterval(
				constantPropagation.evalValueExpression(expression, constantValues, pp, oracle),
				interval.evalValueExpression(expression, intIntervals, pp, oracle));
	}

	@Override
	public ConstantValueIntInterval evalPushAny(
			PushAny pushAny,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return new ConstantValueIntInterval(
				constantPropagation.evalPushAny(pushAny, pp, oracle),
				interval.evalPushAny(pushAny, pp, oracle));
	}

	@Override
	public ConstantValueIntInterval evalPushInv(
			PushInv pushInv,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return new ConstantValueIntInterval(
				constantPropagation.evalPushInv(pushInv, pp, oracle),
				interval.evalPushInv(pushInv, pp, oracle));
	}

	@Override
	public ConstantValueIntInterval evalSkip(
			Skip skip,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return new ConstantValueIntInterval(
				constantPropagation.evalSkip(skip, pp, oracle),
				interval.evalSkip(skip, pp, oracle));
	}

	@Override
	public ConstantValueIntInterval evalTypeCast(
			BinaryExpression cast,
			ConstantValueIntInterval left,
			ConstantValueIntInterval right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return new ConstantValueIntInterval(
				constantPropagation.evalTypeCast(cast, left.getConstantValue(), right.getConstantValue(), pp, oracle),
				interval.evalTypeCast(cast, left.getIntInterval(), right.getIntInterval(), pp, oracle));
	}

	@Override
	public ConstantValueIntInterval evalTypeConv(
			BinaryExpression conv,
			ConstantValueIntInterval left,
			ConstantValueIntInterval right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return new ConstantValueIntInterval(
				constantPropagation.evalTypeConv(conv, left.getConstantValue(), right.getConstantValue(), pp, oracle),
				interval.evalTypeConv(conv, left.getIntInterval(), right.getIntInterval(), pp, oracle));
	}

	@Override
	public Satisfiability satisfiesAbstractValue(
			ConstantValueIntInterval value,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		Satisfiability constantPropSatisfiability = constantPropagation.satisfiesAbstractValue(value.getConstantValue(),
				pp, oracle);
		if (constantPropSatisfiability == Satisfiability.UNKNOWN
				|| constantPropSatisfiability == Satisfiability.BOTTOM) {
			Satisfiability intervalSatisfiability = interval.satisfiesAbstractValue(value.getIntInterval(), pp, oracle);
			if (intervalSatisfiability == Satisfiability.SATISFIED) {
				return Satisfiability.SATISFIED;
			}
		}
		return constantPropSatisfiability;
	}

	@Override
	public Satisfiability satisfiesNullConstant(
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		Satisfiability constantPropSatisfiability = constantPropagation.satisfiesNullConstant(pp, oracle);
		if (constantPropSatisfiability == Satisfiability.UNKNOWN
				|| constantPropSatisfiability == Satisfiability.BOTTOM) {
			Satisfiability intervalSatisfiability = interval.satisfiesNullConstant(pp, oracle);
			if (intervalSatisfiability == Satisfiability.SATISFIED) {
				return Satisfiability.SATISFIED;
			}
		}
		return constantPropSatisfiability;
	}

	@Override
	public Satisfiability satisfiesNonNullConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		Satisfiability constantPropSatisfiability = constantPropagation.satisfiesNonNullConstant(constant, pp, oracle);
		if (constantPropSatisfiability == Satisfiability.UNKNOWN
				|| constantPropSatisfiability == Satisfiability.BOTTOM) {
			Satisfiability intervalSatisfiability = interval.satisfiesNonNullConstant(constant, pp, oracle);
			if (intervalSatisfiability == Satisfiability.SATISFIED) {
				return Satisfiability.SATISFIED;
			}
		}
		return constantPropSatisfiability;
	}

	@Override
	public Satisfiability satisfiesUnaryExpression(
			UnaryExpression expression,
			ConstantValueIntInterval arg,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		Satisfiability constantPropSatisfiability = constantPropagation.satisfiesUnaryExpression(expression,
				arg.getConstantValue(), pp, oracle);
		if (constantPropSatisfiability == Satisfiability.UNKNOWN
				|| constantPropSatisfiability == Satisfiability.BOTTOM) {
			Satisfiability intervalSatisfiability = interval.satisfiesUnaryExpression(expression, arg.getIntInterval(),
					pp, oracle);
			if (intervalSatisfiability == Satisfiability.SATISFIED) {
				return Satisfiability.SATISFIED;
			}
		}
		return constantPropSatisfiability;
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(
			BinaryExpression expression,
			ConstantValueIntInterval left,
			ConstantValueIntInterval right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		Satisfiability constantPropSatisfiability = constantPropagation.satisfiesBinaryExpression(expression,
				left.getConstantValue(), right.getConstantValue(), pp, oracle);
		if (constantPropSatisfiability == Satisfiability.UNKNOWN
				|| constantPropSatisfiability == Satisfiability.BOTTOM) {
			Satisfiability intervalSatisfiability = interval.satisfiesBinaryExpression(expression,
					left.getIntInterval(), right.getIntInterval(), pp, oracle);
			if (intervalSatisfiability == Satisfiability.SATISFIED) {
				return Satisfiability.SATISFIED;
			}
		}
		return constantPropSatisfiability;
	}

	@Override
	public Satisfiability satisfiesTernaryExpression(
			TernaryExpression expression,
			ConstantValueIntInterval left,
			ConstantValueIntInterval middle,
			ConstantValueIntInterval right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		Satisfiability constantPropSatisfiability = constantPropagation.satisfiesTernaryExpression(expression,
				left.getConstantValue(), middle.getConstantValue(), right.getConstantValue(), pp, oracle);
		if (constantPropSatisfiability == Satisfiability.UNKNOWN
				|| constantPropSatisfiability == Satisfiability.BOTTOM) {
			Satisfiability intervalSatisfiability = interval.satisfiesTernaryExpression(expression,
					left.getIntInterval(), middle.getIntInterval(), right.getIntInterval(), pp, oracle);
			if (intervalSatisfiability == Satisfiability.SATISFIED) {
				return Satisfiability.SATISFIED;
			}
		}
		return constantPropSatisfiability;
	}

	@Override
	public ValueEnvironment<ConstantValueIntInterval> assume(
			ValueEnvironment<ConstantValueIntInterval> environment,
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
	public ValueEnvironment<ConstantValueIntInterval> assumeConstant(
			ValueEnvironment<ConstantValueIntInterval> environment,
			Constant expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		Pair<ValueEnvironment<ConstantValue>,
				ValueEnvironment<IntInterval>> environments = splitEnvironment(environment);
		ValueEnvironment<ConstantValue> constantValueEnvironment = constantPropagation
				.assumeConstant(environments.getLeft(), expression, src, dest, oracle);
		ValueEnvironment<IntInterval> intIntervalEnvironment = interval.assumeConstant(environments.getRight(),
				expression, src, dest, oracle);
		return mergeEnvironments(environment, constantValueEnvironment, intIntervalEnvironment);
	}

	@Override
	public ValueEnvironment<ConstantValueIntInterval> assumeIdentifier(
			ValueEnvironment<ConstantValueIntInterval> environment,
			Identifier expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		Pair<ValueEnvironment<ConstantValue>,
				ValueEnvironment<IntInterval>> environments = splitEnvironment(environment);
		ValueEnvironment<ConstantValue> constantValueEnvironment = constantPropagation
				.assumeIdentifier(environments.getLeft(), expression, src, dest, oracle);
		ValueEnvironment<IntInterval> intIntervalEnvironment = interval.assumeIdentifier(environments.getRight(),
				expression, src, dest, oracle);
		return mergeEnvironments(environment, constantValueEnvironment, intIntervalEnvironment);
	}

	@Override
	public ValueEnvironment<ConstantValueIntInterval> assumeUnaryExpression(
			ValueEnvironment<ConstantValueIntInterval> environment,
			UnaryExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		Pair<ValueEnvironment<ConstantValue>,
				ValueEnvironment<IntInterval>> environments = splitEnvironment(environment);
		ValueEnvironment<ConstantValue> constantValueEnvironment = constantPropagation
				.assumeUnaryExpression(environments.getLeft(), expression, src, dest, oracle);
		ValueEnvironment<IntInterval> intIntervalEnvironment = interval.assumeUnaryExpression(environments.getRight(),
				expression, src, dest, oracle);
		return mergeEnvironments(environment, constantValueEnvironment, intIntervalEnvironment);
	}

	@Override
	public ValueEnvironment<ConstantValueIntInterval> assumeBinaryExpression(
			ValueEnvironment<ConstantValueIntInterval> environment,
			BinaryExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		Pair<ValueEnvironment<ConstantValue>,
				ValueEnvironment<IntInterval>> environments = splitEnvironment(environment);
		ValueEnvironment<ConstantValue> constantValueEnvironment = constantPropagation
				.assumeBinaryExpression(environments.getLeft(), expression, src, dest, oracle);
		ValueEnvironment<IntInterval> intIntervalEnvironment = interval.assumeBinaryExpression(environments.getRight(),
				expression, src, dest, oracle);
		return mergeEnvironments(environment, constantValueEnvironment, intIntervalEnvironment);
	}

	@Override
	public ValueEnvironment<ConstantValueIntInterval> assumeTernaryExpression(
			ValueEnvironment<ConstantValueIntInterval> environment,
			TernaryExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		Pair<ValueEnvironment<ConstantValue>,
				ValueEnvironment<IntInterval>> environments = splitEnvironment(environment);
		ValueEnvironment<ConstantValue> constantValueEnvironment = constantPropagation
				.assumeTernaryExpression(environments.getLeft(), expression, src, dest, oracle);
		ValueEnvironment<IntInterval> intIntervalEnvironment = interval.assumeTernaryExpression(environments.getRight(),
				expression, src, dest, oracle);
		return mergeEnvironments(environment, constantValueEnvironment, intIntervalEnvironment);
	}

	@Override
	public ValueEnvironment<ConstantValueIntInterval> assumeValueExpression(
			ValueEnvironment<ConstantValueIntInterval> environment,
			ValueExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		Pair<ValueEnvironment<ConstantValue>,
				ValueEnvironment<IntInterval>> environments = splitEnvironment(environment);
		ValueEnvironment<ConstantValue> constantValueEnvironment = constantPropagation
				.assumeValueExpression(environments.getLeft(), expression, src, dest, oracle);
		ValueEnvironment<IntInterval> intIntervalEnvironment = interval.assumeValueExpression(environments.getRight(),
				expression, src, dest, oracle);
		return mergeEnvironments(environment, constantValueEnvironment, intIntervalEnvironment);
	}

	public static Pair<ValueEnvironment<ConstantValue>, ValueEnvironment<IntInterval>>

			splitEnvironment(
					ValueEnvironment<ConstantValueIntInterval> environment)
					throws SemanticException {

		ValueEnvironment<ConstantValue> constantValueEnvironment = new ValueEnvironment<>(ConstantValue.BOTTOM);
		ValueEnvironment<IntInterval> intIntervalValueEnvironment = new ValueEnvironment<>(IntInterval.BOTTOM);

		for (Identifier id : environment.getKeys()) {
			ConstantValueIntInterval value = environment.getState(id);
			if (value == null)
				continue;

			ConstantValue constant = value.getConstantValue();
			IntInterval interval = value.getIntInterval();

			constantValueEnvironment = constantValueEnvironment.putState(id, constant);
			intIntervalValueEnvironment = intIntervalValueEnvironment.putState(id, interval);
		}

		return Pair.of(constantValueEnvironment, intIntervalValueEnvironment);
	}

	public static ValueEnvironment<ConstantValueIntInterval> mergeEnvironments(
			ValueEnvironment<ConstantValueIntInterval> oldEnvironment,
			ValueEnvironment<ConstantValue> constantEnv,
			ValueEnvironment<IntInterval> intervalEnv)
			throws SemanticException {

		ValueEnvironment<ConstantValueIntInterval> merged = new ValueEnvironment<>(ConstantValueIntInterval.BOTTOM);

		Set<Identifier> allIds = new HashSet<>();
		allIds.addAll(constantEnv.getKeys());
		allIds.addAll(intervalEnv.getKeys());

		for (Identifier id : allIds) {
			ConstantValue constVal = constantEnv.getState(id);
			IntInterval intVal = intervalEnv.getState(id);
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

			ConstantValueIntInterval combined = new ConstantValueIntInterval(constVal, intVal);
			merged = merged.putState(id, combined);
		}
		return merged;
	}

}
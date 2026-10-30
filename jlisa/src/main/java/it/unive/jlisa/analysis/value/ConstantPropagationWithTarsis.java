package it.unive.jlisa.analysis.value;

import it.unive.jlisa.lattices.ConstantValue;
import it.unive.jlisa.lattices.ConstantValueIntInterval;
import it.unive.jlisa.lattices.ConstantValueIntIntervalAutomaton;
import it.unive.jlisa.program.operator.JavaStringAppendBooleanOperator;
import it.unive.jlisa.program.operator.JavaStringAppendCharOperator;
import it.unive.jlisa.program.operator.JavaStringAppendIntOperator;
import it.unive.jlisa.program.operator.JavaStringAppendLongOperator;
import it.unive.jlisa.program.operator.JavaStringCharAtOperator;
import it.unive.jlisa.program.operator.JavaStringIndexOfCharOperator;
import it.unive.jlisa.program.operator.JavaStringIndexOfOperator;
import it.unive.jlisa.program.operator.JavaStringLengthOperator;
import it.unive.jlisa.program.operator.JavaStringReplaceOperator;
import it.unive.jlisa.program.operator.JavaStringSubstringFromToOperator;
import it.unive.jlisa.program.operator.JavaStringSubstringOperator;
import it.unive.jlisa.program.operator.JavaStringValueOfBooleanOperator;
import it.unive.jlisa.program.operator.JavaStringValueOfCharOperator;
import it.unive.jlisa.program.operator.JavaStringValueOfIntOperator;
import it.unive.jlisa.program.operator.JavaStringValueOfLongOperator;
import it.unive.jlisa.program.operator.NaryExpression;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.lattices.string.tarsis.RegexAutomaton;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Operator;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.PushInv;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.binary.LogicalAnd;
import it.unive.lisa.symbolic.value.operator.binary.LogicalOr;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.numeric.MathNumberConversionException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * The reduced product of {@link ConstantPropagationWithIntervals} and of
 * {@link JavaTarsis}. The former tracks every value, the latter the values of
 * strings. The two exchange information on the string operations: a constant
 * string is a single-string automaton (and vice versa), the lengths, the
 * indexes and the characters of strings refine the intervals, the intervals of
 * indexes refine the substrings, and the predicates on strings (equals,
 * startsWith, endsWith, contains) are decided and assumed on automata.
 */
public class ConstantPropagationWithTarsis
		implements
		BaseNonRelationalValueDomain<ConstantValueIntIntervalAutomaton> {

	private final ConstantPropagationWithIntervals values = new ConstantPropagationWithIntervals();

	private final JavaTarsis strings = new JavaTarsis();

	@Override
	public ConstantValueIntIntervalAutomaton top() {
		return ConstantValueIntIntervalAutomaton.TOP;
	}

	@Override
	public ConstantValueIntIntervalAutomaton bottom() {
		return ConstantValueIntIntervalAutomaton.BOTTOM;
	}

	@Override
	public boolean canProcess(
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle) {
		return values.canProcess(expression, pp, oracle);
	}

	/**
	 * Yields whether the given expression is a string, that is, the automaton
	 * of its value is meaningful.
	 */
	private static boolean isString(
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		Type type = expression.getStaticType();
		if (type.isStringType())
			return true;
		if (!type.isUntyped())
			return false;
		Set<Type> types = oracle.getRuntimeTypesOf(expression, pp);
		return !types.isEmpty() && types.stream().allMatch(Type::isStringType);
	}

	/**
	 * Builds the value of the given expression from the two components,
	 * exchanging the information on strings between them.
	 */
	private ConstantValueIntIntervalAutomaton reduce(
			ValueExpression expression,
			ConstantValueIntInterval value,
			RegexAutomaton automaton,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (value.isBottom())
			return bottom();
		if (!isString(expression, pp, oracle))
			return new ConstantValueIntIntervalAutomaton(value, RegexAutomaton.TOP);
		if (automaton.isBottom())
			return bottom();
		ConstantValue constant = value.getConstantValue();
		if (!constant.isTop() && constant.getValue() instanceof String s)
			return new ConstantValueIntIntervalAutomaton(value, strings.constant(s));
		String single = JavaTarsis.singleString(automaton);
		if (single != null)
			value = values.evalConstant(
					new Constant(pp.getProgram().getTypes().getStringType(), single, expression.getCodeLocation()),
					pp, oracle);
		return new ConstantValueIntIntervalAutomaton(value, automaton);
	}

	/**
	 * Refines the interval of the given integer value with the given interval,
	 * if any.
	 */
	private ConstantValueIntInterval refineInteger(
			ValueExpression expression,
			ConstantValueIntInterval value,
			IntInterval info,
			boolean exactConstant,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (info == null || info.isBottom() || value.isBottom())
			return value;
		IntInterval refined = value.getIntInterval().glb(info);
		if (refined.isBottom())
			// the components disagree: we keep the first one
			return value;
		if (exactConstant && refined.isSingleton())
			try {
				return values.evalConstant(new Constant(expression.getStaticType(), refined.getLow().toInt(),
						expression.getCodeLocation()), pp, oracle);
			} catch (MathNumberConversionException e) {
				// we keep the interval
			}
		return new ConstantValueIntInterval(value.getConstantValue(), refined);
	}

	@Override
	public ConstantValueIntIntervalAutomaton evalConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return reduce(constant, values.evalConstant(constant, pp, oracle),
				strings.evalConstant(constant, pp, oracle), pp, oracle);
	}

	@Override
	public ConstantValueIntIntervalAutomaton evalUnaryExpression(
			UnaryExpression expression,
			ConstantValueIntIntervalAutomaton arg,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		ConstantValueIntInterval value = values.evalUnaryExpression(expression, arg.getValue(), pp, oracle);
		Operator operator = expression.getOperator();
		if (operator == JavaStringLengthOperator.INSTANCE && isString(expression.getExpression(), pp, oracle))
			value = refineInteger(expression, value, strings.length(arg.getAutomaton()), true, pp, oracle);

		RegexAutomaton automaton = RegexAutomaton.TOP;
		if (isString(expression, pp, oracle)) {
			IntInterval argInterval = arg.getValue().getIntInterval();
			if (operator == JavaStringValueOfIntOperator.INSTANCE
					|| operator == JavaStringValueOfLongOperator.INSTANCE)
				automaton = strings.integers(argInterval);
			else if (operator == JavaStringValueOfCharOperator.INSTANCE)
				automaton = strings.chars(argInterval);
			else if (operator == JavaStringValueOfBooleanOperator.INSTANCE)
				automaton = booleans(arg.getValue());
			else
				automaton = strings.evalUnaryExpression(expression, arg.getAutomaton(), pp, oracle);
		}
		return reduce(expression, value, automaton, pp, oracle);
	}

	@Override
	public ConstantValueIntIntervalAutomaton evalBinaryExpression(
			BinaryExpression expression,
			ConstantValueIntIntervalAutomaton left,
			ConstantValueIntIntervalAutomaton right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		ConstantValueIntInterval value = values.evalBinaryExpression(expression, left.getValue(), right.getValue(),
				pp, oracle);
		BinaryOperator operator = expression.getOperator();
		boolean stringLeft = isString(expression.getLeft(), pp, oracle);
		boolean stringRight = isString(expression.getRight(), pp, oracle);

		if (JavaTarsis.isPredicate(operator) && stringLeft && stringRight && !value.isBottom()
				&& value.getConstantValue().isTop()) {
			Satisfiability sat = strings.satisfiesPredicate(operator, left.getAutomaton(), right.getAutomaton());
			if (sat == Satisfiability.SATISFIED || sat == Satisfiability.NOT_SATISFIED)
				value = values.evalConstant(new Constant(expression.getStaticType(), sat == Satisfiability.SATISFIED,
						expression.getCodeLocation()), pp, oracle);
		} else if (operator == JavaStringCharAtOperator.INSTANCE && stringLeft)
			// characters are kept as intervals of codes
			value = refineInteger(expression, value,
					strings.charAt(left.getAutomaton(), right.getValue().getIntInterval()), false, pp, oracle);
		else if (operator == JavaStringIndexOfOperator.INSTANCE && stringLeft && stringRight)
			value = refineInteger(expression, value, strings.indexOf(left.getAutomaton(), right.getAutomaton()), true,
					pp, oracle);
		else if (operator == JavaStringIndexOfCharOperator.INSTANCE && stringLeft)
			value = refineInteger(expression, value,
					strings.indexOf(left.getAutomaton(), strings.chars(right.getValue().getIntInterval())), true, pp,
					oracle);

		RegexAutomaton automaton = RegexAutomaton.TOP;
		if (isString(expression, pp, oracle) && stringLeft) {
			IntInterval rightInterval = right.getValue().getIntInterval();
			if (operator == JavaStringSubstringOperator.INSTANCE)
				automaton = strings.substring(left.getAutomaton(), rightInterval, null);
			else if (operator == JavaStringAppendCharOperator.INSTANCE)
				automaton = left.getAutomaton().concat(strings.chars(rightInterval));
			else if (operator == JavaStringAppendIntOperator.INSTANCE
					|| operator == JavaStringAppendLongOperator.INSTANCE)
				automaton = left.getAutomaton().concat(strings.integers(rightInterval));
			else if (operator == JavaStringAppendBooleanOperator.INSTANCE)
				automaton = left.getAutomaton().concat(booleans(right.getValue()));
			else
				automaton = strings.evalBinaryExpression(expression, left.getAutomaton(), right.getAutomaton(), pp,
						oracle);
		}
		return reduce(expression, value, automaton, pp, oracle);
	}

	@Override
	public ConstantValueIntIntervalAutomaton evalTernaryExpression(
			TernaryExpression expression,
			ConstantValueIntIntervalAutomaton left,
			ConstantValueIntIntervalAutomaton middle,
			ConstantValueIntIntervalAutomaton right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		ConstantValueIntInterval value = values.evalTernaryExpression(expression, left.getValue(),
				middle.getValue(), right.getValue(), pp, oracle);
		RegexAutomaton automaton = RegexAutomaton.TOP;
		if (isString(expression, pp, oracle) && isString(expression.getLeft(), pp, oracle)) {
			Operator operator = expression.getOperator();
			if (operator == JavaStringSubstringFromToOperator.INSTANCE)
				automaton = strings.substring(left.getAutomaton(), middle.getValue().getIntInterval(),
						right.getValue().getIntInterval());
			else if (operator == JavaStringReplaceOperator.INSTANCE)
				automaton = strings.replace(left.getAutomaton(),
						stringOrChar(expression.getMiddle(), middle, pp, oracle),
						stringOrChar(expression.getRight(), right, pp, oracle));
			else
				automaton = strings.evalTernaryExpression(expression, left.getAutomaton(), middle.getAutomaton(),
						right.getAutomaton(), pp, oracle);
		}
		return reduce(expression, value, automaton, pp, oracle);
	}

	private RegexAutomaton stringOrChar(
			SymbolicExpression expression,
			ConstantValueIntIntervalAutomaton value,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (isString(expression, pp, oracle))
			return value.getAutomaton();
		return strings.chars(value.getValue().getIntInterval());
	}

	private RegexAutomaton booleans(
			ConstantValueIntInterval value) {
		ConstantValue constant = value.getConstantValue();
		SortedSet<String> result = new TreeSet<>();
		if (!constant.isTop() && !constant.isBottom() && constant.getValue() instanceof Boolean b)
			result.add(String.valueOf(b));
		else {
			result.add("true");
			result.add("false");
		}
		return JavaTarsis.fromLanguage(result);
	}

	@Override
	public ConstantValueIntIntervalAutomaton evalValueExpression(
			ValueExpression expression,
			ConstantValueIntIntervalAutomaton[] subExpressions,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		ConstantValueIntInterval[] subValues = new ConstantValueIntInterval[subExpressions.length];
		for (int i = 0; i < subExpressions.length; i++)
			subValues[i] = subExpressions[i].getValue();
		return reduce(expression, values.evalValueExpression(expression, subValues, pp, oracle), RegexAutomaton.TOP,
				pp, oracle);
	}

	@Override
	public ConstantValueIntIntervalAutomaton evalPushAny(
			PushAny pushAny,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return reduce(pushAny, values.evalPushAny(pushAny, pp, oracle), RegexAutomaton.TOP, pp, oracle);
	}

	@Override
	public ConstantValueIntIntervalAutomaton evalPushInv(
			PushInv pushInv,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		ConstantValueIntInterval value = values.evalPushInv(pushInv, pp, oracle);
		return value.isBottom() ? bottom() : new ConstantValueIntIntervalAutomaton(value, RegexAutomaton.TOP);
	}

	@Override
	public ConstantValueIntIntervalAutomaton evalSkip(
			Skip skip,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		ConstantValueIntInterval value = values.evalSkip(skip, pp, oracle);
		return value.isBottom() ? bottom() : new ConstantValueIntIntervalAutomaton(value, RegexAutomaton.TOP);
	}

	@Override
	public ConstantValueIntIntervalAutomaton evalTypeCast(
			BinaryExpression cast,
			ConstantValueIntIntervalAutomaton left,
			ConstantValueIntIntervalAutomaton right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return reduce(cast, values.evalTypeCast(cast, left.getValue(), right.getValue(), pp, oracle),
				isString(cast.getLeft(), pp, oracle) ? left.getAutomaton() : RegexAutomaton.TOP, pp, oracle);
	}

	@Override
	public ConstantValueIntIntervalAutomaton evalTypeConv(
			BinaryExpression conv,
			ConstantValueIntIntervalAutomaton left,
			ConstantValueIntIntervalAutomaton right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return reduce(conv, values.evalTypeConv(conv, left.getValue(), right.getValue(), pp, oracle),
				isString(conv.getLeft(), pp, oracle) ? left.getAutomaton() : RegexAutomaton.TOP, pp, oracle);
	}

	@Override
	public Satisfiability satisfiesAbstractValue(
			ConstantValueIntIntervalAutomaton value,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return values.satisfiesAbstractValue(value.getValue(), pp, oracle);
	}

	@Override
	public Satisfiability satisfiesConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return values.satisfiesConstant(constant, pp, oracle);
	}

	@Override
	public Satisfiability satisfiesUnaryExpression(
			UnaryExpression expression,
			ConstantValueIntIntervalAutomaton arg,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return values.satisfiesUnaryExpression(expression, arg.getValue(), pp, oracle);
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(
			BinaryExpression expression,
			ConstantValueIntIntervalAutomaton left,
			ConstantValueIntIntervalAutomaton right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		Satisfiability sat = values.satisfiesBinaryExpression(expression, left.getValue(), right.getValue(), pp,
				oracle);
		if (sat == Satisfiability.SATISFIED || sat == Satisfiability.NOT_SATISFIED)
			return sat;
		BinaryOperator operator = expression.getOperator();
		if (JavaTarsis.isPredicate(operator) && isString(expression.getLeft(), pp, oracle)
				&& isString(expression.getRight(), pp, oracle)) {
			Satisfiability decided = strings.satisfiesPredicate(operator, left.getAutomaton(), right.getAutomaton());
			if (decided == Satisfiability.SATISFIED || decided == Satisfiability.NOT_SATISFIED)
				return decided;
		}
		return sat;
	}

	@Override
	public Satisfiability satisfiesTernaryExpression(
			TernaryExpression expression,
			ConstantValueIntIntervalAutomaton left,
			ConstantValueIntIntervalAutomaton middle,
			ConstantValueIntIntervalAutomaton right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return values.satisfiesTernaryExpression(expression, left.getValue(), middle.getValue(), right.getValue(),
				pp, oracle);
	}

	@Override
	public Satisfiability satisfies(
			ValueEnvironment<ConstantValueIntIntervalAutomaton> environment,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (expression instanceof NaryExpression)
			// handled by the first component only (see
			// ConstantPropagationWithIntervals)
			return values.satisfies(project(environment), expression, pp, oracle);
		return BaseNonRelationalValueDomain.super.satisfies(environment, expression, pp, oracle);
	}

	@Override
	public ValueEnvironment<ConstantValueIntIntervalAutomaton> assume(
			ValueEnvironment<ConstantValueIntIntervalAutomaton> environment,
			ValueExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		if (environment.isBottom())
			return environment;
		Satisfiability sat = satisfies(environment, expression, src, oracle);
		if (sat == Satisfiability.NOT_SATISFIED)
			return environment.bottom();
		if (sat == Satisfiability.SATISFIED)
			return environment;

		ValueExpression e = expression.removeNegations();
		// the first component assumes the expression where the integer string
		// operations are replaced by their bounds (the expression implies
		// the rewritten one, so this is sound)
		ValueEnvironment<ConstantValueIntInterval> assumed = values.assume(project(environment),
				bound(environment, e, src, oracle), src, dest, oracle);
		if (assumed.isBottom())
			return environment.bottom();

		Map<Identifier, RegexAutomaton> refined = new HashMap<>();
		if (!refineStrings(environment, e, refined, src, oracle)
				|| !refineLengths(environment, e, refined, src, oracle))
			return environment.bottom();

		ValueEnvironment<ConstantValueIntIntervalAutomaton> result = environment;
		Set<Identifier> ids = new HashSet<>(assumed.getKeys());
		ids.addAll(refined.keySet());
		for (Identifier id : ids) {
			ConstantValueIntIntervalAutomaton old = environment.getState(id);
			ConstantValueIntInterval value = assumed.getKeys().contains(id) ? assumed.getState(id)
					: old.getValue();
			if (!refined.containsKey(id) && value.equals(old.getValue()))
				continue;
			RegexAutomaton automaton = refined.containsKey(id) ? refined.get(id) : old.getAutomaton();
			ConstantValueIntIntervalAutomaton updated = reduce(id, value, automaton, src, oracle);
			if (updated.isBottom())
				return environment.bottom();
			result = result.putState(id, updated);
		}
		return result;
	}

	/**
	 * Replaces, inside the comparisons of {@code expression} (and of its
	 * conjunctions and disjunctions), the integer string operations (e.g., the
	 * length of a string) with their bounds, as computed by this domain.
	 */
	private ValueExpression bound(
			ValueEnvironment<ConstantValueIntIntervalAutomaton> environment,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (!(expression instanceof BinaryExpression binary))
			return expression;
		BinaryOperator operator = binary.getOperator();
		ValueExpression left = (ValueExpression) binary.getLeft();
		ValueExpression right = (ValueExpression) binary.getRight();
		ValueExpression newLeft, newRight;
		if (operator == LogicalAnd.INSTANCE || operator == LogicalOr.INSTANCE) {
			newLeft = bound(environment, left, pp, oracle);
			newRight = bound(environment, right, pp, oracle);
		} else if (operator == ComparisonLt.INSTANCE || operator == ComparisonLe.INSTANCE) {
			// left < right: the left side is at least its lower bound, the
			// right side at most its upper bound
			newLeft = boundOperand(environment, left, false, pp, oracle);
			newRight = boundOperand(environment, right, true, pp, oracle);
		} else if (operator == ComparisonGt.INSTANCE || operator == ComparisonGe.INSTANCE) {
			newLeft = boundOperand(environment, left, true, pp, oracle);
			newRight = boundOperand(environment, right, false, pp, oracle);
		} else
			return expression;
		if (newLeft == left && newRight == right)
			return expression;
		return new BinaryExpression(binary.getStaticType(), newLeft, newRight, operator, binary.getCodeLocation());
	}

	private ValueExpression boundOperand(
			ValueEnvironment<ConstantValueIntIntervalAutomaton> environment,
			ValueExpression operand,
			boolean upper,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		Operator operator = operand instanceof UnaryExpression unary ? unary.getOperator()
				: operand instanceof BinaryExpression binary ? binary.getOperator() : null;
		if (operator != JavaStringLengthOperator.INSTANCE && operator != JavaStringIndexOfOperator.INSTANCE
				&& operator != JavaStringIndexOfCharOperator.INSTANCE)
			return operand;
		IntInterval interval = eval(environment, operand, pp, oracle).getValue().getIntInterval();
		if (interval.isBottom() || interval.isTop())
			return operand;
		try {
			if (upper && !interval.highIsPlusInfinity())
				return new Constant(operand.getStaticType(), interval.getHigh().toInt(), operand.getCodeLocation());
			if (!upper && !interval.lowIsMinusInfinity())
				return new Constant(operand.getStaticType(), interval.getLow().toInt(), operand.getCodeLocation());
		} catch (MathNumberConversionException e) {
			// the operand is kept
		}
		return operand;
	}

	/**
	 * Refines the automata of the string identifiers of the predicates on
	 * strings inside {@code expression} (and of its conjunctions and
	 * disjunctions), assuming that {@code expression} holds.
	 *
	 * @return {@code false} if the expression cannot hold
	 */
	private boolean refineStrings(
			ValueEnvironment<ConstantValueIntIntervalAutomaton> environment,
			ValueExpression expression,
			Map<Identifier, RegexAutomaton> refined,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (expression instanceof BinaryExpression binary && binary.getOperator() == LogicalAnd.INSTANCE)
			return refineStrings(environment, (ValueExpression) binary.getLeft(), refined, pp, oracle)
					&& refineStrings(environment, (ValueExpression) binary.getRight(), refined, pp, oracle);

		if (expression instanceof BinaryExpression binary && binary.getOperator() == LogicalOr.INSTANCE) {
			Map<Identifier, RegexAutomaton> left = new HashMap<>(refined);
			Map<Identifier, RegexAutomaton> right = new HashMap<>(refined);
			boolean l = refineStrings(environment, (ValueExpression) binary.getLeft(), left, pp, oracle);
			boolean r = refineStrings(environment, (ValueExpression) binary.getRight(), right, pp, oracle);
			if (!l && !r)
				return false;
			refined.clear();
			if (!l)
				refined.putAll(right);
			else if (!r)
				refined.putAll(left);
			else
				// an identifier refined by one side only keeps its value
				for (Identifier id : left.keySet())
					if (right.containsKey(id))
						refined.put(id, left.get(id).lub(right.get(id)));
			return true;
		}

		boolean positive = true;
		ValueExpression leaf = expression;
		if (expression instanceof UnaryExpression unary && unary.getOperator() == LogicalNegation.INSTANCE) {
			positive = false;
			leaf = (ValueExpression) unary.getExpression();
		}
		if (!(leaf instanceof BinaryExpression binary) || !JavaTarsis.isPredicate(binary.getOperator()))
			return true;
		ValueExpression left = (ValueExpression) binary.getLeft();
		ValueExpression right = (ValueExpression) binary.getRight();
		if (!isString(left, pp, oracle) || !isString(right, pp, oracle))
			return true;
		RegexAutomaton leftAutomaton = automatonOf(environment, refined, left, pp, oracle);
		RegexAutomaton rightAutomaton = automatonOf(environment, refined, right, pp, oracle);
		for (boolean refineLeft : new boolean[] { true, false }) {
			ValueExpression target = refineLeft ? left : right;
			if (!(target instanceof Identifier id) || !id.canBeAssigned() || id.isWeak())
				continue;
			RegexAutomaton result = strings.refine(binary.getOperator(), leftAutomaton, rightAutomaton, refineLeft,
					positive);
			if (result == null)
				continue;
			if (result.isBottom())
				return false;
			refined.put(id, result);
		}
		return true;
	}

	/**
	 * Refines the automata of the string identifiers whose length is compared
	 * inside {@code expression} (and of its conjunctions), assuming that
	 * {@code expression} holds.
	 *
	 * @return {@code false} if the expression cannot hold
	 */
	private boolean refineLengths(
			ValueEnvironment<ConstantValueIntIntervalAutomaton> environment,
			ValueExpression expression,
			Map<Identifier, RegexAutomaton> refined,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (!(expression instanceof BinaryExpression binary))
			return true;
		BinaryOperator operator = binary.getOperator();
		ValueExpression left = (ValueExpression) binary.getLeft();
		ValueExpression right = (ValueExpression) binary.getRight();
		if (operator == LogicalAnd.INSTANCE)
			return refineLengths(environment, left, refined, pp, oracle)
					&& refineLengths(environment, right, refined, pp, oracle);

		boolean lengthLeft = isLengthOf(left);
		if (!lengthLeft && !isLengthOf(right))
			return true;
		Identifier id = (Identifier) ((UnaryExpression) (lengthLeft ? left : right)).getExpression();
		IntInterval other = eval(environment, lengthLeft ? right : left, pp, oracle).getValue().getIntInterval();
		if (other.isBottom())
			return true;
		// the comparison is turned into length op other
		if (!lengthLeft)
			operator = operator == ComparisonLt.INSTANCE ? ComparisonGt.INSTANCE
					: operator == ComparisonLe.INSTANCE ? ComparisonGe.INSTANCE
							: operator == ComparisonGt.INSTANCE ? ComparisonLt.INSTANCE
									: operator == ComparisonGe.INSTANCE ? ComparisonLe.INSTANCE : operator;
		Long low = other.lowIsMinusInfinity() ? null : bound(other.getLow());
		Long high = other.highIsPlusInfinity() ? null : bound(other.getHigh());
		IntInterval length;
		if (operator == ComparisonLt.INSTANCE)
			length = interval(null, high == null ? null : high - 1);
		else if (operator == ComparisonLe.INSTANCE)
			length = interval(null, high);
		else if (operator == ComparisonGt.INSTANCE)
			length = interval(low == null ? null : low + 1, null);
		else if (operator == ComparisonGe.INSTANCE)
			length = interval(low, null);
		else if (operator == ComparisonEq.INSTANCE)
			length = interval(low, high);
		else
			return true;
		if (length == null)
			return true;
		if (length.isBottom())
			return false;
		RegexAutomaton result = strings.refineLength(automatonOf(environment, refined, id, pp, oracle), length);
		if (result == null)
			return true;
		if (result.isBottom())
			return false;
		refined.put(id, result);
		return true;
	}

	private static boolean isLengthOf(
			ValueExpression expression) {
		return expression instanceof UnaryExpression unary
				&& unary.getOperator() == JavaStringLengthOperator.INSTANCE
				&& unary.getExpression() instanceof Identifier id && id.canBeAssigned() && !id.isWeak();
	}

	private static Long bound(
			MathNumber n) {
		try {
			return n.toLong();
		} catch (MathNumberConversionException e) {
			return null;
		}
	}

	/**
	 * Yields the interval of the lengths between the given bounds (where
	 * {@code null} is unbounded), or {@code null} if it is not bounded.
	 */
	private static IntInterval interval(
			Long low,
			Long high) {
		long l = low == null ? 0 : Math.max(0, low);
		if (high != null && high < l)
			return IntInterval.BOTTOM;
		if (high == null && l == 0)
			return null;
		if (l > Integer.MAX_VALUE || (high != null && high > Integer.MAX_VALUE))
			return null;
		return new IntInterval(Integer.valueOf((int) l), high == null ? null : Integer.valueOf((int) (long) high));
	}

	private RegexAutomaton automatonOf(
			ValueEnvironment<ConstantValueIntIntervalAutomaton> environment,
			Map<Identifier, RegexAutomaton> refined,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (expression instanceof Identifier id && refined.containsKey(id))
			return refined.get(id);
		return eval(environment, expression, pp, oracle).getAutomaton();
	}

	/**
	 * Yields the environment of the first component.
	 */
	private static ValueEnvironment<ConstantValueIntInterval> project(
			ValueEnvironment<ConstantValueIntIntervalAutomaton> environment) {
		ValueEnvironment<ConstantValueIntInterval> result = new ValueEnvironment<>(ConstantValueIntInterval.TOP);
		if (environment.isBottom())
			return result.bottom();
		if (environment.isTop())
			return result.top();
		for (Identifier id : environment.getKeys())
			result = result.putState(id, environment.getState(id).getValue());
		return result;
	}

	@Override
	public Set<BinaryExpression> constraints(
			ValueDomain<?> requesting,
			ValueEnvironment<ConstantValueIntIntervalAutomaton> state,
			ValueExpression e,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return values.constraints(requesting, project(state), e, pp, oracle);
	}
}

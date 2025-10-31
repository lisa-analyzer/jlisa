package it.unive.jlisa.analysis.value;

import it.unive.jlisa.lattices.ConstantValue;
import it.unive.jlisa.lattices.SVCompDomainValue;
import it.unive.jlisa.program.operator.*;
import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.*;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import java.util.Set;

public class SVCompDomain implements BaseNonRelationalValueDomain<SVCompDomainValue> {
	ConstantPropagation constantPropagation = new ConstantPropagation();
	JavaInterval javaInterval = new JavaInterval();

	@Override
	public boolean canProcess(
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (expression instanceof PushInv)
			// the type approximation of a pushinv is bottom, so the below check
			// will always fail regardless of the kind of value we are tracking
			return expression.getStaticType().isValueType();

		Set<Type> rts = null;
		try {
			rts = oracle.getRuntimeTypesOf(expression, pp);
		} catch (SemanticException e) {
			return false;
		}

		if (rts == null || rts.isEmpty())
			// if we have no runtime types, either the type domain has no type
			// information for the given expression (thus it can be anything,
			// also something that we can track) or the computation returned
			// bottom (and the whole state is likely going to go to bottom
			// anyway).
			return true;

		return rts.stream().anyMatch(Type::isValueType) || rts.stream().anyMatch(t -> t.isStringType());
	}

	@Override
	public Satisfiability satisfiesNonNullConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return constantPropagation.satisfiesNonNullConstant(constant, pp, oracle);
	}

	@Override
	public Satisfiability satisfiesUnaryExpression(
			UnaryExpression expression,
			SVCompDomainValue arg,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (arg.getValue() != null && arg.getValue() instanceof ConstantValue cv) {
			return constantPropagation.satisfiesUnaryExpression(expression, cv, pp, oracle);
		}
		if (arg.getValue() != null && arg.getValue() instanceof IntInterval ii) {
			return javaInterval.satisfiesUnaryExpression(expression, ii, pp, oracle);
		}
		return BaseNonRelationalValueDomain.super.satisfiesUnaryExpression(expression, arg, pp, oracle);
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(
			BinaryExpression expression,
			SVCompDomainValue left,
			SVCompDomainValue right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		Satisfiability result = Satisfiability.BOTTOM;
		if (left.getValue() != null && left.getValue() instanceof ConstantValue cv1
				&& right.getValue() != null && right.getValue() instanceof ConstantValue cv2) {
			result = constantPropagation.satisfiesBinaryExpression(expression, cv1, cv2, pp, oracle);
		} else if (left.getValue() != null && left.getValue() instanceof IntInterval ii1
				&& right.getValue() != null && right.getValue() instanceof IntInterval ii2) {
			return javaInterval.satisfiesBinaryExpression(expression, ii1, ii2, pp, oracle);
		}
		// try to get the interval.
		if (result == Satisfiability.BOTTOM || result == Satisfiability.UNKNOWN) {
			IntInterval intIntervalLeft = javaInterval.from(left.getValue());
			IntInterval intIntervalRight = javaInterval.from(right.getValue());
			if (!intIntervalLeft.isBottom() && !intIntervalRight.isBottom()) {
				result = javaInterval.satisfiesBinaryExpression(expression, intIntervalLeft, intIntervalRight, pp,
						oracle);
			}
		}
		if (result == Satisfiability.BOTTOM || result == Satisfiability.UNKNOWN) {
			result = BaseNonRelationalValueDomain.super.satisfiesBinaryExpression(expression, left, right, pp, oracle);
		}
		return result;
	}

	@Override
	public Satisfiability satisfiesTernaryExpression(
			TernaryExpression expression,
			SVCompDomainValue left,
			SVCompDomainValue middle,
			SVCompDomainValue right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (left.getValue() != null && left.getValue() instanceof ConstantValue cv1
				&& middle.getValue() != null && right.getValue() != null
				&& right.getValue() instanceof ConstantValue cv2
				&& right.getValue() != null && right.getValue() instanceof ConstantValue cv3) {
			return constantPropagation.satisfiesTernaryExpression(expression, cv1, cv2, cv3, pp, oracle);
		}
		if (left.getValue() != null && left.getValue() instanceof IntInterval ii1
				&& middle.getValue() != null && right.getValue() != null && right.getValue() instanceof IntInterval ii2
				&& right.getValue() != null && right.getValue() instanceof IntInterval ii3) {
			return javaInterval.satisfiesTernaryExpression(expression, ii1, ii2, ii3, pp, oracle);
		}
		return BaseNonRelationalValueDomain.super.satisfiesTernaryExpression(expression, left, middle, right, pp,
				oracle);
	}

	@Override
	public ValueEnvironment<SVCompDomainValue> assumeBinaryExpression(
			ValueEnvironment<SVCompDomainValue> environment,
			BinaryExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		if (expression.getLeft() instanceof ValueExpression ve1
				&& expression.getRight() instanceof ValueExpression ve2) {
			SVCompDomainValue evalLeft = eval(environment, ve1, src, oracle);
			SVCompDomainValue evalRight = eval(environment, ve2, src, oracle);
			if (evalLeft.getValue() != null && evalRight.getValue() != null) {
				if (oracle.getDynamicTypeOf(ve1, src).isNumericType()
						&& oracle.getDynamicTypeOf(ve2, src).isNumericType()) {
					IntInterval leftInterval = javaInterval.from(evalLeft.getValue());
					IntInterval rightInterval = javaInterval.from(evalRight.getValue());

					Satisfiability result = javaInterval.satisfiesBinaryExpression(expression, leftInterval,
							rightInterval, dest, oracle);
					if (result == Satisfiability.NOT_SATISFIED) {
						return new ValueEnvironment<>(SVCompDomainValue.BOTTOM).bottom();
					}
					if (result == Satisfiability.UNKNOWN && expression.getOperator() instanceof ComparisonGt
							&& !leftInterval.isTop() && !rightInterval.isTop()) {
						return new ValueEnvironment<>(SVCompDomainValue.BOTTOM).bottom();
					}
				}
				// result = satisfiesBinaryExpression(expression, evalLeft,
				// evalRight, dest, oracle);
			}
		}
		// SVCompDomainValue
		return BaseNonRelationalValueDomain.super.assumeBinaryExpression(environment, expression, src, dest, oracle);
	}

	/*
	 * @Override public ValueEnvironment<SVCompDomainValue>
	 * assume(ValueEnvironment<SVCompDomainValue> environment, ValueExpression
	 * expression, ProgramPoint src, ProgramPoint dest, SemanticOracle oracle)
	 * throws SemanticException { Satisfiability sat = satisfies(environment,
	 * expression, dest, oracle); if (sat == Satisfiability.UNKNOWN) {
	 * SVCompDomainValue value = eval(environment, expression, dest, oracle); if
	 * (expression instanceof BinaryExpression) { return
	 * satisfiesBinaryExpression(expression, ) } if (expression instanceof
	 * Identifier i) { return environment.putState(i, new
	 * SVCompDomainValue(constantPropagation.top())); } return
	 * environment.bottom(); } if (sat == Satisfiability.SATISFIED) return
	 * environment; else return new
	 * ValueEnvironment<>(SVCompDomainValue.BOTTOM).bottom(); }
	 */

	@Override
	public SVCompDomainValue top() {
		return SVCompDomainValue.TOP;
	}

	@Override
	public SVCompDomainValue bottom() {
		return SVCompDomainValue.BOTTOM;
	}

	@Override
	public SVCompDomainValue evalNullConstant(
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		throw new SemanticException("null value is not handled by the SVCompDomain.");
	}

	@Override
	public SVCompDomainValue evalNonNullConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return new SVCompDomainValue(constantPropagation.evalNonNullConstant(constant, pp, oracle));
	}

	@Override
	public SVCompDomainValue evalValueExpression(
			ValueExpression expression,
			SVCompDomainValue[] subExpressions,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		ConstantValue[] constantValues = new ConstantValue[subExpressions.length];
		for (int i = 0; i < subExpressions.length; i++) {
			if (!(subExpressions[i].getValue() instanceof ConstantValue cv)) {
				return BaseNonRelationalValueDomain.super.evalValueExpression(expression, subExpressions, pp, oracle);
			} else {
				constantValues[i] = cv;
			}
		}
		return new SVCompDomainValue(constantPropagation.evalValueExpression(expression, constantValues, pp, oracle));
	}

	@Override
	public SVCompDomainValue evalUnaryExpression(
			UnaryExpression expression,
			SVCompDomainValue arg,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		// if arg is top, top is returned
		if (arg.isTop())
			return top();
		if (arg.getValue().isTop()) {
			return top();
		}

		UnaryOperator operator = expression.getOperator();
		if (arg.getValue() instanceof ConstantValue cv) {
			BaseLattice<?> result = constantPropagation.evalUnaryExpression(expression, cv, pp, oracle);
			if (result.isTop()) {
				// if constant propagation returns TOP, we will try to apply the
				// interval domain.
				if (cv.isNumeric()) {
					double value = cv.as(Number.class).doubleValue();
					IntInterval ii = new IntInterval(new MathNumber(value), new MathNumber(value));
					IntInterval resultInterval = javaInterval.evalUnaryExpression(expression, ii, pp, oracle);
					if (!resultInterval.isBottom()) {
						result = resultInterval;
					}
				}
			}
			return new SVCompDomainValue(result);
		}
		if (arg.getValue() instanceof IntInterval ii) {
			return new SVCompDomainValue(javaInterval.evalUnaryExpression(expression, ii, pp, oracle));
		}
		// char
		if (operator instanceof JavaCharacterIsLetterOperator) {
			if (arg.getValue() instanceof ConstantValue cv && cv.getValue() instanceof Integer v)
				return new SVCompDomainValue(new ConstantValue(Character.isLetter(v)));
			else if (arg.getValue() instanceof IntInterval ii) {
				if (ii.getLow().getNumber().longValue() >= 48 && ii.getHigh().getNumber().longValue() <= 57) {
					return new SVCompDomainValue(new ConstantValue(true));
				} else {
					return new SVCompDomainValue(ConstantValue.TOP);
				}

			}
		}
		return top();
	}

	@Override
	public SVCompDomainValue evalTypeConv(
			BinaryExpression conv,
			SVCompDomainValue left,
			SVCompDomainValue right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (left != null && right != null && left.getValue() instanceof ConstantValue cv1
				&& right.getValue() instanceof ConstantValue cv2) {
			return new SVCompDomainValue(constantPropagation.evalTypeConv(conv, cv1, cv2, pp, oracle));
		}
		return BaseNonRelationalValueDomain.super.evalTypeConv(conv, left, right, pp, oracle);
	}

	@Override
	public SVCompDomainValue evalBinaryExpression(
			BinaryExpression expression,
			SVCompDomainValue left,
			SVCompDomainValue right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		// if left or right is top, top is returned
		if (left.isTop() || right.isTop() || left.getValue().isTop() || right.getValue().isTop())
			return top();
		BaseLattice<?> result = bottom();
		if (left.getValue() instanceof ConstantValue cv1 && right.getValue() instanceof ConstantValue cv2) {
			// both are constant value.
			result = constantPropagation.evalBinaryExpression(expression, cv1, cv2, pp, oracle);
			/*
			 * if (result.isTop()) { // if the result is TOP, we will try with
			 * Interval. if (cv1.isNumeric() && cv2.isNumeric()) { double
			 * leftValue = cv1.as(Number.class).doubleValue(); double rightValue
			 * = cv2.as(Number.class).doubleValue(); IntInterval leftInterval =
			 * new IntInterval(new MathNumber(Math.ceil(leftValue)), new
			 * MathNumber(Math.floor(leftValue))); IntInterval rightInterval =
			 * new IntInterval(new MathNumber(Math.ceil(rightValue)), new
			 * MathNumber(Math.floor(rightValue))); IntInterval intervalResult =
			 * javaInterval.evalBinaryExpression(expression, leftInterval,
			 * rightInterval, pp, oracle); if (!intervalResult.isBottom()) {
			 * result = intervalResult; } } else { // TODO: handle special cases
			 * (for example, binary operations involving a numeric parameter and
			 * a non-numeric constant) here. return top(); } } return new
			 * SVCompDomainValue(result);
			 */
		}
		if (result.isTop() || result.isBottom()) {
			IntInterval leftInterval = javaInterval.from(left.getValue());
			IntInterval rightInterval = javaInterval.from(right.getValue());
			IntInterval intervalResult = javaInterval.evalBinaryExpression(expression, leftInterval, rightInterval, pp,
					oracle);
			if (intervalResult.isBottom() && result.isTop()) {
				return new SVCompDomainValue(result);
			} else {
				return new SVCompDomainValue(intervalResult);
			}
		}
		return new SVCompDomainValue(result);
	}

	@Override
	public SVCompDomainValue evalTernaryExpression(
			TernaryExpression expression,
			SVCompDomainValue left,
			SVCompDomainValue middle,
			SVCompDomainValue right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (left.getValue() != null && right.getValue() != null && middle.getValue() != null
				&& left.getValue() instanceof ConstantValue cv1 && middle.getValue() instanceof ConstantValue cv2
				&& right.getValue() instanceof ConstantValue cv3) {
			return new SVCompDomainValue(
					constantPropagation.evalTernaryExpression(expression, cv1, cv2, cv3, pp, oracle));
		}
		return BaseNonRelationalValueDomain.super.evalTernaryExpression(expression, left, middle, right, pp, oracle);
	}

	@Override
	public SVCompDomainValue evalPushAny(
			PushAny pushAny,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return new SVCompDomainValue(javaInterval.evalPushAny(pushAny, pp, oracle));
	}

	@Override
	public Satisfiability satisfies(
			ValueEnvironment<SVCompDomainValue> state,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (expression instanceof NaryExpression) {
			SymbolicExpression[] exprs = ((NaryExpression) expression).getAllOperand(0);
			ConstantValue[] args = new ConstantValue[exprs.length];

			for (int i = 0; i < exprs.length; ++i) {
				SVCompDomainValue left = eval(state, (ValueExpression) exprs[i], pp, oracle);
				if (left.isBottom()) {
					return Satisfiability.BOTTOM;
				}
				if (left.getValue() != null && left.getValue() instanceof ConstantValue cv) {
					args[i] = cv;
				} else {
					// TODO: fallback to Intervals
					return BaseNonRelationalValueDomain.super.satisfies(state, expression, pp, oracle);
				}
			}

			return constantPropagation.satisfiesNaryExpression((NaryExpression) expression, args, pp, oracle);
		}

		return BaseNonRelationalValueDomain.super.satisfies(state, expression, pp, oracle);
	}

	@Override
	public Satisfiability satisfiesAbstractValue(
			SVCompDomainValue value,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (value.getValue() != null && value.getValue() instanceof ConstantValue cv) {
			return constantPropagation.satisfiesAbstractValue(cv, pp, oracle);
		} else if (value.getValue() != null && value.getValue() instanceof IntInterval ii) {
			return javaInterval.satisfiesAbstractValue(ii, pp, oracle);
		}
		return BaseNonRelationalValueDomain.super.satisfiesAbstractValue(value, pp, oracle);
	}

	@Override
	public Satisfiability satisfiesNullConstant(
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return BaseNonRelationalValueDomain.super.satisfiesNullConstant(pp, oracle);
	}

}

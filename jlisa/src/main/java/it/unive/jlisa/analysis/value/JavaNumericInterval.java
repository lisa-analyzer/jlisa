package it.unive.jlisa.analysis.value;

import it.unive.jlisa.program.operator.JavaByteCompareOperator;
import it.unive.jlisa.program.operator.JavaDoubleCompareOperator;
import it.unive.jlisa.program.operator.JavaFloatCompareOperator;
import it.unive.jlisa.program.operator.JavaIntegerCompareOperator;
import it.unive.jlisa.program.operator.JavaLongCompareOperator;
import it.unive.jlisa.program.operator.JavaLongRotateRightOperator;
import it.unive.jlisa.program.operator.JavaMathAbsOperator;
import it.unive.jlisa.program.operator.JavaMathAcosOperator;
import it.unive.jlisa.program.operator.JavaMathAsinOperator;
import it.unive.jlisa.program.operator.JavaMathAtanOperator;
import it.unive.jlisa.program.operator.JavaMathCosOperator;
import it.unive.jlisa.program.operator.JavaMathExpOperator;
import it.unive.jlisa.program.operator.JavaMathFloorOperator;
import it.unive.jlisa.program.operator.JavaMathLog10Operator;
import it.unive.jlisa.program.operator.JavaMathLogOperator;
import it.unive.jlisa.program.operator.JavaMathMax;
import it.unive.jlisa.program.operator.JavaMathMin;
import it.unive.jlisa.program.operator.JavaMathRoundOperator;
import it.unive.jlisa.program.operator.JavaMathSinOperator;
import it.unive.jlisa.program.operator.JavaMathSqrtOperator;
import it.unive.jlisa.program.operator.JavaMathTanOperator;
import it.unive.jlisa.program.operator.JavaMathToRadiansOperator;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.numeric.Interval;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.numeric.MathNumberConversionException;
import java.util.function.Function;

public class JavaNumericInterval extends Interval {

	@Override
	public IntInterval evalConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (constant.getValue() instanceof Number) {
			return fromConstant(constant);
		}
		// If the constant is not a number, return BOTTOM.
		// TOP represents any possible number, but since the constant is not
		// numeric, BOTTOM is more appropriate.
		return IntInterval.BOTTOM;
	}

	public IntInterval fromConstant(
			Constant constant) {
		double value = ((Number) constant.getValue()).doubleValue();
		if (Double.isNaN(value)) {
			return IntInterval.BOTTOM; // not a number
		}
		if (value == Double.POSITIVE_INFINITY) {
			// return new IntInterval(MathNumber.PLUS_INFINITY,
			// MathNumber.PLUS_INFINITY);
			return IntInterval.BOTTOM;
		}
		if (value == Double.NEGATIVE_INFINITY) {
			// return new IntInterval(MathNumber.MINUS_INFINITY,
			// MathNumber.MINUS_INFINITY);
			return IntInterval.BOTTOM;
		}
		return new IntInterval(new MathNumber(value), new MathNumber(value));
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(
			BinaryExpression expression,
			IntInterval left,
			IntInterval right,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (left.isTop() || right.isTop())
			return Satisfiability.UNKNOWN;
		if (left.isBottom() || right.isBottom())
			return Satisfiability.BOTTOM;

		BinaryOperator operator = expression.getOperator();
		if (operator == ComparisonEq.INSTANCE) {
			IntInterval glb = null;
			try {
				glb = left.glb(right);
			} catch (SemanticException e) {
				return Satisfiability.UNKNOWN;
			}

			if (glb.isBottom())
				return Satisfiability.NOT_SATISFIED;
			else if (left.isSingleton() && left.equals(right))
				return Satisfiability.SATISFIED;
			return Satisfiability.UNKNOWN;
		} else if (operator == ComparisonGe.INSTANCE)
			return satisfiesBinaryExpression(expression.withOperator(ComparisonLe.INSTANCE), right, left, pp, oracle);
		else if (operator == ComparisonGt.INSTANCE)
			return satisfiesBinaryExpression(expression.withOperator(ComparisonLt.INSTANCE), right, left, pp, oracle);
		else if (operator == ComparisonLe.INSTANCE) {
			IntInterval glb = null;
			try {
				glb = left.glb(right);
			} catch (SemanticException e) {
				return Satisfiability.UNKNOWN;
			}

			if (glb.isBottom())
				return Satisfiability.fromBoolean(left.getHigh().compareTo(right.getLow()) <= 0);
			// we might have a singleton as glb if the two intervals share a
			// bound
			if (glb.isSingleton() && left.getHigh().compareTo(right.getLow()) == 0)
				return Satisfiability.SATISFIED;
			return Satisfiability.UNKNOWN;
		} else if (operator == ComparisonLt.INSTANCE) {
			IntInterval glb = null;
			try {
				glb = left.glb(right);
			} catch (SemanticException e) {
				return Satisfiability.UNKNOWN;
			}

			if (glb.isBottom())
				return Satisfiability.fromBoolean(left.getHigh().compareTo(right.getLow()) < 0);
			// TODO: verify the correctness of the following condition.
			// Example: left = [0,1], right = [0,0]
			// In this case, the GLB (greatest lower bound) is [0,0].
			// Here, left is NOT less than right, since left.getLow() == 0 and
			// 0 is also the only possible value in right. Therefore, the
			// comparison should not be satisfied.
			if (glb.isSingleton() && right.getHigh().compareTo(left.getLow()) == 0)
				return Satisfiability.NOT_SATISFIED;
			return Satisfiability.UNKNOWN;
		} else if (operator == ComparisonNe.INSTANCE) {
			IntInterval glb = null;
			try {
				glb = left.glb(right);
			} catch (SemanticException e) {
				return Satisfiability.UNKNOWN;
			}
			if (glb.isBottom())
				return Satisfiability.SATISFIED;
			return Satisfiability.UNKNOWN;
		}
		return Satisfiability.UNKNOWN;
	}

	public static IntInterval trigonometric(
			IntInterval i,
			Function<Double, Double> function,
			double period) {
		if (i.isBottom())
			return i;

		if (i.lowIsMinusInfinity() || i.highIsPlusInfinity())
			// unbounded -> all values
			return new IntInterval(-1, 1);

		double a, b;
		try {
			a = i.getLow().toDouble();
			b = i.getHigh().toDouble();
		} catch (MathNumberConversionException e) {
			// this should never happen as both bounds are finite
			return IntInterval.BOTTOM;
		}

		if (b - a >= period)
			// an interval wider than the period will include all values
			return new IntInterval(-1, 1);

		// these are the coefficients of the smaller and greater multiples of pi
		// that are included in the interval
		double pi = Math.PI;
		int kStart = (int) Math.ceil(a / pi);
		int kEnd = (int) Math.floor(b / pi);

		double trig_a = function.apply(a);
		double trig_b = function.apply(b);

		// the min/max are the ones of the bounds, unless a local
		// max/min exists inside the interval: this always correspond
		// to a multiple of pi
		double min = Math.min(trig_a, trig_b);
		double max = Math.max(trig_a, trig_b);

		// we iterate over the multiples of pi inside the interval
		// to scan for local min/max
		for (int k = kStart; k <= kEnd; ++k) {
			double x = function.apply(k * pi);
			min = Math.min(min, x);
			max = Math.max(max, x);
		}

		return new IntInterval((int) Math.floor(min), (int) Math.ceil(max));
	}

	@Override
	public IntInterval evalUnaryExpression(
			UnaryExpression expression,
			IntInterval arg,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (arg.isTop() || arg.isBottom())
			return arg;

		Double l, h;
		try {
			l = arg.getLow().toDouble();
		} catch (MathNumberConversionException e) {
			l = null;
		}
		try {
			h = arg.getHigh().toDouble();
		} catch (MathNumberConversionException e) {
			h = null;
		}

		UnaryOperator operator = expression.getOperator();
		// char
		// if (operator instanceof JavaCharacterIsLetterOperator)
		// if (operator instanceof JavaCharacterIsDigitOperator)
		// if (operator instanceof JavaCharacterIsDefinedOperator)
		// if (operator instanceof JavaCharacterToLowerCaseOperator)
		// if (operator instanceof JavaCharacterToUpperCaseOperator)
		// if (operator instanceof JavaCharacterIsJavaIdentifierPartOperator)
		// if (operator instanceof JavaCharacterIsJavaIdentifierStartOperator)
		// if (operator instanceof JavaCharacterIsLetterOrDigitOperator)
		// if (operator instanceof JavaCharacterIsLowerCaseOperator)
		// if (operator instanceof JavaCharacterIsUpperCaseOperator)

		// numeric
		if (operator instanceof JavaMathSinOperator)
			return trigonometric(arg, Math::sin, 4 * Math.PI);
		if (operator instanceof JavaMathCosOperator)
			return trigonometric(arg, Math::cos, 4 * Math.PI);
		if (operator instanceof JavaMathTanOperator)
			return trigonometric(arg, Math::tan, Math.PI);

		if (operator instanceof JavaMathAsinOperator)
			if (arg.lowIsMinusInfinity() || arg.getLow().compareTo(MathNumber.MINUS_ONE) <= 0)
				if (arg.highIsPlusInfinity() || arg.getHigh().compareTo(MathNumber.ONE) >= 1)
					return new IntInterval(new MathNumber(Math.asin(-1)), new MathNumber(Math.asin(1)));
				else
					return new IntInterval(new MathNumber(Math.asin(-1)), new MathNumber(Math.asin(h)));
			else if (arg.highIsPlusInfinity() || arg.getHigh().compareTo(MathNumber.ONE) >= 1)
				return new IntInterval(new MathNumber(Math.asin(l)), new MathNumber(Math.asin(1)));
			else
				return new IntInterval(new MathNumber(Math.asin(l)), new MathNumber(Math.asin(h)));

		if (operator instanceof JavaMathAcosOperator)
			if (arg.lowIsMinusInfinity() || arg.getLow().compareTo(MathNumber.MINUS_ONE) <= 0)
				if (arg.highIsPlusInfinity() || arg.getHigh().compareTo(MathNumber.ONE) >= 1)
					return new IntInterval(new MathNumber(Math.acos(1)), new MathNumber(Math.acos(-1)));
				else
					return new IntInterval(new MathNumber(Math.acos(h)), new MathNumber(Math.acos(-1)));
			else if (arg.highIsPlusInfinity() || arg.getHigh().compareTo(MathNumber.ONE) >= 1)
				return new IntInterval(new MathNumber(Math.acos(1)), new MathNumber(Math.acos(l)));
			else
				return new IntInterval(new MathNumber(Math.acos(h)), new MathNumber(Math.acos(l)));

		if (operator instanceof JavaMathAtanOperator)
			if (arg.lowIsMinusInfinity())
				if (arg.highIsPlusInfinity())
					return IntInterval.TOP;
				else
					return new IntInterval(MathNumber.MINUS_INFINITY, new MathNumber(Math.atan(h)));
			else if (arg.highIsPlusInfinity())
				return new IntInterval(new MathNumber(Math.atan(l)), MathNumber.PLUS_INFINITY);
			else
				return new IntInterval(new MathNumber(Math.atan(l)), new MathNumber(Math.atan(h)));

		if (operator instanceof JavaMathToRadiansOperator)
			if (arg.lowIsMinusInfinity())
				if (arg.highIsPlusInfinity())
					return IntInterval.TOP;
				else
					return new IntInterval(MathNumber.MINUS_INFINITY, new MathNumber(Math.toRadians(h)));
			else if (arg.highIsPlusInfinity())
				return new IntInterval(new MathNumber(Math.toRadians(l)), MathNumber.PLUS_INFINITY);
			else
				return new IntInterval(new MathNumber(Math.toRadians(l)), new MathNumber(Math.toRadians(h)));

		if (operator instanceof JavaMathSqrtOperator)
			if (arg.lowIsMinusInfinity() || arg.getLow().compareTo(MathNumber.ZERO) <= 0)
				if (arg.getHigh().compareTo(MathNumber.ZERO) <= 0)
					return IntInterval.BOTTOM;
				else if (arg.highIsPlusInfinity())
					return new IntInterval(MathNumber.ZERO, MathNumber.PLUS_INFINITY);
				else
					return new IntInterval(MathNumber.ZERO, new MathNumber(Math.sqrt(h)));
			else if (arg.highIsPlusInfinity())
				return new IntInterval(new MathNumber(Math.sqrt(l)), MathNumber.PLUS_INFINITY);
			else
				return new IntInterval(new MathNumber(Math.sqrt(l)), new MathNumber(Math.sqrt(h)));

		if (operator instanceof JavaMathLogOperator)
			if (arg.lowIsMinusInfinity() || arg.getLow().compareTo(MathNumber.ZERO) <= 0)
				if (arg.getHigh().compareTo(MathNumber.ZERO) <= 0)
					return IntInterval.BOTTOM;
				else if (arg.highIsPlusInfinity())
					return IntInterval.TOP;
				else
					return new IntInterval(MathNumber.MINUS_INFINITY, new MathNumber(Math.log(h)));
			else if (arg.highIsPlusInfinity())
				return new IntInterval(new MathNumber(Math.log(l)), MathNumber.PLUS_INFINITY);
			else
				return new IntInterval(new MathNumber(Math.log(l)), new MathNumber(Math.log(h)));

		if (operator instanceof JavaMathLog10Operator)
			if (arg.lowIsMinusInfinity() || arg.getLow().compareTo(MathNumber.ZERO) <= 0)
				if (arg.getHigh().compareTo(MathNumber.ZERO) <= 0)
					return IntInterval.BOTTOM;
				else if (arg.highIsPlusInfinity())
					return IntInterval.TOP;
				else
					return new IntInterval(MathNumber.MINUS_INFINITY, new MathNumber(Math.log10(h)));
			else if (arg.highIsPlusInfinity())
				return new IntInterval(new MathNumber(Math.log10(l)), MathNumber.PLUS_INFINITY);
			else
				return new IntInterval(new MathNumber(Math.log10(l)), new MathNumber(Math.log10(h)));

		if (operator instanceof JavaMathExpOperator)
			if (arg.lowIsMinusInfinity())
				if (arg.highIsPlusInfinity())
					return IntInterval.TOP;
				else
					return new IntInterval(MathNumber.MINUS_INFINITY, new MathNumber(Math.exp(h)));
			else if (arg.highIsPlusInfinity())
				return new IntInterval(new MathNumber(Math.exp(l)), MathNumber.PLUS_INFINITY);
			else
				return new IntInterval(new MathNumber(Math.exp(l)), new MathNumber(Math.exp(h)));

		if (operator instanceof JavaMathFloorOperator)
			return arg;
		if (operator instanceof JavaMathRoundOperator)
			return arg;

		if (operator instanceof JavaMathAbsOperator)
			if (arg.getLow().compareTo(MathNumber.ZERO) >= 0)
				return arg;
			else if (arg.getHigh().compareTo(MathNumber.ZERO) <= 0)
				return new IntInterval(arg.getHigh().multiply(MathNumber.MINUS_ONE),
						arg.getLow().multiply(MathNumber.MINUS_ONE));
			else if (arg.getHigh().compareTo(arg.getLow().multiply(MathNumber.MINUS_ONE)) >= 0)
				return new IntInterval(MathNumber.ZERO, arg.getHigh());
			else
				return new IntInterval(MathNumber.ZERO, arg.getLow().multiply(MathNumber.MINUS_ONE));

		return super.evalUnaryExpression(expression, arg, pp, oracle);
	}

	@Override
	public IntInterval evalBinaryExpression(
			BinaryExpression expression,
			IntInterval left,
			IntInterval right,
			ProgramPoint pp,
			SemanticOracle oracle) {
		// if left or right is top, top is returned
		if (left.isTop() || right.isTop())
			return top();
		if (left.isBottom() || right.isBottom())
			return bottom();

		BinaryOperator operator = expression.getOperator();

		if (operator instanceof JavaMathMax)
			return new IntInterval(left.getLow().max(right.getLow()), left.getHigh().max(right.getHigh()));

		if (operator instanceof JavaMathMin)
			return new IntInterval(left.getLow().min(right.getLow()), left.getHigh().min(right.getHigh()));

		if (operator instanceof JavaLongRotateRightOperator)
			return new IntInterval(-1, 1);
		if (operator instanceof JavaLongCompareOperator)
			return new IntInterval(-1, 1);
		if (operator instanceof JavaFloatCompareOperator)
			return new IntInterval(-1, 1);
		if (operator instanceof JavaDoubleCompareOperator)
			return new IntInterval(-1, 1);
		if (operator instanceof JavaFloatCompareOperator)
			return new IntInterval(-1, 1);
		if (operator instanceof JavaByteCompareOperator)
			return new IntInterval(-1, 1);
		if (operator instanceof JavaIntegerCompareOperator)
			return new IntInterval(-1, 1);

		return super.evalBinaryExpression(expression, left, right, pp, oracle);
	}

	@Override
	public ValueEnvironment<IntInterval> assume(
			ValueEnvironment<IntInterval> environment,
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
		return super.assume(environment, expression, src, dest, oracle);
	}

}

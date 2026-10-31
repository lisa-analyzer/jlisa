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
import it.unive.jlisa.program.operator.JavaStringCharAtOperator;
import it.unive.jlisa.program.operator.JavaStringLengthOperator;
import it.unive.jlisa.program.type.JavaCharType;
import it.unive.jlisa.program.type.JavaDoubleType;
import it.unive.jlisa.program.type.JavaFloatType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.program.type.JavaNumericType;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.numeric.Interval;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.*;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.DivisionOperator;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
import it.unive.lisa.symbolic.value.operator.RemainderOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.BitwiseShiftLeft;
import it.unive.lisa.symbolic.value.operator.binary.BitwiseShiftRight;
import it.unive.lisa.symbolic.value.operator.binary.BitwiseUnsignedShiftRight;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.symbolic.value.operator.binary.LogicalAnd;
import it.unive.lisa.symbolic.value.operator.binary.LogicalOr;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeTokenType;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.numeric.MathNumberConversionException;
import java.math.BigDecimal;
import java.util.Set;
import java.util.function.Function;

public class JavaNumericInterval extends Interval {

	private static final Function<Double, Double> SIN = Math::sin;

	@Override
	public IntInterval evalConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (constant.getValue() instanceof Number) {
			return fromConstant(constant);
		}
		if (constant.getStaticType() instanceof TypeTokenType) {
			return IntInterval.TOP;
		}

		// If the constant is not a number, return BOTTOM.
		// TOP represents any possible number, but since the constant is not
		// numeric, BOTTOM is more appropriate.
		return IntInterval.BOTTOM;
	}

	public IntInterval fromConstant(
			Constant constant) {

		// a floating point value is read as such whatever the static type of
		// the constant (e.g., a constraint built on an untyped identifier)
		if (constant.getStaticType() == JavaFloatType.INSTANCE || constant.getStaticType() == JavaDoubleType.INSTANCE
				|| constant.getValue() instanceof Double || constant.getValue() instanceof Float) {
			double valueD = ((Number) constant.getValue()).doubleValue();
			if (Double.isNaN(valueD))
				// not a number, but a value of double: no interval holds it,
				// and bottom would make the state unreachable. Returning TOP is
				// sound, as it represents any possible value of the type,
				// including NaN.
				return IntInterval.TOP;
			if (Double.isInfinite(valueD))
				return IntInterval.TOP;

			return new IntInterval(fromDouble(valueD), fromDouble(valueD));
		} else {
			// integer type
			long valueL = ((Number) constant.getValue()).longValue();
			return new IntInterval(new MathNumber(valueL), new MathNumber(valueL));
		}
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
			// the same single value on both sides (as for ==)
			if (left.isSingleton() && left.equals(right))
				return Satisfiability.NOT_SATISFIED;
			return Satisfiability.UNKNOWN;
		}
		return Satisfiability.UNKNOWN;
	}

	/**
	 * Returns the interval of the sine or the cosine of the values of the given
	 * interval. The bounds are the values at the bounds of the interval, and
	 * the exact maximum or minimum (1 or -1) of every point inside it where the
	 * function has one: {@code Math.sin} and {@code Math.cos} are
	 * semi-monotonic, hence between two of these points their values stay
	 * between the values at the bounds.
	 *
	 * @param i    the interval of the argument
	 * @param sine whether the function is the sine (otherwise the cosine)
	 *
	 * @return the interval of the result
	 */
	public static IntInterval sineOrCosine(
			IntInterval i,
			boolean sine) {
		if (i.isBottom())
			return i;
		if (i.lowIsMinusInfinity() || i.highIsPlusInfinity())
			// the sine and the cosine of an infinity are NaN
			return IntInterval.TOP;

		double a, b;
		try {
			a = i.getLow().toDouble();
			b = i.getHigh().toDouble();
		} catch (MathNumberConversionException e) {
			return IntInterval.TOP;
		}
		if (Double.isNaN(a) || Double.isNaN(b) || b - a >= 2 * Math.PI)
			return new IntInterval(-1, 1);

		double fa = sine ? Math.sin(a) : Math.cos(a);
		double fb = sine ? Math.sin(b) : Math.cos(b);
		double min = Math.min(fa, fb);
		double max = Math.max(fa, fb);

		// the maximum and minimum points are pi / 2 + k * pi for the sine and
		// k * pi for the cosine, with value 1 for an even k and -1 for an odd
		// one. These points are computed with doubles: a point a few doubles
		// outside [a, b] is kept, since the exact one may be inside
		double offset = sine ? Math.PI / 2 : 0;
		long kStart = (long) Math.floor((a - offset) / Math.PI) - 1;
		long kEnd = (long) Math.ceil((b - offset) / Math.PI) + 1;
		for (long k = kStart; k <= kEnd; k++) {
			double point = offset + k * Math.PI;
			double tolerance = 4 * Math.ulp(Math.abs(point) + 1);
			if (point < a - tolerance || point > b + tolerance)
				continue;
			double value = k % 2 == 0 ? 1 : -1;
			min = Math.min(min, value);
			max = Math.max(max, value);
		}
		return new IntInterval(fromDouble(min), fromDouble(max));
	}

	public static IntInterval trigonometric(
			IntInterval i,
			Function<Double, Double> function,
			double period) {
		if (i.isBottom())
			return i;

		if (i.lowIsMinusInfinity() || i.highIsPlusInfinity())
			// the trigonometric functions of an infinity are NaN
			return IntInterval.TOP;

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

		int lb = (int) Math.floor(min);
		int ub = (int) Math.ceil(max);

		if (function == SIN) {
			// if [a;b] is within (0;PI), we know that the lower bound is never
			// going to be exactly 0.
			if (a > 0.0 && b < Math.PI && lb == 0 && lb != ub) {
				// we can safely move the lower bound to exclude 0
				double lbTmp = Math.nextUp(0.0);
				return new IntInterval(new MathNumber(lbTmp), new MathNumber(ub));
			}
		}

		return new IntInterval((int) Math.floor(min), (int) Math.ceil(max));
	}

	@Override
	public IntInterval visit(
			UnaryExpression expression,
			IntInterval arg,
			Object... params)
			throws SemanticException {
		if (expression.getOperator() instanceof JavaStringLengthOperator) {
			ProgramPoint pp = (ProgramPoint) params[1];
			SemanticOracle oracle = (SemanticOracle) params[2];
			return evalUnaryExpression(expression, arg, pp, oracle);
		}
		return super.visit(expression, arg, params);
	}

	@Override
	public IntInterval visit(
			BinaryExpression expression,
			IntInterval left,
			IntInterval right,
			Object... params)
			throws SemanticException {
		if (expression.getOperator() instanceof JavaStringCharAtOperator) {
			ProgramPoint pp = (ProgramPoint) params[1];
			SemanticOracle oracle = (SemanticOracle) params[2];
			return evalBinaryExpression(expression, left, right, pp, oracle);
		}
		return super.visit(expression, left, right, params);
	}

	@Override
	public IntInterval evalUnaryExpression(
			UnaryExpression expression,
			IntInterval arg,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		UnaryOperator operator = expression.getOperator();

		if (operator instanceof JavaStringLengthOperator) {
			IntInterval length = new IntInterval(MathNumber.ZERO, MathNumber.PLUS_INFINITY);
			if (oracle.hasWholeValueAnlysis())
				// e.g., the constant propagation knows the string
				length = length.glb(generate(oracle.constraints(this, expression, pp), pp, oracle));
			return length;
		}

		if (arg.isTop() || arg.isBottom())
			return arg;

		if (operator == NumericNegation.INSTANCE) {
			// exact on floating point values (the multiplication by -1 of
			// LiSA rounds the bounds to integers); on integral values,
			// -MIN_VALUE wraps around to MIN_VALUE
			if (oracle == null)
				return top();
			// an unbounded integral interval stands for the values up to the
			// bounds of the type (e.g., -x with x in [0, +Inf] is in [-Inf, 0])
			IntInterval inType = withinType(expression.getExpression(), arg, pp, oracle);
			if (mayWrapAround(new IntInterval(inType.getHigh().multiply(MathNumber.MINUS_ONE),
					inType.getLow().multiply(MathNumber.MINUS_ONE)), oracle.getDynamicTypeOf(expression, pp)))
				return top();
			return new IntInterval(arg.getHigh().multiply(MathNumber.MINUS_ONE),
					arg.getLow().multiply(MathNumber.MINUS_ONE));
		}

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
			return sineOrCosine(arg, true);
		if (operator instanceof JavaMathCosOperator)
			return sineOrCosine(arg, false);
		if (operator instanceof JavaMathTanOperator)
			return trigonometric(arg, Math::tan, Math.PI);

		if ((operator instanceof JavaMathAsinOperator || operator instanceof JavaMathAcosOperator)
				&& (arg.lowIsMinusInfinity() || arg.getLow().compareTo(MathNumber.MINUS_ONE) < 0
						|| arg.highIsPlusInfinity() || arg.getHigh().compareTo(MathNumber.ONE) > 0))
			// NaN outside [-1, 1]
			return IntInterval.TOP;

		if (operator instanceof JavaMathAsinOperator)
			if (arg.lowIsMinusInfinity() || arg.getLow().compareTo(MathNumber.MINUS_ONE) <= 0)
				if (arg.highIsPlusInfinity() || arg.getHigh().compareTo(MathNumber.ONE) >= 1)
					return new IntInterval(fromDouble(Math.asin(-1)), fromDouble(Math.asin(1)));
				else
					return new IntInterval(fromDouble(Math.asin(-1)), fromDouble(Math.asin(h)));
			else if (arg.highIsPlusInfinity() || arg.getHigh().compareTo(MathNumber.ONE) >= 1)
				return new IntInterval(fromDouble(Math.asin(l)), fromDouble(Math.asin(1)));
			else
				return new IntInterval(fromDouble(Math.asin(l)), fromDouble(Math.asin(h)));

		if (operator instanceof JavaMathAcosOperator)
			if (arg.lowIsMinusInfinity() || arg.getLow().compareTo(MathNumber.MINUS_ONE) <= 0)
				if (arg.highIsPlusInfinity() || arg.getHigh().compareTo(MathNumber.ONE) >= 1)
					return new IntInterval(fromDouble(Math.acos(1)), fromDouble(Math.acos(-1)));
				else
					return new IntInterval(fromDouble(Math.acos(h)), fromDouble(Math.acos(-1)));
			else if (arg.highIsPlusInfinity() || arg.getHigh().compareTo(MathNumber.ONE) >= 1)
				return new IntInterval(fromDouble(Math.acos(1)), fromDouble(Math.acos(l)));
			else
				return new IntInterval(fromDouble(Math.acos(h)), fromDouble(Math.acos(l)));

		if (operator instanceof JavaMathAtanOperator)
			if (arg.lowIsMinusInfinity())
				if (arg.highIsPlusInfinity())
					return IntInterval.TOP;
				else
					return new IntInterval(MathNumber.MINUS_INFINITY, fromDouble(Math.atan(h)));
			else if (arg.highIsPlusInfinity())
				return new IntInterval(fromDouble(Math.atan(l)), MathNumber.PLUS_INFINITY);
			else
				return new IntInterval(fromDouble(Math.atan(l)), fromDouble(Math.atan(h)));

		if (operator instanceof JavaMathToRadiansOperator)
			if (arg.lowIsMinusInfinity())
				if (arg.highIsPlusInfinity())
					return IntInterval.TOP;
				else
					return new IntInterval(MathNumber.MINUS_INFINITY, fromDouble(Math.toRadians(h)));
			else if (arg.highIsPlusInfinity())
				return new IntInterval(fromDouble(Math.toRadians(l)), MathNumber.PLUS_INFINITY);
			else
				return new IntInterval(fromDouble(Math.toRadians(l)), fromDouble(Math.toRadians(h)));

		if ((operator instanceof JavaMathSqrtOperator || operator instanceof JavaMathLogOperator
				|| operator instanceof JavaMathLog10Operator)
				&& (arg.lowIsMinusInfinity() || arg.getLow().compareTo(MathNumber.ZERO) < 0))
			// NaN for negative values
			return IntInterval.TOP;

		if (operator instanceof JavaMathSqrtOperator)
			if (arg.lowIsMinusInfinity() || arg.getLow().compareTo(MathNumber.ZERO) <= 0)
				if (arg.getHigh().compareTo(MathNumber.ZERO) <= 0)
					return IntInterval.BOTTOM;
				else if (arg.highIsPlusInfinity())
					return new IntInterval(MathNumber.ZERO, MathNumber.PLUS_INFINITY);
				else
					return new IntInterval(MathNumber.ZERO, fromDouble(Math.sqrt(h)));
			else if (arg.highIsPlusInfinity())
				return new IntInterval(fromDouble(Math.sqrt(l)), MathNumber.PLUS_INFINITY);
			else
				return new IntInterval(fromDouble(Math.sqrt(l)), fromDouble(Math.sqrt(h)));

		if (operator instanceof JavaMathLogOperator)
			if (arg.lowIsMinusInfinity() || arg.getLow().compareTo(MathNumber.ZERO) <= 0)
				if (arg.getHigh().compareTo(MathNumber.ZERO) <= 0)
					return IntInterval.BOTTOM;
				else if (arg.highIsPlusInfinity())
					return IntInterval.TOP;
				else
					return new IntInterval(MathNumber.MINUS_INFINITY, fromDouble(Math.log(h)));
			else if (arg.highIsPlusInfinity())
				return new IntInterval(fromDouble(Math.log(l)), MathNumber.PLUS_INFINITY);
			else
				return new IntInterval(fromDouble(Math.log(l)), fromDouble(Math.log(h)));

		if (operator instanceof JavaMathLog10Operator)
			if (arg.lowIsMinusInfinity() || arg.getLow().compareTo(MathNumber.ZERO) <= 0)
				if (arg.getHigh().compareTo(MathNumber.ZERO) <= 0)
					return IntInterval.BOTTOM;
				else if (arg.highIsPlusInfinity())
					return IntInterval.TOP;
				else
					return new IntInterval(MathNumber.MINUS_INFINITY, fromDouble(Math.log10(h)));
			else if (arg.highIsPlusInfinity())
				return new IntInterval(fromDouble(Math.log10(l)), MathNumber.PLUS_INFINITY);
			else
				return new IntInterval(fromDouble(Math.log10(l)), fromDouble(Math.log10(h)));

		if (operator instanceof JavaMathExpOperator)
			if (arg.lowIsMinusInfinity())
				if (arg.highIsPlusInfinity())
					return IntInterval.TOP;
				else
					return new IntInterval(MathNumber.MINUS_INFINITY, fromDouble(Math.exp(h)));
			else if (arg.highIsPlusInfinity())
				return new IntInterval(fromDouble(Math.exp(l)), MathNumber.PLUS_INFINITY);
			else
				return new IntInterval(fromDouble(Math.exp(l)), fromDouble(Math.exp(h)));

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
	public IntInterval evalTypeConv(
			BinaryExpression conv,
			IntInterval left,
			IntInterval right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (left.isBottom())
			return left;

		Type targetType = conv.getStaticType();
		if (!(targetType instanceof JavaNumericType numType) || !numType.isIntegral())
			// no refinement for non-integral targets (e.g., float, double),
			// except that a converted integral value is never infinite; the
			// conversion rounds (e.g., (float) Long.MAX_VALUE is 2^63)
			return oracle == null ? super.evalTypeConv(conv, left, right, pp, oracle)
					: roundOutward(super.evalTypeConv(conv, withinType(conv.getLeft(), left, pp, oracle),
							right, pp, oracle), conv, pp, oracle);

		IntInterval bounds = typeBounds(numType);
		if (left.getLow().compareTo(bounds.getLow()) >= 0 && left.getHigh().compareTo(bounds.getHigh()) <= 0)
			// the value already fits in the target type: the conversion is
			// exact, no truncation/wrap-around can occur
			return left;

		if (left.isSingleton()
				&& left.getLow().compareTo(new MathNumber(Long.MIN_VALUE)) >= 0
				&& left.getLow().compareTo(new MathNumber(Long.MAX_VALUE)) <= 0) {
			// the value is exactly known: we can precisely replicate Java's
			// narrowing conversion (JLS 5.1.3) instead of conservatively
			// falling back to the whole range of the target type
			try {
				long truncated = truncate(left.getLow().toLong(), numType.getNBits(), numType.isUnsigned());
				return new IntInterval(new MathNumber(truncated), new MathNumber(truncated));
			} catch (MathNumberConversionException e) {
				// should not happen given the bound checks above, but fall
				// back to a sound over-approximation just in case
			}
		}

		// the conversion may truncate/wrap-around bits and we cannot pin down
		// the exact result, so we soundly fall back to the full range
		// representable by the target type
		return bounds;
	}

	/**
	 * Replicates the effect of a Java narrowing primitive conversion (JLS
	 * 5.1.3) of {@code value} to a {@code bits}-wide integral type: the value
	 * is reduced modulo 2^bits and, if the target is signed, reinterpreted in
	 * two's complement.
	 */
	private static long truncate(
			long value,
			int bits,
			boolean unsigned) {
		if (bits >= 64)
			return value;
		long mask = (1L << bits) - 1;
		long result = value & mask;
		if (!unsigned) {
			long signBit = 1L << (bits - 1);
			if ((result & signBit) != 0)
				result -= (1L << bits);
		}
		return result;
	}

	/**
	 * Yields the interval of all the values representable by the given integral
	 * numeric type (e.g., [-128, 127] for {@code byte}).
	 */
	static IntInterval typeBounds(
			JavaNumericType type) {
		int bits = type.getNBits();
		boolean unsigned = type.isUnsigned();
		if (bits == 64 && !unsigned)
			// avoids overflow issues when shifting by 63 bits
			return new IntInterval(new MathNumber(Long.MIN_VALUE), new MathNumber(Long.MAX_VALUE));
		long max = unsigned ? (1L << bits) - 1 : (1L << (bits - 1)) - 1;
		long min = unsigned ? 0L : -(1L << (bits - 1));
		return new IntInterval(new MathNumber(min), new MathNumber(max));
	}

	@Override
	public IntInterval evalBinaryExpression(
			BinaryExpression expression,
			IntInterval left,
			IntInterval right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		BinaryOperator operator = expression.getOperator();
		if (oracle != null && mayBeFloatingPoint(expression, pp, oracle)) {
			// an integral operand of a floating point operation is never
			// infinite (e.g., 0.9f * w with w > 0 is positive and finite),
			// and it is converted to the type of the operation first, which
			// rounds (e.g., Long.MAX_VALUE * 1.0f is 2^63)
			left = asFloatingPointOperand(expression.getLeft(), left, expression, pp, oracle);
			right = asFloatingPointOperand(expression.getRight(), right, expression, pp, oracle);
		}
		if (!(operator instanceof AdditionOperator
				|| operator instanceof SubtractionOperator
				|| operator instanceof MultiplicationOperator)) {
			if (operator instanceof DivisionOperator
					&& !left.isBottom() && !right.isBottom()
					&& (right.includes(IntInterval.ZERO) || left.isInfinite())
					&& mayBeFloatingPoint(expression, pp, oracle))
				// 0.0 / 0.0 and inf / inf are NaN
				return top();
			if (operator instanceof RemainderOperator)
				return remainder(left, right, oracle != null && !mayBeFloatingPoint(expression, pp, oracle));
			IntInterval exact = evalExact(expression, left, right, pp, oracle);
			if (operator instanceof DivisionOperator && oracle != null && isFloatingPoint(expression, pp, oracle))
				exact = keepSign(exact, left, right, true);
			return operator instanceof DivisionOperator ? roundOutward(exact, expression, pp, oracle) : exact;
		}

		// this covers potential overflows for +, -, *, ++, --
		if (!left.isBottom() && !right.isBottom()
				&& (left.isInfinite() || right.isInfinite())
				&& (operator instanceof MultiplicationOperator || !mayBeFloatingPoint(expression, pp, oracle)))
			// one of the operands is already unbounded: an integral result may
			// wrap around, and a product may be 0 times an infinity. The sum
			// and the difference of floating point values do not wrap around,
			// and their interval is computed (e.g., [0, +Inf] + 0.2 is [0.2,
			// +Inf])
			return top();
		IntInterval result = evalExact(expression, left, right, pp, oracle);
		if (operator instanceof MultiplicationOperator && oracle != null && isFloatingPoint(expression, pp, oracle))
			result = keepSign(result, left, right, false);
		if (oracle != null && mayWrapAround(result, oracle.getDynamicTypeOf(expression, pp)))
			// both operands are bounded, but their exact (arbitrary precision)
			// result falls outside the range of the expression's type: Java
			// would wrap it around via two's-complement truncation, which
			// cannot be pinned down without the concrete values
			return top();
		return roundOutward(result, expression, pp, oracle);
	}

	/**
	 * Returns the interval of {@code n % m}, as computed by Java: the result
	 * has the sign of {@code n}, and its absolute value is at most the one of
	 * {@code n} and smaller than the one of {@code m} (e.g., {@code n % 3} is
	 * in [-2, 2] on integers). On floating point numbers, the result is NaN if
	 * {@code m} is 0 or {@code n} is infinite, and it is not an integer (e.g.,
	 * {@code 5.5 % 3.0} is 2.5).
	 *
	 * @param n        the interval of the dividend
	 * @param m        the interval of the divisor
	 * @param integral whether the operands are surely integers
	 *
	 * @return the interval of the remainder
	 */
	public static IntInterval remainder(
			IntInterval n,
			IntInterval m,
			boolean integral) {
		if (!integral && (m.includes(IntInterval.ZERO) || n.isInfinite()))
			return IntInterval.TOP;
		MathNumber low = n.lowIsMinusInfinity() ? MathNumber.MINUS_INFINITY : n.getLow().min(MathNumber.ZERO);
		MathNumber high = n.highIsPlusInfinity() ? MathNumber.PLUS_INFINITY : n.getHigh().max(MathNumber.ZERO);
		if (!m.isInfinite()) {
			MathNumber largest = m.getLow().abs().max(m.getHigh().abs());
			if (largest.isZero())
				// only a divisor 0: an exception, raised by the statement
				return IntInterval.BOTTOM;
			MathNumber bound = integral ? largest.subtract(MathNumber.ONE) : largest;
			low = low.max(bound.multiply(MathNumber.MINUS_ONE));
			high = high.min(bound);
		}
		return new IntInterval(low, high);
	}

	/**
	 * Returns the non-strict comparison equivalent to the given strict one
	 * ({@code <} or {@code >}) on floating point values. When one side is a
	 * constant, it is moved to the next double toward the other side (e.g.,
	 * {@code x < 1.0} gives {@code x <= Math.nextDown(1.0)}, and {@code 0 < x}
	 * gives {@code Double.MIN_VALUE <= x}), since no double lies between a
	 * double and the next one. Otherwise, the strict comparison is weakened to
	 * the non-strict one.
	 *
	 * @param expression the strict comparison
	 *
	 * @return the non-strict comparison
	 */
	private static BinaryExpression nonStrict(
			BinaryExpression expression) {
		boolean lt = expression.getOperator() == ComparisonLt.INSTANCE;
		SymbolicExpression left = expression.getLeft();
		SymbolicExpression right = expression.getRight();
		// x < c gives x <= nextDown(c), c < x gives nextUp(c) <= x, and the
		// same for > in the other direction
		if (right instanceof Constant c && c.getValue() instanceof Number n && !(n instanceof Long))
			right = new Constant(JavaDoubleType.INSTANCE, step(n.doubleValue(), !lt), c.getCodeLocation());
		else if (left instanceof Constant c && c.getValue() instanceof Number n && !(n instanceof Long))
			left = new Constant(JavaDoubleType.INSTANCE, step(n.doubleValue(), lt), c.getCodeLocation());
		return new BinaryExpression(expression.getStaticType(), left, right,
				lt ? ComparisonLe.INSTANCE : ComparisonGe.INSTANCE, expression.getCodeLocation());
	}

	// the next double up or down (infinities and NaN are left as they are)
	private static double step(
			double value,
			boolean up) {
		if (Double.isNaN(value) || Double.isInfinite(value))
			return value;
		return up ? Math.nextUp(value) : Math.nextDown(value);
	}

	/**
	 * Returns the given exact result of a floating point operation, with each
	 * bound rounded outward to a float or double: the low bound down, the high
	 * bound up (a bound that is already a value of the type is kept). Java
	 * rounds the exact result to the nearest float or double (e.g., 0.1 + 0.7
	 * is 0.7999999999999999, below the exact sum), which the widened interval
	 * contains. Results of integral operations are returned as they are.
	 *
	 * @param result     the exact result
	 * @param expression the operation
	 * @param pp         the program point of the operation
	 * @param oracle     the oracle for inter-domain communication
	 *
	 * @return the widened result
	 *
	 * @throws SemanticException if the types cannot be computed
	 */
	private static IntInterval roundOutward(
			IntInterval result,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (result.isTop() || result.isBottom() || oracle == null || !mayBeFloatingPoint(expression, pp, oracle))
			return result;
		// a float has larger units in the last place than a double
		boolean isFloat = oracle.getRuntimeTypesOf(expression, pp).stream()
				.anyMatch(type -> type == JavaFloatType.INSTANCE);
		MathNumber low = result.getLow(), high = result.getHigh();
		try {
			if (!low.isMinusInfinity()) {
				// the nearest value, moved down if it is above the bound
				double d = isFloat ? (float) low.toDouble() : low.toDouble();
				if (!Double.isInfinite(d) && fromDouble(d).compareTo(low) > 0)
					d = isFloat ? Math.nextDown((float) d) : Math.nextDown(d);
				low = Double.isInfinite(d) ? (d < 0 ? MathNumber.MINUS_INFINITY : fromDouble(Double.MAX_VALUE))
						: fromDouble(d);
			}
			if (!high.isPlusInfinity()) {
				// the nearest value, moved up if it is below the bound
				double d = isFloat ? (float) high.toDouble() : high.toDouble();
				if (!Double.isInfinite(d) && fromDouble(d).compareTo(high) < 0)
					d = isFloat ? Math.nextUp((float) d) : Math.nextUp(d);
				high = Double.isInfinite(d) ? (d > 0 ? MathNumber.PLUS_INFINITY : fromDouble(-Double.MAX_VALUE))
						: fromDouble(d);
			}
		} catch (MathNumberConversionException e) {
			return IntInterval.TOP;
		}
		return new IntInterval(low, high);
	}

	/**
	 * Returns whether {@code result}, the exact (arbitrary precision) result of
	 * an arithmetic operation, falls outside the range representable by
	 * {@code type}, meaning that the actual Java computation would wrap around
	 * instead of yielding {@code result}.
	 *
	 * @param result the exact result
	 * @param type   the type of the operation
	 *
	 * @return whether the result may wrap around
	 */
	static boolean mayWrapAround(
			IntInterval result,
			Type type) {
		if (result.isTop() || result.isBottom())
			return false;
		if (!(type instanceof JavaNumericType numType) || !numType.isIntegral())
			return false;
		IntInterval bounds = typeBounds(numType);
		return result.getLow().compareTo(bounds.getLow()) < 0 || result.getHigh().compareTo(bounds.getHigh()) > 0;
	}

	// the result of the operation on the mathematical values, without the
	// wrap-around of integral types (see evalBinaryExpression)
	private IntInterval evalExact(
			BinaryExpression expression,
			IntInterval left,
			IntInterval right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		BinaryOperator operator = expression.getOperator();

		if (operator instanceof JavaStringCharAtOperator)
			return typeBounds(JavaCharType.INSTANCE);

		// if left or right is top, top is returned
		if (left.isTop() || right.isTop())
			return top();
		if (left.isBottom() || right.isBottom())
			return bottom();

		if (operator instanceof AdditionOperator) {
			if (!left.isBottom() && !right.isBottom()) {
				return new IntInterval(left.getLow().add(right.getLow()), left.getHigh().add(right.getHigh()));
			}
		}

		if (operator instanceof SubtractionOperator) {
			if (!left.isBottom() && !right.isBottom()) {
				return new IntInterval(left.getLow().subtract(right.getHigh()),
						left.getHigh().subtract(right.getLow()));
			}
		}

		// exact bounds only between single values (e.g., Math.PI / 4): on
		// ranges, the bounds rounded to integers let loops converge
		if ((operator instanceof MultiplicationOperator || operator instanceof DivisionOperator)
				&& left.isSingleton() && right.isSingleton()
				&& isFloatingPoint(expression, pp, oracle)) {
			IntInterval result = floatingPointProduct(left, right, operator instanceof DivisionOperator);
			if (result != null)
				return result;
		}

		if (operator instanceof JavaMathMax)
			return new IntInterval(left.getLow().max(right.getLow()), left.getHigh().max(right.getHigh()));

		if (operator instanceof JavaMathMin)
			return new IntInterval(left.getLow().min(right.getLow()), left.getHigh().min(right.getHigh()));

		if (operator instanceof JavaLongRotateRightOperator)
			return new IntInterval(-1, 1);
		if (operator instanceof JavaLongCompareOperator)
			return typeBounds(JavaIntType.INSTANCE);
		if (operator instanceof JavaFloatCompareOperator)
			return typeBounds(JavaIntType.INSTANCE);
		if (operator instanceof JavaDoubleCompareOperator)
			return typeBounds(JavaIntType.INSTANCE);
		if (operator instanceof JavaFloatCompareOperator)
			return typeBounds(JavaIntType.INSTANCE);
		if (operator instanceof JavaByteCompareOperator)
			return typeBounds(JavaIntType.INSTANCE);
		if (operator instanceof JavaIntegerCompareOperator)
			return typeBounds(JavaIntType.INSTANCE);
		if (operator instanceof BitwiseShiftLeft)
			return evalShiftLeft(expression, left, right, pp, oracle);
		if (operator instanceof BitwiseShiftRight)
			return evalShiftRight(left, right, false);
		if (operator instanceof BitwiseUnsignedShiftRight)
			return evalShiftRight(left, right, true);

		return super.evalBinaryExpression(expression, left, right, pp, oracle);
	}

	private IntInterval evalShiftLeft(
			BinaryExpression expression,
			IntInterval x,
			IntInterval s,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		JavaNumericType type = integralTypeOf(expression, pp, oracle);
		if (type == null)
			return top();
		int bits = type.getNBits();
		if (s.getLow().compareTo(MathNumber.ZERO) < 0 || s.getHigh().compareTo(new MathNumber(bits - 1L)) > 0)
			return top();

		int loS, hiS;
		try {
			loS = s.getLow().toInt();
			hiS = s.getHigh().toInt();
		} catch (MathNumberConversionException e) {
			return top();
		}

		MathNumber twoLoS = powerOfTwo(loS);
		MathNumber twoHiS = powerOfTwo(hiS);
		MathNumber a = x.getLow().multiply(twoLoS);
		MathNumber b = x.getLow().multiply(twoHiS);
		MathNumber c = x.getHigh().multiply(twoLoS);
		MathNumber d = x.getHigh().multiply(twoHiS);
		MathNumber lo = a.min(b).min(c).min(d);
		MathNumber hi = a.max(b).max(c).max(d);

		IntInterval bounds = typeBounds(type);
		if (lo.compareTo(bounds.getLow()) < 0 || hi.compareTo(bounds.getHigh()) > 0)
			// exact result does not fit the type: real execution would wrap
			return top();
		return new IntInterval(lo, hi);
	}

	private IntInterval evalShiftRight(
			IntInterval x,
			IntInterval s,
			boolean unsigned) {
		if (s.getLow().compareTo(MathNumber.ZERO) < 0)
			return top();
		if (unsigned && x.getLow().compareTo(MathNumber.ZERO) < 0)
			return top();

		int loS, hiS;
		try {
			loS = s.getLow().toInt();
			hiS = s.highIsPlusInfinity() ? Integer.MAX_VALUE : s.getHigh().toInt();
		} catch (MathNumberConversionException e) {
			return top();
		}
		// beyond the bit width every shift amount yields the same result (0,
		// or -1 for negative x under arithmetic shift): cap hiS so
		// powerOfTwo below stays cheap regardless of how large s claims to be
		hiS = Math.min(hiS, 64);

		MathNumber twoLoS = powerOfTwo(loS);
		MathNumber twoHiS = powerOfTwo(hiS);
		MathNumber a = floorDivide(x.getLow(), twoLoS);
		MathNumber b = floorDivide(x.getLow(), twoHiS);
		MathNumber c = floorDivide(x.getHigh(), twoLoS);
		MathNumber d = floorDivide(x.getHigh(), twoHiS);
		MathNumber lo = a.min(b).min(c).min(d);
		MathNumber hi = a.max(b).max(c).max(d);
		return new IntInterval(lo, hi);
	}

	private static MathNumber floorDivide(
			MathNumber n,
			MathNumber d) {
		if (n.isInfinite())
			return n.isPositive() == d.isPositive() ? MathNumber.PLUS_INFINITY : MathNumber.MINUS_INFINITY;
		return n.divide(d).roundDown();
	}

	private static MathNumber powerOfTwo(
			int exponent) {
		MathNumber result = MathNumber.ONE;
		MathNumber two = new MathNumber(2L);
		for (int i = 0; i < exponent; i++)
			result = result.multiply(two);
		return result;
	}

	/**
	 * Returns whether the given expression may have floating point values.
	 *
	 * @param expression the expression
	 * @param pp         the program point where the expression is evaluated
	 * @param oracle     the oracle for inter-domain communication
	 *
	 * @return {@code true} if a runtime type of the expression is {@code float}
	 *             or {@code double}
	 *
	 * @throws SemanticException if the types cannot be computed
	 */
	static boolean mayBeFloatingPoint(
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return oracle.getRuntimeTypesOf(expression, pp).stream()
				.anyMatch(type -> type instanceof JavaNumericType numType && !numType.isIntegral());
	}

	/**
	 * Returns whether the given expression has only floating point values.
	 *
	 * @param expression the expression
	 * @param pp         the program point where the expression is evaluated
	 * @param oracle     the oracle for inter-domain communication
	 *
	 * @return {@code true} if all the runtime types of the expression are
	 *             {@code float} or {@code double}
	 *
	 * @throws SemanticException if the types cannot be computed
	 */
	static boolean isFloatingPoint(
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		Set<Type> types = oracle.getRuntimeTypesOf(expression, pp);
		return !types.isEmpty()
				&& types.stream().allMatch(type -> type instanceof JavaNumericType numType && !numType.isIntegral());
	}

	/**
	 * Returns the product or the quotient of two intervals of floating point
	 * values. The interval of the integral values rounds the bounds to
	 * integers; here they are kept, and widened by one double on each side,
	 * since the result of the operation is rounded to the nearest double.
	 *
	 * @param left     the interval of the left operand
	 * @param right    the interval of the right operand
	 * @param division whether the operation is a division
	 *
	 * @return the interval of the result, or {@code null} if it cannot be
	 *             computed this way (a divisor that may be zero, or bounds that
	 *             are not numbers)
	 */
	private static IntInterval floatingPointProduct(
			IntInterval left,
			IntInterval right,
			boolean division) {
		if (division && right.includes(IntInterval.ZERO))
			return null;
		MathNumber[] bounds = {
				combine(left.getLow(), right.getLow(), division),
				combine(left.getLow(), right.getHigh(), division),
				combine(left.getHigh(), right.getLow(), division),
				combine(left.getHigh(), right.getHigh(), division) };
		MathNumber low = bounds[0];
		MathNumber high = bounds[0];
		for (MathNumber bound : bounds) {
			if (bound.isNaN())
				return null;
			low = low.min(bound);
			high = high.max(bound);
		}
		try {
			if (low.isFinite())
				low = fromDouble(Math.nextDown(low.toDouble()));
			if (high.isFinite())
				high = fromDouble(Math.nextUp(high.toDouble()));
		} catch (MathNumberConversionException e) {
			return null;
		}
		return new IntInterval(low, high);
	}

	/**
	 * Returns the given double as a number, with the infinities as the infinite
	 * numbers.
	 *
	 * @param value the double
	 *
	 * @return the number
	 */
	private static MathNumber fromDouble(
			double value) {
		if (value == Double.POSITIVE_INFINITY)
			return MathNumber.PLUS_INFINITY;
		if (value == Double.NEGATIVE_INFINITY)
			return MathNumber.MINUS_INFINITY;
		// the exact binary value: the shortest decimal (BigDecimal.valueOf)
		// differs from it (e.g., 0.1 is 0.1000000000000000055...), and the
		// exact arithmetic on decimals differs from the one of Java
		return new MathNumber(new BigDecimal(value));
	}

	private static MathNumber combine(
			MathNumber left,
			MathNumber right,
			boolean division) {
		return division ? left.divide(right) : left.multiply(right);
	}

	// the product or quotient of two bounded ranges, with the bounds rounded
	// to integers by the intervals of LiSA moved back to the exact ones when
	// the rounding crosses zero (e.g., [0.9, 0.9] * [1, 10] is [0, 9] once
	// rounded, while it is positive)
	private static IntInterval keepSign(
			IntInterval result,
			IntInterval left,
			IntInterval right,
			boolean division) {
		if (result.isBottom() || result.isTop() || left.isBottom() || right.isBottom()
				|| left.isInfinite() || right.isInfinite() || (division && right.includes(IntInterval.ZERO)))
			return result;
		MathNumber ll = combine(left.getLow(), right.getLow(), division);
		MathNumber lh = combine(left.getLow(), right.getHigh(), division);
		MathNumber hl = combine(left.getHigh(), right.getLow(), division);
		MathNumber hh = combine(left.getHigh(), right.getHigh(), division);
		MathNumber min = ll.min(lh).min(hl).min(hh);
		MathNumber max = ll.max(lh).max(hl).max(hh);
		MathNumber low = result.getLow(), high = result.getHigh();
		if (min.compareTo(MathNumber.ZERO) > 0 && low.compareTo(MathNumber.ZERO) <= 0)
			low = min;
		if (max.compareTo(MathNumber.ZERO) < 0 && high.compareTo(MathNumber.ZERO) >= 0)
			high = max;
		return new IntInterval(low, high);
	}

	// the interval of an expression with only integral values, cut to the
	// values of their types: an unbounded int interval stands for the ints up
	// to the largest one, not for an infinity
	private static IntInterval withinType(
			SymbolicExpression expression,
			IntInterval interval,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (interval.isBottom() || !interval.isInfinite() || !(expression instanceof ValueExpression value))
			return interval;
		Set<Type> types = oracle.getRuntimeTypesOf(value, pp);
		if (types.isEmpty()
				|| !types.stream().allMatch(type -> type instanceof JavaNumericType numType && numType.isIntegral()))
			return interval;
		MathNumber low = null, high = null;
		for (Type type : types) {
			IntInterval bounds = typeBounds((JavaNumericType) type);
			low = low == null ? bounds.getLow() : low.min(bounds.getLow());
			high = high == null ? bounds.getHigh() : high.max(bounds.getHigh());
		}
		low = interval.getLow().max(low);
		high = interval.getHigh().min(high);
		return low.compareTo(high) > 0 ? interval : new IntInterval(low, high);
	}

	// the interval of an operand of the given floating point operation: an
	// operand with only integral values is cut to the values of its types and
	// rounded outward to the type of the operation, to which it is converted
	private static IntInterval asFloatingPointOperand(
			SymbolicExpression operand,
			IntInterval interval,
			BinaryExpression operation,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (interval.isBottom() || interval.isTop() || !(operand instanceof ValueExpression value))
			return interval;
		Set<Type> types = oracle.getRuntimeTypesOf(value, pp);
		if (types.isEmpty()
				|| !types.stream().allMatch(type -> type instanceof JavaNumericType numType && numType.isIntegral()))
			return interval;
		return roundOutward(withinType(operand, interval, pp, oracle), operation, pp, oracle);
	}

	private static JavaNumericType integralTypeOf(
			BinaryExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		Type type = oracle.getDynamicTypeOf(expression, pp);
		if (!(type instanceof JavaNumericType numType) || !numType.isIntegral())
			return null;
		return numType;
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
		if (negatesFloatingPointComparison(environment, expression, false, src, oracle))
			// NaN satisfies every negated ordered comparison (!(x <= 5) holds
			// for NaN), but the intervals would assume the opposite comparison
			// (x > 5), which excludes NaN: nothing is assumed when an operand
			// may be NaN, that is, when its interval is top (NaN is only in
			// top: every operation that may give NaN gives top)
			return environment;
		return super.assume(environment, expression, src, dest, oracle);
	}

	/**
	 * Returns whether the given condition contains, under an odd number of
	 * negations, an ordered comparison ({@code <}, {@code <=}, {@code >},
	 * {@code >=}) with an operand that may be a floating point NaN.
	 *
	 * @param environment the environment where the condition is assumed
	 * @param expression  the condition
	 * @param negated     whether the condition is under an odd number of
	 *                        negations
	 * @param pp          the program point of the condition
	 * @param oracle      the oracle for inter-domain communication
	 *
	 * @return whether such a comparison is in the condition
	 *
	 * @throws SemanticException if the types cannot be computed
	 */
	private boolean negatesFloatingPointComparison(
			ValueEnvironment<IntInterval> environment,
			ValueExpression expression,
			boolean negated,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (expression instanceof UnaryExpression u && u.getOperator() == LogicalNegation.INSTANCE
				&& u.getExpression() instanceof ValueExpression inner)
			return negatesFloatingPointComparison(environment, inner, !negated, pp, oracle);
		if (!(expression instanceof BinaryExpression b) || !(b.getLeft() instanceof ValueExpression left)
				|| !(b.getRight() instanceof ValueExpression right))
			return false;
		BinaryOperator operator = b.getOperator();
		if (operator == LogicalAnd.INSTANCE || operator == LogicalOr.INSTANCE)
			return negatesFloatingPointComparison(environment, left, negated, pp, oracle)
					|| negatesFloatingPointComparison(environment, right, negated, pp, oracle);
		return negated
				&& (operator == ComparisonLt.INSTANCE || operator == ComparisonLe.INSTANCE
						|| operator == ComparisonGt.INSTANCE || operator == ComparisonGe.INSTANCE)
				&& (mayBeNaN(environment, left, pp, oracle) || mayBeNaN(environment, right, pp, oracle));
	}

	// whether the operand may be a floating point NaN: only top intervals
	// contain NaN
	private boolean mayBeNaN(
			ValueEnvironment<IntInterval> environment,
			ValueExpression operand,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return !isIntegral(operand, pp, oracle) && eval(environment, operand, pp, oracle).isTop();
	}

	@Override
	public ValueEnvironment<IntInterval> assumeBinaryExpression(
			ValueEnvironment<IntInterval> environment,
			BinaryExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {

		if (eval(environment, (ValueExpression) expression.getLeft(), src, oracle).isBottom()
				|| eval(environment, (ValueExpression) expression.getRight(), src, oracle).isBottom())
			// an operand without an interval (e.g., a boolean constant): the
			// condition says nothing about intervals
			return environment;

		Satisfiability sat = satisfies(environment, expression, src, oracle);
		if (sat == Satisfiability.NOT_SATISFIED)
			return environment.bottom();
		if (sat == Satisfiability.SATISFIED)
			return environment;

		if (expression.getOperator() != ComparisonNe.INSTANCE) {
			BinaryExpression assumed = expression;
			if ((expression.getOperator() == ComparisonLt.INSTANCE || expression.getOperator() == ComparisonGt.INSTANCE)
					&& (!isIntegral((ValueExpression) expression.getLeft(), src, oracle)
							|| !isIntegral((ValueExpression) expression.getRight(), src, oracle)))
				// the interval domain of LiSA refines the strict comparisons
				// by one (x < 1 gives x <= 0), which is wrong for floating
				// point values (x < 1 holds for x = 0.5): they are assumed as
				// non-strict ones (see nonStrict)
				assumed = nonStrict(expression);
			ValueEnvironment<IntInterval> result = super.assumeBinaryExpression(environment, assumed, src, dest,
					oracle);
			// the interval domain of LiSA refines only one identifier: when
			// both operands are identifiers, the right one is refined too, with
			// the mirrored comparison (e.g., y > x for x < y). Only integral
			// values are refined, since the strict comparisons refine by one
			BinaryOperator mirrored = mirror(expression.getOperator());
			if (!result.isBottom() && mirrored != null
					&& expression.getLeft() instanceof Identifier left
					&& expression.getRight() instanceof Identifier right
					&& isIntegral(left, src, oracle)
					&& isIntegral(right, src, oracle))
				result = super.assumeBinaryExpression(result,
						new BinaryExpression(expression.getStaticType(), right, left, mirrored,
								expression.getCodeLocation()),
						src, dest, oracle);
			return result;
		}

		Identifier id;
		IntInterval eval;
		IntInterval evalId;
		ValueExpression left = (ValueExpression) expression.getLeft();
		ValueExpression right = (ValueExpression) expression.getRight();
		if (left instanceof Identifier) {
			if (!canProcess(right, src, oracle))
				// the expression does not have a numerical value, we do not
				// assume anything on it
				return environment;
			eval = eval(environment, right, src, oracle);
			evalId = eval(environment, left, src, oracle);
			id = (Identifier) left;
		} else if (right instanceof Identifier) {
			if (!canProcess(left, src, oracle))
				// the expression does not have a numerical value, we do not
				// assume anything on it
				return environment;
			eval = eval(environment, left, src, oracle);
			evalId = eval(environment, right, src, oracle);
			id = (Identifier) right;
		} else
			return environment;

		if (eval.isBottom() || evalId.isBottom()) {
			// an operand without an interval (e.g., a value that is not a
			// number): the condition says nothing about intervals
			return environment;
		}

		ValueEnvironment<IntInterval> updatedEnv = environment;
		if (eval.isSingleton()) {
			if (evalId.getLow().equals(eval.getLow())) {
				MathNumber newLow = nextValueUp(id, evalId.getLow());
				IntInterval newInterval = new IntInterval(newLow, evalId.getHigh());
				updatedEnv = environment.putState(id, newInterval);
			}
			if (evalId.getHigh().equals(eval.getLow())) {
				MathNumber newHigh = nextValueDown(id, evalId.getHigh());
				IntInterval newInterval = new IntInterval(evalId.getLow(), newHigh);
				updatedEnv = environment.putState(id, newInterval);
			}
		}
		return updatedEnv;
	}

	private MathNumber nextValueUp(
			Identifier id,
			MathNumber x) {
		if (id.getStaticType() == JavaFloatType.INSTANCE || id.getStaticType() == JavaDoubleType.INSTANCE) {
			// TODO
			return x;
		}
		return x.add(new MathNumber(1));
	}

	private MathNumber nextValueDown(
			Identifier id,
			MathNumber x) {
		if (id.getStaticType() == JavaFloatType.INSTANCE || id.getStaticType() == JavaDoubleType.INSTANCE) {
			// TODO
			return x;
		}
		return x.subtract(new MathNumber(1));
	}

	/**
	 * Returns the comparison that holds when the operands of the given one are
	 * swapped (e.g., {@code >} for {@code <}).
	 *
	 * @param operator the comparison
	 *
	 * @return the mirrored comparison, or {@code null} if the operator is not a
	 *             comparison that can be mirrored
	 */
	private static BinaryOperator mirror(
			BinaryOperator operator) {
		if (operator == ComparisonEq.INSTANCE)
			return ComparisonEq.INSTANCE;
		if (operator == ComparisonLt.INSTANCE)
			return ComparisonGt.INSTANCE;
		if (operator == ComparisonGt.INSTANCE)
			return ComparisonLt.INSTANCE;
		if (operator == ComparisonLe.INSTANCE)
			return ComparisonGe.INSTANCE;
		if (operator == ComparisonGe.INSTANCE)
			return ComparisonLe.INSTANCE;
		return null;
	}

	/**
	 * Returns whether all the runtime types of the given expression are
	 * integral. The runtime types are checked one by one, since the dynamic
	 * type would need their common supertype, which cannot be computed for
	 * every class.
	 *
	 * @param expression the expression
	 * @param pp         the program point of the expression
	 * @param oracle     the oracle for inter-domain communication
	 *
	 * @return {@code true} if the expression is integral
	 *
	 * @throws SemanticException if the type cannot be computed
	 */
	static boolean isIntegral(
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		Set<Type> types = oracle.getRuntimeTypesOf(expression, pp);
		return !types.isEmpty()
				&& types.stream().allMatch(type -> type instanceof JavaNumericType numType && numType.isIntegral());
	}
}

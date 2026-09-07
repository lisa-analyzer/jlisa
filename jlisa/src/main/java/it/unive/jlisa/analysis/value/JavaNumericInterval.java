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
import it.unive.lisa.symbolic.value.*;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
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
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeTokenType;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.numeric.MathNumberConversionException;
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

		if (constant.getStaticType() == JavaFloatType.INSTANCE || constant.getStaticType() == JavaDoubleType.INSTANCE) {
			double valueD = ((Number) constant.getValue()).doubleValue();
			if (Double.isNaN(valueD))
				return IntInterval.BOTTOM; // not a number
			if (Double.isInfinite(valueD))
				return IntInterval.TOP;

			return new IntInterval(new MathNumber(valueD), new MathNumber(valueD));
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

		if (operator instanceof JavaStringLengthOperator)
			return new IntInterval(MathNumber.ZERO, MathNumber.PLUS_INFINITY);

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
			return trigonometric(arg, SIN, 4 * Math.PI);
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
			// no refinement for non-integral targets (e.g., float, double):
			// fall back to the default behavior
			return super.evalTypeConv(conv, left, right, pp, oracle);

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
		return super.assume(environment, expression, src, dest, oracle);
	}

        @Override
        public ValueEnvironment<IntInterval> assumeBinaryExpression(
                ValueEnvironment<IntInterval> environment,
                BinaryExpression expression,
                ProgramPoint src,
                ProgramPoint dest,
                SemanticOracle oracle)
                throws SemanticException {

                Satisfiability sat = satisfies(environment, expression, src, oracle);
                if (sat == Satisfiability.NOT_SATISFIED)
                        return environment.bottom();
                if (sat == Satisfiability.SATISFIED)
                        return environment;

                if (expression.getOperator() != ComparisonNe.INSTANCE) {
                        return super.assumeBinaryExpression(environment, expression, src, dest, oracle);
                }

                Identifier id;
                IntInterval eval;
                IntInterval evalId;
                boolean rightIsExpr;
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
                        rightIsExpr = true;
                } else if (right instanceof Identifier) {
                        if (!canProcess(left, src, oracle))
                                // the expression does not have a numerical value, we do not
                                // assume anything on it
                                return environment;
                        eval = eval(environment, left, src, oracle);
                        evalId = eval(environment, right, src, oracle);
                        id = (Identifier) right;
                        rightIsExpr = false;
                } else
                        return environment;

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

        private MathNumber nextValueUp(Identifier id, MathNumber x) {
                if (id.getStaticType() == JavaFloatType.INSTANCE || id.getStaticType() == JavaDoubleType.INSTANCE) {
                        // TODO
                        return x;
                }
                return x.add(new MathNumber(1));
        }

        private MathNumber nextValueDown(Identifier id, MathNumber x) {
                if (id.getStaticType() == JavaFloatType.INSTANCE || id.getStaticType() == JavaDoubleType.INSTANCE) {
                        // TODO
                        return x;
                }
                return x.subtract(new MathNumber(1));
        }

}

package it.unive.jlisa.program.operator;

import it.unive.jlisa.program.type.JavaByteType;
import it.unive.jlisa.program.type.JavaCharType;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaDoubleType;
import it.unive.jlisa.program.type.JavaFloatType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.program.type.JavaLongType;
import it.unive.jlisa.program.type.JavaNumericType;
import it.unive.jlisa.program.type.JavaShortType;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.Numeric16BitAdd;
import it.unive.lisa.symbolic.value.operator.binary.Numeric16BitMul;
import it.unive.lisa.symbolic.value.operator.binary.Numeric16BitSub;
import it.unive.lisa.symbolic.value.operator.binary.Numeric32BitAdd;
import it.unive.lisa.symbolic.value.operator.binary.Numeric32BitMul;
import it.unive.lisa.symbolic.value.operator.binary.Numeric32BitSub;
import it.unive.lisa.symbolic.value.operator.binary.Numeric64BitAdd;
import it.unive.lisa.symbolic.value.operator.binary.Numeric64BitMul;
import it.unive.lisa.symbolic.value.operator.binary.Numeric64BitSub;
import it.unive.lisa.symbolic.value.operator.binary.Numeric8BitAdd;
import it.unive.lisa.symbolic.value.operator.binary.Numeric8BitMul;
import it.unive.lisa.symbolic.value.operator.binary.Numeric8BitSub;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingAdd;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingMul;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingSub;
import it.unive.lisa.symbolic.value.operator.binary.TypeConv;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeTokenType;
import it.unive.lisa.type.Untyped;
import java.util.Collections;
import java.util.Set;

/**
 * Selection of the operators modeling Java's arithmetic. Integral arithmetic
 * wraps around on overflow (two's complement), and it is thus modeled with the
 * overflowing operators of the bit width of the type of the operation (e.g.,
 * {@link Numeric32BitAdd} for {@code int}). Floating point arithmetic, and
 * arithmetic over values of unknown type, use the non-overflowing operators.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public final class JavaArithmeticOperators {

	private JavaArithmeticOperators() {
	}

	/**
	 * Yields the type of a binary arithmetic operation between values of the
	 * given types, following Java's binary numeric promotion (JLS 5.6.2):
	 * {@code double} if one of them is a {@code double}, otherwise
	 * {@code float} if one of them is a {@code float}, otherwise {@code long}
	 * if one of them is a {@code long}, otherwise {@code int}. Wrapper types
	 * are unboxed first. If one of the types is not numeric, the result is
	 * {@link Untyped}.
	 *
	 * @param left  the type of the left operand
	 * @param right the type of the right operand
	 *
	 * @return the type of the operation
	 */
	public static Type promote(
			Type left,
			Type right) {
		Type l = unbox(left);
		Type r = unbox(right);
		if (!(l instanceof JavaNumericType) || !(r instanceof JavaNumericType))
			return Untyped.INSTANCE;
		if (l instanceof JavaDoubleType || r instanceof JavaDoubleType)
			return JavaDoubleType.INSTANCE;
		if (l instanceof JavaFloatType || r instanceof JavaFloatType)
			return JavaFloatType.INSTANCE;
		if (l instanceof JavaLongType || r instanceof JavaLongType)
			return JavaLongType.INSTANCE;
		return JavaIntType.INSTANCE;
	}

	/**
	 * Yields the type of a binary arithmetic operation between values of the
	 * given runtime types, if all the numeric ones agree on it according to
	 * {@link #promote(Type, Type)}. If no pair of numeric types exists, or if
	 * the pairs promote to different types, the result is {@link Untyped}.
	 *
	 * @param left  the runtime types of the left operand
	 * @param right the runtime types of the right operand
	 *
	 * @return the type of the operation
	 */
	public static Type promote(
			Set<Type> left,
			Set<Type> right) {
		Type result = null;
		for (Type l : left)
			for (Type r : right) {
				Type t = promote(l, r);
				if (t.isUntyped())
					continue;
				if (result != null && !result.equals(t))
					return Untyped.INSTANCE;
				result = t;
			}
		return result == null ? Untyped.INSTANCE : result;
	}

	/**
	 * Yields the type of an increment or decrement of a value with the given
	 * runtime types: the primitive numeric type of the value (see
	 * {@link #unbox(Type)}) after unary numeric promotion (JLS 5.6), if all its
	 * numeric runtime types agree on it, or {@link Untyped} otherwise.
	 * Increments and decrements of {@code byte}, {@code short} and {@code char}
	 * values are thus {@code int} operations, whose result is then narrowed to
	 * the type of the value (see
	 * {@link #narrowed(Set, SymbolicExpression, CodeLocation)}).
	 *
	 * @param types the runtime types of the value
	 *
	 * @return the type of the operation
	 */
	public static Type operandType(
			Set<Type> types) {
		Type result = null;
		for (Type t : types) {
			Type u = promote(t, JavaIntType.INSTANCE);
			if (u.isUntyped())
				continue;
			if (result != null && !result.equals(u))
				return Untyped.INSTANCE;
			result = u;
		}
		return result == null ? Untyped.INSTANCE : result;
	}

	/**
	 * Yields the given result of an increment or decrement of a value with the
	 * given runtime types, converted back to the type of the value if it is
	 * {@code byte}, {@code short} or {@code char} (JLS 15.14.2, 15.15.1: the
	 * result is narrowed to the type of the variable), when all the numeric
	 * runtime types of the value agree on it. Otherwise, the result is yielded
	 * as it is.
	 *
	 * @param types    the runtime types of the value
	 * @param result   the result of the operation
	 * @param location the location of the operation
	 *
	 * @return the narrowed result
	 */
	public static SymbolicExpression narrowed(
			Set<Type> types,
			SymbolicExpression result,
			CodeLocation location) {
		Type target = null;
		for (Type t : types) {
			if (!t.isNumericType())
				continue;
			if (target != null && !target.equals(t))
				return result;
			target = t;
		}
		if (!(target instanceof JavaByteType || target instanceof JavaShortType || target instanceof JavaCharType))
			return result;
		return new BinaryExpression(target, result,
				new Constant(new TypeTokenType(Collections.singleton(target)), target, location), TypeConv.INSTANCE,
				location);
	}

	/**
	 * Yields the primitive type of the given type, that is, the type itself if
	 * it is not a wrapper type, or the type it wraps otherwise.
	 *
	 * @param type the type
	 *
	 * @return the primitive type
	 */
	public static Type unbox(
			Type type) {
		Type inner = type.isReferenceType() ? type.asReferenceType().getInnerType() : type;
		Type unwrapped = JavaClassType.getUnwrappedType(inner);
		return unwrapped == null ? type : unwrapped;
	}

	/**
	 * Yields the operator modeling an addition of the given type.
	 *
	 * @param type the type of the operation
	 *
	 * @return the operator
	 */
	public static BinaryOperator add(
			Type type) {
		switch (integralBits(type)) {
		case 8:
			return Numeric8BitAdd.INSTANCE;
		case 16:
			return Numeric16BitAdd.INSTANCE;
		case 32:
			return Numeric32BitAdd.INSTANCE;
		case 64:
			return Numeric64BitAdd.INSTANCE;
		default:
			return NumericNonOverflowingAdd.INSTANCE;
		}
	}

	/**
	 * Yields the operator modeling a subtraction of the given type.
	 *
	 * @param type the type of the operation
	 *
	 * @return the operator
	 */
	public static BinaryOperator sub(
			Type type) {
		switch (integralBits(type)) {
		case 8:
			return Numeric8BitSub.INSTANCE;
		case 16:
			return Numeric16BitSub.INSTANCE;
		case 32:
			return Numeric32BitSub.INSTANCE;
		case 64:
			return Numeric64BitSub.INSTANCE;
		default:
			return NumericNonOverflowingSub.INSTANCE;
		}
	}

	/**
	 * Yields the operator modeling a multiplication of the given type.
	 *
	 * @param type the type of the operation
	 *
	 * @return the operator
	 */
	public static BinaryOperator mul(
			Type type) {
		switch (integralBits(type)) {
		case 8:
			return Numeric8BitMul.INSTANCE;
		case 16:
			return Numeric16BitMul.INSTANCE;
		case 32:
			return Numeric32BitMul.INSTANCE;
		case 64:
			return Numeric64BitMul.INSTANCE;
		default:
			return NumericNonOverflowingMul.INSTANCE;
		}
	}

	private static int integralBits(
			Type type) {
		if (!(type instanceof JavaNumericType))
			return -1;
		JavaNumericType num = (JavaNumericType) type;
		return num.isIntegral() ? num.getNBits() : -1;
	}
}

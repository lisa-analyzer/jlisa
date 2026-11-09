package it.unive.jlisa.program.operator;

import it.unive.jlisa.program.type.JavaBooleanType;
import it.unive.jlisa.program.type.JavaByteType;
import it.unive.jlisa.program.type.JavaCharType;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.program.type.JavaLongType;
import it.unive.jlisa.program.type.JavaShortType;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.ternary.TernaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Collections;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A pure function of the Java library over primitive values (e.g.,
 * {@link Integer#bitCount(int)}), used as a unary or binary operator. The
 * function is evaluated, when its arguments are constants, by running the
 * corresponding method of the Java library of the analyzer, so that its
 * semantics is exactly the one of the library. Each function is a singleton,
 * identified by its name.<br>
 * <br>
 * Arguments are given as the values of constants of the analysis, and are
 * converted to the types of the parameters of the function (e.g., characters
 * might be represented as {@link Character}s or {@link Integer}s). Functions
 * returning strings ({@link #returnsString()}) yield the contents of the
 * strings, that models of the library store in new string objects.
 */
public final class JavaLibraryFunction implements UnaryOperator, BinaryOperator, TernaryOperator {

	// Integer

	public static final JavaLibraryFunction INTEGER_BIT_COUNT = unary("Integer.bitCount", JavaIntType.INSTANCE,
			a -> Integer.bitCount(i(a)));

	public static final JavaLibraryFunction INTEGER_COMPARE = binary("Integer.compare", JavaIntType.INSTANCE,
			(
					a,
					b) -> Integer.compare(i(a), i(b)));

	public static final JavaLibraryFunction INTEGER_COMPARE_UNSIGNED = binary("Integer.compareUnsigned",
			JavaIntType.INSTANCE, (
					a,
					b) -> Integer.compareUnsigned(i(a), i(b)));

	public static final JavaLibraryFunction INTEGER_HIGHEST_ONE_BIT = unary("Integer.highestOneBit",
			JavaIntType.INSTANCE, a -> Integer.highestOneBit(i(a)));

	public static final JavaLibraryFunction INTEGER_LOWEST_ONE_BIT = unary("Integer.lowestOneBit",
			JavaIntType.INSTANCE, a -> Integer.lowestOneBit(i(a)));

	public static final JavaLibraryFunction INTEGER_MAX = binary("Integer.max", JavaIntType.INSTANCE,
			(
					a,
					b) -> Integer.max(i(a), i(b)));

	public static final JavaLibraryFunction INTEGER_MIN = binary("Integer.min", JavaIntType.INSTANCE,
			(
					a,
					b) -> Integer.min(i(a), i(b)));

	public static final JavaLibraryFunction INTEGER_NUMBER_OF_LEADING_ZEROS = unary("Integer.numberOfLeadingZeros",
			JavaIntType.INSTANCE, a -> Integer.numberOfLeadingZeros(i(a)));

	public static final JavaLibraryFunction INTEGER_NUMBER_OF_TRAILING_ZEROS = unary(
			"Integer.numberOfTrailingZeros", JavaIntType.INSTANCE, a -> Integer.numberOfTrailingZeros(i(a)));

	public static final JavaLibraryFunction INTEGER_REVERSE = unary("Integer.reverse", JavaIntType.INSTANCE,
			a -> Integer.reverse(i(a)));

	public static final JavaLibraryFunction INTEGER_REVERSE_BYTES = unary("Integer.reverseBytes",
			JavaIntType.INSTANCE, a -> Integer.reverseBytes(i(a)));

	public static final JavaLibraryFunction INTEGER_ROTATE_LEFT = binary("Integer.rotateLeft", JavaIntType.INSTANCE,
			(
					a,
					b) -> Integer.rotateLeft(i(a), i(b)));

	public static final JavaLibraryFunction INTEGER_ROTATE_RIGHT = binary("Integer.rotateRight",
			JavaIntType.INSTANCE, (
					a,
					b) -> Integer.rotateRight(i(a), i(b)));

	public static final JavaLibraryFunction INTEGER_SIGNUM = unary("Integer.signum", JavaIntType.INSTANCE,
			a -> Integer.signum(i(a)));

	public static final JavaLibraryFunction INTEGER_SUM = binary("Integer.sum", JavaIntType.INSTANCE,
			(
					a,
					b) -> Integer.sum(i(a), i(b)));

	public static final JavaLibraryFunction INTEGER_TO_UNSIGNED_LONG = unary("Integer.toUnsignedLong",
			JavaLongType.INSTANCE, a -> Integer.toUnsignedLong(i(a)));

	// Long

	public static final JavaLibraryFunction LONG_COMPARE = binary("Long.compare", JavaIntType.INSTANCE,
			(
					a,
					b) -> Long.compare(l(a), l(b)));

	public static final JavaLibraryFunction LONG_COMPARE_UNSIGNED = binary("Long.compareUnsigned",
			JavaIntType.INSTANCE, (
					a,
					b) -> Long.compareUnsigned(l(a), l(b)));

	public static final JavaLibraryFunction LONG_HIGHEST_ONE_BIT = unary("Long.highestOneBit", JavaLongType.INSTANCE,
			a -> Long.highestOneBit(l(a)));

	public static final JavaLibraryFunction LONG_LOWEST_ONE_BIT = unary("Long.lowestOneBit", JavaLongType.INSTANCE,
			a -> Long.lowestOneBit(l(a)));

	public static final JavaLibraryFunction LONG_MAX = binary("Long.max", JavaLongType.INSTANCE,
			(
					a,
					b) -> Long.max(l(a), l(b)));

	public static final JavaLibraryFunction LONG_MIN = binary("Long.min", JavaLongType.INSTANCE,
			(
					a,
					b) -> Long.min(l(a), l(b)));

	public static final JavaLibraryFunction LONG_NUMBER_OF_LEADING_ZEROS = unary("Long.numberOfLeadingZeros",
			JavaIntType.INSTANCE, a -> Long.numberOfLeadingZeros(l(a)));

	public static final JavaLibraryFunction LONG_NUMBER_OF_TRAILING_ZEROS = unary("Long.numberOfTrailingZeros",
			JavaIntType.INSTANCE, a -> Long.numberOfTrailingZeros(l(a)));

	public static final JavaLibraryFunction LONG_REVERSE = unary("Long.reverse", JavaLongType.INSTANCE,
			a -> Long.reverse(l(a)));

	public static final JavaLibraryFunction LONG_REVERSE_BYTES = unary("Long.reverseBytes", JavaLongType.INSTANCE,
			a -> Long.reverseBytes(l(a)));

	public static final JavaLibraryFunction LONG_ROTATE_LEFT = binary("Long.rotateLeft", JavaLongType.INSTANCE,
			(
					a,
					b) -> Long.rotateLeft(l(a), i(b)));

	public static final JavaLibraryFunction LONG_SIGNUM = unary("Long.signum", JavaIntType.INSTANCE,
			a -> Long.signum(l(a)));

	public static final JavaLibraryFunction LONG_SUM = binary("Long.sum", JavaLongType.INSTANCE,
			(
					a,
					b) -> Long.sum(l(a), l(b)));

	// Short and Byte

	public static final JavaLibraryFunction SHORT_COMPARE = binary("Short.compare", JavaIntType.INSTANCE,
			(
					a,
					b) -> Short.compare(s(a), s(b)));

	public static final JavaLibraryFunction SHORT_REVERSE_BYTES = unary("Short.reverseBytes",
			JavaShortType.INSTANCE, a -> Short.reverseBytes(s(a)));

	public static final JavaLibraryFunction SHORT_TO_UNSIGNED_INT = unary("Short.toUnsignedInt",
			JavaIntType.INSTANCE, a -> Short.toUnsignedInt(s(a)));

	public static final JavaLibraryFunction SHORT_TO_UNSIGNED_LONG = unary("Short.toUnsignedLong",
			JavaLongType.INSTANCE, a -> Short.toUnsignedLong(s(a)));

	public static final JavaLibraryFunction BYTE_COMPARE = binary("Byte.compare", JavaIntType.INSTANCE,
			(
					a,
					b) -> Byte.compare(by(a), by(b)));

	// Boolean

	public static final JavaLibraryFunction BOOLEAN_COMPARE = binary("Boolean.compare", JavaIntType.INSTANCE,
			(
					a,
					b) -> Boolean.compare(b(a), b(b)));

	public static final JavaLibraryFunction BOOLEAN_LOGICAL_AND = binary("Boolean.logicalAnd",
			JavaBooleanType.INSTANCE, (
					a,
					b) -> Boolean.logicalAnd(b(a), b(b)));

	public static final JavaLibraryFunction BOOLEAN_LOGICAL_OR = binary("Boolean.logicalOr",
			JavaBooleanType.INSTANCE, (
					a,
					b) -> Boolean.logicalOr(b(a), b(b)));

	public static final JavaLibraryFunction BOOLEAN_LOGICAL_XOR = binary("Boolean.logicalXor",
			JavaBooleanType.INSTANCE, (
					a,
					b) -> Boolean.logicalXor(b(a), b(b)));

	// Character (code points)

	public static final JavaLibraryFunction CHARACTER_COMPARE = binary("Character.compare", JavaIntType.INSTANCE,
			(
					a,
					b) -> Character.compare(c(a), c(b)));

	public static final JavaLibraryFunction CHARACTER_CHAR_COUNT = unary("Character.charCount", JavaIntType.INSTANCE,
			a -> Character.charCount(i(a)));

	public static final JavaLibraryFunction CHARACTER_TO_CODE_POINT = binary("Character.toCodePoint",
			JavaIntType.INSTANCE, (
					a,
					b) -> Character.toCodePoint(c(a), c(b)));

	public static final JavaLibraryFunction CHARACTER_DIGIT = binary("Character.digit", JavaIntType.INSTANCE,
			(
					a,
					b) -> Character.digit(i(a), i(b)));

	public static final JavaLibraryFunction CHARACTER_GET_DIRECTIONALITY = unary("Character.getDirectionality",
			JavaByteType.INSTANCE, a -> Character.getDirectionality(i(a)));

	public static final JavaLibraryFunction CHARACTER_GET_NUMERIC_VALUE = unary("Character.getNumericValue",
			JavaIntType.INSTANCE, a -> Character.getNumericValue(i(a)));

	public static final JavaLibraryFunction CHARACTER_GET_TYPE = unary("Character.getType", JavaIntType.INSTANCE,
			a -> Character.getType(i(a)));

	public static final JavaLibraryFunction CHARACTER_TO_LOWER_CASE = unary("Character.toLowerCase(int)",
			JavaIntType.INSTANCE, a -> Character.toLowerCase(i(a)));

	public static final JavaLibraryFunction CHARACTER_TO_UPPER_CASE = unary("Character.toUpperCase(int)",
			JavaIntType.INSTANCE, a -> Character.toUpperCase(i(a)));

	public static final JavaLibraryFunction CHARACTER_TO_TITLE_CASE = unary("Character.toTitleCase(int)",
			JavaIntType.INSTANCE, a -> Character.toTitleCase(i(a)));

	public static final JavaLibraryFunction CHARACTER_TO_TITLE_CASE_CHAR = unary("Character.toTitleCase(char)",
			JavaCharType.INSTANCE, a -> Character.toTitleCase(c(a)));

	// Character (predicates over code points: the char versions are the
	// same functions, since chars are converted to code points)

	public static final JavaLibraryFunction CHARACTER_IS_ALPHABETIC = predicate("Character.isAlphabetic",
			Character::isAlphabetic);

	public static final JavaLibraryFunction CHARACTER_IS_BMP_CODE_POINT = predicate("Character.isBmpCodePoint",
			Character::isBmpCodePoint);

	public static final JavaLibraryFunction CHARACTER_IS_DEFINED = predicate("Character.isDefined",
			Character::isDefined);

	public static final JavaLibraryFunction CHARACTER_IS_DIGIT = predicate("Character.isDigit", Character::isDigit);

	public static final JavaLibraryFunction CHARACTER_IS_IDENTIFIER_IGNORABLE = predicate(
			"Character.isIdentifierIgnorable", Character::isIdentifierIgnorable);

	public static final JavaLibraryFunction CHARACTER_IS_IDEOGRAPHIC = predicate("Character.isIdeographic",
			Character::isIdeographic);

	public static final JavaLibraryFunction CHARACTER_IS_ISO_CONTROL = predicate("Character.isISOControl",
			Character::isISOControl);

	public static final JavaLibraryFunction CHARACTER_IS_JAVA_IDENTIFIER_PART = predicate(
			"Character.isJavaIdentifierPart", Character::isJavaIdentifierPart);

	public static final JavaLibraryFunction CHARACTER_IS_JAVA_IDENTIFIER_START = predicate(
			"Character.isJavaIdentifierStart", Character::isJavaIdentifierStart);

	public static final JavaLibraryFunction CHARACTER_IS_LETTER = predicate("Character.isLetter",
			Character::isLetter);

	public static final JavaLibraryFunction CHARACTER_IS_LETTER_OR_DIGIT = predicate("Character.isLetterOrDigit",
			Character::isLetterOrDigit);

	public static final JavaLibraryFunction CHARACTER_IS_LOWER_CASE = predicate("Character.isLowerCase",
			Character::isLowerCase);

	public static final JavaLibraryFunction CHARACTER_IS_MIRRORED = predicate("Character.isMirrored",
			Character::isMirrored);

	public static final JavaLibraryFunction CHARACTER_IS_SPACE_CHAR = predicate("Character.isSpaceChar",
			Character::isSpaceChar);

	public static final JavaLibraryFunction CHARACTER_IS_SUPPLEMENTARY_CODE_POINT = predicate(
			"Character.isSupplementaryCodePoint", Character::isSupplementaryCodePoint);

	public static final JavaLibraryFunction CHARACTER_IS_TITLE_CASE = predicate("Character.isTitleCase",
			Character::isTitleCase);

	public static final JavaLibraryFunction CHARACTER_IS_UNICODE_IDENTIFIER_PART = predicate(
			"Character.isUnicodeIdentifierPart", Character::isUnicodeIdentifierPart);

	public static final JavaLibraryFunction CHARACTER_IS_UNICODE_IDENTIFIER_START = predicate(
			"Character.isUnicodeIdentifierStart", Character::isUnicodeIdentifierStart);

	public static final JavaLibraryFunction CHARACTER_IS_UPPER_CASE = predicate("Character.isUpperCase",
			Character::isUpperCase);

	public static final JavaLibraryFunction CHARACTER_IS_VALID_CODE_POINT = predicate(
			"Character.isValidCodePoint", Character::isValidCodePoint);

	public static final JavaLibraryFunction CHARACTER_IS_WHITESPACE = predicate("Character.isWhitespace",
			Character::isWhitespace);

	@SuppressWarnings("deprecation")
	public static final JavaLibraryFunction CHARACTER_IS_JAVA_LETTER = unary("Character.isJavaLetter",
			JavaBooleanType.INSTANCE, a -> Character.isJavaLetter(c(a)));

	@SuppressWarnings("deprecation")
	public static final JavaLibraryFunction CHARACTER_IS_JAVA_LETTER_OR_DIGIT = unary(
			"Character.isJavaLetterOrDigit", JavaBooleanType.INSTANCE, a -> Character.isJavaLetterOrDigit(c(a)));

	@SuppressWarnings("deprecation")
	public static final JavaLibraryFunction CHARACTER_IS_SPACE = unary("Character.isSpace",
			JavaBooleanType.INSTANCE, a -> Character.isSpace(c(a)));

	// conversions to strings

	public static final JavaLibraryFunction INTEGER_TO_STRING = string1("Integer.toString",
			a -> Integer.toString(i(a)));

	public static final JavaLibraryFunction INTEGER_TO_STRING_RADIX = string2("Integer.toString(radix)",
			(
					a,
					b) -> Integer.toString(i(a), i(b)));

	public static final JavaLibraryFunction INTEGER_TO_UNSIGNED_STRING = string1("Integer.toUnsignedString",
			a -> Integer.toUnsignedString(i(a)));

	public static final JavaLibraryFunction INTEGER_TO_UNSIGNED_STRING_RADIX = string2(
			"Integer.toUnsignedString(radix)", (
					a,
					b) -> Integer.toUnsignedString(i(a), i(b)));

	public static final JavaLibraryFunction INTEGER_TO_HEX_STRING = string1("Integer.toHexString",
			a -> Integer.toHexString(i(a)));

	public static final JavaLibraryFunction INTEGER_TO_BINARY_STRING = string1("Integer.toBinaryString",
			a -> Integer.toBinaryString(i(a)));

	public static final JavaLibraryFunction INTEGER_TO_OCTAL_STRING = string1("Integer.toOctalString",
			a -> Integer.toOctalString(i(a)));

	public static final JavaLibraryFunction LONG_TO_STRING = string1("Long.toString", a -> Long.toString(l(a)));

	public static final JavaLibraryFunction LONG_TO_STRING_RADIX = string2("Long.toString(radix)",
			(
					a,
					b) -> Long.toString(l(a), i(b)));

	public static final JavaLibraryFunction LONG_TO_UNSIGNED_STRING = string1("Long.toUnsignedString",
			a -> Long.toUnsignedString(l(a)));

	public static final JavaLibraryFunction LONG_TO_UNSIGNED_STRING_RADIX = string2("Long.toUnsignedString(radix)",
			(
					a,
					b) -> Long.toUnsignedString(l(a), i(b)));

	public static final JavaLibraryFunction LONG_TO_HEX_STRING = string1("Long.toHexString",
			a -> Long.toHexString(l(a)));

	public static final JavaLibraryFunction LONG_TO_BINARY_STRING = string1("Long.toBinaryString",
			a -> Long.toBinaryString(l(a)));

	public static final JavaLibraryFunction LONG_TO_OCTAL_STRING = string1("Long.toOctalString",
			a -> Long.toOctalString(l(a)));

	public static final JavaLibraryFunction SHORT_TO_STRING = string1("Short.toString", a -> Short.toString(s(a)));

	public static final JavaLibraryFunction BYTE_TO_STRING = string1("Byte.toString", a -> Byte.toString(by(a)));

	public static final JavaLibraryFunction BOOLEAN_TO_STRING = string1("Boolean.toString",
			a -> Boolean.toString(b(a)));

	public static final JavaLibraryFunction CHARACTER_TO_STRING = string1("Character.toString",
			a -> Character.toString(c(a)));

	public static final JavaLibraryFunction DOUBLE_TO_STRING = string1("Double.toString",
			a -> Double.toString(((Number) a).doubleValue()));

	public static final JavaLibraryFunction FLOAT_TO_STRING = string1("Float.toString",
			a -> Float.toString(((Number) a).floatValue()));

	// String (the first argument is the value of the receiver)

	public static final JavaLibraryFunction STRING_IS_EMPTY = unary("String.isEmpty", JavaBooleanType.INSTANCE,
			a -> str(a).isEmpty());

	public static final JavaLibraryFunction STRING_INDEX_OF_CHAR = binary("String.indexOf(int)",
			JavaIntType.INSTANCE, (
					a,
					b) -> str(a).indexOf(i(b)));

	public static final JavaLibraryFunction STRING_INDEX_OF_CHAR_FROM = ternary("String.indexOf(int, int)",
			JavaIntType.INSTANCE, args -> str(args[0]).indexOf(i(args[1]), i(args[2])));

	public static final JavaLibraryFunction STRING_LAST_INDEX_OF_CHAR = binary("String.lastIndexOf(int)",
			JavaIntType.INSTANCE, (
					a,
					b) -> str(a).lastIndexOf(i(b)));

	public static final JavaLibraryFunction STRING_LAST_INDEX_OF_CHAR_FROM = ternary("String.lastIndexOf(int, int)",
			JavaIntType.INSTANCE, args -> str(args[0]).lastIndexOf(i(args[1]), i(args[2])));

	public static final JavaLibraryFunction STRING_COMPARE_TO_IGNORE_CASE = binary("String.compareToIgnoreCase",
			JavaIntType.INSTANCE, (
					a,
					b) -> str(a).compareToIgnoreCase(str(b)));

	// Double and Float

	public static final JavaLibraryFunction DOUBLE_COMPARE = binary("Double.compare", JavaIntType.INSTANCE,
			(
					a,
					b) -> Double.compare(((Number) a).doubleValue(), ((Number) b).doubleValue()));

	public static final JavaLibraryFunction FLOAT_COMPARE = binary("Float.compare", JavaIntType.INSTANCE,
			(
					a,
					b) -> Float.compare(((Number) a).floatValue(), ((Number) b).floatValue()));

	// Math

	public static final JavaLibraryFunction MATH_GET_EXPONENT_DOUBLE = unary("Math.getExponent(double)",
			JavaIntType.INSTANCE, a -> Math.getExponent(((Number) a).doubleValue()));

	public static final JavaLibraryFunction MATH_GET_EXPONENT_FLOAT = unary("Math.getExponent(float)",
			JavaIntType.INSTANCE, a -> Math.getExponent(((Number) a).floatValue()));

	private final String name;

	private final Type result;

	private final boolean string;

	private final Function<Object[], Object> function;

	private JavaLibraryFunction(
			String name,
			Type result,
			boolean string,
			Function<Object[], Object> function) {
		this.name = name;
		this.result = result;
		this.string = string;
		this.function = function;
	}

	private static JavaLibraryFunction unary(
			String name,
			Type result,
			Function<Object, Object> function) {
		return new JavaLibraryFunction(name, result, false, args -> function.apply(args[0]));
	}

	private static JavaLibraryFunction binary(
			String name,
			Type result,
			BiFunction<Object, Object, Object> function) {
		return new JavaLibraryFunction(name, result, false, args -> function.apply(args[0], args[1]));
	}

	private static JavaLibraryFunction ternary(
			String name,
			Type result,
			Function<Object[], Object> function) {
		return new JavaLibraryFunction(name, result, false, function);
	}

	private static JavaLibraryFunction string1(
			String name,
			Function<Object, Object> function) {
		return new JavaLibraryFunction(name, null, true, args -> function.apply(args[0]));
	}

	private static JavaLibraryFunction string2(
			String name,
			BiFunction<Object, Object, Object> function) {
		return new JavaLibraryFunction(name, null, true, args -> function.apply(args[0], args[1]));
	}

	private static JavaLibraryFunction predicate(
			String name,
			Function<Integer, Boolean> function) {
		return unary(name, JavaBooleanType.INSTANCE, a -> function.apply(i(a)));
	}

	/**
	 * Yields the type of the values returned by this function.
	 *
	 * @return the type
	 */
	public Type getResultType() {
		return string ? JavaClassType.getStringType() : result;
	}

	/**
	 * Yields {@code true} if this function returns strings.
	 *
	 * @return whether or not this function returns strings
	 */
	public boolean returnsString() {
		return string;
	}

	/**
	 * Applies this function to the given values of constants. Characters are
	 * returned as {@link Character}s, and the other values as the boxed
	 * versions of the return types of the corresponding methods.
	 *
	 * @param args the values of the arguments
	 *
	 * @return the result
	 *
	 * @throws RuntimeException if the values cannot be converted to the types
	 *                              of the parameters of the function
	 */
	public Object apply(
			Object... args) {
		return function.apply(args);
	}

	@Override
	public Set<Type> typeInference(
			TypeSystem types,
			Set<Type> argument) {
		return argument.isEmpty() ? Collections.emptySet() : Collections.singleton(getResultType());
	}

	@Override
	public Set<Type> typeInference(
			TypeSystem types,
			Set<Type> left,
			Set<Type> right) {
		return left.isEmpty() || right.isEmpty() ? Collections.emptySet()
				: Collections.singleton(getResultType());
	}

	@Override
	public Set<Type> typeInference(
			TypeSystem types,
			Set<Type> left,
			Set<Type> middle,
			Set<Type> right) {
		return left.isEmpty() || middle.isEmpty() || right.isEmpty() ? Collections.emptySet()
				: Collections.singleton(getResultType());
	}

	@Override
	public String toString() {
		return name;
	}

	private static int i(
			Object value) {
		if (value instanceof Character)
			return (Character) value;
		if (value instanceof Number)
			return ((Number) value).intValue();
		throw new IllegalArgumentException("Not an integral value: " + value);
	}

	private static long l(
			Object value) {
		if (value instanceof Character)
			return (Character) value;
		if (value instanceof Number)
			return ((Number) value).longValue();
		throw new IllegalArgumentException("Not an integral value: " + value);
	}

	private static short s(
			Object value) {
		return (short) i(value);
	}

	private static byte by(
			Object value) {
		return (byte) i(value);
	}

	private static char c(
			Object value) {
		return (char) i(value);
	}

	private static String str(
			Object value) {
		if (value instanceof String)
			return (String) value;
		throw new IllegalArgumentException("Not a string: " + value);
	}

	private static boolean b(
			Object value) {
		if (value instanceof Boolean)
			return (Boolean) value;
		throw new IllegalArgumentException("Not a boolean value: " + value);
	}
}

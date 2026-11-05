package it.unive.jlisa.triage;

import it.unive.jlisa.program.operator.JavaByteCompareOperator;
import it.unive.jlisa.program.operator.JavaCharacterDigitOperator;
import it.unive.jlisa.program.operator.JavaCharacterIsDefinedOperator;
import it.unive.jlisa.program.operator.JavaCharacterIsDigitOperator;
import it.unive.jlisa.program.operator.JavaCharacterIsJavaIdentifierPartOperator;
import it.unive.jlisa.program.operator.JavaCharacterIsJavaIdentifierStartOperator;
import it.unive.jlisa.program.operator.JavaCharacterIsLetterOperator;
import it.unive.jlisa.program.operator.JavaCharacterIsLetterOrDigitOperator;
import it.unive.jlisa.program.operator.JavaCharacterIsLowerCaseOperator;
import it.unive.jlisa.program.operator.JavaCharacterIsUpperCaseOperator;
import it.unive.jlisa.program.operator.JavaCharacterToLowerCaseOperator;
import it.unive.jlisa.program.operator.JavaCharacterToUpperCaseOperator;
import it.unive.jlisa.program.operator.JavaDoubleCompareOperator;
import it.unive.jlisa.program.operator.JavaFloatCompareOperator;
import it.unive.jlisa.program.operator.JavaIntegerCompareOperator;
import it.unive.jlisa.program.operator.JavaLibraryFunction;
import it.unive.jlisa.program.operator.JavaLongBitCountOperator;
import it.unive.jlisa.program.operator.JavaLongCompareOperator;
import it.unive.jlisa.program.operator.JavaLongRotateRightOperator;
import it.unive.jlisa.program.operator.JavaMathMax;
import it.unive.jlisa.program.operator.JavaMathMin;
import it.unive.jlisa.program.operator.JavaShortCompareOperator;
import it.unive.jlisa.program.operator.JavaStrictMathGetExponentOperator;
import it.unive.jlisa.program.operator.JavaStrictMathMax;
import it.unive.jlisa.program.operator.JavaStrictMathMin;
import it.unive.jlisa.program.operator.JavaStringCompareToOperator;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.triage.encoding.ExpressionEncoder;
import it.unive.lisa.triage.encoding.SortPolicy;
import it.unive.lisa.triage.encoding.SymbolTable;
import it.unive.lisa.triage.smt.FloatingPoints;
import it.unive.lisa.triage.smt.Sort;
import it.unive.lisa.triage.smt.Term;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntFunction;

/**
 * An {@link ExpressionEncoder} that also encodes the operators of the Java
 * frontend, so that counterexamples relying on them can be confirmed. Functions
 * of the Java library ({@link JavaLibraryFunction}, and the operators of the
 * frontend modeling methods of the library) are encoded:
 * <ul>
 * <li>exactly over integers, booleans, strings and floating point numbers,
 * when they can be expressed with the operations of the SMT theories (e.g.,
 * comparisons, bit manipulations through the bits of the integers, conversions
 * of numbers to strings through their digits, searches in strings);</li>
 * <li>through tables over all code points, for the functions over characters
 * (e.g., {@link Character#getType(int)}): the tables are computed by running
 * the functions of the Java library on each code point, so that they agree
 * with the library by construction;</li>
 * <li>exactly only for some of their arguments, when no exact encoding is
 * known (e.g., {@link Double#toString(double)} is encoded only for integral
 * numbers and special values), the result being an unknown function of the
 * arguments otherwise.</li>
 * </ul>
 * Other functions yield unconstrained values, as any other operator unknown to
 * this encoder.
 */
public class JavaExpressionEncoder extends ExpressionEncoder {

	/**
	 * The tables built so far, shared by all encoders since they only depend
	 * on the functions.
	 */
	private static final Map<String, List<Segment>> TABLES = new ConcurrentHashMap<>();

	/**
	 * Builds the encoder.
	 *
	 * @param sorts   the policy deciding the sorts of identifiers
	 * @param symbols the provider of the constants of the encoding
	 */
	public JavaExpressionEncoder(
			SortPolicy sorts,
			SymbolTable symbols) {
		super(sorts, symbols);
	}

	@Override
	protected Term encodeUnary(
			UnaryExpression expression,
			Term arg)
			throws SemanticException {
		Term result = unary(expression.getOperator(), arg, expression);
		return result != null ? result : super.encodeUnary(expression, arg);
	}

	@Override
	protected Term encodeBinary(
			BinaryExpression expression,
			Term left,
			Term right)
			throws SemanticException {
		Term result = binary(expression.getOperator(), left, right, expression);
		return result != null ? result : super.encodeBinary(expression, left, right);
	}

	@Override
	protected Term encodeTernary(
			TernaryExpression expression,
			Term left,
			Term middle,
			Term right)
			throws SemanticException {
		if (expression.getOperator() == JavaLibraryFunction.STRING_INDEX_OF_CHAR_FROM) {
			Term s = asString(left, expression);
			Term from = coerce(right, Sort.INT, expression);
			return indexOf(s, coerce(middle, Sort.INT, expression), Term.ite(lt(from, num(0)), num(0), from),
					expression);
		}
		if (expression.getOperator() == JavaLibraryFunction.STRING_LAST_INDEX_OF_CHAR_FROM) {
			Term s = asString(left, expression);
			Term from = coerce(right, Sort.INT, expression);
			Term len = len(s);
			// the search starts at the given index, or at the end
			Term prefix = substr(s, num(0), Term.ite(ge(from, len), len, add(from, num(1))));
			return lastIndexOf(prefix, coerce(middle, Sort.INT, expression), expression);
		}
		return super.encodeTernary(expression, left, middle, right);
	}

	// unary operators

	private Term unary(
			UnaryOperator op,
			Term arg,
			UnaryExpression e) {
		// operators of the frontend modeling functions over chars
		if (op instanceof JavaCharacterIsDefinedOperator)
			return charTable("isDefined(char)", c -> Character.isDefined((char) c), arg, e);
		if (op instanceof JavaCharacterIsDigitOperator)
			return charTable("isDigit(char)", c -> Character.isDigit((char) c), arg, e);
		if (op instanceof JavaCharacterIsJavaIdentifierPartOperator)
			return charTable("isJavaIdentifierPart(char)", c -> Character.isJavaIdentifierPart((char) c), arg, e);
		if (op instanceof JavaCharacterIsJavaIdentifierStartOperator)
			return charTable("isJavaIdentifierStart(char)", c -> Character.isJavaIdentifierStart((char) c), arg, e);
		if (op instanceof JavaCharacterIsLetterOperator)
			return charTable("isLetter(char)", c -> Character.isLetter((char) c), arg, e);
		if (op instanceof JavaCharacterIsLetterOrDigitOperator)
			return charTable("isLetterOrDigit(char)", c -> Character.isLetterOrDigit((char) c), arg, e);
		if (op instanceof JavaCharacterIsLowerCaseOperator)
			return charTable("isLowerCase(char)", c -> Character.isLowerCase((char) c), arg, e);
		if (op instanceof JavaCharacterIsUpperCaseOperator)
			return charTable("isUpperCase(char)", c -> Character.isUpperCase((char) c), arg, e);
		if (op instanceof JavaCharacterToLowerCaseOperator)
			return charTable("toLowerCase(char)", c -> (int) Character.toLowerCase((char) c), arg, e);
		if (op instanceof JavaCharacterToUpperCaseOperator)
			return charTable("toUpperCase(char)", c -> (int) Character.toUpperCase((char) c), arg, e);
		if (op instanceof JavaStrictMathGetExponentOperator)
			return exponent(arg, e);
		if (op instanceof JavaLongBitCountOperator)
			return bitCount(unsigned(coerce(arg, Sort.INT, e), 64), 64);
		if (!(op instanceof JavaLibraryFunction))
			return null;

		JavaLibraryFunction f = (JavaLibraryFunction) op;
		if (f.toString().startsWith("Character.") && !f.returnsString()) {
			Term table = codePointTable(f, arg, e);
			if (table != null)
				return table;
		}
		if (f == JavaLibraryFunction.MATH_GET_EXPONENT_DOUBLE || f == JavaLibraryFunction.MATH_GET_EXPONENT_FLOAT)
			return exponent(arg, e);
		if (f == JavaLibraryFunction.STRING_IS_EMPTY)
			return Term.eq(len(asString(arg, e)), num(0));
		if (f == JavaLibraryFunction.BOOLEAN_TO_STRING)
			return Term.ite(coerce(arg, Sort.BOOL, e), Term.string("true"), Term.string("false"));
		if (f == JavaLibraryFunction.CHARACTER_TO_STRING)
			return Term.app("str.from_code", Sort.STRING, coerce(arg, Sort.INT, e));
		if (f == JavaLibraryFunction.DOUBLE_TO_STRING)
			return floatToString(toFloat(arg, Sort.FLOAT64, e), e);
		if (f == JavaLibraryFunction.FLOAT_TO_STRING)
			return floatToString(toFloat(arg, Sort.FLOAT32, e), e);

		Term x = coerce(arg, Sort.INT, e);
		if (f == JavaLibraryFunction.INTEGER_SIGNUM || f == JavaLibraryFunction.LONG_SIGNUM)
			return compare(x, num(0));
		if (f == JavaLibraryFunction.INTEGER_TO_UNSIGNED_LONG)
			return unsigned(x, 32);
		if (f == JavaLibraryFunction.SHORT_TO_UNSIGNED_INT || f == JavaLibraryFunction.SHORT_TO_UNSIGNED_LONG)
			return unsigned(x, 16);
		if (f == JavaLibraryFunction.INTEGER_BIT_COUNT)
			return bitCount(unsigned(x, 32), 32);
		if (f == JavaLibraryFunction.INTEGER_HIGHEST_ONE_BIT)
			return signed(highestOneBit(unsigned(x, 32), 32), 32);
		if (f == JavaLibraryFunction.LONG_HIGHEST_ONE_BIT)
			return signed(highestOneBit(unsigned(x, 64), 64), 64);
		if (f == JavaLibraryFunction.INTEGER_LOWEST_ONE_BIT)
			return signed(lowestOneBit(unsigned(x, 32), 32), 32);
		if (f == JavaLibraryFunction.LONG_LOWEST_ONE_BIT)
			return signed(lowestOneBit(unsigned(x, 64), 64), 64);
		if (f == JavaLibraryFunction.INTEGER_NUMBER_OF_LEADING_ZEROS)
			return leadingZeros(unsigned(x, 32), 32);
		if (f == JavaLibraryFunction.LONG_NUMBER_OF_LEADING_ZEROS)
			return leadingZeros(unsigned(x, 64), 64);
		if (f == JavaLibraryFunction.INTEGER_NUMBER_OF_TRAILING_ZEROS)
			return trailingZeros(unsigned(x, 32), 32);
		if (f == JavaLibraryFunction.LONG_NUMBER_OF_TRAILING_ZEROS)
			return trailingZeros(unsigned(x, 64), 64);
		if (f == JavaLibraryFunction.INTEGER_REVERSE)
			return signed(reverse(unsigned(x, 32), 32, 1), 32);
		if (f == JavaLibraryFunction.LONG_REVERSE)
			return signed(reverse(unsigned(x, 64), 64, 1), 64);
		if (f == JavaLibraryFunction.INTEGER_REVERSE_BYTES)
			return signed(reverse(unsigned(x, 32), 32, 8), 32);
		if (f == JavaLibraryFunction.LONG_REVERSE_BYTES)
			return signed(reverse(unsigned(x, 64), 64, 8), 64);
		if (f == JavaLibraryFunction.SHORT_REVERSE_BYTES)
			return signed(reverse(unsigned(x, 16), 16, 8), 16);
		if (f == JavaLibraryFunction.CHARACTER_CHAR_COUNT)
			return Term.ite(ge(x, num(Character.MIN_SUPPLEMENTARY_CODE_POINT)), num(2), num(1));
		if (f == JavaLibraryFunction.INTEGER_TO_STRING || f == JavaLibraryFunction.LONG_TO_STRING
				|| f == JavaLibraryFunction.SHORT_TO_STRING || f == JavaLibraryFunction.BYTE_TO_STRING)
			return decimal(x);
		if (f == JavaLibraryFunction.INTEGER_TO_UNSIGNED_STRING)
			return Term.app("str.from_int", Sort.STRING, unsigned(x, 32));
		if (f == JavaLibraryFunction.LONG_TO_UNSIGNED_STRING)
			return Term.app("str.from_int", Sort.STRING, unsigned(x, 64));
		if (f == JavaLibraryFunction.INTEGER_TO_HEX_STRING)
			return digits(unsigned(x, 32), 16, 32);
		if (f == JavaLibraryFunction.INTEGER_TO_BINARY_STRING)
			return digits(unsigned(x, 32), 2, 32);
		if (f == JavaLibraryFunction.INTEGER_TO_OCTAL_STRING)
			return digits(unsigned(x, 32), 8, 32);
		if (f == JavaLibraryFunction.LONG_TO_HEX_STRING)
			return digits(unsigned(x, 64), 16, 64);
		if (f == JavaLibraryFunction.LONG_TO_BINARY_STRING)
			return digits(unsigned(x, 64), 2, 64);
		if (f == JavaLibraryFunction.LONG_TO_OCTAL_STRING)
			return digits(unsigned(x, 64), 8, 64);
		return null;
	}

	// binary operators

	private Term binary(
			BinaryOperator op,
			Term left,
			Term right,
			BinaryExpression e) {
		if (op instanceof JavaIntegerCompareOperator || op instanceof JavaLongCompareOperator)
			return compare(coerce(left, Sort.INT, e), coerce(right, Sort.INT, e));
		if (op instanceof JavaShortCompareOperator || op instanceof JavaByteCompareOperator)
			return sub(coerce(left, Sort.INT, e), coerce(right, Sort.INT, e));
		if (op instanceof JavaDoubleCompareOperator)
			return floatCompare(toFloat(left, Sort.FLOAT64, e), toFloat(right, Sort.FLOAT64, e));
		if (op instanceof JavaFloatCompareOperator)
			return floatCompare(toFloat(left, Sort.FLOAT32, e), toFloat(right, Sort.FLOAT32, e));
		if (op instanceof JavaMathMax || op instanceof JavaStrictMathMax)
			return maxMin(left, right, true, e);
		if (op instanceof JavaMathMin || op instanceof JavaStrictMathMin)
			return maxMin(left, right, false, e);
		if (op instanceof JavaCharacterDigitOperator)
			return digit(left, right, e);
		if (op instanceof JavaLongRotateRightOperator)
			return signed(rotate(unsigned(coerce(left, Sort.INT, e), 64), coerce(right, Sort.INT, e), 64, false), 64);
		if (op instanceof JavaStringCompareToOperator)
			return compareStrings(asString(left, e), asString(right, e), e);
		if (!(op instanceof JavaLibraryFunction))
			return null;

		JavaLibraryFunction f = (JavaLibraryFunction) op;
		if (f == JavaLibraryFunction.BOOLEAN_COMPARE
				|| f == JavaLibraryFunction.BOOLEAN_LOGICAL_AND
				|| f == JavaLibraryFunction.BOOLEAN_LOGICAL_OR
				|| f == JavaLibraryFunction.BOOLEAN_LOGICAL_XOR) {
			Term a = coerce(left, Sort.BOOL, e);
			Term b = coerce(right, Sort.BOOL, e);
			if (f == JavaLibraryFunction.BOOLEAN_LOGICAL_AND)
				return Term.and(a, b);
			if (f == JavaLibraryFunction.BOOLEAN_LOGICAL_OR)
				return Term.or(a, b);
			if (f == JavaLibraryFunction.BOOLEAN_LOGICAL_XOR)
				return Term.not(Term.eq(a, b));
			// false < true
			return Term.ite(Term.eq(a, b), num(0), Term.ite(a, num(1), num(-1)));
		}
		if (f == JavaLibraryFunction.DOUBLE_COMPARE)
			return floatCompare(toFloat(left, Sort.FLOAT64, e), toFloat(right, Sort.FLOAT64, e));
		if (f == JavaLibraryFunction.FLOAT_COMPARE)
			return floatCompare(toFloat(left, Sort.FLOAT32, e), toFloat(right, Sort.FLOAT32, e));
		if (f == JavaLibraryFunction.STRING_INDEX_OF_CHAR)
			return indexOf(asString(left, e), coerce(right, Sort.INT, e), num(0), e);
		if (f == JavaLibraryFunction.STRING_LAST_INDEX_OF_CHAR)
			return lastIndexOf(asString(left, e), coerce(right, Sort.INT, e), e);
		if (f == JavaLibraryFunction.STRING_COMPARE_TO_IGNORE_CASE)
			return compareIgnoringCase(asString(left, e), asString(right, e), e);
		if (f == JavaLibraryFunction.CHARACTER_DIGIT)
			return digit(left, right, e);

		Term a = coerce(left, Sort.INT, e);
		Term b = coerce(right, Sort.INT, e);
		if (f == JavaLibraryFunction.INTEGER_MAX || f == JavaLibraryFunction.LONG_MAX)
			return Term.ite(ge(a, b), a, b);
		if (f == JavaLibraryFunction.INTEGER_MIN || f == JavaLibraryFunction.LONG_MIN)
			return Term.ite(le(a, b), a, b);
		if (f == JavaLibraryFunction.INTEGER_SUM)
			return wrap(add(a, b), 32, false);
		if (f == JavaLibraryFunction.LONG_SUM)
			return wrap(add(a, b), 64, false);
		if (f == JavaLibraryFunction.INTEGER_COMPARE || f == JavaLibraryFunction.LONG_COMPARE)
			return compare(a, b);
		if (f == JavaLibraryFunction.INTEGER_COMPARE_UNSIGNED)
			return compare(unsigned(a, 32), unsigned(b, 32));
		if (f == JavaLibraryFunction.LONG_COMPARE_UNSIGNED)
			return compare(unsigned(a, 64), unsigned(b, 64));
		if (f == JavaLibraryFunction.SHORT_COMPARE
				|| f == JavaLibraryFunction.BYTE_COMPARE
				|| f == JavaLibraryFunction.CHARACTER_COMPARE)
			// the difference of the values, that cannot overflow
			return sub(a, b);
		if (f == JavaLibraryFunction.CHARACTER_TO_CODE_POINT)
			// ((high << 10) + low) + (MIN_SUPPLEMENTARY_CODE_POINT
			// - (MIN_HIGH_SURROGATE << 10) - MIN_LOW_SURROGATE), that cannot
			// overflow for chars
			return add(add(Term.app("*", Sort.INT, a, num(1024)), b),
					num(Character.MIN_SUPPLEMENTARY_CODE_POINT - (Character.MIN_HIGH_SURROGATE << 10)
							- Character.MIN_LOW_SURROGATE));
		if (f == JavaLibraryFunction.INTEGER_ROTATE_LEFT)
			return signed(rotate(unsigned(a, 32), b, 32, true), 32);
		if (f == JavaLibraryFunction.INTEGER_ROTATE_RIGHT)
			return signed(rotate(unsigned(a, 32), b, 32, false), 32);
		if (f == JavaLibraryFunction.LONG_ROTATE_LEFT)
			return signed(rotate(unsigned(a, 64), b, 64, true), 64);
		if (f == JavaLibraryFunction.INTEGER_TO_STRING_RADIX)
			return withRadix(b, r -> signedDigits(a, r, 32));
		if (f == JavaLibraryFunction.LONG_TO_STRING_RADIX)
			return withRadix(b, r -> signedDigits(a, r, 64));
		if (f == JavaLibraryFunction.INTEGER_TO_UNSIGNED_STRING_RADIX)
			return withRadix(b, r -> digits(unsigned(a, 32), r, 32));
		if (f == JavaLibraryFunction.LONG_TO_UNSIGNED_STRING_RADIX)
			return withRadix(b, r -> digits(unsigned(a, 64), r, 64));
		return null;
	}

	// integers and bits

	private static Term num(
			long value) {
		return Term.integer(value);
	}

	private static Term num(
			BigInteger value) {
		return Term.integer(value);
	}

	private static BigInteger pow2(
			int bits) {
		return BigInteger.ONE.shiftLeft(bits);
	}

	private static Term add(
			Term a,
			Term b) {
		return Term.app("+", Sort.INT, a, b);
	}

	private static Term sub(
			Term a,
			Term b) {
		return Term.app("-", Sort.INT, a, b);
	}

	private static Term lt(
			Term a,
			Term b) {
		return Term.app("<", Sort.BOOL, a, b);
	}

	private static Term le(
			Term a,
			Term b) {
		return Term.app("<=", Sort.BOOL, a, b);
	}

	private static Term ge(
			Term a,
			Term b) {
		return Term.app(">=", Sort.BOOL, a, b);
	}

	private static Term between(
			Term x,
			long low,
			long high) {
		return Term.and(ge(x, num(low)), le(x, num(high)));
	}

	private static Term div(
			Term a,
			BigInteger b) {
		return Term.app("div", Sort.INT, a, num(b));
	}

	private static Term mod(
			Term a,
			BigInteger b) {
		return Term.app("mod", Sort.INT, a, num(b));
	}

	private static Term compare(
			Term a,
			Term b) {
		return Term.ite(lt(a, b), num(-1), Term.ite(Term.eq(a, b), num(0), num(1)));
	}

	/**
	 * The unsigned reading of a signed integer of the given bit width.
	 */
	private static Term unsigned(
			Term x,
			int bits) {
		return Term.ite(lt(x, num(0)), add(x, num(pow2(bits))), x);
	}

	/**
	 * The signed reading of an unsigned integer of the given bit width.
	 */
	private static Term signed(
			Term u,
			int bits) {
		return Term.ite(ge(u, num(pow2(bits - 1))), sub(u, num(pow2(bits))), u);
	}

	/**
	 * The group of {@code width} bits of the unsigned integer {@code u}
	 * starting at bit {@code index}.
	 */
	private static Term bits(
			Term u,
			int index,
			int width) {
		return mod(div(u, pow2(index)), pow2(width));
	}

	private static Term bitCount(
			Term u,
			int bits) {
		Term[] sum = new Term[bits];
		for (int i = 0; i < bits; i++)
			sum[i] = bits(u, i, 1);
		return Term.app("+", Sort.INT, sum);
	}

	private static Term highestOneBit(
			Term u,
			int bits) {
		Term result = num(0);
		for (int i = 0; i < bits; i++)
			result = Term.ite(ge(u, num(pow2(i))), num(pow2(i)), result);
		return result;
	}

	private static Term lowestOneBit(
			Term u,
			int bits) {
		Term result = num(0);
		for (int i = bits - 1; i >= 0; i--)
			result = Term.ite(Term.eq(bits(u, i, 1), num(1)), num(pow2(i)), result);
		return result;
	}

	private static Term leadingZeros(
			Term u,
			int bits) {
		Term result = num(bits);
		for (int i = 0; i < bits; i++)
			result = Term.ite(ge(u, num(pow2(i))), num(bits - 1 - i), result);
		return result;
	}

	private static Term trailingZeros(
			Term u,
			int bits) {
		Term result = num(bits);
		for (int i = bits - 1; i >= 0; i--)
			result = Term.ite(Term.eq(bits(u, i, 1), num(1)), num(i), result);
		return result;
	}

	/**
	 * Reverses the order of the groups of {@code group} bits of the unsigned
	 * integer {@code u} (single bits for {@code Integer.reverse}, bytes for
	 * {@code Integer.reverseBytes}).
	 */
	private static Term reverse(
			Term u,
			int bits,
			int group) {
		int groups = bits / group;
		Term[] sum = new Term[groups];
		for (int i = 0; i < groups; i++)
			sum[i] = Term.app("*", Sort.INT, bits(u, i * group, group),
					num(pow2((groups - 1 - i) * group)));
		return groups == 1 ? sum[0] : Term.app("+", Sort.INT, sum);
	}

	/**
	 * Rotates the unsigned integer {@code u} by the given distance (only its
	 * lowest bits are used, as in Java).
	 */
	private static Term rotate(
			Term u,
			Term distance,
			int bits,
			boolean left) {
		Term k = Term.app("mod", Sort.INT, left ? distance : Term.app("-", Sort.INT, distance), num(bits));
		Term result = u;
		for (int i = 1; i < bits; i++) {
			Term rotated = add(mod(Term.app("*", Sort.INT, u, num(pow2(i))), pow2(bits)), div(u, pow2(bits - i)));
			result = Term.ite(Term.eq(k, num(i)), rotated, result);
		}
		return result;
	}

	private Term maxMin(
			Term left,
			Term right,
			boolean max,
			BinaryExpression e) {
		Sort fp = floatingSort(left, right);
		if (fp == null) {
			Term a = coerce(left, Sort.INT, e);
			Term b = coerce(right, Sort.INT, e);
			return Term.ite(max ? ge(a, b) : le(a, b), a, b);
		}
		// NaN if either is NaN, and -0.0 < 0.0
		Term a = toFloat(left, fp, e);
		Term b = toFloat(right, fp, e);
		Term zeros = Term.and(isZero(a), isZero(b));
		Term negativeA = Term.app("fp.isNegative", Sort.BOOL, a);
		return Term.ite(Term.app("fp.isNaN", Sort.BOOL, a), a,
				Term.ite(Term.app("fp.isNaN", Sort.BOOL, b), b,
						Term.ite(zeros, Term.ite(negativeA, max ? b : a, max ? a : b),
								Term.ite(Term.app(max ? "fp.geq" : "fp.leq", Sort.BOOL, a, b), a, b))));
	}

	// floating point numbers

	private static Term isZero(
			Term f) {
		return Term.app("fp.isZero", Sort.BOOL, f);
	}

	/**
	 * Compares two floating point numbers as {@link Double#compare(double,
	 * double)}: NaN is greater than any other number, and {@code 0.0} is
	 * greater than {@code -0.0}.
	 */
	private static Term floatCompare(
			Term a,
			Term b) {
		Term nanA = Term.app("fp.isNaN", Sort.BOOL, a);
		Term nanB = Term.app("fp.isNaN", Sort.BOOL, b);
		Term zeros = Term.and(isZero(a), isZero(b));
		Term negA = Term.app("fp.isNegative", Sort.BOOL, a);
		Term negB = Term.app("fp.isNegative", Sort.BOOL, b);
		return Term.ite(Term.app("fp.lt", Sort.BOOL, a, b), num(-1),
				Term.ite(Term.app("fp.gt", Sort.BOOL, a, b), num(1),
						Term.ite(nanA, Term.ite(nanB, num(0), num(1)),
								Term.ite(nanB, num(-1),
										Term.ite(Term.and(zeros, Term.not(Term.eq(negA, negB))),
												Term.ite(negA, num(-1), num(1)),
												num(0))))));
	}

	/**
	 * The unbiased exponent of a floating point number, as
	 * {@link Math#getExponent(double)}: it is read from the bits of the
	 * number, given by an auxiliary bit-vector whose interpretation as a
	 * floating point number is the number.
	 */
	private Term exponent(
			Term arg,
			UnaryExpression e) {
		Sort fp = arg.getSort().isFloatingPoint() ? arg.getSort()
				: e.getExpression().getStaticType().isNumericType()
						&& e.getExpression().getStaticType().asNumericType().is32Bits() ? Sort.FLOAT32 : Sort.FLOAT64;
		Term f = toFloat(arg, fp, e);
		int eb = fp.getExponentBits();
		int sb = fp.getSignificandBits();
		int width = eb + sb;
		Term raw = auxiliary(Sort.bitVector(width), e);
		getSymbols().require(Term.eq(f, Term.app("(_ to_fp " + eb + " " + sb + ")", fp, raw)));
		Term exponentBits = Term.app("(_ extract " + (width - 2) + " " + (sb - 1) + ")", Sort.bitVector(eb), raw);
		return sub(Term.app("bv2nat", Sort.INT, exponentBits), num((1L << (eb - 1)) - 1));
	}

	/**
	 * Converts a floating point number to a string as
	 * {@link Double#toString(double)}, precisely only for special values and
	 * for integral numbers below {@code 10^7} (that are printed with a single
	 * decimal digit): the result is an unknown function of the number
	 * otherwise.
	 */
	private Term floatToString(
			Term f,
			UnaryExpression e) {
		Sort fp = f.getSort();
		Term rounded = Term.app("fp.roundToIntegral", fp, Term.value("RTZ", Sort.ROUNDING_MODE), f);
		Term limit = fp.equals(Sort.FLOAT64) ? FloatingPoints.literal(1e7d) : FloatingPoints.literal(1e7f);
		Term integral = Term.and(Term.app("fp.isNormal", Sort.BOOL, f), Term.app("fp.eq", Sort.BOOL, f, rounded),
				Term.app("fp.lt", Sort.BOOL, Term.app("fp.abs", fp, f), limit));
		Term value = Term.app("to_int", Sort.INT, Term.app("fp.to_real", Sort.REAL, f));
		Term printed = Term.app("str.++", Sort.STRING, decimal(value), Term.string(".0"));
		Term negative = Term.app("fp.isNegative", Sort.BOOL, f);
		Term special = Term.ite(Term.app("fp.isNaN", Sort.BOOL, f), Term.string("NaN"),
				Term.ite(Term.app("fp.isInfinite", Sort.BOOL, f),
						Term.ite(negative, Term.string("-Infinity"), Term.string("Infinity")),
						Term.ite(negative, Term.string("-0.0"), Term.string("0.0"))));
		Term isSpecial = Term.or(Term.app("fp.isNaN", Sort.BOOL, f), Term.app("fp.isInfinite", Sort.BOOL, f),
				isZero(f));
		// the guard must hold also when the special values are printed
		return guarded(Term.or(isSpecial, integral), Term.ite(isSpecial, special, printed), List.of(f), e);
	}

	// characters

	/**
	 * A range of code points where a function has the same value, or the
	 * same difference from the code point.
	 */
	private static final class Segment {

		private final int high;

		private final Object value;

		private final Long offset;

		private Segment(
				int high,
				Object value,
				Long offset) {
			this.high = high;
			this.value = value;
			this.offset = offset;
		}
	}

	/**
	 * Encodes the given function over code points (a function of the library
	 * over characters with a single parameter) through its table over all
	 * code points, or yields {@code null} if it is not such a function.
	 */
	private Term codePointTable(
			JavaLibraryFunction f,
			Term arg,
			UnaryExpression e) {
		Object sample;
		try {
			sample = f.apply(0);
		} catch (RuntimeException ex) {
			return null;
		}
		if (!(sample instanceof Boolean || sample instanceof Number || sample instanceof Character))
			return null;
		return table(f.toString(), f::apply, Character.MAX_CODE_POINT, coerce(arg, Sort.INT, e), e);
	}

	/**
	 * Encodes the given function over chars through its table over all chars.
	 */
	private Term charTable(
			String name,
			IntFunction<Object> f,
			Term arg,
			UnaryExpression e) {
		return table(name, f, Character.MAX_VALUE, coerce(arg, Sort.INT, e), e);
	}

	/**
	 * Encodes the given function through its table over the code points from
	 * {@code 0} to {@code max}, computed by running the function: the encoding
	 * is precise only for these code points.
	 */
	private Term table(
			String name,
			IntFunction<Object> f,
			int max,
			Term c,
			ValueExpression e) {
		List<Segment> segments = TABLES.computeIfAbsent(name + "/" + max, k -> segments(f, max));
		boolean bool = segments.get(0).value instanceof Boolean;
		return guarded(between(c, 0, max), tree(segments, 0, segments.size() - 1, c, bool), List.of(c), e);
	}

	/**
	 * A balanced decision tree over the given segments.
	 */
	private static Term tree(
			List<Segment> segments,
			int low,
			int high,
			Term c,
			boolean bool) {
		if (low == high) {
			Segment s = segments.get(low);
			return bool ? Term.bool((Boolean) s.value)
					: s.offset != null ? add(c, num(s.offset)) : num(integral(s.value));
		}
		int mid = (low + high) / 2;
		return Term.ite(le(c, num(segments.get(mid).high)), tree(segments, low, mid, c, bool),
				tree(segments, mid + 1, high, c, bool));
	}

	private static List<Segment> segments(
			IntFunction<Object> f,
			int max) {
		Object[] values = new Object[max + 1];
		for (int c = 0; c <= max; c++)
			values[c] = f.apply(c);
		List<Segment> segments = new ArrayList<>();
		int c = 0;
		while (c <= max) {
			Object v = values[c];
			int end = c;
			while (end + 1 <= max && values[end + 1].equals(v))
				end++;
			Long offset = null;
			if (!(v instanceof Boolean)) {
				// runs where the value follows the code point (e.g., case
				// conversions) are merged as offsets
				long delta = integral(v) - c;
				int run = c;
				while (run + 1 <= max && integral(values[run + 1]) - (run + 1) == delta)
					run++;
				if (run > end) {
					end = run;
					offset = delta;
				}
			}
			segments.add(new Segment(end, v, offset));
			c = end + 1;
		}
		return segments;
	}

	private static long integral(
			Object value) {
		if (value instanceof Character)
			return (Character) value;
		return ((Number) value).longValue();
	}

	/**
	 * The value of a digit, as {@link Character#digit(int, int)}: the value of
	 * the code point as a digit in radix 36, if it is below the radix and the
	 * radix is valid, and {@code -1} otherwise.
	 */
	private Term digit(
			Term cp,
			Term radix,
			BinaryExpression e) {
		Term c = coerce(cp, Sort.INT, e);
		Term r = coerce(radix, Sort.INT, e);
		Term value = table("digit36", x -> Character.digit(x, Character.MAX_RADIX), Character.MAX_CODE_POINT, c, e);
		return Term.ite(Term.and(between(r, Character.MIN_RADIX, Character.MAX_RADIX), ge(value, num(0)),
				lt(value, r)), value, num(-1));
	}

	// strings

	private static Term len(
			Term s) {
		return Term.app("str.len", Sort.INT, s);
	}

	private static Term substr(
			Term s,
			Term from,
			Term length) {
		return Term.app("str.substr", Sort.STRING, s, from, length);
	}

	private static Term at(
			Term s,
			Term i) {
		return Term.app("str.at", Sort.STRING, s, i);
	}

	private static Term code(
			Term s) {
		return Term.app("str.to_code", Sort.INT, s);
	}

	/**
	 * The decimal representation of a (signed) integer.
	 */
	private static Term decimal(
			Term x) {
		return Term.ite(lt(x, num(0)),
				Term.app("str.++", Sort.STRING, Term.string("-"),
						Term.app("str.from_int", Sort.STRING, Term.app("-", Sort.INT, x))),
				Term.app("str.from_int", Sort.STRING, x));
	}

	/**
	 * The representation of a non-negative integer of at most the given number
	 * of bits in the given radix, with lowercase letters for the digits above
	 * {@code 9}, as by {@link Long#toString(long, int)}.
	 */
	private static Term digits(
			Term u,
			int radix,
			int bits) {
		BigInteger r = BigInteger.valueOf(radix);
		BigInteger limit = pow2(bits);
		// the number of digits needed for the largest value
		int max = 1;
		while (r.pow(max).compareTo(limit) < 0)
			max++;
		// all the digits, from the most significant one
		Term[] all = new Term[max];
		for (int i = 0; i < max; i++) {
			Term d = mod(div(u, r.pow(max - 1 - i)), r);
			all[i] = Term.app("str.from_code", Sort.STRING,
					Term.ite(lt(d, num(10)), add(d, num('0')), add(d, num('a' - 10))));
		}
		Term full = max == 1 ? all[0] : Term.app("str.++", Sort.STRING, all);
		// the number of significant digits
		Term count = num(max);
		for (int k = max - 1; k >= 1; k--)
			count = Term.ite(lt(u, num(r.pow(k))), num(k), count);
		return substr(full, Term.app("-", Sort.INT, num(max), count), count);
	}

	/**
	 * The representation of a signed integer of the given bit width in the
	 * given radix, as by {@link Long#toString(long, int)}.
	 */
	private static Term signedDigits(
			Term x,
			int radix,
			int bits) {
		if (radix == 10)
			return decimal(x);
		Term magnitude = Term.ite(lt(x, num(0)), Term.app("-", Sort.INT, x), x);
		Term digits = digits(magnitude, radix, bits);
		return Term.ite(lt(x, num(0)), Term.app("str.++", Sort.STRING, Term.string("-"), digits), digits);
	}

	/**
	 * Selects the representation for the given radix, using radix
	 * {@code 10} for radixes that are not valid, as Java does.
	 */
	private static Term withRadix(
			Term radix,
			IntFunction<Term> representation) {
		Term result = representation.apply(10);
		for (int r = Character.MAX_RADIX; r >= Character.MIN_RADIX; r--)
			if (r != 10)
				result = Term.ite(Term.eq(radix, num(r)), representation.apply(r), result);
		return result;
	}

	/**
	 * The first index of the given char in the given string, from the given
	 * (non-negative) index, as {@link String#indexOf(int, int)}: precise only
	 * for chars (code points of the basic multilingual plane).
	 */
	private Term indexOf(
			Term s,
			Term ch,
			Term from,
			ValueExpression e) {
		Term found = Term.app("str.indexof", Sort.INT, s, Term.app("str.from_code", Sort.STRING, ch), from);
		return guarded(between(ch, 0, Character.MAX_VALUE), found, List.of(s, ch, from), e);
	}

	/**
	 * The last index of the given char in the given string, as
	 * {@link String#lastIndexOf(int)}: an auxiliary index such that the char
	 * is there, and not in the rest of the string (or {@code -1} if the char
	 * is not in the string). Precise only for chars.
	 */
	private Term lastIndexOf(
			Term s,
			Term ch,
			ValueExpression e) {
		Term c = Term.app("str.from_code", Sort.STRING, ch);
		Term i = auxiliary(Sort.INT, e);
		Term len = len(s);
		Term rest = substr(s, add(i, num(1)), sub(sub(len, i), num(1)));
		getSymbols().require(Term.ite(Term.app("str.contains", Sort.BOOL, s, c),
				Term.and(ge(i, num(0)), lt(i, len), Term.eq(at(s, i), c),
						Term.not(Term.app("str.contains", Sort.BOOL, rest, c))),
				Term.eq(i, num(-1))));
		return guarded(between(ch, 0, Character.MAX_VALUE), i, List.of(s, ch), e);
	}

	/**
	 * Compares two strings lexicographically, as
	 * {@link String#compareTo(String)}: through an auxiliary index, the
	 * length of their longest common prefix.
	 */
	private Term compareStrings(
			Term a,
			Term b,
			ValueExpression e) {
		Term la = len(a);
		Term lb = len(b);
		Term min = Term.ite(le(la, lb), la, lb);
		Term k = auxiliary(Sort.INT, e);
		getSymbols().require(Term.and(ge(k, num(0)), le(k, min),
				Term.eq(substr(a, num(0), k), substr(b, num(0), k)),
				Term.or(Term.eq(k, min), Term.not(Term.eq(at(a, k), at(b, k))))));
		return Term.ite(lt(k, min), sub(code(at(a, k)), code(at(b, k))), sub(la, lb));
	}

	/**
	 * Compares two strings ignoring case, as
	 * {@link String#compareToIgnoreCase(String)}: the strings are compared
	 * after converting their uppercase letters to lowercase, precisely only for
	 * ASCII strings.
	 */
	private Term compareIgnoringCase(
			Term a,
			Term b,
			ValueExpression e) {
		Term ascii = Term.value("(re.* (re.range \"\\u{0}\" \"\\u{7f}\"))", Sort.STRING);
		Term guard = Term.and(Term.app("str.in_re", Sort.BOOL, a, ascii), Term.app("str.in_re", Sort.BOOL, b, ascii));
		return guarded(guard, compareStrings(lower(a), lower(b), e), List.of(a, b), e);
	}

	/**
	 * Converts the ASCII uppercase letters of the given string to lowercase.
	 */
	private static Term lower(
			Term s) {
		Term result = s;
		for (char c = 'A'; c <= 'Z'; c++)
			result = Term.app("str.replace_all", Sort.STRING, result, Term.string(String.valueOf(c)),
					Term.string(String.valueOf(Character.toLowerCase(c))));
		return result;
	}
}

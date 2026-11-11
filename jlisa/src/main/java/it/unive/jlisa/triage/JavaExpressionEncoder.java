package it.unive.jlisa.triage;

import it.unive.jlisa.program.operator.JavaByteCompareOperator;
import it.unive.jlisa.program.operator.JavaByteToUnsignedIntOperator;
import it.unive.jlisa.program.operator.JavaByteToUnsignedLongOperator;
import it.unive.jlisa.program.operator.JavaCharacterDigitOperator;
import it.unive.jlisa.program.operator.JavaCharacterEqualsOperator;
import it.unive.jlisa.program.operator.JavaCharacterForDigitOperator;
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
import it.unive.jlisa.program.operator.JavaDoubleIsFiniteOperator;
import it.unive.jlisa.program.operator.JavaDoubleIsInfiniteOperator;
import it.unive.jlisa.program.operator.JavaDoubleIsNaNOperator;
import it.unive.jlisa.program.operator.JavaDoubleLongBitsToDoubleOperator;
import it.unive.jlisa.program.operator.JavaDoubleToLongBitsOperator;
import it.unive.jlisa.program.operator.JavaDoubleToRawLongBitsOperator;
import it.unive.jlisa.program.operator.JavaFloatCompareOperator;
import it.unive.jlisa.program.operator.JavaFloatIsFiniteOperator;
import it.unive.jlisa.program.operator.JavaFloatIsInfiniteOperator;
import it.unive.jlisa.program.operator.JavaFloatIsNaNOperator;
import it.unive.jlisa.program.operator.JavaFloatToIntBitsOperator;
import it.unive.jlisa.program.operator.JavaFloatToRawIntBitsOperator;
import it.unive.jlisa.program.operator.JavaIntegerCompareOperator;
import it.unive.jlisa.program.operator.JavaLibraryFunction;
import it.unive.jlisa.program.operator.JavaLongBitCountOperator;
import it.unive.jlisa.program.operator.JavaLongCompareOperator;
import it.unive.jlisa.program.operator.JavaLongRotateRightOperator;
import it.unive.jlisa.program.operator.JavaMathAbsOperator;
import it.unive.jlisa.program.operator.JavaMathFloorOperator;
import it.unive.jlisa.program.operator.JavaMathMax;
import it.unive.jlisa.program.operator.JavaMathMin;
import it.unive.jlisa.program.operator.JavaMathRoundOperator;
import it.unive.jlisa.program.operator.JavaMathSqrtOperator;
import it.unive.jlisa.program.operator.JavaShortCompareOperator;
import it.unive.jlisa.program.operator.JavaStrictMathAbsOperator;
import it.unive.jlisa.program.operator.JavaStrictMathFloorOperator;
import it.unive.jlisa.program.operator.JavaStrictMathGetExponentOperator;
import it.unive.jlisa.program.operator.JavaStrictMathMax;
import it.unive.jlisa.program.operator.JavaStrictMathMin;
import it.unive.jlisa.program.operator.JavaStrictMathRoundOperator;
import it.unive.jlisa.program.operator.JavaStrictMathSqrtOperator;
import it.unive.jlisa.program.operator.JavaStringAppendBooleanOperator;
import it.unive.jlisa.program.operator.JavaStringAppendCharOperator;
import it.unive.jlisa.program.operator.JavaStringAppendDoubleOperator;
import it.unive.jlisa.program.operator.JavaStringAppendFloatOperator;
import it.unive.jlisa.program.operator.JavaStringAppendIntOperator;
import it.unive.jlisa.program.operator.JavaStringAppendLongOperator;
import it.unive.jlisa.program.operator.JavaStringAppendStringOperator;
import it.unive.jlisa.program.operator.JavaStringCharAtOperator;
import it.unive.jlisa.program.operator.JavaStringCompareToOperator;
import it.unive.jlisa.program.operator.JavaStringDeleteCharAtOperator;
import it.unive.jlisa.program.operator.JavaStringDeleteOperator;
import it.unive.jlisa.program.operator.JavaStringEndsWithOperator;
import it.unive.jlisa.program.operator.JavaStringEqualsIgnoreCaseOperator;
import it.unive.jlisa.program.operator.JavaStringIndexOfCharOperator;
import it.unive.jlisa.program.operator.JavaStringIndexOfOperator;
import it.unive.jlisa.program.operator.JavaStringIndexOfStringFromIndexOperator;
import it.unive.jlisa.program.operator.JavaStringInsertBooleanOperator;
import it.unive.jlisa.program.operator.JavaStringInsertCharOperator;
import it.unive.jlisa.program.operator.JavaStringInsertDoubleOperator;
import it.unive.jlisa.program.operator.JavaStringInsertFloatOperator;
import it.unive.jlisa.program.operator.JavaStringInsertIntOperator;
import it.unive.jlisa.program.operator.JavaStringInsertLongOperator;
import it.unive.jlisa.program.operator.JavaStringInsertStringOperator;
import it.unive.jlisa.program.operator.JavaStringLastIndexOfOperator;
import it.unive.jlisa.program.operator.JavaStringLastIndexOfStringFromIndexOperator;
import it.unive.jlisa.program.operator.JavaStringLastIndexOfStringOperator;
import it.unive.jlisa.program.operator.JavaStringReplaceOperator;
import it.unive.jlisa.program.operator.JavaStringSetCharAtOperator;
import it.unive.jlisa.program.operator.JavaStringStartsWithFromIndexOperator;
import it.unive.jlisa.program.operator.JavaStringStartsWithOperator;
import it.unive.jlisa.program.operator.JavaStringSubstringFromToOperator;
import it.unive.jlisa.program.operator.JavaStringSubstringOperator;
import it.unive.jlisa.program.operator.JavaStringToLowerCaseOperator;
import it.unive.jlisa.program.operator.JavaStringToUpperCaseOperator;
import it.unive.jlisa.program.operator.JavaStringTrimOperator;
import it.unive.jlisa.program.operator.JavaStringValueOfBooleanOperator;
import it.unive.jlisa.program.operator.JavaStringValueOfCharOperator;
import it.unive.jlisa.program.operator.JavaStringValueOfDoubleOperator;
import it.unive.jlisa.program.operator.JavaStringValueOfFloatOperator;
import it.unive.jlisa.program.operator.JavaStringValueOfIntOperator;
import it.unive.jlisa.program.operator.JavaStringValueOfLongOperator;
import it.unive.jlisa.program.type.JavaByteType;
import it.unive.jlisa.program.type.JavaCharType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.program.type.JavaLongType;
import it.unive.jlisa.program.type.JavaShortType;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.ternary.TernaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.triage.encoding.ExpressionEncoder;
import it.unive.lisa.triage.encoding.SortPolicy;
import it.unive.lisa.triage.encoding.SymbolTable;
import it.unive.lisa.triage.encoding.ValueSort;
import it.unive.lisa.triage.smt.FloatingPoints;
import it.unive.lisa.triage.smt.Sort;
import it.unive.lisa.triage.smt.Term;
import it.unive.lisa.type.Type;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntFunction;


/**
 * An {@link ExpressionEncoder} that also encodes the operators of the Java
 * frontend, so that counterexamples relying on them can be confirmed. Functions
 * of the Java library ({@link JavaLibraryFunction}, and the operators of the
 * frontend modeling methods of the library) are encoded:
 * <ul>
 * <li>exactly over integers, booleans, strings and floating point numbers, when
 * they can be expressed with the operations of the SMT theories (e.g.,
 * comparisons, bit manipulations through the bits of the integers, conversions
 * of numbers to strings through their digits, searches in strings);</li>
 * <li>through tables over all code points, for the functions over characters
 * (e.g., {@link Character#getType(int)}): the tables are computed by running
 * the functions of the Java library on each code point, so that they agree with
 * the library by construction;</li>
 * <li>exactly only for some of their arguments, when no exact encoding is known
 * (e.g., {@link Double#toString(double)} is encoded only for integral numbers
 * and special values), the result being an unknown function of the arguments
 * otherwise.</li>
 * </ul>
 * Other functions yield unconstrained values, as any other operator unknown to
 * this encoder.
 */
public class JavaExpressionEncoder extends ExpressionEncoder {

	/**
	 * The tables built so far, shared by all encoders since they only depend on
	 * the functions.
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
		if (expression.getOperator() instanceof JavaStringLastIndexOfStringFromIndexOperator) {
			Term s = asString(left, expression);
			Term t = asString(middle, expression);
			Term from = coerce(right, Sort.INT, expression);
			// the occurrences starting at most at the given index
			Term end = add(from, len(t));
			Term prefix = substr(s, num(0), Term.ite(Term.app(">", Sort.BOOL, end, len(s)), len(s), end));
			return Term.ite(lt(from, num(0)), num(-1), lastIndexOfString(prefix, t, expression));
		}
		Term string = stringTernary(expression.getOperator(), left, middle, right, expression);
		if (string != null)
			return string;
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
			return bitCount(unsigned(coerce(arg, Sort.INT, e), 64), 64, e);
		if (op instanceof JavaByteToUnsignedIntOperator || op instanceof JavaByteToUnsignedLongOperator)
			return unsigned(coerce(arg, Sort.INT, e), 8);
		Term floating = floatUnary(op, arg, e);
		if (floating != null)
			return floating;
		Term string = stringUnary(op, arg, e);
		if (string != null)
			return string;
		Term math = mathUnary(op, typed(arg, e), e);
		if (math != null)
			return math;
		if (!(op instanceof JavaLibraryFunction))
			return null;

		JavaLibraryFunction f = (JavaLibraryFunction) op;
		Term exact = exactCodePointFunction(f, arg, e);
		if (exact != null)
			return exact;
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
			return bitCount(unsigned(x, 32), 32, e);
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
			return signed(reverse(unsigned(x, 32), 32, 1, e), 32);
		if (f == JavaLibraryFunction.LONG_REVERSE)
			return signed(reverse(unsigned(x, 64), 64, 1, e), 64);
		if (f == JavaLibraryFunction.INTEGER_REVERSE_BYTES)
			return signed(reverse(unsigned(x, 32), 32, 8, e), 32);
		if (f == JavaLibraryFunction.LONG_REVERSE_BYTES)
			return signed(reverse(unsigned(x, 64), 64, 8, e), 64);
		if (f == JavaLibraryFunction.SHORT_REVERSE_BYTES)
			return signed(reverse(unsigned(x, 16), 16, 8, e), 16);
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

	/**
	 * Encodes the functions over code points that can be expressed exactly for
	 * all integers (including the ones that are not valid code points), or
	 * yields {@code null} for the other functions.
	 */
	private Term exactCodePointFunction(
			JavaLibraryFunction f,
			Term arg,
			UnaryExpression e) {
		if (f == JavaLibraryFunction.CHARACTER_CHAR_COUNT)
			return Term.ite(ge(coerce(arg, Sort.INT, e), num(Character.MIN_SUPPLEMENTARY_CODE_POINT)), num(2),
					num(1));
		if (f == JavaLibraryFunction.CHARACTER_IS_BMP_CODE_POINT)
			return between(coerce(arg, Sort.INT, e), 0, Character.MAX_VALUE);
		if (f == JavaLibraryFunction.CHARACTER_IS_VALID_CODE_POINT)
			return between(coerce(arg, Sort.INT, e), 0, Character.MAX_CODE_POINT);
		if (f == JavaLibraryFunction.CHARACTER_IS_SUPPLEMENTARY_CODE_POINT)
			return between(coerce(arg, Sort.INT, e), Character.MIN_SUPPLEMENTARY_CODE_POINT,
					Character.MAX_CODE_POINT);
		if (f == JavaLibraryFunction.CHARACTER_IS_ISO_CONTROL) {
			Term x = coerce(arg, Sort.INT, e);
			return Term.or(between(x, 0, 0x1F), between(x, 0x7F, 0x9F));
		}
		return null;
	}

	// binary operators

	private Term binary(
			BinaryOperator op,
			Term left,
			Term right,
			BinaryExpression e) {
		if (op instanceof JavaCharacterForDigitOperator)
			return forDigit(coerce(left, Sort.INT, e), coerce(right, Sort.INT, e));
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
		Term string = stringBinary(op, left, right, e);
		if (string != null)
			return string;
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

	/**
	 * Yields a bit-vector of the given width whose (unsigned) value is the
	 * given unsigned integer: an auxiliary constant constrained to it, so that
	 * the bits of the integer are extracted from the bit-vector rather than
	 * computed through quotients and remainders, that solvers handle poorly.
	 */
	private Term bitVector(
			Term u,
			int bits,
			SymbolicExpression e) {
		Term b = auxiliary(Sort.bitVector(bits), e);
		getSymbols().require(Term.eq(u, Term.app("bv2nat", Sort.INT, b)));
		return b;
	}

	/**
	 * The bits from {@code index} to {@code index + width - 1} of the given
	 * bit-vector.
	 */
	private static Term extract(
			Term b,
			int index,
			int width) {
		return Term.app("(_ extract " + (index + width - 1) + " " + index + ")", Sort.bitVector(width), b);
	}

	private Term bitCount(
			Term u,
			int bits,
			SymbolicExpression e) {
		Term b = bitVector(u, bits, e);
		Term[] sum = new Term[bits];
		for (int i = 0; i < bits; i++)
			sum[i] = Term.app("bv2nat", Sort.INT, extract(b, i, 1));
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
	private Term reverse(
			Term u,
			int bits,
			int group,
			SymbolicExpression e) {
		int groups = bits / group;
		if (groups == 1)
			return u;
		Term b = bitVector(u, bits, e);
		// the lowest group becomes the most significant one
		Term[] parts = new Term[groups];
		for (int i = 0; i < groups; i++)
			parts[i] = extract(b, i * group, group);
		return Term.app("bv2nat", Sort.INT, Term.app("concat", Sort.bitVector(bits), parts));
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
	 * Compares two floating point numbers as
	 * {@link Double#compare(double, double)}: NaN is greater than any other
	 * number, and {@code 0.0} is greater than {@code -0.0}.
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
	 * {@link Math#getExponent(double)}: it is read from the bits of the number,
	 * given by an auxiliary bit-vector whose interpretation as a floating point
	 * number is the number.
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
			ValueExpression e) {
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
	 * A range of code points where a function has the same value, or the same
	 * difference from the code point.
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
	 * over characters with a single parameter) through its table over all code
	 * points, or yields {@code null} if it is not such a function.
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
		// functions over chars are only applied to chars
		int max = CHAR_FUNCTIONS.contains(f) ? Character.MAX_VALUE : Character.MAX_CODE_POINT;
		return table(f.toString(), f::apply, max, coerce(arg, Sort.INT, e), e);
	}

	/**
	 * The functions of the library over characters whose parameter is a char.
	 */
	private static final Set<JavaLibraryFunction> CHAR_FUNCTIONS = Set.of(
			JavaLibraryFunction.CHARACTER_TO_TITLE_CASE_CHAR,
			JavaLibraryFunction.CHARACTER_IS_JAVA_LETTER,
			JavaLibraryFunction.CHARACTER_IS_JAVA_LETTER_OR_DIGIT,
			JavaLibraryFunction.CHARACTER_IS_SPACE);

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
		// the code point is used by every node of the tree: an auxiliary
		// constant equal to it keeps the encoding small
		Term x = auxiliary(Sort.INT, e);
		getSymbols().require(Term.eq(x, c));
		return guarded(between(x, 0, max), tree(segments, 0, segments.size() - 1, x, bool), List.of(x), e);
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

	/**
	 * Yields the given operand, converted to the sort of its static type if it
	 * is a value of unknown kind and its static type is numeric (e.g., the
	 * value of a boxed number read from the heap): the conversion is precise
	 * only if the value has that kind.
	 */
	private Term typed(
			Term arg,
			UnaryExpression e) {
		Type type = e.getExpression().getStaticType();
		if (!arg.getSort().equals(ValueSort.SORT) || !type.isNumericType())
			return arg;
		Sort sort = getSorts().sortOf(type);
		return sort.equals(Sort.INT) || sort.isFloatingPoint() ? coerce(arg, sort, e) : arg;
	}

	/**
	 * Encodes {@link Character#forDigit(int, int)}: the character
	 * representing the given digit in the given radix, or the null character
	 * if the digit or the radix are not valid.
	 */
	private static Term forDigit(
			Term digit,
			Term radix) {
		Term valid = Term.and(between(radix, Character.MIN_RADIX, Character.MAX_RADIX), ge(digit, num(0)),
				lt(digit, radix));
		return Term.ite(valid, Term.ite(lt(digit, num(10)), add(digit, num('0')), add(digit, num('a' - 10))),
				num(0));
	}

	// floating point numbers

	/**
	 * Encodes the predicates over floating point numbers of {@link Double} and
	 * {@link Float}, and the conversions between them and their bit
	 * representations, or yields {@code null} for the other operators (or if
	 * floating point numbers are not encoded).
	 */
	private Term floatUnary(
			UnaryOperator op,
			Term arg,
			UnaryExpression e) {
		if (op instanceof JavaDoubleLongBitsToDoubleOperator) {
			if (!getSorts().encodesFloats())
				return null;
			Term bits = Term.app("(_ int2bv 64)", Sort.bitVector(64), coerce(arg, Sort.INT, e));
			return Term.app("(_ to_fp 11 53)", Sort.FLOAT64, bits);
		}
		Sort fp;
		if (op instanceof JavaDoubleIsNaNOperator || op instanceof JavaDoubleIsInfiniteOperator
				|| op instanceof JavaDoubleIsFiniteOperator || op instanceof JavaDoubleToLongBitsOperator
				|| op instanceof JavaDoubleToRawLongBitsOperator)
			fp = Sort.FLOAT64;
		else if (op instanceof JavaFloatIsNaNOperator || op instanceof JavaFloatIsInfiniteOperator
				|| op instanceof JavaFloatIsFiniteOperator || op instanceof JavaFloatToIntBitsOperator
				|| op instanceof JavaFloatToRawIntBitsOperator)
			fp = Sort.FLOAT32;
		else
			return null;
		if (!getSorts().encodesFloats())
			return null;
		Term x = toFloat(arg, fp, e.getExpression(), e);
		Term nan = Term.app("fp.isNaN", Sort.BOOL, x);
		Term infinite = Term.app("fp.isInfinite", Sort.BOOL, x);
		if (op instanceof JavaDoubleIsNaNOperator || op instanceof JavaFloatIsNaNOperator)
			return nan;
		if (op instanceof JavaDoubleIsInfiniteOperator || op instanceof JavaFloatIsInfiniteOperator)
			return infinite;
		if (op instanceof JavaDoubleIsFiniteOperator || op instanceof JavaFloatIsFiniteOperator)
			return Term.not(Term.or(nan, infinite));
		// the bit representation, where NaNs are represented by the canonical
		// NaN (the raw representation of a NaN is not specified)
		int width = fp.equals(Sort.FLOAT64) ? 64 : 32;
		if (op instanceof JavaDoubleToRawLongBitsOperator || op instanceof JavaFloatToRawIntBitsOperator)
			getSymbols().exactOnlyIf(Term.not(nan), e);
		Term bits = Term.app("bv2int", Sort.INT, Term.app("fp.to_ieee_bv", Sort.bitVector(width), x));
		long canonical = width == 64 ? Double.doubleToLongBits(Double.NaN) : Float.floatToIntBits(Float.NaN);
		return Term.ite(nan, num(canonical), signed(bits, width));
	}

	// math

	/**
	 * Encodes the functions of {@link Math} and {@link StrictMath} that are
	 * specified exactly (absolute value, floor, square root, rounding), or
	 * yields {@code null} for the other operators, or if the kind of the
	 * operand is not known (the functions are overloaded).
	 */
	private Term mathUnary(
			UnaryOperator op,
			Term arg,
			UnaryExpression e) {
		Sort sort = arg.getSort();
		if (op instanceof JavaMathAbsOperator || op instanceof JavaStrictMathAbsOperator) {
			if (sort.isFloatingPoint())
				return Term.app("fp.abs", sort, arg);
			Type type = e.getExpression().getStaticType();
			int bits = type instanceof JavaLongType ? 64
					: type instanceof JavaIntType || type instanceof JavaShortType || type instanceof JavaByteType
							|| type instanceof JavaCharType ? 32 : 0;
			if (bits == 0 || !sort.equals(Sort.INT))
				return null;
			// the absolute value of the minimum integer is the minimum integer
			return wrap(Term.ite(lt(arg, num(0)), Term.app("-", Sort.INT, arg), arg), bits, false);
		}
		if (!sort.isFloatingPoint())
			return null;
		if (op instanceof JavaMathFloorOperator || op instanceof JavaStrictMathFloorOperator)
			return Term.app("fp.roundToIntegral", sort, Term.value("RTN", Sort.ROUNDING_MODE), arg);
		if (op instanceof JavaMathSqrtOperator || op instanceof JavaStrictMathSqrtOperator)
			// correctly rounded, as specified
			return Term.app("fp.sqrt", sort, Term.value("RNE", Sort.ROUNDING_MODE), arg);
		if (op instanceof JavaMathRoundOperator || op instanceof JavaStrictMathRoundOperator)
			return round(arg, sort.equals(Sort.FLOAT64) ? 64 : 32);
		return null;
	}

	/**
	 * Rounds the given floating point number to the closest integer of the
	 * given bit width, with ties rounding up, as {@link Math#round(double)}:
	 * {@code 0} for NaN, and the minimum or maximum integer for the numbers
	 * (and infinities) out of the range of the integers. The floor of the
	 * number is incremented if the fractional part, which is computed exactly,
	 * is at least one half (numbers so large that they have no fractional part
	 * are their own floor).
	 */
	private Term round(
			Term f,
			int bits) {
		Sort sort = f.getSort();
		Term rne = Term.value("RNE", Sort.ROUNDING_MODE);
		Term floor = Term.app("fp.roundToIntegral", sort, Term.value("RTN", Sort.ROUNDING_MODE), f);
		Term half = sort.equals(Sort.FLOAT64) ? FloatingPoints.literal(0.5) : FloatingPoints.literal(0.5f);
		Term one = sort.equals(Sort.FLOAT64) ? FloatingPoints.literal(1.0) : FloatingPoints.literal(1.0f);
		Term fraction = Term.app("fp.sub", sort, rne, f, floor);
		Term rounded = Term.ite(Term.app("fp.geq", Sort.BOOL, fraction, half), Term.app("fp.add", sort, rne, floor, one),
				floor);
		return toIntegral(rounded, bits == 64 ? JavaLongType.INSTANCE : JavaIntType.INSTANCE);
	}

	// strings

	/**
	 * Encodes the operators over strings with one operand of the Java frontend,
	 * or yields {@code null} for the other operators.
	 */
	private Term stringUnary(
			UnaryOperator op,
			Term arg,
			UnaryExpression e) {
		if (op instanceof JavaStringToLowerCaseOperator) {
			Term s = asString(arg, e);
			return guarded(ascii(s), lower(s), List.of(s), e);
		}
		if (op instanceof JavaStringToUpperCaseOperator) {
			Term s = asString(arg, e);
			return guarded(ascii(s), upper(s), List.of(s), e);
		}
		if (op instanceof JavaStringTrimOperator)
			return trim(asString(arg, e), e);
		if (op instanceof JavaStringValueOfIntOperator || op instanceof JavaStringValueOfLongOperator)
			return decimal(coerce(arg, Sort.INT, e));
		if (op instanceof JavaStringValueOfBooleanOperator)
			return Term.ite(coerce(arg, Sort.BOOL, e), Term.string("true"), Term.string("false"));
		if (op instanceof JavaStringValueOfCharOperator)
			return fromChar(coerce(arg, Sort.INT, e));
		if (op instanceof JavaStringValueOfDoubleOperator)
			return floatToString(toFloat(arg, Sort.FLOAT64, e), e);
		if (op instanceof JavaStringValueOfFloatOperator)
			return floatToString(toFloat(arg, Sort.FLOAT32, e), e);
		return null;
	}

	/**
	 * Encodes the operators over strings with two operands of the Java frontend
	 * (including the ones appending to the contents of string builders), or
	 * yields {@code null} for the other operators.
	 */
	private Term stringBinary(
			BinaryOperator op,
			Term left,
			Term right,
			BinaryExpression e) {
		if (op instanceof JavaCharacterEqualsOperator)
			return Term.eq(coerce(left, Sort.INT, e), coerce(right, Sort.INT, e));
		Term appended = representation(op, right, e);
		if (appended != null)
			return concat(asString(left, e), appended);
		if (op instanceof JavaStringCharAtOperator) {
			Term c = code(at(asString(left, e), coerce(right, Sort.INT, e)));
			// chars beyond the basic multilingual plane are two chars in Java
			return guarded(le(c, num(Character.MAX_VALUE)), c, List.of(asString(left, e), coerce(right, Sort.INT, e)),
					e);
		}
		if (op instanceof JavaStringStartsWithOperator)
			return Term.app("str.prefixof", Sort.BOOL, asString(right, e), asString(left, e));
		if (op instanceof JavaStringEndsWithOperator)
			return Term.app("str.suffixof", Sort.BOOL, asString(right, e), asString(left, e));
		if (op instanceof JavaStringIndexOfOperator)
			return Term.app("str.indexof", Sort.INT, asString(left, e), asString(right, e), num(0));
		if (op instanceof JavaStringIndexOfCharOperator)
			return indexOf(asString(left, e), coerce(right, Sort.INT, e), num(0), e);
		if (op instanceof JavaStringLastIndexOfOperator)
			return lastIndexOf(asString(left, e), coerce(right, Sort.INT, e), e);
		if (op instanceof JavaStringLastIndexOfStringOperator)
			return lastIndexOfString(asString(left, e), asString(right, e), e);
		if (op instanceof JavaStringSubstringOperator) {
			Term s = asString(left, e);
			Term from = coerce(right, Sort.INT, e);
			return substr(s, from, sub(len(s), from));
		}
		if (op instanceof JavaStringDeleteCharAtOperator) {
			Term s = asString(left, e);
			Term i = coerce(right, Sort.INT, e);
			return concat(substr(s, num(0), i), substr(s, add(i, num(1)), sub(sub(len(s), i), num(1))));
		}
		if (op instanceof JavaStringEqualsIgnoreCaseOperator)
			return equalsIgnoreCase(asString(left, e), asString(right, e), e);
		return null;
	}

	/**
	 * Encodes the operators over strings with three operands of the Java
	 * frontend (including the ones inserting in, deleting from and changing the
	 * contents of string builders), or yields {@code null} for the other
	 * operators.
	 */
	private Term stringTernary(
			TernaryOperator op,
			Term left,
			Term middle,
			Term right,
			TernaryExpression e) {
		Term inserted = representation(op, right, e);
		if (inserted != null) {
			Term s = asString(left, e);
			Term offset = coerce(middle, Sort.INT, e);
			return concat(concat(substr(s, num(0), offset), inserted), substr(s, offset, sub(len(s), offset)));
		}
		if (op instanceof JavaStringDeleteOperator) {
			// the end is at most the length
			Term s = asString(left, e);
			Term start = coerce(middle, Sort.INT, e);
			Term end = coerce(right, Sort.INT, e);
			Term last = Term.ite(le(end, len(s)), end, len(s));
			return concat(substr(s, num(0), start), substr(s, last, sub(len(s), last)));
		}
		if (op instanceof JavaStringSetCharAtOperator) {
			Term s = asString(left, e);
			Term i = coerce(middle, Sort.INT, e);
			return concat(concat(substr(s, num(0), i), fromChar(coerce(right, Sort.INT, e))),
					substr(s, add(i, num(1)), sub(sub(len(s), i), num(1))));
		}
		if (op instanceof JavaStringSubstringFromToOperator) {
			Term from = coerce(middle, Sort.INT, e);
			return substr(asString(left, e), from, sub(coerce(right, Sort.INT, e), from));
		}
		if (op instanceof JavaStringIndexOfStringFromIndexOperator) {
			Term s = asString(left, e);
			Term t = asString(middle, e);
			Term from = coerce(right, Sort.INT, e);
			// an empty string is found at any index up to the length
			return Term.ite(Term.and(Term.eq(t, Term.string("")), Term.app(">", Sort.BOOL, from, len(s))), len(s),
					Term.app("str.indexof", Sort.INT, s, t, Term.ite(lt(from, num(0)), num(0), from)));
		}
		if (op instanceof JavaStringStartsWithFromIndexOperator) {
			Term s = asString(left, e);
			Term p = asString(middle, e);
			Term from = coerce(right, Sort.INT, e);
			return Term.and(ge(from, num(0)), le(add(from, len(p)), len(s)),
					Term.app("str.prefixof", Sort.BOOL, p, substr(s, from, len(p))));
		}
		if (op instanceof JavaStringReplaceOperator && e.getMiddle().getStaticType() instanceof JavaCharType
				&& e.getRight().getStaticType() instanceof JavaCharType)
			// replacing characters: the sequences of characters are passed as
			// references to the strings (not their contents), that cannot be
			// encoded
			return Term.app("str.replace_all", Sort.STRING, asString(left, e), fromChar(coerce(middle, Sort.INT, e)),
					fromChar(coerce(right, Sort.INT, e)));
		return null;
	}

	/**
	 * Yields the string representation of the value appended or inserted by the
	 * given operator of string builders, as {@link String#valueOf}, or
	 * {@code null} if the operator does not append nor insert a value whose
	 * representation can be encoded.
	 */
	private Term representation(
			Object op,
			Term value,
			ValueExpression e) {
		if (op instanceof JavaStringAppendStringOperator || op instanceof JavaStringInsertStringOperator)
			return asString(value, e);
		if (op instanceof JavaStringAppendCharOperator || op instanceof JavaStringInsertCharOperator)
			return fromChar(coerce(value, Sort.INT, e));
		if (op instanceof JavaStringAppendIntOperator || op instanceof JavaStringInsertIntOperator
				|| op instanceof JavaStringAppendLongOperator || op instanceof JavaStringInsertLongOperator)
			return decimal(coerce(value, Sort.INT, e));
		if (op instanceof JavaStringAppendBooleanOperator || op instanceof JavaStringInsertBooleanOperator)
			return Term.ite(coerce(value, Sort.BOOL, e), Term.string("true"), Term.string("false"));
		if (op instanceof JavaStringAppendDoubleOperator || op instanceof JavaStringInsertDoubleOperator)
			return floatToString(toFloat(value, Sort.FLOAT64, e), e);
		if (op instanceof JavaStringAppendFloatOperator || op instanceof JavaStringInsertFloatOperator)
			return floatToString(toFloat(value, Sort.FLOAT32, e), e);
		return null;
	}

	/**
	 * The given string without its leading and trailing characters up to
	 * {@code '\u0020'}, as {@link String#trim()}: an auxiliary string such that
	 * the given one is made of a prefix of such characters, the auxiliary
	 * string, and a suffix of such characters, where the auxiliary string is
	 * empty or starts and ends with other characters.
	 */
	private Term trim(
			Term s,
			UnaryExpression e) {
		Term blanks = Term.value("(re.* (re.range \"\\u{0}\" \"\\u{20}\"))", Sort.STRING);
		Term prefix = auxiliary(Sort.STRING, e);
		Term trimmed = auxiliary(Sort.STRING, e);
		Term suffix = auxiliary(Sort.STRING, e);
		Term last = sub(len(trimmed), num(1));
		getSymbols().require(Term.and(
				Term.eq(s, concat(concat(prefix, trimmed), suffix)),
				Term.app("str.in_re", Sort.BOOL, prefix, blanks),
				Term.app("str.in_re", Sort.BOOL, suffix, blanks),
				Term.or(Term.eq(trimmed, Term.string("")),
						Term.and(Term.app(">", Sort.BOOL, code(at(trimmed, num(0))), num(0x20)),
								Term.app(">", Sort.BOOL, code(at(trimmed, last)), num(0x20))))));
		return trimmed;
	}

	/**
	 * The last index of the given string in the given one, as
	 * {@link String#lastIndexOf(String)}: an auxiliary index where the string
	 * is found, and after which it is not (or {@code -1} if it is not found, or
	 * the length of the string if the searched one is empty).
	 */
	private Term lastIndexOfString(
			Term s,
			Term t,
			SymbolicExpression e) {
		Term i = auxiliary(Sort.INT, e);
		Term len = len(s);
		Term rest = substr(s, add(i, num(1)), sub(sub(len, i), num(1)));
		getSymbols().require(Term.ite(Term.eq(t, Term.string("")), Term.eq(i, len),
				Term.ite(Term.app("str.contains", Sort.BOOL, s, t),
						Term.and(ge(i, num(0)), le(add(i, len(t)), len), Term.eq(substr(s, i, len(t)), t),
								Term.not(Term.app("str.contains", Sort.BOOL, rest, t))),
						Term.eq(i, num(-1)))));
		return i;
	}

	/**
	 * Whether the given string is made of ASCII characters only.
	 */
	private static Term ascii(
			Term s) {
		return Term.app("str.in_re", Sort.BOOL, s,
				Term.value("(re.* (re.range \"\\u{0}\" \"\\u{7f}\"))", Sort.STRING));
	}

	private static Term concat(
			Term a,
			Term b) {
		return Term.app("str.++", Sort.STRING, a, b);
	}

	/**
	 * The string made of the given char.
	 */
	private static Term fromChar(
			Term c) {
		return Term.app("str.from_code", Sort.STRING, c);
	}

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
	 * Selects the representation for the given radix, using radix {@code 10}
	 * for radixes that are not valid, as Java does.
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
	 * {@link String#lastIndexOf(int)}: an auxiliary index such that the char is
	 * there, and not in the rest of the string (or {@code -1} if the char is
	 * not in the string). Precise only for chars.
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
	 * {@link String#compareTo(String)}: through an auxiliary index, the length
	 * of their longest common prefix.
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
	 * Converts the ASCII lowercase letters of the given string to uppercase.
	 */
	private static Term upper(
			Term s) {
		Term result = s;
		for (char c = 'a'; c <= 'z'; c++)
			result = Term.app("str.replace_all", Sort.STRING, result, Term.string(String.valueOf(c)),
					Term.string(String.valueOf(Character.toUpperCase(c))));
		return result;
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

	/**
	 * The code at position i of s, with the ASCII uppercase letters converted to
	 * lowercase (-1 past the end of the string).
	 */
	private static Term foldedCode(
		Term s,
		int i) {
		Term c = code(at(s, num(i)));
		return Term.ite(between(c, 'A', 'Z'), add(c, num('a' - 'A')), c);
	}

	/**
	 * String#equalsIgnoreCase: precise for strings of different length (false), and
	 * for ASCII strings of at most CASE_BOUND chars.
	 */
	private Term equalsIgnoreCase(
		Term a,
		Term b,
		ValueExpression e) {
		int CASE_BOUND = 8;
		Term sameLength = Term.eq(len(a), len(b));
		Term[] same = new Term[CASE_BOUND + 1];
		Term[] precise = new Term[2 * CASE_BOUND + 1];

		for (int i = 0; i < CASE_BOUND; i++) {
			same[i] = Term.eq(foldedCode(a, i), foldedCode(b, i));
			precise[2 * i] = le(code(at(a, num(i))), num(0x7F));
			precise[2 * i + 1] = le(code(at(b, num(i))), num(0x7F));
		}
		same[CASE_BOUND] = sameLength;
		precise[2 * CASE_BOUND] = le(len(a), num(CASE_BOUND));
		// different lengths: the result is false, whatever the chars are
		Term guard = Term.or(Term.not(sameLength), Term.and(precise));
		return guarded(guard, Term.and(same), List.of(a, b), e);
	}

}

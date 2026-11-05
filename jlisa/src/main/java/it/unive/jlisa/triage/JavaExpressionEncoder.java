package it.unive.jlisa.triage;

import it.unive.jlisa.program.operator.JavaLibraryFunction;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.triage.encoding.ExpressionEncoder;
import it.unive.lisa.triage.encoding.SortPolicy;
import it.unive.lisa.triage.encoding.SymbolTable;
import it.unive.lisa.triage.smt.Sort;
import it.unive.lisa.triage.smt.Term;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An {@link ExpressionEncoder} that also encodes the operators of the Java
 * frontend, so that counterexamples relying on them can be confirmed. Functions
 * of the Java library ({@link JavaLibraryFunction}) are encoded:
 * <ul>
 * <li>exactly, when they can be expressed over integers and booleans (e.g.,
 * {@link Integer#max(int, int)}, {@link Long#signum(long)},
 * {@link Integer#compareUnsigned(int, int)},
 * {@link Character#isValidCodePoint(int)});</li>
 * <li>through a table over the ASCII characters, for the functions over
 * characters (e.g., {@link Character#isLetter(int)}): the table is computed by
 * running the function of the Java library on each character, and the
 * encoding is precise only for ASCII characters (the result is an unknown
 * function of the character otherwise).</li>
 * </ul>
 * Other functions (e.g., bit manipulations) yield unconstrained values, as
 * any other operator unknown to this encoder.
 */
public class JavaExpressionEncoder extends ExpressionEncoder {

	/**
	 * The characters over which functions over characters are encoded through
	 * tables: the ones below this bound.
	 */
	private static final int TABLE_SIZE = 128;

	/**
	 * The functions over characters (with a single integral parameter) encoded
	 * through tables.
	 */
	private static final Set<JavaLibraryFunction> TABLES = Set.of(
			JavaLibraryFunction.CHARACTER_GET_DIRECTIONALITY,
			JavaLibraryFunction.CHARACTER_GET_NUMERIC_VALUE,
			JavaLibraryFunction.CHARACTER_GET_TYPE,
			JavaLibraryFunction.CHARACTER_TO_LOWER_CASE,
			JavaLibraryFunction.CHARACTER_TO_UPPER_CASE,
			JavaLibraryFunction.CHARACTER_TO_TITLE_CASE,
			JavaLibraryFunction.CHARACTER_TO_TITLE_CASE_CHAR,
			JavaLibraryFunction.CHARACTER_IS_ALPHABETIC,
			JavaLibraryFunction.CHARACTER_IS_DEFINED,
			JavaLibraryFunction.CHARACTER_IS_DIGIT,
			JavaLibraryFunction.CHARACTER_IS_IDENTIFIER_IGNORABLE,
			JavaLibraryFunction.CHARACTER_IS_IDEOGRAPHIC,
			JavaLibraryFunction.CHARACTER_IS_JAVA_IDENTIFIER_PART,
			JavaLibraryFunction.CHARACTER_IS_JAVA_IDENTIFIER_START,
			JavaLibraryFunction.CHARACTER_IS_LETTER,
			JavaLibraryFunction.CHARACTER_IS_LETTER_OR_DIGIT,
			JavaLibraryFunction.CHARACTER_IS_LOWER_CASE,
			JavaLibraryFunction.CHARACTER_IS_MIRRORED,
			JavaLibraryFunction.CHARACTER_IS_SPACE_CHAR,
			JavaLibraryFunction.CHARACTER_IS_TITLE_CASE,
			JavaLibraryFunction.CHARACTER_IS_UNICODE_IDENTIFIER_PART,
			JavaLibraryFunction.CHARACTER_IS_UNICODE_IDENTIFIER_START,
			JavaLibraryFunction.CHARACTER_IS_UPPER_CASE,
			JavaLibraryFunction.CHARACTER_IS_WHITESPACE,
			JavaLibraryFunction.CHARACTER_IS_JAVA_LETTER,
			JavaLibraryFunction.CHARACTER_IS_JAVA_LETTER_OR_DIGIT,
			JavaLibraryFunction.CHARACTER_IS_SPACE);

	/**
	 * The tables built so far, shared by all encoders since they only depend
	 * on the functions.
	 */
	private static final Map<JavaLibraryFunction, List<Segment>> CACHE = new ConcurrentHashMap<>();

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
		if (expression.getOperator() instanceof JavaLibraryFunction) {
			Term result = unary((JavaLibraryFunction) expression.getOperator(), arg, expression);
			if (result != null)
				return result;
		}
		return super.encodeUnary(expression, arg);
	}

	@Override
	protected Term encodeBinary(
			BinaryExpression expression,
			Term left,
			Term right)
			throws SemanticException {
		if (expression.getOperator() instanceof JavaLibraryFunction) {
			Term result = binary((JavaLibraryFunction) expression.getOperator(), left, right, expression);
			if (result != null)
				return result;
		}
		return super.encodeBinary(expression, left, right);
	}

	private Term unary(
			JavaLibraryFunction f,
			Term arg,
			UnaryExpression e) {
		if (TABLES.contains(f))
			return table(f, coerce(arg, Sort.INT, e), e);

		Term x = coerce(arg, Sort.INT, e);
		if (f == JavaLibraryFunction.INTEGER_SIGNUM || f == JavaLibraryFunction.LONG_SIGNUM)
			return Term.ite(lt(x, num(0)), num(-1), Term.ite(Term.eq(x, num(0)), num(0), num(1)));
		if (f == JavaLibraryFunction.INTEGER_TO_UNSIGNED_LONG)
			return unsigned(x, 32);
		if (f == JavaLibraryFunction.SHORT_TO_UNSIGNED_INT || f == JavaLibraryFunction.SHORT_TO_UNSIGNED_LONG)
			return unsigned(x, 16);
		if (f == JavaLibraryFunction.CHARACTER_CHAR_COUNT)
			return Term.ite(ge(x, num(0x10000)), num(2), num(1));
		if (f == JavaLibraryFunction.CHARACTER_IS_BMP_CODE_POINT)
			return between(x, 0, 0xFFFF);
		if (f == JavaLibraryFunction.CHARACTER_IS_VALID_CODE_POINT)
			return between(x, 0, Character.MAX_CODE_POINT);
		if (f == JavaLibraryFunction.CHARACTER_IS_SUPPLEMENTARY_CODE_POINT)
			return between(x, Character.MIN_SUPPLEMENTARY_CODE_POINT, Character.MAX_CODE_POINT);
		if (f == JavaLibraryFunction.CHARACTER_IS_ISO_CONTROL)
			return Term.or(between(x, 0, 0x1F), between(x, 0x7F, 0x9F));
		return null;
	}

	private Term binary(
			JavaLibraryFunction f,
			Term left,
			Term right,
			BinaryExpression e) {
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

		Term a = coerce(left, Sort.INT, e);
		Term b = coerce(right, Sort.INT, e);
		if (f == JavaLibraryFunction.INTEGER_MAX || f == JavaLibraryFunction.LONG_MAX)
			return Term.ite(ge(a, b), a, b);
		if (f == JavaLibraryFunction.INTEGER_MIN || f == JavaLibraryFunction.LONG_MIN)
			return Term.ite(Term.app("<=", Sort.BOOL, a, b), a, b);
		if (f == JavaLibraryFunction.INTEGER_SUM)
			return wrap(Term.app("+", Sort.INT, a, b), 32);
		if (f == JavaLibraryFunction.LONG_SUM)
			return wrap(Term.app("+", Sort.INT, a, b), 64);
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
			return Term.app("-", Sort.INT, a, b);
		if (f == JavaLibraryFunction.CHARACTER_TO_CODE_POINT)
			// ((high << 10) + low) + (MIN_SUPPLEMENTARY_CODE_POINT
			// - (MIN_HIGH_SURROGATE << 10) - MIN_LOW_SURROGATE), that cannot
			// overflow for chars
			return Term.app("+", Sort.INT, Term.app("*", Sort.INT, a, num(1024)), b,
					num(Character.MIN_SUPPLEMENTARY_CODE_POINT - (Character.MIN_HIGH_SURROGATE << 10)
							- Character.MIN_LOW_SURROGATE));
		return null;
	}

	/**
	 * A range of characters where a function has the same value, or the
	 * same difference from the character.
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
	 * Encodes the given function over characters through its table over the
	 * ASCII characters, computed by running the function: the encoding is
	 * precise only for ASCII characters.
	 */
	private Term table(
			JavaLibraryFunction f,
			Term c,
			UnaryExpression e) {
		List<Segment> segments = CACHE.computeIfAbsent(f, JavaExpressionEncoder::segments);
		boolean bool = segments.get(0).value instanceof Boolean;
		Term result = null;
		for (int i = segments.size() - 1; i >= 0; i--) {
			Segment s = segments.get(i);
			Term value = bool ? Term.bool((Boolean) s.value)
					: s.offset != null ? Term.app("+", Sort.INT, c, num(s.offset)) : num(integral(s.value));
			result = result == null ? value : Term.ite(Term.app("<=", Sort.BOOL, c, num(s.high)), value, result);
		}
		return guarded(between(c, 0, TABLE_SIZE - 1), result, List.of(c), e);
	}

	private static List<Segment> segments(
			JavaLibraryFunction f) {
		Object[] values = new Object[TABLE_SIZE];
		for (int c = 0; c < TABLE_SIZE; c++)
			values[c] = f.apply(c);
		List<Segment> segments = new ArrayList<>();
		int c = 0;
		while (c < TABLE_SIZE) {
			Object v = values[c];
			int end = c;
			while (end + 1 < TABLE_SIZE && values[end + 1].equals(v))
				end++;
			Long offset = null;
			if (!(v instanceof Boolean)) {
				// runs where the value follows the character (e.g., case
				// conversions) are merged as offsets
				long delta = integral(v) - c;
				int run = c;
				while (run + 1 < TABLE_SIZE && integral(values[run + 1]) - (run + 1) == delta)
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

	private static Term num(
			long value) {
		return Term.integer(value);
	}

	private static Term lt(
			Term a,
			Term b) {
		return Term.app("<", Sort.BOOL, a, b);
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
		return Term.and(ge(x, num(low)), Term.app("<=", Sort.BOOL, x, num(high)));
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
		return Term.ite(lt(x, num(0)), Term.app("+", Sort.INT, x, Term.integer(BigInteger.ONE.shiftLeft(bits))), x);
	}

	/**
	 * Wraps a mathematical integer to the range of signed integers of the
	 * given bit width.
	 */
	private static Term wrap(
			Term x,
			int bits) {
		BigInteger half = BigInteger.ONE.shiftLeft(bits - 1);
		return Term.app("-", Sort.INT,
				Term.app("mod", Sort.INT, Term.app("+", Sort.INT, x, Term.integer(half)),
						Term.integer(BigInteger.ONE.shiftLeft(bits))),
				Term.integer(half));
	}
}

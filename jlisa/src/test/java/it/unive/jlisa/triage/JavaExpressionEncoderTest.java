package it.unive.jlisa.triage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import it.unive.jlisa.program.operator.JavaByteToUnsignedIntOperator;
import it.unive.jlisa.program.operator.JavaCharacterEqualsOperator;
import it.unive.jlisa.program.operator.JavaCharacterForDigitOperator;
import it.unive.jlisa.program.operator.JavaDoubleIsFiniteOperator;
import it.unive.jlisa.program.operator.JavaDoubleIsInfiniteOperator;
import it.unive.jlisa.program.operator.JavaDoubleIsNaNOperator;
import it.unive.jlisa.program.operator.JavaDoubleLongBitsToDoubleOperator;
import it.unive.jlisa.program.operator.JavaDoubleToLongBitsOperator;
import it.unive.jlisa.program.operator.JavaDoubleToRawLongBitsOperator;
import it.unive.jlisa.program.operator.JavaFloatIsFiniteOperator;
import it.unive.jlisa.program.operator.JavaFloatIsInfiniteOperator;
import it.unive.jlisa.program.operator.JavaFloatIsNaNOperator;
import it.unive.jlisa.program.operator.JavaFloatToIntBitsOperator;
import it.unive.jlisa.program.operator.JavaFloatToRawIntBitsOperator;
import it.unive.jlisa.program.operator.JavaLibraryFunction;
import it.unive.jlisa.program.operator.JavaMathAbsOperator;
import it.unive.jlisa.program.operator.JavaMathFloorOperator;
import it.unive.jlisa.program.operator.JavaMathRoundOperator;
import it.unive.jlisa.program.operator.JavaMathSqrtOperator;
import it.unive.jlisa.program.operator.JavaStringAppendBooleanOperator;
import it.unive.jlisa.program.operator.JavaStringAppendCharOperator;
import it.unive.jlisa.program.operator.JavaStringAppendIntOperator;
import it.unive.jlisa.program.operator.JavaStringAppendLongOperator;
import it.unive.jlisa.program.operator.JavaStringAppendStringOperator;
import it.unive.jlisa.program.operator.JavaStringCharAtOperator;
import it.unive.jlisa.program.operator.JavaStringDeleteCharAtOperator;
import it.unive.jlisa.program.operator.JavaStringDeleteOperator;
import it.unive.jlisa.program.operator.JavaStringEndsWithOperator;
import it.unive.jlisa.program.operator.JavaStringEqualsIgnoreCaseOperator;
import it.unive.jlisa.program.operator.JavaStringIndexOfOperator;
import it.unive.jlisa.program.operator.JavaStringIndexOfStringFromIndexOperator;
import it.unive.jlisa.program.operator.JavaStringInsertCharOperator;
import it.unive.jlisa.program.operator.JavaStringInsertIntOperator;
import it.unive.jlisa.program.operator.JavaStringInsertStringOperator;
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
import it.unive.jlisa.program.operator.JavaStringValueOfIntOperator;
import it.unive.jlisa.program.operator.JavaStringValueOfLongOperator;
import it.unive.jlisa.program.type.JavaBooleanType;
import it.unive.jlisa.program.type.JavaByteType;
import it.unive.jlisa.program.type.JavaCharType;
import it.unive.jlisa.program.type.JavaDoubleType;
import it.unive.jlisa.program.type.JavaFloatType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.program.type.JavaLongType;
import it.unive.jlisa.program.type.JavaShortType;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.type.StringType;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.TypeConv;
import it.unive.lisa.symbolic.value.operator.ternary.TernaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.triage.encoding.SortPolicy;
import it.unive.lisa.triage.encoding.SymbolTable;
import it.unive.lisa.triage.encoding.ValueSort;
import it.unive.lisa.triage.smt.FloatingPoints;
import it.unive.lisa.triage.smt.SmtLibProcessSolver;
import it.unive.lisa.triage.smt.SmtResult;
import it.unive.lisa.triage.smt.SmtSolver;
import it.unive.lisa.triage.smt.Sort;
import it.unive.lisa.triage.smt.Term;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeTokenType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Checks the encodings of the functions of the Java library produced by
 * {@link JavaExpressionEncoder} against the functions themselves: for each
 * function and each tested combination of arguments, the encoding of the
 * function applied to the arguments must denote the value computed by the Java
 * library, whenever the encoding claims to be precise. Tests are skipped if z3
 * is not available.
 */
public class JavaExpressionEncoderTest {

	private static final SourceCodeLocation LOC = new SourceCodeLocation("test", 0, 0);

	private static SmtSolver solver;

	private static int counter;

	/**
	 * A symbol table recording the conditions under which the encoding is
	 * precise, and the requirements of the encoding.
	 */
	private static class Symbols
			implements
			SymbolTable {

		private final List<Term> guards = new ArrayList<>();

		private final List<Term> requirements = new ArrayList<>();

		private boolean havoc;

		@Override
		public Term constant(
				Identifier id,
				Sort sort) {
			return solver.declare(id.getName() + "#" + (counter++), sort);
		}

		@Override
		public Term fresh(
				Sort sort,
				SymbolicExpression origin) {
			havoc = true;
			return solver.declare("fresh#" + (counter++), sort);
		}

		@Override
		public Term fresh(
				Sort sort,
				SymbolicExpression origin,
				Term guard) {
			guards.add(guard);
			return solver.declare("fresh#" + (counter++), sort);
		}

		@Override
		public void exactOnlyIf(
				Term guard) {
			guards.add(guard);
		}

		@Override
		public void require(
				Term condition) {
			requirements.add(condition);
		}
	}

	@BeforeAll
	public static void start() {
		assumeTrue(SmtLibProcessSolver.isZ3Available(), "z3 is not available");
		solver = SmtLibProcessSolver.z3();
		solver.declareDatatype(ValueSort.DATATYPE);
	}

	@AfterAll
	public static void stop() {
		if (solver != null)
			solver.close();
	}

	private static Constant constant(
			Object value) {
		Type type;
		if (value instanceof Integer)
			type = JavaIntType.INSTANCE;
		else if (value instanceof Long)
			type = JavaLongType.INSTANCE;
		else if (value instanceof Character)
			type = JavaCharType.INSTANCE;
		else if (value instanceof Boolean)
			type = JavaBooleanType.INSTANCE;
		else if (value instanceof Double)
			type = JavaDoubleType.INSTANCE;
		else if (value instanceof Float)
			type = JavaFloatType.INSTANCE;
		else
			type = StringType.INSTANCE;
		return new Constant(type, value, LOC);
	}

	private static Term expected(
			Object value) {
		if (value instanceof Boolean)
			return Term.bool((Boolean) value);
		if (value instanceof String)
			return Term.string((String) value);
		if (value instanceof Character)
			return Term.integer((Character) value);
		return Term.integer(((Number) value).longValue());
	}

	/**
	 * Checks the encoding of the given function applied to the given arguments,
	 * yielding whether the encoding is precise for them.
	 */
	private static boolean check(
			JavaLibraryFunction f,
			Object... args)
			throws SemanticException {
		Object value;
		try {
			value = f.apply(args);
		} catch (RuntimeException e) {
			// the function throws for these arguments
			return true;
		}
		Symbols symbols = new Symbols();
		JavaExpressionEncoder encoder = new JavaExpressionEncoder(new SortPolicy(new HashMap<>()), symbols);
		SymbolicExpression expr;
		Type type = f.returnsString() ? StringType.INSTANCE : f.getResultType();
		if (args.length == 1)
			expr = new UnaryExpression(type, constant(args[0]), f, LOC);
		else if (args.length == 2)
			expr = new BinaryExpression(type, constant(args[0]), constant(args[1]), f, LOC);
		else
			expr = new TernaryExpression(type, constant(args[0]), constant(args[1]), constant(args[2]), f, LOC);
		Term term = expr.accept(encoder);
		if (symbols.havoc)
			return false;
		solver.push();
		for (Term r : symbols.requirements)
			solver.assertTerm(r);
		for (Term g : symbols.guards)
			solver.assertTerm(g);
		SmtResult precise = solver.check();
		solver.pop();
		if (precise != SmtResult.SAT)
			// the encoding is not precise for these arguments
			return false;
		solver.push();
		for (Term r : symbols.requirements)
			solver.assertTerm(r);
		for (Term g : symbols.guards)
			solver.assertTerm(g);
		solver.assertTerm(Term.not(Term.eq(term, expected(value))));
		SmtResult res = solver.check();
		solver.pop();
		assertEquals(SmtResult.UNSAT, res, f + " on " + java.util.Arrays.toString(args) + " should be " + value);
		return true;
	}

	private static final Random RANDOM = new Random(42);

	private static List<Integer> ints() {
		List<Integer> result = new ArrayList<>(List.of(0, 1, -1, 2, -2, 7, 100, 255, 256, 65535, 65536,
				Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE - 1, Integer.MIN_VALUE + 1, 0x55555555,
				0x80000000, 0xF0F0F0F0));
		for (int i = 0; i < 6; i++)
			result.add(RANDOM.nextInt());
		return result;
	}

	private static List<Long> longs() {
		List<Long> result = new ArrayList<>(List.of(0L, 1L, -1L, 2L, 7L, 255L, Long.MAX_VALUE, Long.MIN_VALUE,
				0x5555555555555555L, 0x0123456789ABCDEFL, (long) Integer.MIN_VALUE));
		for (int i = 0; i < 6; i++)
			result.add(RANDOM.nextLong());
		return result;
	}

	private static List<Integer> codePoints() {
		List<Integer> result = new ArrayList<>(List.of(0, 9, 10, 32, 36, 48, 57, 65, 90, 95, 97, 122, 127, 128,
				0xA0, 0xC0, 0x01C5, 0x0660, 0x2028, 0x3000, 0x4E00, 0xD800, 0xDC00, 0xFF10, 0xFF21, 0xFFFF, 0x10000,
				0x1D400, Character.MAX_CODE_POINT, -1, Character.MAX_CODE_POINT + 1));
		for (int i = 0; i < 10; i++)
			result.add(RANDOM.nextInt(0x30000));
		return result;
	}

	private static List<JavaLibraryFunction> functions() throws IllegalAccessException {
		List<JavaLibraryFunction> result = new ArrayList<>();
		for (Field field : JavaLibraryFunction.class.getFields())
			if (Modifier.isStatic(field.getModifiers()) && field.getType() == JavaLibraryFunction.class)
				result.add((JavaLibraryFunction) field.get(null));
		return result;
	}

	/**
	 * Narrows the given integer to the type of the parameters of the function
	 * with the given name.
	 */
	private static int narrow(
			String name,
			int value) {
		return name.startsWith("Short.") ? (short) value : name.startsWith("Byte.") ? (byte) value : value;
	}

	@Test
	public void integerFunctions() throws Exception {
		for (JavaLibraryFunction f : functions()) {
			String name = f.toString();
			if (!name.startsWith("Integer.") && !name.startsWith("Short.") && !name.startsWith("Byte."))
				continue;
			for (Integer a : ints())
				if (name.contains("radix") || name.contains("rotate") || name.endsWith("compare")
						|| name.contains("compareUnsigned") || name.endsWith("max") || name.endsWith("min")
						|| name.endsWith("sum")) {
					for (Integer b : List.of(0, 1, -1, 2, 8, 10, 16, 31, 32, 33, 36, 37, -5, 100, a))
						assertTrue(check(f, narrow(name, a), narrow(name, b)), f + " not precise on " + a + ", " + b);
				} else
					assertTrue(check(f, narrow(name, a)), f + " not precise on " + a);
		}
	}

	@Test
	public void longFunctions() throws Exception {
		for (JavaLibraryFunction f : functions()) {
			String name = f.toString();
			if (!name.startsWith("Long."))
				continue;
			for (Long a : longs())
				if (name.contains("radix") || name.contains("rotate")) {
					for (Integer b : List.of(0, 1, -1, 2, 8, 10, 16, 36, 37, 63, 64, 65))
						assertTrue(check(f, a, b), f + " not precise on " + a + ", " + b);
				} else if (name.endsWith("compare") || name.contains("compareUnsigned") || name.endsWith("max")
						|| name.endsWith("min") || name.endsWith("sum")) {
					for (Long b : List.of(0L, -1L, 1L, Long.MAX_VALUE, Long.MIN_VALUE, a))
						assertTrue(check(f, a, b), f + " not precise on " + a + ", " + b);
				} else
					assertTrue(check(f, a), f + " not precise on " + a);
		}
	}

	@Test
	public void characterFunctions() throws Exception {
		for (JavaLibraryFunction f : functions()) {
			String name = f.toString();
			if (!name.startsWith("Character."))
				continue;
			for (Integer cp : codePoints()) {
				boolean valid = cp >= 0 && cp <= Character.MAX_CODE_POINT;
				boolean isChar = cp >= 0 && cp <= Character.MAX_VALUE;
				if (name.equals("Character.digit"))
					for (Integer radix : List.of(-1, 0, 2, 8, 10, 16, 36, 37))
						assertTrue(check(f, cp, radix) || !valid, f + " not precise on " + cp + ", " + radix);
				else if (name.equals("Character.compare") || name.equals("Character.toCodePoint")) {
					if (isChar)
						for (Integer other : List.of(0, 65, 0xDC00, 0xFFFF, cp))
							assertTrue(check(f, (char) (int) cp, (char) (int) other),
									f + " not precise on " + cp + ", " + other);
				} else if (name.contains("(char)") || name.equals("Character.isJavaLetter")
						|| name.equals("Character.isJavaLetterOrDigit") || name.equals("Character.isSpace")
						|| name.equals("Character.toString")) {
					if (isChar)
						assertTrue(check(f, (char) (int) cp), f + " not precise on " + cp);
				} else
					assertTrue(check(f, cp) || !valid, f + " not precise on " + cp);
			}
		}
	}

	@Test
	public void booleanFunctions() throws Exception {
		for (JavaLibraryFunction f : functions()) {
			String name = f.toString();
			if (!name.startsWith("Boolean."))
				continue;
			for (Boolean a : List.of(true, false))
				if (name.equals("Boolean.toString"))
					assertTrue(check(f, a));
				else
					for (Boolean b : List.of(true, false))
						assertTrue(check(f, a, b), f + " not precise on " + a + ", " + b);
		}
	}

	@Test
	public void stringFunctions() throws Exception {
		List<String> strings = List.of("", "a", "abc", "abcabc", "Hello World", "hELLO world", "aXb", "xéy",
				"Été");
		List<Integer> chars = List.of((int) 'a', (int) 'b', (int) 'c', (int) 'o', (int) 'z', 0xE9, 0x1F600);
		for (String s : strings) {
			check(JavaLibraryFunction.STRING_IS_EMPTY, s);
			for (Integer c : chars) {
				boolean bmp = c <= Character.MAX_VALUE;
				assertTrue(check(JavaLibraryFunction.STRING_INDEX_OF_CHAR, s, c) || !bmp);
				assertTrue(check(JavaLibraryFunction.STRING_LAST_INDEX_OF_CHAR, s, c) || !bmp);
				for (Integer from : List.of(-1, 0, 1, 3, 100)) {
					assertTrue(check(JavaLibraryFunction.STRING_INDEX_OF_CHAR_FROM, s, c, from) || !bmp);
					assertTrue(check(JavaLibraryFunction.STRING_LAST_INDEX_OF_CHAR_FROM, s, c, from) || !bmp);
				}
			}
			for (String t : strings) {
				boolean ascii = s.chars().allMatch(c -> c < 128) && t.chars().allMatch(c -> c < 128);
				assertTrue(check(JavaLibraryFunction.STRING_COMPARE_TO_IGNORE_CASE, s, t) || !ascii);
			}
		}
	}

	/**
	 * Checks the encoding of the given operator of the Java frontend applied to
	 * the given arguments against the given value (computed by Java): precise
	 * encodings must yield it.
	 */
	private static void checkOperator(
			Object op,
			Type type,
			Supplier<Object> reference,
			Object... args)
			throws SemanticException {
		Object value;
		try {
			value = reference.get();
		} catch (RuntimeException e) {
			// the operation throws for these arguments
			return;
		}
		Symbols symbols = new Symbols();
		JavaExpressionEncoder encoder = new JavaExpressionEncoder(new SortPolicy(new HashMap<>()), symbols);
		SymbolicExpression expr;
		if (args.length == 1)
			expr = new UnaryExpression(type, constant(args[0]), (UnaryOperator) op, LOC);
		else if (args.length == 2)
			expr = new BinaryExpression(type, constant(args[0]), constant(args[1]), (BinaryOperator) op, LOC);
		else
			expr = new TernaryExpression(type, constant(args[0]), constant(args[1]), constant(args[2]),
					(TernaryOperator) op, LOC);
		Term term = expr.accept(encoder);
		String what = op + " on " + java.util.Arrays.toString(args);
		assertFalse(symbols.havoc, what + " is not precise");
		solver.push();
		try {
			for (Term r : symbols.requirements)
				solver.assertTerm(r);
			for (Term g : symbols.guards)
				solver.assertTerm(g);
			assertEquals(SmtResult.SAT, solver.check(), what + " is not precise");
			Term exp = value instanceof Double ? FloatingPoints.literal((Double) value)
					: value instanceof Float ? FloatingPoints.literal((Float) value) : expected(value);
			solver.assertTerm(Term.not(Term.eq(term, exp)));
			assertEquals(SmtResult.UNSAT, solver.check(), what + " should be " + value);
		} finally {
			solver.pop();
		}
	}

	@Test
	public void stringOperators() throws Exception {
		Type str = StringType.INSTANCE;
		Type integer = JavaIntType.INSTANCE;
		Type bool = JavaBooleanType.INSTANCE;
		List<String> strings = List.of("", "a", "abc", "abcabc", "  Hello World \t", "hELLO", "\n\t ", "a-b-c");
		for (String s : strings) {
			checkOperator(JavaStringTrimOperator.INSTANCE, str, () -> s.trim(), s);
			checkOperator(JavaStringToLowerCaseOperator.INSTANCE, str, () -> s.toLowerCase(), s);
			checkOperator(JavaStringToUpperCaseOperator.INSTANCE, str, () -> s.toUpperCase(), s);
			for (int i = -1; i <= s.length() + 1; i++) {
				int k = i;
				checkOperator(JavaStringCharAtOperator.INSTANCE, JavaCharType.INSTANCE, () -> s.charAt(k), s, k);
				checkOperator(JavaStringSubstringOperator.INSTANCE, str, () -> s.substring(k), s, k);
				checkOperator(JavaStringDeleteCharAtOperator.INSTANCE, str,
						() -> new StringBuilder(s).deleteCharAt(k).toString(), s, k);
				checkOperator(JavaStringInsertStringOperator.INSTANCE, str,
						() -> new StringBuilder(s).insert(k, "xy").toString(), s, k, "xy");
				checkOperator(JavaStringInsertCharOperator.INSTANCE, str,
						() -> new StringBuilder(s).insert(k, 'q').toString(), s, k, 'q');
				checkOperator(JavaStringInsertIntOperator.INSTANCE, str,
						() -> new StringBuilder(s).insert(k, -42).toString(), s, k, -42);
				checkOperator(JavaStringSetCharAtOperator.INSTANCE, str, () -> {
					StringBuilder sb = new StringBuilder(s);
					sb.setCharAt(k, 'z');
					return sb.toString();
				}, s, k, 'z');
				for (int j = k; j <= s.length() + 2; j++) {
					int l = j;
					checkOperator(JavaStringSubstringFromToOperator.INSTANCE, str, () -> s.substring(k, l), s, k, l);
					checkOperator(JavaStringDeleteOperator.INSTANCE, str,
							() -> new StringBuilder(s).delete(k, l).toString(), s, k, l);
				}
			}
			for (String t : List.of("", "a", "bc", "abc", "ell", "-")) {
				checkOperator(JavaStringStartsWithOperator.INSTANCE, bool, () -> s.startsWith(t), s, t);
				checkOperator(JavaStringEndsWithOperator.INSTANCE, bool, () -> s.endsWith(t), s, t);
				checkOperator(JavaStringIndexOfOperator.INSTANCE, integer, () -> s.indexOf(t), s, t);
				checkOperator(JavaStringLastIndexOfStringOperator.INSTANCE, integer, () -> s.lastIndexOf(t), s, t);
				checkOperator(JavaStringEqualsIgnoreCaseOperator.INSTANCE, bool, () -> s.equalsIgnoreCase(t), s, t);
				checkOperator(JavaStringAppendStringOperator.INSTANCE, str, () -> s + t, s, t);
				for (int from : List.of(-1, 0, 1, 3, 100)) {
					checkOperator(JavaStringIndexOfStringFromIndexOperator.INSTANCE, integer,
							() -> s.indexOf(t, from), s, t, from);
					checkOperator(JavaStringStartsWithFromIndexOperator.INSTANCE, bool, () -> s.startsWith(t, from),
							s, t, from);
				}
			}
			checkOperator(JavaStringReplaceOperator.INSTANCE, str, () -> s.replace('a', 'b'), s, 'a', 'b');
			checkOperator(JavaStringAppendCharOperator.INSTANCE, str, () -> s + 'c', s, 'c');
			checkOperator(JavaStringAppendIntOperator.INSTANCE, str, () -> s + -7, s, -7);
			checkOperator(JavaStringAppendLongOperator.INSTANCE, str, () -> s + 123456789012L, s, 123456789012L);
			checkOperator(JavaStringAppendBooleanOperator.INSTANCE, str, () -> s + true, s, true);
		}
		checkOperator(JavaStringValueOfIntOperator.INSTANCE, str, () -> String.valueOf(-15), -15);
		checkOperator(JavaStringValueOfLongOperator.INSTANCE, str, () -> String.valueOf(1L << 40), 1L << 40);
		checkOperator(JavaStringValueOfCharOperator.INSTANCE, str, () -> String.valueOf('x'), 'x');
		checkOperator(JavaStringValueOfBooleanOperator.INSTANCE, str, () -> String.valueOf(false), false);
		checkOperator(JavaCharacterEqualsOperator.INSTANCE, bool, () -> 'a' == 'a', 'a', 'a');
		checkOperator(JavaCharacterEqualsOperator.INSTANCE, bool, () -> 'a' == 'b', 'a', 'b');
	}

	@Test
	public void mathFunctions() throws Exception {
		Type dbl = JavaDoubleType.INSTANCE;
		for (int i : List.of(0, 5, -5, Integer.MAX_VALUE, Integer.MIN_VALUE))
			checkOperator(JavaMathAbsOperator.INSTANCE, JavaIntType.INSTANCE, () -> Math.abs(i), i);
		for (long l : List.of(0L, 7L, -7L, Long.MAX_VALUE, Long.MIN_VALUE))
			checkOperator(JavaMathAbsOperator.INSTANCE, JavaLongType.INSTANCE, () -> Math.abs(l), l);
		List<Double> doubles = List.of(0.0, -0.0, 0.5, -0.5, 1.5, -1.5, 2.5, -2.5, 3.7, -3.7, 1e300, -1e300, 9.3e18,
				-9.3e18, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.MIN_VALUE, 2.0);
		for (double d : doubles) {
			checkOperator(JavaMathAbsOperator.INSTANCE, dbl, () -> Math.abs(d), d);
			checkOperator(JavaMathFloorOperator.INSTANCE, dbl, () -> Math.floor(d), d);
			checkOperator(JavaMathSqrtOperator.INSTANCE, dbl, () -> Math.sqrt(d), d);
			checkOperator(JavaMathRoundOperator.INSTANCE, dbl, () -> Math.round(d), d);
			float f = (float) d;
			checkOperator(JavaMathRoundOperator.INSTANCE, dbl, () -> Math.round(f), f);
			checkOperator(JavaMathAbsOperator.INSTANCE, JavaFloatType.INSTANCE, () -> Math.abs(f), f);
		}
	}

	@Test
	public void floatingPointPredicatesAndBits() throws Exception {
		Type bool = JavaBooleanType.INSTANCE;
		List<Double> doubles = List.of(0.0, -0.0, 1.0, -1.5, 1e300, Double.MIN_VALUE, Double.MAX_VALUE, Double.NaN,
				Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);
		for (double d : doubles) {
			checkOperator(JavaDoubleIsNaNOperator.INSTANCE, bool, () -> Double.isNaN(d), d);
			checkOperator(JavaDoubleIsInfiniteOperator.INSTANCE, bool, () -> Double.isInfinite(d), d);
			checkOperator(JavaDoubleIsFiniteOperator.INSTANCE, bool, () -> Double.isFinite(d), d);
			checkOperator(JavaDoubleToLongBitsOperator.INSTANCE, JavaLongType.INSTANCE,
					() -> Double.doubleToLongBits(d), d);
			checkOperator(JavaDoubleLongBitsToDoubleOperator.INSTANCE, JavaDoubleType.INSTANCE,
					() -> Double.longBitsToDouble(Double.doubleToLongBits(d)), Double.doubleToLongBits(d));
			if (!Double.isNaN(d))
				checkOperator(JavaDoubleToRawLongBitsOperator.INSTANCE, JavaLongType.INSTANCE,
						() -> Double.doubleToRawLongBits(d), d);
			float f = (float) d;
			checkOperator(JavaFloatIsNaNOperator.INSTANCE, bool, () -> Float.isNaN(f), f);
			checkOperator(JavaFloatIsInfiniteOperator.INSTANCE, bool, () -> Float.isInfinite(f), f);
			checkOperator(JavaFloatIsFiniteOperator.INSTANCE, bool, () -> Float.isFinite(f), f);
			checkOperator(JavaFloatToIntBitsOperator.INSTANCE, JavaIntType.INSTANCE, () -> Float.floatToIntBits(f), f);
			if (!Float.isNaN(f))
				checkOperator(JavaFloatToRawIntBitsOperator.INSTANCE, JavaIntType.INSTANCE,
						() -> Float.floatToRawIntBits(f), f);
		}
		for (int b : List.of(0, 1, 127, -1, -128))
			checkOperator(JavaByteToUnsignedIntOperator.INSTANCE, JavaIntType.INSTANCE,
					() -> Byte.toUnsignedInt((byte) b), b);
		for (int radix : List.of(1, 2, 10, 16, 36, 37))
			for (int digit : List.of(-1, 0, 1, 9, 10, 15, 35, 36))
				checkOperator(JavaCharacterForDigitOperator.INSTANCE, JavaCharType.INSTANCE,
						() -> Character.forDigit(digit, radix), digit, radix);
		for (String s : List.of("", "a", "abcabc", "aaaa"))
			for (String t : List.of("", "a", "bc", "abc", "z"))
				for (int from = -1; from <= s.length() + 1; from++) {
					int k = from;
					checkOperator(JavaStringLastIndexOfStringFromIndexOperator.INSTANCE, JavaIntType.INSTANCE,
							() -> s.lastIndexOf(t, k), s, t, k);
				}
	}

	@Test
	public void floatingPointFunctions() throws Exception {
		List<Double> doubles = List.of(0.0, -0.0, 1.0, -1.0, 1.5, 123.0, -42.0, 1e7, 9999999.0, 1e-3, Double.NaN,
				Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.MIN_VALUE, Double.MAX_VALUE, 0.1);
		for (Double d : doubles) {
			assertTrue(check(JavaLibraryFunction.MATH_GET_EXPONENT_DOUBLE, d), "getExponent " + d);
			assertTrue(check(JavaLibraryFunction.MATH_GET_EXPONENT_FLOAT, (float) (double) d), "getExponent " + d);
			boolean printable = d.isNaN() || d.isInfinite() || d == 0
					|| (d == Math.rint(d) && Math.abs(d) < 1e7 && Math.abs(d) >= Double.MIN_NORMAL);
			assertTrue(check(JavaLibraryFunction.DOUBLE_TO_STRING, d) || !printable, "toString " + d);
			assertTrue(check(JavaLibraryFunction.FLOAT_TO_STRING, (float) (double) d) || !printable,
					"toString " + d);
			for (Double other : doubles) {
				assertTrue(check(JavaLibraryFunction.DOUBLE_COMPARE, d, other), "compare " + d + " " + other);
				assertTrue(check(JavaLibraryFunction.FLOAT_COMPARE, (float) (double) d, (float) (double) other),
						"compare " + d + " " + other);
			}
		}
	}

	@Test
	public void narrowingConversions() throws SemanticException {
		// as Java's casts, conversions keep the lowest bits (chars are
		// unsigned)
		Object[][] cases = {
				{ JavaCharType.INSTANCE, -1, 65535 },
				{ JavaCharType.INSTANCE, 65536 + 'a', (int) 'a' },
				{ JavaByteType.INSTANCE, 200, -56 },
				{ JavaShortType.INSTANCE, 40000, -25536 },
				{ JavaIntType.INSTANCE, 1L << 32, 0 } };
		for (Object[] c : cases) {
			Type target = (Type) c[0];
			Symbols symbols = new Symbols();
			JavaExpressionEncoder encoder = new JavaExpressionEncoder(new SortPolicy(new HashMap<>()), symbols);
			BinaryExpression conversion = new BinaryExpression(target, constant(c[1]),
					new Constant(new TypeTokenType(Set.of(target)), target, LOC), TypeConv.INSTANCE, LOC);
			Term term = conversion.accept(encoder);
			assertFalse(symbols.havoc, "(" + target + ") " + c[1] + " is not precise");
			solver.push();
			for (Term r : symbols.requirements)
				solver.assertTerm(r);
			for (Term g : symbols.guards)
				solver.assertTerm(g);
			solver.assertTerm(Term.not(Term.eq(term, expected(c[2]))));
			assertEquals(SmtResult.UNSAT, solver.check(), "(" + target + ") " + c[1] + " should be " + c[2]);
			solver.pop();
		}
	}

	@Test
	public void floatingPointLiterals() {
		for (double d : new double[] { 0.0, -0.0, 1.5, Double.NaN, Double.NEGATIVE_INFINITY, Double.MIN_VALUE })
			assertEquals(Double.doubleToRawLongBits(d) == Double.doubleToRawLongBits(Double.NaN)
					? Double.NaN
					: d,
					FloatingPoints.parse(evaluate(FloatingPoints.literal(d)), Sort.FLOAT64));
	}

	private static String evaluate(
			Term term) {
		solver.push();
		assertEquals(SmtResult.SAT, solver.check());
		String value = solver.getValues(List.of(term)).get(term);
		solver.pop();
		return value;
	}
}

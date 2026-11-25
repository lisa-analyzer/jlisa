package it.unive.jlisa.triage;

import it.unive.jlisa.program.operator.JavaMathCosOperator;
import it.unive.jlisa.program.operator.JavaStringInsertFloatOperator;
import it.unive.jlisa.program.operator.JavaStringInsertDoubleOperator;
import it.unive.jlisa.program.operator.JavaStringAppendFloatOperator;
import it.unive.jlisa.program.operator.JavaStringAppendDoubleOperator;
import it.unive.jlisa.program.operator.JavaStringValueOfFloatOperator;
import it.unive.jlisa.program.operator.JavaStringValueOfDoubleOperator;
import it.unive.jlisa.program.operator.JavaStringCompareToOperator;
import it.unive.jlisa.program.operator.JavaLongCompareOperator;
import it.unive.jlisa.program.operator.JavaByteCompareOperator;
import it.unive.jlisa.program.operator.JavaShortCompareOperator;
import it.unive.jlisa.program.operator.JavaIntegerCompareOperator;
import it.unive.jlisa.program.operator.JavaCharacterDigitOperator;
import it.unive.jlisa.program.operator.JavaCharacterToUpperCaseOperator;
import it.unive.jlisa.program.operator.JavaCharacterIsLetterOperator;
import it.unive.jlisa.program.operator.JavaCharacterIsDigitOperator;
import it.unive.jlisa.program.operator.JavaDoubleCompareOperator;
import it.unive.jlisa.program.operator.JavaStrictMathMin;
import it.unive.jlisa.program.operator.JavaStrictMathMax;
import it.unive.jlisa.program.operator.JavaMathMin;
import it.unive.jlisa.program.operator.JavaMathMax;
import it.unive.jlisa.program.operator.JavaStrictMathSqrtOperator;
import it.unive.jlisa.program.operator.JavaStrictMathFloorOperator;
import it.unive.jlisa.program.operator.JavaStrictMathAbsOperator;
import it.unive.jlisa.program.operator.JavaStringIndexOfCharFromIndexOperator;
import it.unive.jlisa.program.operator.JavaMathToRadiansOperator;
import it.unive.jlisa.program.operator.JavaStringLengthOperator;
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
import it.unive.jlisa.program.operator.JavaMathExpOperator;
import it.unive.jlisa.program.operator.JavaMathFloorOperator;
import it.unive.jlisa.program.operator.JavaMathRoundOperator;
import it.unive.jlisa.program.operator.JavaMathSinOperator;
import it.unive.jlisa.program.operator.JavaMathSqrtOperator;
import it.unive.jlisa.program.operator.JavaStrictMathExpOperator;
import it.unive.jlisa.program.operator.JavaStrictMathSinOperator;
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
import it.unive.lisa.symbolic.value.Variable;
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
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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

		/**
		 * The functions computed on concrete values, with the sorts of their
		 * arguments.
		 */
		private final Set<String> evaluated = new HashSet<>();

		@Override
		public Term evaluable(
				String function,
				Sort result,
				List<Term> arguments,
				java.util.function.Function<List<Number>, Number> evaluator,
				SymbolicExpression origin) {
			List<Sort> sorts = new ArrayList<>();
			for (Term arg : arguments)
				sorts.add(arg.getSort());
			evaluated.add(function + sorts);
			return Term.app(function(function, sorts, result), result, arguments.toArray(Term[]::new));
		}

		// the unknown functions are keyed by their names and sorts, as in the
		// verification (see Unrolling), so that two operations whose
		// functions have the same name share them
		@Override
		public Term apply(
				String function,
				Sort result,
				List<Term> arguments,
				SymbolicExpression origin,
				Term guard) {
			guards.add(guard);
			return Term.app(function(function, sortsOf(arguments), result), result, arguments.toArray(Term[]::new));
		}

		@Override
		public Term unknown(
				String function,
				Sort result,
				List<Term> arguments,
				SymbolicExpression origin) {
			havoc = true;
			return Term.app(function(function, sortsOf(arguments), result), result, arguments.toArray(Term[]::new));
		}

		// the constants of the identifiers, the same for the same identifier
		private final java.util.Map<String, Term> constants = new java.util.HashMap<>();

		@Override
		public Term constant(
				Identifier id,
				Sort sort) {
			return constants.computeIfAbsent(id.getName() + ":" + sort,
					k -> solver.declare(id.getName() + "#" + (counter++), sort));
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

	// the functions declared to the solver, by name and sorts
	private static final java.util.Map<String, String> FUNCTIONS = new java.util.HashMap<>();

	private static String function(
			String name,
			List<Sort> arguments,
			Sort result) {
		return FUNCTIONS.computeIfAbsent(name + arguments + result,
				k -> solver.declareFunction(name + "#" + FUNCTIONS.size(), arguments, result));
	}

	private static List<Sort> sortsOf(
			List<Term> terms) {
		List<Sort> sorts = new ArrayList<>();
		for (Term t : terms)
			sorts.add(t.getSort());
		return sorts;
	}

	/**
	 * Checks the encoding of an expression whose value is known: its
	 * requirements alone must be satisfiable (a requirement must never exclude
	 * the execution, also when the encoding is not precise); then, the
	 * encoding is precise if its guards hold too, and its value must be the
	 * expected one.
	 *
	 * @return whether the encoding is precise
	 */
	private static boolean precise(
			Symbols symbols,
			Term term,
			Term expected,
			String what) {
		if (symbols.havoc)
			return false;
		solver.push();
		try {
			for (Term r : symbols.requirements)
				solver.assertTerm(r);
			assertEquals(SmtResult.SAT, solver.check(), what + ": the requirements exclude the execution");
			for (Term g : symbols.guards)
				solver.assertTerm(g);
			if (solver.check() != SmtResult.SAT)
				return false;
			solver.assertTerm(Term.not(Term.eq(term, expected)));
			assertEquals(SmtResult.UNSAT, solver.check(), what + " should be " + expected);
			return true;
		} finally {
			solver.pop();
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
		return precise(symbols, term, expected(value), f + " on " + java.util.Arrays.toString(args));
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

	/**
	 * The representations in a radix that is not a literal: the one of the
	 * radix is selected (radix 10 for invalid radixes), and the constraints of
	 * the others do not exclude it.
	 */
	@Test
	public void conversionsWithARadixThatIsNotALiteral() throws Exception {
		for (int[] c : new int[][] { { 255, 16 }, { 255, 2 }, { -255, 36 }, { 255, 10 }, { 255, 37 }, { 0, 8 } })
			for (JavaLibraryFunction f : List.of(JavaLibraryFunction.INTEGER_TO_STRING_RADIX,
					JavaLibraryFunction.INTEGER_TO_UNSIGNED_STRING_RADIX)) {
				Symbols symbols = new Symbols();
				Variable x = new Variable(JavaIntType.INSTANCE, "x", LOC);
				Variable r = new Variable(JavaIntType.INSTANCE, "r", LOC);
				java.util.Map<it.unive.lisa.symbolic.value.Identifier, java.util.Set<Type>> types = new HashMap<>();
				types.put(x, java.util.Set.of(JavaIntType.INSTANCE));
				types.put(r, java.util.Set.of(JavaIntType.INSTANCE));
				JavaExpressionEncoder encoder = new JavaExpressionEncoder(new SortPolicy(types), symbols);
				Term term = new BinaryExpression(StringType.INSTANCE, x, r, f, LOC).accept(encoder);
				symbols.requirements.add(Term.eq(x.accept(encoder), Term.integer(c[0])));
				symbols.requirements.add(Term.eq(r.accept(encoder), Term.integer(c[1])));
				assertTrue(precise(symbols, term, expected(f.apply(c[0], c[1])), f + " on " + c[0] + ", " + c[1]),
						f + " not precise on " + c[0] + ", " + c[1]);
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
		List<Integer> chars = List.of((int) 'a', (int) 'b', (int) 'c', (int) 'o', (int) 'z', 0xE9, 0x1F600, -1,
				Character.MAX_CODE_POINT + 1, Integer.MIN_VALUE, Integer.MAX_VALUE);
		for (String s : strings) {
			assertTrue(check(JavaLibraryFunction.STRING_IS_EMPTY, s), "isEmpty not precise on " + s);
			for (Integer c : chars) {
				boolean bmp = c >= 0 && c <= Character.MAX_VALUE;
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
		Term exp = value instanceof Double ? FloatingPoints.literal((Double) value)
				: value instanceof Float ? FloatingPoints.literal((Float) value) : expected(value);
		assertTrue(precise(symbols, term, exp, what), what + " is not precise");
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

	/**
	 * The length of a string is exact only for the strings of UTF-16 code
	 * units: a character above 0xFFFF has length 1 in SMT, 2 in Java.
	 */
	@Test
	public void lengthIsExactOnlyForUtf16Strings() throws SemanticException {
		checkOperator(JavaStringLengthOperator.INSTANCE, JavaIntType.INSTANCE, () -> "abc".length(), "abc");
		Symbols symbols = new Symbols();
		JavaExpressionEncoder encoder = new JavaExpressionEncoder(new SortPolicy(new HashMap<>()), symbols);
		Variable s = new Variable(StringType.INSTANCE, "s" + counter++, LOC);
		new UnaryExpression(JavaIntType.INSTANCE, s, JavaStringLengthOperator.INSTANCE, LOC).accept(encoder);
		Term string = s.accept(encoder);
		solver.push();
		try {
			for (Term r : symbols.requirements)
				solver.assertTerm(r);
			for (Term g : symbols.guards)
				solver.assertTerm(g);
			// a string with a character above 0xFFFF
			Term emoji = Term.value("\"\\u{1f600}\"", Sort.STRING);
			solver.assertTerm(Term.eq(string, string.getSort().equals(Sort.STRING) ? emoji : ValueSort.wrap(emoji)));
			assertEquals(SmtResult.UNSAT, solver.check(), "the length of a non-UTF-16 string should not be exact");
		} finally {
			solver.pop();
		}
	}

	/**
	 * Math.floor and Math.sqrt have only double overloads: a float argument is
	 * widened, and the result is a double.
	 */
	@Test
	public void floorAndSqrtOfFloatsAreDoubles() throws SemanticException {
		for (float f : new float[] { 2.0f, 0.1f, -3.7f, 1e-3f }) {
			checkOperator(JavaMathSqrtOperator.INSTANCE, JavaDoubleType.INSTANCE, () -> Math.sqrt(f), f);
			checkOperator(JavaMathFloorOperator.INSTANCE, JavaDoubleType.INSTANCE, () -> Math.floor(f), f);
		}
	}

	/**
	 * Math.toRadians is exact (a product rounded to nearest, since JDK 9), and
	 * indexOf(char, from) is encoded as String.indexOf(int, int).
	 */
	@Test
	public void toRadiansAndIndexOfCharFrom() throws SemanticException {
		for (double d : new double[] { 0.0, -0.0, 1.0, 90.0, 180.0, -45.5, 1e300, Double.NaN,
				Double.POSITIVE_INFINITY })
			checkOperator(JavaMathToRadiansOperator.INSTANCE, JavaDoubleType.INSTANCE, () -> Math.toRadians(d), d);
		for (String s : List.of("", "abcabc", "xyz"))
			for (int from = -1; from <= s.length() + 1; from++) {
				int k = from;
				checkOperator(JavaStringIndexOfCharFromIndexOperator.INSTANCE, JavaIntType.INSTANCE,
						() -> s.indexOf('c', k), s, 'c', k);
			}
	}

	/**
	 * The operators of the frontend modeling methods of the library that are
	 * also functions of the library share their encodings with them.
	 */
	@Test
	public void frontendOperators() throws Exception {
		Type dbl = JavaDoubleType.INSTANCE;
		Type integer = JavaIntType.INSTANCE;
		Type bool = JavaBooleanType.INSTANCE;
		Type str = StringType.INSTANCE;
		List<Double> doubles = List.of(0.0, -0.0, 1.5, -2.5, 4.0, Double.NaN, Double.POSITIVE_INFINITY,
				Double.NEGATIVE_INFINITY);
		for (double a : doubles) {
			checkOperator(JavaStrictMathAbsOperator.INSTANCE, dbl, () -> StrictMath.abs(a), a);
			checkOperator(JavaStrictMathFloorOperator.INSTANCE, dbl, () -> StrictMath.floor(a), a);
			checkOperator(JavaStrictMathSqrtOperator.INSTANCE, dbl, () -> StrictMath.sqrt(a), a);
			for (double b : doubles) {
				// NaN if either is NaN, and -0.0 < 0.0
				checkOperator(JavaMathMax.INSTANCE, dbl, () -> Math.max(a, b), a, b);
				checkOperator(JavaMathMin.INSTANCE, dbl, () -> Math.min(a, b), a, b);
				checkOperator(JavaStrictMathMax.INSTANCE, dbl, () -> StrictMath.max(a, b), a, b);
				checkOperator(JavaStrictMathMin.INSTANCE, dbl, () -> StrictMath.min(a, b), a, b);
				checkOperator(JavaDoubleCompareOperator.INSTANCE, integer, () -> Double.compare(a, b), a, b);
			}
		}
		for (char c : new char[] { 'a', 'Z', '5', ' ', 0xE9, 0x0660, 0xFFFF }) {
			checkOperator(JavaCharacterIsDigitOperator.INSTANCE, bool, () -> Character.isDigit(c), c);
			checkOperator(JavaCharacterIsLetterOperator.INSTANCE, bool, () -> Character.isLetter(c), c);
			checkOperator(JavaCharacterToUpperCaseOperator.INSTANCE, JavaCharType.INSTANCE,
					() -> Character.toUpperCase(c), c);
			for (int radix : new int[] { -1, 2, 10, 16, 36, 37 })
				checkOperator(JavaCharacterDigitOperator.INSTANCE, integer, () -> Character.digit(c, radix), c, radix);
		}
		for (int a : new int[] { 0, 1, -1, 127, -128, 32767, -32768, Integer.MAX_VALUE, Integer.MIN_VALUE })
			for (int b : new int[] { 0, -1, 127, -32768, Integer.MIN_VALUE }) {
				checkOperator(JavaIntegerCompareOperator.INSTANCE, integer, () -> Integer.compare(a, b), a, b);
				checkOperator(JavaShortCompareOperator.INSTANCE, integer,
						() -> Short.compare((short) a, (short) b), (int) (short) a, (int) (short) b);
				checkOperator(JavaByteCompareOperator.INSTANCE, integer, () -> Byte.compare((byte) a, (byte) b),
						(int) (byte) a, (int) (byte) b);
				checkOperator(JavaLongCompareOperator.INSTANCE, integer, () -> Long.compare(a, (long) b << 32),
						(long) a, (long) b << 32);
			}
		List<String> strings = List.of("", "a", "abc", "abd", "ab", "B");
		for (String a : strings)
			for (String b : strings)
				checkOperator(JavaStringCompareToOperator.INSTANCE, integer, () -> a.compareTo(b), a, b);
		// the values whose representation is encoded: special values and
		// integral numbers below 10^7
		for (double d : new double[] { 0.0, 1.0, -42.0, 1234567.0, Double.NaN, Double.NEGATIVE_INFINITY }) {
			float f = (float) d;
			checkOperator(JavaStringValueOfDoubleOperator.INSTANCE, str, () -> String.valueOf(d), d);
			checkOperator(JavaStringValueOfFloatOperator.INSTANCE, str, () -> String.valueOf(f), f);
			checkOperator(JavaStringAppendDoubleOperator.INSTANCE, str, () -> "x" + d, "x", d);
			checkOperator(JavaStringAppendFloatOperator.INSTANCE, str, () -> "x" + f, "x", f);
			checkOperator(JavaStringInsertDoubleOperator.INSTANCE, str,
					() -> new StringBuilder("ab").insert(1, d).toString(), "ab", 1, d);
			checkOperator(JavaStringInsertFloatOperator.INSTANCE, str,
					() -> new StringBuilder("ab").insert(1, f).toString(), "ab", 1, f);
		}
	}

	/**
	 * Different functions computed on concrete values are different unknown
	 * functions (their names do not collide), also the ones of Math and
	 * StrictMath with the same name.
	 */
	@Test
	public void functionsDoNotCollide() throws SemanticException {
		Symbols symbols = new Symbols();
		JavaExpressionEncoder encoder = new JavaExpressionEncoder(new SortPolicy(new HashMap<>()), symbols);
		Constant x = constant(0.5);
		Term sin = new UnaryExpression(JavaDoubleType.INSTANCE, x, JavaMathSinOperator.INSTANCE, LOC).accept(encoder);
		Term cos = new UnaryExpression(JavaDoubleType.INSTANCE, x, JavaMathCosOperator.INSTANCE, LOC).accept(encoder);
		Term strictSin = new UnaryExpression(JavaDoubleType.INSTANCE, x, JavaStrictMathSinOperator.INSTANCE, LOC)
				.accept(encoder);
		Term again = new UnaryExpression(JavaDoubleType.INSTANCE, x, JavaMathSinOperator.INSTANCE, LOC)
				.accept(encoder);
		solver.push();
		try {
			for (Term r : symbols.requirements)
				solver.assertTerm(r);
			// the same function on the same argument is the same value...
			solver.assertTerm(Term.not(Term.eq(sin, again)));
			assertEquals(SmtResult.UNSAT, solver.check(), "Math.sin is not a function");
		} finally {
			solver.pop();
		}
		for (Term other : List.of(cos, strictSin)) {
			solver.push();
			try {
				for (Term r : symbols.requirements)
					solver.assertTerm(r);
				// ... while different functions are not forced to be equal
				solver.assertTerm(Term.not(Term.eq(sin, other)));
				assertEquals(SmtResult.SAT, solver.check(), "Math.sin collides with " + other);
			} finally {
				solver.pop();
			}
		}
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
			assertTrue(precise(symbols, term, expected(c[2]), "(" + target + ") " + c[1]),
					"(" + target + ") " + c[1] + " is not precise");
		}
	}

	/**
	 * A symbol table recording the unknown functions applied, with their
	 * sorts.
	 */
	private static class FunctionSymbols
			extends
			Symbols {

		private final Set<String> applied = new HashSet<>();

		@Override
		public Term apply(
				String function,
				Sort result,
				List<Term> arguments,
				SymbolicExpression origin,
				Term guard) {
			List<Sort> sorts = new ArrayList<>();
			for (Term arg : arguments)
				sorts.add(arg.getSort());
			applied.add(function + sorts + result);
			return super.apply(function, result, arguments, origin, guard);
		}
	}

	@Test
	public void libraryFunctionsAreDifferentFunctions() throws SemanticException {
		// outside the code points, the functions of the library are unknown
		// functions, identified by their name and sorts: they must not be
		// forced to be equal (e.g., Character.getType(-1) is 0, while
		// Character.toLowerCase(-1) is -1)
		Variable x = new Variable(JavaIntType.INSTANCE, "x", LOC);
		Set<String> names = new HashSet<>();
		for (JavaLibraryFunction f : List.of(JavaLibraryFunction.CHARACTER_GET_TYPE,
				JavaLibraryFunction.CHARACTER_TO_LOWER_CASE)) {
			FunctionSymbols symbols = new FunctionSymbols();
			JavaExpressionEncoder encoder = new JavaExpressionEncoder(new SortPolicy(new HashMap<>()), symbols);
			new UnaryExpression(JavaIntType.INSTANCE, x, f, LOC).accept(encoder);
			assertEquals(1, symbols.applied.size(), f + " applies " + symbols.applied);
			names.addAll(symbols.applied);
		}
		assertEquals(2, names.size(), "getType and toLowerCase share " + names);
	}

	@Test
	public void mathAndStrictMathAreDifferentFunctions() throws SemanticException {
		// functions are identified by their name and sorts, and the functions
		// of Math and StrictMath can differ
		Object[][] pairs = {
				{ JavaMathSinOperator.INSTANCE, JavaStrictMathSinOperator.INSTANCE },
				{ JavaMathExpOperator.INSTANCE, JavaStrictMathExpOperator.INSTANCE } };
		for (Object[] pair : pairs) {
			Set<String> names = new HashSet<>();
			for (Object op : pair) {
				Symbols symbols = new Symbols();
				JavaExpressionEncoder encoder = new JavaExpressionEncoder(new SortPolicy(new HashMap<>()), symbols);
				new UnaryExpression(JavaDoubleType.INSTANCE, constant(0.5d), (UnaryOperator) op, LOC).accept(encoder);
				assertEquals(1, symbols.evaluated.size(), op + " computes " + symbols.evaluated);
				names.addAll(symbols.evaluated);
			}
			assertEquals(2, names.size(), pair[0] + " and " + pair[1] + " share " + names);
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

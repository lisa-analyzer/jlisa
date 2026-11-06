package it.unive.jlisa.triage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import it.unive.jlisa.program.operator.JavaLibraryFunction;
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
import it.unive.lisa.symbolic.value.operator.binary.TypeConv;
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
import java.util.List;
import java.util.Random;
import java.util.Set;
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

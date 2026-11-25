package it.unive.jlisa.triage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import it.unive.jlisa.Main;
import it.unive.jlisa.svcomp.SVCOMPProperty;
import it.unive.lisa.outputs.json.JsonReport;
import it.unive.lisa.triage.smt.SmtLibProcessSolver;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * End-to-end tests of the verification of the warnings of SV-COMP tasks: each
 * program of the tests is analyzed for a property, and the verdict of its
 * warnings is checked.
 */
public class TriageVerdictsTest {

	/**
	 * The folder of the programs of the tests.
	 */
	private static final Path TASKS = Paths.get("java-testcases", "triage");

	/**
	 * The common files of the SV-COMP tasks of the tests, with the
	 * {@code Verifier} of SV-COMP.
	 */
	private static final Path COMMON = Paths.get("java-testcases", "svcomp", "common");

	/**
	 * The verdict of the warnings of a property.
	 */
	private enum Verdict {
		/**
		 * Some warning is confirmed.
		 */
		DEFINITE,
		/**
		 * No warning is confirmed, and some is not refuted.
		 */
		POSSIBLE,
		/**
		 * All warnings are refuted.
		 */
		REFUTED,
		/**
		 * There are no warnings: the analysis alone proves the property.
		 */
		NONE
	}

	/**
	 * Asserts that the property is proved, by the analysis (no warning) or by
	 * the verification of the warnings (all refuted). The tests whose point is
	 * the verification of the warnings assert {@link Verdict#REFUTED} instead,
	 * so that a regression dropping the warnings is noticed.
	 */
	private static void assertSafe(
			Verdict verdict,
			String program) {
		assertTrue(verdict == Verdict.REFUTED || verdict == Verdict.NONE, program + " is " + verdict);
	}

	@BeforeAll
	public static void solver() {
		assumeTrue(SmtLibProcessSolver.isZ3Available(), "z3 is not available");
	}

	/**
	 * Runs jlisa on a program of the tests for a property, and yields the
	 * verdict of its warnings.
	 */
	private static Verdict verdict(
			String program,
			SVCOMPProperty property,
			Path outdir)
			throws Exception {
		List<JsonReport.JsonMessage> warnings = warnings(program, property, outdir);
		// no warning is not a refutation (e.g., if a regression drops them)
		if (warnings.isEmpty())
			return Verdict.NONE;
		if (warnings.stream().anyMatch(JsonReport.JsonMessage::isDefinite))
			return Verdict.DEFINITE;
		if (warnings.stream().allMatch(JsonReport.JsonMessage::isRefuted))
			return Verdict.REFUTED;
		return Verdict.POSSIBLE;
	}

	private static List<JsonReport.JsonMessage> warnings(
			String program,
			SVCOMPProperty property,
			Path outdir,
			String... options)
			throws Exception {
		List<String> args = new ArrayList<>(List.of(
				"check",
				"-s", COMMON.toString(), TASKS.resolve(program).toString(),
				"-o", outdir.toString(),
				"-n", "ConstantPropagation",
				"-m", "Statistics",
				"-c", "Assert",
				"-t", "Combined",
				"-l", "ERROR",
				"--svcomp-property", TASKS.resolve(property.getName() + ".prp").toString()));
		args.addAll(List.of(options));
		Main.run(args.toArray(String[]::new));
		JsonReport report;
		try (Reader reader = Files.newBufferedReader(outdir.resolve("report.json"))) {
			report = JsonReport.read(reader);
		}
		return List.copyOf(report.getWarnings());
	}

	/**
	 * Fields of primitive types hold values of their types: their contents are
	 * encoded precisely, and a violation depending on them is confirmed.
	 */
	@Test
	public void violationsOnPrimitiveFieldsAreConfirmed(
			@TempDir Path dir)
			throws Exception {
		assertEquals(Verdict.DEFINITE, verdict("typed-fields", SVCOMPProperty.ASSERT, dir));
	}

	/**
	 * The values of boxed numbers are fields of primitive types.
	 */
	@Test
	public void violationsOnBoxedValuesAreConfirmed(
			@TempDir Path dir)
			throws Exception {
		assertEquals(Verdict.DEFINITE, verdict("boxed-values", SVCOMPProperty.ASSERT, dir));
	}

	/**
	 * A Character equals only a Character with the same value: not an Integer
	 * with the same code...
	 */
	@Test
	public void characterEqualsOtherClassesIsRefuted(
			@TempDir Path dir)
			throws Exception {
		assertSafe(verdict("character-equals-other", SVCOMPProperty.ASSERT, dir), "character-equals-other");
	}

	/**
	 * ... while it equals a Character with the same value.
	 */
	@Test
	public void characterEqualsSameCharacterIsConfirmed(
			@TempDir Path dir)
			throws Exception {
		assertEquals(Verdict.DEFINITE, verdict("character-equals-same", SVCOMPProperty.ASSERT, dir));
	}

	/**
	 * A String equals only a String with the same contents: not an Integer.
	 */
	@Test
	public void stringEqualsOtherClassesIsRefuted(
			@TempDir Path dir)
			throws Exception {
		assertSafe(verdict("string-equals-other", SVCOMPProperty.ASSERT, dir), "string-equals-other");
	}

	/**
	 * Runtime exceptions of the language are confirmed: an index out of the
	 * bounds of an array, and an integer division by zero.
	 */
	@Test
	public void languageRuntimeExceptionsAreConfirmed(
			@TempDir Path dir)
			throws Exception {
		for (String program : List.of("aioobe-array", "division-by-zero"))
			assertEquals(Verdict.DEFINITE, verdict(program, SVCOMPProperty.RUNTIME_EXCEPTION, dir.resolve(program)),
					program);
	}

	/**
	 * The elements of arrays are visited by for-each loops, and getChars writes
	 * the cells of its destination: violations are confirmed, and assertions
	 * that hold are refuted.
	 */
	@Test
	public void arrayElementsAreVisitedAndWritten(
			@TempDir Path dir)
			throws Exception {
		// the violation in the loop is found, but not confirmed (a limit of
		// the verification of loops)
		assertEquals(Verdict.POSSIBLE, verdict("foreach-array", SVCOMPProperty.ASSERT, dir.resolve("a")));
		// a spurious violation is found (not confirmed): the sum computed by
		// the loop is not refuted (see the pending bug on the exits of
		// for-each loops over arrays)
		assertEquals(Verdict.POSSIBLE, verdict("foreach-array-holds", SVCOMPProperty.ASSERT, dir.resolve("b")));
		assertSafe(verdict("getchars-cells", SVCOMPProperty.ASSERT, dir.resolve("c")), "getchars-cells");
	}

	/**
	 * A static field inherited from an ancestor and written by a summarized
	 * callee is unknown after the call.
	 */
	@Test
	public void inheritedStaticFieldsWrittenByCalleesAreUnknown(
			@TempDir Path dir)
			throws Exception {
		assertNotEquals(Verdict.REFUTED, verdict("static-inherited-written", SVCOMPProperty.ASSERT, dir));
	}

	/**
	 * The default methods of interfaces are called on the objects of the
	 * classes implementing them.
	 */
	@Test
	public void defaultMethodsAreCalled(
			@TempDir Path dir)
			throws Exception {
		assertEquals(Verdict.DEFINITE, verdict("default-methods", SVCOMPProperty.ASSERT, dir.resolve("a")));
		assertSafe(verdict("default-methods-holds", SVCOMPProperty.ASSERT, dir.resolve("b")), "default-methods-holds");
	}

	/**
	 * Runtime exceptions whose counterexamples rely on imprecise values are
	 * certified by execution too, also within a time limit (that leaves time
	 * to the certification).
	 */
	@Test
	public void runtimeExceptionsAreCertifiedByExecution(
			@TempDir Path dir)
			throws Exception {
		List<JsonReport.JsonMessage> warnings = warnings("certified-exception", SVCOMPProperty.RUNTIME_EXCEPTION,
				dir.resolve("a"), "--certification");
		assertTrue(warnings.stream().anyMatch(w -> w.isDefinite() && w.getExplanation() != null
				&& w.getExplanation().contains(", certified by execution")), warnings.toString());
		// the limit counts from the start of the virtual machine, that runs
		// all the tests: it is large enough not to be reached
		warnings = warnings("certified", SVCOMPProperty.ASSERT, dir.resolve("b"), "--certification",
				"--time-limit", "3600");
		assertTrue(warnings.stream().anyMatch(w -> w.isDefinite() && w.getExplanation() != null
				&& w.getExplanation().contains(", certified by execution")), warnings.toString());
	}

	/**
	 * Assertions on fields that hold are refuted.
	 */
	@Test
	public void assertionsOnFieldsThatHoldAreRefuted(
			@TempDir Path dir)
			throws Exception {
		assertSafe(verdict("field-increment", SVCOMPProperty.ASSERT, dir), "field-increment");
	}

	/**
	 * The runtime types of the objects allocated by the program are known, so
	 * that tests of the types of the objects are precise: a violation depending
	 * on the type of an object is confirmed...
	 */
	@Test
	public void violationsOnRuntimeTypesAreConfirmed(
			@TempDir Path dir)
			throws Exception {
		assertEquals(Verdict.DEFINITE, verdict("instanceof-confirmed", SVCOMPProperty.ASSERT, dir));
	}

	/**
	 * ... and one that cannot happen because of the type of an object is
	 * refuted.
	 */
	@Test
	public void violationsPreventedByRuntimeTypesAreRefuted(
			@TempDir Path dir)
			throws Exception {
		assertEquals(Verdict.REFUTED, verdict("instanceof-refuted", SVCOMPProperty.ASSERT, dir));
	}

	/**
	 * The results of recursive calls satisfy the summaries of their callees,
	 * proved by induction on the depth of the recursion.
	 */
	@Test
	public void assertionsOnRecursiveResultsAreRefuted(
			@TempDir Path dir)
			throws Exception {
		assertEquals(Verdict.REFUTED, verdict("recursive-sum", SVCOMPProperty.ASSERT, dir));
	}

	/**
	 * The relational comparisons of Java compare the values wrapped by boxed
	 * operands.
	 */
	@Test
	public void violationsOnBoxedComparisonsAreConfirmed(
			@TempDir Path dir)
			throws Exception {
		assertEquals(Verdict.DEFINITE, verdict("boxed-comparisons", SVCOMPProperty.ASSERT, dir));
	}

	/**
	 * Boxed booleans used as conditions are unboxed.
	 */
	@Test
	public void violationsOnBoxedConditionsAreConfirmed(
			@TempDir Path dir)
			throws Exception {
		assertEquals(Verdict.DEFINITE, verdict("boxed-boolean", SVCOMPProperty.ASSERT, dir));
	}

	/**
	 * The contents of strings, held by their {@code value} field, are encoded
	 * as strings.
	 */
	@Test
	public void violationsOnStringContentsAreConfirmed(
			@TempDir Path dir)
			throws Exception {
		assertEquals(Verdict.DEFINITE, verdict("string-contents", SVCOMPProperty.ASSERT, dir));
	}

	/**
	 * Recursions with several recursive calls entered with exact values are
	 * unrolled only up to a bound, and solved afterwards: unrolling fib(25)
	 * call by call would take hundreds of thousands of calls.
	 */
	@Test
	public void recursionsWithExactValuesAreUnrolledUpToABound(
			@TempDir Path dir) {
		assertNotEquals(Verdict.DEFINITE, assertTimeoutPreemptively(Duration.ofMinutes(3),
				() -> verdict("recursive-unrolling", SVCOMPProperty.ASSERT, dir)));
	}

	/**
	 * The conversion to string of objects that are not boxed primitive values
	 * does not prevent the analysis of the rest of the program (the violation
	 * is not refuted, but not confirmed either, as the string of a generic
	 * object is not modeled)...
	 */
	@Test
	public void valueOfObjectsIsAnalyzed(
			@TempDir Path dir)
			throws Exception {
		assertNotEquals(Verdict.REFUTED, verdict("valueof-object", SVCOMPProperty.ASSERT, dir));
	}

	/**
	 * ... and neither does the one of values that might be null.
	 */
	@Test
	public void valueOfPossiblyNullValuesIsAnalyzed(
			@TempDir Path dir)
			throws Exception {
		assertEquals(Verdict.DEFINITE, verdict("valueof-null", SVCOMPProperty.ASSERT, dir));
	}

	/**
	 * Casting null never fails: a cast that can only see null or instances of
	 * the type does not raise exceptions.
	 */
	@Test
	public void castsOfNullDoNotFail(
			@TempDir Path dir)
			throws Exception {
		assertEquals(Verdict.REFUTED, verdict("cast-null", SVCOMPProperty.RUNTIME_EXCEPTION, dir));
	}

	/**
	 * Strings are sequences of UTF-16 code units: violations depending on their
	 * characters are confirmed with strings whose code points are each a single
	 * code unit (e.g., the result of {@code compareTo} is the difference of the
	 * first different code units).
	 */
	@Test
	public void violationsOnStringsAreConfirmedWithSingleCodeUnits(
			@TempDir Path dir)
			throws Exception {
		List<JsonReport.JsonMessage> warnings = warnings("string-compare", SVCOMPProperty.ASSERT, dir);
		JsonReport.JsonMessage definite = warnings.stream()
				.filter(JsonReport.JsonMessage::isDefinite)
				.findFirst()
				.orElseThrow();
		Pattern escape = Pattern.compile("\\\\u\\{([0-9a-fA-F]+)\\}");
		for (JsonReport.JsonReproducingInput input : definite.getReproducingInputs())
			if (input.getSource().contains("nondetString")) {
				Matcher m = escape.matcher(input.getValue());
				while (m.find()) {
					int code = Integer.parseInt(m.group(1), 16);
					assertTrue(code < 0xD800 || code >= 0xE000 && code <= 0xFFFF,
							"not a single code unit: " + input.getValue());
				}
			}
	}

	/**
	 * The representations of integers in radixes that are powers of two are
	 * encoded so that violations depending on them are confirmed.
	 */
	@Test
	public void violationsOnRadixConversionsAreConfirmed(
			@TempDir Path dir)
			throws Exception {
		assertEquals(Verdict.DEFINITE, verdict("radix-conversion", SVCOMPProperty.ASSERT, dir));
	}

	/**
	 * The functions of {@link Math} whose results are not specified exactly are
	 * computed on the values of the counterexamples, so that violations
	 * depending on them are confirmed.
	 */
	@Test
	public void violationsOnMathFunctionsAreConfirmed(
			@TempDir Path dir)
			throws Exception {
		assertEquals(Verdict.DEFINITE, verdict("math-functions", SVCOMPProperty.ASSERT, dir));
	}

	/**
	 * Monotone functions are NaN outside of their domain: the values learned
	 * for them do not exclude the arguments where they are NaN, nor the ones
	 * where they are numbers, so that violations are never refuted because of
	 * them.
	 */
	@Test
	public void violationsOnMathFunctionsWithNaNsAreNotRefuted(
			@TempDir Path dir)
			throws Exception {
		assertNotEquals(Verdict.REFUTED, verdict("math-nan", SVCOMPProperty.ASSERT, dir));
	}

	/**
	 * The ranges of the functions of {@link Math} whose results are not
	 * specified exactly are known, so that violations outside of them are
	 * refuted.
	 */
	@Test
	public void violationsOutOfTheRangesOfMathFunctionsAreRefuted(
			@TempDir Path dir)
			throws Exception {
		assertEquals(Verdict.REFUTED, verdict("math-ranges", SVCOMPProperty.ASSERT, dir));
	}

	/**
	 * Counterexamples relying on values that are not encoded precisely are
	 * confirmed if their witnesses are validated by executing the program.
	 */
	@Test
	public void impreciseCounterexamplesAreCertifiedByExecution(
			@TempDir Path dir)
			throws Exception {
		List<JsonReport.JsonMessage> warnings = warnings("certified", SVCOMPProperty.ASSERT, dir, "--certification");
		assertTrue(warnings.stream().anyMatch(w -> w.isDefinite() && w.getExplanation() != null
				&& w.getExplanation().contains(", certified by execution")), warnings.toString());
	}

	/**
	 * Without certification (the default), counterexamples relying on values
	 * that are not encoded precisely stay possible.
	 */
	@Test
	public void impreciseCounterexamplesAreNotCertifiedByDefault(
			@TempDir Path dir)
			throws Exception {
		List<JsonReport.JsonMessage> warnings = warnings("certified", SVCOMPProperty.ASSERT, dir);
		assertTrue(warnings.stream().noneMatch(JsonReport.JsonMessage::isDefinite), warnings.toString());
	}

	/**
	 * Counterexamples relying on values that are not encoded precisely, whose
	 * witnesses are not validated by executing the program, stay possible.
	 */
	@Test
	public void impreciseCounterexamplesThatAreNotReproducedStayPossible(
			@TempDir Path dir)
			throws Exception {
		List<JsonReport.JsonMessage> warnings = warnings("certified-spurious", SVCOMPProperty.ASSERT, dir,
				"--certification");
		assertTrue(warnings.stream().noneMatch(JsonReport.JsonMessage::isDefinite), warnings.toString());
	}

	/**
	 * Values of generic collections, whose models yield objects, are unboxed
	 * when compared with primitive values.
	 */
	@Test
	public void comparisonsOfObjectsWithPrimitivesAreAnalyzed(
			@TempDir Path dir)
			throws Exception {
		assertNotEquals(Verdict.REFUTED, verdict("object-comparison", SVCOMPProperty.ASSERT, dir));
	}

	/**
	 * The calls of the right operand of a short-circuiting operator that are
	 * not evaluated produce no reproducing input: violations reached where the
	 * left operand decides the result are confirmed with the inputs of the
	 * calls that are executed only (here, the one of line 5, but not the one of
	 * line 6).
	 */
	@Test
	public void callsNotEvaluatedByShortCircuitingOperatorsProduceNoInputs(
			@TempDir Path dir)
			throws Exception {
		List<JsonReport.JsonMessage> warnings = warnings("short-circuit", SVCOMPProperty.ASSERT, dir);
		JsonReport.JsonMessage definite = warnings.stream()
				.filter(JsonReport.JsonMessage::isDefinite)
				.findFirst()
				.orElseThrow();
		assertTrue(definite.getReproducingInputs().stream().anyMatch(i -> i.getLocation().contains("Main.java':5:")));
		assertTrue(definite.getReproducingInputs().stream().noneMatch(i -> i.getLocation().contains("Main.java':6:")),
				"input of a call that is not evaluated");
	}

	/**
	 * The calls that the models do not expand (e.g., recursive ones) keep the
	 * fields that their callees cannot write: an assertion on a field that no
	 * callee writes is refuted, also when the field is read before the call in
	 * the same condition...
	 */
	@Test
	public void assertionsOnFieldsNotWrittenByCalleesAreRefuted(
			@TempDir Path dir)
			throws Exception {
		assertEquals(Verdict.REFUTED, verdict("heap-unwritten-by-recursion", SVCOMPProperty.ASSERT, dir));
		assertEquals(Verdict.REFUTED, verdict("heap-unwritten-fields", SVCOMPProperty.ASSERT, dir));
	}

	/**
	 * The fields that natives write (e.g., the value of an atomic integer) are
	 * written also when a class of the program has a field with the same name.
	 */
	@Test
	public void fieldsWrittenByNativesAreUnknownAfterTheCalls(
			@TempDir Path dir)
			throws Exception {
		assertNotEquals(Verdict.REFUTED, verdict("heap-native-fields", SVCOMPProperty.ASSERT, dir));
	}

	/**
	 * A primitive passed to a parameter of a supertype of its wrapper class is
	 * boxed (c.equals('A') reaches Character.equals(Object)).
	 */
	@Test
	public void primitivesAreBoxedAndWidened(
			@TempDir Path dir)
			throws Exception {
		assertSafe(verdict("char-equals", SVCOMPProperty.ASSERT, dir), "char-equals");
	}

	/**
	 * Integer.parseInt is encoded for ASCII strings: the values parsed from
	 * strings that are not constant, and the NumberFormatException of the
	 * strings that are not parsable, are confirmed.
	 */
	@Test
	public void parsedIntegersAreEncoded(
			@TempDir Path dir)
			throws Exception {
		assertEquals(Verdict.DEFINITE, verdict("parseint-nondet", SVCOMPProperty.ASSERT, dir.resolve("value")));
		assertEquals(Verdict.DEFINITE,
				verdict("parseint-nondet-nfe", SVCOMPProperty.RUNTIME_EXCEPTION, dir.resolve("nfe")));
	}

	/**
	 * The bounds of strings and string builders raise
	 * StringIndexOutOfBoundsException, also caught as an
	 * IndexOutOfBoundsException, and getChars checks the bounds of the receiver
	 * before reading the destination.
	 */
	@Test
	public void stringBoundsRaiseStringIndexOutOfBoundsException(
			@TempDir Path dir)
			throws Exception {
		assertSafe(verdict("string-index-caught", SVCOMPProperty.RUNTIME_EXCEPTION, dir), "string-index-caught");
	}

	/**
	 * A static initializer writing the static fields of another class writes
	 * them when its class is first used, also after they have been read.
	 */
	@Test
	public void staticFieldsWrittenByOtherInitializersAreUnknown(
			@TempDir Path dir)
			throws Exception {
		assertNotEquals(Verdict.REFUTED, verdict("static-initializer-writes", SVCOMPProperty.ASSERT, dir));
	}

	/**
	 * ... also through the methods they call.
	 */
	@Test
	public void staticFieldsWrittenByCalleesOfInitializersAreUnknown(
			@TempDir Path dir)
			throws Exception {
		assertNotEquals(Verdict.REFUTED, verdict("static-initializer-calls", SVCOMPProperty.ASSERT, dir));
	}

	/**
	 * ... while the fields that callees might write are unknown after the
	 * calls: written by their code, by the code called on their behalf (e.g.,
	 * the conversion of objects to strings), or reflectively.
	 */
	@Test
	public void fieldsWrittenByCalleesAreUnknownAfterTheCalls(
			@TempDir Path dir)
			throws Exception {
		assertNotEquals(Verdict.REFUTED, verdict("heap-written-by-recursion", SVCOMPProperty.ASSERT, dir));
		assertNotEquals(Verdict.REFUTED,
				verdict("heap-written-by-recursion-callback", SVCOMPProperty.ASSERT, dir));
		assertNotEquals(Verdict.REFUTED,
				verdict("heap-written-by-recursion-reflection", SVCOMPProperty.ASSERT, dir));
	}

	/**
	 * Null values raise NullPointerException where they are dereferenced:
	 * reading a cell of an array, and unboxing (assignments, conditions,
	 * comparisons).
	 */
	@Test
	public void nullValuesRaiseExceptions(
			@TempDir Path dir)
			throws Exception {
		for (String program : List.of("npe-array", "npe-unboxing", "npe-boolean-condition", "npe-comparison"))
			assertEquals(Verdict.DEFINITE, verdict(program, SVCOMPProperty.RUNTIME_EXCEPTION, dir.resolve(program)),
					program);
	}

	/**
	 * Exceptions are raised only in the executions raising them, and the
	 * others continue normally: the field of a reference that might be null
	 * is read only where it is not, a cast fails only where the value is not
	 * an instance of the type, and a null key is rejected only by the maps
	 * that do not accept it.
	 */
	@Test
	public void exceptionsAreRaisedOnlyWhereTheyHappen(
			@TempDir Path dir)
			throws Exception {
		for (String program : List.of("npe-field-normal-path", "cast-mixed", "null-key-mixed-maps"))
			assertEquals(Verdict.REFUTED, verdict(program, SVCOMPProperty.ASSERT, dir.resolve(program)), program);
		assertEquals(Verdict.DEFINITE, verdict("null-key-mixed-maps-reach", SVCOMPProperty.ASSERT, dir));
	}

	/**
	 * The bounds of strings, string builders and lists raise
	 * IndexOutOfBoundsException exactly when the indexes are out of them.
	 */
	@Test
	public void indexesOutOfBoundsRaiseExceptions(
			@TempDir Path dir)
			throws Exception {
		for (String program : List.of("substring-inverted", "bounds-getchars", "bounds-builder-delete",
				"bounds-builder-insert", "bounds-list-remove", "bounds-list-add", "bounds-list-set"))
			assertEquals(Verdict.DEFINITE, verdict(program, SVCOMPProperty.RUNTIME_EXCEPTION, dir.resolve(program)),
					program);
		for (String program : List.of("bounds-getchars-valid", "bounds-builder-valid"))
			assertSafe(verdict(program, SVCOMPProperty.RUNTIME_EXCEPTION, dir.resolve(program)), program);
		assertSafe(verdict("substring-full", SVCOMPProperty.RUNTIME_EXCEPTION, dir.resolve("substring-full")),
				"substring-full");
	}

	/**
	 * The iterators of collections without an order advance over their
	 * elements, and raise NoSuchElementException when there are no more.
	 */
	@Test
	public void iteratorsOfSetsAreExhausted(
			@TempDir Path dir)
			throws Exception {
		assertEquals(Verdict.DEFINITE, verdict("set-iterator-exhausted", SVCOMPProperty.RUNTIME_EXCEPTION, dir));
	}

	/**
	 * Integer.parseInt, System.arraycopy, Arrays.copyOf and String.split
	 * raise their exceptions, and compute their results (arraycopy writes the
	 * destination, also when it is the source).
	 */
	@Test
	public void libraryMethodsRaiseTheirExceptions(
			@TempDir Path dir)
			throws Exception {
		for (String program : List.of("parseint-invalid", "arraycopy-bounds", "copyof-negative",
				"split-invalid-regex"))
			assertEquals(Verdict.DEFINITE, verdict(program, SVCOMPProperty.RUNTIME_EXCEPTION, dir.resolve(program)),
					program);
		assertEquals(Verdict.DEFINITE, verdict("arraycopy-write", SVCOMPProperty.ASSERT, dir.resolve("write")));
		assertSafe(verdict("arraycopy-value", SVCOMPProperty.ASSERT, dir.resolve("arraycopy-value")), "arraycopy-value");
		assertSafe(verdict("arraycopy-overlap", SVCOMPProperty.ASSERT, dir.resolve("arraycopy-overlap")),
				"arraycopy-overlap");
		// a source of unknown type might overwrite the destination
		Verdict unknown = verdict("arraycopy-unknown-type", SVCOMPProperty.ASSERT, dir.resolve("unknown-type"));
		assertTrue(unknown == Verdict.DEFINITE || unknown == Verdict.POSSIBLE, "arraycopy-unknown-type is " + unknown);
		// arrays of different primitive types always raise ArrayStoreException
		assertSafe(verdict("arraycopy-store", SVCOMPProperty.ASSERT, dir.resolve("arraycopy-store")),
				"arraycopy-store");
		assertSafe(verdict("parseint-value", SVCOMPProperty.ASSERT, dir.resolve("parseint-value")), "parseint-value");
	}
}

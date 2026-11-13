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
		 * All warnings are refuted (or there are none).
		 */
		REFUTED
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
		Main.main(args.toArray(String[]::new));
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
	 * Assertions on fields that hold are refuted.
	 */
	@Test
	public void assertionsOnFieldsThatHoldAreRefuted(
			@TempDir Path dir)
			throws Exception {
		assertEquals(Verdict.REFUTED, verdict("field-increment", SVCOMPProperty.ASSERT, dir));
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
		List<JsonReport.JsonMessage> warnings = warnings("certified", SVCOMPProperty.ASSERT, dir);
		assertTrue(warnings.stream().anyMatch(w -> w.isDefinite() && w.getExplanation() != null
				&& w.getExplanation().contains(", certified by execution")), warnings.toString());
	}

	/**
	 * Without certification, counterexamples relying on values that are not
	 * encoded precisely stay possible.
	 */
	@Test
	public void impreciseCounterexamplesAreNotCertifiedIfDisabled(
			@TempDir Path dir)
			throws Exception {
		List<JsonReport.JsonMessage> warnings = warnings("certified", SVCOMPProperty.ASSERT, dir,
				"--no-certification");
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
		assertEquals(Verdict.POSSIBLE, verdict("certified-spurious", SVCOMPProperty.ASSERT, dir));
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
}

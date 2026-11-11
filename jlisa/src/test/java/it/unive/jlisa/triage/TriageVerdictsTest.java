package it.unive.jlisa.triage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
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
import java.util.List;
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
		Main.main(new String[] {
				"check",
				"-s", COMMON.toString(), TASKS.resolve(program).toString(),
				"-o", outdir.toString(),
				"-n", "ConstantPropagation",
				"-m", "Statistics",
				"-c", "Assert",
				"-t", "Combined",
				"-l", "ERROR",
				"--svcomp-property", TASKS.resolve(property.getName() + ".prp").toString() });
		JsonReport report;
		try (Reader reader = Files.newBufferedReader(outdir.resolve("report.json"))) {
			report = JsonReport.read(reader);
		}
		List<JsonReport.JsonMessage> warnings = List.copyOf(report.getWarnings());
		if (warnings.stream().anyMatch(JsonReport.JsonMessage::isDefinite))
			return Verdict.DEFINITE;
		if (warnings.stream().allMatch(JsonReport.JsonMessage::isRefuted))
			return Verdict.REFUTED;
		return Verdict.POSSIBLE;
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
}

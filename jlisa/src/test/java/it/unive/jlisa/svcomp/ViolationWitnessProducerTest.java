package it.unive.jlisa.svcomp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.jlisa.Main;
import it.unive.lisa.witness.v2.Location;
import it.unive.lisa.witness.v2.ViolationWitness;
import it.unive.lisa.witness.v2.WitnessReader;
import it.unive.lisa.witness.v2.metadata.Task;
import it.unive.lisa.witness.v2.violation.Segment;
import it.unive.lisa.witness.v2.violation.Waypoint;
import it.unive.lisa.witness.v2.violation.WaypointType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests of the violation witnesses that jlisa writes for SV-COMP tasks.
 *
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public class ViolationWitnessProducerTest {

	/**
	 * The folder of the tasks of the tests, in the layout of SV-COMP.
	 */
	private static final Path TASKS = Paths.get("java-testcases", "svcomp-witness");

	/**
	 * The common files of the SV-COMP tasks of the tests, with the
	 * {@code Verifier} of SV-COMP.
	 */
	private static final Path COMMON = Paths.get("java-testcases", "svcomp", "common");

	/**
	 * Runs jlisa on a program of the tests, together with the common files of
	 * the tasks, as SV-COMP gives them.
	 *
	 * @param program    the folder of the program
	 * @param outdir     the output directory
	 * @param properties the properties of the task
	 *
	 * @throws Exception if the analysis fails
	 */
	private static void run(
			String program,
			Path outdir,
			SVCOMPProperty... properties)
			throws Exception {
		List<String> args = new ArrayList<>(List.of(
				"-s", COMMON.toString(), TASKS.resolve(program).toString(),
				"-o", outdir.toString(),
				"-n", "ConstantPropagation",
				"--no-html"));
		for (SVCOMPProperty property : properties) {
			args.add("--svcomp-property");
			args.add(TASKS.resolve(property.getName() + ".prp").toString());
		}
		Main.main(args.toArray(new String[0]));
	}

	/**
	 * Runs jlisa on a program of the tests for a single property, and returns
	 * the file where the witness of the property is written, if there is one.
	 *
	 * @param program  the folder of the program
	 * @param property the property of the task
	 * @param outdir   the output directory
	 *
	 * @return the file of the witness
	 *
	 * @throws Exception if the analysis fails
	 */
	private static Path witness(
			String program,
			SVCOMPProperty property,
			Path outdir)
			throws Exception {
		run(program, outdir, property);
		return ViolationWitnessProducer.file(outdir, property);
	}

	/**
	 * Reads a witness, checks that it is made of a single target, and returns
	 * the location of the target.
	 *
	 * @param file the file of the witness
	 *
	 * @return the location of the target
	 *
	 * @throws Exception if the witness cannot be read
	 */
	private static Location target(
			Path file)
			throws Exception {
		ViolationWitness witness = (ViolationWitness) WitnessReader.read(file);
		List<Segment> segments = witness.getSequence().getSegments();
		assertEquals(1, segments.size());
		List<Waypoint> waypoints = segments.get(0).getWaypoints();
		assertEquals(1, waypoints.size());
		assertEquals(WaypointType.TARGET, waypoints.get(0).getType());
		return waypoints.get(0).getLocation();
	}

	/**
	 * Returns the path of the main file of a program of the tests, as written
	 * in the input files of its task.
	 *
	 * @param program the folder of the program
	 *
	 * @return the path
	 */
	private static String main(
			String program) {
		return TASKS.resolve(program).resolve("Main.java").toString();
	}

	/**
	 * Checks that an uncaught exception in a program of the tests gives a
	 * witness whose target is at the given position of its main file.
	 *
	 * @param program the folder of the program
	 * @param line    the line of the target
	 * @param column  the column of the target
	 * @param outdir  the output directory
	 *
	 * @throws Exception if the analysis fails
	 */
	private static void assertRuntimeExceptionTarget(
			String program,
			int line,
			int column,
			Path outdir)
			throws Exception {
		Path file = witness(program, SVCOMPProperty.RUNTIME_EXCEPTION, outdir);
		assertEquals(new Location(main(program), line, column), target(file));
	}

	/**
	 * Checks that an uncaught exception in a program of the tests is a definite
	 * violation, but gives no witness.
	 *
	 * @param program the folder of the program
	 * @param outdir  the output directory
	 *
	 * @throws Exception if the analysis fails
	 */
	private static void assertDefiniteExceptionWithoutWitness(
			String program,
			Path outdir)
			throws Exception {
		assertFalse(Files.exists(witness(program, SVCOMPProperty.RUNTIME_EXCEPTION, outdir)));
		assertTrue(Files.readString(outdir.resolve("report.json"))
				.contains("DEFINITE: uncaught runtime exception in main method"));
	}

	/**
	 * Checks that a failing assertion gives a witness whose target is the
	 * assertion, with the input files, their hashes and the specification of
	 * the task.
	 */
	@Test
	public void aFailingAssertionGivesAWitnessOnTheAssertion(
			@TempDir Path dir)
			throws Exception {
		Path file = witness("assert-fails", SVCOMPProperty.ASSERT, dir);
		String main = main("assert-fails");
		assertEquals(new Location(main, 4, 5), target(file));

		Task task = WitnessReader.read(file).getEntries().get(0).getMetadata().getTask();
		assertTrue(task.getInputFiles().contains(main));
		assertEquals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(
				Paths.get(main)))), task.getInputFileHashes().get(main));
		assertEquals(SVCOMPProperty.ASSERT.getSpecification(), task.getSpecification());
	}

	/**
	 * Checks that an uncaught exception gives a witness whose target is the
	 * statement that throws it, inside the method called by the main one.
	 */
	@Test
	public void anUncaughtExceptionGivesAWitnessOnTheThrow(
			@TempDir Path dir)
			throws Exception {
		Path file = witness("exception-thrown", SVCOMPProperty.RUNTIME_EXCEPTION, dir);
		assertEquals(new Location(main("exception-thrown"), 3, 5), target(file));
		assertEquals(SVCOMPProperty.RUNTIME_EXCEPTION.getSpecification(),
				WitnessReader.read(file).getEntries().get(0).getMetadata().getTask().getSpecification());
	}

	/**
	 * Checks that an exception raised in the initializer of a local variable
	 * gives a witness whose target is the initializer, a full expression on its
	 * own.
	 */
	@Test
	public void anExceptionInAnInitializerGivesAWitnessOnTheInitializer(
			@TempDir Path dir)
			throws Exception {
		assertRuntimeExceptionTarget("exception-in-initializer", 5, 17, dir);
	}

	/**
	 * Checks that an exception that can be raised at two statements gives no
	 * witness, since the statement of the violation is not known.
	 */
	@Test
	public void anExceptionRaisedAtTwoStatementsGivesNoWitness(
			@TempDir Path dir)
			throws Exception {
		assertDefiniteExceptionWithoutWitness("exception-two-origins", dir);
	}

	/**
	 * Checks that an exception raised in a returned expression gives a witness
	 * whose target is the expression, a full expression on its own.
	 */
	@Test
	public void anExceptionInAReturnedExpressionGivesAWitnessOnTheExpression(
			@TempDir Path dir)
			throws Exception {
		assertRuntimeExceptionTarget("exception-in-return", 5, 12, dir);
	}

	/**
	 * Checks that an exception raised in the argument of a throw gives a
	 * witness whose target is the thrown expression.
	 */
	@Test
	public void anExceptionInAThrownExpressionGivesAWitnessOnTheExpression(
			@TempDir Path dir)
			throws Exception {
		assertRuntimeExceptionTarget("exception-in-throw-argument", 5, 11, dir);
	}

	/**
	 * Checks that an exception raised in the condition of an assertion gives a
	 * witness whose target is the condition.
	 */
	@Test
	public void anExceptionInAnAssertionGivesAWitnessOnTheCondition(
			@TempDir Path dir)
			throws Exception {
		assertRuntimeExceptionTarget("exception-in-assert", 5, 12, dir);
	}

	/**
	 * Checks that an exception raised in an initializer converted to the type
	 * of its variable gives a witness whose target is the initializer.
	 */
	@Test
	public void anExceptionInAConvertedInitializerGivesAWitnessOnTheInitializer(
			@TempDir Path dir)
			throws Exception {
		assertRuntimeExceptionTarget("exception-in-implicit-cast", 5, 14, dir);
	}

	/**
	 * Checks that an exception raised in the initializer of a field gives a
	 * witness whose target is the initializer.
	 */
	@Test
	public void anExceptionInAFieldInitializerGivesAWitnessOnTheInitializer(
			@TempDir Path dir)
			throws Exception {
		assertRuntimeExceptionTarget("exception-in-field-initializer", 5, 13, dir);
	}

	/**
	 * Checks that a call on a null receiver gives a witness whose target is the
	 * call.
	 */
	@Test
	public void aCallOnANullReceiverGivesAWitnessOnTheCall(
			@TempDir Path dir)
			throws Exception {
		assertRuntimeExceptionTarget("null-receiver", 5, 5, dir);
	}

	/**
	 * Checks that a synchronized statement on a null lock gives a witness whose
	 * target is the synchronized statement.
	 */
	@Test
	public void aSynchronizedStatementOnANullLockGivesAWitnessOnTheStatement(
			@TempDir Path dir)
			throws Exception {
		assertRuntimeExceptionTarget("synchronized-null", 3, 5, dir);
	}

	/**
	 * Checks that an exception caught in a called method is not an origin of
	 * the exception that leaves it.
	 */
	@Test
	public void anExceptionCaughtInACalledMethodIsNotTheTarget(
			@TempDir Path dir)
			throws Exception {
		assertRuntimeExceptionTarget("exception-caught-in-callee", 10, 5, dir);
	}

	/**
	 * Checks that an exception of the same type raised at two statements gives
	 * no witness, since the statement of the violation is not known.
	 */
	@Test
	public void anExceptionOfTheSameTypeRaisedAtTwoStatementsGivesNoWitness(
			@TempDir Path dir)
			throws Exception {
		assertDefiniteExceptionWithoutWitness("exception-same-type-twice", dir);
	}

	/**
	 * Checks that each property has its own witness, when the task has more
	 * than one property.
	 */
	@Test
	public void eachPropertyHasItsOwnWitness(
			@TempDir Path dir)
			throws Exception {
		run("exception-thrown", dir, SVCOMPProperty.ASSERT, SVCOMPProperty.RUNTIME_EXCEPTION);
		assertTrue(Files.exists(ViolationWitnessProducer.file(dir, SVCOMPProperty.RUNTIME_EXCEPTION)));
		assertFalse(Files.exists(ViolationWitnessProducer.file(dir, SVCOMPProperty.ASSERT)));
	}

	/**
	 * Checks that an assertion that holds gives no witness.
	 */
	@Test
	public void anAssertionThatHoldsGivesNoWitness(
			@TempDir Path dir)
			throws Exception {
		assertFalse(Files.exists(witness("assert-holds", SVCOMPProperty.ASSERT, dir)));
	}

	/**
	 * Checks that a violation of another property than the one of the task
	 * gives no witness.
	 */
	@Test
	public void aViolationOfAnotherPropertyGivesNoWitness(
			@TempDir Path dir)
			throws Exception {
		assertFalse(Files.exists(witness("assert-fails", SVCOMPProperty.RUNTIME_EXCEPTION, dir)));
	}
}

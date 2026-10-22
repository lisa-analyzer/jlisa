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
	 * @param program  the folder of the program
	 * @param property the property file
	 * @param outdir   the output directory
	 *
	 * @return the file where the witness is written, if there is one
	 *
	 * @throws Exception if the analysis fails
	 */
	private static Path run(
			String program,
			String property,
			Path outdir)
			throws Exception {
		Main.main(new String[] {
				"-s", COMMON.toString(), TASKS.resolve(program).toString(),
				"-o", outdir.toString(),
				"-n", "ConstantPropagation",
				"--no-html",
				"--svcomp-property", TASKS.resolve(property).toString() });
		return outdir.resolve(ViolationWitnessProducer.WITNESS_DIR).resolve(ViolationWitnessProducer.WITNESS_FILE);
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
	 * Checks that a failing assertion gives a witness whose target is the
	 * assertion, with the input files, their hashes and the specification of
	 * the task.
	 */
	@Test
	public void aFailingAssertionGivesAWitnessOnTheAssertion(
			@TempDir Path dir)
			throws Exception {
		Path file = run("assert-fails", "valid-assert.prp", dir);
		String main = main("assert-fails");
		assertEquals(new Location(main, 4, 5), target(file));

		Task task = WitnessReader.read(file).getEntries().get(0).getMetadata().getTask();
		assertTrue(task.getInputFiles().contains(main));
		assertEquals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(
				Paths.get(main)))), task.getInputFileHashes().get(main));
		assertEquals(SVCOMPProperty.ASSERT.getSpecification(), task.getSpecification());
	}

	/**
	 * Checks that an uncaught exception gives no witness, since its violations
	 * are not reported yet.
	 */
	@Test
	public void anUncaughtExceptionGivesNoWitness(
			@TempDir Path dir)
			throws Exception {
		assertFalse(Files.exists(run("exception-thrown", "no-runtime-exception.prp", dir)));
	}

	/**
	 * Checks that an assertion that holds gives no witness.
	 */
	@Test
	public void anAssertionThatHoldsGivesNoWitness(
			@TempDir Path dir)
			throws Exception {
		assertFalse(Files.exists(run("assert-holds", "valid-assert.prp", dir)));
	}

	/**
	 * Checks that a violation of another property than the one of the task
	 * gives no witness.
	 */
	@Test
	public void aViolationOfAnotherPropertyGivesNoWitness(
			@TempDir Path dir)
			throws Exception {
		assertFalse(Files.exists(run("assert-fails", "no-runtime-exception.prp", dir)));
	}
}

package it.unive.jlisa.svcomp;

import it.unive.jlisa.checkers.Violation;
import it.unive.lisa.outputs.messages.StatementMessage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Certifies candidate confirmations of warnings by executing the program: the
 * violation witness of a warning, confirmed with the reproducing inputs of its
 * candidate, is validated by the witness validator of jlisa (run as a separate
 * process, as SV-COMP validates witnesses), and the confirmation is certified
 * only if the validator reports the witness as correct, that is, if a run of
 * the program with the values of the witness violates the property at the
 * target of the witness.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class WitnessCertifier
		implements
		Predicate<StatementMessage> {

	private static final Logger LOG = LogManager.getLogger(WitnessCertifier.class);

	/**
	 * The maximum time given to the validation of a witness, in milliseconds.
	 */
	public static final long MAX_VALIDATION_MILLIS = 60_000;

	/**
	 * The verdict of the validator for a correct witness.
	 */
	private static final String CORRECT = "[VERDICT] Witness Correct";

	private final List<String> sources;

	private final Map<SVCOMPProperty, String> propertyFiles;

	private final Function<StatementMessage, Violation> violations;

	private final long deadline;

	/**
	 * Builds the certifier.
	 *
	 * @param sources       the sources of the task, as given to jlisa
	 * @param propertyFiles the property files of the task, for each property
	 * @param violations    yields the violation reported by a confirmed warning
	 *                          (with its target and reproducing inputs), or
	 *                          {@code null} if it has none
	 * @param deadline      the time by which certifications must end (as
	 *                          returned by {@link System#currentTimeMillis()}),
	 *                          or {@code 0} for no deadline
	 */
	public WitnessCertifier(
			List<String> sources,
			Map<SVCOMPProperty, String> propertyFiles,
			Function<StatementMessage, Violation> violations,
			long deadline) {
		this.sources = sources;
		this.propertyFiles = propertyFiles;
		this.violations = violations;
		this.deadline = deadline;
	}

	@Override
	public boolean test(
			StatementMessage confirmed) {
		Violation violation = violations.apply(confirmed);
		if (violation == null)
			return false;
		for (Map.Entry<SVCOMPProperty, String> property : propertyFiles.entrySet())
			if (property.getKey().getViolation() == violation.getKind())
				return certify(violation, property.getValue());
		return false;
	}

	private boolean certify(
			Violation violation,
			String propertyFile) {
		long time = MAX_VALIDATION_MILLIS;
		if (deadline > 0)
			time = Math.min(time, deadline - System.currentTimeMillis());
		if (time <= 0)
			return false;
		ViolationWitnessProducer producer = ViolationWitnessProducer.load(sources, propertyFile);
		if (producer == null)
			return false;
		producer.accept(violation);
		Path dir = null;
		try {
			dir = Files.createTempDirectory("jlisa-certify");
			producer.write(dir.toString());
			Path witness = ViolationWitnessProducer.file(dir, producer.getProperty());
			if (!Files.isRegularFile(witness))
				return false;
			File output = dir.resolve("validation.log").toFile();
			List<String> command = new ArrayList<>(List.of(
					Paths.get(System.getProperty("java.home"), "bin", "java").toString(),
					"--add-modules", "jdk.jdi",
					"-cp", System.getProperty("java.class.path"),
					"it.unive.jlisa.Main", "validate",
					"-l", "ERROR",
					"--witness", witness.toAbsolutePath().toString(),
					"--svcomp-property", propertyFile,
					// last, since it takes all the following arguments
					"--source"));
			command.addAll(sources);
			Process process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(output).start();
			if (!process.waitFor(time, TimeUnit.MILLISECONDS)) {
				process.destroyForcibly();
				LOG.info("The certification of {} ran out of time", violation);
				return false;
			}
			boolean correct = Files.readAllLines(output.toPath(), StandardCharsets.UTF_8).stream()
					.anyMatch(line -> line.trim().equals(CORRECT));
			LOG.info("The certification of {} {}", violation, correct ? "succeeded" : "failed");
			return correct;
		} catch (IOException e) {
			LOG.warn("The certification of {} failed: {}", violation, e.getMessage());
			return false;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		} finally {
			if (dir != null)
				delete(dir);
		}
	}

	private static void delete(
			Path dir) {
		try (Stream<Path> files = Files.walk(dir)) {
			files.sorted(Comparator.reverseOrder()).forEach(f -> f.toFile().delete());
		} catch (IOException e) {
			// temporary files are left
		}
	}
}

package it.unive.jlisa.svcomp;

import it.unive.jlisa.checkers.Violation;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.witness.v2.InvalidWitnessException;
import it.unive.lisa.witness.v2.Location;
import it.unive.lisa.witness.v2.ViolationWitness;
import it.unive.lisa.witness.v2.ViolationWitnessBuilder;
import it.unive.lisa.witness.v2.WitnessWriter;
import it.unive.lisa.witness.v2.metadata.Producer;
import it.unive.lisa.witness.v2.metadata.Task;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A producer of the violation witness of an SV-COMP task. It receives the
 * definite violations found by the analysis, keeps the first one of the
 * property of the task, and writes a witness whose only waypoint is the target,
 * where the violation happens. The witness only claims that an execution
 * violates the property at the target, hence it needs no other waypoint.
 *
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public final class ViolationWitnessProducer
		implements
		Consumer<Violation> {

	private static final Logger LOG = LogManager.getLogger(ViolationWitnessProducer.class);

	/**
	 * The name of the folder of the witnesses, inside the output directory.
	 * Each property has its own folder inside it.
	 */
	public static final String WITNESS_DIR = "witness";

	/**
	 * The name of the file of the witness.
	 */
	public static final String WITNESS_FILE = "witness.yml";

	/**
	 * The task of the witness.
	 */
	private final Task task;

	/**
	 * The property of the task.
	 */
	private final SVCOMPProperty property;

	/**
	 * The tool producing the witness.
	 */
	private final Producer producer;

	/**
	 * The first violation of the property of the task, or {@code null} if none
	 * has been received yet.
	 */
	private Violation violation;

	/**
	 * Returns the property of the task.
	 *
	 * @return the property
	 */
	public SVCOMPProperty getProperty() {
		return property;
	}

	/**
	 * Builds the producer of the witness of the given task.
	 *
	 * @param task     the task
	 * @param property the property of the task
	 * @param producer the tool producing the witness
	 */
	public ViolationWitnessProducer(
			Task task,
			SVCOMPProperty property,
			Producer producer) {
		this.task = task;
		this.property = property;
		this.producer = producer;
	}

	/**
	 * Builds the producer of the witness of the task made of the given input
	 * files and property file. The property file is read once, and its text is
	 * both the specification of the witness and the property whose violations
	 * are kept.
	 *
	 * @param inputs       the input files and directories of the task, as given
	 *                         to the verifier
	 * @param propertyFile the property file of the task
	 *
	 * @return the producer, or {@code null} if the task cannot be loaded or its
	 *             property is not an SV-COMP property of Java tasks
	 */
	public static ViolationWitnessProducer load(
			List<String> inputs,
			String propertyFile) {
		Task task;
		try {
			task = SVCOMPWitnesses.task(inputs, propertyFile);
		} catch (IOException | RuntimeException e) {
			LOG.warn("No violation witness: the task cannot be loaded ({})", e.getMessage());
			return null;
		}
		SVCOMPProperty property = SVCOMPProperty.of(task.getSpecification());
		if (property == null) {
			LOG.warn("No violation witness: unknown property '{}'", task.getSpecification());
			return null;
		}
		return new ViolationWitnessProducer(task, property, SVCOMPWitnesses.producer(null));
	}

	@Override
	public void accept(
			Violation violation) {
		if (this.violation == null && violation.getKind() == property.getViolation())
			this.violation = violation;
	}

	/**
	 * Builds the witness of the first violation received of the property of the
	 * task.
	 *
	 * @return the witness, or nothing if no such violation was received, or if
	 *             its location cannot be written in the witness
	 */
	public Optional<ViolationWitness> build() {
		if (violation == null)
			return Optional.empty();
		Location target = location(violation.getTarget());
		if (target == null)
			return Optional.empty();
		try {
			return Optional.of(new ViolationWitnessBuilder(task, producer).target(target).build());
		} catch (InvalidWitnessException e) {
			LOG.warn("No violation witness for {}: {}", violation, e.getMessage());
			return Optional.empty();
		}
	}

	/**
	 * Writes the witness of the first violation received of the property of the
	 * task, in the file {@value #WITNESS_FILE} of the folder named after the
	 * property (e.g., {@code valid-assert}) inside the folder
	 * {@value #WITNESS_DIR} of the given output directory. Nothing is written
	 * if there is no witness, and a failure is only logged.
	 *
	 * @param outdir the output directory
	 */
	public void write(
			String outdir) {
		Optional<ViolationWitness> witness = build();
		if (witness.isEmpty())
			return;
		Path file = file(Paths.get(outdir), property);
		try {
			Files.createDirectories(file.getParent());
			WitnessWriter.write(witness.get(), file);
			LOG.info("Violation witness written to {}", file);
		} catch (IOException | RuntimeException e) {
			LOG.warn("The violation witness cannot be written to {}: {}", file, e.getMessage());
		}
	}

	/**
	 * Returns the file where the witness of the given property is written,
	 * inside the given output directory.
	 *
	 * @param outdir   the output directory
	 * @param property the property
	 *
	 * @return the file
	 */
	public static Path file(
			Path outdir,
			SVCOMPProperty property) {
		return outdir.resolve(WITNESS_DIR).resolve(property.getName()).resolve(WITNESS_FILE);
	}

	/**
	 * Returns the location of the witness for the given statement, that is, its
	 * start. The statement is the target of the violation, the innermost
	 * statement or full expression where it happens. Its file is the input file
	 * of the task with the same name as the source file of the statement, which
	 * must be unique. Columns of LiSA start from 0, while those of witnesses
	 * start from 1.
	 *
	 * @param statement the statement
	 *
	 * @return the location, or {@code null} if it cannot be determined
	 */
	private Location location(
			Statement statement) {
		if (!(statement.getLocation() instanceof SourceCodeLocation)) {
			LOG.warn("No violation witness for {}: no source location", violation);
			return null;
		}
		SourceCodeLocation location = (SourceCodeLocation) statement.getLocation();
		if (location.getLine() < 1) {
			LOG.warn("No violation witness for {}: no line", violation);
			return null;
		}
		String file = SVCOMPWitnesses.inputFile(task, location.getSourceFile());
		if (file == null) {
			LOG.warn("No violation witness for {}: no input file or more than one is named {}", violation,
					location.getSourceFile());
			return null;
		}
		Integer column = location.getCol() < 0 ? null : location.getCol() + 1;
		return new Location(file, location.getLine(), column);
	}
}

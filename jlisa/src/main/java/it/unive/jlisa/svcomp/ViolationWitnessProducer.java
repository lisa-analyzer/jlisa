package it.unive.jlisa.svcomp;

import it.unive.jlisa.checkers.Violation;
import it.unive.jlisa.witness.validator.violation.ViolationWitnessWriter;
import it.unive.lisa.checks.verification.ReproducingInput;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.SourceCodeLocation;
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
import java.util.regex.Pattern;
import java.util.function.Consumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A producer of the violation witness of an SV-COMP task. It receives the
 * definite violations found by the analysis, keeps the first one of the
 * property of the task, and writes a witness ending with the target, where the
 * violation happens. If the violation comes with the inputs reproducing it,
 * the values returned by nondeterministic calls are given as function return
 * waypoints before the target, in the order they are consumed; otherwise, the
 * witness only claims that an execution violates the property at the target,
 * hence it needs no other waypoint.
 *
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public final class ViolationWitnessProducer
		implements
		Consumer<Violation> {

	private static final Logger LOG = LogManager.getLogger(ViolationWitnessProducer.class);

	/**
	 * The prefix of the description of the inputs that are values returned by
	 * calls (see {@link it.unive.lisa.triage.inputs.InputSource}).
	 */
	private static final String CALL_RESULT = "return of ";

	private static final Pattern INTEGER = Pattern.compile("-?[0-9]+");

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
		Location target = location(violation.getTarget().getLocation());
		if (target == null)
			return Optional.empty();
		try {
			ViolationWitnessBuilder builder = new ViolationWitnessBuilder(task, producer);
			for (ReproducingInput input : violation.getInputs()) {
				String constraint = returnConstraint(input);
				if (constraint == null)
					continue;
				Location call = location(input.getLocation());
				if (call != null)
					builder.functionReturn(call, constraint);
			}
			return Optional.of(builder.target(target).build());
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
	 * Returns the constraint of the function return waypoint for the given
	 * input, if it is the value returned by a call and it can be written in a
	 * witness (values of primitive types and strings, see
	 * {@link ViolationWitnessWriter#constraint(Object)}).
	 *
	 * @param input the input
	 *
	 * @return the constraint, such as {@code \result == 42}, or {@code null}
	 */
	private static String returnConstraint(
			ReproducingInput input) {
		if (!input.getSource().startsWith(CALL_RESULT))
			// e.g., parameters of the main method
			return null;
		Object value = javaValue(input);
		if (value == null) {
			LOG.debug("The value of {} cannot be written in the witness", input);
			return null;
		}
		return ViolationWitnessWriter.constraint(value);
	}

	/**
	 * Returns the Java value of the given input, returned by a call, of the
	 * return type of the call, or {@code null} if it is not a value of a
	 * primitive type or a string. The source of the input starts with the
	 * return type (e.g., {@code return of int ...}); its value is printed as
	 * the verification of the warnings does: integers (also for characters),
	 * booleans, numbers as printed by Java for floating point types, and
	 * strings as SMT-LIB string literals.
	 *
	 * @param input the input
	 *
	 * @return the value, or {@code null}
	 */
	private static Object javaValue(
			ReproducingInput input) {
		String value = input.getValue();
		String[] source = input.getSource().substring(CALL_RESULT.length()).split(" ", 2);
		if (value == null || source.length == 0)
			return null;
		try {
			switch (source[0]) {
			case "bool":
			case "boolean":
				return value.equals("true") ? Boolean.TRUE : value.equals("false") ? Boolean.FALSE : null;
			case "byte":
				return INTEGER.matcher(value).matches() ? Byte.valueOf(value) : null;
			case "short":
				return INTEGER.matcher(value).matches() ? Short.valueOf(value) : null;
			case "int":
				return INTEGER.matcher(value).matches() ? Integer.valueOf(value) : null;
			case "long":
				return INTEGER.matcher(value).matches() ? Long.valueOf(value) : null;
			case "char":
				if (!INTEGER.matcher(value).matches())
					return null;
				int code = Integer.parseInt(value);
				return code >= Character.MIN_VALUE && code <= Character.MAX_VALUE ? Character.valueOf((char) code)
						: null;
			case "float":
				return Float.valueOf(value);
			case "double":
				return Double.valueOf(value);
			case "java.lang.String*":
			case "java.lang.String":
				return smtString(value);
			default:
				return null;
			}
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/**
	 * Returns the string denoted by the given SMT-LIB string literal: two
	 * double quotes stand for one, and {@code \\u{h}}, {@code \\ud{h}} and
	 * {@code \\udddd} escapes for the character with the given hexadecimal
	 * code. Returns {@code null} if the text is not such a literal.
	 *
	 * @param literal the literal
	 *
	 * @return the string, or {@code null}
	 */
	private static String smtString(
			String literal) {
		if (literal.length() < 2 || !literal.startsWith("\"") || !literal.endsWith("\""))
			return null;
		String body = literal.substring(1, literal.length() - 1);
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < body.length(); i++) {
			char c = body.charAt(i);
			if (c == '"') {
				// a quote is written twice inside the literal
				if (i + 1 >= body.length() || body.charAt(i + 1) != '"')
					return null;
				result.append('"');
				i++;
			} else if (c == '\\' && i + 1 < body.length() && body.charAt(i + 1) == 'u') {
				int end;
				String hex;
				if (i + 2 < body.length() && body.charAt(i + 2) == '{') {
					end = body.indexOf('}', i + 3);
					if (end < 0)
						return null;
					hex = body.substring(i + 3, end);
				} else {
					end = i + 5;
					if (end >= body.length())
						return null;
					hex = body.substring(i + 2, end + 1);
				}
				try {
					result.appendCodePoint(Integer.parseInt(hex, 16));
				} catch (IllegalArgumentException e) {
					return null;
				}
				i = end;
			} else
				result.append(c);
		}
		return result.toString();
	}

	/**
	 * Returns the location of the witness for the given code location, that
	 * is, its start. The location is the one of the target of the violation,
	 * the innermost statement or full expression where it happens, or of a
	 * call returning a nondeterministic value. Its file is the input file of
	 * the task with the same name as its source file, which must be unique.
	 * Columns of LiSA start from 0, while those of witnesses start from 1.
	 *
	 * @param codeLocation the code location
	 *
	 * @return the location, or {@code null} if it cannot be determined
	 */
	private Location location(
			CodeLocation codeLocation) {
		if (!(codeLocation instanceof SourceCodeLocation)) {
			LOG.warn("No violation witness for {}: no source location", violation);
			return null;
		}
		SourceCodeLocation location = (SourceCodeLocation) codeLocation;
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

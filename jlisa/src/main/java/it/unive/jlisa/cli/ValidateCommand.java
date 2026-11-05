package it.unive.jlisa.cli;

import it.unive.jlisa.witness.validator.RunSummary;
import it.unive.jlisa.witness.validator.ValidationException;
import it.unive.jlisa.witness.validator.correctness.CorrectnessValidator;
import it.unive.jlisa.witness.validator.violation.ViolationValidatorV2;
import it.unive.lisa.witness.v2.UnsupportedWitnessVersionException;
import it.unive.lisa.witness.v2.ViolationWitness;
import it.unive.lisa.witness.v2.Witness;
import it.unive.lisa.witness.v2.WitnessReadException;
import it.unive.lisa.witness.v2.WitnessReader;
import java.io.File;
import java.nio.file.Paths;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The {@code validate} sub-command of jlisa: the witness validator.
 *
 * <pre>{@code
 * jlisa validate -w <witness> -s <inputs...> --svcomp-property <file.prp> [--type violation|correctness]
 *                [--input-search] [-j <java>] [-l <level>]
 * }</pre>
 * <p>
 * The task is given as SV-COMP gives it to the tool: its input files and
 * directories, and its property file. The kind of witness (violation or
 * correctness) is read from the witness; with {@code --type}, a witness of the
 * other kind is not validated.
 * <h3>Output (stdout)</h3>
 * <ul>
 * <li>{@code Witness Correct} — the witness is valid (violation
 * reproduced)</li>
 * <li>{@code Witness Spurious} — the witness path does not lead to the property
 * violation</li>
 * <li>{@code Could not validate} — timeout, unsupported witness, or a
 * correctness witness (only parsed, not validated)</li>
 * <li>{@code Invalid witness format} — the witness has a version of the witness
 * format that is not supported (e.g. 2.3)</li>
 * <li>{@code Parsing failed} — the witness cannot be read (e.g. a GraphML
 * witness, an empty or malformed file)</li>
 * <li>{@code Invalid witness type} — the witness is not of the kind given with
 * {@code --type}</li>
 * </ul>
 * <p>
 * The logs go to stderr. The exit code is always 0 so that BenchExec does not
 * misinterpret a non-zero exit as a validator crash.
 */
public final class ValidateCommand {

	private static final Logger LOG = LogManager.getLogger(ValidateCommand.class);

	private static final String COMMAND = "validate";

	private static final String VIOLATION = "violation";

	private static final String CORRECTNESS = "correctness";

	private ValidateCommand() {
	}

	/**
	 * Runs {@code jlisa validate} with the arguments that follow the
	 * sub-command.
	 *
	 * @param args the arguments of the sub-command
	 */
	public static void run(
			String[] args) {
		Options options = options();
		CommandLine cmd = SvcompTask.parse(COMMAND, options, args);

		String witnessPath = cmd.getOptionValue("witness");
		if (witnessPath == null)
			SvcompTask.usage(COMMAND, options, "--witness is required");
		String type = cmd.getOptionValue("type");
		if (type != null && !type.equals(VIOLATION) && !type.equals(CORRECTNESS))
			SvcompTask.usage(COMMAND, options, "--type must be violation or correctness, not " + type);
		if (!new File(witnessPath).exists()) {
			System.err.println("Error: witness file not found: " + witnessPath);
			RunSummary.couldNotValidate("witness-not-found", witnessPath);
			System.exit(0);
		}

		try {
			SvcompTask task = SvcompTask.of(COMMAND, cmd, options);
			Witness witness;
			try {
				witness = WitnessReader.read(Paths.get(witnessPath));
			} catch (WitnessReadException e) {
				if (unsupportedVersion(e)) {
					LOG.error("Invalid witness format: {}", e.getMessage());
					RunSummary.invalidFormat(e.getMessage());
				} else {
					LOG.error("Parsing failed: {}", e.getMessage());
					RunSummary.parsingFailed(e.getMessage());
				}
				System.exit(0);
				return;
			}
			LOG.info("Parsed witness: {}", witness);
			String kind = witness instanceof ViolationWitness ? VIOLATION : CORRECTNESS;
			if (type != null && !type.equals(kind)) {
				RunSummary.invalidType("expected a " + type + " witness, got a " + kind + " witness");
				System.exit(0);
				return;
			}
			if (witness instanceof ViolationWitness violationWitness) {
				ViolationValidatorV2.validate(violationWitness, task.property(), task.sources(),
						cmd.getOptionValue("java"), cmd.hasOption("input-search"));
			} else {
				// correctness witness are not validated yet (just linting)
				CorrectnessValidator.validateMock();
			}
		} catch (ValidationException e) {
			LOG.error("Validation error: {}", e.getMessage(), e);
			RunSummary.couldNotValidate("validation-error", e.getMessage());
		} catch (Exception e) {
			LOG.error("Unexpected error: {}", e.getMessage(), e);
			RunSummary.couldNotValidate("internal-error", e.getClass().getSimpleName() + ": " + e.getMessage());
		}

		System.exit(0);
	}

	/**
	 * Returns whether the witness could not be read because its version of the
	 * witness format is not supported.
	 */
	private static boolean unsupportedVersion(
			WitnessReadException e) {
		for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause())
			if (cause instanceof UnsupportedWitnessVersionException)
				return true;
		return false;
	}

	private static Options options() {
		Options opts = SharedOptions.create("The property file of the task");
		opts.addOption(Option.builder("w")
				.longOpt("witness")
				.hasArg()
				.argName("FILE")
				.desc("Witness file to validate (YAML, format 2.4)")
				.build());
		opts.addOption(Option.builder()
				.longOpt("type")
				.hasArg()
				.argName("TYPE")
				.desc("The kind of witness expected: violation or correctness. A witness of the other kind "
						+ "gives Invalid witness type (default: any kind, read from the witness)")
				.build());
		opts.addOption(Option.builder()
				.longOpt("input-search")
				.desc("When the witness does not fix every nondet call and its values do not lead to the "
						+ "violation, search for values of the other calls that do (default: off)")
				.build());
		opts.addOption(SvcompTask.javaOption());
		SharedOptions.markRequired(opts, "witness", SharedOptions.SOURCE, SharedOptions.SVCOMP_PROPERTY);
		return opts;
	}
}

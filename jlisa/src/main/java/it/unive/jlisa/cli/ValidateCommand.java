package it.unive.jlisa.cli;

import it.unive.jlisa.witness.validator.RunSummary;
import it.unive.jlisa.witness.validator.ValidationException;
import it.unive.jlisa.witness.validator.correctness.CorrectnessValidator;
import it.unive.jlisa.witness.validator.violation.ViolationValidatorV2;
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
 * jlisa validate -w <witness> -s <inputs...> --svcomp-property <file.prp> [--input-search] [-j <java>] [-l <level>]
 * }</pre>
 * <p>
 * The task is given as SV-COMP gives it to the tool: its input files and
 * directories, and its property file. The kind of witness (violation or
 * correctness) is read from the witness.
 * <h3>Output (stdout)</h3>
 * <ul>
 * <li>{@code Witness Correct} — the witness is valid (violation
 * reproduced)</li>
 * <li>{@code Witness Spurious} — the witness path does not lead to the property
 * violation</li>
 * <li>{@code Could not validate} — timeout, unsupported witness, or a
 * correctness witness (only parsed, not validated)</li>
 * <li>{@code Parsing failed} — the witness is not a valid YAML witness of
 * format 2.4 (e.g. another version, a GraphML witness, an empty file)</li>
 * </ul>
 * <p>
 * The logs go to stderr. The exit code is always 0 so that BenchExec does not
 * misinterpret a non-zero exit as a validator crash.
 */
public final class ValidateCommand {

	private static final Logger LOG = LogManager.getLogger(ValidateCommand.class);

	private static final String COMMAND = "validate";

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
				LOG.error("Parsing failed: {}", e.getMessage());
				RunSummary.parsingFailed(e.getMessage());
				System.exit(0);
				return;
			}
			LOG.info("Parsed witness: {}", witness);
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

	private static Options options() {
		Options opts = SharedOptions.create("The property file of the task");
		opts.addOption(Option.builder("w")
				.longOpt("witness")
				.hasArg()
				.argName("FILE")
				.desc("Witness file to validate (YAML, format 2.4)")
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

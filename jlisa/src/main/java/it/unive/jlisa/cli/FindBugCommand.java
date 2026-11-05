package it.unive.jlisa.cli;

import it.unive.jlisa.witness.validator.RunSummary;
import it.unive.jlisa.witness.validator.ValidationException;
import it.unive.jlisa.witness.validator.violation.BugFinder;
import it.unive.jlisa.witness.validator.violation.ViolationWitnessWriter;
import java.nio.file.Paths;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The {@code find-bug} sub-command of jlisa: looks for inputs that violate the
 * property of an SV-COMP task, by running the program.
 *
 * <pre>{@code
 * jlisa find-bug -s <inputs...> --svcomp-property <file.prp> [-t <seconds>] [--witness-output <file>]
 *                [-j <java>] [-l <level>]
 * }</pre>
 * <p>
 * It prints {@code Violation found} on stdout when a run violates the property,
 * otherwise {@code Could not validate}. The logs go to stderr, and the exit
 * code is always 0.
 */
public final class FindBugCommand {

	private static final Logger LOG = LogManager.getLogger(FindBugCommand.class);

	private static final String COMMAND = "find-bug";

	private FindBugCommand() {
	}

	/**
	 * Runs {@code jlisa find-bug} with the arguments that follow the
	 * sub-command.
	 *
	 * @param args the arguments of the sub-command
	 */
	public static void run(
			String[] args) {
		Options options = options();
		CommandLine cmd = SvcompTask.parse(COMMAND, options, args);

		try {
			SvcompTask task = SvcompTask.of(COMMAND, cmd, options);
			ViolationWitnessWriter.Request witnessOutput = null;
			if (cmd.hasOption("witness-output"))
				witnessOutput = new ViolationWitnessWriter.Request(Paths.get(cmd.getOptionValue("witness-output")),
						List.of(cmd.getOptionValues(SharedOptions.SOURCE)),
						cmd.getOptionValue(SharedOptions.SVCOMP_PROPERTY));
			String timeLimit = cmd.getOptionValue("time-limit", "60");
			long millis;
			try {
				millis = (long) (Double.parseDouble(timeLimit) * 1000);
			} catch (NumberFormatException e) {
				RunSummary.couldNotValidate("usage", "--time-limit is not a number: " + timeLimit);
				System.exit(0);
				return;
			}
			BugFinder.find(task.property(), task.sources(), cmd.getOptionValue("java"), millis, witnessOutput);
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
		opts.addOption(Option.builder("t")
				.longOpt("time-limit")
				.hasArg()
				.argName("SECONDS")
				.desc("Time for the bug-finding search (default 60)")
				.build());
		opts.addOption(Option.builder()
				.longOpt("witness-output")
				.hasArg()
				.argName("FILE")
				.desc("Write the violation witness of the violation found to this file")
				.build());
		opts.addOption(SvcompTask.javaOption());
		SharedOptions.markRequired(opts, SharedOptions.SOURCE, SharedOptions.SVCOMP_PROPERTY);
		return opts;
	}
}

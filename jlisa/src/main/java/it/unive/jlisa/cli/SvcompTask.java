package it.unive.jlisa.cli;

import it.unive.jlisa.Main;
import it.unive.jlisa.svcomp.SVCOMPProperty;
import it.unive.jlisa.witness.validator.RunSummary;
import it.unive.jlisa.witness.validator.ValidationException;
import it.unive.jlisa.witness.validator.model.Property;
import it.unive.jlisa.witness.validator.model.TaskSources;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * An SV-COMP task as the {@code validate} and {@code find-bug} sub-commands
 * take it: its input files and directories ({@code --source}) and its property
 * file ({@code --svcomp-property}), with the command-line handling the two
 * sub-commands share. Both always exit with code 0, so that BenchExec does not
 * take a wrong command line for a crash of the validator.
 *
 * @param sources  the sources of the task
 * @param property the property of the task
 */
record SvcompTask(
		TaskSources sources,
		Property property) {

	/**
	 * Returns the option with the Java executable that runs the program.
	 *
	 * @return the option
	 */
	static Option javaOption() {
		return Option.builder("j")
				.longOpt("java")
				.hasArg()
				.argName("PATH")
				.desc("Path to the Java executable used to run benchmarks (default: java on PATH). "
						+ "For SV-COMP benchmarks compiled to Java 8, pass a Java 8 binary here.")
				.build();
	}

	/**
	 * Returns the parsed command line of a sub-command, after setting its log
	 * level. With {@code --help} or {@code --version}, or after a wrong command
	 * line, the run ends here.
	 *
	 * @param command the name of the sub-command
	 * @param options the options of the sub-command
	 * @param args    the arguments of the sub-command
	 *
	 * @return the parsed command line
	 */
	static CommandLine parse(
			String command,
			Options options,
			String[] args) {
		CommandLine cmd = null;
		try {
			cmd = new DefaultParser().parse(options, args);
			SharedOptions.applyLogLevel(cmd);
		} catch (ParseException e) {
			usage(command, options, e.getMessage());
		}
		if (cmd.hasOption("help")) {
			SharedOptions.printHelp(command, options, System.out);
			System.exit(0);
		}
		if (cmd.hasOption("version")) {
			System.out.println(Main.versionLine());
			System.exit(0);
		}
		return cmd;
	}

	/**
	 * Returns the task of the command line. Without {@code --source} and
	 * {@code --svcomp-property}, or with a property that is not supported, the
	 * run ends here.
	 *
	 * @param command the name of the sub-command
	 * @param cmd     the parsed command line
	 * @param options the options of the sub-command
	 *
	 * @return the task
	 *
	 * @throws IOException         if the property file cannot be read
	 * @throws ValidationException if the sources cannot be collected
	 */
	static SvcompTask of(
			String command,
			CommandLine cmd,
			Options options)
			throws IOException,
			ValidationException {
		if (!cmd.hasOption(SharedOptions.SOURCE) || !cmd.hasOption(SharedOptions.SVCOMP_PROPERTY))
			usage(command, options, "--source and --svcomp-property are required");
		String propertyFile = cmd.getOptionValue(SharedOptions.SVCOMP_PROPERTY);
		SVCOMPProperty property = SVCOMPProperty.of(Files.readString(Paths.get(propertyFile)));
		if (property == null) {
			RunSummary.couldNotValidate("unsupported-property", propertyFile);
			System.exit(0);
		}
		return new SvcompTask(TaskSources.fromInputs(List.of(cmd.getOptionValues(SharedOptions.SOURCE))),
				property == SVCOMPProperty.ASSERT ? Property.ASSERT : Property.RUNTIME);
	}

	/**
	 * Ends the run after a wrong command line: prints the error and the usage
	 * on standard error, then the verdict {@code Could not validate}.
	 *
	 * @param command the name of the sub-command
	 * @param options the options of the sub-command
	 * @param message what is wrong
	 */
	static void usage(
			String command,
			Options options,
			String message) {
		System.err.println("Error: " + message + ".");
		SharedOptions.printHelp(command, options, System.err);
		RunSummary.couldNotValidate("usage", message);
		System.exit(0);
	}
}

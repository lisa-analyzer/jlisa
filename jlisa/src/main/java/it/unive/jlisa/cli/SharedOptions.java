package it.unive.jlisa.cli;

import it.unive.jlisa.LogManager;
import java.io.PrintStream;
import java.io.PrintWriter;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Level;

/**
 * The options shared by the sub-commands of jlisa ({@code check},
 * {@code validate} and {@code find-bug}).
 */
public final class SharedOptions {

	/**
	 * The option with the input files and directories of the program.
	 */
	public static final String SOURCE = "source";

	/**
	 * The option with the SV-COMP property file of the task.
	 */
	public static final String SVCOMP_PROPERTY = "svcomp-property";

	/**
	 * The option with the log level.
	 */
	public static final String LOG_LEVEL = "log-level";

	private SharedOptions() {
	}

	/**
	 * Returns a new set of options holding the shared ones: help, version,
	 * sources, SV-COMP property and log level.
	 *
	 * @param propertyDescription the description of the SV-COMP property
	 *                                option, which depends on the sub-command
	 *
	 * @return the options
	 */
	public static Options create(
			String propertyDescription) {
		Options options = new Options();
		options.addOption(Option.builder("h")
				.longOpt("help")
				.desc("Print this help message")
				.build());
		options.addOption(Option.builder("v")
				.longOpt("version")
				.desc("Print the version of jlisa")
				.build());
		options.addOption(Option.builder("s")
				.longOpt(SOURCE)
				.hasArgs()
				.argName("INPUTS")
				.desc("The input files and directories of the program, as SV-COMP gives them to the tool "
						+ "(e.g. -s common/ task/)")
				.build());
		options.addOption(Option.builder()
				.longOpt(SVCOMP_PROPERTY)
				.hasArg()
				.argName("PRP")
				.desc(propertyDescription)
				.build());
		options.addOption(Option.builder("l")
				.longOpt(LOG_LEVEL)
				.hasArg()
				.argName("LEVEL")
				.desc("Log level: OFF, FATAL, ERROR, WARN, INFO, DEBUG (default), TRACE, ALL")
				.build());
		return options;
	}

	/**
	 * Marks options of a sub-command as required in its usage, by starting
	 * their descriptions with {@code (required)}. The sub-command checks them
	 * itself: options required by the parser would fail before {@code --help}
	 * is read.
	 *
	 * @param options the options of the sub-command
	 * @param names   the long names of the required options
	 */
	public static void markRequired(
			Options options,
			String... names) {
		for (String name : names) {
			Option option = options.getOption(name);
			option.setDescription("(required) " + option.getDescription());
		}
	}

	/**
	 * Sets the log level of jlisa and LiSA given with {@code --log-level}, if
	 * any.
	 *
	 * @param cmd the parsed command line
	 *
	 * @throws ParseException if the level is not a log level
	 */
	public static void applyLogLevel(
			CommandLine cmd)
			throws ParseException {
		if (!cmd.hasOption(LOG_LEVEL))
			return;
		String name = cmd.getOptionValue(LOG_LEVEL).toUpperCase();
		Level level = Level.getLevel(name);
		if (level == null)
			throw new ParseException("Invalid log level: " + name);
		LogManager.setLogLevel(level);
	}

	/**
	 * Prints the usage of a sub-command.
	 *
	 * @param command the name of the sub-command
	 * @param options the options of the sub-command
	 * @param out     where to print it: standard output when asked with
	 *                    {@code --help}, standard error after a wrong command
	 *                    line
	 */
	public static void printHelp(
			String command,
			Options options,
			PrintStream out) {
		HelpFormatter formatter = new HelpFormatter();
		PrintWriter writer = new PrintWriter(out, true);
		formatter.printHelp(writer, formatter.getWidth(), "jlisa " + command, null, options,
				formatter.getLeftPadding(), formatter.getDescPadding(), null, true);
		writer.flush();
	}
}

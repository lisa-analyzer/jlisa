package it.unive.jlisa.witness.validator;

import it.unive.jlisa.svcomp.SVCOMPProperty;
import it.unive.jlisa.witness.validator.correctness.CorrectnessValidator;
import it.unive.jlisa.witness.validator.model.Property;
import it.unive.jlisa.witness.validator.model.TaskSources;
import it.unive.jlisa.witness.validator.parser.WitnessModel;
import it.unive.jlisa.witness.validator.parser.WitnessParserV1;
import it.unive.jlisa.witness.validator.parser.WitnessParserV2;
import it.unive.jlisa.witness.validator.violation.BugFinder;
import it.unive.jlisa.witness.validator.violation.ViolationValidator;
import it.unive.jlisa.witness.validator.violation.ViolationWitnessWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

/**
 * Entry point for the jLISA Witness Validator.
 * <h3>Usage</h3>
 * 
 * <pre>{@code
 * jlisa-witness-validator --witness <file> [--input-search] TASK [--java <path>] [--verbose]
 * jlisa-witness-validator --find-bug [--time-limit <seconds>] [--witness-output <file>] TASK
 *                         [--java <path>] [--verbose]
 *
 * TASK: --source <inputs...> --svcomp-property <file.prp>   (as SV-COMP gives it to the tool)
 *    or --benchmark <task.yml> --property assert|runtime [--extra-sources <dir>]
 * }</pre>
 * <p>
 * The property is given explicitly; it is never derived from the task file.
 * <h3>Output (stdout)</h3>
 * <ul>
 * <li>{@code Witness Correct} — the witness is valid (violation reproduced /
 * invariants verified)</li>
 * <li>{@code Witness Spurious} — the witness path does not lead to the property
 * violation</li>
 * <li>{@code Could not validate} — parse error, timeout, or unsupported witness
 * type</li>
 * <li>{@code Violation found} — ({@code --find-bug}) a run violates the
 * property</li>
 * </ul>
 * <p>
 * The exit code is always 0 so that BenchExec does not misinterpret a non-zero
 * exit as a validator crash.
 */
public final class Main {

	private static final Logger LOG = LogManager.getLogger(Main.class);

	private Main() {
	}

	/**
	 * The entry point of the application.
	 *
	 * @param args the command-line arguments
	 */
	public static void main(
			String[] args) {
		Logging.configure();
		Options options = buildOptions();

		CommandLine cmd;
		try {
			cmd = new DefaultParser().parse(options, args);
		} catch (ParseException e) {
			System.err.println("Error: " + e.getMessage());
			new HelpFormatter().printHelp("jlisa-witness-validator", options);
			RunSummary.couldNotValidate("usage", e.getMessage());
			System.exit(0);
			return;
		}

		if (cmd.hasOption("help")) {
			new HelpFormatter().printHelp("jlisa-witness-validator", options);
			System.exit(0);
			return;
		}

		// Elevate log level when --verbose is requested
		if (cmd.hasOption("verbose")) {
			Configurator.setRootLevel(Level.DEBUG);
		}

		String witnessPath = cmd.getOptionValue("witness");
		String java8Exec = cmd.getOptionValue("java");
		boolean findBug = cmd.hasOption("find-bug");

		if (!findBug && witnessPath == null) {
			usage(options, "--witness or --find-bug is required");
			return;
		}
		if (witnessPath != null && !new File(witnessPath).exists()) {
			System.err.println("Error: witness file not found: " + witnessPath);
			RunSummary.couldNotValidate("witness-not-found", witnessPath);
			System.exit(0);
			return;
		}
		ViolationWitnessWriter.Request witnessOutput = null;
		if (cmd.hasOption("witness-output")) {
			if (!findBug || !cmd.hasOption("source")) {
				usage(options, "--witness-output needs --find-bug, --source and --svcomp-property");
				return;
			}
			witnessOutput = new ViolationWitnessWriter.Request(Paths.get(cmd.getOptionValue("witness-output")),
					List.of(cmd.getOptionValues("source")), cmd.getOptionValue("svcomp-property"));
		}

		try {
			Task task = task(cmd, options);
			if (task == null) {
				return;
			}
			if (findBug) {
				String timeLimit = cmd.getOptionValue("time-limit", "60");
				long millis;
				try {
					millis = (long) (Double.parseDouble(timeLimit) * 1000);
				} catch (NumberFormatException e) {
					RunSummary.couldNotValidate("usage", "--time-limit is not a number: " + timeLimit);
					System.exit(0);
					return;
				}
				BugFinder.find(task.property(), task.sources(), java8Exec, millis, witnessOutput);
			} else {
				WitnessModel model = parseWitness(new File(witnessPath));
				LOG.info("Parsed witness: {}", model);
				switch (model.type()) {
				case VIOLATION -> ViolationValidator.validate(model, task.property(), task.sources(), java8Exec,
						cmd.hasOption("input-search"));
				case CORRECTNESS -> {
					if (task.taskFile() == null) {
						RunSummary.couldNotValidate("correctness-witness",
								"correctness witnesses are only validated with --benchmark");
					} else {
						CorrectnessValidator.validate(model, task.taskFile());
					}
				}
				}
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
	 * The task to validate or to search, with its property.
	 *
	 * @param sources  the sources of the task
	 * @param property the property
	 * @param taskFile the task file ({@code --benchmark}), or null when the
	 *                     task is given by its inputs
	 */
	private record Task(
			TaskSources sources,
			Property property,
			File taskFile) {
	}

	/**
	 * The task of the command line, given either as SV-COMP gives it to the
	 * tool ({@code --source} and {@code --svcomp-property}), or by a task file
	 * ({@code --benchmark}, {@code --property} and {@code --extra-sources}).
	 * Returns null after reporting a wrong command line.
	 */
	private static Task task(
			CommandLine cmd,
			Options options)
			throws ValidationException,
			IOException {
		boolean svcomp = cmd.hasOption("source") || cmd.hasOption("svcomp-property");
		if (svcomp && (cmd.hasOption("benchmark") || cmd.hasOption("property") || cmd.hasOption("extra-sources"))) {
			usage(options, "--source and --svcomp-property cannot be given with --benchmark, --property or "
					+ "--extra-sources");
			return null;
		}
		if (svcomp) {
			if (!cmd.hasOption("source") || !cmd.hasOption("svcomp-property")) {
				usage(options, "--source and --svcomp-property go together");
				return null;
			}
			String propertyFile = cmd.getOptionValue("svcomp-property");
			SVCOMPProperty property = SVCOMPProperty.of(Files.readString(Paths.get(propertyFile)));
			if (property == null) {
				RunSummary.couldNotValidate("unsupported-property", propertyFile);
				System.exit(0);
				return null;
			}
			return new Task(TaskSources.fromInputs(List.of(cmd.getOptionValues("source"))),
					property == SVCOMPProperty.ASSERT ? Property.ASSERT : Property.RUNTIME, null);
		}
		String benchmarkPath = cmd.getOptionValue("benchmark");
		String propertyName = cmd.getOptionValue("property");
		if (benchmarkPath == null || propertyName == null) {
			usage(options, "--source and --svcomp-property, or --benchmark and --property, are required");
			return null;
		}
		File benchmarkFile = new File(benchmarkPath);
		if (!benchmarkFile.exists()) {
			System.err.println("Error: benchmark file not found: " + benchmarkPath);
			RunSummary.couldNotValidate("benchmark-not-found", benchmarkPath);
			System.exit(0);
			return null;
		}
		String extraSourcesPath = cmd.getOptionValue("extra-sources");
		File extraSourcesDir = extraSourcesPath != null && !extraSourcesPath.isBlank() ? new File(extraSourcesPath)
				: null;
		return new Task(TaskSources.fromTaskFile(benchmarkFile, extraSourcesDir), Property.fromName(propertyName),
				benchmarkFile);
	}

	private static void usage(
			Options options,
			String message) {
		System.err.println("Error: " + message + ".");
		new HelpFormatter().printHelp("jlisa-witness-validator", options);
		RunSummary.couldNotValidate("usage", message);
		System.exit(0);
	}

	// ------------------------------------------------------------------
	// witness parsing — format detection by extension and content
	// ------------------------------------------------------------------

	/**
	 * Selects the appropriate parser based on the witness file extension.
	 * <ul>
	 * <li>{@code .graphml} → {@link WitnessParserV1}</li>
	 * <li>{@code .yaml} / {@code .yml} → {@link WitnessParserV2}</li>
	 * </ul>
	 */
	private static WitnessModel parseWitness(
			File witnessFile)
			throws ValidationException {
		String name = witnessFile.getName().toLowerCase();
		if (name.endsWith(".graphml") || name.endsWith(".xml")) {
			return WitnessParserV1.parse(witnessFile);
		}
		if (name.endsWith(".yaml") || name.endsWith(".yml")) {
			return WitnessParserV2.parse(witnessFile);
		}
		// Unknown extension — peek at the first line to decide
		try {
			String firstLine = java.nio.file.Files.lines(witnessFile.toPath())
					.filter(l -> !l.isBlank())
					.findFirst()
					.orElse("");
			if (firstLine.contains("<graphml") || firstLine.startsWith("<?xml")) {
				return WitnessParserV1.parse(witnessFile);
			}
			if (firstLine.startsWith("format_version") || firstLine.startsWith("---")) {
				return WitnessParserV2.parse(witnessFile);
			}
		} catch (java.io.IOException e) {
			throw new ValidationException("Cannot read witness file: " + e.getMessage(), e);
		}
		throw new ValidationException(
				"Cannot determine witness format from extension or content of: " + witnessFile.getName());
	}

	// ------------------------------------------------------------------
	// CLI definition
	// ------------------------------------------------------------------

	private static Options buildOptions() {
		Options opts = new Options();

		opts.addOption(Option.builder("h")
				.longOpt("help")
				.desc("Print this help message and exit")
				.build());

		opts.addOption(Option.builder("w")
				.longOpt("witness")
				.hasArg()
				.argName("FILE")
				.desc("Witness file to validate (.graphml for v1, .yaml for v2)")
				.required(false)
				.build());

		opts.addOption(Option.builder("b")
				.longOpt("benchmark")
				.hasArg()
				.argName("BENCHMARK")
				.desc("The Java benchmark yml")
				.required(false)
				.build());

		opts.addOption(Option.builder("p")
				.longOpt("property")
				.hasArg()
				.argName("NAME")
				.desc("Property to validate against: assert (valid-assert) or runtime "
						+ "(no-runtime-exception). The SV-COMP wrapper derives it from the .prp file.")
				.required(false)
				.build());

		opts.addOption(Option.builder("j")
				.longOpt("java")
				.hasArg()
				.argName("PATH")
				.desc("Path to the Java executable used to run benchmarks (default: java on PATH). "
						+ "For SV-COMP benchmarks compiled to Java 8, pass a Java 8 binary here.")
				.required(false)
				.build());

		opts.addOption(Option.builder("s")
				.longOpt("extra-sources")
				.hasArg()
				.argName("DIR")
				.desc("Root directory of additional Java sources to compile alongside the benchmark "
						+ "(e.g. the SV-COMP common/ directory containing Verifier.java). "
						+ "Sources are collected recursively and compiled into the benchmark output dir.")
				.required(false)
				.build());

		opts.addOption(Option.builder()
				.longOpt("input-search")
				.desc("When the witness does not fix every nondet call and its values do not lead to the "
						+ "violation, search for values of the other calls that do (default: off)")
				.build());

		opts.addOption(Option.builder("f")
				.longOpt("find-bug")
				.desc("Bug-finding mode: no witness; look for inputs that violate the property and print "
						+ "\"" + BugFinder.VERDICT_FOUND + "\" or \"Could not validate\"")
				.build());

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
				.desc("Bug-finding mode: write the violation witness of the violation found to this file "
						+ "(needs --source and --svcomp-property)")
				.build());

		opts.addOption(Option.builder()
				.longOpt("source")
				.hasArgs()
				.argName("INPUTS")
				.desc("The input files and directories of the task, as SV-COMP gives them to the tool "
						+ "(with --svcomp-property, instead of --benchmark)")
				.build());

		opts.addOption(Option.builder()
				.longOpt("svcomp-property")
				.hasArg()
				.argName("PRP")
				.desc("The property file of the task (with --source, instead of --property)")
				.build());

		opts.addOption(Option.builder("v")
				.longOpt("verbose")
				.desc("Enable verbose debug logging to stderr (default: off for BenchExec runs)")
				.build());

		return opts;
	}
}

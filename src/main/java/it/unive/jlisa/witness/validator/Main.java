package it.unive.jlisa.witness.validator;

import it.unive.jlisa.witness.validator.correctness.CorrectnessValidator;
import it.unive.jlisa.witness.validator.model.Property;
import it.unive.jlisa.witness.validator.parser.WitnessModel;
import it.unive.jlisa.witness.validator.parser.WitnessParserV1;
import it.unive.jlisa.witness.validator.parser.WitnessParserV2;
import it.unive.jlisa.witness.validator.violation.ViolationValidator;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.File;

/**
 * Entry point for the jLISA Witness Validator.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * jlisa-witness-validator --witness <file> --benchmark <task.yml> --property assert|runtime
 *                         [--java <path>] [--verbose]
 * }</pre>
 *
 * <p>The property is given explicitly; it is never derived from the task file.
 *
 * <h3>Output (stdout)</h3>
 * <ul>
 *   <li>{@code Witness Correct}  — the witness is valid (violation reproduced / invariants verified)</li>
 *   <li>{@code Witness Spurious} — the witness path does not lead to the property violation</li>
 *   <li>{@code Could not validate} — parse error, timeout, or unsupported witness type</li>
 * </ul>
 *
 * <p>The exit code is always 0 so that BenchExec does not misinterpret a non-zero exit as
 * a validator crash.
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
    public static void main(String[] args) {
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
        String benchmarkPath = cmd.getOptionValue("benchmark");
        String propertyName = cmd.getOptionValue("property");
        String java8Exec = cmd.getOptionValue("java");
        String extraSourcesPath = cmd.getOptionValue("extra-sources");

        if (witnessPath == null || benchmarkPath == null || propertyName == null) {
            System.err.println("Error: --witness, --benchmark and --property are required.");
            new HelpFormatter().printHelp("jlisa-witness-validator", options);
            RunSummary.couldNotValidate("usage", "--witness, --benchmark and --property are required");
            System.exit(0);
            return;
        }

        File witnessFile = new File(witnessPath);
        File benchmarkFile = new File(benchmarkPath);

        if (!witnessFile.exists()) {
            System.err.println("Error: witness file not found: " + witnessPath);
            RunSummary.couldNotValidate("witness-not-found", witnessPath);
            System.exit(0);
            return;
        }

        if (!benchmarkFile.exists()) {
            System.err.println("Error: benchmark file not found: " + benchmarkPath);
            RunSummary.couldNotValidate("benchmark-not-found", benchmarkPath);
            System.exit(0);
            return;
        }

        try {
            Property property = Property.fromName(propertyName);
            WitnessModel model = parseWitness(witnessFile);
            LOG.info("Parsed witness: {}", model);

            File extraSourcesDir = (extraSourcesPath != null && !extraSourcesPath.isBlank())
                    ? new File(extraSourcesPath)
                    : null;

            switch (model.type()) {
                case VIOLATION -> ViolationValidator.validate(model, property, benchmarkFile, java8Exec, extraSourcesDir);
                case CORRECTNESS -> CorrectnessValidator.validate(model, benchmarkFile);
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

    // ------------------------------------------------------------------
    // witness parsing — format detection by extension and content
    // ------------------------------------------------------------------

    /**
     * Selects the appropriate parser based on the witness file extension.
     * <ul>
     *   <li>{@code .graphml} → {@link WitnessParserV1}</li>
     *   <li>{@code .yaml} / {@code .yml} → {@link WitnessParserV2}</li>
     * </ul>
     */
    private static WitnessModel parseWitness(File witnessFile) throws ValidationException {
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

        opts.addOption(Option.builder("v")
                .longOpt("verbose")
                .desc("Enable verbose debug logging to stderr (default: off for BenchExec runs)")
                .build());

        return opts;
    }
}

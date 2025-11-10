package it.unive.jlisa;

import it.unive.jlisa.analysis.heap.JavaFieldSensitivePointBasedHeap;
import it.unive.jlisa.analysis.type.JavaInferredTypes;
import it.unive.jlisa.analysis.value.ConstantPropagationWithIntervals;
import it.unive.jlisa.analysis.value.Reachability;
import it.unive.jlisa.checkers.AssertCheckerConstantPropagationWithIntervals;
import it.unive.jlisa.frontend.JavaFrontend;
import it.unive.jlisa.frontend.exceptions.CSVExceptionWriter;
import it.unive.jlisa.frontend.exceptions.ParsingException;
import it.unive.jlisa.interprocedural.callgraph.JavaContextBasedAnalysis;
import it.unive.jlisa.interprocedural.callgraph.JavaKDepthToken;
import it.unive.jlisa.interprocedural.callgraph.JavaRTACallGraph;
import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.SimpleAbstractDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.interprocedural.ReturnTopPolicy;
import it.unive.lisa.program.Program;
import java.io.IOException;
import java.util.Arrays;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

public class Main {

	private static Logger LOG = org.apache.logging.log4j.LogManager.getLogger(Main.class);

	public static void main(
			String[] args)
			throws IOException,
			ParseException,
			ParsingException {

		// Define options
		Options options = new Options();

		Option helpOption = new Option("h", "help", false, "Print this help message");
		Option sourceOption = Option.builder("s")
				.longOpt("source")
				.hasArgs()
				.desc("Source files (e.g. -s file1 file2 file3)")
				.required(false) // Will validate manually if help is not used
				.build();

		Option outdirOption = Option.builder("o")
				.longOpt("outdir")
				.hasArg()
				.desc("Output directory")
				.required(false)
				.build();

		Option logLevel = Option.builder("l")
				.longOpt("log-level")
				.hasArg()
				.desc("Log level: (INFO, DEBUG, WARNING, ERROR, FATAL, TRACE, ALL, OFF)")
				.required(false)
				.build();

		Option checker = Option.builder("c")
				.longOpt("checker")
				.hasArg()
				.desc("Checker: (Assert)")
				.required(false)
				.build();

		Option numericalDomainOption = Option.builder("n")
				.longOpt("numericalDomain")
				.hasArg()
				.desc("Numerical domain: (ConstantPropagation)")
				.required(false)
				.build();

		Option mode = Option.builder("m")
				.longOpt("mode")
				.hasArg()
				.desc("Execution mode: (Statistics, Debug [DEFAULT])")
				.required(false)
				.build();

		Option version = Option.builder("v")
				.longOpt("version")
				.desc("Version of the tool")
				.required(false)
				.build();

		Option noHtmlOutput = Option.builder()
				.longOpt("no-html")
				.desc("Disable HTML output (enabled by default)")
				.required(false)
				.build();

		Option dumpExcs = Option.builder("e")
				.longOpt("dump-exceptions")
				.desc("When in statistics mode, log exceptions to standard output")
				.required(false)
				.build();

		options.addOption(noHtmlOutput);

		options.addOption(helpOption);
		options.addOption(sourceOption);
		options.addOption(outdirOption);
		options.addOption(logLevel);
		options.addOption(checker);
		options.addOption(numericalDomainOption);
		options.addOption(mode);
		options.addOption(version);
		options.addOption(noHtmlOutput);
		options.addOption(dumpExcs);
		// Create parser and formatter
		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		String[] sources = new String[0];
		String outdir = "";
		String checkerName = "", numericalDomain = "", executionMode = "Debug";
		boolean htmlOutput = true;
		boolean dumpExceptions = false;

		try {
			CommandLine cmd = parser.parse(options, args);

			// Handle help
			if (cmd.hasOption("h") || args.length == 0) {
				formatter.printHelp("jlisa", options, true);
				System.exit(0);
			}

			if (cmd.hasOption("v")) {
				Package jlisaPkg = Main.class.getPackage();
				String implementationVersion = jlisaPkg.getImplementationVersion();
				System.out.println("JLiSA version: " + implementationVersion);
				System.exit(0);
			}

			// Check required manually if help was not triggered
			if (!cmd.hasOption("s") || !cmd.hasOption("o")) {
				throw new ParseException("Missing required options: --source and/or --outdir");
			}

			// Check required manually if help was not triggered
			if (!cmd.hasOption("n") || !cmd.hasOption("o")) {
				throw new ParseException("Missing required options: --numericalDomain");
			}
			if (cmd.hasOption("l")) {
				String log4jLevelName = cmd.getOptionValue("l").toUpperCase();
				Level level = Level.getLevel(log4jLevelName);
				if (level == null) {
					throw new ParseException("Invalid log level: " + log4jLevelName);
				}
				LogManager.setLogLevel(level);
			}

			if (cmd.hasOption("no-html")) {
				htmlOutput = false;
			}

			if (cmd.hasOption("e")) {
				dumpExceptions = true;
			}

			checkerName = cmd.getOptionValue("c", "Assert");
			numericalDomain = cmd.getOptionValue("n");

			sources = cmd.getOptionValues("s");
			outdir = cmd.getOptionValue("o");
			if (!outdir.endsWith("/")) {
				outdir += "/";
			}
			// Output
			LOG.info("Source files:");
			for (String file : sources) {
				LOG.info(" - " + file);
			}

			LOG.info("Output directory: " + outdir);

			if (cmd.hasOption("m"))
				executionMode = cmd.getOptionValue("m");

		} catch (ParseException e) {
			LOG.error("Error: " + e.getMessage());
			formatter.printHelp("jlisa", options, true);
			System.exit(1);
		}

		switch (executionMode) {
		case "Debug":
			runDebug(sources, outdir, checkerName, numericalDomain, htmlOutput);
			break;
		case "Statistics":
			runStatistics(sources, outdir, checkerName, numericalDomain, htmlOutput, dumpExceptions);
			break;
		default:
			LOG.error("Unknown execution mode: " + executionMode);
			System.exit(1);
		}
	}

	private static void runDebug(
			String[] sources,
			String outdir,
			String checkerName,
			String numericalDomain,
			boolean htmlOutput)
			throws IOException,
			ParseException,
			ParsingException {
		JavaFrontend frontend = runFrontend(sources);
		runAnalysis(outdir, checkerName, numericalDomain, frontend, htmlOutput);
	}

	private static void runStatistics(
			String[] sources,
			String outdir,
			String checkerName,
			String numericalDomain,
			boolean htmlOutput,
			boolean dumpExceptions) {
		JavaFrontend frontend = null;
		try {
			frontend = runFrontend(sources);
		} catch (Throwable e) {
			CSVExceptionWriter.writeCSV(outdir + "frontend.csv", e);
			LOG.error("Some errors occurred in the frontend outside the parsing phase. Check " + outdir
					+ "/frontend.csv file.");
			if (dumpExceptions)
				e.printStackTrace(System.out);
			System.exit(1);
		}
		try {
			runAnalysis(outdir, checkerName, numericalDomain, frontend, htmlOutput);
		} catch (Throwable e) {
			CSVExceptionWriter.writeCSV(outdir + "analysis.csv", e.getCause() != null ? e.getCause() : e);
			LOG.error("Some errors occurred during the analysis. Check " + outdir + "analysis.csv file.");
			if (dumpExceptions)
				e.printStackTrace(System.out);
			System.exit(1);
		}
	}

	private static JavaFrontend runFrontend(
			String[] sources)
			throws IOException,
			ParsingException {
		JavaFrontend frontend = null;
		frontend = new JavaFrontend();
		frontend.parseFromListOfFile(Arrays.stream(sources).toList());
		return frontend;
	}

	private static void runAnalysis(
			String outdir,
			String checkerName,
			String numericalDomain,
			JavaFrontend frontend,
			boolean htmlOutput)
			throws ParseException {
		Program p = frontend.getProgram();
		LiSAConfiguration conf = new LiSAConfiguration();
		conf.workdir = outdir;
		conf.serializeResults = false;
		conf.jsonOutput = true;
		if (htmlOutput) {
			conf.analysisGraphs = LiSAConfiguration.GraphType.HTML_WITH_SUBNODES;
		}
		conf.interproceduralAnalysis = new JavaContextBasedAnalysis<>(JavaKDepthToken.getSingleton(150));
		conf.callGraph = new JavaRTACallGraph();
		conf.openCallPolicy = ReturnTopPolicy.INSTANCE;
		conf.optimize = false;
		switch (checkerName) {
		case "Assert":
			conf.semanticChecks.add(new AssertCheckerConstantPropagationWithIntervals());
			break;
		case "":
			break;
		default:
			throw new ParseException("Invalid checker name: " + checkerName);
		}
		ValueDomain<?> domain;
		switch (numericalDomain) {
		case "ConstantPropagation":
			domain = new Reachability<>(new ConstantPropagationWithIntervals());
			break;
		default:
			throw new ParseException("Invalid numerical domain name: " + numericalDomain);
		}

		conf.analysis = new SimpleAbstractDomain<>(
				new JavaFieldSensitivePointBasedHeap(),
				domain,
				new JavaInferredTypes());

		LiSA lisa = new LiSA(conf);
		lisa.run(p);
	}
}
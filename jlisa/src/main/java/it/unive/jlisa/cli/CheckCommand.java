package it.unive.jlisa.cli;

import it.unive.jlisa.Main;
import it.unive.jlisa.analysis.heap.JavaFieldSensitivePointBasedHeap;
import it.unive.jlisa.analysis.type.JavaInferredTypes;
import it.unive.jlisa.analysis.value.ConstantPropagationWithIntervals;
import it.unive.jlisa.analysis.value.ConstantPropagationWithTarsis;
import it.unive.jlisa.checkers.AssertChecker;
import it.unive.jlisa.frontend.JavaFrontend;
import it.unive.jlisa.frontend.exceptions.CSVExceptionWriter;
import it.unive.jlisa.frontend.exceptions.ParsingException;
import it.unive.jlisa.interprocedural.callgraph.JavaRTACallGraph;
import it.unive.jlisa.interprocedural.callgraph.Recursion;
import it.unive.jlisa.svcomp.ViolationWitnessProducer;
import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.SimpleAbstractDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.interprocedural.ReturnTopPolicy;
import it.unive.lisa.interprocedural.inlining.InliningAnalysis;
import it.unive.lisa.listeners.BottomTopListener;
import it.unive.lisa.listeners.CallResolutionListener;
import it.unive.lisa.outputs.HtmlResults;
import it.unive.lisa.outputs.JSONReportDumper;
import it.unive.lisa.outputs.JSONResults;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.cfg.fixpoints.forward.ForwardDescendingNarrowingFixpoint;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Logger;

/**
 * The {@code check} sub-command of jlisa: analyses a program, reports the
 * warnings of its checkers and, for each SV-COMP property file given, writes
 * the violation witness of a definite violation of the property.
 */
public final class CheckCommand {

	private static final Logger LOG = org.apache.logging.log4j.LogManager.getLogger(CheckCommand.class);

	private CheckCommand() {
	}

	/**
	 * Runs {@code jlisa check} with the arguments that follow the sub-command.
	 *
	 * @param args the arguments of the sub-command
	 *
	 * @throws IOException      if a source cannot be read
	 * @throws ParseException   if the checker or the domain is not known
	 * @throws ParsingException if the frontend fails
	 */
	public static void run(
			String[] args)
			throws IOException,
			ParseException,
			ParsingException {

		Options options = SharedOptions.create(
				"SV-COMP property file of the task. When given, a violation witness is written for a definite "
						+ "violation of the property. The option can be repeated, one witness for each property");

		Option outdirOption = Option.builder("o")
				.longOpt("outdir")
				.hasArg()
				.argName("DIR")
				.desc("Output directory")
				.required(false)
				.build();

		Option checker = Option.builder("c")
				.longOpt("checker")
				.hasArg()
				.argName("CHECKER")
				.desc("Checker: (Assert)")
				.required(false)
				.build();

		Option numericalDomainOption = Option.builder("n")
				.longOpt("numericalDomain")
				.hasArg()
				.argName("DOMAIN")
				.desc("Numerical domain: (ConstantPropagation [the same as ConstantPropagationWithTarsis], ConstantPropagationWithTarsis, ConstantPropagationWithIntervals)")
				.required(false)
				.build();

		Option mode = Option.builder("m")
				.longOpt("mode")
				.hasArg()
				.argName("MODE")
				.desc("Execution mode: (Statistics, Debug [DEFAULT])")
				.required(false)
				.build();

		Option htmlOutput = Option.builder()
				.longOpt("dump-html")
				.desc("Write the HTML pages of the analysed methods (disabled by default)")
				.required(false)
				.build();

		Option graphOutput = Option.builder()
				.longOpt("dump-graphs")
				.desc("Write the JSON graphs of the analysed methods (disabled by default); the report is always written")
				.required(false)
				.build();

		Option dumpExcs = Option.builder("e")
				.longOpt("dump-exceptions")
				.desc("When in statistics mode, log exceptions to standard output")
				.required(false)
				.build();

		Option debugInfo = Option.builder("d")
				.longOpt("debug-information")
				.desc("Produce debug information for the analysis (open calls, bottom states, ...)")
				.required(false)
				.build();

		options.addOption(outdirOption);
		options.addOption(checker);
		options.addOption(numericalDomainOption);
		options.addOption(mode);
		options.addOption(htmlOutput);
		options.addOption(graphOutput);
		options.addOption(dumpExcs);
		options.addOption(debugInfo);
		SharedOptions.markRequired(options, SharedOptions.SOURCE, "outdir", "numericalDomain");
		CommandLineParser parser = new DefaultParser();
		String[] sources = new String[0];
		String outdir = "";
		String checkerName = "", numericalDomain = "", executionMode = "Debug";
		boolean html = false;
		boolean graphs = false;
		boolean dumpExceptions = false;
		boolean debug = false;
		String[] propertyFiles = null;

		try {
			CommandLine cmd = parser.parse(options, args);

			// Handle help
			if (cmd.hasOption("h") || args.length == 0) {
				SharedOptions.printHelp("check", options, System.out);
				System.exit(0);
			}

			if (cmd.hasOption("v")) {
				System.out.println(Main.versionLine());
				System.exit(0);
			}

			if (!cmd.hasOption("s") || !cmd.hasOption("o")) {
				throw new ParseException("Missing required options: --source and/or --outdir");
			}
			if (!cmd.hasOption("n")) {
				throw new ParseException("Missing required options: --numericalDomain");
			}
			SharedOptions.applyLogLevel(cmd);

			if (cmd.hasOption("dump-html")) {
				html = true;
			}

			if (cmd.hasOption("dump-graphs")) {
				graphs = true;
			}

			if (cmd.hasOption("e")) {
				dumpExceptions = true;
			}

			if (cmd.hasOption("d")) {
				debug = true;
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

			propertyFiles = cmd.getOptionValues(SharedOptions.SVCOMP_PROPERTY);

		} catch (ParseException e) {
			LOG.error("Error: " + e.getMessage());
			SharedOptions.printHelp("check", options, System.err);
			System.exit(1);
		}

		switch (executionMode) {
		case "Debug":
			runDebug(sources, outdir, checkerName, numericalDomain, html, graphs, debug, propertyFiles);
			break;
		case "Statistics":
			runStatistics(sources, outdir, checkerName, numericalDomain, html, graphs, dumpExceptions, debug,
					propertyFiles);
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
			boolean htmlOutput,
			boolean graphOutput,
			boolean debug,
			String[] propertyFiles)
			throws IOException,
			ParseException,
			ParsingException {
		JavaFrontend frontend = runFrontend(sources);
		runAnalysis(sources, outdir, checkerName, numericalDomain, frontend, htmlOutput, graphOutput, debug,
				propertyFiles);
	}

	private static void runStatistics(
			String[] sources,
			String outdir,
			String checkerName,
			String numericalDomain,
			boolean htmlOutput,
			boolean graphOutput,
			boolean dumpExceptions,
			boolean debug,
			String[] propertyFiles) {
		JavaFrontend frontend = null;
		try {
			frontend = runFrontend(sources);
		} catch (Throwable e) {
			Throwable root = e;
			while (root.getCause() != null)
				root = root.getCause();
			CSVExceptionWriter.writeCSV(outdir + "frontend.csv", root);
			LOG.error("Some errors occurred in the frontend outside the parsing phase. Check " + outdir
					+ "/frontend.csv file.");
			if (dumpExceptions)
				root.printStackTrace(System.out);
			System.exit(1);
		}
		try {
			runAnalysis(sources, outdir, checkerName, numericalDomain, frontend, htmlOutput, graphOutput, debug,
					propertyFiles);
		} catch (Throwable e) {
			Throwable root = e;
			while (root.getCause() != null)
				root = root.getCause();
			CSVExceptionWriter.writeCSV(outdir + "analysis.csv", root);
			LOG.error("Some errors occurred during the analysis. Check " + outdir + "analysis.csv file.");
			if (dumpExceptions)
				root.printStackTrace(System.out);
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
			String[] sources,
			String outdir,
			String checkerName,
			String numericalDomain,
			JavaFrontend frontend,
			boolean htmlOutput,
			boolean graphOutput,
			boolean debug,
			String[] propertyFiles)
			throws ParseException {
		Program p = frontend.getProgram();
		List<ViolationWitnessProducer> witnesses = new ArrayList<>();
		if (propertyFiles != null)
			for (String propertyFile : propertyFiles) {
				ViolationWitnessProducer witness = ViolationWitnessProducer.load(Arrays.asList(sources), propertyFile);
				// a property given twice has a single witness
				if (witness != null
						&& witnesses.stream().noneMatch(other -> other.getProperty() == witness.getProperty()))
					witnesses.add(witness);
			}
		LiSAConfiguration conf = new LiSAConfiguration();
		conf.workdir = outdir;
		if (graphOutput)
			conf.outputs.add(new JSONResults<>());
		conf.outputs.add(new JSONReportDumper());
		conf.interproceduralAnalysis = new InliningAnalysis<>(200, false);
		conf.callGraph = new JavaRTACallGraph();
		conf.openCallPolicy = ReturnTopPolicy.INSTANCE;
		conf.wideningThreshold = 20;
		// a descending phase with narrowing after the fixpoint with widening,
		// only without recursion: the inlining analysis rejects a second
		// result for a recursive call
		if (!Recursion.mayBeRecursive(p))
			conf.forwardDescendingFixpoint = new ForwardDescendingNarrowingFixpoint<>();
		switch (checkerName) {
		case "Assert":
			conf.semanticChecks.add(witnesses.isEmpty() ? new AssertChecker<>()
					: new AssertChecker<>(violation -> witnesses.forEach(witness -> witness.accept(violation))));
			break;
		case "":
			break;
		default:
			throw new ParseException("Invalid checker name: " + checkerName);
		}
		ValueDomain<?> domain;
		switch (numericalDomain) {
		case "ConstantPropagation":
		case "ConstantPropagationWithTarsis":
			domain = new ConstantPropagationWithTarsis();
			break;
		case "ConstantPropagationWithIntervals":
			domain = new ConstantPropagationWithIntervals();
			break;
		default:
			throw new ParseException("Invalid numerical domain name: " + numericalDomain);
		}

		conf.analysis = new SimpleAbstractDomain<>(
				new JavaFieldSensitivePointBasedHeap(),
				domain,
				new JavaInferredTypes());

		if (htmlOutput)
			conf.outputs.add(new HtmlResults<>(true));

		if (debug) {
			conf.asynchronousListeners.add(new BottomTopListener(false));
			conf.asynchronousListeners.add(new CallResolutionListener());
		}

		LiSA lisa = new LiSA(conf);
		lisa.run(p);

		// write the witnesses
		for (ViolationWitnessProducer witness : witnesses)
			witness.write(outdir);
	}
}

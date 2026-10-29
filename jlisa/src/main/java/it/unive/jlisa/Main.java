package it.unive.jlisa;

import it.unive.jlisa.analysis.heap.JavaFieldSensitivePointBasedHeap;
import it.unive.jlisa.analysis.type.JavaInferredTypes;
import it.unive.jlisa.analysis.value.ConstantPropagationWithApron;
import it.unive.jlisa.analysis.value.ConstantPropagationWithIntervals;
import it.unive.jlisa.checkers.AssertChecker;
import it.unive.jlisa.frontend.JavaFrontend;
import it.unive.jlisa.frontend.exceptions.CSVExceptionWriter;
import it.unive.jlisa.frontend.exceptions.ParsingException;
import it.unive.jlisa.interprocedural.callgraph.JavaRTACallGraph;
import it.unive.jlisa.interprocedural.callgraph.Recursion;
import it.unive.jlisa.svcomp.ViolationWitnessProducer;
import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.SimpleAbstractDomain;
import it.unive.lisa.analysis.apron.Apron;
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
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

public class Main {

	private static Logger LOG = org.apache.logging.log4j.LogManager.getLogger(Main.class);

	// whether the statistics mode writes the JSON graphs of the analyzed
	// methods (option --no-graphs)
	private static boolean jsonGraphs = true;

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
				.desc("Numerical domain: (ConstantPropagation, ApronPolka, ApronPplite, ApronPplite-<kind of PPLite, e.g., UF_Poly>, ApronOctagon)")
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

		Option noGraphs = Option.builder()
				.longOpt("no-graphs")
				.desc("When in statistics mode, do not write the JSON graphs of the analyzed methods (written by default)")
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

		Option svcompProperty = Option.builder()
				.longOpt("svcomp-property")
				.hasArg()
				.desc("SV-COMP property file of the task. When given, a violation witness is written for a definite violation of the property. The option can be repeated, one witness for each property")
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
		options.addOption(debugInfo);
		options.addOption(svcompProperty);
		options.addOption(noGraphs);
		// Create parser and formatter
		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		String[] sources = new String[0];
		String outdir = "";
		String checkerName = "", numericalDomain = "", executionMode = "Debug";
		boolean htmlOutput = true;
		boolean dumpExceptions = false;
		boolean debug = false;
		String[] propertyFiles = null;

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

			if (cmd.hasOption("no-graphs")) {
				jsonGraphs = false;
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

			propertyFiles = cmd.getOptionValues("svcomp-property");

		} catch (ParseException e) {
			LOG.error("Error: " + e.getMessage());
			formatter.printHelp("jlisa", options, true);
			System.exit(1);
		}

		switch (executionMode) {
		case "Debug":
			runDebug(sources, outdir, checkerName, numericalDomain, htmlOutput, debug, propertyFiles);
			break;
		case "Statistics":
			runStatistics(sources, outdir, checkerName, numericalDomain, htmlOutput, dumpExceptions, debug,
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
			boolean debug,
			String[] propertyFiles)
			throws IOException,
			ParseException,
			ParsingException {
		JavaFrontend frontend = runFrontend(sources);
		runAnalysis(sources, outdir, checkerName, numericalDomain, frontend, htmlOutput, debug, propertyFiles);
	}

	private static void runStatistics(
			String[] sources,
			String outdir,
			String checkerName,
			String numericalDomain,
			boolean htmlOutput,
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
			runAnalysis(sources, outdir, checkerName, numericalDomain, frontend, htmlOutput, debug, propertyFiles);
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
		if (jsonGraphs)
			conf.outputs.add(new JSONResults<>());
		conf.outputs.add(new JSONReportDumper());
		conf.interproceduralAnalysis = new InliningAnalysis<>(150, false);
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
			domain = new ConstantPropagationWithIntervals();
			break;
		case "ApronPolka":
			domain = withApron(Apron.ApronDomain.Polka);
			break;
		case "ApronPplite":
			domain = withApron(Apron.ApronDomain.Pplite);
			break;
		case "ApronOctagon":
			domain = withApron(Apron.ApronDomain.Octagon);
			break;
		default:
			if (!numericalDomain.startsWith("ApronPplite-"))
				throw new ParseException("Invalid numerical domain name: " + numericalDomain);
			// e.g., ApronPplite-UF_Poly: a kind of polyhedra of PPLite
			domain = withApron(Apron.ApronDomain.Pplite, numericalDomain.substring("ApronPplite-".length()));
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

	/**
	 * Returns the product of {@link ConstantPropagationWithIntervals} and of
	 * the given domain of Apron. The native libraries of Apron are loaded from
	 * the folder given by the system property {@code jlisa.native}, or else
	 * from {@code java.library.path}.
	 *
	 * @param apronDomain the domain of Apron
	 *
	 * @return the product
	 *
	 * @throws ParseException if the native libraries of Apron cannot be loaded
	 */
	private static ValueDomain<?> withApron(
			Apron.ApronDomain apronDomain)
			throws ParseException {
		return withApron(apronDomain, null);
	}

	/**
	 * Returns the product of {@link ConstantPropagationWithIntervals} and of
	 * the given domain of Apron, as {@link #withApron(Apron.ApronDomain)}, with
	 * the given kind of polyhedra of PPLite.
	 *
	 * @param apronDomain the domain of Apron
	 * @param ppliteKind  the kind of polyhedra of PPLite (see
	 *                        {@link #setPpliteKind(String)}), or {@code null}
	 *                        for the default one
	 *
	 * @return the product
	 *
	 * @throws ParseException if the native libraries of Apron cannot be loaded
	 *                            or the kind cannot be set
	 */
	private static ValueDomain<?> withApron(
			Apron.ApronDomain apronDomain,
			String ppliteKind)
			throws ParseException {
		String folder = System.getProperty("jlisa.native");
		if (folder != null)
			Apron.loadLibrary(folder);
		else
			Apron.loadLibrary();
		if (!Apron.isAvailable())
			throw new ParseException("The native libraries of Apron cannot be loaded");
		Apron.setManager(apronDomain);
		// before any state of Apron is created
		if (ppliteKind != null)
			setPpliteKind(ppliteKind);
		return new ConstantPropagationWithApron();
	}

	/**
	 * Sets the kind of polyhedra of the PPLite manager of Apron (e.g.,
	 * {@code F_Poly}, factored polyhedra, whose variables are split in
	 * independent blocks; {@code UF_Poly}, that also leaves out the
	 * unconstrained variables), before any state is created. The manager is
	 * private in {@link Apron}: it is reached by reflection.
	 *
	 * @param kind the kind: Poly, F_Poly, U_Poly, UF_Poly, P_Set or FP_Set
	 *
	 * @throws ParseException if the kind cannot be set
	 */
	private static void setPpliteKind(
			String kind)
			throws ParseException {
		try {
			java.lang.reflect.Field field = Apron.class.getDeclaredField("manager");
			field.setAccessible(true);
			apron.Pplite manager = (apron.Pplite) field.get(null);
			manager.setKind(kind);
			if (!kind.equals(manager.getKind()))
				throw new ParseException("PPLite did not accept the kind " + kind);
		} catch (ReflectiveOperationException | ClassCastException e) {
			throw new ParseException("Cannot set the kind of PPLite: " + e);
		}
	}
}

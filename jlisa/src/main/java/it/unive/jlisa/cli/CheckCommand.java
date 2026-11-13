package it.unive.jlisa.cli;

import it.unive.jlisa.Main;
import it.unive.jlisa.analysis.heap.JavaFieldSensitivePointBasedHeap;
import it.unive.jlisa.analysis.type.JavaInferredTypes;
import it.unive.jlisa.analysis.value.ConstantPropagation;
import it.unive.jlisa.analysis.value.ConstantPropagationWithIntervals;
import it.unive.jlisa.analysis.value.ConstantPropagationWithTarsis;
import it.unive.jlisa.analysis.value.JavaNumericInterval;
import it.unive.jlisa.analysis.value.JavaTarsis;
import it.unive.jlisa.analysis.value.JavaUpperBounds;
import it.unive.jlisa.analysis.value.JavaWholeValueAnalysis;
import it.unive.jlisa.checkers.AssertChecker;
import it.unive.jlisa.checkers.Violation;
import it.unive.jlisa.checkers.ViolationKind;
import it.unive.jlisa.frontend.JavaFrontend;
import it.unive.jlisa.frontend.exceptions.CSVExceptionWriter;
import it.unive.jlisa.frontend.exceptions.ParsingException;
import it.unive.jlisa.interprocedural.JavaEntryStateContextBasedAnalysis;
import it.unive.jlisa.interprocedural.callgraph.JavaRTACallGraph;
import it.unive.jlisa.interprocedural.callgraph.Recursion;
import it.unive.jlisa.svcomp.SVCOMPProperty;
import it.unive.jlisa.svcomp.SVCOMPVerifier;
import it.unive.jlisa.svcomp.ViolationWitnessProducer;
import it.unive.jlisa.svcomp.WitnessCertifier;
import it.unive.lisa.LiSA;
import it.unive.lisa.LiSAReport;
import it.unive.lisa.analysis.SimpleAbstractDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.checks.verification.ErrorViolation;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.interprocedural.ReturnTopPolicy;
import it.unive.lisa.interprocedural.inlining.InliningAnalysis;
import it.unive.lisa.listeners.BottomTopListener;
import it.unive.lisa.listeners.CallResolutionListener;
import it.unive.lisa.outputs.HtmlResults;
import it.unive.lisa.outputs.JSONReportDumper;
import it.unive.lisa.outputs.JSONResults;
import it.unive.lisa.outputs.messages.Message;
import it.unive.lisa.outputs.messages.StatementMessage;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.cfg.fixpoints.forward.ForwardDescendingNarrowingFixpoint;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.triage.reachability.ReachabilityVerifier;
import it.unive.lisa.triage.smt.SmtLibProcessSolver;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

	/**
	 * The time by which the verification of the warnings must end (as
	 * {@link System#currentTimeMillis()} gives it), when a time limit is given
	 * ({@code --time-limit}), or {@code 0}.
	 */
	private static long verificationDeadline = 0;

	/**
	 * Whether the candidate confirmations of the warnings that stay possible
	 * are certified by validating their violation witnesses (enabled by
	 * {@code --certification}).
	 */
	private static boolean certification = false;

	/**
	 * The time left, out of the time limit, for writing the results after the
	 * verification of the warnings, in milliseconds: the fraction
	 * {@value #RESULTS_FRACTION} of the limit, at most
	 * {@value #MAX_RESULTS_MILLIS}.
	 */
	private static final long MAX_RESULTS_MILLIS = 30_000;

	private static final double RESULTS_FRACTION = 0.1;

	/**
	 * The maximum number of recursive calls unrolled for each recursive method
	 * (by both interprocedural analyses), after which its recursion is solved
	 * instead of unrolled further.
	 */
	private static final int MAX_RECURSIVE_FIXPOINTS = 1000;

	// the default bound of the unrollings of a recursive call (see
	// JavaEntryStateContextBasedAnalysis)
	private static final int ENTRY_STATE_BOUND = 200;

	// the interprocedural analysis: Inlining, or EntryState[:bound]
	private static String interprocedural = "EntryState";

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
				.desc("Numerical domain: (ConstantPropagation [the same as JavaWholeValueTarsis], ConstantPropagationWithTarsis, ConstantPropagationWithIntervals, JavaWholeValueTarsis)")
				.required(false)
				.build();

		Option verifierOption = Option.builder("t")
				.longOpt("verifier")
				.hasArg()
				.argName("VERIFIER")
				.desc("Verifier of the warnings: (Reachability [DEFAULT], verifying them through the results of "
						+ "the analysis; Combined, the SV-COMP verifier, falling back to model checking with z3)")
				.required(false)
				.build();

		Option interproceduralOption = Option.builder("i")
				.longOpt("interprocedural")
				.hasArg()
				.argName("ANALYSIS")
				.desc("Interprocedural analysis: (EntryState[:bound] [DEFAULT], unrolling recursive calls while their "
						+ "entry state is new, up to bound times [DEFAULT " + ENTRY_STATE_BOUND + "], and solving them "
						+ "with a fixpoint otherwise; Inlining, inlining calls up to a depth of 200)")
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
		options.addOption(verifierOption);
		options.addOption(interproceduralOption);
		options.addOption(mode);
		options.addOption(htmlOutput);
		options.addOption(graphOutput);
		options.addOption(dumpExcs);
		options.addOption(debugInfo);
		Option z3Path = Option.builder()
				.longOpt("z3")
				.hasArg()
				.argName("PATH")
				.desc("Path to the z3 executable used to verify warnings (by default, z3 is searched in the PATH; without it, warnings that cannot be verified through the results of the analysis stay possible)")
				.required(false)
				.build();
		options.addOption(z3Path);
		Option timeLimit = Option.builder()
				.longOpt("time-limit")
				.hasArg()
				.argName("SECONDS")
				.desc("Time limit of the whole run, in seconds: the verification of the warnings ends in time to "
						+ "write the results (warnings not verified by then stay possible). No limit by default")
				.required(false)
				.build();
		options.addOption(timeLimit);
		Option certificationOption = Option.builder()
				.longOpt("certification")
				.desc("Certify the counterexamples that the verification cannot confirm by validating their "
						+ "violation witnesses, when a property file is given (disabled by default)")
				.required(false)
				.build();
		options.addOption(certificationOption);
		SharedOptions.markRequired(options, SharedOptions.SOURCE, "outdir", "numericalDomain");
		CommandLineParser parser = new DefaultParser();
		String[] sources = new String[0];
		String outdir = "";
		String checkerName = "", numericalDomain = "", verifierName = "Reachability", executionMode = "Debug";
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
			verifierName = cmd.getOptionValue("t", "Reachability");
			if (!verifierName.equals("Reachability") && !verifierName.equals("Combined"))
				throw new ParseException("Invalid verifier name: " + verifierName);

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

			if (cmd.hasOption("z3"))
				SmtLibProcessSolver.setZ3Path(cmd.getOptionValue("z3"));

			certification = cmd.hasOption("certification");

			interprocedural = cmd.getOptionValue("i", "EntryState");
			if (!interprocedural.equals("Inlining") && !interprocedural.matches("EntryState(:[1-9][0-9]*)?"))
				throw new ParseException("Invalid interprocedural analysis: " + interprocedural);

			if (cmd.hasOption("time-limit")) {
				long limit;
				try {
					limit = Long.parseLong(cmd.getOptionValue("time-limit")) * 1000;
				} catch (NumberFormatException e) {
					throw new ParseException("Invalid time limit: " + cmd.getOptionValue("time-limit"));
				}
				if (limit <= 0)
					throw new ParseException("Invalid time limit: " + cmd.getOptionValue("time-limit"));
				// the limit counts from the start of the virtual machine
				long start = ManagementFactory.getRuntimeMXBean().getStartTime();
				verificationDeadline = start + limit
						- Math.min(MAX_RESULTS_MILLIS, (long) (limit * RESULTS_FRACTION));
			}

		} catch (ParseException e) {
			LOG.error("Error: " + e.getMessage());
			SharedOptions.printHelp("check", options, System.err);
			System.exit(1);
		}

		switch (executionMode) {
		case "Debug":
			runDebug(sources, outdir, checkerName, numericalDomain, verifierName, html, graphs, debug, propertyFiles);
			break;
		case "Statistics":
			runStatistics(sources, outdir, checkerName, numericalDomain, verifierName, html, graphs, dumpExceptions,
					debug, propertyFiles);
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
			String verifierName,
			boolean htmlOutput,
			boolean graphOutput,
			boolean debug,
			String[] propertyFiles)
			throws IOException,
			ParseException,
			ParsingException {
		JavaFrontend frontend = runFrontend(sources);
		runAnalysis(sources, outdir, checkerName, numericalDomain, verifierName, frontend, htmlOutput, graphOutput,
				debug, propertyFiles);
	}

	private static void runStatistics(
			String[] sources,
			String outdir,
			String checkerName,
			String numericalDomain,
			String verifierName,
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
			runAnalysis(sources, outdir, checkerName, numericalDomain, verifierName, frontend, htmlOutput, graphOutput,
					debug, propertyFiles);
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
			String verifierName,
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

		// recursions are unrolled up to 200 frames, and solved when deeper or
		// when their call trees grow too large (e.g., fib(n - 1) + fib(n - 2))
		if (interprocedural.startsWith("EntryState"))
			conf.interproceduralAnalysis = new JavaEntryStateContextBasedAnalysis<>(interprocedural.contains(":")
					? Integer.parseInt(interprocedural.substring(interprocedural.indexOf(':') + 1))
					: ENTRY_STATE_BOUND, MAX_RECURSIVE_FIXPOINTS);
		else
			conf.interproceduralAnalysis = new InliningAnalysis<>(200, false, MAX_RECURSIVE_FIXPOINTS);

		conf.callGraph = new JavaRTACallGraph();
		conf.openCallPolicy = ReturnTopPolicy.INSTANCE;
		conf.wideningThreshold = 20;
		// a descending phase with narrowing after the fixpoint with widening,
		// only without recursion: the inlining analysis rejects a second
		// result for a recursive call
		if (!Recursion.mayBeRecursive(p))
			conf.forwardDescendingFixpoint = new ForwardDescendingNarrowingFixpoint<>();
		// warnings are verified after the checks
		SVCOMPVerifier<?, ?> svcomp = null;
		switch (verifierName) {
		case "Reachability":
			conf.warningVerifier = new ReachabilityVerifier<>();
			break;
		case "Combined":
			svcomp = new SVCOMPVerifier<>(SVCOMPVerifier.firstViolation().withDeadline(verificationDeadline));
			conf.warningVerifier = svcomp;
			break;
		default:
			throw new ParseException("Invalid verifier name: " + verifierName);
		}
		AssertChecker<?> checker = null;
		switch (checkerName) {
		case "Assert":
			// with no property, or if the properties are unknown, both kinds
			// of violations are checked; otherwise, only the ones of the
			// given properties, since the others are not looked at
			boolean assertions = witnesses.isEmpty()
					|| witnesses.stream().anyMatch(w -> w.getProperty() == SVCOMPProperty.ASSERT);
			boolean exceptions = witnesses.isEmpty()
					|| witnesses.stream().anyMatch(w -> w.getProperty() == SVCOMPProperty.RUNTIME_EXCEPTION);
			checker = new AssertChecker<>(assertions, exceptions);
			conf.semanticChecks.add(checker);
			break;
		case "":
			break;
		default:
			throw new ParseException("Invalid checker name: " + checkerName);
		}
		// the candidate confirmations of the warnings that stay possible are
		// certified by validating their witnesses, as SV-COMP does
		if (certification && svcomp != null && checker != null && !witnesses.isEmpty()) {
			Map<SVCOMPProperty, String> files = new LinkedHashMap<>();
			for (String propertyFile : propertyFiles) {
				ViolationWitnessProducer witness = ViolationWitnessProducer.load(Arrays.asList(sources), propertyFile);
				if (witness != null)
					files.putIfAbsent(witness.getProperty(), propertyFile);
			}
			AssertChecker<?> warnings = checker;
			svcomp.setCertifier(new WitnessCertifier(Arrays.asList(sources), files,
					warning -> violationOf(warning, warnings), verificationDeadline));
		}
		ValueDomain<?> domain;
		switch (numericalDomain) {
		case "ConstantPropagationWithTarsis":
			domain = new ConstantPropagationWithTarsis();
			break;
		case "ConstantPropagationWithIntervals":
			domain = new ConstantPropagationWithIntervals();
			break;
		case "JavaWholeValueTarsis":
			// the participants of ConstantPropagationWithTarsis, combined by
			// the whole-value analysis
			domain = new JavaWholeValueAnalysis(new ConstantPropagation(), new JavaNumericInterval(),
					new JavaTarsis());
			break;
		case "JavaWholeValueUB":
			// JavaWholeValueTarsis with the upper bounds of the Pentagons (x <
			// y): with the intervals, the Pentagons of Logozzo and Fähndrich
			domain = new JavaWholeValueAnalysis(new ConstantPropagation(), new JavaNumericInterval(),
					new JavaUpperBounds(), new JavaTarsis());
			break;
		case "ConstantPropagation":
		case "JavaWholeValueUBLE":
			// JavaWholeValueUB with the relations x <= y too
			domain = new JavaWholeValueAnalysis(new ConstantPropagation(), new JavaNumericInterval(),
					new JavaUpperBounds(true), new JavaTarsis());
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
		LiSAReport report = lisa.run(p);

		// the violations confirmed by the verification of the warnings
		if (checker != null && !witnesses.isEmpty())
			for (Violation violation : confirmedViolations(report, checker))
				witnesses.forEach(witness -> witness.accept(violation));

		// write the witnesses
		for (ViolationWitnessProducer witness : witnesses)
			witness.write(outdir);
	}

	/**
	 * Yields the violations reported by the warnings of the given checker that
	 * have been confirmed by the verification of the warnings, with the inputs
	 * reproducing them, if known. The target of an uncaught runtime exception
	 * is the statement raising it, as recorded by the checker; the target of a
	 * failing assertion is the assertion itself.
	 *
	 * @param report  the report of the analysis
	 * @param checker the checker that issued the warnings
	 *
	 * @return the violations
	 */
	private static List<Violation> confirmedViolations(
			LiSAReport report,
			AssertChecker<?> checker) {
		List<Violation> violations = new ArrayList<>();
		for (Message warning : report.getWarnings()) {
			if (!warning.isDefinite())
				continue;
			Violation violation = violationOf(warning, checker);
			if (violation != null)
				violations.add(violation);
		}
		return violations;
	}

	/**
	 * Yields the violation reported by the given warning of the given checker,
	 * with the inputs reproducing it, if known. The target of an uncaught
	 * runtime exception is the statement raising it, as recorded by the
	 * checker; the target of a failing assertion is the assertion itself.
	 *
	 * @param warning the warning
	 * @param checker the checker that issued the warning
	 *
	 * @return the violation, or {@code null} if the warning reports none, or if
	 *             its target is unknown
	 */
	private static Violation violationOf(
			Message warning,
			AssertChecker<?> checker) {
		if (warning.getViolation() == null || !(warning instanceof StatementMessage))
			return null;
		Statement target;
		ViolationKind kind;
		if (warning.getViolation() instanceof ErrorViolation) {
			kind = ViolationKind.RUNTIME_EXCEPTION;
			target = checker.runtimeExceptionTarget(warning).orElse(null);
		} else {
			kind = ViolationKind.ASSERT;
			target = ((StatementMessage) warning).getStatement();
		}
		return target == null ? null : new Violation(kind, target, warning.getReproducingInputs());
	}
}

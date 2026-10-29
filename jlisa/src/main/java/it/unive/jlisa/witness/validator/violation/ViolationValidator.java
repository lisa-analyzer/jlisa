package it.unive.jlisa.witness.validator.violation;

import com.sun.jdi.VirtualMachine;
import it.unive.jlisa.witness.validator.RunSummary;
import it.unive.jlisa.witness.validator.ValidationException;
import it.unive.jlisa.witness.validator.model.BenchmarkProperties;
import it.unive.jlisa.witness.validator.model.Interception;
import it.unive.jlisa.witness.validator.model.Property;
import it.unive.jlisa.witness.validator.model.ViolationPlan;
import it.unive.jlisa.witness.validator.parser.BenchmarkPropertiesParser;
import it.unive.jlisa.witness.validator.parser.WitnessModel;
import it.unive.jlisa.witness.validator.search.InputSearch;
import it.unive.jlisa.witness.validator.violation.EventLoop.ValidationResult;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Orchestrates violation witness validation using the JDI runtime-steering
 * approach.
 * <h3>High-level flow</h3>
 * <ol>
 * <li>Locate Java sources in the benchmark directory</li>
 * <li>Compile them with {@code --release 8 -g} via
 * {@link JdiLauncher#compileIfNeeded} (no shell invocation)</li>
 * <li>Find the main class (the class with a {@code public static void main}
 * method)</li>
 * <li>Launch the target JVM under JDI with {@link JdiLauncher#launch}</li>
 * <li>Run the {@link EventLoop}, which steers execution according to the
 * {@link ViolationPlan} extracted from the witness</li>
 * <li>Map the {@link ValidationResult} to a BenchExec verdict string and print
 * to stdout</li>
 * </ol>
 */
public final class ViolationValidator {

	private static final String VERDICT_CORRECT = "Witness Correct";
	private static final String VERDICT_SPURIOUS = "Witness Spurious";
	private static final String VERDICT_ERROR = "Tool Error";
	private static final String VERDICT_UNKNOWN = "Could not validate";

	/**
	 * Time for the first run and the input search together, out of BenchExec's
	 * 90 s; the rest is left for compiling, starting and stopping.
	 */
	private static final long TIME_BUDGET_MILLIS = 75_000;

	private ViolationValidator() {
	}

	/**
	 * Validates the violation witness and prints the verdict to {@code stdout}.
	 *
	 * @param model           the parsed witness model
	 * @param property        the property the witness is validated against
	 * @param benchmarkFile   directory containing the benchmark Java sources /
	 *                            classes
	 * @param java8Exec       path to the Java 8 executable, or {@code null} to
	 *                            use {@code PATH}
	 * @param extraSourcesDir optional root directory of additional sources to
	 *                            compile alongside the benchmark (e.g. the
	 *                            SV-COMP {@code common/} directory containing
	 *                            {@code Verifier.java}); may be {@code null}
	 * @param inputSearch     whether to search for values of the nondet calls
	 *                            the witness does not fix, when the first run
	 *                            does not reach the violation
	 * 
	 * @throws ValidationException propagated to {@code Main}, which maps it to
	 *                                 {@code Tool Error}
	 */
	public static void validate(
			WitnessModel model,
			Property property,
			File benchmarkFile,
			String java8Exec,
			File extraSourcesDir,
			boolean inputSearch)
			throws ValidationException {

		long start = System.currentTimeMillis();
		ViolationPlan plan = model.violationPlan();
		BenchmarkProperties properties;
		try {
			properties = BenchmarkPropertiesParser.parse(benchmarkFile);
		} catch (FileNotFoundException e) {
			throw new ValidationException(e.getMessage());
		}
		File benchmarkDir = properties.directory();

		ValidatorLogger.plan("Plan: {}", plan);
		for (var ix : plan.interceptions()) {
			ValidatorLogger.plan("  Interception: {}", ix);
		}
		for (var bd : plan.branchDecisions()) {
			ValidatorLogger.plan("  BranchDecision: {}", bd);
		}
		for (var ap : plan.avoidPoints()) {
			ValidatorLogger.plan("  AvoidPoint: {}", ap);
		}
		for (var tp : plan.targetPoints()) {
			ValidatorLogger.plan("  TargetPoint: {}", tp);
		}

		if (plan.isEmpty()) {
			RunSummary.count("plan.empty");
			ValidatorLogger.warn("[VALIDATE] Violation plan is empty — cannot steer execution");
			// printVerdict(VERDICT_UNKNOWN);
			// return;
		}

		// Compile if needed (including any extra sources such as Verifier.java
		// from common/)
		File classOutputDir = benchmarkDir;

		List<File> extraSources = new ArrayList<>();
		extraSources.add(extraSourcesDir);
		extraSources.addAll(properties.inputFiles());
		JdiLauncher.compileIfNeeded(benchmarkDir, classOutputDir, null, extraSources);

		// Build classpath
		String classpath = buildClasspath(benchmarkDir);

		// Find the main class
		String mainClass = findMainClass(benchmarkDir);
		ValidatorLogger.jdi("Main class: {}", mainClass);

		// Launch the target JVM
		VirtualMachine vm = JdiLauncher.launch(mainClass, classpath, java8Exec);

		// The witness values, as they are before the first run consumes them
		// (for the search)
		List<Interception> witnessValues = plan.interceptions().stream()
				.map(ix -> new Interception(ix.className(), ix.methodName(), ix.descriptor(), ix.returnValue(),
						ix.count()))
				.toList();

		// Run the event loop
		EventLoop loop = new EventLoop(vm, plan, property);
		ValidationResult result = loop.run();

		// No violation, but the run chose values of its own for calls the
		// witness says nothing
		// about: look for values of those calls that do violate the property
		// where the witness says.
		if (inputSearch && (result == ValidationResult.UNKNOWN || result == ValidationResult.ERROR)
				&& loop.freeNondetCalls() > 0 && !loop.witnessValueNotApplied() && plan.skippedSteps() == 0) {
			result = searchInputs(mainClass, classpath, java8Exec, extraSources, property, plan, witnessValues,
					start + TIME_BUDGET_MILLIS, result);
		}

		// Map result to BenchExec verdict
		String verdict = switch (result) {
		case CORRECT -> VERDICT_CORRECT;
		case SPURIOUS -> VERDICT_SPURIOUS;
		case UNKNOWN -> VERDICT_UNKNOWN;
		case ERROR -> VERDICT_ERROR;
		};

		ValidatorLogger.result(verdict);
		RunSummary.print(result.name().toLowerCase());
		printVerdict(verdict);
	}

	// ------------------------------------------------------------------
	// private helpers
	// ------------------------------------------------------------------

	/**
	 * Runs the input search; returns CORRECT if it finds a violation, else the
	 * result it was given.
	 */
	private static ValidationResult searchInputs(
			String mainClass,
			String classpath,
			String java8Exec,
			List<File> sourceDirs,
			Property property,
			ViolationPlan plan,
			List<Interception> witnessValues,
			long deadline,
			ValidationResult otherwise)
			throws ValidationException {
		Optional<InputSearch.Found> found;
		try {
			found = new InputSearch(mainClass, classpath, java8Exec, sourceDirs, property,
					plan.targetPoints(), witnessValues).search(deadline);
		} catch (IOException e) {
			ValidatorLogger.warn("[SEARCH] cannot read the sources: {}", e.getMessage());
			return otherwise;
		}
		if (found.isEmpty()) {
			return otherwise;
		}
		InputSearch.Found f = found.get();
		ValidatorLogger.result("Input search: {} in run {}, values {}", f.detail(), f.runs(), f.values());
		RunSummary.reason("search-violation", f.detail() + " with values of the calls the witness does not fix "
				+ f.values() + " (search run " + f.runs() + ")");
		return ValidationResult.CORRECT;
	}

	/**
	 * Builds the classpath string for the target JVM by collecting all
	 * directories that contain {@code .class} files under {@code benchmarkDir}.
	 */
	static String buildClasspath(
			File benchmarkDir) {
		// Include the benchmark directory and any immediate subdirectories with
		// classes
		StringBuilder cp = new StringBuilder(benchmarkDir.getAbsolutePath());
		File[] subdirs = benchmarkDir.listFiles(File::isDirectory);
		if (subdirs != null) {
			for (File sub : subdirs) {
				File[] classes = sub.listFiles((
						d,
						n) -> n.endsWith(".class"));
				if (classes != null && classes.length > 0) {
					cp.append(File.pathSeparator).append(sub.getAbsolutePath());
				}
			}
		}
		return cp.toString();
	}

	/**
	 * Finds the fully-qualified name of the class declaring
	 * {@code public static void main(String[])}.
	 * <p>
	 * Strategy:
	 * <ol>
	 * <li>Look for a file named {@code Main.java} (most common in SV-COMP)</li>
	 * <li>Scan all {@code .java} files for a {@code public static void main}
	 * method</li>
	 * </ol>
	 */
	static String findMainClass(
			File benchmarkDir)
			throws ValidationException {
		try {
			// Prefer Main.java
			File mainFile = new File(benchmarkDir, "Main.java");
			if (mainFile.exists()) {
				return extractClassName(mainFile);
			}

			// Scan all .java files
			File[] sources = benchmarkDir.listFiles((
					dir,
					name) -> name.endsWith(".java"));
			if (sources == null || sources.length == 0) {
				throw new ValidationException("No Java sources found in " + benchmarkDir.getAbsolutePath());
			}

			for (File src : sources) {
				String content = Files.readString(src.toPath());
				if (content.contains("public static void main")) {
					return extractClassName(src);
				}
			}

			// Last resort: use the first .java file's class name
			return extractClassName(sources[0]);

		} catch (IOException e) {
			throw new ValidationException("Error reading benchmark sources: " + e.getMessage(), e);
		}
	}

	/**
	 * Extracts the fully-qualified class name from a Java source file by
	 * reading the {@code package} declaration and the file name.
	 */
	private static String extractClassName(
			File javaFile)
			throws IOException {
		String content = Files.readString(javaFile.toPath());
		String simpleName = javaFile.getName().replace(".java", "");

		// Find package declaration
		for (String line : content.split("\\r?\\n")) {
			String trimmed = line.trim();
			if (trimmed.startsWith("package ") && trimmed.endsWith(";")) {
				String pkg = trimmed.substring(8, trimmed.length() - 1).trim();
				return pkg + "." + simpleName;
			}
		}

		return simpleName; // default package
	}

	/**
	 * Writes the verdict to {@code stdout} (the only channel BenchExec reads).
	 */
	private static void printVerdict(
			String verdict) {
		System.out.println(verdict);
		System.out.flush();
	}
}

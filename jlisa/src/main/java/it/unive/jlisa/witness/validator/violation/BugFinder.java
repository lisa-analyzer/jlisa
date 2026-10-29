package it.unive.jlisa.witness.validator.violation;

import it.unive.jlisa.witness.validator.RunSummary;
import it.unive.jlisa.witness.validator.ValidationException;
import it.unive.jlisa.witness.validator.model.BenchmarkProperties;
import it.unive.jlisa.witness.validator.model.Property;
import it.unive.jlisa.witness.validator.parser.BenchmarkPropertiesParser;
import it.unive.jlisa.witness.validator.search.InputSearch;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Bug-finding mode: looks for a violation of the property without a witness,
 * with the input search (every {@code Verifier.nondet*()} call is free, the
 * violation can be anywhere).
 * <p>
 * Prints {@code Violation found} if a run of the program violates the property,
 * else {@code Could not validate}. For valid-assert the violation is an
 * uncaught {@code AssertionError}, with assertions enabled. For
 * no-runtime-exception it is an uncaught {@code RuntimeException}, and the same
 * values must violate the property both with and without assertions enabled,
 * since the rules do not say which applies. It never claims that the property
 * holds.
 */
public final class BugFinder {

	public static final String VERDICT_FOUND = "Violation found";
	private static final String VERDICT_UNKNOWN = "Could not validate";

	private BugFinder() {
	}

	/**
	 * @param timeLimitMillis time for the search, from the start of this call
	 */
	public static void find(
			Property property,
			File benchmarkFile,
			String java8Exec,
			File extraSourcesDir,
			long timeLimitMillis)
			throws ValidationException {
		long deadline = System.currentTimeMillis() + timeLimitMillis;
		BenchmarkProperties properties;
		try {
			properties = BenchmarkPropertiesParser.parse(benchmarkFile);
		} catch (FileNotFoundException e) {
			throw new ValidationException(e.getMessage());
		}
		File benchmarkDir = properties.directory();
		List<File> sources = new ArrayList<>();
		sources.add(extraSourcesDir);
		sources.addAll(properties.inputFiles());
		JdiLauncher.compileIfNeeded(benchmarkDir, benchmarkDir, null, sources);
		String classpath = ViolationValidator.buildClasspath(benchmarkDir);
		String mainClass = ViolationValidator.findMainClass(benchmarkDir);

		InputSearch search;
		try {
			search = new InputSearch(mainClass, classpath, java8Exec, sources, property, List.of(), List.of());
		} catch (IOException e) {
			throw new ValidationException("Cannot read the sources: " + e.getMessage(), e);
		}
		Optional<InputSearch.Found> found = search.search(deadline);
		if (found.isEmpty()) {
			RunSummary.reason("no-violation-found", "no run of the search violated " + property);
			RunSummary.print("unknown");
			print(VERDICT_UNKNOWN);
			return;
		}
		InputSearch.Found f = found.get();
		if (property == Property.RUNTIME && !search.violatesWithoutAssertions(f.values(), deadline)) {
			RunSummary.reason("needs-assertions", f.detail() + " with values " + f.values()
					+ " only with assertions enabled");
			RunSummary.print("unknown");
			print(VERDICT_UNKNOWN);
			return;
		}
		RunSummary.reason("violation-found",
				f.detail() + " with values " + f.values() + " (search run " + f.runs() + ")");
		RunSummary.print("found");
		print(VERDICT_FOUND);
	}

	private static void print(
			String verdict) {
		System.out.println(verdict);
		System.out.flush();
	}
}

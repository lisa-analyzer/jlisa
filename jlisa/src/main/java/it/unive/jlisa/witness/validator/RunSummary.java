package it.unive.jlisa.witness.validator;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Why a run ended with its verdict, and what happened to the witness on the
 * way.
 * <p>
 * At the end of every run two lines go to {@code stdout}, whatever the log
 * level (also with {@code -l OFF}), for example:
 * 
 * <pre>
 * [STATS]  witness.values=4 nondet.fixed=3 nondet.free=1 edges.unparseable-scope=2 ...
 * [REASON] unknown/program-exited: program ended without the violation; 1 nondet call(s) not fixed by the witness
 * </pre>
 * 
 * The reason is {@code <verdict>/<code>: <detail>}. The codes are fixed, so
 * logs of many runs can be grouped by them (e.g.
 * {@code grep -h '\[REASON\]' *.log | cut -d: -f1 | sort | uniq -c}). The
 * verdict follows, on a line of its own after {@link #VERDICT_PREFIX} (e.g.
 * {@code [VERDICT] Witness Correct}).
 */
public final class RunSummary {

	/**
	 * The prefix of the line with the verdict of the run.
	 */
	public static final String VERDICT_PREFIX = "[VERDICT] ";

	private static final Map<String, Integer> COUNTS = new LinkedHashMap<>();
	private static String reasonCode;
	private static String reasonDetail;
	private static boolean printed;

	private RunSummary() {
	}

	/** Adds one to a counter, e.g. {@code nondet.free}. */
	public static void count(
			String key) {
		add(key, 1);
	}

	public static void add(
			String key,
			int n) {
		COUNTS.merge(key, n, Integer::sum);
	}

	/**
	 * Yields a copy of the counters, to restore them with
	 * {@link #restoreCounts(Map)} (e.g., before running the program again).
	 */
	public static Map<String, Integer> counts() {
		return new LinkedHashMap<>(COUNTS);
	}

	/** Replaces the counters with the given ones (see {@link #counts()}). */
	public static void restoreCounts(
			Map<String, Integer> counts) {
		COUNTS.clear();
		COUNTS.putAll(counts);
	}

	/** Sets a counter to a value, replacing an earlier one. */
	public static void set(
			String key,
			int n) {
		COUNTS.put(key, n);
	}

	/** Records why the run ends; a later call replaces an earlier one. */
	public static void reason(
			String code,
			String detail) {
		reasonCode = code;
		reasonDetail = detail;
	}

	/**
	 * Ends a run that stops before a verdict ({@code Main}: wrong arguments,
	 * missing files, errors): prints the summary with this reason, then the
	 * verdict {@code Could not validate} on {@code stdout}.
	 */
	public static void couldNotValidate(
			String code,
			String detail) {
		reason(code, detail);
		print("unknown");
		printVerdict("Could not validate");
	}

	/**
	 * Ends a run whose witness cannot be parsed (wrong format version, GraphML,
	 * empty or malformed file): prints the summary with this reason, then the
	 * verdict {@code Parsing failed} on {@code stdout}.
	 */
	public static void parsingFailed(
			String detail) {
		reason("parse-error", detail);
		print("error");
		printVerdict("Parsing failed");
	}

	/**
	 * Ends a run whose witness has a version of the witness format that is not
	 * supported: prints the summary with this reason, then the verdict
	 * {@code Invalid witness format} on {@code stdout}.
	 */
	public static void invalidFormat(
			String detail) {
		reason("invalid-witness-format", detail);
		print("error");
		printVerdict("Invalid witness format");
	}

	/**
	 * Ends a run whose witness is not of the kind expected with {@code --type}
	 * (e.g. a correctness witness in the validation of violation witnesses):
	 * prints the summary with this reason, then the verdict
	 * {@code Invalid witness type} on {@code stdout}.
	 */
	public static void invalidType(
			String detail) {
		reason("invalid-witness-type", detail);
		print("error");
		printVerdict("Invalid witness type");
	}

	/**
	 * Prints {@code [STATS]} and {@code [REASON]} once; {@code verdict} is
	 * correct, spurious, unknown or error.
	 */
	public static void print(
			String verdict) {
		if (printed) {
			return;
		}
		printed = true;
		String stats = COUNTS.entrySet().stream()
				.map(e -> e.getKey() + "=" + e.getValue())
				.collect(Collectors.joining(" "));
		System.out.println("[STATS]  " + (stats.isEmpty() ? "-" : stats));
		System.out.println("[REASON] " + verdict + "/" + (reasonCode == null ? "not-recorded" : reasonCode) + ": "
				+ (reasonDetail == null ? "-" : reasonDetail));
	}

	/**
	 * Prints the verdict of the run on {@code stdout}, after
	 * {@link #VERDICT_PREFIX}: among the logs, the SV-COMP wrapper finds it by
	 * that prefix.
	 *
	 * @param verdict the verdict (e.g. {@code Witness Correct})
	 */
	public static void printVerdict(
			String verdict) {
		System.out.println(VERDICT_PREFIX + verdict);
		System.out.flush();
	}
}

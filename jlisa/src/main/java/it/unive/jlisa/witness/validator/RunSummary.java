package it.unive.jlisa.witness.validator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Why a run ended with its verdict, and what happened to the witness on the way.
 *
 * <p>At the end of every run two lines go to {@code stderr}, also without {@code --verbose}:
 * <pre>
 * WARN  [STATS]  witness.values=4 nondet.fixed=3 nondet.free=1 edges.unparseable-scope=2 ...
 * WARN  [REASON] unknown/program-exited: program ended without the violation; 1 nondet call(s) not fixed by the witness
 * </pre>
 * The reason is {@code <verdict>/<code>: <detail>}. The codes are fixed, so logs of many runs can be
 * grouped by them (e.g. {@code grep -h '\[REASON\]' *.log | cut -d: -f1 | sort | uniq -c}).
 * {@code stdout} still carries only the verdict line, which is what BenchExec reads.
 */
public final class RunSummary {

    private static final Logger LOG = LogManager.getLogger(RunSummary.class);

    private static final Map<String, Integer> COUNTS = new LinkedHashMap<>();
    private static String reasonCode;
    private static String reasonDetail;
    private static boolean printed;

    private RunSummary() {
    }

    /** Adds one to a counter, e.g. {@code nondet.free}. */
    public static void count(String key) {
        add(key, 1);
    }

    public static void add(String key, int n) {
        COUNTS.merge(key, n, Integer::sum);
    }

    /** Sets a counter to a value, replacing an earlier one. */
    public static void set(String key, int n) {
        COUNTS.put(key, n);
    }

    /** Records why the run ends; a later call replaces an earlier one. */
    public static void reason(String code, String detail) {
        reasonCode = code;
        reasonDetail = detail;
    }

    /**
     * Ends a run that stops before a verdict ({@code Main}: wrong arguments, missing files, errors):
     * prints the summary with this reason, then the verdict {@code Could not validate} on {@code stdout}.
     */
    public static void couldNotValidate(String code, String detail) {
        reason(code, detail);
        print("unknown");
        System.out.println("Could not validate");
    }

    /** Prints {@code [STATS]} and {@code [REASON]} once; {@code verdict} is correct, spurious, unknown or error. */
    public static void print(String verdict) {
        if (printed) {
            return;
        }
        printed = true;
        String stats = COUNTS.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(" "));
        LOG.warn("[STATS]  {}", stats.isEmpty() ? "-" : stats);
        LOG.warn("[REASON] {}/{}: {}", verdict,
                reasonCode == null ? "not-recorded" : reasonCode,
                reasonDetail == null ? "-" : reasonDetail);
    }
}

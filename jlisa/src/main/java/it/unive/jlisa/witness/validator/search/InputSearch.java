package it.unive.jlisa.witness.validator.search;

import it.unive.jlisa.witness.validator.RunSummary;
import it.unive.jlisa.witness.validator.ValidationException;
import it.unive.jlisa.witness.validator.model.Interception;
import it.unive.jlisa.witness.validator.model.Property;
import it.unive.jlisa.witness.validator.model.TargetPoint;
import it.unive.jlisa.witness.validator.violation.ValidatorLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Looks for values of the {@code Verifier.nondet*()} calls that the witness does not fix, such that
 * the program violates the property where the witness says. It learns them from the program:
 *
 * <ol>
 *   <li>a colored run: every free call site returns its own recognizable value;</li>
 *   <li>at the watched lines (comparisons, assertions, string tests), a variable holding a value that
 *       only one site returned links the line to that site (input-to-state correspondence), and what
 *       the line compares it with becomes a candidate for the site;</li>
 *   <li>candidates are tried by generators taking turns, so that no kind starves the others: the
 *       defaults, all learned values together, one site at a time on the best run so far, on the
 *       colored values and on the defaults, boundary values, combinations of boolean sites.</li>
 * </ol>
 *
 * <p>It only confirms: a run that violates the property is a real violation consistent with the
 * witness (the witness fixes nothing about the free calls). It never rejects.
 */
public final class InputSearch {

    /** At most this many runs, colored run included. */
    static final int MAX_RUNS = 40;
    /** Time limit of one run. */
    static final long RUN_TIMEOUT_MILLIS = 10_000;
    static final int JOINT_VARIANTS = 6;
    static final int MAX_BOOLEAN_SITES = 5;

    /** A violating run: the values of the free calls and how many runs it took. */
    public record Found(Map<String, Val> values, int runs, String detail) {
    }

    private final String mainClass;
    private final String classpath;
    private final String java;
    private final ProgramText text;
    private final Property property;
    private final List<TargetPoint> targets;
    private final List<Interception> witnessValues;

    /**
     * @param witnessValues the witness's interceptions, with their counts before any run
     * @param sourceDirs    the task's source directories (to find the lines worth watching)
     */
    public InputSearch(String mainClass, String classpath, String java, Collection<java.io.File> sourceDirs,
                       Property property, List<TargetPoint> targets, List<Interception> witnessValues)
            throws java.io.IOException {
        this.mainClass = mainClass;
        this.classpath = classpath;
        this.java = java;
        this.text = new ProgramText(sourceDirs);
        this.property = property;
        this.targets = targets;
        this.witnessValues = witnessValues;
    }

    /** Searches until a violation, the run limit or the deadline (epoch milliseconds). */
    public Optional<Found> search(long deadline) throws ValidationException {
        Map<String, String> types = new LinkedHashMap<>();
        Map<String, LinkedHashSet<Val>> learned = new LinkedHashMap<>();
        Set<Map<String, Val>> tried = new HashSet<>();
        int runs = 0;

        SearchRun first = run(Map.of(), true, deadline);
        runs++;
        if (first == null) {
            return Optional.empty();
        }
        if (first.outcome == SearchRun.Outcome.VIOLATION) {
            return found(first.injected, runs, first.detail);
        }
        if (first.outcome == SearchRun.Outcome.WITNESS_VALUE_NOT_APPLIED || first.freeSites.isEmpty()) {
            return Optional.empty(); // nothing the search may choose
        }
        absorb(first, types, learned);
        Map<String, Val> colored = completed(Map.of(), first);
        Map<String, Val> defaults = new LinkedHashMap<>();
        AtomicReference<Map<String, Val>> best = new AtomicReference<>(colored);
        int bestScore = first.score();

        List<Supplier<List<Map<String, Val>>>> generators = List.of(
                () -> List.of(defaults),
                () -> jointVariants(best.get(), learned),
                () -> singles(best.get(), types, learned, false),
                () -> singles(colored, types, learned, false),
                () -> singles(defaults, types, learned, false),
                () -> singles(defaults, types, learned, true),
                () -> singles(colored, types, learned, true),
                () -> booleanCombos(defaults, types));
        int g = 0;
        int idle = 0;
        while (System.currentTimeMillis() < deadline && runs < MAX_RUNS && idle < generators.size()) {
            Map<String, Val> next = null;
            for (Map<String, Val> c : generators.get(g).get()) {
                if (!tried.contains(c)) {
                    next = c;
                    break;
                }
            }
            g = (g + 1) % generators.size();
            if (next == null) {
                idle++;
                continue;
            }
            idle = 0;
            tried.add(next);
            SearchRun r = run(next, false, deadline);
            runs++;
            if (r == null || r.outcome == SearchRun.Outcome.WITNESS_VALUE_NOT_APPLIED) {
                break;
            }
            ValidatorLogger.debug("[SEARCH] run {}: {} -> {} {}", runs, next, r.outcome, r.detail);
            if (r.outcome == SearchRun.Outcome.VIOLATION) {
                return found(completed(next, r), runs, r.detail);
            }
            absorb(r, types, learned);
            if (r.score() > bestScore) {
                bestScore = r.score();
                best.set(completed(next, r));
            }
        }
        RunSummary.add("search.runs", runs);
        RunSummary.add("search.free-sites", types.size());
        return Optional.empty();
    }

    /**
     * Replays found values with Java assertions disabled: true if the property is violated again.
     * For no-runtime-exception the rules do not say whether assertions are enabled; a violation
     * that happens both with and without them does not depend on it.
     */
    public boolean violatesWithoutAssertions(Map<String, Val> values, long deadline) throws ValidationException {
        long left = deadline - System.currentTimeMillis();
        if (left <= 0) {
            return false;
        }
        SearchRun r = SearchRun.run(mainClass, classpath, java, text, property, targets, witnessValues,
                values, false, Math.min(left, RUN_TIMEOUT_MILLIS), false);
        return r.outcome == SearchRun.Outcome.VIOLATION;
    }

    private SearchRun run(Map<String, Val> assignment, boolean colored, long deadline) throws ValidationException {
        long left = deadline - System.currentTimeMillis();
        if (left <= 0) {
            return null;
        }
        return SearchRun.run(mainClass, classpath, java, text, property, targets, witnessValues,
                assignment, colored, Math.min(left, RUN_TIMEOUT_MILLIS));
    }

    private static Optional<Found> found(Map<String, Val> values, int runs, String detail) {
        RunSummary.add("search.runs", runs);
        return Optional.of(new Found(values, runs, detail));
    }

    /** The assignment with the values the run actually used for its free sites. */
    private static Map<String, Val> completed(Map<String, Val> assignment, SearchRun r) {
        Map<String, Val> out = new LinkedHashMap<>(r.injected);
        out.putAll(assignment);
        out.keySet().retainAll(r.freeSites.keySet());
        return out;
    }

    private static void absorb(SearchRun r, Map<String, String> types, Map<String, LinkedHashSet<Val>> learned) {
        r.freeSites.forEach(types::putIfAbsent);
        for (Map.Entry<String, LinkedHashSet<Val>> e : r.learned.entrySet()) {
            LinkedHashSet<Val> into = learned.computeIfAbsent(e.getKey(), k -> new LinkedHashSet<>());
            Val used = r.injected.get(e.getKey());
            for (Val v : e.getValue()) {
                if (used == null || !used.same(v)) {
                    into.add(v);
                }
            }
        }
    }

    /** Every site with learned values takes its i-th learned value at the same time (i = 0, 1, ...). */
    private static List<Map<String, Val>> jointVariants(Map<String, Val> base, Map<String, LinkedHashSet<Val>> learned) {
        int depth = 0;
        for (LinkedHashSet<Val> v : learned.values()) {
            depth = Math.max(depth, v.size());
        }
        List<Map<String, Val>> out = new ArrayList<>();
        for (int i = 0; i < Math.min(depth, JOINT_VARIANTS); i++) {
            Map<String, Val> a = new LinkedHashMap<>(base);
            for (Map.Entry<String, LinkedHashSet<Val>> e : learned.entrySet()) {
                List<Val> vals = new ArrayList<>(e.getValue());
                if (!vals.isEmpty()) {
                    a.put(e.getKey(), vals.get(Math.min(i, vals.size() - 1)));
                }
            }
            out.add(a);
        }
        return out;
    }

    /** One site changed on the base: to a learned value, or to a boundary value of its type. */
    private static List<Map<String, Val>> singles(Map<String, Val> base, Map<String, String> types,
                                                  Map<String, LinkedHashSet<Val>> learned, boolean boundaries) {
        List<Map<String, Val>> out = new ArrayList<>();
        for (Map.Entry<String, String> s : types.entrySet()) {
            Collection<Val> vals = boundaries ? Val.boundaries(s.getValue()) : learned.getOrDefault(s.getKey(), new LinkedHashSet<>());
            for (Val v : vals) {
                Map<String, Val> a = new LinkedHashMap<>(base);
                a.put(s.getKey(), v);
                out.add(a);
            }
        }
        return out;
    }

    /** All combinations of the boolean sites (colors cannot tell two booleans apart). */
    private static List<Map<String, Val>> booleanCombos(Map<String, Val> base, Map<String, String> types) {
        List<String> bools = new ArrayList<>();
        types.forEach((site, type) -> {
            if (type.equals("boolean")) {
                bools.add(site);
            }
        });
        List<Map<String, Val>> out = new ArrayList<>();
        if (bools.isEmpty() || bools.size() > MAX_BOOLEAN_SITES) {
            return out;
        }
        for (int bits = 0; bits < (1 << bools.size()); bits++) {
            Map<String, Val> a = new LinkedHashMap<>(base);
            for (int i = 0; i < bools.size(); i++) {
                a.put(bools.get(i), Val.of((bits >> i) & 1));
            }
            out.add(a);
        }
        return out;
    }
}

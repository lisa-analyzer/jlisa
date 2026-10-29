package it.unive.jlisa.witness.validator.model;

/**
 * A single loop invariant from a correctness witness.
 *
 * <p>The {@code value} is a Java expression string (e.g. {@code "i >= 0 && i <= n && s >= 0"})
 * that must hold at the loop head identified by {@code (fileName, line, function)}.
 *
 * <p>The three induction conditions — initiation, inductiveness, safety — are checked by
 * {@code InductionChecker} using jLISA's abstract-interpretation infrastructure.
 */
public record CorrectnessInvariant(
        String fileName,
        int line,
        String function,
        InvariantType type,
        String format,
        String value) {

    /**
     * The semantic category of the invariant.
     */
    public enum InvariantType {
        LOOP_INVARIANT,
        LOCATION_INVARIANT
    }

    @Override
    public String toString() {
        return type + " @ " + fileName + ":" + line + " [" + function + "] = " + value;
    }
}

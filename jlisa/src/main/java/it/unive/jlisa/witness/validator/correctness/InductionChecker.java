package it.unive.jlisa.witness.validator.correctness;

import it.unive.jlisa.witness.validator.correctness.InvariantParser.Constraint;
import it.unive.jlisa.witness.validator.correctness.InvariantParser.ParsedInvariant;
import it.unive.jlisa.witness.validator.violation.ValidatorLogger;
import it.unive.lisa.program.cfg.statement.Statement;

/**
 * Checks the three induction conditions for a loop invariant.
 *
 * <h3>Conditions</h3>
 * <ol>
 *   <li><strong>Initiation</strong>: I holds before the first loop iteration
 *       (pre-loop abstract state implies I).</li>
 *   <li><strong>Inductiveness</strong>: I ∧ loop-condition ∧ body ⊨ I' — executing the
 *       loop body with I assumed preserves I.</li>
 *   <li><strong>Safety</strong>: I ∧ ¬loop-condition ⊨ property — at loop exit,
 *       the property holds.</li>
 * </ol>
 *
 * <h3>Current scope</h3>
 * <p>Full checking requires extracting per-variable interval bounds from jLISA's
 * analysis results at specific program points.  This API is being developed as part
 * of the jLISA correctness-witness integration.  Until available, all three methods
 * return {@link CheckResult#UNKNOWN} and the validator outputs {@code Could not validate}
 * rather than an incorrect verdict.
 *
 * <p>The interface is designed so that implementations can be dropped in once jLISA
 * exposes the required abstract-state query API.
 */
public final class InductionChecker {

    /**
     * Result of an induction condition check.
     */
    public enum CheckResult {
        /**
         * The condition provably holds in the abstract domain.
         */
        HOLDS,
        /**
         * The condition provably does not hold — the invariant is too weak or wrong.
         */
        DOES_NOT_HOLD,
        /**
         * The abstract domain information is insufficient to determine the result.
         */
        UNKNOWN
    }

    private InductionChecker() {
    }

    /**
     * Checks whether the invariant holds at the loop head according to jLISA's computed
     * abstract state.
     *
     * <p><strong>TODO:</strong> implement using jLISA's per-variable interval query API.
     *
     * @param invariant parsed invariant to verify
     * @param loopHead  the loop-head {@link Statement} in the jLISA CFG
     * @return the check result
     */
    public static CheckResult checkInitiation(ParsedInvariant invariant, Statement loopHead) {
        ValidatorLogger.warn(
                "[INDUCTION] Initiation check for '{}' at {} — pending jLISA abstract state API",
                invariant, loopHead.getLocation());
        logConstraints(invariant);
        return CheckResult.UNKNOWN;
    }

    /**
     * Checks whether executing the loop body preserves the invariant.
     *
     * <p><strong>TODO:</strong> requires re-running jLISA analysis with the invariant as a
     * precondition at the loop head.
     *
     * @param invariant parsed invariant to verify
     * @param loopHead  the loop-head {@link Statement}
     * @return the check result
     */
    public static CheckResult checkInductiveness(ParsedInvariant invariant, Statement loopHead) {
        ValidatorLogger.warn(
                "[INDUCTION] Inductiveness check for '{}' — not yet implemented",
                invariant);
        return CheckResult.UNKNOWN;
    }

    /**
     * Checks whether the invariant at loop exit implies the property.
     *
     * <p><strong>TODO:</strong> requires computing the post-state of I ∧ ¬loop-condition
     * and verifying no error state is reachable.
     *
     * @param invariant parsed invariant to verify
     * @param loopHead  the loop-head {@link Statement}
     * @return the check result
     */
    public static CheckResult checkSafety(ParsedInvariant invariant, Statement loopHead) {
        ValidatorLogger.warn(
                "[INDUCTION] Safety check for '{}' — not yet implemented",
                invariant);
        return CheckResult.UNKNOWN;
    }

    // ------------------------------------------------------------------
    // private helpers
    // ------------------------------------------------------------------

    private static void logConstraints(ParsedInvariant invariant) {
        for (Constraint c : invariant.constraints()) {
            ValidatorLogger.debug("[INDUCTION]   Constraint: {}", c);
        }
    }
}

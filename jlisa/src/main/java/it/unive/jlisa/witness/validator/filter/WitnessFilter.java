package it.unive.jlisa.witness.validator.filter;

/**
 * Content-based noise filter for v1 witness assumption edges.
 *
 * <p>Certain assumption edges in GraphML witnesses refer to JVM internals, standard-library
 * methods, or model-checker bookkeeping that are irrelevant to property-violation reproduction.
 * This filter discards them before plan construction.
 *
 * <p>All decisions are based on the <em>content</em> of the scope string — no producer-specific
 * logic, no {@code if (producer == "...")} branches anywhere in this class.
 */
public final class WitnessFilter {

    private WitnessFilter() {
    }

    /**
     * Returns {@code true} if this assumption edge should be retained for plan construction.
     *
     * @param scope the raw {@code assumption.scope} value (may be {@code null})
     * @return {@code false} when the edge is noise and should be discarded
     */
    public static boolean isInteresting(String scope) {
        if (scope == null || scope.isBlank()) {
            return false;
        }

        // JVM standard library — not relevant to program behaviour
        if (scope.contains("java.lang.") || scope.contains("java/lang/")) {
            return false;
        }
        if (scope.contains("java.util.") || scope.contains("java/util/")) {
            return false;
        }

        // CProver model-checker internal bookkeeping
        if (scope.contains("cprover")) {
            return false;
        }

        // Static initializers that are not Verifier-related are JVM housekeeping
        if (scope.contains("<clinit>") && !scope.contains("Verifier")) {
            return false;
        }

        return true;
    }
}

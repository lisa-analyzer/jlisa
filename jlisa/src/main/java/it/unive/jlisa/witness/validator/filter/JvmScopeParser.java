package it.unive.jlisa.witness.validator.filter;

import java.util.Optional;

/**
 * Parses JVM method scope strings from v1 witnesses ({@code assumption.scope} values).
 *
 * <p>Two formats are auto-detected purely from the string structure — no producer-specific
 * logic anywhere in this class.
 *
 * <h3>Format A — JVM object-type notation</h3>
 * Example: {@code java::LMain;.getBoolean()Z}
 * <br>Detected by: the fragment after {@code java::} contains an {@code L...;} JVM type descriptor
 * before the first {@code .} separator.
 * <br>Extraction: strip {@code L} and {@code ;} → class name {@code Main}; token after {@code .}
 * up to {@code (} → method name {@code getBoolean}; remainder → descriptor {@code ()Z}.
 *
 * <h3>Format B — qualified name with colon separator</h3>
 * Example: {@code java::org.sosy_lab.sv_benchmarks.Verifier.nondetInt:()I}
 * <br>Detected by: presence of {@code :} before the descriptor in the part after {@code java::}.
 * <br>Extraction: last {@code .} before {@code :} splits class from method; everything after
 * {@code :} is the descriptor.
 */
public final class JvmScopeParser {

    private JvmScopeParser() {
    }

    /**
     * Result of a successful parse.
     *
     * @param className  fully-qualified class name (e.g. {@code org.sosy_lab.sv_benchmarks.Verifier})
     * @param methodName simple method name (e.g. {@code nondetInt})
     * @param descriptor JVM method descriptor starting with {@code (} (e.g. {@code ()I})
     */
    public record ParsedScope(String className, String methodName, String descriptor) {

        /**
         * Returns {@code true} if this scope refers to a {@code Verifier.nondet*} method.
         */
        public boolean isVerifierNondet() {
            return (className.endsWith(".Verifier") || className.equals("Verifier"))
                    && methodName.startsWith("nondet");
        }
    }

    /**
     * Attempts to parse {@code scope}.
     *
     * @param scope the raw {@code assumption.scope} string from the witness
     * @return a parsed result, or {@link Optional#empty()} if the string is not a recognised
     * JVM scope format
     */
    public static Optional<ParsedScope> parse(String scope) {
        if (scope == null || scope.isBlank()) {
            return Optional.empty();
        }

        // All known formats begin with "java::"
        if (!scope.startsWith("java::")) {
            return Optional.empty();
        }

        String inner = scope.substring(6); // strip "java::"

        // ---- Format A: contains "L<ClassName>;" (JVM object-type notation) ----
        // Pattern: java::LMain;.getBoolean([Ljava/lang/String;)Z
        //                    ^---^  object type descriptor around the class name
        if (isFormatA(inner)) {
            return parseFormatA(inner);
        }

        // ---- Format B: qualified name with colon separator ----
        // Pattern: java::org.sosy_lab.sv_benchmarks.Verifier.nondetInt:()I
        int colonIdx = inner.lastIndexOf(':');
        if (colonIdx > 0 && colonIdx < inner.length() - 1) {
            return parseFormatB(inner, colonIdx);
        }

        return Optional.empty();
    }

    // ------------------------------------------------------------------
    // private helpers
    // ------------------------------------------------------------------

    /**
     * Returns {@code true} when {@code inner} (the part after {@code java::}) looks like
     * Format A.  The heuristic: it contains an {@code L} followed later by {@code ;.} which
     * signals a JVM object-type descriptor for the declaring class.
     */
    private static boolean isFormatA(String inner) {
        // e.g. "LMain;.getBoolean()Z" or "LMain;.main([Ljava/lang/String;)V"
        // or "java::LMain;.nondetInt:()I" — but Format B takes precedence if ":" appears
        // after the descriptor; here we look for L...;. specifically at the start of inner
        // (after stripping java:: prefix) as the class is the subject of the scope.
        int lIdx = inner.indexOf('L');
        int semiIdx = lIdx >= 0 ? inner.indexOf(';', lIdx) : -1;
        return lIdx >= 0 && semiIdx > lIdx && semiIdx < inner.length() - 1
                && inner.charAt(semiIdx + 1) == '.';
    }

    private static Optional<ParsedScope> parseFormatA(String inner) {
        // inner = "LMain;.getBoolean()Z"
        int lIdx = inner.indexOf('L');
        int semiIdx = inner.indexOf(';', lIdx);
        String className = inner.substring(lIdx + 1, semiIdx).replace('/', '.');

        // After ";." is the method signature
        int dotIdx = semiIdx + 1; // points to '.'
        if (dotIdx >= inner.length() - 1) {
            return Optional.empty();
        }
        String rest = inner.substring(dotIdx + 1); // "getBoolean()Z"
        int parenIdx = rest.indexOf('(');
        if (parenIdx < 0) {
            return Optional.empty();
        }
        String methodName = rest.substring(0, parenIdx);
        String descriptor = rest.substring(parenIdx);
        return Optional.of(new ParsedScope(className, methodName, descriptor));
    }

    private static Optional<ParsedScope> parseFormatB(String inner, int colonIdx) {
        // inner = "org.sosy_lab.sv_benchmarks.Verifier.nondetInt:()I"
        String descriptor = inner.substring(colonIdx + 1); // "()I"
        String beforeColon = inner.substring(0, colonIdx); // "org.sosy_lab...Verifier.nondetInt"
        int lastDot = beforeColon.lastIndexOf('.');
        if (lastDot < 0) {
            // No dot → the method is in the default package; treat as simple class name
            return Optional.of(new ParsedScope("", beforeColon, descriptor));
        }
        String className = beforeColon.substring(0, lastDot);
        String methodName = beforeColon.substring(lastDot + 1);
        return Optional.of(new ParsedScope(className, methodName, descriptor));
    }
}

package it.unive.jlisa.witness.validator.filter;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts concrete values from v1 witness assumption strings.
 *
 * <p>Three patterns are tried in order, without any producer-specific branching:
 * <ol>
 *   <li>Boolean literals {@code true} / {@code false}</li>
 *   <li>Return-slot assignments: {@code return_tmpN = <val>;} — signals a method return value</li>
 *   <li>Variable assignments: {@code <varName> = <val>;} — a program-variable assignment</li>
 * </ol>
 *
 * <p>The {@code return_tmpN} pattern is a structural signal present in GDart and JBMC witnesses
 * alike; it is not treated as a producer-specific marker.
 */
public final class ValueExtractor {

    private static final Pattern RETURN_TMP = Pattern.compile("^return_tmp\\d+\\s*=\\s*(.+?);?$");
    private static final Pattern VAR_ASSIGN = Pattern.compile("^([A-Za-z_$][\\w$]*)\\s*=\\s*(.+?);?$");

    private ValueExtractor() {
    }

    /**
     * Extraction result.
     *
     * @param varName  variable name (may be {@code null} for plain boolean literals or when the
     *                 assignment target is irrelevant)
     * @param value    the concrete value string (e.g. {@code "0"}, {@code "true"})
     * @param isReturn {@code true} when the assignment targets a {@code return_tmpN} slot,
     *                 meaning {@code value} is the method's intended return value
     */
    public record Extracted(String varName, String value, boolean isReturn) {
    }

    /**
     * Attempts to extract a concrete value from an assumption string.
     *
     * @param assumption the raw assumption text from the witness edge
     * @return the extraction result, or {@link Optional#empty()} if no pattern matches
     */
    public static Optional<Extracted> extract(String assumption) {
        if (assumption == null || assumption.isBlank()) {
            return Optional.empty();
        }

        String trimmed = assumption.trim();

        // Pattern 1: plain boolean literals
        if ("true".equals(trimmed)) {
            return Optional.of(new Extracted(null, "true", false));
        }
        if ("false".equals(trimmed)) {
            return Optional.of(new Extracted(null, "false", false));
        }

        // Pattern 2: return_tmpN = <val>;
        Matcher retMatcher = RETURN_TMP.matcher(trimmed);
        if (retMatcher.matches()) {
            return Optional.of(new Extracted(null, retMatcher.group(1).trim(), true));
        }

        // Pattern 3: <varName> = <val>;
        Matcher varMatcher = VAR_ASSIGN.matcher(trimmed);
        if (varMatcher.matches()) {
            return Optional.of(new Extracted(varMatcher.group(1).trim(), varMatcher.group(2).trim(), false));
        }

        return Optional.empty();
    }
}

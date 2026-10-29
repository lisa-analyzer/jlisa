package it.unive.jlisa.witness.validator.correctness;

import it.unive.jlisa.witness.validator.ValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a {@code java_expression} invariant string into a structured representation
 * that can be evaluated against jLISA's abstract domain.
 *
 * <p>The current implementation handles linear arithmetic inequalities and conjunctions
 * of the form used in typical loop invariants for SV-COMP Java benchmarks:
 * <ul>
 *   <li>{@code "i >= 0 && i <= n && s >= 0"}</li>
 *   <li>{@code "x == 42"}</li>
 *   <li>{@code "count >= 0"}</li>
 * </ul>
 *
 * <p>Non-linear and object-referencing invariants are flagged as unsupported.
 */
public final class InvariantParser {

    /**
     * An atomic linear arithmetic constraint (e.g. {@code i >= 0}).
     */
    public record Constraint(String lhs, String op, String rhs) {

        @Override
        public String toString() {
            return lhs + " " + op + " " + rhs;
        }
    }

    /**
     * A parsed invariant expressed as a conjunction of {@link Constraint}s.
     */
    public record ParsedInvariant(List<Constraint> constraints) {

        public boolean isEmpty() {
            return constraints.isEmpty();
        }

        @Override
        public String toString() {
            return String.join(" && ", constraints.stream().map(Constraint::toString).toList());
        }
    }

    // Matches: identifier op (identifier | number)
    // Supported operators: >=, <=, ==, !=, >, <
    private static final Pattern CONSTRAINT_PATTERN = Pattern.compile(
            "([A-Za-z_$][\\w$]*)\\s*(>=|<=|==|!=|>|<)\\s*([-A-Za-z_$0-9][\\w$]*)");

    private InvariantParser() {
    }

    /**
     * Parses {@code expression} into a conjunction of linear arithmetic constraints.
     *
     * @param expression the {@code value} field from a correctness witness invariant entry
     * @return the parsed invariant
     * @throws ValidationException if the expression contains constructs we cannot handle
     */
    public static ParsedInvariant parse(String expression) throws ValidationException {
        if (expression == null || expression.isBlank()) {
            throw new ValidationException("Invariant expression is empty");
        }

        List<Constraint> constraints = new ArrayList<>();

        // Split on && (the only conjunction form we support at this level)
        String[] parts = expression.split("&&");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            Matcher m = CONSTRAINT_PATTERN.matcher(trimmed);
            if (!m.matches()) {
                throw new ValidationException(
                        "Unsupported invariant clause '" + trimmed + "'. "
                                + "Only linear arithmetic comparisons (e.g. i >= 0) are currently supported.");
            }

            constraints.add(new Constraint(m.group(1), m.group(2), m.group(3)));
        }

        if (constraints.isEmpty()) {
            throw new ValidationException("No parseable constraints in invariant: " + expression);
        }

        return new ParsedInvariant(constraints);
    }
}

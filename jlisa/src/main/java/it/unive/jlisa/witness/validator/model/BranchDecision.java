package it.unive.jlisa.witness.validator.model;

/**
 * A branching waypoint from a v2 witness: at the given source location the program must take
 * the branch indicated by {@code direction} ({@code true} = then-branch, {@code false} = else).
 *
 * <p>The event loop sets a breakpoint at {@code line}. If the actual branch direction would
 * differ from {@code direction}, {@code VariableSteerer} flips the controlling variable.
 */
public record BranchDecision(String fileName, int line, boolean direction) {

    @Override
    public String toString() {
        return fileName + ":" + line + " → " + (direction ? "true (then)" : "false (else)");
    }
}
